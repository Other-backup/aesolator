#include "virgl_server.h"

#include <dlfcn.h>
#include <errno.h>
#include <sched.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

#include "pipe/p_defines.h"
#include "virgl_hw.h"

#include "virgl_server_protocol.h"
#include "virgl_server_shm.h"

extern EGLDisplay globalEGLDisplay;
extern EGLContext globalEGLContext;

static const char *const VIRGL_BACKEND_SONAME = "libvirglrenderer.so";

static void *virgl_server_require_symbol_locked(const char *name) {
    void *symbol = dlsym(g_virgl_server.backend_handle, name);
    if (!symbol) {
        VIRGL_LOGE("Missing backend symbol %s: %s", name, dlerror());
    }
    return symbol;
}

static int virgl_server_load_backend_locked(void) {
    if (g_virgl_server.backend_loaded) {
        return 0;
    }

    g_virgl_server.backend_handle = dlopen(VIRGL_BACKEND_SONAME, RTLD_NOW | RTLD_LOCAL);
    if (!g_virgl_server.backend_handle) {
        VIRGL_LOGE("dlopen(%s) failed: %s", VIRGL_BACKEND_SONAME, dlerror());
        return -ENOENT;
    }

    g_virgl_server.api.init = (pfn_virgl_renderer_init) virgl_server_require_symbol_locked("virgl_renderer_init");
    g_virgl_server.api.poll = (pfn_virgl_renderer_poll) virgl_server_require_symbol_locked("virgl_renderer_poll");
    g_virgl_server.api.context_create = (pfn_virgl_renderer_context_create) virgl_server_require_symbol_locked("virgl_renderer_context_create");
    g_virgl_server.api.context_destroy = (pfn_virgl_renderer_context_destroy) virgl_server_require_symbol_locked("virgl_renderer_context_destroy");
    g_virgl_server.api.resource_create = (pfn_virgl_renderer_resource_create) virgl_server_require_symbol_locked("virgl_renderer_resource_create");
    g_virgl_server.api.resource_unref = (pfn_virgl_renderer_resource_unref) virgl_server_require_symbol_locked("virgl_renderer_resource_unref");
    g_virgl_server.api.get_cap_set = (pfn_virgl_renderer_get_cap_set) virgl_server_require_symbol_locked("virgl_renderer_get_cap_set");
    g_virgl_server.api.fill_caps = (pfn_virgl_renderer_fill_caps) virgl_server_require_symbol_locked("virgl_renderer_fill_caps");
    g_virgl_server.api.resource_attach_iov = (pfn_virgl_renderer_resource_attach_iov) virgl_server_require_symbol_locked("virgl_renderer_resource_attach_iov");
    g_virgl_server.api.resource_detach_iov = (pfn_virgl_renderer_resource_detach_iov) virgl_server_require_symbol_locked("virgl_renderer_resource_detach_iov");
    g_virgl_server.api.create_fence = (pfn_virgl_renderer_create_fence) virgl_server_require_symbol_locked("virgl_renderer_create_fence");
    g_virgl_server.api.ctx_attach_resource = (pfn_virgl_renderer_ctx_attach_resource) virgl_server_require_symbol_locked("virgl_renderer_ctx_attach_resource");
    g_virgl_server.api.ctx_detach_resource = (pfn_virgl_renderer_ctx_detach_resource) virgl_server_require_symbol_locked("virgl_renderer_ctx_detach_resource");
    g_virgl_server.api.submit_cmd = (pfn_virgl_renderer_submit_cmd) virgl_server_require_symbol_locked("virgl_renderer_submit_cmd");
    g_virgl_server.api.transfer_read_iov = (pfn_virgl_renderer_transfer_read_iov) virgl_server_require_symbol_locked("virgl_renderer_transfer_read_iov");
    g_virgl_server.api.transfer_write_iov = (pfn_virgl_renderer_transfer_write_iov) virgl_server_require_symbol_locked("virgl_renderer_transfer_write_iov");
    g_virgl_server.api.resource_get_info = (pfn_virgl_renderer_resource_get_info) virgl_server_require_symbol_locked("virgl_renderer_resource_get_info");
    g_virgl_server.api.cleanup = (pfn_virgl_renderer_cleanup) virgl_server_require_symbol_locked("virgl_renderer_cleanup");

    if (!g_virgl_server.api.init || !g_virgl_server.api.poll ||
        !g_virgl_server.api.context_create || !g_virgl_server.api.context_destroy ||
        !g_virgl_server.api.resource_create || !g_virgl_server.api.resource_unref ||
        !g_virgl_server.api.get_cap_set || !g_virgl_server.api.fill_caps ||
        !g_virgl_server.api.resource_attach_iov || !g_virgl_server.api.resource_detach_iov ||
        !g_virgl_server.api.create_fence || !g_virgl_server.api.ctx_attach_resource ||
        !g_virgl_server.api.ctx_detach_resource || !g_virgl_server.api.submit_cmd ||
        !g_virgl_server.api.transfer_read_iov || !g_virgl_server.api.transfer_write_iov ||
        !g_virgl_server.api.resource_get_info || !g_virgl_server.api.cleanup) {
        dlclose(g_virgl_server.backend_handle);
        memset(&g_virgl_server.api, 0, sizeof(g_virgl_server.api));
        g_virgl_server.backend_handle = NULL;
        return -EINVAL;
    }

    g_virgl_server.backend_loaded = true;
    return 0;
}

