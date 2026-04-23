package com.winlator.cmod.container;

import android.content.Context;

import com.winlator.cmod.contentdialog.GraphicsDriverConfigDialog;
import com.winlator.cmod.contents.GladioOpenGLDriverPackageManager;
import com.winlator.cmod.contents.MesaOpenGLDriverPackageManager;
import com.winlator.cmod.contents.VirGLDriverPackageManager;
import com.winlator.cmod.contents.VortekVulkanDriverPackageManager;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.VortekExtensionPolicy;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class GraphicsDrivers {
    public static final String WRAPPER = "wrapper";
    public static final String VORTEK = "vortek";
    public static final String ZINK = "zink";
    public static final String VIRGL = "virgl";
    public static final String GLADIO = "gladio";
    public static final String GLADIUM = "gladium";
    public static final String AEMALI_PANVK = "aemali-panvk";
    public static final String AEMALI_GALLIUM = "aemali-gallium";
    private static final String GRAPHICS_DRIVER_ASSET_DIR = "graphics_driver";
    private static final String GRAPHICS_DRIVER_ARCHIVE_SUFFIX = ".tzst";
    private static final String DEFAULT_MALI_GALLIUM_DRIVER = "panfrost";
    private static final String DEFAULT_MALI_GL_VERSION = "3.1";
    private static final String DEFAULT_WRAPPER_ZINK_GL_VERSION = "4.0";
    private static final String ROUTING_AUTO = "auto";
    private static final String ROUTING_VULKAN_FIRST = "vulkan-first";
    private static final String ROUTING_OPENGL_FIRST = "opengl-first";
    private static final String DEFAULT_VORTEK_VK_MAX_VERSION = "1.4";
    private static final String DEFAULT_VORTEK_IMAGE_CACHE_SIZE = "256";
    private static final List<String> BUILTIN_DRIVER_ORDER = Arrays.asList(VORTEK, VIRGL);
    private static final Pattern VERSION_TOKEN_PATTERN = Pattern.compile("(\\d+|[A-Za-z]+)");
    private static final Pattern GRAPHICS_EXTENSION_TOKEN_PATTERN = Pattern.compile("GL_[A-Za-z0-9_]+");

    public static final class BundledDriverAsset {
        public final String driverId;
        public final String packageLabel;
        public final String version;
        public final String assetFileName;
        public final String assetPath;
        public final String extractProbePath;

        BundledDriverAsset(String driverId,
                           String packageLabel,
                           String version,
                           String assetFileName,
                           String assetPath,
                           String extractProbePath) {
            this.driverId = driverId;
            this.packageLabel = packageLabel;
            this.version = version;
            this.assetFileName = assetFileName;
            this.assetPath = assetPath;
            this.extractProbePath = extractProbePath;
        }
    }

    private static final class BuiltinDriverTemplate {
        final String driverId;
        final String packageLabel;
        final String fallbackVersion;
        final String extractProbePath;

        BuiltinDriverTemplate(String driverId, String packageLabel, String fallbackVersion, String extractProbePath) {
            this.driverId = driverId;
            this.packageLabel = packageLabel;
            this.fallbackVersion = fallbackVersion;
            this.extractProbePath = extractProbePath;
        }
    }

    public static String normalize(String rawGraphicsDriver) {
        String normalized = StringUtils.parseIdentifier(rawGraphicsDriver);
        if (normalized.isEmpty()) return WRAPPER;
        if (GLADIUM.equals(normalized)) return GLADIO;
        return normalized;
    }

    public static boolean isKnown(String graphicsDriver) {
        String normalized = normalize(graphicsDriver);
        return WRAPPER.equals(normalized)
                || VORTEK.equals(normalized)
                || ZINK.equals(normalized)
                || VIRGL.equals(normalized)
                || GLADIO.equals(normalized)
                || AEMALI_GALLIUM.equals(normalized);
    }

    public static boolean isWrapper(String graphicsDriver) {
        return WRAPPER.equals(normalize(graphicsDriver));
    }

    public static boolean isVortek(String graphicsDriver) {
        return VORTEK.equals(normalize(graphicsDriver));
    }

    public static boolean isZink(String graphicsDriver) {
        return ZINK.equals(normalize(graphicsDriver));
    }

    public static boolean isVirgl(String graphicsDriver) {
        return VIRGL.equals(normalize(graphicsDriver));
    }

    public static boolean isGladio(String graphicsDriver) {
        return GLADIO.equals(normalize(graphicsDriver));
    }

    public static boolean isMediaTekWrapperFamily(String graphicsDriver) {
        String normalized = normalize(graphicsDriver);
        return VORTEK.equals(normalized) || GLADIO.equals(normalized);
    }

    public static boolean isUnifiedVortekFamily(String graphicsDriver) {
        String normalized = normalize(graphicsDriver);
        return VORTEK.equals(normalized)
                || GLADIO.equals(normalized)
                || AEMALI_GALLIUM.equals(normalized)
                || AEMALI_PANVK.equals(normalized);
    }

    public static boolean isAeMaliGallium(String graphicsDriver) {
        return AEMALI_GALLIUM.equals(normalize(graphicsDriver));
    }

    public static boolean isMesaOpenGlBridge(String graphicsDriver) {
        String normalized = normalize(graphicsDriver);
        return ZINK.equals(normalized) || AEMALI_GALLIUM.equals(normalized);
    }

    public static String getMesaGalliumDriver(String graphicsDriver) {
        String normalized = normalize(graphicsDriver);
        if (ZINK.equals(normalized)) return "zink";
        if (AEMALI_GALLIUM.equals(normalized)) return "panfrost";
        return "";
    }

    public static String getVirglGalliumDriver() {
        return "virpipe";
    }

    public static String getWrapperGalliumDriver(String rawDriver) {
        String normalized = StringUtils.parseIdentifier(rawDriver == null ? "" : rawDriver);
        if ("freedreno".equals(normalized)) return "freedreno";
        return "zink";
    }

    public static boolean isWrapperZinkOpenGlDriver(String rawDriver) {
        return "zink".equals(getWrapperGalliumDriver(rawDriver));
    }

    public static String normalizeWrapperGlVersion(String requestedVersion, String rawGalliumDriver) {
        if (!isWrapperZinkOpenGlDriver(rawGalliumDriver)) return "";
        String normalized = StringUtils.parseNumber(requestedVersion, DEFAULT_WRAPPER_ZINK_GL_VERSION);
        return normalized.isEmpty() ? DEFAULT_WRAPPER_ZINK_GL_VERSION : normalized;
    }

    public static String normalizeGraphicsExtensionList(String rawExtensions) {
        if (rawExtensions == null || rawExtensions.trim().isEmpty()) return "";
        LinkedHashSet<String> extensions = new LinkedHashSet<>();
        Matcher matcher = GRAPHICS_EXTENSION_TOKEN_PATTERN.matcher(rawExtensions);
        while (matcher.find()) {
            String token = matcher.group();
            if (token != null && !token.trim().isEmpty()) {
                extensions.add(token.trim());
            }
        }
        if (extensions.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (String extension : extensions) {
            if (builder.length() > 0) builder.append('|');
            builder.append(extension);
        }
        return builder.toString();
    }

    public static String[] splitGraphicsExtensionList(String rawExtensions) {
        String normalized = normalizeGraphicsExtensionList(rawExtensions);
        return normalized.isEmpty() ? new String[0] : normalized.split("\\|");
    }

    public static String buildMesaExtensionOverride(boolean disableGLKHRDebug,
                                                    boolean disableVertexArrayBGRA,
                                                    String extraDisabledExtensions) {
        LinkedHashSet<String> disabledExtensions = new LinkedHashSet<>();
        if (disableGLKHRDebug) disabledExtensions.add("GL_KHR_debug");
        if (disableVertexArrayBGRA) disabledExtensions.add("GL_EXT_vertex_array_bgra");
        disabledExtensions.addAll(Arrays.asList(splitGraphicsExtensionList(extraDisabledExtensions)));
        StringBuilder mesaExtensionOverride = new StringBuilder();
        for (String disabledExtension : disabledExtensions) {
            if (disabledExtension == null || disabledExtension.trim().isEmpty()) continue;
            if (mesaExtensionOverride.length() > 0) mesaExtensionOverride.append(' ');
            mesaExtensionOverride.append('-').append(disabledExtension.trim());
        }
        return mesaExtensionOverride.toString();
    }

    public static boolean isMediaTekConfigRestartRequired(String oldGraphicsDriverConfig, String newGraphicsDriverConfig) {
        if (oldGraphicsDriverConfig == null || newGraphicsDriverConfig == null) return false;
        if (oldGraphicsDriverConfig.equals(newGraphicsDriverConfig)) return false;

        KeyValueSet oldConfig = new KeyValueSet(migrateToUnifiedTopLevelConfig(VORTEK, oldGraphicsDriverConfig));
        KeyValueSet newConfig = new KeyValueSet(migrateToUnifiedTopLevelConfig(VORTEK, newGraphicsDriverConfig));
        return !oldConfig.get("vulkanDriverEntry", VortekVulkanDriverPackageManager.SYSTEM_ENTRY)
                        .equals(newConfig.get("vulkanDriverEntry", VortekVulkanDriverPackageManager.SYSTEM_ENTRY))
                || !oldConfig.get("vortekPackageVersion").equals(newConfig.get("vortekPackageVersion"))
                || !oldConfig.get("gladioPackageVersion").equals(newConfig.get("gladioPackageVersion"))
                || !normalizeMediaTekRoutingMode(oldConfig.get("routingMode", ROUTING_AUTO))
                        .equals(normalizeMediaTekRoutingMode(newConfig.get("routingMode", ROUTING_AUTO)))
                || !oldConfig.get("galliumDriver", DEFAULT_MALI_GALLIUM_DRIVER)
                        .equals(newConfig.get("galliumDriver", DEFAULT_MALI_GALLIUM_DRIVER))
                || !oldConfig.get("glVersion", DEFAULT_MALI_GL_VERSION)
                        .equals(newConfig.get("glVersion", DEFAULT_MALI_GL_VERSION))
                || oldConfig.getBoolean("disableVertexArrayBGRA", true) != newConfig.getBoolean("disableVertexArrayBGRA", true)
                || oldConfig.getBoolean("disableGLKHRDebug", true) != newConfig.getBoolean("disableGLKHRDebug", true)
                || !normalizeGraphicsExtensionSet(oldConfig.get("extraDisabledExtensions", ""))
                        .equals(normalizeGraphicsExtensionSet(newConfig.get("extraDisabledExtensions", "")))
                || !VortekExtensionPolicy.normalizeProfile(oldConfig.get("extensionProfile", VortekExtensionPolicy.PROFILE_MALI_SYSTEM))
                        .equals(VortekExtensionPolicy.normalizeProfile(newConfig.get("extensionProfile", VortekExtensionPolicy.PROFILE_MALI_SYSTEM)))
                || !normalizeExtensionSet(oldConfig.get("disabledDeviceExtensions", ""))
                        .equals(normalizeExtensionSet(newConfig.get("disabledDeviceExtensions", "")))
                || !normalizeExtensionSet(oldConfig.get("exposedDeviceExtensions", ""))
                        .equals(normalizeExtensionSet(newConfig.get("exposedDeviceExtensions", "")))
                || !oldConfig.get("vkMaxVersion", DEFAULT_VORTEK_VK_MAX_VERSION)
                        .equals(newConfig.get("vkMaxVersion", DEFAULT_VORTEK_VK_MAX_VERSION))
                || !oldConfig.get("maxDeviceMemory", "0")
                        .equals(newConfig.get("maxDeviceMemory", "0"))
                || !oldConfig.get("imageCacheSize", DEFAULT_VORTEK_IMAGE_CACHE_SIZE)
                        .equals(newConfig.get("imageCacheSize", DEFAULT_VORTEK_IMAGE_CACHE_SIZE))
                || oldConfig.getInt("resourceMemoryType") != newConfig.getInt("resourceMemoryType")
                || oldConfig.getBoolean("gladioNoError", true) != newConfig.getBoolean("gladioNoError", true);
    }

    public static boolean usesKeyValueConfig(String graphicsDriver) {
        String normalized = normalize(graphicsDriver);
        return VORTEK.equals(normalized) || isMesaOpenGlBridge(normalized) || VIRGL.equals(normalized) || GLADIO.equals(normalized);
    }

    public static ArrayList<String> getSelectableEntries(Context context) {
        List<String> assetFileNames = listBundledGraphicsDriverAssetFileNames(context);
        if (!assetFileNames.isEmpty()) {
            return buildSelectableEntriesFromFileNames(assetFileNames);
        }

        ArrayList<String> entries = new ArrayList<>();
        entries.add(getDisplayLabel(WRAPPER));
        for (String driverId : BUILTIN_DRIVER_ORDER) {
            entries.add(getDisplayLabel(driverId));
        }
        return entries;
    }

    private static String normalizeMediaTekRoutingMode(String routingMode) {
        String normalized = StringUtils.parseIdentifier(routingMode == null ? "" : routingMode);
        if (ROUTING_VULKAN_FIRST.equals(normalized) || ROUTING_OPENGL_FIRST.equals(normalized)) return normalized;
        return ROUTING_AUTO;
    }

    private static String normalizeGraphicsExtensionSet(String rawExtensions) {
        TreeSet<String> extensions = new TreeSet<>();
        extensions.addAll(Arrays.asList(splitGraphicsExtensionList(rawExtensions)));
        return String.join("|", extensions);
    }

    private static String normalizeExtensionSet(String rawExtensions) {
        if (rawExtensions == null || rawExtensions.trim().isEmpty()) return "";
        TreeSet<String> extensions = new TreeSet<>();
        for (String extension : rawExtensions.split("\\|")) {
            if (extension == null) continue;
            String normalized = extension.trim();
            if (!normalized.isEmpty()) extensions.add(normalized);
        }
        return String.join("|", extensions);
    }

    public static String getTopLevelSelectableDriver(String graphicsDriver) {
        String normalized = normalize(graphicsDriver);
        if (normalized.isEmpty()) return WRAPPER;
        if (ZINK.equals(normalized)) return WRAPPER;
        if (GLADIO.equals(normalized) || AEMALI_GALLIUM.equals(normalized) || AEMALI_PANVK.equals(normalized)) {
            return VORTEK;
        }
        return normalized;
    }

    public static String migrateToUnifiedTopLevelConfig(String graphicsDriver, String rawConfig) {
        String normalized = normalize(graphicsDriver);
        String trimmed = rawConfig == null ? "" : rawConfig.trim();
        if (ZINK.equals(normalized)) {
            return migrateZinkTopLevelConfig(trimmed);
        }
        if (!isUnifiedVortekFamily(normalized)) {
            return trimmed;
        }

        KeyValueSet migrated = new KeyValueSet(trimmed);
        String gladioPackageEntry = trimConfigValue(migrated.get("gladioPackageVersion"));
        String legacyPackageEntry = trimConfigValue(migrated.get("packageVersion"));

        if (gladioPackageEntry.isEmpty()) {
            if (!legacyPackageEntry.isEmpty() || AEMALI_GALLIUM.equals(normalized)) {
                migrated.put("gladioPackageVersion", toUnifiedAeMaliOpenGlEntry(legacyPackageEntry));
            } else if (GLADIO.equals(normalized)) {
                migrated.put("gladioPackageVersion", getBuiltinTemplate(GLADIO).fallbackVersion);
            }
        }

        if (trimConfigValue(migrated.get("routingMode")).isEmpty() && (GLADIO.equals(normalized) || AEMALI_GALLIUM.equals(normalized))) {
            migrated.put("routingMode", ROUTING_OPENGL_FIRST);
        }

        if (AEMALI_GALLIUM.equals(normalized) || GladioOpenGLDriverPackageManager.isAeMaliPackageEntry(migrated.get("gladioPackageVersion"))) {
            migrated.put(
                    "galliumDriver",
                    normalizeMaliGalliumDriverValue(migrated.get("galliumDriver"), DEFAULT_MALI_GALLIUM_DRIVER)
            );
            migrated.put("glVersion", normalizeGlVersionValue(migrated.get("glVersion"), DEFAULT_MALI_GL_VERSION));
            migrated.put("disableVertexArrayBGRA", migrated.getBoolean("disableVertexArrayBGRA", true) ? "1" : "0");
            migrated.put("disableGLKHRDebug", migrated.getBoolean("disableGLKHRDebug", true) ? "1" : "0");
        }

        return migrated.toString();
    }

    public static String defaultConfig(String graphicsDriver) {
        return usesKeyValueConfig(graphicsDriver) ? "" : Container.DEFAULT_GRAPHICSDRIVERCONFIG;
    }

    public static String sanitizeConfigShape(String graphicsDriver, String rawConfig) {
        String trimmed = rawConfig == null ? "" : rawConfig.trim();
        if (trimmed.isEmpty()) return defaultConfig(graphicsDriver);

        boolean keyValueDriver = usesKeyValueConfig(graphicsDriver);
        boolean looksLegacyConfig = trimmed.contains(";") || trimmed.contains("legacyRequestedDriver=");
        boolean looksKeyValueConfig = trimmed.contains(",")
                || trimmed.contains("adrenotoolsDriver=")
                || trimmed.contains("glVersion=")
                || trimmed.contains("disableGLKHRDebug=")
                || trimmed.contains("gladioNoError=");

        if (keyValueDriver) {
            return looksLegacyConfig && !looksKeyValueConfig ? "" : trimmed;
        }
        return looksLegacyConfig ? trimmed : Container.DEFAULT_GRAPHICSDRIVERCONFIG;
    }

    public static HashMap<String, String> parseConfig(String graphicsDriver, String rawConfig) {
        HashMap<String, String> mapped = new HashMap<>();
        if (rawConfig == null || rawConfig.trim().isEmpty()) {
            return mapped;
        }

        if (usesKeyValueConfig(graphicsDriver)) {
            KeyValueSet keyValueSet = new KeyValueSet(rawConfig);
            for (String[] entry : keyValueSet) {
                if (entry == null || entry.length < 2) continue;
                String key = entry[0] == null ? "" : entry[0].trim();
                if (key.isEmpty()) continue;
                mapped.put(key, entry[1] == null ? "" : entry[1].trim());
            }
            return mapped;
        }

        mapped.putAll(GraphicsDriverConfigDialog.parseGraphicsDriverConfig(rawConfig));
        return mapped;
    }

    public static KeyValueSet toKeyValueSetConfig(String graphicsDriver, String rawConfig) {
        if (rawConfig == null || rawConfig.trim().isEmpty()) {
            return new KeyValueSet();
        }
        if (usesKeyValueConfig(graphicsDriver)) {
            return new KeyValueSet(rawConfig);
        }

        HashMap<String, String> mapped = GraphicsDriverConfigDialog.parseGraphicsDriverConfig(rawConfig);
        KeyValueSet config = new KeyValueSet();
        for (String key : mapped.keySet()) {
            config.put(key, mapped.get(key));
        }
        return config;
    }

    public static String getDisplayVersion(String graphicsDriver, String graphicsDriverConfig) {
        return getDisplayVersion(null, graphicsDriver, graphicsDriverConfig);
    }

    public static String getDisplayVersion(Context context, String graphicsDriver, String graphicsDriverConfig) {
        String normalized = normalize(graphicsDriver);
        switch (normalized) {
            case VORTEK:
                return resolveVortekDisplayVersion(context, graphicsDriverConfig);
            case VIRGL:
                return resolveVirglDisplayVersion(context, graphicsDriverConfig);
            case GLADIO:
                return resolveGladioDisplayVersion(context, graphicsDriverConfig);
            case AEMALI_GALLIUM:
                return resolveAeMaliGalliumDisplayVersion(context, graphicsDriverConfig);
            case ZINK:
                return getBundledDriverAsset(context, normalized).version;
            case WRAPPER:
                String version = parseConfig(graphicsDriver, graphicsDriverConfig).get("version");
                return version == null || version.trim().isEmpty() ? DefaultVersion.WRAPPER : version.trim();
            default:
                return normalized.toUpperCase(Locale.US);
        }
    }

    private static String resolveVortekDisplayVersion(Context context, String graphicsDriverConfig) {
        if (context != null) {
            KeyValueSet config = toKeyValueSetConfig(VORTEK, graphicsDriverConfig);
            String driverEntry = config.get("vulkanDriverEntry", VortekVulkanDriverPackageManager.SYSTEM_ENTRY);
            if (VortekVulkanDriverPackageManager.isCustomPackageEntry(driverEntry)
                    || VortekVulkanDriverPackageManager.isBundledAeMaliPackageEntry(driverEntry)) {
                VortekVulkanDriverPackageManager.PackageInfo info =
                        new VortekVulkanDriverPackageManager(context).getPackageInfo(driverEntry);
                if (info != null && info.version != null && !info.version.trim().isEmpty()) {
                    return info.version.trim();
                }
                String entryId = VortekVulkanDriverPackageManager.toEntryId(driverEntry);
                if (!entryId.isEmpty()) return entryId;
            }

            String bundledVersion = config.get("vortekPackageVersion", "");
            if (!bundledVersion.trim().isEmpty()) {
                return getBundledDriverAsset(context, VORTEK, bundledVersion).version;
            }
        }
        return getBundledDriverAsset(context, VORTEK).version;
    }

    private static String resolveVirglDisplayVersion(Context context, String graphicsDriverConfig) {
        if (context != null) {
            KeyValueSet config = toKeyValueSetConfig(VIRGL, graphicsDriverConfig);
            String packageEntry = config.get("packageVersion", "");
            if (VirGLDriverPackageManager.isCustomPackageEntry(packageEntry)) {
                VirGLDriverPackageManager.PackageInfo info =
                        new VirGLDriverPackageManager(context).getPackageInfo(packageEntry);
                if (info != null && info.version != null && !info.version.trim().isEmpty()) {
                    return info.version.trim();
                }
                String entryId = VirGLDriverPackageManager.toEntryId(packageEntry);
                if (!entryId.isEmpty()) return entryId;
            }

            if (!packageEntry.trim().isEmpty()) {
                return getBundledDriverAsset(context, VIRGL, packageEntry).version;
            }
        }
        return getBundledDriverAsset(context, VIRGL).version;
    }

    private static String resolveGladioDisplayVersion(Context context, String graphicsDriverConfig) {
        if (context != null) {
            KeyValueSet config = toKeyValueSetConfig(GLADIO, graphicsDriverConfig);
            String packageEntry = config.get("gladioPackageVersion", "");
            if (GladioOpenGLDriverPackageManager.requiresManagedPackageLookup(packageEntry)) {
                GladioOpenGLDriverPackageManager.PackageInfo info =
                        new GladioOpenGLDriverPackageManager(context).getPackageInfo(packageEntry);
                if (info != null && info.version != null && !info.version.trim().isEmpty()) {
                    return info.version.trim();
                }
                String entryId = GladioOpenGLDriverPackageManager.toEntryId(packageEntry);
                if (!entryId.isEmpty()) return entryId;
            }

            if (!packageEntry.trim().isEmpty()) {
                return getBundledDriverAsset(context, GLADIO, packageEntry).version;
            }
        }
        return getBundledDriverAsset(context, GLADIO).version;
    }

    private static String resolveAeMaliGalliumDisplayVersion(Context context, String graphicsDriverConfig) {
        if (context != null) {
            KeyValueSet config = toKeyValueSetConfig(AEMALI_GALLIUM, graphicsDriverConfig);
            String packageEntry = config.get("packageVersion", "");
            if (MesaOpenGLDriverPackageManager.isCustomPackageEntry(packageEntry)) {
                MesaOpenGLDriverPackageManager.PackageInfo info =
                        new MesaOpenGLDriverPackageManager(context, AEMALI_GALLIUM).getPackageInfo(packageEntry);
                if (info != null && info.version != null && !info.version.trim().isEmpty()) {
                    return info.version.trim();
                }
                String entryId = MesaOpenGLDriverPackageManager.toEntryId(packageEntry);
                if (!entryId.isEmpty()) return entryId;
            }
            if (!packageEntry.trim().isEmpty()) {
                return getBundledDriverAsset(context, AEMALI_GALLIUM, packageEntry).version;
            }
        }
        return getBundledDriverAsset(context, AEMALI_GALLIUM).version;
    }

    public static String getDisplayLabel(String graphicsDriver) {
        String normalized = normalize(graphicsDriver);
        if (WRAPPER.equals(normalized)) return "Wrapper";
        BuiltinDriverTemplate template = getBuiltinTemplate(normalized);
        return template == null ? normalized.toUpperCase(Locale.US) : template.packageLabel;
    }

    public static BundledDriverAsset getBundledDriverAsset(Context context, String graphicsDriver) {
        BundledDriverAsset resolved = context == null ? null : resolveBundledDriverAsset(context, graphicsDriver);
        return resolved != null ? resolved : fallbackBundledDriverAsset(graphicsDriver);
    }

    public static BundledDriverAsset getBundledDriverAsset(Context context, String graphicsDriver, String requestedVersion) {
        BundledDriverAsset resolved = context == null ? null : resolveBundledDriverAsset(context, graphicsDriver, requestedVersion);
        if (resolved != null) return resolved;
        return getBundledDriverAsset(context, graphicsDriver);
    }

    public static BundledDriverAsset resolveBundledDriverAsset(Context context, String graphicsDriver) {
        return resolveBundledDriverAssetFromFileNames(graphicsDriver, listBundledGraphicsDriverAssetFileNames(context));
    }

    public static BundledDriverAsset resolveBundledDriverAsset(Context context, String graphicsDriver, String requestedVersion) {
        return resolveBundledDriverAssetFromFileNames(graphicsDriver, requestedVersion, listBundledGraphicsDriverAssetFileNames(context));
    }

    static BundledDriverAsset resolveBundledDriverAssetFromFileNames(String graphicsDriver, Iterable<String> assetFileNames) {
        String normalized = normalize(graphicsDriver);
        BuiltinDriverTemplate template = getBuiltinTemplate(normalized);
        if (template == null) {
            return null;
        }

        BundledDriverAsset best = null;
        if (assetFileNames == null) {
            return null;
        }

        for (String assetFileName : assetFileNames) {
            BundledDriverAsset candidate = parseBundledDriverAssetFileName(template, assetFileName);
            if (candidate == null) {
                continue;
            }
            if (best == null || compareBundledVersions(candidate.version, best.version) > 0) {
                best = candidate;
            }
        }
        return best;
    }

    static BundledDriverAsset resolveBundledDriverAssetFromFileNames(String graphicsDriver, String requestedVersion, Iterable<String> assetFileNames) {
        String normalizedVersion = requestedVersion == null ? "" : requestedVersion.trim();
        if (normalizedVersion.isEmpty()) return resolveBundledDriverAssetFromFileNames(graphicsDriver, assetFileNames);
        String normalized = normalize(graphicsDriver);
        BuiltinDriverTemplate template = getBuiltinTemplate(normalized);
        if (template == null || assetFileNames == null) return null;

        BundledDriverAsset best = null;
        for (String assetFileName : assetFileNames) {
            BundledDriverAsset candidate = parseBundledDriverAssetFileName(template, assetFileName);
            if (candidate != null && candidate.version.equalsIgnoreCase(normalizedVersion)) return candidate;
            if (candidate != null && (best == null || compareBundledVersions(candidate.version, best.version) > 0)) {
                best = candidate;
            }
        }
        if (best != null && template.fallbackVersion.equalsIgnoreCase(normalizedVersion)) return best;
        return null;
    }

    public static ArrayList<String> getBundledDriverVersions(Context context, String graphicsDriver) {
        ArrayList<String> versions = new ArrayList<>();
        String normalized = normalize(graphicsDriver);
        BuiltinDriverTemplate template = getBuiltinTemplate(normalized);
        if (template == null) return versions;
        for (String assetFileName : listBundledGraphicsDriverAssetFileNames(context)) {
            BundledDriverAsset candidate = parseBundledDriverAssetFileName(template, assetFileName);
            if (candidate == null || versions.contains(candidate.version)) continue;
            versions.add(candidate.version);
        }
        versions.sort(GraphicsDrivers::compareBundledVersionsDescending);
        if (versions.isEmpty()) versions.add(template.fallbackVersion);
        return versions;
    }

    private static int compareBundledVersionsDescending(String left, String right) {
        return -compareBundledVersions(left, right);
    }

    static ArrayList<String> buildSelectableEntriesFromFileNames(Iterable<String> assetFileNames) {
        ArrayList<String> entries = new ArrayList<>();
        entries.add(getDisplayLabel(WRAPPER));
        for (String driverId : BUILTIN_DRIVER_ORDER) {
            if (VORTEK.equals(driverId)) {
                if (resolveBundledDriverAssetFromFileNames(VORTEK, assetFileNames) != null
                        || resolveBundledDriverAssetFromFileNames(GLADIO, assetFileNames) != null
                        || resolveBundledDriverAssetFromFileNames(AEMALI_GALLIUM, assetFileNames) != null) {
                    entries.add(getDisplayLabel(driverId));
                }
                continue;
            }
            if (resolveBundledDriverAssetFromFileNames(driverId, assetFileNames) != null) {
                entries.add(getDisplayLabel(driverId));
            }
        }
        return entries;
    }

    private static String migrateZinkTopLevelConfig(String rawConfig) {
        HashMap<String, String> config = GraphicsDriverConfigDialog.parseGraphicsDriverConfig(Container.DEFAULT_GRAPHICSDRIVERCONFIG);
        String trimmed = rawConfig == null ? "" : rawConfig.trim();
        if (trimmed.contains(";")) {
            config.putAll(GraphicsDriverConfigDialog.parseGraphicsDriverConfig(trimmed));
        } else if (!trimmed.isEmpty()) {
            KeyValueSet legacyConfig = new KeyValueSet(trimmed);
            config.put("galliumDriver", getWrapperGalliumDriver(legacyConfig.get("galliumDriver", ZINK)));
            config.put("glVersion", normalizeWrapperGlVersion(legacyConfig.get("glVersion", DEFAULT_WRAPPER_ZINK_GL_VERSION), ZINK));
            config.put("disableVertexArrayBGRA", legacyConfig.getBoolean("disableVertexArrayBGRA", true) ? "1" : "0");
            config.put("disableGLKHRDebug", legacyConfig.getBoolean("disableGLKHRDebug", true) ? "1" : "0");
            return GraphicsDriverConfigDialog.toGraphicsDriverConfig(config);
        }

        String wrapperGalliumDriver = getWrapperGalliumDriver(config.get("galliumDriver"));
        config.put("galliumDriver", wrapperGalliumDriver);
        String glVersion = normalizeWrapperGlVersion(config.get("glVersion"), wrapperGalliumDriver);
        if (!glVersion.isEmpty()) config.put("glVersion", glVersion);
        config.put("disableVertexArrayBGRA", parseBooleanConfigValue(config.get("disableVertexArrayBGRA"), true) ? "1" : "0");
        config.put("disableGLKHRDebug", parseBooleanConfigValue(config.get("disableGLKHRDebug"), true) ? "1" : "0");
        return GraphicsDriverConfigDialog.toGraphicsDriverConfig(config);
    }

    private static String toUnifiedAeMaliOpenGlEntry(String packageEntry) {
        String normalized = trimConfigValue(packageEntry);
        if (MesaOpenGLDriverPackageManager.isCustomPackageEntry(normalized)) {
            return GladioOpenGLDriverPackageManager.toAeMaliCustomEntry(MesaOpenGLDriverPackageManager.toEntryId(normalized));
        }
        String version = normalized.isEmpty() ? getBuiltinTemplate(AEMALI_GALLIUM).fallbackVersion : normalized;
        return GladioOpenGLDriverPackageManager.toBundledAeMaliEntry(version);
    }

    private static String normalizeMaliGalliumDriverValue(String requestedDriver, String fallbackDriver) {
        String normalized = StringUtils.parseIdentifier(requestedDriver == null ? "" : requestedDriver);
        if ("panfrost".equals(normalized) || "lima".equals(normalized) || "zink".equals(normalized) || "softpipe".equals(normalized)) {
            return normalized;
        }
        String fallback = StringUtils.parseIdentifier(fallbackDriver == null ? "" : fallbackDriver);
        return fallback.isEmpty() ? DEFAULT_MALI_GALLIUM_DRIVER : fallback;
    }

    private static String normalizeGlVersionValue(String requestedVersion, String fallbackVersion) {
        String normalized = StringUtils.parseNumber(requestedVersion, fallbackVersion);
        return normalized.isEmpty() ? fallbackVersion : normalized;
    }

    private static String trimConfigValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean parseBooleanConfigValue(String value, boolean fallback) {
        String normalized = trimConfigValue(value).toLowerCase(Locale.US);
        if (normalized.isEmpty()) return fallback;
        if ("1".equals(normalized) || "true".equals(normalized) || "t".equals(normalized)) return true;
        if ("0".equals(normalized) || "false".equals(normalized) || "f".equals(normalized)) return false;
        return fallback;
    }

    static int compareBundledVersions(String left, String right) {
        List<String> leftTokens = tokenizeVersion(left);
        List<String> rightTokens = tokenizeVersion(right);
        int count = Math.max(leftTokens.size(), rightTokens.size());
        for (int i = 0; i < count; i++) {
            String leftToken = i < leftTokens.size() ? leftTokens.get(i) : "";
            String rightToken = i < rightTokens.size() ? rightTokens.get(i) : "";
            boolean leftNumeric = isNumericToken(leftToken);
            boolean rightNumeric = isNumericToken(rightToken);
            if (leftNumeric && rightNumeric) {
                int cmp = new BigInteger(leftToken).compareTo(new BigInteger(rightToken));
                if (cmp != 0) return cmp;
                continue;
            }
            if (leftNumeric != rightNumeric) {
                return leftNumeric ? 1 : -1;
            }
            int cmp = leftToken.compareToIgnoreCase(rightToken);
            if (cmp != 0) return cmp;
        }
        return left.compareToIgnoreCase(right);
    }

    private static BundledDriverAsset parseBundledDriverAssetFileName(BuiltinDriverTemplate template, String assetFileName) {
        if (assetFileName == null) return null;
        String trimmed = assetFileName.trim();
        String prefix = template.driverId + "-";
        if (!trimmed.startsWith(prefix) || !trimmed.endsWith(GRAPHICS_DRIVER_ARCHIVE_SUFFIX)) {
            return null;
        }

        String version = trimmed.substring(prefix.length(), trimmed.length() - GRAPHICS_DRIVER_ARCHIVE_SUFFIX.length()).trim();
        if (version.isEmpty()) return null;

        return new BundledDriverAsset(
                template.driverId,
                template.packageLabel,
                version,
                trimmed,
                GRAPHICS_DRIVER_ASSET_DIR + "/" + trimmed,
                resolveBundledExtractProbePath(template, trimmed)
        );
    }

    private static BundledDriverAsset fallbackBundledDriverAsset(String graphicsDriver) {
        BuiltinDriverTemplate template = getBuiltinTemplate(graphicsDriver);
        if (template == null) {
            return null;
        }
        String assetFileName = template.driverId + "-" + template.fallbackVersion + GRAPHICS_DRIVER_ARCHIVE_SUFFIX;
        return new BundledDriverAsset(
                template.driverId,
                template.packageLabel,
                template.fallbackVersion,
                assetFileName,
                GRAPHICS_DRIVER_ASSET_DIR + "/" + assetFileName,
                resolveBundledExtractProbePath(template, assetFileName)
        );
    }

    private static String resolveBundledExtractProbePath(BuiltinDriverTemplate template, String assetFileName) {
        if (template != null && VORTEK.equals(template.driverId)) {
            return "usr/share/aesolator/graphics_driver/" + assetFileName + ".installed";
        }
        return template == null ? "" : template.extractProbePath;
    }

    private static BuiltinDriverTemplate getBuiltinTemplate(String graphicsDriver) {
        String normalized = normalize(graphicsDriver);
        switch (normalized) {
            case VORTEK:
                return new BuiltinDriverTemplate(VORTEK, "Vortek", "2.1.3", "usr/lib/libvulkan_vortek.so");
            case ZINK:
                return new BuiltinDriverTemplate(ZINK, "Zink", "26.1.0", "usr/lib/libGL.so.1");
            case VIRGL:
                return new BuiltinDriverTemplate(VIRGL, "VirGL", "26.1.0", "usr/lib/libGL.so.1");
            case GLADIO:
                return new BuiltinDriverTemplate(GLADIO, "Gladio", "1.0", "usr/lib/libGL.so.1.7.0");
            case AEMALI_PANVK:
                return new BuiltinDriverTemplate(AEMALI_PANVK, "AeMali PanVK", "26.1.0", "usr/lib/libvulkan_panfrost.so");
            case AEMALI_GALLIUM:
                return new BuiltinDriverTemplate(AEMALI_GALLIUM, "AeMali Gallium", "26.1.0", "usr/lib/libEGL.so");
            default:
                return null;
        }
    }

    private static List<String> listBundledGraphicsDriverAssetFileNames(Context context) {
        if (context == null) return Collections.emptyList();
        try {
            String[] assetEntries = context.getAssets().list(GRAPHICS_DRIVER_ASSET_DIR);
            if (assetEntries == null || assetEntries.length == 0) {
                return Collections.emptyList();
            }
            ArrayList<String> fileNames = new ArrayList<>(Arrays.asList(assetEntries));
            Collections.sort(fileNames);
            return fileNames;
        } catch (IOException ignored) {
            return Collections.emptyList();
        }
    }

    private static List<String> tokenizeVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<String> tokens = new ArrayList<>();
        Matcher matcher = VERSION_TOKEN_PATTERN.matcher(version);
        while (matcher.find()) {
            tokens.add(matcher.group(1));
        }
        if (!tokens.isEmpty()) {
            return tokens;
        }

        return Collections.singletonList(version);
    }

    private static boolean isNumericToken(String token) {
        return token != null && !token.isEmpty() && Character.isDigit(token.charAt(0));
    }
}
