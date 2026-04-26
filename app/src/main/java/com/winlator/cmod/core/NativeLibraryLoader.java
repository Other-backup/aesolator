package com.winlator.cmod.core;

import android.content.Context;

import org.json.JSONObject;

import java.util.HashSet;

public final class NativeLibraryLoader {
    private static final Object LOAD_LOCK = new Object();
    private static final HashSet<String> LOADED_LIBRARIES = new HashSet<>();

    private NativeLibraryLoader() {}

    public static void ensureLoaded(String libraryName, String reason) {
        ensureLoaded(null, libraryName, reason);
    }

    public static void ensureLoaded(Context context, String libraryName, String reason) {
        String normalizedLibrary = libraryName == null ? "" : libraryName.trim();
        if (normalizedLibrary.isEmpty()) {
            throw new IllegalArgumentException("libraryName is required");
        }

        synchronized (LOAD_LOCK) {
            if (LOADED_LIBRARIES.contains(normalizedLibrary)) return;

            Context sinkContext = context != null ? context.getApplicationContext() : ForensicLogger.getAppContext();
            JSONObject fields = ForensicLogger.fields(
                    "library", normalizedLibrary,
                    "reason", reason == null ? "unspecified" : reason
            );

            if (sinkContext != null) {
                ForensicLogger.checkpoint(
                        sinkContext,
                        "info",
                        "NATIVE_LIBRARY_LOAD_BEGIN",
                        null,
                        "native_loader",
                        "loading_native_library",
                        fields
                );
            }

            try {
                System.loadLibrary(normalizedLibrary);
                LOADED_LIBRARIES.add(normalizedLibrary);
                if (sinkContext != null) {
                    ForensicLogger.checkpoint(
                            sinkContext,
                            "info",
                            "NATIVE_LIBRARY_LOAD_READY",
                            null,
                            "native_loader",
                            "loaded_native_library",
                            fields
                    );
                }
            }
            catch (Throwable error) {
                if (sinkContext != null) {
                    ForensicLogger.error(
                            sinkContext,
                            "NATIVE_LIBRARY_LOAD_FAILURE",
                            null,
                            "native_loader",
                            "failed_loading_native_library",
                            error,
                            fields
                    );
                }
                throw error;
            }
        }
    }
}
