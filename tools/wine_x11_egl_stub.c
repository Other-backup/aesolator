/*
 * Ae.solator Wine X11 EGL compatibility shim.
 *
 * Some donor Winlator/Proton runtimes ship a win32u EGL path without Wine's
 * WINE_USE_EGL gate. On Android X11 shells that can make desktop window
 * materialization fail before the X11/GLX path becomes usable. This tiny EGL
 * surface exports the symbols those runtimes probe and returns one OpenGL-capable
 * pixel format, keeping the desktop path alive without replacing the real host
 * EGL libraries used by non-Wine Android components.
 */
#include <stdint.h>
#include <string.h>

typedef void *EGLDisplay;
typedef void *EGLConfig;
typedef void *EGLContext;
typedef void *EGLSurface;
typedef void *EGLSync;
typedef void *EGLImage;
typedef void *EGLClientBuffer;
typedef void *EGLNativeDisplayType;
typedef void *EGLNativePixmapType;
typedef void *EGLNativeWindowType;
typedef int EGLBoolean;
typedef int EGLenum;
typedef int EGLint;
typedef intptr_t EGLAttrib;
typedef uint64_t EGLTime;

#define EGL_FALSE 0
#define EGL_TRUE 1
#define EGL_SUCCESS 0x3000
#define EGL_BAD_PARAMETER 0x300c
#define EGL_OPENGL_API 0x30a2
#define EGL_OPENGL_ES_API 0x30a0
#define EGL_CONDITION_SATISFIED 0x30f6
#define EGL_CONFIG_ID 0x3028
#define EGL_SURFACE_TYPE 0x3033
#define EGL_RENDERABLE_TYPE 0x3040
#define EGL_NATIVE_VISUAL_ID 0x302e
#define EGL_NATIVE_RENDERABLE 0x302d
#define EGL_COLOR_BUFFER_TYPE 0x303f
#define EGL_RED_SIZE 0x3024
#define EGL_GREEN_SIZE 0x3023
#define EGL_BLUE_SIZE 0x3022
#define EGL_ALPHA_SIZE 0x3021
#define EGL_DEPTH_SIZE 0x3025
#define EGL_STENCIL_SIZE 0x3026
#define EGL_TRANSPARENT_TYPE 0x3034
#define EGL_TRANSPARENT_RED_VALUE 0x3037
#define EGL_TRANSPARENT_GREEN_VALUE 0x3036
#define EGL_TRANSPARENT_BLUE_VALUE 0x3035
#define EGL_WINDOW_BIT 0x0004
#define EGL_PBUFFER_BIT 0x0001
#define EGL_PIXMAP_BIT 0x0002
#define EGL_OPENGL_BIT 0x0008
#define EGL_RGB_BUFFER 0x308e
#define EGL_NONE 0x3038
#define EGL_EXTENSIONS 0x3055
#define EGL_VENDOR 0x3053
#define EGL_VERSION 0x3054
#define EGL_CLIENT_APIS 0x308d

static EGLint last_error = EGL_SUCCESS;
static int config_handle;
static int context_handle;
static int surface_handle;
static int sync_handle;

static void set_config_attrib(EGLint *value, EGLint attrib)
{
    switch (attrib)
    {
    case EGL_CONFIG_ID: *value = 1; break;
    case EGL_SURFACE_TYPE: *value = EGL_WINDOW_BIT | EGL_PBUFFER_BIT | EGL_PIXMAP_BIT; break;
    case EGL_RENDERABLE_TYPE: *value = EGL_OPENGL_BIT; break;
    case EGL_NATIVE_VISUAL_ID: *value = 0; break;
    case EGL_NATIVE_RENDERABLE: *value = EGL_FALSE; break;
    case EGL_COLOR_BUFFER_TYPE: *value = EGL_RGB_BUFFER; break;
    case EGL_RED_SIZE: *value = 8; break;
    case EGL_GREEN_SIZE: *value = 8; break;
    case EGL_BLUE_SIZE: *value = 8; break;
    case EGL_ALPHA_SIZE: *value = 8; break;
    case EGL_DEPTH_SIZE: *value = 24; break;
    case EGL_STENCIL_SIZE: *value = 8; break;
    case EGL_TRANSPARENT_TYPE: *value = EGL_NONE; break;
    case EGL_TRANSPARENT_RED_VALUE:
    case EGL_TRANSPARENT_GREEN_VALUE:
    case EGL_TRANSPARENT_BLUE_VALUE: *value = 0; break;
    default: *value = 0; break;
    }
}

