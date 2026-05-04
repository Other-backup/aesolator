package com.winlator.cmod.container;

import android.os.Environment;
import android.util.Log;

import com.winlator.cmod.box64.Box64Preset;
import com.winlator.cmod.box86_64.Box86_64Preset;
import com.winlator.cmod.contentdialog.DXVKConfigDialog;
import com.winlator.cmod.contentdialog.GraphicsDriverConfigDialog;
import com.winlator.cmod.contentdialog.WineD3DConfigDialog;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineThemeManager;
import com.winlator.cmod.fexcore.FEXCorePreset;
import com.winlator.cmod.winhandler.WinHandler;
import com.winlator.cmod.xenvironment.ImageFs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class Container {
    public enum XrControllerMapping {
        BUTTON_A, BUTTON_B, BUTTON_X, BUTTON_Y, BUTTON_GRIP, BUTTON_TRIGGER,
        THUMBSTICK_UP, THUMBSTICK_DOWN, THUMBSTICK_LEFT, THUMBSTICK_RIGHT
    }

    public static final String EXTERNAL_DISPLAY_MODE_OFF = "off";
    public static final String EXTERNAL_DISPLAY_MODE_TOUCHPAD = "touchpad";
    public static final String EXTERNAL_DISPLAY_MODE_KEYBOARD = "keyboard";
    public static final String EXTERNAL_DISPLAY_MODE_HYBRID = "hybrid";
    public static final String DEFAULT_EXTERNAL_DISPLAY_MODE = EXTERNAL_DISPLAY_MODE_OFF;

    public static final String DEFAULT_ENV_VARS = "WRAPPER_MAX_IMAGE_COUNT=0 ZINK_DESCRIPTORS=lazy ZINK_DEBUG=compact MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=512MB mesa_glthread=true AERO_WINE_SYNC_BACKEND=auto TU_DEBUG=noconform,sysmem DXVK_HUD=devinfo,fps,frametimes,gpuload,version,api";
    public static final String DEFAULT_SCREEN_SIZE = "1280x720";
    public static final String DEFAULT_GRAPHICS_DRIVER = GraphicsDrivers.WRAPPER;
    public static final String DEFAULT_AUDIO_DRIVER = "alsa";
    public static final String DEFAULT_EMULATOR = "FEXCore";
    public static final String DEFAULT_DXWRAPPER = "dxvk+vkd3d";
    public static final String GLIBC = "glibc";
    public static final String BIONIC = "bionic";
    public static final String DEFAULT_VARIANT = BIONIC;
    public static final String DEFAULT_DXWRAPPERCONFIG = "version=" + DefaultVersion.DXVK + ",framerate=0,async=0,asyncCache=0" + ",vkd3dVersion=" + DefaultVersion.VKD3D + ",vkd3dLevel=12_1" + ",csmt=3" + ",gpuName=NVIDIA GeForce GTX 480" + ",videoMemorySize=2048" + ",strict_shader_math=1" + ",OffscreenRenderingMode=fbo" + ",renderer=gl";
    public static final String DEFAULT_GRAPHICSDRIVERCONFIG =
            "vulkanVersion=1.4" + ";version=" + ";blacklistedExtensions=" + ";maxDeviceMemory=0" + ";presentMode=mailbox" + ";syncFrame=0" + ";disablePresentWait=0" + ";resourceType=auto" + ";bcnEmulation=auto" + ";bcnEmulationType=compute" + ";bcnEmulationCache=0" + ";gpuName=Device";
    public static final String GRAPHICS_CONFIG_LEGACY_REQUESTED_DRIVER = "legacyRequestedDriver";
    public static final String GRAPHICS_CONFIG_LEGACY_PROVIDER_HINT = "legacyProviderHint";
    public static final String GRAPHICS_CONFIG_LEGACY_POLICY = "legacyPolicy";
    public static final String DEFAULT_DDRAWRAPPER = "none";
    public static final String DEFAULT_WINCOMPONENTS = "direct3d=1,directsound=0,directmusic=0,directshow=0,directplay=0,xaudio=0,vcrun2010=1";
    public static final String FALLBACK_WINCOMPONENTS = "direct3d=1,directsound=1,directmusic=1,directshow=1,directplay=1,xaudio=1,vcrun2010=1";
    public static final String DEFAULT_DRIVES = buildDefaultDrives();
    public static final byte STARTUP_SELECTION_NORMAL = 0;
    public static final byte STARTUP_SELECTION_ESSENTIAL = 1;
    public static final byte STARTUP_SELECTION_AGGRESSIVE = 2;
    public static final String SUSPEND_POLICY_AUTO = "auto";
    public static final String SUSPEND_POLICY_NEVER = "never";
    public static final String SUSPEND_POLICY_MANUAL = "manual";
    public static final String STEAM_TYPE_NORMAL = "normal";
    public static final String STEAM_TYPE_LIGHT = "light";
    public static final String STEAM_TYPE_ULTRALIGHT = "ultralight";
    public static final byte MAX_DRIVE_LETTERS = 26;

    public final int id;

    private String name;
    private String screenSize = DEFAULT_SCREEN_SIZE;
    private String envVars = DEFAULT_ENV_VARS;
    private String graphicsDriver = DEFAULT_GRAPHICS_DRIVER;
    private String graphicsDriverVersion = DefaultVersion.WRAPPER;
    private String graphicsDriverConfig = DEFAULT_GRAPHICSDRIVERCONFIG;
    private String dxwrapper = DEFAULT_DXWRAPPER;
    private String dxwrapperConfig = "";
    private String wincomponents = DEFAULT_WINCOMPONENTS;
    private String audioDriver = DEFAULT_AUDIO_DRIVER;
    private String drives = DEFAULT_DRIVES;
    private String wineVersion = WineInfo.MAIN_WINE_VERSION.identifier();
    private boolean showFPS;
    private boolean fullscreenStretched;
    private boolean launchRealSteam;
    private boolean allowSteamUpdates;
    private boolean wow64Mode = true;
    private boolean needsUnpacking = true;
    private byte startupSelection = STARTUP_SELECTION_ESSENTIAL;
    private String cpuList;
    private String cpuListWoW64;
    private String desktopTheme = WineThemeManager.DEFAULT_DESKTOP_THEME;
    private String box86Version = DefaultVersion.BOX64;
    private String box64Version;
    private String box86Preset = Box86_64Preset.COMPATIBILITY;
    private String box64Preset = Box64Preset.COMPATIBILITY;
    private String fexcoreVersion;
    private String fexcorePreset = FEXCorePreset.INTERMEDIATE;
    private File rootDir;
    private String installPath = "";
    private JSONObject extraData;
    private JSONObject sessionMetadata;
    private int rcfileId;
    private String midiSoundFont = "";
    private int inputType = WinHandler.DEFAULT_INPUT_TYPE;
    private String lc_all = "";
    private int primaryController = 1;
    private String controllerMapping = new String(new char[XrControllerMapping.values().length]);
    private String execArgs = "";
    private String executablePath = "";
    private boolean sdlControllerAPI;
    private String language = "english";
    private byte dinputMapperType = 1;
    private boolean disableMouseInput;
    private boolean touchscreenMode;
    private boolean shooterMode = true;
    private String gestureConfig = "";
    private String externalDisplayMode = DEFAULT_EXTERNAL_DISPLAY_MODE;
    private boolean externalDisplaySwap;
    private boolean useDRI3 = true;
    private String steamType = STEAM_TYPE_NORMAL;
    private boolean gstreamerWorkaround;
    private boolean forceDlc;
    private boolean steamOfflineMode;
    private boolean useLegacyDRM;
    private boolean unpackFiles;
    private String suspendPolicy = SUSPEND_POLICY_MANUAL;
    private boolean portraitMode;
    private String emulator;
    private String containerVariant = DEFAULT_VARIANT;

    private ContainerManager containerManager;

    private static String buildDefaultDrives() {
        String externalRoot = resolveExternalStoragePath();
        String downloadsRoot = resolveDownloadsPath(externalRoot);
        return "F:" + externalRoot + "D:" + downloadsRoot;
    }

    private static String resolveExternalStoragePath() {
        try {
            File dir = Environment.getExternalStorageDirectory();
            if (dir != null) return dir.getAbsolutePath();
        } catch (Throwable ignored) {
        }
        return "/storage/emulated/0";
    }

    private static String resolveDownloadsPath(String externalRoot) {
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (dir != null) return dir.getAbsolutePath();
        } catch (Throwable ignored) {
        }
        return externalRoot + "/Download";
    }

    public Container(int id) {
        this.id = id;
        this.name = "Container-" + id;
    }

    public Container(int id, ContainerManager containerManager) {
        this.id = id;
        this.name = "Container-" + id;
        this.containerManager = containerManager;
    }

    public ContainerManager getManager() {
        return containerManager;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(String screenSize) {
        this.screenSize = screenSize;
    }

    public String getEnvVars() {
        return envVars;
    }

    public void setEnvVars(String envVars) {
        this.envVars = envVars != null ? envVars : "";
    }

    public String getGraphicsDriver() {
        return normalizeGraphicsDriver(graphicsDriver);
    }

    public void setGraphicsDriver(String graphicsDriver) {
        this.graphicsDriver = normalizeGraphicsDriver(graphicsDriver);
    }

    public static String normalizeGraphicsDriver(String graphicsDriver) {
        String normalized = GraphicsDrivers.normalize(graphicsDriver);
        if (normalized.isEmpty()) return DEFAULT_GRAPHICS_DRIVER;
        if (GraphicsDrivers.isKnown(normalized)) return normalized;
        if (isLegacyGraphicsCompatToken(normalized)) return DEFAULT_GRAPHICS_DRIVER;
        return normalized;
    }

    public static String extractLegacyGraphicsProviderHint(String graphicsDriver) {
        String raw = GraphicsDrivers.normalize(graphicsDriver == null ? "" : graphicsDriver);
        if (raw.isEmpty()) return "";
        if (GraphicsDrivers.isKnown(raw)) return "";
        if (raw.contains("turnip")) return "turnip-vulkan";
        if (raw.contains("zink")) return "zink-opengl";
        if (raw.contains("llvmpipe")) return "llvmpipe-software";
        return "";
    }

    public static String extractLegacyGraphicsPolicy(String graphicsDriver) {
        String hint = extractLegacyGraphicsProviderHint(graphicsDriver);
        if (hint.isEmpty()) return "";
        if (hint.endsWith("-route")) return "route-degraded";
        if ("llvmpipe-software".equals(hint)) return "software-secondary path";
        return "provider-compat";
    }

    public static String extractLegacyGraphicsRequestedDriver(String graphicsDriver) {
        String normalized = GraphicsDrivers.normalize(graphicsDriver == null ? "" : graphicsDriver);
        if (normalized.isEmpty() || DEFAULT_GRAPHICS_DRIVER.equals(normalized)) return "";
        if (GraphicsDrivers.isKnown(normalized)) return "";
        return isLegacyGraphicsCompatToken(normalized) ? normalized : "";
    }

    public static String reconcileLegacyGraphicsConfig(String rawGraphicsDriver, String graphicsDriverConfig) {
        String normalizedDriver = normalizeGraphicsDriver(rawGraphicsDriver);
        if (GraphicsDrivers.usesKeyValueConfig(normalizedDriver)) {
            return graphicsDriverConfig == null ? "" : graphicsDriverConfig;
        }

        HashMap<String, String> config = GraphicsDriverConfigDialog.parseGraphicsDriverConfig(
                graphicsDriverConfig == null || graphicsDriverConfig.trim().isEmpty()
                        ? DEFAULT_GRAPHICSDRIVERCONFIG
                        : graphicsDriverConfig
        );
        String requestedDriver = extractLegacyGraphicsRequestedDriver(rawGraphicsDriver);
        String existingRequestedDriver = trimConfigValue(config.get(GRAPHICS_CONFIG_LEGACY_REQUESTED_DRIVER));

        // Preserve existing donor-imported wrapper route metadata when a wrapper-normalized route is re-saved unchanged.
        if (requestedDriver.isEmpty() && DEFAULT_GRAPHICS_DRIVER.equals(normalizedDriver) && !existingRequestedDriver.isEmpty()) {
            requestedDriver = existingRequestedDriver;
        }

        if (requestedDriver.isEmpty()) {
            config.remove(GRAPHICS_CONFIG_LEGACY_REQUESTED_DRIVER);
            config.remove(GRAPHICS_CONFIG_LEGACY_PROVIDER_HINT);
            config.remove(GRAPHICS_CONFIG_LEGACY_POLICY);
        } else {
            config.put(GRAPHICS_CONFIG_LEGACY_REQUESTED_DRIVER, requestedDriver);
            config.put(GRAPHICS_CONFIG_LEGACY_PROVIDER_HINT, extractLegacyGraphicsProviderHint(requestedDriver));
            config.put(GRAPHICS_CONFIG_LEGACY_POLICY, extractLegacyGraphicsPolicy(requestedDriver));
        }

        return GraphicsDriverConfigDialog.toGraphicsDriverConfig(config);
    }

    public static String resolveLegacyGraphicsRequestedDriver(String graphicsDriver, String graphicsDriverConfig) {
        String configuredValue = extractGraphicsConfigValue(graphicsDriverConfig, GRAPHICS_CONFIG_LEGACY_REQUESTED_DRIVER);
        return configuredValue.isEmpty() ? extractLegacyGraphicsRequestedDriver(graphicsDriver) : configuredValue;
    }

    public static String resolveLegacyGraphicsProviderHint(String graphicsDriver, String graphicsDriverConfig) {
        String configuredValue = extractGraphicsConfigValue(graphicsDriverConfig, GRAPHICS_CONFIG_LEGACY_PROVIDER_HINT);
        if (!configuredValue.isEmpty()) return configuredValue;
        String requestedDriver = resolveLegacyGraphicsRequestedDriver(graphicsDriver, graphicsDriverConfig);
        return requestedDriver.isEmpty() ? extractLegacyGraphicsProviderHint(graphicsDriver) : extractLegacyGraphicsProviderHint(requestedDriver);
    }

    public static String resolveLegacyGraphicsPolicy(String graphicsDriver, String graphicsDriverConfig) {
        String configuredValue = extractGraphicsConfigValue(graphicsDriverConfig, GRAPHICS_CONFIG_LEGACY_POLICY);
        if (!configuredValue.isEmpty()) return configuredValue;
        String requestedDriver = resolveLegacyGraphicsRequestedDriver(graphicsDriver, graphicsDriverConfig);
        return requestedDriver.isEmpty() ? extractLegacyGraphicsPolicy(graphicsDriver) : extractLegacyGraphicsPolicy(requestedDriver);
    }

    private static boolean isLegacyGraphicsCompatToken(String normalized) {
        return normalized.contains("turnip")
                || normalized.contains("llvmpipe")
                || normalized.contains("zink");
    }

    private static String extractGraphicsConfigValue(String graphicsDriverConfig, String key) {
        HashMap<String, String> config = GraphicsDriverConfigDialog.parseGraphicsDriverConfig(graphicsDriverConfig);
        return trimConfigValue(config.get(key));
    }

    private static String trimConfigValue(String value) {
        return value == null ? "" : value.trim();
    }

    public String getGraphicsDriverVersion() {
        return graphicsDriverVersion != null ? graphicsDriverVersion : DefaultVersion.WRAPPER;
    }

    public void setGraphicsDriverVersion(String graphicsDriverVersion) {
        this.graphicsDriverVersion = graphicsDriverVersion != null ? graphicsDriverVersion : DefaultVersion.WRAPPER;
    }

    public String getGraphicsDriverConfig() {
        return graphicsDriverConfig != null ? graphicsDriverConfig : "";
    }

    public void setGraphicsDriverConfig(String graphicsDriverConfig) {
        this.graphicsDriverConfig = graphicsDriverConfig != null ? graphicsDriverConfig : "";
    }

    public String getDXWrapper() {
        return dxwrapper;
    }

    public void setDXWrapper(String dxwrapper) {
        this.dxwrapper = dxwrapper;
    }

    public String getDXWrapperConfig() {
        return dxwrapperConfig != null ? dxwrapperConfig : "";
    }

    public void setDXWrapperConfig(String dxwrapperConfig) {
        this.dxwrapperConfig = dxwrapperConfig != null ? dxwrapperConfig : "";
    }

    public String getAudioDriver() {
        return audioDriver;
    }

    public void setAudioDriver(String audioDriver) {
        this.audioDriver = audioDriver;
    }

    public String getWinComponents() {
        return wincomponents;
    }

    public void setWinComponents(String wincomponents) {
        this.wincomponents = wincomponents;
    }

    public String getDrives() {
        return drives;
    }

    public void setDrives(String drives) {
        this.drives = drives;
    }

    public String getLC_ALL() {
        return lc_all;
    }

    public void setLC_ALL(String lc_all) {
        this.lc_all = lc_all;
    }

    public int getPrimaryController() {
        return primaryController;
    }

    public void setPrimaryController(int primaryController) {
        this.primaryController = primaryController;
    }

    public byte getControllerMapping(XrControllerMapping input) {
        return (byte) controllerMapping.charAt(input.ordinal());
    }

    public void setControllerMapping(String controllerMapping) {
        this.controllerMapping = controllerMapping;
    }

    public boolean isFullscreenStretched() {
        return fullscreenStretched;
    }

    public void setFullscreenStretched(boolean fullscreenStretched) {
        this.fullscreenStretched = fullscreenStretched;
    }

    public boolean isShowFPS() {
        return showFPS;
    }

    public void setShowFPS(boolean showFPS) {
        this.showFPS = showFPS;
    }

    public boolean isLaunchRealSteam() {
        return launchRealSteam;
    }

    public void setLaunchRealSteam(boolean launchRealSteam) {
        this.launchRealSteam = launchRealSteam;
    }

    public boolean isAllowSteamUpdates() {
        return allowSteamUpdates;
    }

    public void setAllowSteamUpdates(boolean allowSteamUpdates) {
        this.allowSteamUpdates = allowSteamUpdates;
    }

    public boolean isWoW64Mode() {
        return wow64Mode;
    }

    public void setWoW64Mode(boolean wow64Mode) {
        this.wow64Mode = wow64Mode;
    }

    public boolean isNeedsUnpacking() {
        return needsUnpacking;
    }

    public void setNeedsUnpacking(boolean needsUnpacking) {
        this.needsUnpacking = needsUnpacking;
    }

    public byte getStartupSelection() {
        return startupSelection;
    }

    public void setStartupSelection(byte startupSelection) {
        this.startupSelection = startupSelection;
    }

    public String getCPUList() {
        return getCPUList(false);
    }

    public String getCPUList(boolean allowFallback) {
        return cpuList != null ? cpuList : (allowFallback ? getFallbackCPUList() : null);
    }

    public void setCPUList(String cpuList) {
        this.cpuList = cpuList != null && !cpuList.isEmpty() ? cpuList : null;
    }

    public String getCPUListWoW64() {
        return getCPUListWoW64(false);
    }

    public String getCPUListWoW64(boolean allowFallback) {
        return cpuListWoW64 != null ? cpuListWoW64 : (allowFallback ? getFallbackCPUListWoW64() : null);
    }

    public void setCPUListWoW64(String cpuListWoW64) {
        this.cpuListWoW64 = cpuListWoW64 != null && !cpuListWoW64.isEmpty() ? cpuListWoW64 : null;
    }

    public String getBox86Version() {
        return box86Version;
    }

    public void setBox86Version(String box86Version) {
        this.box86Version = box86Version;
    }

    public String getBox64Version() {
        return box64Version;
    }

    public void setBox64Version(String version) {
        this.box64Version = version;
    }

    public String getBox86Preset() {
        return box86Preset;
    }

    public void setBox86Preset(String box86Preset) {
        this.box86Preset = box86Preset;
    }

    public String getBox64Preset() {
        return box64Preset;
    }

    public void setBox64Preset(String box64Preset) {
        this.box64Preset = box64Preset;
    }

    public void setFEXCoreVersion(String version) {
        this.fexcoreVersion = version;
    }

    public String getFEXCoreVersion() {
        return fexcoreVersion;
    }

    public void setFEXCorePreset(String preset) {
        this.fexcorePreset = preset;
    }

    public String getFEXCorePreset() {
        return fexcorePreset;
    }

    public void setEmulator(String emulator) {
        if (emulator == null || emulator.trim().isEmpty()) {
            this.emulator = DEFAULT_EMULATOR;
        }
        else {
            this.emulator = emulator;
        }
    }

    public String getEmulator() {
        if (emulator == null || emulator.trim().isEmpty()) {
            return DEFAULT_EMULATOR;
        }
        return emulator;
    }

    public void setContainerVariant(String containerVariant) {
        if (containerVariant == null || containerVariant.trim().isEmpty()) {
            this.containerVariant = DEFAULT_VARIANT;
        }
        else if (GLIBC.equalsIgnoreCase(containerVariant)) {
            this.containerVariant = GLIBC;
        }
        else {
            this.containerVariant = BIONIC;
        }
    }

    public String getContainerVariant() {
        return containerVariant == null || containerVariant.trim().isEmpty() ? DEFAULT_VARIANT : containerVariant;
    }

    public File getRootDir() {
        return rootDir;
    }

    public void setRootDir(File rootDir) {
        this.rootDir = rootDir;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath != null ? installPath : "";
    }

    public void setExtraData(JSONObject extraData) {
        this.extraData = extraData;
    }

    public String getExtra(String name) {
        return getExtra(name, "");
    }

    public String getExtra(String name, String fallback) {
        try {
            return extraData != null && extraData.has(name) ? extraData.getString(name) : fallback;
        }
        catch (JSONException e) {
            return fallback;
        }
    }

    public void putExtra(String name, Object value) {
        if (extraData == null) extraData = new JSONObject();
        try {
            if (value != null) {
                extraData.put(name, value);
            }
            else {
                extraData.remove(name);
            }
        }
        catch (JSONException e) {
            Log.e("Container", "Failed to put extra", e);
        }
    }

    private static String sanitizeSessionMetadataKey(String name) {
        if (name == null) return "";
        return name.trim();
    }

    public String getSessionMetadata(String name) {
        return getSessionMetadata(name, "");
    }

    public String getSessionMetadata(String name, String fallback) {
        String key = sanitizeSessionMetadataKey(name);
        if (key.isEmpty()) return fallback;
        try {
            return sessionMetadata != null && sessionMetadata.has(key) ? sessionMetadata.getString(key) : fallback;
        }
        catch (JSONException e) {
            return fallback;
        }
    }

    public void putSessionMetadata(String name, Object value) {
        String key = sanitizeSessionMetadataKey(name);
        if (key.isEmpty()) return;
        if (sessionMetadata == null) sessionMetadata = new JSONObject();
        try {
            if (value != null) {
                sessionMetadata.put(key, value);
            }
            else {
                sessionMetadata.remove(key);
            }
        }
        catch (JSONException e) {
            Log.e("Container", "Failed to put session metadata", e);
        }
    }

    public void clearSessionMetadata() {
        sessionMetadata = null;
    }

    public String getWineVersion() {
        return wineVersion;
    }

    public void setWineVersion(String wineVersion) {
        this.wineVersion = wineVersion;
    }

    public File getConfigFile() {
        return new File(rootDir, ".container");
    }

    public File getDesktopDir() {
        return new File(rootDir, ".wine/drive_c/users/" + ImageFs.USER + "/Desktop/");
    }

    public File getStartMenuDir() {
        return new File(rootDir, ".wine/drive_c/ProgramData/Microsoft/Windows/Start Menu/");
    }

    public File getIconsDir(int size) {
        return new File(rootDir, ".local/share/icons/hicolor/" + size + "x" + size + "/apps/");
    }

    public String getDesktopTheme() {
        return desktopTheme;
    }

    public void setDesktopTheme(String desktopTheme) {
        this.desktopTheme = desktopTheme;
    }

    public int getRCFileId() {
        return rcfileId;
    }

    public void setRcfileId(int id) {
        rcfileId = id;
    }

    public String getMIDISoundFont() {
        return midiSoundFont;
    }

    public void setMidiSoundFont(String fileName) {
        midiSoundFont = fileName;
    }

    public int getInputType() {
        return inputType;
    }

    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    public String getExecArgs() {
        return execArgs;
    }

    public void setExecArgs(String execArgs) {
        this.execArgs = execArgs != null ? execArgs : "";
    }

    public String getExecutablePath() {
        return executablePath;
    }

    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath != null ? executablePath : "";
    }

    public boolean isSdlControllerAPI() {
        return sdlControllerAPI;
    }

    public void setSdlControllerAPI(boolean sdlControllerAPI) {
        this.sdlControllerAPI = sdlControllerAPI;
    }

    public String getLanguage() {
        return language != null ? language : "english";
    }

    public void setLanguage(String language) {
        this.language = (language != null && !language.isEmpty()) ? language : "english";
    }

    public boolean isGstreamerWorkaround() {
        return gstreamerWorkaround;
    }

    public void setGstreamerWorkaround(boolean gstreamerWorkaround) {
        this.gstreamerWorkaround = gstreamerWorkaround;
    }

    public byte getDinputMapperType() {
        return dinputMapperType;
    }

    public void setDinputMapperType(byte dinputMapperType) {
        this.dinputMapperType = dinputMapperType;
    }

    public boolean isDisableMouseInput() {
        return disableMouseInput;
    }

    public void setDisableMouseInput(boolean disableMouseInput) {
        this.disableMouseInput = disableMouseInput;
    }

    public boolean isTouchscreenMode() {
        return touchscreenMode;
    }

    public void setTouchscreenMode(boolean touchscreenMode) {
        this.touchscreenMode = touchscreenMode;
    }

    public boolean isShooterMode() {
        return shooterMode;
    }

    public void setShooterMode(boolean shooterMode) {
        this.shooterMode = shooterMode;
    }

    public String getGestureConfig() {
        return gestureConfig != null ? gestureConfig : "";
    }

    public void setGestureConfig(String gestureConfig) {
        this.gestureConfig = gestureConfig != null ? gestureConfig : "";
    }

    public String getExternalDisplayMode() {
        return externalDisplayMode != null ? externalDisplayMode : DEFAULT_EXTERNAL_DISPLAY_MODE;
    }

    public void setExternalDisplayMode(String externalDisplayMode) {
        this.externalDisplayMode = externalDisplayMode != null ? externalDisplayMode : DEFAULT_EXTERNAL_DISPLAY_MODE;
    }

    public boolean isExternalDisplaySwap() {
        return externalDisplaySwap;
    }

    public void setExternalDisplaySwap(boolean externalDisplaySwap) {
        this.externalDisplaySwap = externalDisplaySwap;
    }

    public boolean isUseDRI3() {
        return useDRI3;
    }

    public void setUseDRI3(boolean useDRI3) {
        this.useDRI3 = useDRI3;
    }

    public String getSteamType() {
        return steamType;
    }

    public void setSteamType(String steamType) {
        String normalized = steamType == null ? "" : steamType.toLowerCase(Locale.ROOT);
        switch (normalized) {
            case STEAM_TYPE_LIGHT:
                this.steamType = STEAM_TYPE_LIGHT;
                break;
            case STEAM_TYPE_ULTRALIGHT:
                this.steamType = STEAM_TYPE_ULTRALIGHT;
                break;
            default:
                this.steamType = STEAM_TYPE_NORMAL;
                break;
        }
    }

    public boolean isForceDlc() {
        return forceDlc;
    }

    public void setForceDlc(boolean forceDlc) {
        this.forceDlc = forceDlc;
    }

    public boolean isSteamOfflineMode() {
        return steamOfflineMode;
    }

    public void setSteamOfflineMode(boolean steamOfflineMode) {
        this.steamOfflineMode = steamOfflineMode;
    }

    public boolean isUseLegacyDRM() {
        return useLegacyDRM;
    }

    public void setUseLegacyDRM(boolean useLegacyDRM) {
        this.useLegacyDRM = useLegacyDRM;
    }

    public boolean isUnpackFiles() {
        return unpackFiles;
    }

    public void setUnpackFiles(boolean unpackFiles) {
        this.unpackFiles = unpackFiles;
    }

    public static String normalizeSuspendPolicy(String suspendPolicy) {
        String normalized = suspendPolicy == null ? "" : suspendPolicy.toLowerCase(Locale.ROOT);
        switch (normalized) {
            case SUSPEND_POLICY_NEVER:
                return SUSPEND_POLICY_NEVER;
            case SUSPEND_POLICY_MANUAL:
                return SUSPEND_POLICY_MANUAL;
            case SUSPEND_POLICY_AUTO:
            default:
                return SUSPEND_POLICY_AUTO;
        }
    }

    public String getSuspendPolicy() {
        return normalizeSuspendPolicy(suspendPolicy);
    }

    public void setSuspendPolicy(String suspendPolicy) {
        this.suspendPolicy = normalizeSuspendPolicy(suspendPolicy);
    }

    public boolean isPortraitMode() {
        return portraitMode;
    }

    public void setPortraitMode(boolean portraitMode) {
        this.portraitMode = portraitMode;
    }

    public String getContainerJson() {
        String content = FileUtils.readString(getConfigFile());
        return content == null ? "{}" : content.replace("\\u0000", "").replace("\u0000", "");
    }

    public Iterable<String[]> drivesIterator() {
        return drivesIterator(drives);
    }

    public static char getNextAvailableDriveLetter(String drives) throws Exception {
        char drive = 'A';
        while (drives.contains(drive + ":")) {
            drive += 1;
            if (drive > 'Z') {
                throw new Exception("All drive letters taken");
            }
        }
        return drive;
    }

    public static Iterable<String[]> drivesIterator(final String drives) {
        final int[] index = {drives.indexOf(":")};
        final String[] item = new String[2];
        return () -> new Iterator<String[]>() {
            @Override
            public boolean hasNext() {
                return index[0] != -1;
            }

            @Override
            public String[] next() {
                item[0] = String.valueOf(drives.charAt(index[0] - 1));
                int nextIndex = drives.indexOf(":", index[0] + 1);
                item[1] = drives.substring(index[0] + 1, nextIndex != -1 ? nextIndex - 1 : drives.length());
                index[0] = nextIndex;
                return item;
            }
        };
    }

    public void saveData() {
        try {
            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("name", name);
            data.put("screenSize", screenSize);
            data.put("envVars", envVars);
            data.put("cpuList", cpuList);
            data.put("cpuListWoW64", cpuListWoW64);
            data.put("graphicsDriver", graphicsDriver);
            data.put("graphicsDriverVersion", graphicsDriverVersion);
            data.put("graphicsDriverConfig", graphicsDriverConfig);
            data.put("emulator", getEmulator());
            data.put("dxwrapper", dxwrapper);
            if (!getDXWrapperConfig().isEmpty()) data.put("dxwrapperConfig", dxwrapperConfig);
            data.put("audioDriver", audioDriver);
            data.put("wincomponents", wincomponents);
            data.put("drives", drives);
            data.put("showFPS", showFPS);
            data.put("fullscreenStretched", fullscreenStretched);
            data.put("launchRealSteam", launchRealSteam);
            data.put("allowSteamUpdates", allowSteamUpdates);
            data.put("inputType", inputType);
            data.put("dinputMapperType", dinputMapperType);
            data.put("wow64Mode", wow64Mode);
            data.put("needsUnpacking", needsUnpacking);
            data.put("startupSelection", startupSelection);
            data.put("box86Version", box86Version);
            data.put("box64Version", box64Version);
            data.put("box86Preset", box86Preset);
            data.put("box64Preset", box64Preset);
            data.put("fexcoreVersion", fexcoreVersion);
            data.put("fexcorePreset", fexcorePreset);
            data.put("desktopTheme", desktopTheme);
            data.put("containerVariant", getContainerVariant());
            data.put("extraData", extraData);
            data.put("sessionMetadata", sessionMetadata);
            data.put("rcfileId", rcfileId);
            data.put("midiSoundFont", midiSoundFont);
            data.put("lc_all", lc_all);
            data.put("primaryController", primaryController);
            data.put("controllerMapping", controllerMapping);
            data.put("execArgs", execArgs);
            data.put("executablePath", executablePath);
            data.put("installPath", installPath);
            data.put("sdlControllerAPI", sdlControllerAPI);
            data.put("disableMouseInput", disableMouseInput);
            data.put("touchscreenMode", touchscreenMode);
            data.put("shooterMode", shooterMode);
            if (!getGestureConfig().isEmpty()) data.put("gestureConfig", gestureConfig);
            data.put("externalDisplayMode", getExternalDisplayMode());
            data.put("externalDisplaySwap", externalDisplaySwap);
            data.put("useDRI3", useDRI3);
            data.put("steamType", steamType);
            data.put("language", language);
            data.put("gstreamerWorkaround", gstreamerWorkaround);
            data.put("forceDlc", forceDlc);
            data.put("steamOfflineMode", steamOfflineMode);
            data.put("useLegacyDRM", useLegacyDRM);
            data.put("unpackFiles", unpackFiles);
            data.put("suspendPolicy", getSuspendPolicy());
            data.put("portraitMode", portraitMode);
            if (!WineInfo.isMainWineVersion(wineVersion)) data.put("wineVersion", wineVersion);
            FileUtils.writeString(getConfigFile(), data.toString());
        }
        catch (JSONException e) {
            Log.e("Container", "Failed to save data", e);
        }
    }

    public void loadData(JSONObject data) throws JSONException {
        wineVersion = WineInfo.MAIN_WINE_VERSION.identifier();
        dxwrapperConfig = "";
        checkObsoleteOrMissingProperties(data);

        for (Iterator<String> it = data.keys(); it.hasNext(); ) {
            String key = it.next();
            switch (key) {
                case "name":
                    setName(data.getString(key));
                    break;
                case "screenSize":
                    setScreenSize(data.getString(key));
                    break;
                case "envVars":
                    setEnvVars(data.getString(key));
                    break;
                case "cpuList":
                    setCPUList(data.getString(key));
                    break;
                case "cpuListWoW64":
                    setCPUListWoW64(data.getString(key));
                    break;
                case "graphicsDriver":
                    setGraphicsDriver(data.getString(key));
                    break;
                case "graphicsDriverVersion":
                    setGraphicsDriverVersion(data.getString(key));
                    break;
                case "graphicsDriverConfig":
                    setGraphicsDriverConfig(data.getString(key));
                    break;
                case "emulator":
                    setEmulator(data.getString(key));
                    break;
                case "containerVariant":
                    setContainerVariant(data.getString(key));
                    break;
                case "wincomponents":
                    setWinComponents(data.getString(key));
                    break;
                case "dxwrapper":
                    setDXWrapper(data.getString(key));
                    break;
                case "dxwrapperConfig":
                    setDXWrapperConfig(data.getString(key));
                    break;
                case "drives":
                    setDrives(data.getString(key));
                    break;
                case "showFPS":
                    setShowFPS(data.getBoolean(key));
                    break;
                case "fullscreenStretched":
                    setFullscreenStretched(data.getBoolean(key));
                    break;
                case "launchRealSteam":
                    setLaunchRealSteam(data.getBoolean(key));
                    break;
                case "allowSteamUpdates":
                    setAllowSteamUpdates(data.getBoolean(key));
                    break;
                case "steamType":
                    setSteamType(data.getString(key));
                    break;
                case "language":
                    setLanguage(data.getString(key));
                    break;
                case "inputType":
                    setInputType(data.getInt(key));
                    break;
                case "dinputMapperType":
                    setDinputMapperType((byte) data.getInt(key));
                    break;
                case "wow64Mode":
                    setWoW64Mode(data.getBoolean(key));
                    break;
                case "needsUnpacking":
                    setNeedsUnpacking(data.getBoolean(key));
                    break;
                case "startupSelection":
                    setStartupSelection((byte) data.getInt(key));
                    break;
                case "extraData":
                    setExtraData(data.getJSONObject(key));
                    break;
                case "sessionMetadata":
                    try {
                        sessionMetadata = data.getJSONObject(key);
                    }
                    catch (JSONException e) {
                        sessionMetadata = null;
                    }
                    break;
                case "wineVersion":
                    setWineVersion(data.getString(key));
                    break;
                case "box86Version":
                    setBox86Version(data.getString(key));
                    break;
                case "box64Version":
                    setBox64Version(data.getString(key));
                    break;
                case "box86Preset":
                    setBox86Preset(data.getString(key));
                    break;
                case "box64Preset":
                    setBox64Preset(data.getString(key));
                    break;
                case "fexcoreVersion":
                    setFEXCoreVersion(data.getString(key));
                    break;
                case "fexcorePreset":
                    setFEXCorePreset(data.getString(key));
                    break;
                case "audioDriver":
                    setAudioDriver(data.getString(key));
                    break;
                case "desktopTheme":
                    setDesktopTheme(data.getString(key));
                    break;
                case "rcfileId":
                    setRcfileId(data.getInt(key));
                    break;
                case "midiSoundFont":
                    setMidiSoundFont(data.getString(key));
                    break;
                case "lc_all":
                    setLC_ALL(data.getString(key));
                    break;
                case "primaryController":
                    setPrimaryController(data.getInt(key));
                    break;
                case "controllerMapping":
                    controllerMapping = data.getString(key);
                    break;
                case "execArgs":
                    setExecArgs(data.getString(key));
                    break;
                case "executablePath":
                    setExecutablePath(data.getString(key));
                    break;
                case "installPath":
                    setInstallPath(data.getString(key));
                    break;
                case "sdlControllerAPI":
                    setSdlControllerAPI(data.getBoolean(key));
                    break;
                case "disableMouseInput":
                    setDisableMouseInput(data.getBoolean(key));
                    break;
                case "touchscreenMode":
                    setTouchscreenMode(data.getBoolean(key));
                    break;
                case "shooterMode":
                    setShooterMode(data.getBoolean(key));
                    break;
                case "gestureConfig":
                    setGestureConfig(data.optString(key, ""));
                    break;
                case "externalDisplayMode":
                    setExternalDisplayMode(data.getString(key));
                    break;
                case "externalDisplaySwap":
                    setExternalDisplaySwap(data.getBoolean(key));
                    break;
                case "useDRI3":
                    setUseDRI3(data.getBoolean(key));
                    break;
                case "gstreamerWorkaround":
                    setGstreamerWorkaround(data.getBoolean(key));
                    break;
                case "forceDlc":
                    setForceDlc(data.getBoolean(key));
                    break;
                case "steamOfflineMode":
                    setSteamOfflineMode(data.getBoolean(key));
                    break;
                case "useLegacyDRM":
                    setUseLegacyDRM(data.getBoolean(key));
                    break;
                case "unpackFiles":
                    setUnpackFiles(data.getBoolean(key));
                    break;
                case "suspendPolicy":
                    setSuspendPolicy(data.getString(key));
                    break;
                case "portraitMode":
                    setPortraitMode(data.getBoolean(key));
                    break;
            }
        }
    }

    public static void checkObsoleteOrMissingProperties(JSONObject data) {
        try {
            if (!data.has("envVars")) data.put("envVars", DEFAULT_ENV_VARS);
            if (!data.has("wincomponents")) data.put("wincomponents", DEFAULT_WINCOMPONENTS);

            if (data.has("dxcomponents")) {
                data.put("wincomponents", data.getString("dxcomponents"));
                data.remove("dxcomponents");
            }

            if (data.has("dxwrapper")) {
                String dxwrapper = data.getString("dxwrapper");
                if (dxwrapper.equals("original-wined3d")) {
                    data.put("dxwrapper", DEFAULT_DXWRAPPER);
                }
                else if (dxwrapper.startsWith("d8vk-") || dxwrapper.startsWith("dxvk-")) {
                    data.put("dxwrapper", dxwrapper);
                }
            }

            if (data.has("graphicsDriver")) {
                String graphicsDriver = data.getString("graphicsDriver");
                String graphicsDriverConfig = data.optString("graphicsDriverConfig", DEFAULT_GRAPHICSDRIVERCONFIG);
                data.put("graphicsDriver", normalizeGraphicsDriver(graphicsDriver));
                data.put("graphicsDriverConfig", reconcileLegacyGraphicsConfig(graphicsDriver, graphicsDriverConfig));
            }

            if (data.has("envVars") && data.has("extraData")) {
                JSONObject extraData = data.getJSONObject("extraData");
                int appVersion = Integer.parseInt(extraData.optString("appVersion", "0"));
                if (appVersion < 16) {
                    EnvVars defaultEnvVars = new EnvVars(DEFAULT_ENV_VARS);
                    EnvVars existingEnvVars = new EnvVars(data.getString("envVars"));
                    for (String name : defaultEnvVars) {
                        if (!existingEnvVars.has(name)) {
                            existingEnvVars.put(name, defaultEnvVars.get(name));
                        }
                    }
                    data.put("envVars", existingEnvVars.toString());
                }
            }

            KeyValueSet defaultWincomponents = new KeyValueSet(DEFAULT_WINCOMPONENTS);
            KeyValueSet currentWincomponents = new KeyValueSet(data.getString("wincomponents"));
            String result = "";

            for (String[] defaultWincomponent : defaultWincomponents) {
                String value = defaultWincomponent[1];

                for (String[] currentWincomponent : currentWincomponents) {
                    if (defaultWincomponent[0].equals(currentWincomponent[0])) {
                        value = currentWincomponent[1];
                        break;
                    }
                }

                result += (!result.isEmpty() ? "," : "") + defaultWincomponent[0] + "=" + value;
            }

            data.put("wincomponents", result);
        }
        catch (JSONException e) {
            Log.e("Container", "Failed to check obsolete or missing properties", e);
        }
    }

    public static String getFallbackCPUList() {
        String cpuList = "";
        int numProcessors = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < numProcessors; i++) cpuList += (!cpuList.isEmpty() ? "," : "") + i;
        return cpuList;
    }

    public static String getFallbackCPUListWoW64() {
        String cpuList = "";
        int numProcessors = Runtime.getRuntime().availableProcessors();
        for (int i = numProcessors / 2; i < numProcessors; i++) cpuList += (!cpuList.isEmpty() ? "," : "") + i;
        return cpuList;
    }

    public boolean hasEnvVar(String keyValue) {
        if (envVars == null || envVars.isEmpty()) return false;
        String[] vars = envVars.split(",");
        for (String var : vars) {
            if (var.trim().equalsIgnoreCase(keyValue.trim())) {
                return true;
            }
        }
        return false;
    }
}
