package com.winlator.cmod.renderer;

import com.winlator.cmod.core.WinlatorNative;

public class NativeRenderer {
    static {
        WinlatorNative.ensureLoaded("NativeRenderer");
    }

    public native static void eglSwapBuffersWrapper(long dpy, long surf);

    public native static boolean initEGLContext(Object nativeWindow);

    public static native long getEGLDisplay();

    public static native long getEGLSurface();
}
