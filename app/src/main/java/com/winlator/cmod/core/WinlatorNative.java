package com.winlator.cmod.core;

import android.content.Context;

import org.json.JSONObject;

public final class WinlatorNative {
    private static final Object LOAD_LOCK = new Object();
    private static volatile boolean loaded = false;

    private WinlatorNative() {}

    public static boolean isLoaded() {
        return loaded;
    }

    public static void ensureLoaded() {
        ensureLoaded(null, "unspecified");
    }

    public static void ensureLoaded(String reason) {
        ensureLoaded(null, reason);
    }

    public static void ensureLoaded(Context context, String reason) {
        if (loaded) return;
        synchronized (LOAD_LOCK) {
            if (loaded) return;

            Context sinkContext = context != null ? context.getApplicationContext() : ForensicLogger.getAppContext();
            JSONObject fields = ForensicLogger.fields(
                    "reason", reason == null ? "unspecified" : reason
            );

            if (sinkContext != null) {
                ForensicLogger.checkpoint(
                        sinkContext,
                        "info",
                        "WINLATOR_NATIVE_LOAD_BEGIN",
                        null,
                        "native_loader",
                        "loading_libwinlator",
                        fields
                );
            }

            try {
                System.loadLibrary("winlator");
                loaded = true;

                if (sinkContext != null) {
                    ForensicLogger.checkpoint(
                            sinkContext,
                            "info",
                            "WINLATOR_NATIVE_LOAD_READY",
                            null,
                            "native_loader",
                            "loaded_libwinlator",
                            fields
                    );
                }
            }
            catch (Throwable error) {
                if (sinkContext != null) {
                    ForensicLogger.error(
                            sinkContext,
                            "WINLATOR_NATIVE_LOAD_FAILURE",
                            null,
                            "native_loader",
                            "failed_loading_libwinlator",
                            error,
                            fields
                    );
                }
                throw error;
            }
        }
    }
}
