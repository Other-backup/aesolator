package com.winlator.cmod.core;

import android.util.Log;

public abstract class DefaultVersion {
    private static final String TAG = "DefaultVersion";

    public static final String BOX64 = "0.3.7";
    public static final String WOWBOX64 = "0.3.7";
    public static final String FEXCORE = "2508";
    public static final String WRAPPER = "System";
    public static final String WRAPPER_ADRENO = "turnip25.1.0";
    public static final String DXVK = resolveDefaultDxvk();
    public static final String D8VK = "1.0";
    public static final String VKD3D = "None";

    private static String resolveDefaultDxvk() {
        try {
            String renderer = GPUInformation.getRenderer(null, null);
            return renderer != null && renderer.contains("Mali") ? "1.10.3" : "2.3.1";
        } catch (Throwable throwable) {
            Log.w(TAG, "Falling back to generic DXVK default", throwable);
            return "2.3.1";
        }
    }
}
