package com.winlator.cmod.core;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public abstract class GPUInformation {

    public static String getDeviceIdFromGPUName(Context context, String gpuName) {
        return lookupGpuCardField(context, gpuName, "deviceID");
    }

    public static String getVendorIdFromGPUName(Context context, String gpuName) {
        return lookupGpuCardField(context, gpuName, "vendorID");
    }

    private static String lookupGpuCardField(Context context, String gpuName, String fieldName) {
        if (context == null || gpuName == null || gpuName.trim().isEmpty()) return "";
        String gpuNameList = FileUtils.readString(context, "gpu_cards.json");
        try {
            JSONArray jsonArray = new JSONArray(gpuNameList);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject gpu = jsonArray.getJSONObject(i);
                if (gpu.optString("name").contains(gpuName)) {
                    return gpu.optString(fieldName, "");
                }
            }
        }
        catch (JSONException ignored) { /* best-effort path; keep surrounding flow intact. */ }
        return "";
    }

    private static String getSystemRendererName(Context context) {
        if (context == null) return "";
        String renderer = getRenderer(null, context);
        return renderer == null ? "" : renderer.toLowerCase(Locale.ENGLISH);
    }

    public static boolean isAdrenoGPU(Context context) {
        return getSystemRendererName(context).contains("adreno");
    }

    public static boolean isAdreno6xx(Context context) {
        return getSystemRendererName(context).matches(".*adreno[^6]+6[0-9]{2}.*");
    }

    public static boolean isAdreno8Elite(Context context) {
        String renderer = getSystemRendererName(context);
        return renderer.contains("adreno") && renderer.matches(".*\\b8(3[0-9]|4[0-9]|5[0-9])\\b.*");
    }

    public static boolean isTurnipCapable(Context context) {
        String renderer = getSystemRendererName(context);
        return renderer.contains("adreno") && renderer.matches(".*\\b[67][0-9]{2}\\b.*");
    }

    public static boolean isAdreno710_720_732(Context context) {
        String renderer = getSystemRendererName(context);
        return renderer.contains("adreno") && renderer.matches(".*\\b(710|720|732)\\b.*");
    }

    public static boolean isDriverSupported(String driverName, Context context) {
        if (!isAdrenoGPU(context) && !driverName.equals("System"))
            return false;

        String renderer = getRenderer(driverName, context);

        return !renderer.toLowerCase().contains("unknown");
    }
    public native static String getVulkanVersion(String driverName, Context context);
    public native static int getVendorID(String driverName, Context context);
    public native static String getRenderer(String driverName, Context context);
    public native static String[] enumerateExtensions(String driverName, Context context);

    static {
        WinlatorNative.ensureLoaded("GPUInformation");
    }
}
