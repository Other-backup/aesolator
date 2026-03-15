package com.winlator.cmod.xenvironment.components;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Process;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.box64.Box64Preset;
import com.winlator.cmod.box64.Box64PresetManager;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.GPUInformation;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.ProcessHelper;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.fexcore.FEXCoreManager;
import com.winlator.cmod.fexcore.FEXCorePreset;
import com.winlator.cmod.fexcore.FEXCorePresetManager;
import com.winlator.cmod.launchdeps.LaunchDependencyRegistry;
import com.winlator.cmod.xconnector.UnixSocketConfig;
import com.winlator.cmod.xenvironment.EnvironmentComponent;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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

    protected void extractBox64Files() {
        ImageFs imageFs = environment.getImageFs();
        Context context = environment.getContext();

        // Fallback to default if the shared preference is not set or is empty
        String box64Version = container.getBox64Version();

        if (shortcut != null)
            box64Version = shortcut.getExtra("box64Version", shortcut.container.getBox64Version());

        Log.d("GuestProgramLauncherComponent", "box64Version: " + box64Version);

        File rootDir = imageFs.getRootDir();
        File box64Binary = new File(rootDir, "/usr/bin/box64");
        boolean payloadMissing = needsFileRefresh(box64Binary);

        if (payloadMissing || !box64Version.equals(container.getExtra("box64Version"))) {
            ContentProfile profile = resolveInstalledContentProfile(ContentProfile.ContentType.CONTENT_TYPE_BOX64, box64Version);
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
                            "profile_entry", profile != null ? ContentsManager.getEntryName(profile) : ""
                    )
            );
            if (profile != null)
                contentsManager.applyContent(profile);
            else
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, "box64/box64-" + box64Version + ".tzst", rootDir);
            container.putExtra("box64Version", box64Version);
            container.saveData();
        }

        // Set execute permissions for box64 just in case
        if (box64Binary.exists()) {
            FileUtils.chmod(box64Binary, 0755);
        }
    }

    protected void extractEmulatorsDlls() {;
        Context context = environment.getContext();
        File rootDir = environment.getImageFs().getRootDir();
        File system32dir = new File(rootDir + "/home/xuser/.wine/drive_c/windows/system32");
        boolean containerDataChanged = false;

        String wowbox64Version = container.getBox64Version();
        String fexcoreVersion = container.getFEXCoreVersion();

        if (shortcut != null) {
            wowbox64Version = shortcut.getExtra("box64Version", shortcut.container.getBox64Version());
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
            String appId = shortcut != null ? shortcut.name : guestExecutable;
            if (wineInfo == null) {
                wineInfo = WineInfo.fromIdentifier(
                        environment.getContext(),
                        contentsManager,
                        container.getWineVersion(),
                        resolveRequestedRuntimeModel()
                );
            }
            LaunchDependencyRegistry.DependencyRunResult dependencyResult =
                    LaunchDependencyRegistry.runDependencies(environment.getContext(), container, shortcut, appId);
            if (!dependencyResult.success) {
                Log.e("GuestProgramLauncherComponent", "Launch dependencies failed: " + dependencyResult.dependencyId + " / " + dependencyResult.message);
                String failMessage = dependencyResult.message == null || dependencyResult.message.trim().isEmpty()
                        ? "Missing launch dependency: " + dependencyResult.dependencyId
                        : dependencyResult.message;
                AppUtils.showToast(environment.getContext(), failMessage);
                if (terminationCallback != null) terminationCallback.call(-1);
                return;
            }
            if (wineInfo.isArm64EC())
                extractEmulatorsDlls();
            else
                extractBox64Files();
            LaunchDependencyRegistry.runPreLaunchSteps(environment.getContext(), container, shortcut, appId, this);
            checkDependencies();
            pid = execGuestProgram();
            if (pid == -1) {
                Log.e("GuestProgramLauncherComponent", "Guest runtime process failed to start for " + appId);
                AppUtils.showToast(environment.getContext(), "Failed to start Wine runtime process");
                if (terminationCallback != null) terminationCallback.call(-1);
            }
        }
    }


    private String checkDependencies() {
        String curlPath = environment.getImageFs().getRootDir().getPath() + "/usr/lib/libXau.so";
        String lddCommand = "ldd " + curlPath;

        StringBuilder output = new StringBuilder("Checking Curl dependencies...\n");

        try {
            java.lang.Process process = Runtime.getRuntime().exec(lddCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = errorReader.readLine()) != null) {
                output.append(line).append("\n");
            }

            process.waitFor();
        } catch (Exception e) {
            output.append("Error running ldd: ").append(e.getMessage());
        }

        Log.d("CurlDeps", output.toString()); // Log the full dependency output
        return output.toString();
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

    protected void applyMoboxRuntimeContracts(ImageFs imageFs, EnvVars launchEnv, File rootDir, String winePath) {
        launchEnv.put("PATH", buildRuntimePath(imageFs, rootDir, winePath));

        File runtimeTmpDir = imageFs.getTmpDir();
        if (!runtimeTmpDir.exists()) {
            runtimeTmpDir.mkdirs();
        }
        String runtimeTmpPath = runtimeTmpDir.getPath();
        if (!launchEnv.has("TMPDIR")) launchEnv.put("TMPDIR", runtimeTmpPath);
        if (!launchEnv.has("TEMP")) launchEnv.put("TEMP", runtimeTmpPath);
        if (!launchEnv.has("TMP")) launchEnv.put("TMP", runtimeTmpPath);

        boolean hasGlibcBin = new File(rootDir, "/usr/glibc/bin").isDirectory();
        launchEnv.put("AERO_RUNTIME_BOOTSTRAP_MODEL", "contents_contract");
        launchEnv.put("AERO_RUNTIME_COMPONENT_MODEL", "wcp_contents");
        launchEnv.put("AERO_RUNTIME_MOBOX_PATH_COMPAT", hasGlibcBin ? "1" : "0");
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
        return lowered.contains("explorer /desktop=shell");
    }

    private String resolveRequestedEmulator() {
        String emulator = container.getEmulator();
        if (shortcut != null) {
            emulator = shortcut.getExtra("emulator", container.getEmulator());
        }
        return emulator == null ? "" : emulator;
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
            String hodll;
            if (effectiveEmulator.toLowerCase(Locale.ROOT).equals("fexcore")) {
                hodll = desktopShellBootstrap ? "wowbox64.dll" : "libwow64fex.dll";
            } else {
                hodll = "wowbox64.dll";
            }
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
            return winePath + "/" + guestExecutable;
        }
        return imageFs.getBinDir() + "/box64 " + guestExecutable;
    }

    protected int execGuestProgram() {
        Context context = environment.getContext();
        ImageFs imageFs = environment.getImageFs();
        File rootDir = imageFs.getRootDir();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enableBox64Logs = preferences.getBoolean("enable_box64_logs", false);
        boolean openWithAndroidBrowser = preferences.getBoolean("open_with_android_browser", false);
        boolean shareAndroidClipboard = preferences.getBoolean("share_android_clipboard", false);

        EnvVars launchEnv = new EnvVars();
        String requestedEmulator = resolveRequestedEmulator();
        boolean desktopShellBootstrap = wineInfo.isArm64EC() && isDesktopShellBootstrap();
        boolean fexRequested = "fexcore".equalsIgnoreCase(requestedEmulator);
        String effectiveEmulator = desktopShellBootstrap && fexRequested ? "wowbox64" : requestedEmulator;

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
        String wineBinPath = wineRootPath + "/bin";
        String wineLibPath = wineRootPath + "/lib";
        String wineLib64Path = wineRootPath + "/lib64";
        String wineUnixPath = wineLibPath + "/wine/aarch64-unix";
        String wineDllPath = wineLibPath + "/wine";

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


        launchEnv.put("ANDROID_SYSVSHM_SERVER", rootDir.getPath() + UnixSocketConfig.SYSVSHM_SERVER_PATH);

        String primaryDNS = "8.8.4.4";
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Service.CONNECTIVITY_SERVICE);
        if (connectivityManager.getActiveNetwork() != null) {
            ArrayList<InetAddress> dnsServers = new ArrayList<>(connectivityManager.getLinkProperties(connectivityManager.getActiveNetwork()).getDnsServers());
            primaryDNS = dnsServers.get(0).toString().substring(1);
        }
        launchEnv.put("ANDROID_RESOLV_DNS", primaryDNS);
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

        String command = buildGuestCommand(context, imageFs, rootDir, launchEnv, winePath, effectiveEmulator, desktopShellBootstrap);

        // **Maybe remove this: Set execute permissions for box64 if necessary (Glibc/Proot artifact)
        File box64File = new File(rootDir, "/usr/bin/box64");
        if (box64File.exists()) {
            FileUtils.chmod(box64File, 0755);
        }

        return ProcessHelper.exec(command, launchEnv.toStringArray(), rootDir, (status) -> {
            synchronized (lock) {
                pid = -1;
            }

            if (terminationCallback != null)
                terminationCallback.call(status);
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
}