static void virgl_server_unload_backend_locked(void) {
    if (g_virgl_server.backend_handle) {
        dlclose(g_virgl_server.backend_handle);
        g_virgl_server.backend_handle = NULL;
    }
    memset(&g_virgl_server.api, 0, sizeof(g_virgl_server.api));
    g_virgl_server.backend_loaded = false;
}

void virgl_server_set_current_client_locked(struct virgl_client *client) {
    g_virgl_server.current_client = client;
}

void virgl_server_clear_current_client_locked(struct virgl_client *client) {
    if (g_virgl_server.current_client == client) {
        g_virgl_server.current_client = NULL;
    }
}

struct virgl_client *virgl_server_find_client_by_ctx_locked(uint32_t ctx_id) {
    struct virgl_client *client = g_virgl_server.clients;
    while (client) {
        if (client->ctx_id == ctx_id) {
            return client;
        }
        client = client->next;
    }
    return NULL;
}

struct virgl_resource_entry *virgl_server_find_resource(struct virgl_client *client, uint32_t handle) {
    struct virgl_resource_entry *entry = client ? client->resources : NULL;
    while (entry) {
        if (entry->handle == handle) {
            return entry;
        }
        entry = entry->next;
    }
    return NULL;
}

static int virgl_server_resolve_shared_config_locked(EGLConfig *config_out) {
    EGLint config_id = 0;
    EGLint count = 0;
    EGLConfig config = NULL;
    const EGLint config_attrs[] = {
            EGL_CONFIG_ID, 0,
            EGL_NONE
    };
    const EGLint fallback_attrs[] = {
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
            EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_NONE
    };

    if (!eglQueryContext(globalEGLDisplay, globalEGLContext, EGL_CONFIG_ID, &config_id)) {
        VIRGL_LOGE("eglQueryContext(EGL_CONFIG_ID) failed: 0x%x", eglGetError());
        config_id = 0;
    }

    if (config_id != 0) {
        EGLint attrs[sizeof(config_attrs) / sizeof(config_attrs[0])];
        memcpy(attrs, config_attrs, sizeof(attrs));
        attrs[1] = config_id;
        if (eglChooseConfig(globalEGLDisplay, attrs, &config, 1, &count) && count == 1) {
            *config_out = config;
            return 0;
        }
    }

    if (eglChooseConfig(globalEGLDisplay, fallback_attrs, &config, 1, &count) && count == 1) {
        *config_out = config;
        return 0;
    }

    VIRGL_LOGE("eglChooseConfig failed for shared VirGL display: 0x%x", eglGetError());
    return -EINVAL;
}

static int virgl_server_refresh_shared_egl_locked(void) {
    EGLConfig config = NULL;

    if (globalEGLDisplay == EGL_NO_DISPLAY || globalEGLContext == EGL_NO_CONTEXT) {
        VIRGL_LOGE("Global EGL bridge is not initialized");
        return -EINVAL;
    }

    if (!eglBindAPI(EGL_OPENGL_ES_API)) {
        VIRGL_LOGE("eglBindAPI(EGL_OPENGL_ES_API) failed: 0x%x", eglGetError());
        return -EINVAL;
    }

    if (virgl_server_resolve_shared_config_locked(&config) < 0) {
        return -EINVAL;
    }

    g_virgl_server.shared_display = globalEGLDisplay;
    g_virgl_server.shared_context = globalEGLContext;
    g_virgl_server.shared_config = config;
    return 0;
}

