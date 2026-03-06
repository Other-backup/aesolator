package com.winlator.cmod.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
    public static final String PREF_ENABLE_SHIZUKU_CAPTURE = "forensic_issue_include_shizuku_capture";
    public static final String PREF_ADB_CAPTURE_MODE = "forensic_adb_capture_mode";
    public static final String ADB_CAPTURE_MODE_AUTO = "auto";
    public static final String ADB_CAPTURE_MODE_NONROOT = "nonroot";
    public static final String ADB_CAPTURE_MODE_ROOT = "root";
    public static final String ADB_CAPTURE_MODE_SHIZUKU = "shizuku";

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
        snapshot.enableShizukuCapture = preferences.getBoolean(PREF_ENABLE_SHIZUKU_CAPTURE, false);
        snapshot.adbCaptureMode = normalizeAdbCaptureMode(preferences.getString(PREF_ADB_CAPTURE_MODE, ADB_CAPTURE_MODE_AUTO));
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
        editor.putBoolean(PREF_ENABLE_SHIZUKU_CAPTURE, snapshot.enableShizukuCapture);
        editor.putString(PREF_ADB_CAPTURE_MODE, normalizeAdbCaptureMode(snapshot.adbCaptureMode));
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
                + ";mesa_graphics:" + (snapshot.enableTurnipLogs ? "mesa,file" : "off")
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
            obj.put("enableShizukuCapture", snapshot.enableShizukuCapture);
            obj.put("adbCaptureMode", normalizeAdbCaptureMode(snapshot.adbCaptureMode));
            obj.put("rootBinaryPresent", isRootBinaryPresent());
            obj.put("runAsCapable", isRunAsCapable(context));
            obj.put("shizukuInstalled", isShizukuInstalled(context));
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
                + " | MesaGraphics=" + flag(snapshot.enableTurnipLogs)
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
        if (snapshot == null) snapshot = new Snapshot();
        String mode = normalizeAdbCaptureMode(snapshot.adbCaptureMode);
        boolean shizukuReady = snapshot.enableShizukuCapture && isShizukuInstalled(context);
        return "ADB/non-root=" + flag(snapshot.enableNonRootCapture)
                + " | root extras=" + (snapshot.enableRootCapture && isRootBinaryPresent() ? "ready" : snapshot.enableRootCapture ? "requested-no-su" : "off")
                + " | shizuku=" + (shizukuReady ? "ready" : snapshot.enableShizukuCapture ? "requested-missing" : "off")
                + " | adb mode=" + mode
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
        Snapshot snapshot = load(context);
        return "App forensic bundle includes JSONL/runtime files only. For maximum capture run adb matrix: "
                + buildIssueCaptureCommand(context) + ". "
                + "For on-device root capture use: " + buildOnDeviceRootCaptureCommand(context, snapshot)
                + " (bundle and archive are written to /sdcard/Winlator/forensics).";
    }

    public static String buildIssueCaptureCommand(Context context) {
        return buildIssueCaptureCommand(context, isRootBinaryPresent(), false);
    }

    public static String buildIssueCaptureCommand(Context context, boolean preferRoot) {
        return buildIssueCaptureCommand(context, preferRoot, false);
    }

    public static String buildIssueCaptureCommand(Context context, boolean preferRoot, boolean preferShizuku) {
        return "bash ci/winlator/forensic-adb-issue-capture.sh --serial <serial> --package "
                + context.getPackageName()
                + (preferRoot ? " --prefer-root" : "")
                + (preferShizuku ? " --prefer-shizuku" : "")
                + " --bundle-dir <out-dir>";
    }

    public static String buildOnDeviceRootCaptureScript(Context context) {
        return buildOnDeviceRootCaptureScript(context, load(context));
    }

    public static String buildOnDeviceRootCaptureScript(Context context, Snapshot snapshot) {
        String packageName = context == null ? "" : context.getPackageName();
        Snapshot safeSnapshot = snapshot == null ? load(context) : snapshot;
        String runtimeSummary = sanitizeForShell(buildRuntimeSummary(safeSnapshot));
        String captureSummary = sanitizeForShell(buildCaptureSummary(context, safeSnapshot));
        return "set -e;"
                + " ROOT_DIR=/sdcard/Winlator;"
                + " FORENSIC_DIR=\"$ROOT_DIR/forensics\";"
                + " BUNDLES_DIR=\"$FORENSIC_DIR/issue-bundles\";"
                + " TS=$(date +%Y-%m-%d_%H-%M-%S);"
                + " ISSUE_DIR=\"$BUNDLES_DIR/issue_$TS\";"
                + " ARCHIVE=\"$FORENSIC_DIR/issue_$TS.tar.gz\";"
                + " mkdir -p \"$ISSUE_DIR\";"
                + " logcat -d -v threadtime > \"$ISSUE_DIR/logcat_root.txt\" 2>/dev/null || true;"
                + " getprop > \"$ISSUE_DIR/getprop.txt\" 2>/dev/null || true;"
                + " ps -A -o PID,PPID,USER,NAME,ARGS > \"$ISSUE_DIR/ps.txt\" 2>/dev/null || true;"
                + " printf '%s\\n' '" + runtimeSummary + "' > \"$ISSUE_DIR/runtime_summary.txt\";"
                + " printf '%s\\n' '" + captureSummary + "' > \"$ISSUE_DIR/capture_summary.txt\";"
                + " printf 'WINE_DEBUG=%s\\n' '" + (safeSnapshot.enableWineDebug ? "1" : "0") + "' > \"$ISSUE_DIR/layer_markers.env\";"
                + " printf 'LOADER_TRACE=%s\\n' '" + (safeSnapshot.enableLoaderTrace ? "1" : "0") + "' >> \"$ISSUE_DIR/layer_markers.env\";"
                + " printf 'BOX64_LOGS=%s\\n' '" + (safeSnapshot.enableBox64Logs ? "1" : "0") + "' >> \"$ISSUE_DIR/layer_markers.env\";"
                + " printf 'FEX_LOGS=%s\\n' '" + (safeSnapshot.enableFexLogs ? "1" : "0") + "' >> \"$ISSUE_DIR/layer_markers.env\";"
                + " printf 'TURNIP_LOGS=%s\\n' '" + (safeSnapshot.enableTurnipLogs ? "1" : "0") + "' >> \"$ISSUE_DIR/layer_markers.env\";"
                + " printf 'DXVK_LOGS=%s\\n' '" + (safeSnapshot.enableDxvkLogs ? "1" : "0") + "' >> \"$ISSUE_DIR/layer_markers.env\";"
                + " printf 'VKD3D_LOGS=%s\\n' '" + (safeSnapshot.enableVkd3dLogs ? "1" : "0") + "' >> \"$ISSUE_DIR/layer_markers.env\";"
                + " printf 'DGVOODOO_LOGS=%s\\n' '" + (safeSnapshot.enableDgVoodooLogs ? "1" : "0") + "' >> \"$ISSUE_DIR/layer_markers.env\";"
                + " printf 'VULKAN_API_DUMP=%s\\n' '" + (safeSnapshot.enableVulkanApiDump ? "1" : "0") + "' >> \"$ISSUE_DIR/layer_markers.env\";"
                + " printf 'VULKAN_LOADER_DEBUG=%s\\n' '" + (safeSnapshot.enableVulkanLoaderDebug ? "1" : "0") + "' >> \"$ISSUE_DIR/layer_markers.env\";"
                + " printf 'VULKAN_VALIDATION=%s\\n' '" + (safeSnapshot.enableVulkanValidation ? "1" : "0") + "' >> \"$ISSUE_DIR/layer_markers.env\";"
                + " printf 'PULSE_LOGS=%s\\n' '" + (safeSnapshot.enablePulseLogs ? "1" : "0") + "' >> \"$ISSUE_DIR/layer_markers.env\";"
                + " printf 'ALSA_LOGS=%s\\n' '" + (safeSnapshot.enableAlsaLogs ? "1" : "0") + "' >> \"$ISSUE_DIR/layer_markers.env\";"
                + " cp -a /data/data/" + packageName + "/files/Winlator/logs/forensics \"$ISSUE_DIR/forensics\" 2>/dev/null || true;"
                + " cp -a /data/data/" + packageName + "/files/forensics/issue-bundles \"$ISSUE_DIR/app_issue_bundles\" 2>/dev/null || true;"
                + " if command -v tar >/dev/null 2>&1; then tar -czf \"$ARCHIVE\" -C \"$BUNDLES_DIR\" \"issue_$TS\" 2>/dev/null || true;"
                + " elif command -v toybox >/dev/null 2>&1; then toybox tar -czf \"$ARCHIVE\" -C \"$BUNDLES_DIR\" \"issue_$TS\" 2>/dev/null || true; fi;"
                + " echo \"bundle=$ISSUE_DIR\";"
                + " echo \"archive=$ARCHIVE\";";
    }

    public static String buildOnDeviceRootCaptureCommand(Context context) {
        return buildOnDeviceRootCaptureCommand(context, load(context));
    }

    public static String buildOnDeviceRootCaptureCommand(Context context, Snapshot snapshot) {
        String script = buildOnDeviceRootCaptureScript(context, snapshot);
        return "su -c '" + script.replace("'", "'\\''") + "'";
    }

    public static String buildCaptureCommand(Context context, Snapshot snapshot) {
        if (snapshot == null) {
            return buildIssueCaptureCommand(context, isRootBinaryPresent(), false);
        }
        String mode = normalizeAdbCaptureMode(snapshot.adbCaptureMode);
        boolean allowRoot = snapshot.enableRootCapture && isRootBinaryPresent();
        boolean allowShizuku = snapshot.enableShizukuCapture && isShizukuInstalled(context);
        if (ADB_CAPTURE_MODE_ROOT.equals(mode)) {
            return allowRoot ? buildOnDeviceRootCaptureCommand(context, snapshot) : buildIssueCaptureCommand(context, false, false);
        }
        if (ADB_CAPTURE_MODE_NONROOT.equals(mode)) return buildIssueCaptureCommand(context, false, false);
        if (ADB_CAPTURE_MODE_SHIZUKU.equals(mode)) return buildIssueCaptureCommand(context, false, allowShizuku);
        return buildIssueCaptureCommand(context, allowRoot, allowShizuku && !allowRoot);
    }

    public static String buildIssueBrowseCommand(Context context) {
        return "adb shell run-as "
                + context.getPackageName()
                + " sh -c 'cd files/forensics && find issue-bundles -maxdepth 2 -type f | sort'";
    }

    public static String normalizeAdbCaptureMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        if (ADB_CAPTURE_MODE_NONROOT.equals(normalized)) return ADB_CAPTURE_MODE_NONROOT;
        if (ADB_CAPTURE_MODE_ROOT.equals(normalized)) return ADB_CAPTURE_MODE_ROOT;
        if (ADB_CAPTURE_MODE_SHIZUKU.equals(normalized)) return ADB_CAPTURE_MODE_SHIZUKU;
        return ADB_CAPTURE_MODE_AUTO;
    }

    public static boolean isShizukuInstalled(Context context) {
        if (context == null) return false;
        try {
            context.getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private static String flag(boolean enabled) {
        return enabled ? "on" : "off";
    }

    private static String sanitizeForShell(String value) {
        if (value == null) return "";
        return value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace("'", "");
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
        public boolean enableShizukuCapture = false;
        public String adbCaptureMode = ADB_CAPTURE_MODE_AUTO;
    }
}
