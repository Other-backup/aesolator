#include <EGL/egl.h>
#include <android/native_window_jni.h>
#include <jni.h>

EGLDisplay globalEGLDisplay = EGL_NO_DISPLAY;
EGLSurface globalEGLSurface = EGL_NO_SURFACE;
EGLContext globalEGLContext = EGL_NO_CONTEXT;

static void destroy_global_egl_state(void) {
    if (globalEGLDisplay == EGL_NO_DISPLAY) return;

    eglMakeCurrent(globalEGLDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);

    if (globalEGLContext != EGL_NO_CONTEXT) {
        eglDestroyContext(globalEGLDisplay, globalEGLContext);
        globalEGLContext = EGL_NO_CONTEXT;
    }

    if (globalEGLSurface != EGL_NO_SURFACE) {
        eglDestroySurface(globalEGLDisplay, globalEGLSurface);
        globalEGLSurface = EGL_NO_SURFACE;
    }

    eglTerminate(globalEGLDisplay);
    globalEGLDisplay = EGL_NO_DISPLAY;
}

JNIEXPORT void JNICALL
Java_com_winlator_cmod_renderer_NativeRenderer_eglSwapBuffersWrapper(JNIEnv *env, jclass cls, jlong dpy, jlong surf) {
    (void)env;
    (void)cls;
    if (dpy && surf) eglSwapBuffers((EGLDisplay)dpy, (EGLSurface)surf);
}

JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_renderer_NativeRenderer_initEGLContext(JNIEnv *env, jclass cls, jobject nativeWindow) {
    (void)cls;

    if (!nativeWindow) return JNI_FALSE;

    ANativeWindow *window = ANativeWindow_fromSurface(env, nativeWindow);
    if (!window) return JNI_FALSE;

    destroy_global_egl_state();

    EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display == EGL_NO_DISPLAY) {
        ANativeWindow_release(window);
        return JNI_FALSE;
    }

    if (!eglInitialize(display, NULL, NULL)) {
        ANativeWindow_release(window);
        return JNI_FALSE;
    }

    static const EGLint configAttribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_NONE
    };
    EGLConfig config = NULL;
    EGLint configCount = 0;
    if (!eglChooseConfig(display, configAttribs, &config, 1, &configCount) || configCount != 1) {
        ANativeWindow_release(window);
        eglTerminate(display);
        return JNI_FALSE;
    }

    EGLSurface surface = eglCreateWindowSurface(display, config, window, NULL);
    ANativeWindow_release(window);
    if (surface == EGL_NO_SURFACE) {
        eglTerminate(display);
        return JNI_FALSE;
    }

    static const EGLint contextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL_NONE
    };
    EGLContext context = eglCreateContext(display, config, EGL_NO_CONTEXT, contextAttribs);
    if (context == EGL_NO_CONTEXT) {
        eglDestroySurface(display, surface);
        eglTerminate(display);
        return JNI_FALSE;
    }

    if (!eglMakeCurrent(display, surface, surface, context)) {
        eglDestroyContext(display, context);
        eglDestroySurface(display, surface);
        eglTerminate(display);
        return JNI_FALSE;
    }

    globalEGLDisplay = display;
    globalEGLSurface = surface;
    globalEGLContext = context;
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL
Java_com_winlator_cmod_renderer_NativeRenderer_getEGLDisplay(JNIEnv *env, jclass cls) {
    (void)env;
    (void)cls;
    return (jlong)globalEGLDisplay;
}

JNIEXPORT jlong JNICALL
Java_com_winlator_cmod_renderer_NativeRenderer_getEGLSurface(JNIEnv *env, jclass cls) {
    (void)env;
    (void)cls;
    return (jlong)globalEGLSurface;
}