static virgl_renderer_gl_context virgl_server_create_gl_context_cb(void *cookie, int scanout_idx, struct virgl_renderer_gl_ctx_param *param) {
    EGLContext shared = g_virgl_server.shared_context;
    EGLContext created;
    EGLint major = param && param->major_ver > 0 ? param->major_ver : 3;
    EGLint context_attrs[] = {
            EGL_CONTEXT_CLIENT_VERSION, major >= 3 ? 3 : 2,
            EGL_NONE
    };
    struct virgl_client *client = g_virgl_server.current_client;

    (void) cookie;
    (void) scanout_idx;

    created = eglCreateContext(g_virgl_server.shared_display, g_virgl_server.shared_config, shared, context_attrs);
    if (created == EGL_NO_CONTEXT) {
        VIRGL_LOGE("eglCreateContext failed: 0x%x", eglGetError());
        return NULL;
    }

    if (client && !param->shared && !client->main_context) {
        client->main_context = created;
    }

    return created;
}

static void virgl_server_destroy_gl_context_cb(void *cookie, virgl_renderer_gl_context ctx) {
    (void) cookie;
    if (ctx && g_virgl_server.shared_display != EGL_NO_DISPLAY) {
        eglDestroyContext(g_virgl_server.shared_display, (EGLContext) ctx);
    }
}

static int virgl_server_make_current_cb(void *cookie, int scanout_idx, virgl_renderer_gl_context ctx) {
    EGLBoolean ok;
    (void) cookie;
    (void) scanout_idx;

    ok = eglMakeCurrent(g_virgl_server.shared_display, EGL_NO_SURFACE, EGL_NO_SURFACE, (EGLContext) ctx);
    if (!ok) {
        VIRGL_LOGE("eglMakeCurrent failed: 0x%x", eglGetError());
        return -EINVAL;
    }
    return 0;
}

static void virgl_server_write_fence_cb(void *cookie, uint32_t fence) {
    (void) cookie;
    (void) fence;
}

static void virgl_server_write_context_fence_cb(void *cookie, uint32_t ctx_id, uint32_t ring_idx, uint64_t fence_id) {
    struct virgl_client *client = virgl_server_find_client_by_ctx_locked(ctx_id);
    (void) cookie;
    (void) ring_idx;

    if (client) {
        client->retired_fence_id = (uint32_t) fence_id;
    }
}

static void *virgl_server_get_egl_display_cb(void *cookie) {
    (void) cookie;
    return g_virgl_server.shared_display == EGL_NO_DISPLAY ? NULL : g_virgl_server.shared_display;
}

static const struct virgl_renderer_callbacks virgl_server_callbacks = {
        .version = VIRGL_RENDERER_CALLBACKS_VERSION,
        .write_fence = virgl_server_write_fence_cb,
        .create_gl_context = virgl_server_create_gl_context_cb,
        .destroy_gl_context = virgl_server_destroy_gl_context_cb,
        .make_current = virgl_server_make_current_cb,
        .write_context_fence = virgl_server_write_context_fence_cb,
        .get_egl_display = virgl_server_get_egl_display_cb,
};

static int virgl_server_make_main_context_current_locked(struct virgl_client *client) {
    if (!client || !client->main_context) {
        return -ENODEV;
    }
    return virgl_server_make_current_cb(NULL, 0, client->main_context);
}

static void virgl_server_destroy_resource_entry_locked(struct virgl_resource_entry *entry) {
    if (!entry) {
        return;
    }

    if (entry->framebuffer) {
        glDeleteFramebuffers(1, &entry->framebuffer);
    }

    if (entry->iov.iov_base && entry->iov.iov_len > 0) {
        munmap(entry->iov.iov_base, entry->iov.iov_len);
    }

    free(entry);
}

static void virgl_server_remove_resource_locked(struct virgl_client *client, struct virgl_resource_entry *target) {
    struct virgl_resource_entry **cursor = client ? &client->resources : NULL;

    if (!cursor || !target) {
        return;
    }

    while (*cursor) {
        if (*cursor == target) {
            *cursor = target->next;
            return;
        }
        cursor = &(*cursor)->next;
    }
}