EGLBoolean eglBindAPI(EGLenum api) { return api == EGL_OPENGL_API || api == EGL_OPENGL_ES_API; }
EGLBoolean eglBindTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer) { return EGL_TRUE; }
EGLBoolean eglChooseConfig(EGLDisplay dpy, const EGLint *attrib_list, EGLConfig *configs, EGLint config_size, EGLint *num_config)
{
    if (num_config) *num_config = 1;
    if (configs && config_size > 0) configs[0] = (EGLConfig)&config_handle;
    return EGL_TRUE;
}
EGLint eglClientWaitSync(EGLDisplay dpy, EGLSync sync, EGLint flags, EGLTime timeout) { return EGL_CONDITION_SATISFIED; }
EGLBoolean eglCopyBuffers(EGLDisplay dpy, EGLSurface surface, EGLNativePixmapType target) { return EGL_TRUE; }
EGLContext eglCreateContext(EGLDisplay dpy, EGLConfig config, EGLContext share_context, const EGLint *attrib_list) { return (EGLContext)&context_handle; }
EGLImage eglCreateImage(EGLDisplay dpy, EGLContext ctx, EGLenum target, EGLClientBuffer buffer, const EGLAttrib *attrib_list) { return (EGLImage)&surface_handle; }
EGLSurface eglCreatePbufferFromClientBuffer(EGLDisplay dpy, EGLenum buftype, EGLClientBuffer buffer, EGLConfig config, const EGLint *attrib_list) { return (EGLSurface)&surface_handle; }
EGLSurface eglCreatePbufferSurface(EGLDisplay dpy, EGLConfig config, const EGLint *attrib_list) { return (EGLSurface)&surface_handle; }
EGLSurface eglCreatePixmapSurface(EGLDisplay dpy, EGLConfig config, EGLNativePixmapType pixmap, const EGLint *attrib_list) { return (EGLSurface)&surface_handle; }
EGLSurface eglCreatePlatformPixmapSurface(EGLDisplay dpy, EGLConfig config, void *native_pixmap, const EGLAttrib *attrib_list) { return (EGLSurface)&surface_handle; }
EGLSurface eglCreatePlatformWindowSurface(EGLDisplay dpy, EGLConfig config, void *native_window, const EGLAttrib *attrib_list) { return (EGLSurface)&surface_handle; }
EGLSync eglCreateSync(EGLDisplay dpy, EGLenum type, const EGLAttrib *attrib_list) { return (EGLSync)&sync_handle; }
EGLSurface eglCreateWindowSurface(EGLDisplay dpy, EGLConfig config, EGLNativeWindowType win, const EGLint *attrib_list) { return (EGLSurface)&surface_handle; }
EGLBoolean eglDestroyContext(EGLDisplay dpy, EGLContext context) { return EGL_TRUE; }
EGLBoolean eglDestroyImage(EGLDisplay dpy, EGLImage image) { return EGL_TRUE; }
EGLBoolean eglDestroySurface(EGLDisplay dpy, EGLSurface surface) { return EGL_TRUE; }
EGLBoolean eglDestroySync(EGLDisplay dpy, EGLSync sync) { return EGL_TRUE; }
EGLBoolean eglGetConfigAttrib(EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint *value)
{
    if (!value)
    {
        last_error = EGL_BAD_PARAMETER;
        return EGL_FALSE;
    }
    set_config_attrib(value, attribute);
    last_error = EGL_SUCCESS;
    return EGL_TRUE;
}
EGLBoolean eglGetConfigs(EGLDisplay dpy, EGLConfig *configs, EGLint config_size, EGLint *num_config)
{
    if (num_config) *num_config = 1;
    if (configs && config_size > 0) configs[0] = (EGLConfig)&config_handle;
    return EGL_TRUE;
}
EGLContext eglGetCurrentContext(void) { return (EGLContext)&context_handle; }
EGLDisplay eglGetCurrentDisplay(void) { return (EGLDisplay)1; }
EGLSurface eglGetCurrentSurface(EGLint readdraw) { return (EGLSurface)&surface_handle; }
EGLDisplay eglGetDisplay(EGLNativeDisplayType display_id) { return (EGLDisplay)1; }
EGLint eglGetError(void)
{
    EGLint err = last_error;
    last_error = EGL_SUCCESS;
    return err;
}
EGLDisplay eglGetPlatformDisplay(EGLenum platform, void *native_display, const EGLAttrib *attrib_list) { return (EGLDisplay)1; }
EGLBoolean eglGetSyncAttrib(EGLDisplay dpy, EGLSync sync, EGLint attribute, EGLAttrib *value)
{
    if (value) *value = 0;
    return EGL_TRUE;
}
EGLBoolean eglInitialize(EGLDisplay dpy, EGLint *major, EGLint *minor)
{
    if (major) *major = 1;
    if (minor) *minor = 5;
    return EGL_TRUE;
}
EGLBoolean eglMakeCurrent(EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext context) { return EGL_TRUE; }
EGLenum eglQueryAPI(void) { return EGL_OPENGL_API; }
EGLBoolean eglQueryContext(EGLDisplay dpy, EGLContext ctx, EGLint attribute, EGLint *value)
{
    if (value) *value = 0;
    return EGL_TRUE;
}
const char *eglQueryString(EGLDisplay dpy, EGLint name)
{
    if (name == EGL_EXTENSIONS) return "EGL_KHR_client_get_all_proc_addresses EGL_EXT_platform_base EGL_KHR_create_context EGL_KHR_create_context_no_error EGL_KHR_no_config_context";
    if (name == EGL_VENDOR) return "Ae.solator Wine X11 EGL compatibility shim";
    if (name == EGL_VERSION) return "1.5 Ae.solator";
    if (name == EGL_CLIENT_APIS) return "OpenGL OpenGL_ES";
    return "";
}
EGLBoolean eglQuerySurface(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint *value)
{
    if (value) *value = 1;
    return EGL_TRUE;
}
EGLBoolean eglReleaseTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer) { return EGL_TRUE; }
EGLBoolean eglReleaseThread(void) { return EGL_TRUE; }
EGLBoolean eglSurfaceAttrib(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint value) { return EGL_TRUE; }
EGLBoolean eglSwapBuffers(EGLDisplay dpy, EGLSurface surface) { return EGL_TRUE; }
EGLBoolean eglSwapInterval(EGLDisplay dpy, EGLint interval) { return EGL_TRUE; }
EGLBoolean eglTerminate(EGLDisplay dpy) { return EGL_TRUE; }
EGLBoolean eglWaitClient(void) { return EGL_TRUE; }
EGLBoolean eglWaitGL(void) { return EGL_TRUE; }
EGLBoolean eglWaitNative(EGLint engine) { return EGL_TRUE; }
EGLBoolean eglWaitSync(EGLDisplay dpy, EGLSync sync, EGLint flags) { return EGL_TRUE; }

