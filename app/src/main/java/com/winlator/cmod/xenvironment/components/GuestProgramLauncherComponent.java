package com.winlator.cmod.xenvironment.components;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.os.Process;
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
        ContentProfile profile = contentsManager.findProfileByVersion(type, versionName, true);
        if (profile != null) return profile;
        return contentsManager.findProfileByVersion(type, versionName, false);
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
        File box64Binary = new File(rootDir, "/usr/bin/box64");
        File localBox64Binary = new File(rootDir, "/usr/local/bin/box64");
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
            if (profile != null)
                contentsManager.applyContent(profile);
            else
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
        File system32dir = new File(imageFs.getWinePrefixDir(), "drive_c/windows/system32");
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
            if (profile != null)
                contentsManager.applyContent(profile);
            else
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
            if (profile != null)
                contentsManager.applyContent(profile);
            else
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
            if (pid == -1) {
                Log.e("GuestProgramLauncherComponent", "Guest runtime process failed to start for " + appId);
                AppUtils.showToast(environment.getContext(), "Failed to start Wine runtime process");
                if (terminationCallback != null) terminationCallback.call(-1);
            }
        }
    }


    @Override
    public void stop() {
        synchronized (lock) {
            if (pid != -1) {
                Process.killProcess(pid);
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

        File glibcBin = new File(rootDir, "/usr/glibc/bin");
        if (glibcBin.exists() && glibcBin.isDirectory()) {
            segments.add(glibcBin.getPath());
        }

        File usrBin = new File(rootDir, "/usr/bin");
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

        boolean hasGlibcBin = new File(rootDir, "/usr/glibc/bin").isDirectory();
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
        if (!launchEnv.has("AERO_REDIRECT_DEBUG")) {
            launchEnv.put("AERO_REDIRECT_DEBUG", "0");
        }
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
                || lowered.contains("explorer.exe /desktop=shell");
    }

    private String resolveRequestedEmulator() {
        String emulator = container.getEmulator();
        if (shortcut != null) {
            emulator = shortcut.getExtra("emulator", container.getEmulator());
        }
        return emulator == null ? "" : emulator;
    }

    protected File getArm64EcSystem32Dir(ImageFs imageFs) {
        if (imageFs == null) return null;
        return new File(imageFs.wineprefix, "drive_c/windows/system32");
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
            String hodll = effectiveEmulator.toLowerCase(Locale.ROOT).equals("fexcore")
                    ? "libwow64fex.dll"
                    : "wowbox64.dll";
            launchEnv.put("HODLL", hodll);
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
                            "hodll", hodll
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
        File runtimeWineUnixDir = WineUtils.resolveRuntimeWineUnixDir(runtimeRootDir);
        if (runtimeBinDir == null || runtimeLibDir == null || runtimeWineLibDir == null || runtimeWineUnixDir == null) {
            Log.e(
                    "GuestProgramLauncherComponent",
                    "Runtime layout incomplete for launch: root=" + wineRootPath
                            + " bin=" + (runtimeBinDir != null)
                            + " lib=" + (runtimeLibDir != null)
                            + " wineLib=" + (runtimeWineLibDir != null)
                            + " wineUnix=" + (runtimeWineUnixDir != null)
            );
            return -1;
        }
        String wineBinPath = runtimeBinDir.getPath();
        String wineLibPath = runtimeLibDir.getPath();
        String wineLib64Path = wineRootPath + "/lib64";
        String wineUnixPath = runtimeWineUnixDir.getPath();
        String wineDllPath = runtimeWineLibDir.getPath();

        // Setting up essential environment variables for Wine
        launchEnv.put("HOME", imageFs.home_path);
        launchEnv.put("USER", ImageFs.USER);
        launchEnv.put("TMPDIR", imageFs.getTmpDir().getPath());
        launchEnv.put("XDG_DATA_DIRS", rootDir.getPath() + "/usr/share");
        launchEnv.put(
                "LD_LIBRARY_PATH",
                wineLibPath + ":" + wineLib64Path + ":" + wineUnixPath + ":" + rootDir.getPath() + "/usr/lib" + ":" + rootDir.getPath() + "/usr/lib64" + ":" + "/system/lib64"
        );
        launchEnv.put("WINEDLLPATH", wineDllPath + "/aarch64-windows:" + wineDllPath + "/i386-windows:" + wineDllPath);
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

        String ld_preload = "";

        // Check for specific shared memory libraries
        if ((new File(imageFs.getLibDir(), "libandroid-sysvshm.so")).exists()){
            ld_preload = imageFs.getLibDir() + "/libandroid-sysvshm.so";
        }

        launchEnv.put("LD_PRELOAD", ld_preload);

        if (this.envVars != null) {
            launchEnv.putAll(this.envVars);
        }

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
        File box64File = new File(rootDir, "/usr/bin/box64");
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

        return ProcessHelper.exec(command, launchEnv.toStringArray(), rootDir, (status) -> {
            ForensicLogger.logEvent(
                    context,
                    status == 0 ? "info" : "warn",
                    "LAUNCH_EXEC_EXIT",
                    forensicTraceId,
                    "guest_program_launcher",
                    "launch_exec_exit",
                    ForensicLogger.fields(
                            "status", status,
                            "track_primary_pid", trackPrimaryPid,
                            "guest_executable", guestExecutable != null ? guestExecutable : ""
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
        });
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

    private void bindActiveContainerHome(ImageFs imageFs) {
        if (imageFs == null || container == null) return;
        ContainerManager.activateContainerHome(new File(imageFs.getRootDir(), "home"), container);
        imageFs.setHomeDir(container.getRootDir());
    }
}
