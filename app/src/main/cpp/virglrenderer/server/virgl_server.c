#include "virgl_server.h"

#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#include "virgl_server_protocol.h"

struct virgl_server_state g_virgl_server = {
        .lock = PTHREAD_MUTEX_INITIALIZER,
        .next_ctx_id = 1,
        .shared_display = EGL_NO_DISPLAY,
        .shared_context = EGL_NO_CONTEXT,
};

static struct virgl_client *virgl_server_handle_new_connection(int fd) {
    struct virgl_client *client = calloc(1, sizeof(*client));
    if (!client) {
        return NULL;
    }
    client->fd = fd;
    return client;
}

static void virgl_server_unregister_component_locked(JNIEnv *env) {
    if (g_virgl_server.jni.component_ref) {
        (*env)->DeleteGlobalRef(env, g_virgl_server.jni.component_ref);
        g_virgl_server.jni.component_ref = NULL;
    }
    g_virgl_server.jni.kill_connection = NULL;
    g_virgl_server.jni.flush_frontbuffer = NULL;
}

int virgl_server_ensure_jni(JNIEnv *env, jobject obj) {
    jclass cls;
    jobject global_ref;

    if (!env || !obj) {
        return -EINVAL;
    }

    pthread_mutex_lock(&g_virgl_server.lock);
    if ((*env)->GetJavaVM(env, &g_virgl_server.jni.vm) != JNI_OK) {
        pthread_mutex_unlock(&g_virgl_server.lock);
        return -EINVAL;
    }

    cls = (*env)->GetObjectClass(env, obj);
    if (!cls) {
        pthread_mutex_unlock(&g_virgl_server.lock);
        return -EINVAL;
    }

    g_virgl_server.jni.kill_connection = (*env)->GetMethodID(env, cls, "killConnection", "(I)V");
    g_virgl_server.jni.flush_frontbuffer = (*env)->GetMethodID(env, cls, "flushFrontbuffer", "(II)V");
    if (!g_virgl_server.jni.kill_connection || !g_virgl_server.jni.flush_frontbuffer) {
        (*env)->DeleteLocalRef(env, cls);
        pthread_mutex_unlock(&g_virgl_server.lock);
        return -EINVAL;
    }

    if (g_virgl_server.jni.component_ref &&
        (*env)->IsSameObject(env, g_virgl_server.jni.component_ref, obj)) {
        (*env)->DeleteLocalRef(env, cls);
        pthread_mutex_unlock(&g_virgl_server.lock);
        return 0;
    }

    global_ref = (*env)->NewGlobalRef(env, obj);
    if (!global_ref) {
        (*env)->DeleteLocalRef(env, cls);
        pthread_mutex_unlock(&g_virgl_server.lock);
        return -ENOMEM;
    }

    virgl_server_unregister_component_locked(env);
    g_virgl_server.jni.component_ref = global_ref;
    (*env)->DeleteLocalRef(env, cls);
    pthread_mutex_unlock(&g_virgl_server.lock);
    return 0;
}

JNIEnv *virgl_server_get_env(bool *attached) {
    JNIEnv *env = NULL;
    JavaVM *vm = g_virgl_server.jni.vm;

    if (attached) {
        *attached = false;
    }
    if (!vm) {
        return NULL;
    }

    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) == JNI_OK) {
        return env;
    }

    if ((*vm)->AttachCurrentThread(vm, &env, NULL) != JNI_OK) {
        return NULL;
    }

    if (attached) {
        *attached = true;
    }
    return env;
}

void virgl_server_kill_connection(struct virgl_client *client) {
    bool attached = false;
    JNIEnv *env = virgl_server_get_env(&attached);
    jobject component_ref = NULL;
    jmethodID kill_connection = NULL;

    if (!client || !env) {
        return;
    }

    pthread_mutex_lock(&g_virgl_server.lock);
    component_ref = g_virgl_server.jni.component_ref;
    kill_connection = g_virgl_server.jni.kill_connection;
    pthread_mutex_unlock(&g_virgl_server.lock);

    if (component_ref && kill_connection) {
        (*env)->CallVoidMethod(env, component_ref, kill_connection, client->fd);
    }

    if (attached) {
        (*g_virgl_server.jni.vm)->DetachCurrentThread(g_virgl_server.jni.vm);
    }
}

int virgl_block_write(int fd, const void *buf, int size) {
    const char *ptr = buf;
    int left = size;

    while (left > 0) {
        int ret = write(fd, ptr, left);
        if (ret < 0) {
            return -errno;
        }
        left -= ret;
        ptr += ret;
    }

    return size;
}

int virgl_block_read(int fd, void *buf, int size) {
    char *ptr = buf;
    int left = size;

    while (left > 0) {
        int ret = read(fd, ptr, left);
        if (ret <= 0) {
            return ret == -1 ? -errno : 0;
        }
        left -= ret;
        ptr += ret;
    }

    return size;
}