static void virgl_server_remove_client_locked(struct virgl_client *client) {
    struct virgl_client **cursor = &g_virgl_server.clients;

    while (*cursor) {
        if (*cursor == client) {
            *cursor = client->next;
            client->next = NULL;
            return;
        }
        cursor = &(*cursor)->next;
    }
}

static int virgl_server_attach_texture_locked(struct virgl_resource_entry *entry, uint32_t tex_id) {
    GLenum status;

    if (!entry) {
        return -EINVAL;
    }

    if (entry->framebuffer) {
        glDeleteFramebuffers(1, &entry->framebuffer);
        entry->framebuffer = 0;
    }

    glGenFramebuffers(1, &entry->framebuffer);
    glBindFramebuffer(GL_FRAMEBUFFER, entry->framebuffer);

    switch (entry->target) {
        case PIPE_TEXTURE_2D:
        case PIPE_TEXTURE_1D:
        case PIPE_TEXTURE_RECT:
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex_id, 0);
            break;
        case PIPE_TEXTURE_CUBE:
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X, tex_id, 0);
            break;
        case PIPE_TEXTURE_2D_ARRAY:
        case PIPE_TEXTURE_CUBE_ARRAY:
        case PIPE_TEXTURE_1D_ARRAY:
        case PIPE_TEXTURE_3D:
            glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, tex_id, 0, 0);
            break;
        default:
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            VIRGL_LOGE("Unsupported VirGL texture target=%u for handle=%u", entry->target, entry->handle);
            return -EINVAL;
    }

    status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    if (status != GL_FRAMEBUFFER_COMPLETE) {
        VIRGL_LOGE("Framebuffer incomplete for handle=%u status=0x%x", entry->handle, status);
        return -EINVAL;
    }

    entry->tex_id = tex_id;
    return 0;
}

static int virgl_server_call_flush_frontbuffer_locked(int drawable, int framebuffer) {
    bool attached = false;
    JNIEnv *env = virgl_server_get_env(&attached);
    int ret = 0;

    if (!env || !g_virgl_server.jni.component_ref || !g_virgl_server.jni.flush_frontbuffer) {
        return -EINVAL;
    }

    (*env)->CallVoidMethod(env, g_virgl_server.jni.component_ref, g_virgl_server.jni.flush_frontbuffer, drawable, framebuffer);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        ret = -EINVAL;
    }

    if (attached) {
        (*g_virgl_server.jni.vm)->DetachCurrentThread(g_virgl_server.jni.vm);
    }
    return ret;
}

static void virgl_server_destroy_resources_locked(struct virgl_client *client) {
    struct virgl_resource_entry *entry = client ? client->resources : NULL;
    struct virgl_resource_entry *next;

    if (!client) {
        return;
    }

    if (client->main_context) {
        (void) virgl_server_make_main_context_current_locked(client);
    }

    while (entry) {
        next = entry->next;
        g_virgl_server.api.resource_detach_iov((int) entry->handle, NULL, NULL);
        g_virgl_server.api.ctx_detach_resource((int) client->ctx_id, (int) entry->handle);
        g_virgl_server.api.resource_unref(entry->handle);
        virgl_server_destroy_resource_entry_locked(entry);
        entry = next;
    }

    client->resources = NULL;
}

int virgl_server_create_renderer(struct virgl_client *client, uint32_t length) {
    int ret;

    (void) length;
    if (!client) {
        return -EINVAL;
    }

    pthread_mutex_lock(&g_virgl_server.lock);
    if (client->initialized) {
        pthread_mutex_unlock(&g_virgl_server.lock);
        return -EALREADY;
    }

    ret = virgl_server_load_backend_locked();
    if (ret < 0) {
        pthread_mutex_unlock(&g_virgl_server.lock);
        return ret;
    }

    ret = virgl_server_refresh_shared_egl_locked();
    if (ret < 0) {
        pthread_mutex_unlock(&g_virgl_server.lock);
        return ret;
    }

    if (!g_virgl_server.renderer_initialized) {
        ret = g_virgl_server.api.init(&g_virgl_server, VIRGL_RENDERER_USE_GLES, (struct virgl_renderer_callbacks *) &virgl_server_callbacks);
        if (ret != 0) {
            pthread_mutex_unlock(&g_virgl_server.lock);
            return -EINVAL;
        }
        g_virgl_server.renderer_initialized = true;
    }

    client->ctx_id = g_virgl_server.next_ctx_id++;
    virgl_server_set_current_client_locked(client);
    ret = g_virgl_server.api.context_create(client->ctx_id, 0, NULL);
    virgl_server_clear_current_client_locked(client);
    if (ret != 0) {
        pthread_mutex_unlock(&g_virgl_server.lock);
        return -EINVAL;
    }

    client->initialized = true;
    client->retired_fence_id = 0;
    client->fence_id = 0;
    client->next = g_virgl_server.clients;
    g_virgl_server.clients = client;
    g_virgl_server.active_clients++;
    pthread_mutex_unlock(&g_virgl_server.lock);
    return 0;
}

