package com.winlator.cmod.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import androidx.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

public final class ForensicConfig {
    public static final String DEFAULT_WINE_DEBUG_CHANNELS = "warn,err,fixme";

    public static final String PREF_ENABLE_WINE_DEBUG = "enable_wine_debug";
    public static final String PREF_WINE_DEBUG_CHANNELS = "wine_debug_channels";
    public static final String PREF_ENABLE_LOADER_TRACE = "enable_loader_trace";
    public static final String PREF_ENABLE_BOX64_LOGS = "enable_box64_logs";
    public static final String PREF_ENABLE_FEX_LOGS = "enable_fex_logs";
    public static final String PREF_ENABLE_TURNIP_LOGS = "enable_turnip_logs";
    public static final String PREF_ENABLE_VULKAN_API_DUMP = "enable_vulkan_api_dump";
    public static final String PREF_ENABLE_VULKAN_LOADER_DEBUG = "enable_vulkan_loader_debug";
    public static final String PREF_ENABLE_VULKAN_VALIDATION = "enable_vulkan_validation";
    public static final String PREF_ENABLE_DXVK_LOGS = "enable_dxvk_logs";
    public static final String PREF_ENABLE_VKD3D_LOGS = "enable_vkd3d_logs";
    public static final String PREF_ENABLE_DGVOODOO_LOGS = "enable_dgvoodoo_logs";
    public static final String PREF_ENABLE_PULSE_LOGS = "enable_pulse_logs";
    public static final String PREF_ENABLE_ALSA_LOGS = "enable_alsa_logs";
    public static final String PREF_ENABLE_DEVICE_SNAPSHOT = "forensic_issue_include_device_snapshot";
    public static final String PREF_ENABLE_NONROOT_CAPTURE = "forensic_issue_include_nonroot_capture";
    public static final String PREF_ENABLE_ROOT_CAPTURE = "forensic_issue_include_root_capture";

    private ForensicConfig() {}

    public static Snapshot load(Context context) {
        return fromPreferences(PreferenceManager.getDefaultSharedPreferences(context));
    }

    public static Snapshot fromPreferences(SharedPreferences preferences) {
        Snapshot snapshot = new Snapshot();
        snapshot.enableWineDebug = preferences.getBoolean(PREF_ENABLE_WINE_DEBUG, false);
        snapshot.wineDebugChannels = normalizeChannels(preferences.getString(PREF_WINE_DEBUG_CHANNELS, DEFAULT_WINE_DEBUG_CHANNELS));
        snapshot.enableLoaderTrace = preferences.getBoolean(PREF_ENABLE_LOADER_TRACE, false);
        snapshot.enableBox64Logs = preferences.getBoolean(PREF_ENABLE_BOX64_LOGS, false);
        snapshot.enableFexLogs = preferences.getBoolean(PREF_ENABLE_FEX_LOGS, false);
        snapshot.enableTurnipLogs = preferences.getBoolean(PREF_ENABLE_TURNIP_LOGS, false);
        snapshot.enableVulkanApiDump = preferences.getBoolean(PREF_ENABLE_VULKAN_API_DUMP, false);
        snapshot.enableVulkanLoaderDebug = preferences.getBoolean(PREF_ENABLE_VULKAN_LOADER_DEBUG, false);
        snapshot.enableVulkanValidation = preferences.getBoolean(PREF_ENABLE_VULKAN_VALIDATION, false);
        snapshot.enableDxvkLogs = preferences.getBoolean(PREF_ENABLE_DXVK_LOGS, false);
        snapshot.enableVkd3dLogs = preferences.getBoolean(PREF_ENABLE_VKD3D_LOGS, false);
        snapshot.enableDgVoodooLogs = preferences.getBoolean(PREF_ENABLE_DGVOODOO_LOGS, false);
        snapshot.enablePulseLogs = preferences.getBoolean(PREF_ENABLE_PULSE_LOGS, false);
        snapshot.enableAlsaLogs = preferences.getBoolean(PREF_ENABLE_ALSA_LOGS, false);
        snapshot.enableDeviceSnapshot = preferences.getBoolean(PREF_ENABLE_DEVICE_SNAPSHOT, true);
        snapshot.enableNonRootCapture = preferences.getBoolean(PREF_ENABLE_NONROOT_CAPTURE, true);
        snapshot.enableRootCapture = preferences.getBoolean(PREF_ENABLE_ROOT_CAPTURE, true);
        return snapshot;
    }

