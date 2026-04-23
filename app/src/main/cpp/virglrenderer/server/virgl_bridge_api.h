#ifndef VIRGL_BRIDGE_API_H
#define VIRGL_BRIDGE_API_H

#include <stdbool.h>
#include <stdint.h>
#include <sys/uio.h>

struct virgl_box {
    uint32_t x;
    uint32_t y;
    uint32_t z;
    uint32_t w;
    uint32_t h;
    uint32_t d;
};

typedef void *virgl_renderer_gl_context;

struct virgl_renderer_gl_ctx_param {
    int version;
    bool shared;
    int major_ver;
    int minor_ver;
    int compat_ctx;
};

#define VIRGL_RENDERER_CALLBACKS_VERSION 4

struct virgl_renderer_callbacks {
    int version;
    void (*write_fence)(void *cookie, uint32_t fence);
    virgl_renderer_gl_context (*create_gl_context)(void *cookie, int scanout_idx, struct virgl_renderer_gl_ctx_param *param);
    void (*destroy_gl_context)(void *cookie, virgl_renderer_gl_context ctx);
    int (*make_current)(void *cookie, int scanout_idx, virgl_renderer_gl_context ctx);
    int (*get_drm_fd)(void *cookie);
    void (*write_context_fence)(void *cookie, uint32_t ctx_id, uint32_t ring_idx, uint64_t fence_id);
    int (*get_server_fd)(void *cookie, uint32_t version);
    void *(*get_egl_display)(void *cookie);
};

#define VIRGL_RENDERER_USE_GLES (1 << 4)

struct virgl_renderer_resource_create_args {
    uint32_t handle;
    uint32_t target;
    uint32_t format;
    uint32_t bind;
    uint32_t width;
    uint32_t height;
    uint32_t depth;
    uint32_t array_size;
    uint32_t last_level;
    uint32_t nr_samples;
    uint32_t flags;
};

struct virgl_renderer_resource_info {
    uint32_t handle;
    uint32_t virgl_format;
    uint32_t width;
    uint32_t height;
    uint32_t depth;
    uint32_t flags;
    uint32_t tex_id;
    uint32_t stride;
    int drm_fourcc;
    int fd;
};

typedef int (*pfn_virgl_renderer_init)(void *cookie, int flags, struct virgl_renderer_callbacks *cb);
typedef void (*pfn_virgl_renderer_poll)(void);
typedef int (*pfn_virgl_renderer_context_create)(uint32_t handle, uint32_t nlen, const char *name);
typedef void (*pfn_virgl_renderer_context_destroy)(uint32_t handle);
typedef int (*pfn_virgl_renderer_resource_create)(struct virgl_renderer_resource_create_args *args, struct iovec *iov, uint32_t num_iovs);
typedef void (*pfn_virgl_renderer_resource_unref)(uint32_t res_handle);
typedef void (*pfn_virgl_renderer_get_cap_set)(uint32_t set, uint32_t *max_ver, uint32_t *max_size);
typedef void (*pfn_virgl_renderer_fill_caps)(uint32_t set, uint32_t version, void *caps);
typedef int (*pfn_virgl_renderer_resource_attach_iov)(int res_handle, struct iovec *iov, int num_iovs);
typedef void (*pfn_virgl_renderer_resource_detach_iov)(int res_handle, struct iovec **iov, int *num_iovs);
typedef int (*pfn_virgl_renderer_create_fence)(int client_fence_id, uint32_t ctx_id);
typedef void (*pfn_virgl_renderer_ctx_attach_resource)(int ctx_id, int res_handle);
typedef void (*pfn_virgl_renderer_ctx_detach_resource)(int ctx_id, int res_handle);
typedef int (*pfn_virgl_renderer_submit_cmd)(void *buffer, int ctx_id, int ndw);
typedef int (*pfn_virgl_renderer_transfer_read_iov)(uint32_t handle, uint32_t ctx_id, uint32_t level, uint32_t stride, uint32_t layer_stride, struct virgl_box *box, uint64_t offset, struct iovec *iov, int iov_cnt);
typedef int (*pfn_virgl_renderer_transfer_write_iov)(uint32_t handle, uint32_t ctx_id, int level, uint32_t stride, uint32_t layer_stride, struct virgl_box *box, uint64_t offset, struct iovec *iov, unsigned int iov_cnt);
typedef int (*pfn_virgl_renderer_resource_get_info)(int res_handle, struct virgl_renderer_resource_info *info);
typedef void (*pfn_virgl_renderer_cleanup)(void *cookie);

struct virgl_bridge_backend_api {
    pfn_virgl_renderer_init init;
    pfn_virgl_renderer_poll poll;
    pfn_virgl_renderer_context_create context_create;
    pfn_virgl_renderer_context_destroy context_destroy;
    pfn_virgl_renderer_resource_create resource_create;
    pfn_virgl_renderer_resource_unref resource_unref;
    pfn_virgl_renderer_get_cap_set get_cap_set;
    pfn_virgl_renderer_fill_caps fill_caps;
    pfn_virgl_renderer_resource_attach_iov resource_attach_iov;
    pfn_virgl_renderer_resource_detach_iov resource_detach_iov;
    pfn_virgl_renderer_create_fence create_fence;
    pfn_virgl_renderer_ctx_attach_resource ctx_attach_resource;
    pfn_virgl_renderer_ctx_detach_resource ctx_detach_resource;
    pfn_virgl_renderer_submit_cmd submit_cmd;
    pfn_virgl_renderer_transfer_read_iov transfer_read_iov;
    pfn_virgl_renderer_transfer_write_iov transfer_write_iov;
    pfn_virgl_renderer_resource_get_info resource_get_info;
    pfn_virgl_renderer_cleanup cleanup;
};

#endif