void virgl_server_destroy_renderer(struct virgl_client *client) {
    if (!client) {
        return;
    }

    pthread_mutex_lock(&g_virgl_server.lock);
    if (!client->initialized) {
        pthread_mutex_unlock(&g_virgl_server.lock);
        return;
    }

    virgl_server_destroy_resources_locked(client);
    g_virgl_server.api.context_destroy(client->ctx_id);
    virgl_server_remove_client_locked(client);
    virgl_server_clear_current_client_locked(client);
    client->initialized = false;
    client->ctx_id = 0;
    client->main_context = NULL;
    client->fence_id = 0;
    client->retired_fence_id = 0;

    if (g_virgl_server.active_clients > 0) {
        g_virgl_server.active_clients--;
    }

    if (g_virgl_server.active_clients == 0 && g_virgl_server.renderer_initialized) {
        g_virgl_server.api.cleanup(&g_virgl_server);
        g_virgl_server.renderer_initialized = false;
        g_virgl_server.shared_display = EGL_NO_DISPLAY;
        g_virgl_server.shared_context = EGL_NO_CONTEXT;
        g_virgl_server.shared_config = NULL;
        virgl_server_unload_backend_locked();
    }
    pthread_mutex_unlock(&g_virgl_server.lock);
}

int virgl_server_send_caps(struct virgl_client *client, uint32_t length) {
    uint32_t header[2];
    uint32_t max_ver = 0;
    uint32_t max_size = 0;
    void *caps = NULL;
    int ret = 0;

    (void) length;
    if (!client || !client->initialized) {
        return -EINVAL;
    }

    pthread_mutex_lock(&g_virgl_server.lock);
    g_virgl_server.api.get_cap_set(2, &max_ver, &max_size);
    if (max_size == 0) {
        pthread_mutex_unlock(&g_virgl_server.lock);
        return -EINVAL;
    }

    caps = calloc(1, max_size);
    if (!caps) {
        pthread_mutex_unlock(&g_virgl_server.lock);
        return -ENOMEM;
    }

    g_virgl_server.api.fill_caps(2, max_ver == 0 ? 1 : max_ver, caps);
    pthread_mutex_unlock(&g_virgl_server.lock);

    header[0] = max_size + 1;
    header[1] = 2;
    if (virgl_block_write(client->fd, header, sizeof(header)) < 0 ||
        virgl_block_write(client->fd, caps, (int) max_size) < 0) {
        ret = -EIO;
    }

    free(caps);
    return ret;
}