    public static void save(Context context, Snapshot snapshot) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        apply(editor, snapshot);
        editor.apply();
    }

    public static void apply(SharedPreferences.Editor editor, Snapshot snapshot) {
        editor.putBoolean(PREF_ENABLE_WINE_DEBUG, snapshot.enableWineDebug);
        editor.putString(PREF_WINE_DEBUG_CHANNELS, normalizeChannels(snapshot.wineDebugChannels));
        editor.putBoolean(PREF_ENABLE_LOADER_TRACE, snapshot.enableLoaderTrace);
        editor.putBoolean(PREF_ENABLE_BOX64_LOGS, snapshot.enableBox64Logs);
        editor.putBoolean(PREF_ENABLE_FEX_LOGS, snapshot.enableFexLogs);
        editor.putBoolean(PREF_ENABLE_TURNIP_LOGS, snapshot.enableTurnipLogs);
        editor.putBoolean(PREF_ENABLE_VULKAN_API_DUMP, snapshot.enableVulkanApiDump);
        editor.putBoolean(PREF_ENABLE_VULKAN_LOADER_DEBUG, snapshot.enableVulkanLoaderDebug);
        editor.putBoolean(PREF_ENABLE_VULKAN_VALIDATION, snapshot.enableVulkanValidation);
        editor.putBoolean(PREF_ENABLE_DXVK_LOGS, snapshot.enableDxvkLogs);
        editor.putBoolean(PREF_ENABLE_VKD3D_LOGS, snapshot.enableVkd3dLogs);
        editor.putBoolean(PREF_ENABLE_DGVOODOO_LOGS, snapshot.enableDgVoodooLogs);
        editor.putBoolean(PREF_ENABLE_PULSE_LOGS, snapshot.enablePulseLogs);
        editor.putBoolean(PREF_ENABLE_ALSA_LOGS, snapshot.enableAlsaLogs);
        editor.putBoolean(PREF_ENABLE_DEVICE_SNAPSHOT, snapshot.enableDeviceSnapshot);
        editor.putBoolean(PREF_ENABLE_NONROOT_CAPTURE, snapshot.enableNonRootCapture);
        editor.putBoolean(PREF_ENABLE_ROOT_CAPTURE, snapshot.enableRootCapture);
    }

    public static boolean shouldEnableLoaderTrace(Snapshot snapshot, boolean forensicMode) {
        return forensicMode || snapshot.enableLoaderTrace || snapshot.enableWineDebug;
    }

    public static String buildEffectiveWineDebug(boolean enableWineDebug, String wineDebugChannels, boolean loaderTraceEnabled) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        if (enableWineDebug) {
            for (String token : getChannelList(wineDebugChannels)) {
                if (token.startsWith("+") || token.startsWith("-")) tokens.add(token);
                else tokens.add("+" + token);
            }
        }
        if (tokens.isEmpty()) tokens.add("-all");
        if (loaderTraceEnabled) {
            tokens.add("+loaddll");
            tokens.add("+module");
        }
        return String.join(",", tokens);
    }

    public static String buildLoaderTraceMode(Snapshot snapshot) {
        return "wine:loaddll,module"
                + ";box64:" + (snapshot.enableBox64Logs ? "stdout,file,dynarec_missing" : "off")
                + ";fex:" + (snapshot.enableFexLogs ? "debug,file" : "off")
                + ";dxvk:" + (snapshot.enableDxvkLogs ? "native,file" : "off")
                + ";vkd3d:" + (snapshot.enableVkd3dLogs ? "native,file" : "off")
                + ";dgvoodoo:" + (snapshot.enableDgVoodooLogs ? "native,file" : "off")
                + ";turnip:" + (snapshot.enableTurnipLogs ? "mesa,file" : "off")
                + ";pulse:" + (snapshot.enablePulseLogs ? "service" : "off")
                + ";alsa:" + (snapshot.enableAlsaLogs ? "service" : "off");
    }

    public static String normalizeChannels(String channels) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String raw : getChannelList(channels)) {
            String token = raw.toLowerCase(Locale.ROOT).trim();
            if (!token.isEmpty()) tokens.add(token);
        }
        if (tokens.isEmpty()) {
            for (String token : DEFAULT_WINE_DEBUG_CHANNELS.split(",")) tokens.add(token.trim());
        }
        return String.join(",", tokens);
    }

    public static ArrayList<String> getChannelList(String channels) {
        ArrayList<String> values = new ArrayList<>();
        if (channels == null || channels.trim().isEmpty()) {
            for (String token : DEFAULT_WINE_DEBUG_CHANNELS.split(",")) values.add(token.trim());
            return values;
        }
        for (String raw : channels.split(",")) {
            String token = raw == null ? "" : raw.trim();
            if (!token.isEmpty()) values.add(token);
        }
        if (values.isEmpty()) {
            for (String token : DEFAULT_WINE_DEBUG_CHANNELS.split(",")) values.add(token.trim());
        }
        return values;
    }

    public static JSONObject toJson(Context context, Snapshot snapshot) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("enableWineDebug", snapshot.enableWineDebug);
            obj.put("wineDebugChannels", normalizeChannels(snapshot.wineDebugChannels));
            obj.put("enableLoaderTrace", snapshot.enableLoaderTrace);
            obj.put("enableBox64Logs", snapshot.enableBox64Logs);
            obj.put("enableFexLogs", snapshot.enableFexLogs);
            obj.put("enableTurnipLogs", snapshot.enableTurnipLogs);
            obj.put("enableVulkanApiDump", snapshot.enableVulkanApiDump);
            obj.put("enableVulkanLoaderDebug", snapshot.enableVulkanLoaderDebug);
            obj.put("enableVulkanValidation", snapshot.enableVulkanValidation);
            obj.put("enableDxvkLogs", snapshot.enableDxvkLogs);
            obj.put("enableVkd3dLogs", snapshot.enableVkd3dLogs);
            obj.put("enableDgVoodooLogs", snapshot.enableDgVoodooLogs);
            obj.put("enablePulseLogs", snapshot.enablePulseLogs);
            obj.put("enableAlsaLogs", snapshot.enableAlsaLogs);
            obj.put("enableDeviceSnapshot", snapshot.enableDeviceSnapshot);
            obj.put("enableNonRootCapture", snapshot.enableNonRootCapture);
            obj.put("enableRootCapture", snapshot.enableRootCapture);
            obj.put("rootBinaryPresent", isRootBinaryPresent());
            obj.put("runAsCapable", isRunAsCapable(context));
            obj.put("adbShellRecommended", true);
            obj.put("loaderTraceMode", buildLoaderTraceMode(snapshot));
        }
        catch (JSONException ignored) {}
        return obj;
    }

    public static String buildRuntimeSummary(Snapshot snapshot) {
        return "Wine=" + flag(snapshot.enableWineDebug)
                + " | Loader=" + flag(snapshot.enableLoaderTrace)
                + " | Box64=" + flag(snapshot.enableBox64Logs)
                + " | FEX=" + flag(snapshot.enableFexLogs)
                + " | Turnip=" + flag(snapshot.enableTurnipLogs)
                + " | DXVK=" + flag(snapshot.enableDxvkLogs)
                + " | VKD3D=" + flag(snapshot.enableVkd3dLogs)
                + " | dgVoodoo=" + flag(snapshot.enableDgVoodooLogs)
                + " | Pulse=" + flag(snapshot.enablePulseLogs)
                + " | ALSA=" + flag(snapshot.enableAlsaLogs)
                + " | VulkanDump=" + flag(snapshot.enableVulkanApiDump)
                + " | LoaderDebug=" + flag(snapshot.enableVulkanLoaderDebug)
                + " | Validation=" + flag(snapshot.enableVulkanValidation);
    }

    public static String buildCaptureSummary(Context context, Snapshot snapshot) {
        return "ADB/non-root=" + flag(snapshot.enableNonRootCapture)
                + " | root extras=" + (snapshot.enableRootCapture && isRootBinaryPresent() ? "ready" : snapshot.enableRootCapture ? "requested-no-su" : "off")
                + " | device snapshot=" + flag(snapshot.enableDeviceSnapshot)
                + " | run-as=" + (isRunAsCapable(context) ? "ready" : "off");
    }

    public static boolean isRootBinaryPresent() {
        return new File("/system/bin/su").exists()
                || new File("/system/xbin/su").exists()
                || new File("/sbin/su").exists();
    }

    public static boolean isRunAsCapable(Context context) {
        if (context == null) return false;
        ApplicationInfo info = context.getApplicationInfo();
        return info != null && (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public static String buildIssueCaptureContract(Context context) {
        return "App forensic bundle includes JSONL/runtime files only. For maximum capture run adb matrix: "
                + buildIssueCaptureCommand(context) + ".";
    }

    public static String buildIssueCaptureCommand(Context context) {
        return buildIssueCaptureCommand(context, isRootBinaryPresent());
    }

    public static String buildIssueCaptureCommand(Context context, boolean preferRoot) {
        return "bash ci/winlator/forensic-adb-issue-capture.sh --serial <serial> --package "
                + context.getPackageName()
                + (preferRoot ? " --prefer-root" : "")
                + " --bundle-dir <out-dir>";
    }

    public static String buildCaptureCommand(Context context, Snapshot snapshot) {
        boolean preferRoot = snapshot != null && snapshot.enableRootCapture && isRootBinaryPresent();
        return buildIssueCaptureCommand(context, preferRoot);
    }

    public static String buildIssueBrowseCommand(Context context) {
        return "adb shell run-as "
                + context.getPackageName()
                + " sh -c 'cd files/forensics && find issue-bundles -maxdepth 2 -type f | sort'";
    }

    private static String flag(boolean enabled) {
        return enabled ? "on" : "off";
    }

    public static final class Snapshot {
        public boolean enableWineDebug;
        public String wineDebugChannels = DEFAULT_WINE_DEBUG_CHANNELS;
        public boolean enableLoaderTrace;
        public boolean enableBox64Logs;
        public boolean enableFexLogs;
        public boolean enableTurnipLogs;
        public boolean enableVulkanApiDump;
        public boolean enableVulkanLoaderDebug;
        public boolean enableVulkanValidation;
        public boolean enableDxvkLogs;
        public boolean enableVkd3dLogs;
        public boolean enableDgVoodooLogs;
        public boolean enablePulseLogs;
        public boolean enableAlsaLogs;
        public boolean enableDeviceSnapshot = true;
        public boolean enableNonRootCapture = true;
        public boolean enableRootCapture = true;
    }
}
