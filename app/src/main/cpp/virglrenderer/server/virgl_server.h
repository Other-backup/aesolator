#ifndef VIRGL_SERVER_H
#define VIRGL_SERVER_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <jni.h>
#include <pthread.h>
#include <sys/uio.h>

#include <android/log.h>

#include "virgl_bridge_api.h"

#define VIRGL_LOG_TAG "VirGLBridge"
#define VIRGL_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, VIRGL_LOG_TAG, __VA_ARGS__)
#define VIRGL_LOGW(...) __android_log_print(ANDROID_LOG_WARN, VIRGL_LOG_TAG, __VA_ARGS__)
#define VIRGL_LOGI(...) __android_log_print(ANDROID_LOG_INFO, VIRGL_LOG_TAG, __VA_ARGS__)

struct virgl_resource_entry {
    uint32_t handle;
    uint32_t target;
    struct iovec iov;
    GLuint framebuffer;
    GLuint tex_id;
    struct virgl_resource_entry *next;
};

struct virgl_client {
    int fd;
    uint32_t ctx_id;
    uint32_t fence_id;
    uint32_t retired_fence_id;
    bool initialized;
    virgl_renderer_gl_context main_context;
    struct virgl_resource_entry *resources;
    struct virgl_client *next;
};

struct virgl_component_jni {
    JavaVM *vm;
    jobject component_ref;
    jmethodID kill_connection;
    jmethodID flush_frontbuffer;
};

struct virgl_server_state {
    pthread_mutex_t lock;
    void *backend_handle;
    struct virgl_bridge_backend_api api;
    bool backend_loaded;
    bool renderer_initialized;
    uint32_t next_ctx_id;
    int active_clients;
    EGLDisplay shared_display;
    EGLConfig shared_config;
    EGLContext shared_context;
    struct virgl_component_jni jni;
    struct virgl_client *clients;
    struct virgl_client *current_client;
};

extern struct virgl_server_state g_virgl_server;

int virgl_server_create_renderer(struct virgl_client *client, uint32_t length);
int virgl_server_send_caps(struct virgl_client *client, uint32_t length);
int virgl_server_resource_create(struct virgl_client *client, uint32_t length);
int virgl_server_resource_destroy(struct virgl_client *client, uint32_t length);
int virgl_server_transfer_get(struct virgl_client *client, uint32_t length);
int virgl_server_transfer_put(struct virgl_client *client, uint32_t length);
int virgl_server_submit_cmd(struct virgl_client *client, uint32_t length);
int virgl_server_resource_busy_wait(struct virgl_client *client, uint32_t length);
int virgl_server_flush_frontbuffer(struct virgl_client *client, uint32_t length);

int virgl_block_read(int fd, void *buf, int size);
int virgl_block_write(int fd, const void *buf, int size);
int virgl_server_send_fd(int sock_fd, int fd);
void virgl_server_destroy_renderer(struct virgl_client *client);
void virgl_server_destroy_client(struct virgl_client **client_ptr);
void virgl_server_kill_connection(struct virgl_client *client);
int virgl_server_create_fence(struct virgl_client *client);
void virgl_server_set_current_client_locked(struct virgl_client *client);
void virgl_server_clear_current_client_locked(struct virgl_client *client);
struct virgl_client *virgl_server_find_client_by_ctx_locked(uint32_t ctx_id);
struct virgl_resource_entry *virgl_server_find_resource(struct virgl_client *client, uint32_t handle);
int virgl_server_ensure_jni(JNIEnv *env, jobject obj);
JNIEnv *virgl_server_get_env(bool *attached);

#endif