int virgl_server_resource_create(struct virgl_client *client, uint32_t length) {
    uint32_t recv_buf[11];
    struct virgl_renderer_resource_create_args args = {0};
    struct virgl_resource_entry *entry;
    int fd = -1;
    int ret = 0;

    (void) length;
    if (!client || !client->initialized) {
        return -EINVAL;
    }

    if (virgl_block_read(client->fd, recv_buf, sizeof(recv_buf)) != (int) sizeof(recv_buf)) {
        return -EIO;
    }

    args.handle = recv_buf[0];
    args.target = recv_buf[1];
    args.format = recv_buf[2];
    args.bind = recv_buf[3];
    args.width = recv_buf[4];
    args.height = recv_buf[5];
    args.depth = recv_buf[6];
    args.array_size = recv_buf[7];
    args.last_level = recv_buf[8];
    args.nr_samples = recv_buf[9];
    args.flags = 0;

    entry = calloc(1, sizeof(*entry));
    if (!entry) {
        return -ENOMEM;
    }
    entry->handle = args.handle;
    entry->target = args.target;
    entry->iov.iov_len = recv_buf[10];

    if (entry->iov.iov_len > 0) {
        fd = virgl_server_new_shm(args.handle, entry->iov.iov_len);
        if (fd < 0) {
            free(entry);
            return fd;
        }

        entry->iov.iov_base = mmap(NULL, entry->iov.iov_len, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
        if (entry->iov.iov_base == MAP_FAILED) {
            ret = -errno;
            close(fd);
            free(entry);
            return ret;
        }
    }

    pthread_mutex_lock(&g_virgl_server.lock);
    if (virgl_server_find_resource(client, args.handle)) {
        pthread_mutex_unlock(&g_virgl_server.lock);
        if (fd >= 0) close(fd);
        virgl_server_destroy_resource_entry_locked(entry);
        return -EEXIST;
    }

    virgl_server_set_current_client_locked(client);
    ret = g_virgl_server.api.resource_create(&args, NULL, 0);
    if (ret == 0) {
        g_virgl_server.api.ctx_attach_resource((int) client->ctx_id, (int) args.handle);
        if (entry->iov.iov_len > 0) {
            ret = g_virgl_server.api.resource_attach_iov((int) args.handle, &entry->iov, 1);
        }
    }
    virgl_server_clear_current_client_locked(client);
    if (ret == 0) {
        entry->next = client->resources;
        client->resources = entry;
    }
    pthread_mutex_unlock(&g_virgl_server.lock);

    if (ret != 0) {
        if (fd >= 0) close(fd);
        virgl_server_destroy_resource_entry_locked(entry);
        return -EINVAL;
    }

    if (fd >= 0) {
        ret = virgl_server_send_fd(client->fd, fd);
        close(fd);
        if (ret < 0) {
            return ret;
        }
    }

    return 0;
}

int virgl_server_resource_destroy(struct virgl_client *client, uint32_t length) {
    uint32_t recv_buf[1];
    struct virgl_resource_entry *entry;

    (void) length;
    if (!client || !client->initialized) {
        return -EINVAL;
    }

    if (virgl_block_read(client->fd, recv_buf, sizeof(recv_buf)) != (int) sizeof(recv_buf)) {
        return -EIO;
    }

    pthread_mutex_lock(&g_virgl_server.lock);
    entry = virgl_server_find_resource(client, recv_buf[0]);
    if (!entry) {
        pthread_mutex_unlock(&g_virgl_server.lock);
        return -ESRCH;
    }

    if (client->main_context) {
        (void) virgl_server_make_main_context_current_locked(client);
    }
    g_virgl_server.api.resource_detach_iov((int) entry->handle, NULL, NULL);
    g_virgl_server.api.ctx_detach_resource((int) client->ctx_id, (int) entry->handle);
    g_virgl_server.api.resource_unref(entry->handle);
    virgl_server_remove_resource_locked(client, entry);
    virgl_server_destroy_resource_entry_locked(entry);
    pthread_mutex_unlock(&g_virgl_server.lock);
    return 0;
}

static int virgl_server_transfer_common(struct virgl_client *client, bool from_host) {
    uint32_t recv_buf[10];
    struct virgl_box box;
    struct virgl_resource_entry *entry;
    int ret;

    if (virgl_block_read(client->fd, recv_buf, sizeof(recv_buf)) != (int) sizeof(recv_buf)) {
        return -EIO;
    }

    box.x = recv_buf[2];
    box.y = recv_buf[3];
    box.z = recv_buf[4];
    box.w = recv_buf[5];
    box.h = recv_buf[6];
    box.d = recv_buf[7];

    pthread_mutex_lock(&g_virgl_server.lock);
    entry = virgl_server_find_resource(client, recv_buf[0]);
    if (!entry || recv_buf[9] >= entry->iov.iov_len) {
        pthread_mutex_unlock(&g_virgl_server.lock);
        return -ESRCH;
    }

    virgl_server_set_current_client_locked(client);
    if (from_host) {
        ret = g_virgl_server.api.transfer_read_iov(
                recv_buf[0], client->ctx_id, recv_buf[1], 0, 0, &box, recv_buf[9], &entry->iov, 1);
    } else {
        ret = g_virgl_server.api.transfer_write_iov(
                recv_buf[0], client->ctx_id, (int) recv_buf[1], 0, 0, &box, recv_buf[9], &entry->iov, 1);
    }
    virgl_server_clear_current_client_locked(client);
    pthread_mutex_unlock(&g_virgl_server.lock);
    return ret == 0 ? 0 : -EINVAL;
}

int virgl_server_transfer_get(struct virgl_client *client, uint32_t length) {
    (void) length;
    if (!client || !client->initialized) {
        return -EINVAL;
    }
    return virgl_server_transfer_common(client, true);
}

int virgl_server_transfer_put(struct virgl_client *client, uint32_t length) {
    (void) length;
    if (!client || !client->initialized) {
        return -EINVAL;
    }
    return virgl_server_transfer_common(client, false);
}

int virgl_server_create_fence(struct virgl_client *client) {
    int ret;

    if (!client) {
        return -EINVAL;
    }

    ret = g_virgl_server.api.create_fence((int) ++client->fence_id, client->ctx_id);
    return ret == 0 ? 0 : -EINVAL;
}

int virgl_server_submit_cmd(struct virgl_client *client, uint32_t length) {
    uint32_t *buffer;
    int buffer_len = (int) (length * sizeof(uint32_t));
    int ret;

    if (!client || !client->initialized) {
        return -EINVAL;
    }

    buffer = malloc((size_t) buffer_len);
    if (!buffer) {
        return -ENOMEM;
    }

    if (virgl_block_read(client->fd, buffer, buffer_len) != buffer_len) {
        free(buffer);
        return -EIO;
    }

    pthread_mutex_lock(&g_virgl_server.lock);
    virgl_server_set_current_client_locked(client);
    ret = g_virgl_server.api.submit_cmd(buffer, (int) client->ctx_id, (int) length);
    if (ret == 0) {
        ret = virgl_server_create_fence(client);
    }
    virgl_server_clear_current_client_locked(client);
    pthread_mutex_unlock(&g_virgl_server.lock);
    free(buffer);
    return ret == 0 ? 0 : -EINVAL;
}

int virgl_server_resource_busy_wait(struct virgl_client *client, uint32_t length) {
    uint32_t recv_buf[2];
    uint32_t send_buf[3];
    bool busy;

    (void) length;
    if (!client || !client->initialized) {
        return -EINVAL;
    }

    if (virgl_block_read(client->fd, recv_buf, sizeof(recv_buf)) != (int) sizeof(recv_buf)) {
        return -EIO;
    }

    pthread_mutex_lock(&g_virgl_server.lock);
    do {
        busy = client->retired_fence_id != client->fence_id;
        if (!busy || !(recv_buf[1] & VCMD_BUSY_WAIT_FLAG_WAIT)) {
            break;
        }
        g_virgl_server.api.poll();
        sched_yield();
    } while (1);
    pthread_mutex_unlock(&g_virgl_server.lock);

    send_buf[0] = 1;
    send_buf[1] = VCMD_RESOURCE_BUSY_WAIT;
    send_buf[2] = busy ? 1 : 0;
    return virgl_block_write(client->fd, send_buf, sizeof(send_buf)) < 0 ? -EIO : 0;
}

int virgl_server_flush_frontbuffer(struct virgl_client *client, uint32_t length) {
    uint32_t recv_buf[2];
    struct virgl_resource_entry *entry;
    struct virgl_renderer_resource_info info = {0};
    int ret;

    (void) length;
    if (!client || !client->initialized) {
        return -EINVAL;
    }

    if (virgl_block_read(client->fd, recv_buf, sizeof(recv_buf)) != (int) sizeof(recv_buf)) {
        return -EIO;
    }

    pthread_mutex_lock(&g_virgl_server.lock);
    entry = virgl_server_find_resource(client, recv_buf[0]);
    if (!entry) {
        pthread_mutex_unlock(&g_virgl_server.lock);
        return -ESRCH;
    }

    ret = virgl_server_make_main_context_current_locked(client);
    if (ret == 0) {
        ret = g_virgl_server.api.resource_get_info((int) entry->handle, &info);
    }
    if (ret == 0 && info.tex_id == 0) {
        ret = -EINVAL;
    }
    if (ret == 0 && (entry->framebuffer == 0 || entry->tex_id != info.tex_id)) {
        ret = virgl_server_attach_texture_locked(entry, info.tex_id);
    }
    if (ret == 0) {
        glFlush();
        ret = virgl_server_call_flush_frontbuffer_locked((int) recv_buf[1], (int) entry->framebuffer);
    }
    pthread_mutex_unlock(&g_virgl_server.lock);
    return ret;
}
