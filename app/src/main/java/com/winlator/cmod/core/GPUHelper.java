package com.winlator.cmod.core;

import android.util.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class GPUHelper {
    public static final int VK_API_VERSION_1_3 = vkMakeVersion(1, 3, 0);
    private static final Executor IO = Executors.newSingleThreadExecutor();
    private static final CompletableFuture<Integer> API_VERSION_FUTURE =
            CompletableFuture.supplyAsync(GPUHelper::vkGetApiVersion, IO);

    static {
        System.loadLibrary("winlator");
    }

    public static native int vkGetApiVersion();

    public static native String[] vkGetDeviceExtensions();

    public static int vkGetApiVersionSafe() {
        try {
            return API_VERSION_FUTURE.getNow(VK_API_VERSION_1_3);
        }
        catch (CompletionException ex) {
            Log.e("GPUHelper", "Failed to get Vulkan API version", ex);
            return VK_API_VERSION_1_3;
        }
    }

    public static int vkVersionPatch() {
        try {
            return vkGetApiVersionSafe() & 0xFFF;
        }
        catch (UnsatisfiedLinkError e) {
            Log.e("GPUHelper", "Failed to load Vulkan library", e);
            return 0;
        }
        catch (Exception e) {
            Log.e("GPUHelper", "Failed to get Vulkan version patch", e);
            return 0;
        }
    }

    public static int vkMakeVersion(String value) {
        Pattern pattern = Pattern.compile("([0-9]+)\\.([0-9]+)\\.?([0-9]+)?");
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) return 0;
        try {
            int major = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
            int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            if (matcher.group(1) == null && patch == 0) patch = minor;
            return vkMakeVersion(major, minor, patch);
        }
        catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static int vkMakeVersion(int major, int minor, int patch) {
        return (major << 22) | (minor << 12) | patch;
    }

    public static int vkVersionMajor(int version) {
        return version >> 22;
    }

    public static int vkVersionMinor(int version) {
        return (version >> 12) & 1023;
    }
}