int virgl_server_send_fd(int sock_fd, int fd) {
    struct iovec iov;
    struct msghdr msg = {0};
    char control[CMSG_SPACE(sizeof(int))];
    char payload = 0;
    struct cmsghdr *cmsg;

    memset(control, 0, sizeof(control));
    iov.iov_base = &payload;
    iov.iov_len = sizeof(payload);

    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;
    msg.msg_control = control;
    msg.msg_controllen = sizeof(control);

    cmsg = CMSG_FIRSTHDR(&msg);
    cmsg->cmsg_level = SOL_SOCKET;
    cmsg->cmsg_type = SCM_RIGHTS;
    cmsg->cmsg_len = CMSG_LEN(sizeof(int));
    *((int *) CMSG_DATA(cmsg)) = fd;

    if (sendmsg(sock_fd, &msg, 0) < 0) {
        return -errno;
    }

    return 0;
}

void virgl_server_destroy_client(struct virgl_client **client_ptr) {
    if (!client_ptr || !*client_ptr) {
        return;
    }

    virgl_server_destroy_renderer(*client_ptr);
    free(*client_ptr);
    *client_ptr = NULL;
}

static void virgl_server_handle_request(struct virgl_client *client) {
    uint32_t header[2];
    int ret;

    ret = virgl_block_read(client->fd, &header, sizeof(header));
    if (ret < 0 || ret < (int) sizeof(header)) {
        virgl_server_kill_connection(client);
        return;
    }

    if (!client->initialized && header[1] != VCMD_CREATE_RENDERER) {
        virgl_server_kill_connection(client);
        return;
    }

    switch (header[1]) {
        case VCMD_CREATE_RENDERER:
            ret = virgl_server_create_renderer(client, header[0]);
            break;
        case VCMD_GET_CAPS:
            ret = virgl_server_send_caps(client, header[0]);
            break;
        case VCMD_RESOURCE_CREATE:
            ret = virgl_server_resource_create(client, header[0]);
            break;
        case VCMD_RESOURCE_DESTROY:
            ret = virgl_server_resource_destroy(client, header[0]);
            break;
        case VCMD_TRANSFER_GET:
            ret = virgl_server_transfer_get(client, header[0]);
            break;
        case VCMD_TRANSFER_PUT:
            ret = virgl_server_transfer_put(client, header[0]);
            break;
        case VCMD_SUBMIT_CMD:
            ret = virgl_server_submit_cmd(client, header[0]);
            break;
        case VCMD_RESOURCE_BUSY_WAIT:
            ret = virgl_server_resource_busy_wait(client, header[0]);
            break;
        case VCMD_FLUSH_FRONTBUFFER:
            ret = virgl_server_flush_frontbuffer(client, header[0]);
            break;
        default:
            ret = -EINVAL;
            break;
    }

    if (ret < 0) {
        VIRGL_LOGE("request=%u fd=%d failed ret=%d", header[1], client->fd, ret);
        virgl_server_kill_connection(client);
    }
}

JNIEXPORT jlong JNICALL
Java_com_winlator_cmod_xenvironment_components_VirGLRendererComponent_handleNewConnection(JNIEnv *env, jobject obj, jint fd) {
    struct virgl_client *client;

    if (virgl_server_ensure_jni(env, obj) < 0) {
        return 0;
    }

    client = virgl_server_handle_new_connection(fd);
    return (jlong) (intptr_t) client;
}

JNIEXPORT void JNICALL
Java_com_winlator_cmod_xenvironment_components_VirGLRendererComponent_handleRequest(JNIEnv *env, jobject obj, jlong client_ptr) {
    (void) env;
    (void) obj;
    virgl_server_handle_request((struct virgl_client *) (intptr_t) client_ptr);
}

JNIEXPORT void JNICALL
Java_com_winlator_cmod_xenvironment_components_VirGLRendererComponent_destroyClient(JNIEnv *env, jobject obj, jlong client_ptr) {
    struct virgl_client *client = (struct virgl_client *) (intptr_t) client_ptr;
    (void) env;
    (void) obj;
    virgl_server_destroy_client(&client);
}

JNIEXPORT void JNICALL
Java_com_winlator_cmod_xenvironment_components_VirGLRendererComponent_destroyRenderer(JNIEnv *env, jobject obj, jlong client_ptr) {
    struct virgl_client *client = (struct virgl_client *) (intptr_t) client_ptr;
    (void) env;
    (void) obj;
    virgl_server_destroy_renderer(client);
}

JNIEXPORT jlong JNICALL
Java_com_winlator_cmod_xenvironment_components_VirGLRendererComponent_getCurrentEGLContextPtr(JNIEnv *env, jobject obj) {
    EGLContext context = eglGetCurrentContext();
    (void) env;
    (void) obj;
    return context == EGL_NO_CONTEXT ? 0 : (jlong) (intptr_t) context;
}
