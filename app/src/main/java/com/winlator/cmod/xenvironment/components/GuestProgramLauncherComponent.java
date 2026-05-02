package com.winlator.cmod.xenvironment.components;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.os.SystemClock;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.box64.Box64Preset;
import com.winlator.cmod.box64.Box64PresetManager;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.AndroidBionicHostLdPathHelper;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.GPUInformation;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.ProcessHelper;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.fexcore.FEXCoreManager;
import com.winlator.cmod.fexcore.FEXCorePreset;
import com.winlator.cmod.fexcore.FEXCorePresetManager;
import com.winlator.cmod.launchdeps.LaunchDependencyRegistry;
import com.winlator.cmod.xconnector.UnixSocketConfig;
import com.winlator.cmod.xenvironment.EnvironmentComponent;
import com.winlator.cmod.xenvironment.ImageFs;
import com.winlator.cmod.xenvironment.ImageFsInstaller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GuestProgramLauncherComponent extends EnvironmentComponent {
    private String guestExecutable;
    private static int pid = -1;
    private String[] bindingPaths;
    private EnvVars envVars;
    private WineInfo wineInfo;
    private String box64Preset = Box64Preset.COMPATIBILITY;
    private String fexcorePreset = FEXCorePreset.INTERMEDIATE;
    private Callback<Integer> terminationCallback;
    private static final Object lock = new Object();
    private final ContentsManager contentsManager;
    private final ContentProfile wineProfile;
    private Container container;
    private final Shortcut shortcut;

    @FunctionalInterface
    private interface LaunchStageAction {
        void run() throws Exception;
    }

    private static final class PrimaryDnsResolution {
        final String address;
        final String source;
        final boolean activeNetworkPresent;
        final boolean linkPropertiesPresent;
        final int dnsServerCount;

        PrimaryDnsResolution(String address, String source, boolean activeNetworkPresent,
                             boolean linkPropertiesPresent, int dnsServerCount) {
            this.address = address;
            this.source = source;
            this.activeNetworkPresent = activeNetworkPresent;
            this.linkPropertiesPresent = linkPropertiesPresent;
            this.dnsServerCount = dnsServerCount;
        }
    }

    private static final class LaunchOutputRing {
        private static final int MAX_LINES = 40;
        private static final int MAX_CHARS = 8192;
        private static final int MAX_LINE_CHARS = 2048;
        private final ArrayList<String> lines = new ArrayList<>();
        private int charCount = 0;

        synchronized void add(String line) {
            String value = line == null ? "" : line;
            if (value.length() > MAX_LINE_CHARS) {
                value = value.substring(0, MAX_LINE_CHARS) + " [truncated]";
            }
            lines.add(value);
            charCount += value.length();
            trim();
        }

        synchronized int lineCount() {
            return lines.size();
        }

        synchronized String snapshot() {
            return String.join("\n", lines);
        }

        private void trim() {
            while (!lines.isEmpty() && (lines.size() > MAX_LINES || charCount > MAX_CHARS)) {
                String removed = lines.remove(0);
                charCount -= removed != null ? removed.length() : 0;
            }
        }
    }

    public void setWineInfo(WineInfo wineInfo) {
        this.wineInfo = wineInfo;
    }
    public WineInfo getWineInfo() {
        return this.wineInfo;
    }

    public Container getContainer() { return this.container; }
    public void setContainer(Container container) { this.container = container; }

    protected ContentsManager getContentsManager() {
        return contentsManager;
    }

    protected ContentProfile getWineProfile() {
        return wineProfile;
    }

    protected Shortcut getShortcut() {
        return shortcut;
    }

    private String resolveLaunchAppId() {
        if (container != null) {
            String sessionAppId = container.getSessionMetadata("appId", "").trim();
            if (!sessionAppId.isEmpty()) return sessionAppId;
        }
        if (shortcut != null) {
            String shortcutAppId = shortcut.getExtra("appId", "").trim();
            if (!shortcutAppId.isEmpty()) return shortcutAppId;
            String shortcutName = shortcut.name != null ? shortcut.name.trim() : "";
            if (!shortcutName.isEmpty()) return shortcutName;
        }
        return guestExecutable;
    }

    private String resolveForensicTraceIdHint() {
        if (envVars == null) return null;
        String traceId = envVars.get("AERO_FORENSIC_TRACE_ID");
        if (traceId == null) return null;
        traceId = traceId.trim();
        return traceId.isEmpty() ? null : traceId;
    }

    private String normalizeVersion(String value, String fallback) {
        if (value == null) return fallback;
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private void logLaunchStageEvent(Context context, String severity, String eventId, String traceId,
                                     String stageId, String appId, long elapsedMs, Throwable error,
                                     Object... extraFields) {
        ArrayList<Object> fields = new ArrayList<>();
        fields.add("stage_id");
        fields.add(stageId == null ? "" : stageId);
        fields.add("app_id");
        fields.add(appId == null || appId.trim().isEmpty() ? "-" : appId);
        fields.add("container_id");
        fields.add(container != null ? container.id : -1);
        fields.add("guest_executable");
        fields.add(guestExecutable != null ? guestExecutable : "");
        if (elapsedMs >= 0L) {
            fields.add("elapsed_ms");
            fields.add(elapsedMs);
        }
        if (error != null) {
            fields.add("error_class");
            fields.add(error.getClass().getName());
            fields.add("error_detail");
            fields.add(String.valueOf(error.getMessage()));
        }
        if (extraFields != null) {
            for (Object field : extraFields) {
                fields.add(field);
            }
        }
        ForensicLogger.logEvent(
                context,
                severity,
                eventId,
                traceId,
                "guest_program_launcher",
                stageId == null ? "" : stageId,
                ForensicLogger.fields(fields.toArray())
        );
    }

    private boolean runLaunchStage(Context context, String traceId, String appId, String stageId,
                                   LaunchStageAction action, Object... extraFields) {
        logLaunchStageEvent(context, "info", "LAUNCH_STAGE_START", traceId, stageId, appId, -1L, null, extraFields);
        long startedAt = SystemClock.elapsedRealtime();
        try {
            action.run();
            logLaunchStageEvent(
                    context,
                    "info",
                    "LAUNCH_STAGE_DONE",
                    traceId,
                    stageId,
                    appId,
                    SystemClock.elapsedRealtime() - startedAt,
                    null,
                    extraFields
            );
            return true;
        } catch (Exception e) {
            Log.e("GuestProgramLauncherComponent", "Launch stage failed: " + stageId, e);
            logLaunchStageEvent(
                    context,
                    "error",
                    "LAUNCH_STAGE_FAILED",
                    traceId,
                    stageId,
                    appId,
                    SystemClock.elapsedRealtime() - startedAt,
                    e,
                    extraFields
            );
            return false;
        }
    }

    private void failLaunchPreparation(Context context, String traceId, String appId, String detail) {
        String message = detail == null || detail.trim().isEmpty()
                ? "Failed to prepare Wine runtime launch"
                : detail.trim();
        logLaunchStageEvent(
                context,
                "warn",
                "LAUNCH_PREPARE_ABORT",
                traceId,
                "launch_prepare_abort",
                appId,
                -1L,
                null,
                "detail", message
        );
        AppUtils.showToast(context, message);
        if (terminationCallback != null) terminationCallback.call(-1);
    }

    private PrimaryDnsResolution resolvePrimaryDns(Context context) {
        String fallback = "8.8.4.4";
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Service.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return new PrimaryDnsResolution(fallback, "fallback_no_connectivity_manager", false, false, 0);
        }

        try {
            android.net.Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                return new PrimaryDnsResolution(fallback, "fallback_no_active_network", false, false, 0);
            }

            LinkProperties linkProperties = connectivityManager.getLinkProperties(activeNetwork);
            if (linkProperties == null) {
                return new PrimaryDnsResolution(fallback, "fallback_no_link_properties", true, false, 0);
            }

            List<InetAddress> dnsServers = linkProperties.getDnsServers();
            if (dnsServers == null || dnsServers.isEmpty()) {
                return new PrimaryDnsResolution(fallback, "fallback_no_dns_servers", true, true, 0);
            }

            InetAddress primaryServer = dnsServers.get(0);
            String hostAddress = primaryServer != null ? primaryServer.getHostAddress() : "";
            hostAddress = hostAddress == null ? "" : hostAddress.trim();
            if (hostAddress.isEmpty()) {
                return new PrimaryDnsResolution(fallback, "fallback_empty_dns_address", true, true, dnsServers.size());
            }
            return new PrimaryDnsResolution(hostAddress, "active_network_dns", true, true, dnsServers.size());
        } catch (Exception e) {
            Log.w("GuestProgramLauncherComponent", "Unable to resolve active network DNS, falling back", e);
            return new PrimaryDnsResolution(fallback, "fallback_dns_exception", true, false, 0);
        }
    }

    private boolean needsFileRefresh(File... files) {
        for (File file : files) {
            if (file == null || !file.isFile()) return true;
        }
        return false;
    }

    private ContentProfile resolveInstalledContentProfile(ContentProfile.ContentType type, String versionName) {
        if (type == null || versionName == null || versionName.trim().isEmpty()) return null;
        ContentProfile profile = contentsManager.findInstalledProfileByVersion(type, versionName, true);
        if (profile != null) return profile;
        profile = contentsManager.findInstalledProfileByVersion(type, versionName, false);
        if (profile != null) return profile;
        profile = contentsManager.findProfileByVersion(type, versionName, true);
        if (profile != null) return profile;
        profile = contentsManager.findProfileByVersion(type, versionName, false);
        if (profile != null) return profile;

        String runtimeModel = environment != null && environment.getImageFs() != null
                ? environment.getImageFs().getRuntimeLibcModel()
                : "";
        String requestedArch = wineInfo != null ? wineInfo.getArch() : "";
        return contentsManager.findBestInstalledProfile(type, runtimeModel, requestedArch, true);
    }

    private String resolveEmbeddedBox64Archive(Context context, ImageFs imageFs, String box64Version) {
        String runtimeModel = imageFs != null ? imageFs.getRuntimeLibcModel() : "";
        ArrayList<String> candidates = new ArrayList<>();
        if ("bionic".equalsIgnoreCase(runtimeModel)) {
            candidates.add("box86_64/box64-" + box64Version + "-bionic.tzst");
        }
        candidates.add("box86_64/box64-" + box64Version + ".tzst");
        candidates.add("box64/box64-" + box64Version + ".tzst");

        for (String candidate : candidates) {
            if (assetExists(context, candidate)) return candidate;
        }
        return candidates.get(candidates.size() - 1);
    }

    private boolean assetExists(Context context, String assetPath) {
        if (context == null || assetPath == null || assetPath.trim().isEmpty()) return false;
        try (InputStream ignored = context.getAssets().open(assetPath)) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    protected void extractBox64Files() {
        ImageFs imageFs = environment.getImageFs();
        Context context = environment.getContext();

        // degrade to default if the shared preference is not set or is empty
        String box64Version = normalizeVersion(container.getBox64Version(), DefaultVersion.BOX64);

        if (shortcut != null)
            box64Version = normalizeVersion(shortcut.getExtra("box64Version", shortcut.container.getBox64Version()), box64Version);

        Log.d("GuestProgramLauncherComponent", "box64Version: " + box64Version);

        File rootDir = imageFs.getRootDir();
        File box64Binary = new File(rootDir, "usr/bin/box64");
        File localBox64Binary = new File(rootDir, "usr/local/bin/box64");
        boolean payloadMissing = needsFileRefresh(box64Binary, localBox64Binary);

        if (payloadMissing || !box64Version.equals(container.getExtra("box64Version"))) {
            ContentProfile profile = resolveInstalledContentProfile(ContentProfile.ContentType.CONTENT_TYPE_BOX64, box64Version);
            String embeddedArchive = resolveEmbeddedBox64Archive(context, imageFs, box64Version);
            ForensicLogger.logEvent(
                    context,
                    "info",
                    "BOX64_PAYLOAD_REFRESH",
                    null,
                    "launch_dependency",
                    "box64_payload_refresh",
                    ForensicLogger.fields(
                            "version", box64Version,
                            "payload_missing", payloadMissing,
                            "profile_found", profile != null,
                            "profile_entry", profile != null ? ContentsManager.getEntryName(profile) : "",
                            "embedded_archive", embeddedArchive
                    )
            );
            boolean appliedProfile = profile != null && contentsManager.applyContent(profile);
            if (!appliedProfile)
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, embeddedArchive, rootDir);
            container.putExtra("box64Version", box64Version);
            container.saveData();
        }

        // Set execute permissions for box64 just in case
        if (box64Binary.exists()) {
            FileUtils.chmod(box64Binary, 0755);
        }
        if (localBox64Binary.exists()) {
            FileUtils.chmod(localBox64Binary, 0755);
        }
    }

    protected void extractEmulatorsDlls() {;
        Context context = environment.getContext();
        ImageFs imageFs = environment.getImageFs();
        File system32dir = resolveArm64EcSystem32Dir(imageFs);
        boolean containerDataChanged = false;

        String wowbox64Version = normalizeVersion(container.getBox64Version(), DefaultVersion.WOWBOX64);
        String fexcoreVersion = normalizeVersion(container.getFEXCoreVersion(), DefaultVersion.FEXCORE);

        if (shortcut != null) {
            wowbox64Version = normalizeVersion(shortcut.getExtra("box64Version", shortcut.container.getBox64Version()), wowbox64Version);
        }

        Log.d("GuestProgramLauncherComponent", "box64Version in use: " + wowbox64Version);
        Log.d("GuestProgramLauncherComponent", "fexcoreVersion in use: " + fexcoreVersion);

        File wowbox64Dll = new File(system32dir, "wowbox64.dll");
        boolean wowbox64Missing = needsFileRefresh(wowbox64Dll);
        if (wowbox64Missing || !wowbox64Version.equals(container.getExtra("box64Version"))) {
            ContentProfile profile = resolveInstalledContentProfile(ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64, wowbox64Version);
            ForensicLogger.logEvent(
                    context,
                    "info",
                    "WOWBOX64_PAYLOAD_REFRESH",
                    null,
                    "launch_dependency",
                    "wowbox64_payload_refresh",
                    ForensicLogger.fields(
                            "version", wowbox64Version,
                            "payload_missing", wowbox64Missing,
                            "profile_found", profile != null,
                            "profile_entry", profile != null ? ContentsManager.getEntryName(profile) : ""
                    )
            );
            boolean appliedProfile = profile != null && contentsManager.applyContent(profile);
            if (!appliedProfile)
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, environment.getContext(), "wowbox64/wowbox64-" + wowbox64Version + ".tzst", system32dir);
            container.putExtra("box64Version", wowbox64Version);
            containerDataChanged = true;
        }

        File fexArm64ecDll = new File(system32dir, "libarm64ecfex.dll");
        File fexWow64Dll = new File(system32dir, "libwow64fex.dll");
        boolean fexPayloadMissing = needsFileRefresh(fexArm64ecDll, fexWow64Dll);
        if (fexPayloadMissing || !fexcoreVersion.equals(container.getExtra("fexcoreVersion"))) {
            ContentProfile profile = resolveInstalledContentProfile(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE, fexcoreVersion);
            ForensicLogger.logEvent(
                    context,
                    "info",
                    "FEXCORE_PAYLOAD_REFRESH",
                    null,
                    "launch_dependency",
                    "fexcore_payload_refresh",
                    ForensicLogger.fields(
                            "version", fexcoreVersion,
                            "payload_missing", fexPayloadMissing,
                            "profile_found", profile != null,
                            "profile_entry", profile != null ? ContentsManager.getEntryName(profile) : ""
                    )
            );
            boolean appliedProfile = profile != null && contentsManager.applyContent(profile);
            if (!appliedProfile)
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, environment.getContext(), "fexcore/fexcore-" + fexcoreVersion + ".tzst", system32dir);
            container.putExtra("fexcoreVersion", fexcoreVersion);
            containerDataChanged = true;
        }
        if (containerDataChanged) container.saveData();
    }

    public GuestProgramLauncherComponent(ContentsManager contentsManager, ContentProfile wineProfile, Shortcut shortcut) {
        this.contentsManager = contentsManager;
        this.wineProfile = wineProfile;
        this.shortcut = shortcut;
    }

    @Override
    public void start() {
        synchronized (lock) {
            if (container == null) {
                Log.w("GuestProgramLauncherComponent", "Container is null, skipping guest program start");
                return;
            }
            bindActiveContainerHome(environment.getImageFs());
            String appId = resolveLaunchAppId();
            String traceId = resolveForensicTraceIdHint();
            logLaunchStageEvent(
                    environment.getContext(),
                    "info",
                    "LAUNCH_STAGE_START",
                    traceId,
                    "launcher_start",
                    appId,
                    -1L,
                    null,
                    "wine_info_present", wineInfo != null
            );
            if (wineInfo == null) {
                wineInfo = WineInfo.fromIdentifier(
                        environment.getContext(),
                        contentsManager,
                        container.getWineVersion(),
                        resolveRequestedRuntimeModel()
                );
            }
            LaunchDependencyRegistry.DependencyRunResult dependencyResult =
                    LaunchDependencyRegistry.runDependencies(environment.getContext(), contentsManager, container, shortcut, appId, traceId);
            if (!dependencyResult.success) {
                Log.e("GuestProgramLauncherComponent", "Launch dependencies failed: " + dependencyResult.dependencyId + " / " + dependencyResult.message);
                String failMessage = dependencyResult.message == null || dependencyResult.message.trim().isEmpty()
                        ? "Missing launch dependency: " + dependencyResult.dependencyId
                        : dependencyResult.message;
                AppUtils.showToast(environment.getContext(), failMessage);
                if (terminationCallback != null) terminationCallback.call(-1);
                return;
            }
            if (wineInfo.isArm64EC()) {
                if (!runLaunchStage(environment.getContext(), traceId, appId, "extract_emulators_dlls",
                        this::extractEmulatorsDlls,
                        "arm64ec", true)) {
                    failLaunchPreparation(environment.getContext(), traceId, appId, "Failed to refresh emulator payloads");
                    return;
                }
                if (requiresBox64ForArm64EcLaunch()) {
                    if (!runLaunchStage(environment.getContext(), traceId, appId, "extract_box64_files",
                            this::extractBox64Files,
                            "arm64ec", true,
                            "required_for_arm64ec", true)) {
                        failLaunchPreparation(environment.getContext(), traceId, appId, "Failed to refresh box64 payload");
                        return;
                    }
                }
            }
            else if (!runLaunchStage(environment.getContext(), traceId, appId, "extract_box64_files",
                    this::extractBox64Files,
                    "arm64ec", false)) {
                failLaunchPreparation(environment.getContext(), traceId, appId, "Failed to refresh box64 payload");
                return;
            }
            if (!runLaunchStage(environment.getContext(), traceId, appId, "prelaunch_steps",
                    () -> LaunchDependencyRegistry.runPreLaunchSteps(environment.getContext(), container, shortcut, appId, traceId, this))) {
                failLaunchPreparation(environment.getContext(), traceId, appId, "Pre-launch step failed");
                return;
            }
            if (!runLaunchStage(environment.getContext(), traceId, appId, "prelaunch_stale_wine_process_reap",
                    ProcessHelper::hardKillStaleWineProcesses)) {
                failLaunchPreparation(environment.getContext(), traceId, appId, "Failed to clear stale Wine processes before launch");
                return;
            }
            long execStartedAt = SystemClock.elapsedRealtime();
            try {
                pid = execGuestProgram();
            } catch (Exception e) {
                Log.e("GuestProgramLauncherComponent", "Guest runtime process threw before exec submit for " + appId, e);
                logLaunchStageEvent(
                        environment.getContext(),
                        "error",
                        "LAUNCH_STAGE_FAILED",
                        traceId,
                        "exec_guest_program",
                        appId,
                        SystemClock.elapsedRealtime() - execStartedAt,
                        e
                );
                failLaunchPreparation(environment.getContext(), traceId, appId, "Failed to prepare Wine runtime launch");
                return;
            }
            if (pid == -1) {
                Log.e("GuestProgramLauncherComponent", "Guest runtime process failed to start for " + appId);
                logLaunchStageEvent(
                        environment.getContext(),
                        "error",
                        "LAUNCH_STAGE_FAILED",
                        traceId,
                        "exec_guest_program",
                        appId,
                        SystemClock.elapsedRealtime() - execStartedAt,
                        null,
                        "pid", pid,
                        "failure_reason", "exec_guest_program_returned_negative_pid"
                );
                AppUtils.showToast(environment.getContext(), "Failed to start Wine runtime process");
                if (terminationCallback != null) terminationCallback.call(-1);
                return;
            }
            logLaunchStageEvent(
                    environment.getContext(),
                    "info",
                    "LAUNCH_STAGE_DONE",
                    traceId,
                    "exec_guest_program",
                    appId,
                    SystemClock.elapsedRealtime() - execStartedAt,
                    null,
                    "pid", pid
            );
        }
    }


    @Override
    public void stop() {
        synchronized (lock) {
            if (pid != -1) {
                ProcessHelper.killProcessTree(pid);
                pid = -1;
            }
        }
    }

    public Callback<Integer> getTerminationCallback() {
        return terminationCallback;
    }

    public void setTerminationCallback(Callback<Integer> terminationCallback) {
        this.terminationCallback = terminationCallback;
    }

    public String getGuestExecutable() {
        return guestExecutable;
    }

    public void setGuestExecutable(String guestExecutable) {
        this.guestExecutable = guestExecutable;
    }

    public String[] getBindingPaths() {
        return bindingPaths;
    }

    public void setBindingPaths(String[] bindingPaths) {
        if (bindingPaths == null || bindingPaths.length == 0) {
            this.bindingPaths = null;
            return;
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String path : bindingPaths) {
            if (path == null || path.trim().isEmpty()) continue;
            try {
                String normalizedPath = Paths.get(path.trim()).toAbsolutePath().normalize().toString();
                File candidate = new File(normalizedPath);
                if (candidate.exists() && candidate.isDirectory()) {
                    normalized.add(normalizedPath);
                }
            } catch (Exception ignored) {
            }
        }
        this.bindingPaths = normalized.isEmpty() ? null : normalized.toArray(new String[0]);
    }

    protected String buildRuntimePath(ImageFs imageFs, File rootDir, String winePath) {
        LinkedHashSet<String> segments = new LinkedHashSet<>();
        if (winePath != null && !winePath.trim().isEmpty()) {
            segments.add(winePath.trim());
        }

        File usrLocalBin = imageFs.getLocalBinDir();
        if (usrLocalBin.exists() && usrLocalBin.isDirectory()) {
            segments.add(usrLocalBin.getPath());
        }

        File glibcBin = new File(rootDir, "usr/glibc/bin");
        if (glibcBin.exists() && glibcBin.isDirectory()) {
            segments.add(glibcBin.getPath());
        }

        File usrBin = new File(rootDir, "usr/bin");
        if (usrBin.exists() && usrBin.isDirectory()) {
            segments.add(usrBin.getPath());
        }

        segments.add("/system/bin");
        return String.join(":", segments);
    }

    protected boolean requiresBox64ForArm64EcLaunch() {
        return false;
    }

    protected void applyMoboxRuntimeContracts(ImageFs imageFs, EnvVars launchEnv, File rootDir, String winePath) {
        launchEnv.put("PATH", buildRuntimePath(imageFs, rootDir, winePath));

        File runtimeTmpDir = imageFs.getTmpDir();
        if (!runtimeTmpDir.exists()) {
            runtimeTmpDir.mkdirs();
        }
        String runtimeTmpPath = runtimeTmpDir.getPath();
        if (!launchEnv.has("TMPDIR")) launchEnv.put("TMPDIR", runtimeTmpPath);
        if (!launchEnv.has("transient")) launchEnv.put("transient", runtimeTmpPath);
        if (!launchEnv.has("TMP")) launchEnv.put("TMP", runtimeTmpPath);

        boolean hasGlibcBin = new File(rootDir, "usr/glibc/bin").isDirectory();
        launchEnv.put("AERO_RUNTIME_BOOTSTRAP_MODEL", "contents_contract");
        launchEnv.put("AERO_RUNTIME_COMPONENT_MODEL", "wcp_contents");
        launchEnv.put("AERO_RUNTIME_MOBOX_PATH_COMPAT", hasGlibcBin ? "1" : "0");
    }

    protected void applyRuntimePathContracts(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv, String winePath) {
        if (context == null || imageFs == null || rootDir == null) return;

        String filesDirPath = context.getFilesDir().getAbsolutePath();
        launchEnv.put("AERO_RUNTIME_PACKAGE_NAME", context.getPackageName());
        launchEnv.put("AERO_RUNTIME_FILES_PATH", filesDirPath);
        launchEnv.put("AERO_RUNTIME_ROOTFS_PATH", rootDir.getAbsolutePath());
        launchEnv.put("AERO_RUNTIME_TMP_PATH", imageFs.getTmpDir().getAbsolutePath());
        launchEnv.put("AERO_RUNTIME_WINE_PATH", winePath);
        launchEnv.put("AERO_RUNTIME_ANDROID_HOST_LIB_PATH", imageFs.getAndroidHostLibDir().getAbsolutePath());
        applyXlibPathContracts(context, rootDir, launchEnv);
        if (shouldEnableRuntimeRedirectDebug(launchEnv)) {
            launchEnv.put("AERO_REDIRECT_DEBUG", "1");
        } else if (!launchEnv.has("AERO_REDIRECT_DEBUG")) {
            launchEnv.put("AERO_REDIRECT_DEBUG", "0");
        }
    }

    private void applyXlibPathContracts(Context context, File rootDir, EnvVars launchEnv) {
        if (rootDir == null || launchEnv == null) return;
        File x11ShareDir = new File(rootDir, "usr/share/X11");
        File x11LocaleDir = new File(x11ShareDir, "locale");
        File x11KeysymDb = new File(x11ShareDir, "XKeysymDB");
        File x11ErrorDb = new File(x11ShareDir, "XErrorDB");
        File x11XcmsDb = new File(x11ShareDir, "Xcms.txt");
        File termuxX11Socket = new File(rootDir, "usr/tmp/.X11-unix/X0");

        if (x11LocaleDir.isDirectory()) launchEnv.put("XLOCALEDIR", x11LocaleDir.getAbsolutePath());
        if (x11KeysymDb.isFile()) launchEnv.put("XKEYSYMDB", x11KeysymDb.getAbsolutePath());
        if (x11ErrorDb.isFile()) launchEnv.put("XERRORDB", x11ErrorDb.getAbsolutePath());
        if (x11XcmsDb.isFile()) launchEnv.put("XCMSDB", x11XcmsDb.getAbsolutePath());

        ForensicLogger.logEvent(
                context,
                "info",
                "XLIB_PATH_CONTRACT_APPLIED",
                null,
                "guest_program_launcher",
                "xlib_path_contract_applied",
                ForensicLogger.fields(
                        "xlocale_dir", launchEnv.get("XLOCALEDIR"),
                        "xlocale_dir_present", x11LocaleDir.isDirectory(),
                        "xkeysymdb", launchEnv.get("XKEYSYMDB"),
                        "xkeysymdb_present", x11KeysymDb.isFile(),
                        "xerrordb", launchEnv.get("XERRORDB"),
                        "xerrordb_present", x11ErrorDb.isFile(),
                        "xcmsdb", launchEnv.get("XCMSDB"),
                        "xcmsdb_present", x11XcmsDb.isFile(),
                        "termux_hardcoded_socket_source", "/data/data/com.termux/files/usr/tmp/.X11-unix/X0",
                        "termux_hardcoded_socket_target", termuxX11Socket.getAbsolutePath(),
                        "termux_hardcoded_socket_target_present", termuxX11Socket.exists()
                )
        );
    }

    protected boolean shouldEnableRuntimeRedirectDebug(EnvVars launchEnv) {
        if (launchEnv == null) return false;
        return "1".equals(launchEnv.get("AERO_FORENSIC_MODE").trim())
                || !launchEnv.get("AERO_FORENSIC_TRACE_ID").trim().isEmpty()
                || "1".equals(launchEnv.get("AERO_RUNTIME_FORENSIC_DEBUG").trim());
    }

    private void applyRuntimeRedirectDebugContract(Context context, EnvVars launchEnv, String traceId, String appId) {
        if (!shouldEnableRuntimeRedirectDebug(launchEnv)) return;
        String previous = launchEnv.get("AERO_REDIRECT_DEBUG");
        launchEnv.put("AERO_REDIRECT_DEBUG", "1");
        logLaunchStageEvent(
                context,
                "info",
                "RUNTIME_REDIRECT_FORENSIC_DEBUG_ENABLED",
                traceId,
                "runtime_redirect_forensic_debug",
                appId,
                -1L,
                null,
                "previous_redirect_debug", previous,
                "redirect_debug", launchEnv.get("AERO_REDIRECT_DEBUG"),
                "forensic_mode", launchEnv.get("AERO_FORENSIC_MODE"),
                "forensic_trace_id_present", !launchEnv.get("AERO_FORENSIC_TRACE_ID").trim().isEmpty()
        );
    }

    private boolean shouldDisableFullscreenHack() {
        if (shortcut != null && !shortcut.getExtra("fullscreenStretched").isEmpty()) {
            return shortcut.getExtraBoolean("fullscreenStretched", false);
        }
        return container != null && container.isFullscreenStretched();
    }

    public EnvVars getEnvVars() {
        return envVars;
    }

    public void setEnvVars(EnvVars envVars) {
        this.envVars = envVars;
    }

    public String getBox64Preset() {
        return box64Preset;
    }

    public void setBox64Preset(String box64Preset) {
        this.box64Preset = box64Preset;
    }

    public void setFEXCorePreset (String fexcorePreset) { this.fexcorePreset = fexcorePreset; }

    private boolean isDesktopShellBootstrap() {
        if (shortcut != null || guestExecutable == null) return false;
        String lowered = guestExecutable.toLowerCase(Locale.ROOT);
        return lowered.contains("explorer /desktop=shell")
                || lowered.contains("explorer.exe /desktop=shell")
                || lowered.startsWith("wine winhandler.exe")
                || lowered.startsWith("wine \"winhandler.exe\"")
                || lowered.startsWith("wine c:\\windows\\winhandler.exe")
                || lowered.startsWith("wine \"c:\\windows\\winhandler.exe\"")
                || lowered.contains(" winhandler.exe \"wfm.exe\"")
                || lowered.contains("\\winhandler.exe\" \"")
                || lowered.contains("\\winhandler.exe ");
    }

    private String resolveRequestedEmulator() {
        String emulator = container.getEmulator();
        if (shortcut != null) {
            emulator = shortcut.getExtra("emulator", container.getEmulator());
        }
        return emulator == null ? "" : emulator;
    }

    protected File getArm64EcSystem32Dir(ImageFs imageFs) {
        return resolveArm64EcSystem32Dir(imageFs);
    }

    protected File resolveArm64EcSystem32Dir(ImageFs imageFs) {
        if (imageFs == null) return null;
        return new File(WineUtils.resolveHostWineDriveCRoot(imageFs.getRootDir()), "windows/system32");
    }

    protected boolean hasWowbox64Payload(ImageFs imageFs) {
        File system32Dir = getArm64EcSystem32Dir(imageFs);
        return system32Dir != null && new File(system32Dir, "wowbox64.dll").isFile();
    }

    protected boolean hasFexArm64EcPayload(ImageFs imageFs) {
        File system32Dir = getArm64EcSystem32Dir(imageFs);
        if (system32Dir == null) return false;
        return new File(system32Dir, "libwow64fex.dll").isFile()
                && new File(system32Dir, "libarm64ecfex.dll").isFile();
    }

    protected boolean isDesktopShellBootstrapLaunch() {
        return isDesktopShellBootstrap();
    }

    protected String resolveEffectiveArm64EcEmulator() {
        ImageFs imageFs = environment != null ? environment.getImageFs() : null;
        return resolveEffectiveEmulator(imageFs, resolveRequestedEmulator(), isDesktopShellBootstrap());
    }

    protected boolean shouldUseDirectArm64EcGuestLaunch(ImageFs imageFs, String effectiveEmulator, boolean desktopShellBootstrap) {
        if (wineInfo == null || !wineInfo.isArm64EC() || imageFs == null) {
            return false;
        }

        if ("wowbox64".equalsIgnoreCase(effectiveEmulator)) {
            return hasWowbox64Payload(imageFs);
        }
        if ("fexcore".equalsIgnoreCase(effectiveEmulator)) {
            return hasFexArm64EcPayload(imageFs);
        }
        return false;
    }

    protected String resolveEffectiveEmulator(ImageFs imageFs, String requestedEmulator, boolean desktopShellBootstrap) {
        String normalizedRequested = requestedEmulator == null ? "" : requestedEmulator.trim();
        if (wineInfo == null || !wineInfo.isArm64EC()) return normalizedRequested;
        if (!"fexcore".equalsIgnoreCase(normalizedRequested)) return normalizedRequested;
        if (hasFexArm64EcPayload(imageFs)) return "fexcore";
        if (hasWowbox64Payload(imageFs)) return "wowbox64";
        return normalizedRequested;
    }

    protected String getLauncherModel(ImageFs imageFs) {
        String requestedRuntimeModel = resolveRequestedRuntimeModel();
        if (!requestedRuntimeModel.isEmpty()) return requestedRuntimeModel;
        return imageFs != null ? imageFs.getRuntimeLibcModel() : "bionic";
    }

    protected String resolveRequestedRuntimeModel() {
        if (wineProfile != null) {
            String profileRuntimeModel = wineProfile.getRuntimeModel();
            if (!profileRuntimeModel.isEmpty()) return profileRuntimeModel;
        }
        if (container != null) {
            String containerRuntimeModel = ContentProfile.inferRuntimeModelFromEntryName(container.getWineVersion());
            if (!containerRuntimeModel.isEmpty()) return containerRuntimeModel;
            return ContentProfile.normalizeRuntimeModel(container.getContainerVariant());
        }
        return "";
    }

    protected void applyLauncherSpecificEnvVars(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv) {
        launchEnv.put("AERO_RUNTIME_LAUNCHER_MODEL", getLauncherModel(imageFs));
    }

    protected boolean usesAndroidBionicHostEnv(String effectiveEmulator, boolean desktopShellBootstrap) {
        return false;
    }

    protected void applyAndroidBionicHostEnv(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv) {
    }

    protected String buildGuestCommand(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv,
                                       String winePath, String effectiveEmulator, boolean desktopShellBootstrap) {
        String command = "";
        String overriddenCommand = launchEnv.get("GUEST_PROGRAM_LAUNCHER_COMMAND");
        if (!overriddenCommand.isEmpty()) {
            String[] parts = overriddenCommand.split(";");
            for (String part : parts) command += part + " ";
            return command.trim();
        }

        if (wineInfo.isArm64EC()) {
            boolean usesFexCore = effectiveEmulator.toLowerCase(Locale.ROOT).equals("fexcore");
            String hodll = usesFexCore
                    ? "libwow64fex.dll"
                    : "wowbox64.dll";
            launchEnv.put("HODLL", hodll);
            String hodll64 = usesFexCore ? "libarm64ecfex.dll" : "";
            if (hodll64.isEmpty()) {
                launchEnv.remove("HODLL64");
            } else {
                launchEnv.put("HODLL64", hodll64);
            }
            ForensicLogger.logEvent(
                    context,
                    "info",
                    "WINHANDLER_EMULATOR_FALLBACK",
                    null,
                    "guest_program_launcher",
                    "winhandler_emulator_fallback",
                    ForensicLogger.fields(
                            "requested_emulator", resolveRequestedEmulator(),
                            "effective_emulator", effectiveEmulator,
                            "desktop_shell_bootstrap", desktopShellBootstrap,
                            "guest_executable", guestExecutable != null ? guestExecutable : "",
                            "hodll", hodll,
                            "hodll64", hodll64
                    )
            );
            if (shouldUseWineBinaryLauncher(guestExecutable, desktopShellBootstrap)) {
                return winePath + "/wine " + guestExecutable;
            }
            return winePath + "/" + guestExecutable;
        }
        return imageFs.getBinDir() + "/box64 " + guestExecutable;
    }

    private boolean shouldUseWineBinaryLauncher(String guestExecutable, boolean desktopShellBootstrap) {
        if (desktopShellBootstrap) return false;
        String normalized = guestExecutable != null ? guestExecutable.trim() : "";
        if (normalized.isEmpty()) return false;
        String token = firstGuestToken(normalized).toLowerCase(Locale.ROOT);
        if (token.isEmpty()) return false;
        return token.endsWith(".exe")
                || token.endsWith(".cmd")
                || token.endsWith(".bat")
                || token.endsWith(".vbs")
                || token.endsWith(".js")
                || token.endsWith(".msi")
                || token.contains(":\\");
    }

    private String firstGuestToken(String command) {
        if (command == null) return "";
        String normalized = command.trim();
        if (normalized.isEmpty()) return "";
        boolean quoted = normalized.startsWith("\"");
        StringBuilder builder = new StringBuilder();
        for (int i = quoted ? 1 : 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (quoted) {
                if (ch == '"') break;
                builder.append(ch);
            } else {
                if (Character.isWhitespace(ch)) break;
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    protected int execGuestProgram() {
        return execGuestProgramInternal(true, terminationCallback);
    }

    public int launchDetachedGuestProgram(String guestExecutableOverride, Callback<Integer> detachedTerminationCallback) {
        String normalizedGuestExecutable = guestExecutableOverride != null ? guestExecutableOverride.trim() : "";
        if (normalizedGuestExecutable.isEmpty()) return -1;
        synchronized (lock) {
            String originalGuestExecutable = guestExecutable;
            guestExecutable = normalizedGuestExecutable;
            try {
                return execGuestProgramInternal(false, detachedTerminationCallback);
            } finally {
                guestExecutable = originalGuestExecutable;
            }
        }
    }

    private int execGuestProgramInternal(boolean trackPrimaryPid, Callback<Integer> activeTerminationCallback) {
        Context context = environment.getContext();
        ImageFs imageFs = environment.getImageFs();
        bindActiveContainerHome(imageFs);
        File rootDir = imageFs.getRootDir();
        String appId = resolveLaunchAppId();
        String stageTraceId = resolveForensicTraceIdHint();
        if (!runLaunchStage(context, stageTraceId, appId, "ensure_rootfs_launch_layout",
                () -> ImageFsInstaller.ensureRootfsLaunchLayout(context, imageFs))) {
            return -1;
        }
        if (!runLaunchStage(context, stageTraceId, appId, "ensure_bionic_host_support",
                () -> ImageFsInstaller.ensureBionicHostSupport(context, imageFs))) {
            return -1;
        }
        if (!runLaunchStage(context, stageTraceId, appId, "ensure_app_native_guest_libs",
                () -> ImageFsInstaller.ensureAppNativeGuestLibs(context, imageFs))) {
            return -1;
        }
        if (!runLaunchStage(context, stageTraceId, appId, "ensure_prefix_pack_toolkit",
                () -> ImageFsInstaller.ensurePrefixPackToolkit(context, imageFs))) {
            return -1;
        }

        bindLaunchRuntimeWinePath(context, imageFs, stageTraceId, appId);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enableBox64Logs = preferences.getBoolean("enable_box64_logs", false);
        boolean openWithAndroidBrowser = preferences.getBoolean("open_with_android_browser", false);
        boolean shareAndroidClipboard = preferences.getBoolean("share_android_clipboard", false);

        EnvVars launchEnv = new EnvVars();
        String requestedEmulator = resolveRequestedEmulator();
        boolean desktopShellBootstrap = wineInfo.isArm64EC() && isDesktopShellBootstrap();
        String effectiveEmulator = resolveEffectiveEmulator(imageFs, requestedEmulator, desktopShellBootstrap);

        addBox64EnvVars(launchEnv, enableBox64Logs);
        if ("fexcore".equalsIgnoreCase(effectiveEmulator)) {
            launchEnv.putAll(FEXCorePresetManager.getEnvVars(context, fexcorePreset));
        } else {
            launchEnv.remove("FEX_TSOENABLED");
            launchEnv.remove("FEX_VECTORTSOENABLED");
            launchEnv.remove("FEX_MEMCPYSETTSOENABLED");
            launchEnv.remove("FEX_HALFBARRIERTSOENABLED");
            launchEnv.remove("FEX_X87REDUCEDPRECISION");
            launchEnv.remove("FEX_MULTIBLOCK");
            launchEnv.remove("FEX_MAXINST");
            launchEnv.remove("FEX_FORCESVEWIDTH");
            launchEnv.remove("FEX_HOSTFEATURES");
            launchEnv.remove("FEX_SMALLTSCSCALE");
            launchEnv.remove("FEX_SMC_CHECKS");
            launchEnv.remove("FEX_SMCCHECKS");
            launchEnv.remove("FEX_VOLATILEMETADATA");
            launchEnv.remove("FEX_MONOHACKS");
            launchEnv.remove("FEX_HIDEHYPERVISORBIT");
            launchEnv.remove("FEX_DISABLEL2CACHE");
            launchEnv.remove("FEX_DYNAMICL1CACHE");
            launchEnv.remove("FEX_LOG_LEVEL");
            launchEnv.remove("FEX_OUTPUTLOG");
            launchEnv.remove("FEX_PROFILESTATS");
            launchEnv.remove("FEX_SILENTLOG");
            launchEnv.remove("FEX_DEBUG");
        }

        String renderer = GPUInformation.getRenderer(null, null);

        if (renderer.contains("Mali"))
            launchEnv.put("BOX64_MMAP32", "0");

        if (launchEnv.get("BOX64_MMAP32").equals("1") && !wineInfo.isArm64EC()) {
            Log.d("GuestProgramLauncherComponent", "Disabling map memory placed");
            launchEnv.put("WRAPPER_DISABLE_PLACED", "1");
        }

        String wineRootPath = imageFs.getWinePath();
        File runtimeRootDir = new File(wineRootPath);
        WineUtils.RuntimeLayout runtimeLayout = WineUtils.resolveRuntimeLayout(runtimeRootDir);
        File runtimeBinDir = runtimeLayout.binDir;
        File runtimeLibDir = runtimeLayout.libDir;
        File runtimeWineLibDir = runtimeLayout.wineLibDir;
        File runtimeWineUnixDir = WineUtils.resolveRuntimeWineUnixDir(runtimeRootDir, wineInfo);
        if (runtimeBinDir == null || runtimeLibDir == null || runtimeWineLibDir == null || runtimeWineUnixDir == null) {
            Log.e(
                    "GuestProgramLauncherComponent",
                    "Runtime layout incomplete for launch: root=" + wineRootPath
                            + " bin=" + (runtimeBinDir != null)
                            + " lib=" + (runtimeLibDir != null)
                            + " wineLib=" + (runtimeWineLibDir != null)
                            + " wineUnix=" + (runtimeWineUnixDir != null)
            );
            logLaunchStageEvent(
                    context,
                    "error",
                    "RUNTIME_LAYOUT_INCOMPLETE",
                    stageTraceId,
                    "runtime_layout_contract",
                    appId,
                    -1L,
                    null,
                    "runtime_root", wineRootPath,
                    "runtime_model", getLauncherModel(imageFs),
                    "wine_arch", wineInfo != null ? wineInfo.getArch() : "",
                    "bin_present", runtimeBinDir != null,
                    "lib_present", runtimeLibDir != null,
                    "wine_lib_present", runtimeWineLibDir != null,
                    "wine_unix_present", runtimeWineUnixDir != null
            );
            return -1;
        }
        String wineBinPath = runtimeBinDir.getPath();
        String wineLibPath = runtimeLibDir.getPath();
        String wineLib64Path = wineRootPath + "/lib64";
        String wineUnixPath = runtimeWineUnixDir.getPath();
        String wineDllPath = runtimeWineLibDir.getPath();
        repairRuntimeExecutablePermissions(context, runtimeBinDir);
        String launcherModel = getLauncherModel(imageFs);
        WineUtils.RuntimeAbiContract abiContract = WineUtils.validateRuntimeAbiContract(
                rootDir,
                runtimeRootDir,
                wineInfo,
                launcherModel
        );
        File runtimeWineWindowsDir = WineUtils.resolveRuntimeWineWindowsDir(runtimeRootDir, wineInfo);
        logLaunchStageEvent(
                context,
                abiContract.complete ? "info" : "error",
                abiContract.complete ? "RUNTIME_ABI_CONTRACT_OK" : "RUNTIME_ABI_CONTRACT_FAILED",
                stageTraceId,
                "runtime_abi_contract",
                appId,
                -1L,
                null,
                "runtime_root", abiContract.runtimeRootPath,
                "runtime_model", abiContract.runtimeModel,
                "wine_arch", abiContract.arch,
                "required", abiContract.required,
                "complete", abiContract.complete,
                "reason", abiContract.reason,
                "missing", abiContract.missing,
                "wine_binary", abiContract.wineBinaryPath,
                "wine_unix_dir", abiContract.wineUnixDirPath,
                "wine_windows_dir", abiContract.wineWindowsDirPath,
                "glibc_loader", abiContract.glibcLoaderPath,
                "glibc_libc", abiContract.glibcLibcPath,
                "glibc_loader_rejected", abiContract.glibcLoaderRejectedPath,
                "glibc_libc_rejected", abiContract.glibcLibcRejectedPath,
                "glibc_guest_loader_mode", abiContract.glibcGuestLoaderMode,
                "glibc_guest_support", abiContract.glibcGuestSupportPath,
                "glibc_guest_support_rejected", abiContract.glibcGuestSupportRejectedPath
        );
        if (!abiContract.complete) {
            return -1;
        }
        if (ContentProfile.RUNTIME_MODEL_GLIBC.equalsIgnoreCase(launcherModel)) {
            applyGlibcActiveWineOverlay(context, imageFs, runtimeRootDir, runtimeWineLibDir, stageTraceId, appId);
            logLaunchStageEvent(
                    context,
                    "info",
                    "GLIBC_LAUNCH_BROAD_REPAIR_DEFERRED",
                    stageTraceId,
                    "glibc_launch_broad_repair_deferred",
                    appId,
                    -1L,
                    null,
                    "runtime_root", runtimeRootDir.getAbsolutePath(),
                    "reason", "install_and_content_sync_own_imagefs_path_and_elf_interpreter_repair"
            );
        }

        // Setting up essential environment variables for Wine
        launchEnv.put("HOME", imageFs.home_path);
        launchEnv.put("USER", ImageFs.USER);
        launchEnv.put("TMPDIR", imageFs.getTmpDir().getPath());
        launchEnv.put("XDG_DATA_DIRS", rootDir.getPath() + "/usr/share");
        launchEnv.put(
                "LD_LIBRARY_PATH",
                wineLibPath + ":" + wineLib64Path + ":" + wineUnixPath + ":" + rootDir.getPath() + "/usr/lib" + ":" + rootDir.getPath() + "/usr/lib64" + ":" + "/system/lib64"
        );
        String wineDllSearchPath = buildRuntimeWineDllPath(wineDllPath, wineUnixPath, wineInfo);
        launchEnv.put("WINEDLLPATH", wineDllSearchPath);
        launchEnv.put("XDG_CONFIG_DIRS", rootDir.getPath() + "/usr/etc/xdg");
        launchEnv.put("GST_PLUGIN_PATH", rootDir.getPath() + "/usr/lib/gstreamer-1.0");
        launchEnv.put("FONTCONFIG_PATH", rootDir.getPath() + "/usr/etc/fonts");
        launchEnv.put("VK_LAYER_PATH", rootDir.getPath() + "/usr/share/vulkan/implicit_layer.d" + ":" + rootDir.getPath() + "/usr/share/vulkan/explicit_layer.d");
        launchEnv.put("WRAPPER_LAYER_PATH", rootDir.getPath() + "/usr/lib");
        launchEnv.put("WRAPPER_CACHE_PATH", rootDir.getPath() + "/usr/var/cache");
        if (desktopShellBootstrap) {
            launchEnv.remove("WINE_NO_DUPLICATE_EXPLORER");
        } else {
            launchEnv.put("WINE_NO_DUPLICATE_EXPLORER", "1");
        }
        launchEnv.put("PREFIX", rootDir.getPath() + "/usr");
        launchEnv.put("DISPLAY", ":0");
        if (shouldDisableFullscreenHack()) {
            launchEnv.put("WINE_DISABLE_FULLSCREEN_HACK", "1");
        } else {
            launchEnv.remove("WINE_DISABLE_FULLSCREEN_HACK");
        }
        launchEnv.put("GST_PLUGIN_FEATURE_RANK", "ximagesink:3000");
        launchEnv.put("ALSA_CONFIG_PATH", rootDir.getPath() + "/usr/share/alsa/alsa.conf" + ":" + rootDir.getPath() + "/usr/etc/alsa/conf.d/android_aserver.conf");
        launchEnv.put("ALSA_PLUGIN_DIR", rootDir.getPath() + "/usr/lib/alsa-lib");
        launchEnv.put("OPENSSL_CONF", rootDir.getPath() + "/usr/etc/tls/openssl.cnf");
        launchEnv.put("SSL_CERT_FILE", rootDir.getPath() + "/usr/etc/tls/cert.pem");
        launchEnv.put("SSL_CERT_DIR", rootDir.getPath() + "/usr/etc/tls/certs");
        launchEnv.put("WINE_X11FORCEGLX", "1");
        launchEnv.put("WINE_GST_NO_GL", "1");
        applySmartphoneWineDriverContract(context, launchEnv, desktopShellBootstrap, launcherModel);
        launchEnv.put("SteamGameId", "0");
        launchEnv.put("PROTON_AUDIO_CONVERT", "0");
        launchEnv.put("PROTON_VIDEO_CONVERT", "0");
        launchEnv.put("PROTON_DEMUX", "0");

        String winePath = wineBinPath;

        Log.d("GuestProgramLauncherComponent", "WinePath is " + winePath);

        applyMoboxRuntimeContracts(imageFs, launchEnv, rootDir, winePath);
        applyRuntimePathContracts(context, imageFs, rootDir, launchEnv, wineRootPath);
        launchEnv.put("AERO_RUNTIME_WINE_BIN_PATH", wineBinPath);
        launchEnv.put("AERO_RUNTIME_WINE_LIB_PATH", wineLibPath);
        launchEnv.put("AERO_RUNTIME_WINE_DLL_PATH", wineDllPath);
        launchEnv.put("AERO_RUNTIME_WINE_UNIX_PATH", wineUnixPath);
        logLaunchStageEvent(
                context,
                "info",
                "RUNTIME_WINE_DLLPATH_CONTRACT_APPLIED",
                stageTraceId,
                "runtime_winedllpath_contract",
                appId,
                -1L,
                null,
                "runtime_root", wineRootPath,
                "wine_dll_path", wineDllPath,
                "wine_unix_path", wineUnixPath,
                "winedllpath_head", summarizePathHead(wineDllSearchPath, 8),
                "contains_unix_path", wineDllSearchPath.contains(wineUnixPath),
                "winex11_unix_present", new File(wineUnixPath, "winex11.so").isFile(),
                "wine_windows_path", runtimeWineWindowsDir != null ? runtimeWineWindowsDir.getPath() : "",
                "contains_windows_path", runtimeWineWindowsDir != null && wineDllSearchPath.contains(runtimeWineWindowsDir.getPath()),
                "winex11_pe_present", runtimeWineWindowsDir != null && new File(runtimeWineWindowsDir, "winex11.drv").isFile()
        );


        launchEnv.put("ANDROID_SYSVSHM_SERVER", rootDir.getPath() + UnixSocketConfig.SYSVSHM_SERVER_PATH);

        PrimaryDnsResolution dnsResolution = resolvePrimaryDns(context);
        launchEnv.put("ANDROID_RESOLV_DNS", dnsResolution.address);
        logLaunchStageEvent(
                context,
                "info",
                "LAUNCH_DNS_READY",
                stageTraceId,
                "resolve_primary_dns",
                appId,
                -1L,
                null,
                "primary_dns", dnsResolution.address,
                "dns_source", dnsResolution.source,
                "active_network_present", dnsResolution.activeNetworkPresent,
                "link_properties_present", dnsResolution.linkPropertiesPresent,
                "dns_server_count", dnsResolution.dnsServerCount
        );
        launchEnv.put("WINE_NEW_NDIS", "1");

        StringBuilder ownedLdPreload = new StringBuilder();
        appendFileIfExists(ownedLdPreload, new File(imageFs.getLibDir(), "libandroid-sysvshm.so"));
        File fakeInputLibrary = applyFakeEvdevRuntimeBridge(context, imageFs, launchEnv);
        appendFileIfExists(ownedLdPreload, fakeInputLibrary);
        appendAndroidOemCryptoLdPreload(ownedLdPreload, imageFs);
        launchEnv.put("LD_PRELOAD", ownedLdPreload.toString());
        ensureRuntimeSdlCompatLink(context, imageFs);
        applyProtonControllerBridgeEnv(context, imageFs, launchEnv);
        mergeExternalEnvVars(launchEnv, ownedLdPreload.toString(), launchEnv.get("FAKE_EVDEV_DIR"));
        FEXCorePresetManager.normalizeSmcChecksEnvVars(launchEnv, this.envVars);
        applyRuntimeRedirectDebugContract(context, launchEnv, stageTraceId, appId);

        if (openWithAndroidBrowser) {
            launchEnv.put("WINE_OPEN_WITH_ANDROID_BROWSER", "1");
        } else {
            launchEnv.remove("WINE_OPEN_WITH_ANDROID_BROWSER");
        }
        if (shareAndroidClipboard) {
            launchEnv.put("WINE_FROM_ANDROID_CLIPBOARD", "1");
            launchEnv.put("WINE_TO_ANDROID_CLIPBOARD", "1");
        } else {
            launchEnv.remove("WINE_FROM_ANDROID_CLIPBOARD");
            launchEnv.remove("WINE_TO_ANDROID_CLIPBOARD");
        }
        launchEnv.put("AERO_RUNTIME_BROWSER_BRIDGE", openWithAndroidBrowser ? "1" : "0");
        launchEnv.put("AERO_RUNTIME_CLIPBOARD_SYNC", shareAndroidClipboard ? "1" : "0");
        applyLauncherSpecificEnvVars(context, imageFs, rootDir, launchEnv);
        if (usesAndroidBionicHostEnv(effectiveEmulator, desktopShellBootstrap)) {
            applyAndroidBionicHostEnv(context, imageFs, rootDir, launchEnv);
            applyDesktopShellX11BootstrapIsolation(context, imageFs, rootDir, launchEnv, stageTraceId, appId, effectiveEmulator, desktopShellBootstrap);
            logAndroidBionicHostFinalEnv(
                    context,
                    imageFs,
                    rootDir,
                    launchEnv,
                    stageTraceId,
                    appId,
                    effectiveEmulator,
                    desktopShellBootstrap
            );
        }

        if (launchEnv.has("MANGOHUD")) {
            launchEnv.remove("MANGOHUD");
        }
        if (launchEnv.has("MANGOHUD_CONFIG")) {
            launchEnv.remove("MANGOHUD_CONFIG");
        }

        if (bindingPaths != null && bindingPaths.length > 0) {
            launchEnv.put("AESO_BIND_PATHS", String.join(":", bindingPaths));
            launchEnv.put("AESO_BIND_PATH_COUNT", String.valueOf(bindingPaths.length));
        }

        long buildCommandStartedAt = SystemClock.elapsedRealtime();
        String command;
        try {
            command = buildGuestCommand(context, imageFs, rootDir, launchEnv, winePath, effectiveEmulator, desktopShellBootstrap);
        } catch (Exception e) {
            Log.e("GuestProgramLauncherComponent", "Unable to build guest command", e);
            logLaunchStageEvent(
                    context,
                    "error",
                    "LAUNCH_STAGE_FAILED",
                    stageTraceId,
                    "build_guest_command",
                    appId,
                    SystemClock.elapsedRealtime() - buildCommandStartedAt,
                    e,
                    "requested_emulator", requestedEmulator,
                    "effective_emulator", effectiveEmulator,
                    "desktop_shell_bootstrap", desktopShellBootstrap
            );
            return -1;
        }
        logLaunchStageEvent(
                context,
                "info",
                "LAUNCH_STAGE_DONE",
                stageTraceId,
                "build_guest_command",
                appId,
                SystemClock.elapsedRealtime() - buildCommandStartedAt,
                null,
                "requested_emulator", requestedEmulator,
                "effective_emulator", effectiveEmulator,
                "desktop_shell_bootstrap", desktopShellBootstrap,
                "env_hash", ForensicLogger.hashEnvVars(launchEnv)
        );
        String resolvedForensicTraceId = launchEnv.get("AERO_FORENSIC_TRACE_ID");
        if (resolvedForensicTraceId != null) {
            resolvedForensicTraceId = resolvedForensicTraceId.trim();
        }
        if (resolvedForensicTraceId != null && resolvedForensicTraceId.isEmpty()) {
            resolvedForensicTraceId = null;
        }
        final String forensicTraceId = resolvedForensicTraceId;

        // **Maybe remove this: Set execute permissions for box64 if necessary (Glibc/Proot artifact)
        File box64File = new File(rootDir, "usr/bin/box64");
        if (box64File.exists()) {
            FileUtils.chmod(box64File, 0755);
        }

        ForensicLogger.logEvent(
                context,
                "info",
                "LAUNCH_EXEC_SUBMIT",
                forensicTraceId,
                "guest_program_launcher",
                "launch_exec_submit",
                ForensicLogger.fields(
                        "track_primary_pid", trackPrimaryPid,
                        "desktop_shell_bootstrap", desktopShellBootstrap,
                        "requested_emulator", requestedEmulator,
                        "effective_emulator", effectiveEmulator,
                        "guest_executable", guestExecutable != null ? guestExecutable : "",
                        "command", command
                )
        );

        final String submittedGuestExecutable = guestExecutable != null ? guestExecutable : "";
        final LaunchOutputRing launchOutput = new LaunchOutputRing();
        int launchedPid = ProcessHelper.exec(command, launchEnv.toStringArray(), rootDir, (status) -> {
            String outputTail = launchOutput.snapshot();
            ForensicLogger.logEvent(
                    context,
                    resolveLaunchExitSeverity(status, trackPrimaryPid, desktopShellBootstrap),
                    "LAUNCH_EXEC_EXIT",
                    forensicTraceId,
                    "guest_program_launcher",
                    "launch_exec_exit",
                    ForensicLogger.fields(
                            "status", status,
                            "track_primary_pid", trackPrimaryPid,
                            "guest_executable", submittedGuestExecutable,
                            "command", command,
                            "output_tail_line_count", launchOutput.lineCount(),
                            "output_tail_sha256", ForensicLogger.sha256Hex(outputTail),
                            "output_tail", outputTail
                    )
            );
            if (trackPrimaryPid) {
                synchronized (lock) {
                    pid = -1;
                }
            }

            if (activeTerminationCallback != null) {
                activeTerminationCallback.call(status);
            }
        }, launchOutput::add);
        return launchedPid;
    }

    private String resolveLaunchExitSeverity(int status, boolean trackPrimaryPid, boolean desktopShellBootstrap) {
        if (status == 0) return "info";
        if ((status == 137 || status == 143) && (trackPrimaryPid || desktopShellBootstrap)) return "info";
        return "warn";
    }

    private void applySmartphoneWineDriverContract(Context context, EnvVars launchEnv,
                                                   boolean desktopShellBootstrap, String launcherModel) {
        if (launchEnv == null) return;
        String previous = launchEnv.get("WINEDLLOVERRIDES");
        String merged = forceDisabledWineDrivers(previous, "winewayland.drv", "winemac.drv");
        launchEnv.put("WINEDLLOVERRIDES", merged);
        ForensicLogger.logEvent(
                context,
                "info",
                "SMARTPHONE_WINE_DRIVER_CONTRACT_APPLIED",
                null,
                "guest_program_launcher",
                "smartphone_wine_driver_contract_applied",
                ForensicLogger.fields(
                        "runtime_model", launcherModel == null ? "" : launcherModel,
                        "desktop_shell_bootstrap", desktopShellBootstrap,
                        "driver_priority", "x11",
                        "disabled_drivers", "winewayland.drv,winemac.drv",
                        "winedlloverrides_before", previous,
                        "winedlloverrides", merged,
                        "reason", "current_android_host_contract_ships_x11_engine_not_wayland_or_mac"
                )
        );
    }

    private String forceDisabledWineDrivers(String existing, String... driverNames) {
        StringBuilder result = new StringBuilder();
        for (String driverName : driverNames) {
            String normalizedDriver = driverName == null ? "" : driverName.trim().toLowerCase(Locale.ROOT);
            if (normalizedDriver.isEmpty()) continue;
            if (result.length() > 0) result.append(',');
            result.append(normalizedDriver);
        }
        if (result.length() > 0) result.append("=d");

        String normalizedExisting = existing == null ? "" : existing.trim();
        if (!normalizedExisting.isEmpty()) {
            for (String clause : normalizedExisting.split(";")) {
                String normalizedClause = clause == null ? "" : clause.trim();
                if (normalizedClause.isEmpty()) continue;
                if (isWineDriverOverrideClause(normalizedClause, driverNames)) continue;
                if (result.length() > 0) result.append(';');
                result.append(normalizedClause);
            }
        }
        return result.toString();
    }

    private boolean isWineDriverOverrideClause(String clause, String... driverNames) {
        if (clause == null) return false;
        String normalizedClause = clause.toLowerCase(Locale.ROOT);
        int equals = normalizedClause.indexOf('=');
        String names = equals >= 0 ? normalizedClause.substring(0, equals) : normalizedClause;
        for (String part : names.split(",")) {
            String normalizedPart = part == null ? "" : part.trim();
            if (normalizedPart.isEmpty()) continue;
            for (String driverName : driverNames) {
                String normalizedDriver = driverName == null ? "" : driverName.trim().toLowerCase(Locale.ROOT);
                if (normalizedPart.equals(normalizedDriver)) return true;
            }
        }
        return false;
    }

    protected void addBox64EnvVars(EnvVars envVars, boolean enableLogs) {
        envVars.put("BOX64_DYNAREC", "1");
        envVars.putAll(Box64PresetManager.getEnvVars("box64", environment.getContext(), box64Preset));
        if (!envVars.has("BOX64_NOBANNER")) {
            envVars.put("BOX64_NOBANNER", ProcessHelper.PRINT_DEBUG && enableLogs ? "0" : "1");
        }
        if (!envVars.has("BOX64_X11GLX")) envVars.put("BOX64_X11GLX", "1");
        if (!envVars.has("BOX64_NORCFILES")) envVars.put("BOX64_NORCFILES", "1");

        if (enableLogs) {
            envVars.put("BOX64_LOG", "1");
            envVars.put("BOX64_DYNAREC_MISSING", "1");
        }

        if (container != null) {
            String cpuList = container.getCPUList(true);
            if (cpuList != null && !cpuList.isEmpty()) {
                envVars.put("BOX64_CPULIST", cpuList);
                envVars.put("BOX86_CPULIST", cpuList);
            }
        }
    }

    public void suspendProcess() {
        synchronized (lock) {
            if (pid != -1) ProcessHelper.suspendProcess(pid);
        }
    }

    public void resumeProcess() {
        synchronized (lock) {
            if (pid != -1) ProcessHelper.resumeProcess(pid);
        }
    }

    protected void appendLdPreload(StringBuilder builder, String value) {
        if (builder == null || value == null) return;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return;
        if (builder.length() > 0) builder.append(':');
        builder.append(trimmed);
    }

    protected void appendExistingLdPreload(StringBuilder builder, EnvVars launchEnv) {
        if (builder == null || launchEnv == null) return;
        appendLdPreload(builder, launchEnv.get("LD_PRELOAD"));
    }

    protected void appendFileIfExists(StringBuilder builder, File file) {
        if (builder == null || file == null || !file.isFile()) return;
        appendLdPreload(builder, file.getPath());
    }

    protected void appendAndroidHostClosureLdPreload(StringBuilder builder, ImageFs imageFs, boolean includeRedirect) {
        if (builder == null || imageFs == null) return;

        File hostLibDir = imageFs.getAndroidHostLibDir();
        if (!hostLibDir.isDirectory()) {
            hostLibDir = imageFs.getLibDir();
        }

        // Keep the Android-host preload as small as possible. The direct host launcher
        // only needs bootstrap shims here; the rest should resolve through
        // LD_LIBRARY_PATH and the runtime's own unix-side loader.
        String[] preloadOrder = new String[] {
                "libandroid-support.so",
                "libandroid-sysvshm.so"
        };

        for (String libraryName : preloadOrder) {
            appendFileIfExists(builder, new File(hostLibDir, libraryName));
        }

        if (includeRedirect) {
            appendFileIfExists(builder, new File(hostLibDir, "libredirect-bionic.so"));
        }
    }

    protected void applyAndroidBionicHostLdLibraryPath(Context context, ImageFs imageFs, EnvVars launchEnv, String owner) {
        if (imageFs == null || launchEnv == null) return;
        String currentLdLibraryPath = launchEnv.get("LD_LIBRARY_PATH");
        String ldLibraryPath = buildAndroidBionicHostLdLibraryPath(imageFs, currentLdLibraryPath);
        launchEnv.put("LD_LIBRARY_PATH", ldLibraryPath);
        ForensicLogger.logEvent(
                context,
                "info",
                "ANDROID_BIONIC_HOST_LIBPATH_ORDER_APPLIED",
                null,
                "guest_program_launcher",
                "android_bionic_host_library_path_order_applied",
                ForensicLogger.fields(
                        "owner", owner == null ? "" : owner,
                        "mode", "runtime_first_system_tail_host_last_without_guest_usr_lib",
                        "guest_lib_dir", imageFs.getLibDir().getPath(),
                        "host_lib_dir", imageFs.getAndroidHostLibDir().getPath(),
                        "ld_library_path_before", summarizePathHead(currentLdLibraryPath, 6),
                        "ld_library_path_head", summarizePathHead(ldLibraryPath, 6)
                )
        );
    }

    private void logAndroidBionicHostFinalEnv(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv,
                                             String traceId, String appId, String effectiveEmulator,
                                             boolean desktopShellBootstrap) {
        if (imageFs == null || launchEnv == null) return;
        String launcherModel = ContentProfile.normalizeRuntimeModel(getLauncherModel(imageFs));
        boolean glibcLauncher = ContentProfile.RUNTIME_MODEL_GLIBC.equals(launcherModel);
        File runtimeRoot = new File(imageFs.getWinePath());
        File wineUnixDir = WineUtils.resolveRuntimeWineUnixDir(runtimeRoot, wineInfo);
        File hostLibDir = imageFs.getAndroidHostLibDir();
        File eglStubDir = new File(hostLibDir, "wine-x11-egl-stub");
        File redirectLib = new File(hostLibDir, "libredirect-bionic.so");
        File rootedX11Socket = rootDir != null ? new File(rootDir, UnixSocketConfig.XSERVER_PATH.substring(1)) : null;
        File rootedSysvShmSocket = rootDir != null ? new File(rootDir, UnixSocketConfig.SYSVSHM_SERVER_PATH.substring(1)) : null;
        String ldLibraryPath = launchEnv.get("LD_LIBRARY_PATH");
        String ldPreload = launchEnv.get("LD_PRELOAD");

        logLaunchStageEvent(
                context,
                "info",
                glibcLauncher ? "GLIBC_GUEST_FINAL_ENV_APPLIED" : "ANDROID_BIONIC_HOST_FINAL_ENV_APPLIED",
                traceId,
                glibcLauncher ? "glibc_guest_final_env" : "android_bionic_host_final_env",
                appId,
                -1L,
                null,
                "runtime_model", launcherModel,
                "effective_emulator", effectiveEmulator,
                "desktop_shell_bootstrap", desktopShellBootstrap,
                "runtime_root", runtimeRoot.getPath(),
                "wine_unix_dir", wineUnixDir != null ? wineUnixDir.getPath() : "",
                "winex11_so_present", wineUnixDir != null && new File(wineUnixDir, "winex11.so").isFile(),
                "host_lib_dir", hostLibDir.getPath(),
                "host_libx11_present", new File(hostLibDir, "libX11.so").isFile(),
                "host_libxext_present", new File(hostLibDir, "libXext.so").isFile(),
                "host_sysvshm_present", new File(hostLibDir, "libandroid-sysvshm.so").isFile(),
                "display", launchEnv.get("DISPLAY"),
                "winedlloverrides", launchEnv.get("WINEDLLOVERRIDES"),
                "android_sysvshm_server", launchEnv.get("ANDROID_SYSVSHM_SERVER"),
                "wine_x11forceglx", launchEnv.get("WINE_X11FORCEGLX"),
                "wine_use_egl", launchEnv.get("WINE_USE_EGL"),
                "egl_compat_dir", launchEnv.get("AERO_WINE_X11_EGL_COMPAT_DIR"),
                "egl_stub_global_ld", launchEnv.get("AERO_WINE_X11_EGL_STUB_GLOBAL_LD"),
                "redirect_debug", launchEnv.get("AERO_REDIRECT_DEBUG"),
                "contains_egl_stub_global_ld", containsPathSegment(ldLibraryPath, eglStubDir.getPath()),
                "host_redirect_present", redirectLib.isFile(),
                "redirect_preload_present", containsPathSegment(ldPreload, redirectLib.getPath()),
                "absolute_x11_socket_path", UnixSocketConfig.XSERVER_PATH,
                "absolute_x11_socket_present", new File(UnixSocketConfig.XSERVER_PATH).exists(),
                "rooted_x11_socket_path", rootedX11Socket != null ? rootedX11Socket.getPath() : "",
                "rooted_x11_socket_present", rootedX11Socket != null && rootedX11Socket.exists(),
                "x11_socket_namespaces", "pathname,abstract,donor-abstract-aliases",
                "x11_transport_contract", "DISPLAY=:0 with rooted tmp/usr-tmp pathname sockets plus Linux abstract aliases for Winlator/GameNative/Termux XCB paths",
                "rooted_sysvshm_socket_path", rootedSysvShmSocket != null ? rootedSysvShmSocket.getPath() : "",
                "rooted_sysvshm_socket_present", rootedSysvShmSocket != null && rootedSysvShmSocket.exists(),
                "ld_library_path_head", summarizePathHead(ldLibraryPath, 8),
                "ld_preload_head", summarizePathHead(ldPreload, 8),
                "vk_icd_filenames", launchEnv.get("VK_ICD_FILENAMES"),
                "vk_driver_files", launchEnv.get("VK_DRIVER_FILES"),
                "vk_layer_path", launchEnv.get("VK_LAYER_PATH"),
                "vk_implicit_layer_path", launchEnv.get("VK_IMPLICIT_LAYER_PATH"),
                "vk_loader_drivers_disable", launchEnv.get("VK_LOADER_DRIVERS_DISABLE"),
                "vk_loader_layers_disable", launchEnv.get("VK_LOADER_LAYERS_DISABLE"),
                "desktop_shell_vulkan_isolated", launchEnv.get("AERO_DESKTOP_SHELL_VULKAN_ISOLATED"),
                "desktop_shell_x11_bootstrap_route", launchEnv.get("AERO_DESKTOP_SHELL_X11_BOOTSTRAP_ROUTE")
        );
    }

    private void applyDesktopShellX11BootstrapIsolation(Context context, ImageFs imageFs, File rootDir, EnvVars launchEnv,
                                                        String traceId, String appId, String effectiveEmulator,
                                                        boolean desktopShellBootstrap) {
        if (!desktopShellBootstrap || imageFs == null || rootDir == null || launchEnv == null) return;

        String previousVkIcdFilenames = launchEnv.get("VK_ICD_FILENAMES");
        String previousVkDriverFiles = launchEnv.get("VK_DRIVER_FILES");
        String previousVkLayerPath = launchEnv.get("VK_LAYER_PATH");
        String previousVkImplicitLayerPath = launchEnv.get("VK_IMPLICIT_LAYER_PATH");
        String previousLoaderDriversDisable = launchEnv.get("VK_LOADER_DRIVERS_DISABLE");
        String previousLoaderLayersDisable = launchEnv.get("VK_LOADER_LAYERS_DISABLE");

        File emptyDriverDir = new File(imageFs.getTmpDir(), "x11-bootstrap-empty-vulkan-driver.d");
        File emptyExplicitLayerDir = new File(imageFs.getTmpDir(), "x11-bootstrap-empty-vulkan-explicit-layer.d");
        File emptyImplicitLayerDir = new File(imageFs.getTmpDir(), "x11-bootstrap-empty-vulkan-implicit-layer.d");
        ensureDirectory(emptyDriverDir);
        ensureDirectory(emptyExplicitLayerDir);
        ensureDirectory(emptyImplicitLayerDir);

        launchEnv.remove("VK_ICD_FILENAMES");
        launchEnv.put("VK_DRIVER_FILES", emptyDriverDir.getPath());
        launchEnv.remove("VK_ADD_DRIVER_FILES");
        launchEnv.put("VK_LOADER_DRIVERS_DISABLE", appendLoaderFilter(previousLoaderDriversDisable, "*"));

        launchEnv.remove("VK_INSTANCE_LAYERS");
        launchEnv.put("VK_LAYER_PATH", emptyExplicitLayerDir.getPath());
        launchEnv.put("VK_IMPLICIT_LAYER_PATH", emptyImplicitLayerDir.getPath());
        launchEnv.remove("VK_ADD_LAYER_PATH");
        launchEnv.remove("VK_ADD_IMPLICIT_LAYER_PATH");
        launchEnv.remove("VK_LOADER_LAYERS_ALLOW");
        launchEnv.remove("VK_LOADER_LAYERS_ENABLE");
        launchEnv.put("VK_LOADER_LAYERS_DISABLE", appendLoaderFilter(previousLoaderLayersDisable, "*"));

        launchEnv.put("WINE_X11FORCEGLX", "1");
        launchEnv.put("WINE_USE_EGL", "0");
        launchEnv.put("AERO_DESKTOP_SHELL_VULKAN_ISOLATED", "1");
        launchEnv.put("AERO_DESKTOP_SHELL_X11_BOOTSTRAP_ROUTE", "x11-glx-bootstrap-with-empty-vulkan-driver-discovery");
        launchEnv.put("AERO_DESKTOP_SHELL_X11_BOOTSTRAP_OWNER", "winex11-process-attach-before-mapwindow");

        logLaunchStageEvent(
                context,
                "info",
                "DESKTOP_SHELL_X11_BOOTSTRAP_VULKAN_ISOLATED",
                traceId,
                "desktop_shell_x11_bootstrap_vulkan_isolated",
                appId,
                -1L,
                null,
                "effective_emulator", effectiveEmulator,
                "previous_vk_icd_filenames", previousVkIcdFilenames,
                "previous_vk_driver_files", previousVkDriverFiles,
                "previous_vk_layer_path", previousVkLayerPath,
                "previous_vk_implicit_layer_path", previousVkImplicitLayerPath,
                "previous_vk_loader_drivers_disable", previousLoaderDriversDisable,
                "previous_vk_loader_layers_disable", previousLoaderLayersDisable,
                "vk_driver_files", launchEnv.get("VK_DRIVER_FILES"),
                "vk_layer_path", launchEnv.get("VK_LAYER_PATH"),
                "vk_implicit_layer_path", launchEnv.get("VK_IMPLICIT_LAYER_PATH"),
                "vk_loader_drivers_disable", launchEnv.get("VK_LOADER_DRIVERS_DISABLE"),
                "vk_loader_layers_disable", launchEnv.get("VK_LOADER_LAYERS_DISABLE"),
                "wine_x11forceglx", launchEnv.get("WINE_X11FORCEGLX"),
                "wine_use_egl", launchEnv.get("WINE_USE_EGL"),
                "reason", "winex11_process_attach_must_reach_mapwindow_before_vulkan_provider_loading"
        );
    }

    private static void ensureDirectory(File directory) {
        if (directory != null && !directory.isDirectory()) {
            directory.mkdirs();
        }
    }

    private static String appendLoaderFilter(String value, String filter) {
        String normalizedFilter = filter == null ? "" : filter.trim();
        if (normalizedFilter.isEmpty()) return value == null ? "" : value.trim();
        String normalizedValue = value == null ? "" : value.trim();
        if (normalizedValue.isEmpty()) return normalizedFilter;
        for (String part : normalizedValue.split(",")) {
            if (normalizedFilter.equalsIgnoreCase(part.trim())) return normalizedValue;
        }
        return normalizedValue + "," + normalizedFilter;
    }

    protected String buildAndroidBionicHostLdLibraryPath(ImageFs imageFs, String currentLdLibraryPath) {
        return AndroidBionicHostLdPathHelper.buildDirectGuestLdLibraryPath(
                currentLdLibraryPath,
                imageFs.getLibDir().getPath(),
                new File(imageFs.getRootDir(), "usr/lib64").getPath(),
                imageFs.getAndroidHostLibDir().getPath()
        );
    }

    protected static String buildRuntimeWineDllPath(String wineDllPath, String wineUnixPath, WineInfo wineInfo) {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        String system32Tree = wineInfo != null && wineInfo.usesAarch64WindowsTree()
                ? "aarch64-windows"
                : "x86_64-windows";
        appendLdLibraryPath(paths, wineDllPath + "/" + system32Tree);
        if (wineInfo == null || wineInfo.isWin64()) {
            appendLdLibraryPath(paths, wineDllPath + "/i386-windows");
        }
        appendLdLibraryPath(paths, wineDllPath);
        appendLdLibraryPath(paths, wineUnixPath);
        return String.join(":", paths);
    }

    private void applyGlibcActiveWineOverlay(Context context, ImageFs imageFs, File runtimeRootDir,
                                             File runtimeWineLibDir, String traceId, String appId) {
        File rootDir = imageFs != null ? imageFs.getRootDir() : null;
        File usrLibDir = rootDir != null ? new File(rootDir, "usr/lib") : null;
        File activeWineDir = usrLibDir != null ? new File(usrLibDir, "wine") : null;
        File markerFile = usrLibDir != null ? new File(usrLibDir, ".aeso_active_wine_overlay") : null;
        boolean sourceReady = runtimeWineLibDir != null && runtimeWineLibDir.isDirectory();
        boolean usrLibReady = usrLibDir != null && (usrLibDir.isDirectory() || usrLibDir.mkdirs());
        String desiredTarget = sourceReady ? runtimeWineLibDir.getAbsolutePath() : "";
        boolean activePathPresentBefore = pathExistsOrSymlink(activeWineDir);
        boolean wasSymlink = activeWineDir != null && FileUtils.isSymlink(activeWineDir);
        String previousTarget = wasSymlink ? FileUtils.readSymlink(activeWineDir) : "";
        boolean alreadyActive = wasSymlink && desiredTarget.equals(previousTarget);
        boolean replaced = false;
        boolean created = false;
        boolean blocked = false;
        String errorDetail = "";

        if (sourceReady && usrLibReady && activeWineDir != null) {
            try {
                if (alreadyActive) {
                    writeGlibcActiveWineOverlayMarker(markerFile, runtimeRootDir, runtimeWineLibDir);
                } else {
                    if (pathExistsOrSymlink(activeWineDir)) {
                        if (wasSymlink || isManagedGlibcActiveWineOverlay(activeWineDir, markerFile)) {
                            replaced = FileUtils.delete(activeWineDir);
                            if (!replaced && pathExistsOrSymlink(activeWineDir)) {
                                throw new IOException("unable_to_remove_previous_active_wine_overlay");
                            }
                        } else if (activeWineDir.isDirectory() && FileUtils.isEmpty(activeWineDir)) {
                            replaced = FileUtils.delete(activeWineDir);
                        } else {
                            blocked = true;
                            errorDetail = "preserve_unmanaged_usr_lib_wine";
                        }
                    }

                    if (!blocked && !pathExistsOrSymlink(activeWineDir)) {
                        created = FileUtils.symlink(runtimeWineLibDir, activeWineDir);
                        if (!created) {
                            errorDetail = "symlink_create_failed";
                        } else {
                            writeGlibcActiveWineOverlayMarker(markerFile, runtimeRootDir, runtimeWineLibDir);
                        }
                    }
                }
            } catch (Exception e) {
                blocked = true;
                errorDetail = e.getClass().getSimpleName() + ":" + String.valueOf(e.getMessage());
            }
        } else if (!sourceReady) {
            errorDetail = "runtime_wine_lib_missing";
        } else if (!usrLibReady) {
            errorDetail = "usr_lib_unavailable";
        }

        boolean activeSymlink = activeWineDir != null && FileUtils.isSymlink(activeWineDir);
        String activeTarget = activeSymlink ? FileUtils.readSymlink(activeWineDir) : "";
        boolean activeMatchesRuntime = activeSymlink && desiredTarget.equals(activeTarget);
        logLaunchStageEvent(
                context,
                activeMatchesRuntime && !blocked ? "info" : "warn",
                "GLIBC_ACTIVE_WINE_OVERLAY_APPLIED",
                traceId,
                "glibc_active_wine_overlay",
                appId,
                -1L,
                null,
                "source_ready", sourceReady,
                "usr_lib_ready", usrLibReady,
                "runtime_root", runtimeRootDir != null ? runtimeRootDir.getAbsolutePath() : "",
                "runtime_wine_lib", desiredTarget,
                "active_wine_path", activeWineDir != null ? activeWineDir.getAbsolutePath() : "",
                "active_path_present_before", activePathPresentBefore,
                "was_symlink", wasSymlink,
                "previous_target", previousTarget,
                "already_active", alreadyActive,
                "replaced", replaced,
                "created", created,
                "active_symlink", activeSymlink,
                "active_target", activeTarget,
                "active_matches_runtime", activeMatchesRuntime,
                "blocked", blocked,
                "error_detail", errorDetail
        );
    }

    private boolean isManagedGlibcActiveWineOverlay(File activeWineDir, File markerFile) {
        if (activeWineDir == null || markerFile == null || !markerFile.isFile()) return false;
        String marker = FileUtils.readString(markerFile);
        return marker != null
                && marker.contains("managed_by=aesolator")
                && marker.contains("active_wine_path=" + activeWineDir.getAbsolutePath());
    }

    private void writeGlibcActiveWineOverlayMarker(File markerFile, File runtimeRootDir, File runtimeWineLibDir) {
        if (markerFile == null || runtimeRootDir == null || runtimeWineLibDir == null) return;
        FileUtils.writeString(
                markerFile,
                "managed_by=aesolator\n"
                        + "contract=glibc_active_wine_overlay\n"
                        + "active_wine_path=" + new File(markerFile.getParentFile(), "wine").getAbsolutePath() + "\n"
                        + "runtime_root=" + runtimeRootDir.getAbsolutePath() + "\n"
                        + "runtime_wine_lib=" + runtimeWineLibDir.getAbsolutePath() + "\n"
        );
        FileUtils.chmod(markerFile, 0644);
    }

    private static boolean pathExistsOrSymlink(File path) {
        return path != null && (path.exists() || FileUtils.isSymlink(path));
    }

    private static boolean containsPathSegment(String pathValue, String expectedSegment) {
        String expected = normalizePathSegment(expectedSegment);
        if (pathValue == null || pathValue.trim().isEmpty() || expected.isEmpty()) return false;
        for (String segment : pathValue.split(":")) {
            if (expected.equals(normalizePathSegment(segment))) return true;
        }
        return false;
    }

    private static String normalizePathSegment(String path) {
        if (path == null) return "";
        String normalized = path.trim();
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static void appendLdLibraryDir(Set<String> paths, File directory) {
        if (directory == null || !directory.isDirectory()) return;
        appendLdLibraryPath(paths, directory.getPath());
    }

    private static void appendLdLibraryPath(Set<String> paths, String path) {
        if (paths == null || path == null) return;
        String trimmed = path.trim();
        if (!trimmed.isEmpty()) paths.add(trimmed);
    }

    protected static String summarizePathHead(String pathValue, int segmentLimit) {
        if (pathValue == null || pathValue.trim().isEmpty() || segmentLimit <= 0) return "";
        String[] segments = pathValue.split(":");
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < segments.length && i < segmentLimit; i++) {
            if (segments[i] == null || segments[i].trim().isEmpty()) continue;
            if (summary.length() > 0) summary.append(':');
            summary.append(segments[i].trim());
        }
        if (segments.length > segmentLimit && summary.length() > 0) {
            summary.append(":...");
        }
        return summary.toString();
    }

    private void repairRuntimeExecutablePermissions(Context context, File runtimeBinDir) {
        if (runtimeBinDir == null || !runtimeBinDir.isDirectory()) return;
        File[] binaries = runtimeBinDir.listFiles();
        if (binaries == null) return;
        int repairedCount = 0;
        for (File file : binaries) {
            if (file != null && file.isFile()) {
                FileUtils.chmod(file, 0755);
                repairedCount++;
            }
        }
        ForensicLogger.logEvent(
                context,
                "info",
                "RUNTIME_BIN_PERMISSION_REPAIR",
                null,
                "guest_program_launcher",
                "runtime_bin_permission_repair",
                ForensicLogger.fields(
                        "runtime_bin_dir", runtimeBinDir.getAbsolutePath(),
                        "repaired_count", repairedCount
                )
        );
    }

    private void bindLaunchRuntimeWinePath(Context context, ImageFs imageFs, String traceId, String appId) {
        if (imageFs == null) return;

        String before = normalizePathValue(imageFs.getWinePath());
        String targetPath = resolveLaunchRuntimeWinePath();
        if (!targetPath.isEmpty()) {
            imageFs.setWinePath(targetPath);
        }
        String after = normalizePathValue(imageFs.getWinePath());
        logLaunchStageEvent(
                context,
                "info",
                "LAUNCH_RUNTIME_WINE_PATH_BOUND",
                traceId,
                "runtime_wine_path_bound",
                appId,
                -1L,
                null,
                "runtime_profile", wineProfile != null ? ContentsManager.getEntryName(wineProfile) : "-",
                "imagefs_root", imageFs.getRootDir().getAbsolutePath(),
                "imagefs_root_name", imageFs.getRootDir().getName(),
                "imagefs_root_is_active_alias", ImageFs.ACTIVE_ROOT_DIR_NAME.equals(imageFs.getRootDir().getName()) ? "1" : "0",
                "active_root_alias", ImageFs.getActiveRootDir(context).getAbsolutePath(),
                "active_root_alias_target", FileUtils.isSymlink(ImageFs.getActiveRootDir(context)) ? FileUtils.readSymlink(ImageFs.getActiveRootDir(context)) : "",
                "wine_info_path", wineInfo != null ? normalizePathValue(wineInfo.path) : "",
                "wine_path_before", before,
                "wine_path_after", after,
                "wine_path_rebound", !before.equals(after) ? "1" : "0",
                "runtime_core_payload_present", !after.isEmpty() && WineUtils.hasRuntimeCorePayload(new File(after)) ? "1" : "0"
        );
    }

    private String resolveLaunchRuntimeWinePath() {
        File profileRoot = resolveWineProfileRuntimeRoot();
        if (profileRoot != null) return profileRoot.getPath();

        String wineInfoPath = wineInfo != null ? normalizePathValue(wineInfo.path) : "";
        if (!wineInfoPath.isEmpty()) {
            File wineInfoRoot = WineUtils.resolveCanonicalRuntimeRoot(new File(wineInfoPath));
            if (wineInfoRoot != null && WineUtils.hasRuntimeCorePayload(wineInfoRoot)) {
                return wineInfoRoot.getPath();
            }
            return wineInfoPath;
        }
        return "";
    }

    private File resolveWineProfileRuntimeRoot() {
        if (contentsManager == null
                || wineProfile == null
                || !wineProfile.isWineProtonFamily()
                || !contentsManager.isInstalledProfileUsable(wineProfile)) {
            return null;
        }

        File runtimeRoot = WineUtils.resolveCanonicalRuntimeRoot(contentsManager.getRuntimeRootDir(wineProfile));
        if (runtimeRoot != null && WineUtils.hasRuntimeCorePayload(runtimeRoot)) {
            return runtimeRoot;
        }

        ContentsManager.InstalledProfileDiagnostics diagnostics =
                contentsManager.resolveInstalledProfileDiagnostics(wineProfile);
        if (diagnostics.runtimePayloadPresent && !normalizePathValue(diagnostics.runtimeRoot).equals("-")) {
            runtimeRoot = WineUtils.resolveCanonicalRuntimeRoot(new File(diagnostics.runtimeRoot));
            if (runtimeRoot != null && WineUtils.hasRuntimeCorePayload(runtimeRoot)) {
                return runtimeRoot;
            }
        }
        return null;
    }

    private static String normalizePathValue(String value) {
        return value == null ? "" : value.trim();
    }

    private void ensureRuntimeSdlCompatLink(Context context, ImageFs imageFs) {
        File libDir = imageFs != null ? imageFs.getLibDir() : null;
        File sdlSo = libDir != null ? new File(libDir, "libSDL2-2.0.so") : null;
        File sdlCompatLink = libDir != null ? new File(libDir, "libSDL2-2.0.so.0") : null;
        boolean created = false;
        boolean failed = false;
        String errorDetail = "";
        if (sdlSo != null && sdlSo.isFile() && sdlCompatLink != null && !sdlCompatLink.exists()) {
            try {
                FileUtils.symlink(sdlSo.getName(), sdlCompatLink.getAbsolutePath());
                created = sdlCompatLink.exists();
                failed = !created;
            } catch (Exception e) {
                failed = true;
                errorDetail = String.valueOf(e.getMessage());
            }
        }
        ForensicLogger.logEvent(
                context,
                failed ? "warn" : "info",
                "SDL_RUNTIME_COMPAT_LINK",
                null,
                "guest_program_launcher",
                "sdl_runtime_compat_link",
                ForensicLogger.fields(
                        "source_exists", sdlSo != null && sdlSo.isFile(),
                        "source_path", sdlSo != null ? sdlSo.getAbsolutePath() : "",
                        "compat_link_exists", sdlCompatLink != null && sdlCompatLink.exists(),
                        "compat_link_path", sdlCompatLink != null ? sdlCompatLink.getAbsolutePath() : "",
                        "created", created,
                        "failed", failed,
                        "error_detail", errorDetail
                )
        );
    }

    private void appendAndroidOemCryptoLdPreload(StringBuilder builder, ImageFs imageFs) {
        if (builder == null || imageFs == null) return;
        File[] cryptoCandidates = new File[] {
                new File("/system/lib64/libcrypto.so"),
                new File("/system_ext/lib64/libcrypto.so"),
                new File(imageFs.getLibDir(), "libcrypto.so.3")
        };
        File selected = null;
        for (File candidate : cryptoCandidates) {
            if (candidate.isFile()) {
                appendFileIfExists(builder, candidate);
                selected = candidate;
                break;
            }
        }
        ForensicLogger.logEvent(
                appContextOrFallback(imageFs),
                selected != null ? "info" : "warn",
                "ANDROID_OEM_CRYPTO_PRELOAD_SELECTED",
                null,
                "guest_program_launcher",
                "android_oem_crypto_preload_selected",
                ForensicLogger.fields(
                        "selected_path", selected != null ? selected.getAbsolutePath() : "",
                        "candidate_count", cryptoCandidates.length
                )
        );
    }

    private Context appContextOrFallback(ImageFs imageFs) {
        Context forensicContext = environment != null ? environment.getContext() : null;
        return forensicContext != null ? forensicContext : ForensicLogger.getAppContext();
    }

    private void applyProtonControllerBridgeEnv(Context context, ImageFs imageFs, EnvVars launchEnv) {
        launchEnv.put("PROTON_ENABLE_HIDRAW", "0");
        launchEnv.put("SDL_GAMECONTROLLER_ALLOW_STEAM_VIRTUAL_GAMEPAD", "1");
        launchEnv.put("SDL_JOYSTICK_HIDAPI", "0");
        File evshimLibrary = resolveEvshimLibrary(imageFs);
        ForensicLogger.logEvent(
                context,
                "info",
                "PROTON_CONTROLLER_BRIDGE_ENV_APPLIED",
                null,
                "guest_program_launcher",
                "proton_controller_bridge_env_applied",
                ForensicLogger.fields(
                        "evshim_present", evshimLibrary != null && evshimLibrary.isFile(),
                        "evshim_path", evshimLibrary != null ? evshimLibrary.getAbsolutePath() : "",
                        "proton_enable_hidraw", launchEnv.get("PROTON_ENABLE_HIDRAW"),
                        "sdl_virtual_gamepad", launchEnv.get("SDL_GAMECONTROLLER_ALLOW_STEAM_VIRTUAL_GAMEPAD"),
                        "sdl_hidapi", launchEnv.get("SDL_JOYSTICK_HIDAPI")
                )
        );
    }

    private File applyFakeEvdevRuntimeBridge(Context context, ImageFs imageFs, EnvVars launchEnv) {
        File fakeInputLibrary = resolveFakeInputLibrary(imageFs);
        File devInputDir = imageFs != null ? new File(imageFs.getRootDir(), "dev/input") : null;
        int createdNodeCount = ensureFakeEvdevNodes(devInputDir);
        if (devInputDir != null && devInputDir.isDirectory()) {
            launchEnv.put("FAKE_EVDEV_DIR", devInputDir.getAbsolutePath());
            launchEnv.put("FAKE_EVDEV_VIBRATION", "1");
        }
        ForensicLogger.logEvent(
                context,
                fakeInputLibrary != null && fakeInputLibrary.isFile() ? "info" : "warn",
                "FAKE_EVDEV_RUNTIME_BRIDGE_READY",
                null,
                "guest_program_launcher",
                "fake_evdev_runtime_bridge_ready",
                ForensicLogger.fields(
                        "fakeinput_present", fakeInputLibrary != null && fakeInputLibrary.isFile(),
                        "fakeinput_path", fakeInputLibrary != null ? fakeInputLibrary.getAbsolutePath() : "",
                        "fake_evdev_dir", devInputDir != null ? devInputDir.getAbsolutePath() : "",
                        "fake_evdev_dir_present", devInputDir != null && devInputDir.isDirectory(),
                        "created_node_count", createdNodeCount
                )
        );
        return fakeInputLibrary;
    }

    private int ensureFakeEvdevNodes(File devInputDir) {
        if (devInputDir == null) return 0;
        if (!devInputDir.exists() && !devInputDir.mkdirs()) return 0;
        int createdCount = 0;
        for (int slot = 0; slot < 4; slot++) {
            createdCount += ensureFakeEvdevNode(new File(devInputDir, "event" + slot));
            createdCount += ensureFakeEvdevNode(new File(devInputDir, "js" + slot));
        }
        return createdCount;
    }

    private int ensureFakeEvdevNode(File node) {
        if (node == null || node.exists()) return 0;
        try {
            File parent = node.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            return node.createNewFile() ? 1 : 0;
        } catch (IOException ignored) {
            return 0;
        }
    }

    protected File resolveEvshimLibrary(ImageFs imageFs) {
        if (imageFs == null) return null;
        File hostEvshim = new File(imageFs.getAndroidHostLibDir(), "libevshim.so");
        if (hostEvshim.isFile()) return hostEvshim;
        File guestEvshim = new File(imageFs.getLibDir(), "libevshim.so");
        return guestEvshim.isFile() ? guestEvshim : null;
    }

    protected File resolveFakeInputLibrary(ImageFs imageFs) {
        if (imageFs == null) return null;
        File guestFakeInput = new File(imageFs.getLibDir(), "libfakeinput.so");
        return guestFakeInput.isFile() ? guestFakeInput : null;
    }

    private static String mergePreloadValue(String protectedLdPreload, String overrideLdPreload) {
        String normalizedProtected = protectedLdPreload == null ? "" : protectedLdPreload.trim();
        String normalizedOverride = overrideLdPreload == null ? "" : overrideLdPreload.trim();
        if (normalizedProtected.isEmpty()) return normalizedOverride;
        if (normalizedOverride.isEmpty()) return normalizedProtected;
        if (normalizedProtected.equals(normalizedOverride)) return normalizedProtected;
        return normalizedProtected + ":" + normalizedOverride;
    }

    private void mergeExternalEnvVars(EnvVars launchEnv, String protectedLdPreload, String protectedFakeEvdevDir) {
        if (launchEnv == null || this.envVars == null) return;
        String overrideLdPreload = this.envVars.get("LD_PRELOAD");
        String overrideFakeEvdevDir = this.envVars.get("FAKE_EVDEV_DIR");
        launchEnv.putAll(this.envVars);
        launchEnv.put("LD_PRELOAD", mergePreloadValue(protectedLdPreload, overrideLdPreload));
        if (protectedFakeEvdevDir != null && !protectedFakeEvdevDir.trim().isEmpty()) {
            launchEnv.put("FAKE_EVDEV_DIR", protectedFakeEvdevDir.trim());
        } else if (overrideFakeEvdevDir != null && !overrideFakeEvdevDir.trim().isEmpty()) {
            launchEnv.put("FAKE_EVDEV_DIR", overrideFakeEvdevDir.trim());
        }
    }

    private void bindActiveContainerHome(ImageFs imageFs) {
        if (imageFs == null || container == null) return;
        String runtimeModel = wineProfile != null ? wineProfile.getRuntimeModel() : "";
        if (ContentProfile.normalizeRuntimeModel(runtimeModel).isEmpty()) {
            runtimeModel = ContainerManager.resolveContainerRuntimeModel(container);
        }
        String runtimeIdentity = wineProfile != null
                ? ContentsManager.getEntryName(wineProfile)
                : ContainerManager.resolveContainerRuntimeIdentity(container);
        ImageFs.ensureContainerHomeForRuntime(environment.getContext(), container.id, container.getRootDir(), runtimeModel, runtimeIdentity);
        ContainerManager.activateContainerHome(new File(imageFs.getRootDir(), "home"), container);
        imageFs.setHomeDir(container.getRootDir());
    }
}