void *eglGetProcAddress(const char *name)
{
#define EGL_STUB(name_) if (!strcmp(name, #name_)) return (void *)name_
    EGL_STUB(eglBindAPI); EGL_STUB(eglBindTexImage); EGL_STUB(eglChooseConfig); EGL_STUB(eglClientWaitSync);
    EGL_STUB(eglCopyBuffers); EGL_STUB(eglCreateContext); EGL_STUB(eglCreateImage); EGL_STUB(eglCreatePbufferFromClientBuffer);
    EGL_STUB(eglCreatePbufferSurface); EGL_STUB(eglCreatePixmapSurface); EGL_STUB(eglCreatePlatformPixmapSurface);
    EGL_STUB(eglCreatePlatformWindowSurface); EGL_STUB(eglCreateSync); EGL_STUB(eglCreateWindowSurface);
    EGL_STUB(eglDestroyContext); EGL_STUB(eglDestroyImage); EGL_STUB(eglDestroySurface); EGL_STUB(eglDestroySync);
    EGL_STUB(eglGetConfigAttrib); EGL_STUB(eglGetConfigs); EGL_STUB(eglGetCurrentContext); EGL_STUB(eglGetCurrentDisplay);
    EGL_STUB(eglGetCurrentSurface); EGL_STUB(eglGetDisplay); EGL_STUB(eglGetError); EGL_STUB(eglGetPlatformDisplay);
    EGL_STUB(eglGetProcAddress); EGL_STUB(eglGetSyncAttrib); EGL_STUB(eglInitialize); EGL_STUB(eglMakeCurrent);
    EGL_STUB(eglQueryAPI); EGL_STUB(eglQueryContext); EGL_STUB(eglQueryString); EGL_STUB(eglQuerySurface);
    EGL_STUB(eglReleaseTexImage); EGL_STUB(eglReleaseThread); EGL_STUB(eglSurfaceAttrib); EGL_STUB(eglSwapBuffers);
    EGL_STUB(eglSwapInterval); EGL_STUB(eglTerminate); EGL_STUB(eglWaitClient); EGL_STUB(eglWaitGL);
    EGL_STUB(eglWaitNative); EGL_STUB(eglWaitSync);
#undef EGL_STUB
    return 0;
}
