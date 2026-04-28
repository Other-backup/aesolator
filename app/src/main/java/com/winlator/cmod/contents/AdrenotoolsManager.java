package com.winlator.cmod.contents;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.winlator.cmod.SettingsFragment;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contentdialog.GraphicsDriverConfigDialog;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.GPUInformation;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.xenvironment.ImageFs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdrenotoolsManager {
    private static final String TAG = "AdrenotoolsManager";
    private static final String OVERLAY_BACKUP_DIR = "_overlay_backup";
    private static final String OVERLAY_MANIFEST = "overlay-manifest.txt";
    private static final Pattern BUNDLED_DRIVER_ASSET_PATTERN =
            Pattern.compile("^adrenotools-(.+)\\.tzst$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMERIC_TOKEN_PATTERN = Pattern.compile("(\\d+)");

    public static final class DriverFileMapping {
        public final String source;
        public final String target;

        public DriverFileMapping(String source, String target) {
            this.source = source == null ? "" : source.trim();
            this.target = target == null ? "" : target.trim();
        }

        public boolean isUsable() {
            return !source.isEmpty() && !target.isEmpty();
        }
    }

    public static final class DriverPackageInfo {
        public final String entryId;
        public String name = "";
        public String description = "";
        public String driverVersion = "";
        public String libraryName = "";
        public String provider = "";
        public String providerLane = "";
        public String driverRoute = "";
        public String graphicsStackProfile = "";
        public String companionProviderLane = "";
        public String preferredGalliumDriver = "";
        public String sourceRepo = "";
        public String sourceType = "";
        public String sourceVersion = "";
        public String artifactName = "";
        public String releaseTag = "";
        public String channel = "";
        public String archiveFormat = "";
        public String archiveLayout = "";
        public String installSurface = "";
        public String libraryType = "";
        public String mesaMainCommit = "";
        public String mesaStableTag = "";
        public boolean fromResources = false;
        public boolean systemSelection = false;
        public long lastModified = 0L;
        public final ArrayList<String> translationLayers = new ArrayList<>();
        public final ArrayList<String> apiFocus = new ArrayList<>();
        public final ArrayList<String> forensicEnvPrefixes = new ArrayList<>();
        public final ArrayList<String> forensicLogPrefixes = new ArrayList<>();
        public final ArrayList<DriverFileMapping> fileMappings = new ArrayList<>();

        public DriverPackageInfo(String entryId) {
            this.entryId = entryId == null ? "" : entryId.trim();
        }

        public boolean isSystemSelection() {
            return systemSelection;
        }

        public boolean isTurnipProvider() {
            String lane = providerLane.toLowerCase(Locale.US);
            String library = libraryName.toLowerCase(Locale.US);
            return lane.contains("turnip") || library.contains("vulkan_freedreno");
        }

        public boolean isOpenGlProvider() {
            String lane = providerLane.toLowerCase(Locale.US);
            String library = libraryName.toLowerCase(Locale.US);
            String type = libraryType.toLowerCase(Locale.US);
            return lane.contains("opengl") || type.contains("opengl") || library.startsWith("libgl");
        }

        public boolean hasOverlayPayload() {
            return !fileMappings.isEmpty();
        }

        public String getArchLabel() {
            String combined = ((entryId == null ? "" : entryId) + " "
                    + name + " "
                    + artifactName + " "
                    + libraryName).toLowerCase(Locale.US);
            if (combined.contains("arm64ec")) return "ARM64EC";
            if (combined.contains("x86_64") || combined.contains("x86-64") || combined.contains("amd64")) return "x86_64";
            if (combined.contains("arm64")) return "ARM64";
            return "generic";
        }

        public String getDisplayProviderLabel() {
            if (isTurnipProvider()) return "Turnip Vulkan";
            if (isOpenGlProvider()) return "Freedreno Gallium";
            if (!providerLane.isEmpty()) return providerLane;
            if (!provider.isEmpty()) return provider;
            return "driver";
        }

        public String getDisplayRouteLabel() {
            if ("vulkan-first".equalsIgnoreCase(driverRoute)) return "Vulkan-first";
            if ("native-gl".equalsIgnoreCase(driverRoute)) return "Native GL";
            if (driverRoute == null || driverRoute.trim().isEmpty()) return "route-auto";
            return driverRoute;
        }

        public String getSourceLabel() {
            if (sourceRepo == null || sourceRepo.trim().isEmpty()) return "local package";
            String lower = sourceRepo.toLowerCase(Locale.US);
            if (lower.contains("stevenmxz")) return "StevenMXZ";
            if (lower.contains("whitebelyash")) return "whitebelyash";
            if (lower.contains("mrpurple")) return "MrPurple";
            if (lower.contains("gamenative")) return "GameNative";
            if (lower.contains("kosoymiki")) return "Ae.solator";
            return sourceRepo.trim();
        }
    }

    private final File adrenotoolsContentDir;
    private final Context mContext;

    public AdrenotoolsManager(Context context) {
        this.mContext = context;
        this.adrenotoolsContentDir = new File(mContext.getFilesDir(), "contents/adrenotools");
        if (!adrenotoolsContentDir.exists()) adrenotoolsContentDir.mkdirs();
    }

    public String getLibraryName(String adrenoToolsDriverId) {
        DriverPackageInfo info = getDriverPackageInfo(adrenoToolsDriverId);
        return info == null ? "" : info.libraryName;
    }

    public String getDriverName(String adrenoToolsDriverId) {
        DriverPackageInfo info = getDriverPackageInfo(adrenoToolsDriverId);
        if (info == null) return "";
        return info.name == null || info.name.trim().isEmpty() ? info.entryId : info.name;
    }

    public String getDriverVersion(String adrenoToolsDriverId) {
        DriverPackageInfo info = getDriverPackageInfo(adrenoToolsDriverId);
        return info == null ? "" : info.driverVersion;
    }

    public String getDriverPath(String adrenotoolsDriverId) {
        return adrenotoolsContentDir.getAbsolutePath() + "/" + adrenotoolsDriverId + "/";
    }

    public DriverPackageInfo getDriverPackageInfo(String adrenoToolsDriverId) {
        if (isSystemDriverId(adrenoToolsDriverId)) {
            DriverPackageInfo info = new DriverPackageInfo("System");
            info.systemSelection = true;
            info.name = "System";
            info.provider = "system";
            info.providerLane = "system-graphics";
            info.driverRoute = "system";
            return info;
        }

        boolean fromResources = isFromResources(adrenoToolsDriverId);
        if (fromResources) extractDriverFromResources(adrenoToolsDriverId);

        File driverPath = new File(adrenotoolsContentDir, adrenoToolsDriverId);
        if (!driverPath.isDirectory()) return null;
        return parseDriverPackageInfo(adrenoToolsDriverId, driverPath, fromResources);
    }

    public ArrayList<DriverPackageInfo> enumerateInstalledDriverPackages() {
        ArrayList<DriverPackageInfo> drivers = new ArrayList<>();
        File[] files = adrenotoolsContentDir.listFiles();
        if (files == null) return drivers;

        for (File file : files) {
            if (file == null || !file.isDirectory()) continue;
            if ("tmp".equals(file.getName()) || OVERLAY_BACKUP_DIR.equals(file.getName())) continue;
            boolean fromResources = isFromResources(file.getName());
            if (fromResources || new File(file, "meta.json").isFile()) {
                DriverPackageInfo info = parseDriverPackageInfo(file.getName(), file, fromResources);
                if (info != null) drivers.add(info);
            }
        }
        return drivers;
    }

    public ArrayList<String> enumerateBundledDriverEntryIds() {
        ArrayList<String> entryIds = new ArrayList<>();
        AssetManager assetManager = mContext.getAssets();
        try {
            String[] assetEntries = assetManager.list("graphics_driver");
            if (assetEntries == null) return entryIds;
            for (String assetEntry : assetEntries) {
                if (assetEntry == null) continue;
                Matcher matcher = BUNDLED_DRIVER_ASSET_PATTERN.matcher(assetEntry.trim());
                if (!matcher.matches()) continue;
                String entryId = matcher.group(1).trim();
                if (entryId.isEmpty() || entryIds.contains(entryId)) continue;
                entryIds.add(entryId);
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to enumerate bundled graphics drivers", e);
        }
        sortWrapperDriverEntryIds(entryIds);
        return entryIds;
    }

    public ArrayList<String> enumerateAvailableWrapperVersionEntries() {
        ArrayList<String> versionEntries = new ArrayList<>();
        versionEntries.add(DefaultVersion.WRAPPER);

        for (String entryId : enumerateBundledDriverEntryIds()) {
            if (!versionEntries.contains(entryId)) versionEntries.add(entryId);
        }

        for (DriverPackageInfo info : enumerateInstalledDriverPackages()) {
            if (info == null || info.fromResources) continue;
            if (!versionEntries.contains(info.entryId)) versionEntries.add(info.entryId);
        }

        return versionEntries;
    }

    public String getPreferredWrapperDriverId() {
        if (!GPUInformation.isAdrenoGPU(mContext)) return DefaultVersion.WRAPPER;

        for (String entryId : enumerateBundledDriverEntryIds()) {
            if (GPUInformation.isDriverSupported(entryId, mContext)) return entryId;
        }

        return DefaultVersion.WRAPPER;
    }

    public void extractBundledDriverResources() {
        for (String entryId : enumerateBundledDriverEntryIds()) {
            extractDriverFromResources(entryId);
        }
    }

    public int countInstalledDriverPackages(ContentProfile.ContentType type) {
        if (type != ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER
                && type != ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER) {
            return 0;
        }

        int count = 0;
        for (DriverPackageInfo info : enumerateInstalledDriverPackages()) {
            if (matchesInstalledDriverProfile(type, info)) count++;
        }
        return count;
    }

    public boolean isInstalledDriverProfile(ContentProfile remoteProfile) {
        if (remoteProfile == null) return false;
        for (DriverPackageInfo info : enumerateInstalledDriverPackages()) {
            if (matchesInstalledDriverProfile(remoteProfile, info)) return true;
        }
        return false;
    }

    public DriverPackageInfo resolvePreferredDriverForLane(String providerLane, DriverPackageInfo requestedInfo) {
        if (providerLane == null || providerLane.trim().isEmpty()) return requestedInfo;
        String normalizedLane = providerLane.trim().toLowerCase(Locale.US);

        if (requestedInfo != null && normalizedLane.equals(requestedInfo.providerLane.toLowerCase(Locale.US))) {
            return requestedInfo;
        }

        DriverPackageInfo best = null;
        int bestScore = Integer.MIN_VALUE;
        for (DriverPackageInfo candidate : enumerateInstalledDriverPackages()) {
            if (candidate == null) continue;
            if (!normalizedLane.equals(candidate.providerLane.toLowerCase(Locale.US))) continue;
            int score = scoreLaneCandidate(candidate, requestedInfo);
            if (best == null || score > bestScore
                    || (score == bestScore && candidate.lastModified > best.lastModified)) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private int scoreLaneCandidate(DriverPackageInfo candidate, DriverPackageInfo requestedInfo) {
        int score = 0;
        if (candidate == null) return score;
        if (requestedInfo == null) return score;
        if (candidate.entryId.equalsIgnoreCase(requestedInfo.entryId)) score += 500;
        if (!requestedInfo.sourceRepo.isEmpty() && requestedInfo.sourceRepo.equalsIgnoreCase(candidate.sourceRepo)) score += 120;
        if (!requestedInfo.channel.isEmpty() && requestedInfo.channel.equalsIgnoreCase(candidate.channel)) score += 30;
        if (!requestedInfo.releaseTag.isEmpty() && requestedInfo.releaseTag.equalsIgnoreCase(candidate.releaseTag)) score += 20;
        if (!requestedInfo.companionProviderLane.isEmpty()
                && requestedInfo.companionProviderLane.equalsIgnoreCase(candidate.providerLane)) {
            score += 12;
        }
        return score;
    }

    private void sortWrapperDriverEntryIds(List<String> entryIds) {
        if (entryIds == null || entryIds.size() < 2) return;
        Collections.sort(entryIds, this::compareWrapperDriverEntryIds);
    }

    private int compareWrapperDriverEntryIds(String left, String right) {
        int leftRank = getWrapperDriverLaneRank(left);
        int rightRank = getWrapperDriverLaneRank(right);
        if (leftRank != rightRank) return Integer.compare(rightRank, leftRank);

        ArrayList<Integer> leftTokens = extractNumericTokens(left);
        ArrayList<Integer> rightTokens = extractNumericTokens(right);
        int tokenCount = Math.max(leftTokens.size(), rightTokens.size());
        for (int i = 0; i < tokenCount; i++) {
            int leftValue = i < leftTokens.size() ? leftTokens.get(i) : -1;
            int rightValue = i < rightTokens.size() ? rightTokens.get(i) : -1;
            if (leftValue != rightValue) return Integer.compare(rightValue, leftValue);
        }

        return right.compareToIgnoreCase(left);
    }

    private int getWrapperDriverLaneRank(String entryId) {
        String normalized = entryId == null ? "" : entryId.trim().toLowerCase(Locale.US);
        if (normalized.startsWith("turnip")) return 3;
        if (normalized.startsWith("v")) return 2;
        return 1;
    }

    private ArrayList<Integer> extractNumericTokens(String entryId) {
        ArrayList<Integer> numericTokens = new ArrayList<>();
        if (entryId == null) return numericTokens;
        Matcher matcher = NUMERIC_TOKEN_PATTERN.matcher(entryId.toLowerCase(Locale.US));
        while (matcher.find()) {
            try {
                numericTokens.add(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return numericTokens;
    }

    private DriverPackageInfo parseDriverPackageInfo(String entryId, File driverPath, boolean fromResources) {
        if (driverPath == null || !driverPath.isDirectory()) return null;

        DriverPackageInfo info = new DriverPackageInfo(entryId);
        info.fromResources = fromResources;
        info.lastModified = driverPath.lastModified();

        File metaFile = new File(driverPath, "meta.json");
        if (metaFile.isFile()) {
            try {
                JSONObject metaJson = new JSONObject(FileUtils.readString(metaFile));
                info.name = optTrim(metaJson, "name");
                info.description = optTrim(metaJson, "description");
                info.driverVersion = optTrim(metaJson, "driverVersion");
                info.libraryName = optTrim(metaJson, "libraryName");
                info.provider = optTrim(metaJson, "provider");
                info.providerLane = optTrim(metaJson, "providerLane");
                info.driverRoute = optTrim(metaJson, "driverRoute");
                info.graphicsStackProfile = optTrim(metaJson, "graphicsStackProfile");
                info.companionProviderLane = optTrim(metaJson, "companionProviderLane");
                info.preferredGalliumDriver = optTrim(metaJson, "preferredGalliumDriver");
                info.sourceRepo = optTrim(metaJson, "sourceRepo");
                info.sourceType = optTrim(metaJson, "sourceType");
                info.sourceVersion = optTrim(metaJson, "sourceVersion");
                info.artifactName = optTrim(metaJson, "artifactName");
                info.releaseTag = optTrim(metaJson, "releaseTag");
                info.channel = optTrim(metaJson, "channel");
                info.archiveFormat = optTrim(metaJson, "archiveFormat");
                info.archiveLayout = optTrim(metaJson, "archiveLayout");
                info.installSurface = optTrim(metaJson, "installSurface");
                info.libraryType = optTrim(metaJson, "libraryType");
                info.mesaMainCommit = optTrim(metaJson, "mesaMainCommit");
                info.mesaStableTag = optTrim(metaJson, "mesaStableTag");
                appendStringArray(metaJson.optJSONArray("translationLayers"), info.translationLayers);
                appendStringArray(metaJson.optJSONArray("apiFocus"), info.apiFocus);
                appendStringArray(metaJson.optJSONArray("forensicEnvPrefixes"), info.forensicEnvPrefixes);
                appendStringArray(metaJson.optJSONArray("forensicLogPrefixes"), info.forensicLogPrefixes);
                appendFileMappings(metaJson.optJSONArray("files"), info.fileMappings);
            } catch (JSONException e) {
                Log.w(TAG, "Failed to parse driver meta for " + entryId, e);
            }
        }

        File contractFile = new File(driverPath, "ae-runtime-contract.json");
        if (contractFile.isFile()) {
            try {
                JSONObject contractJson = new JSONObject(FileUtils.readString(contractFile));
                fillIfEmpty(info.providerLane, value -> info.providerLane = value, optTrim(contractJson, "providerLane"));
                fillIfEmpty(info.driverRoute, value -> info.driverRoute = value, optTrim(contractJson, "driverRoute"));
                fillIfEmpty(info.graphicsStackProfile, value -> info.graphicsStackProfile = value, optTrim(contractJson, "graphicsStackProfile"));
                fillIfEmpty(info.archiveFormat, value -> info.archiveFormat = value, optTrim(contractJson, "archiveFormat"));
                fillIfEmpty(info.archiveLayout, value -> info.archiveLayout = value, optTrim(contractJson, "archiveLayout"));
                fillIfEmpty(info.installSurface, value -> info.installSurface = value, optTrim(contractJson, "installSurface"));
                fillIfEmpty(info.preferredGalliumDriver, value -> info.preferredGalliumDriver = value, optTrim(contractJson, "preferredGalliumDriver"));

                JSONObject routePolicy = contractJson.optJSONObject("providerRoutePolicy");
                if (routePolicy != null && info.companionProviderLane.isEmpty()) {
                    String companion = optTrim(routePolicy, "companion");
                    if (companion.isEmpty()) companion = optTrim(routePolicy, "secondary path");
                    info.companionProviderLane = companion;
                }

                appendStringArray(contractJson.optJSONArray("translationLayers"), info.translationLayers);

                JSONObject compatibility = contractJson.optJSONObject("compatibility");
                if (compatibility != null) {
                    appendStringArray(compatibility.optJSONArray("apiFocus"), info.apiFocus);
                    if (info.companionProviderLane.isEmpty()) {
                        String companion = optTrim(compatibility, "openglFallbackLane");
                        if (companion.isEmpty()) companion = optTrim(compatibility, "vulkanCompanionLane");
                        info.companionProviderLane = companion;
                    }
                }

                JSONObject forensic = contractJson.optJSONObject("forensic");
                if (forensic != null) {
                    appendStringArray(forensic.optJSONArray("requiredEnvPrefixes"), info.forensicEnvPrefixes);
                    appendStringArray(forensic.optJSONArray("logPrefixes"), info.forensicLogPrefixes);
                }
            } catch (JSONException e) {
                Log.w(TAG, "Failed to parse runtime contract for " + entryId, e);
            }
        }

        File sourceFile = new File(driverPath, "aero-source.json");
        if (!sourceFile.isFile()) sourceFile = new File(driverPath, "mesa-source.json");
        if (sourceFile.isFile()) {
            try {
                JSONObject sourceJson = new JSONObject(FileUtils.readString(sourceFile));
                fillIfEmpty(info.mesaMainCommit, value -> info.mesaMainCommit = value, optTrim(sourceJson, "resolvedMesaMainCommit"));
                fillIfEmpty(info.mesaStableTag, value -> info.mesaStableTag = value, optTrim(sourceJson, "resolvedMesaStableTag"));
            } catch (JSONException e) {
                Log.w(TAG, "Failed to parse source contract for " + entryId, e);
            }
        }

        File profileFile = new File(driverPath, "profile.json");
        if (profileFile.isFile()) {
            try {
                JSONObject profileJson = new JSONObject(FileUtils.readString(profileFile));
                fillIfEmpty(info.sourceRepo, value -> info.sourceRepo = value, optTrim(profileJson, "sourceRepo"));
                fillIfEmpty(info.releaseTag, value -> info.releaseTag = value, optTrim(profileJson, "releaseTag"));
                fillIfEmpty(info.artifactName, value -> info.artifactName = value, optTrim(profileJson, "artifactName"));
                fillIfEmpty(info.channel, value -> info.channel = value, optTrim(profileJson, "channel"));
                appendFileMappings(profileJson.optJSONArray("files"), info.fileMappings);
            } catch (JSONException e) {
                Log.w(TAG, "Failed to parse legacy profile for " + entryId, e);
            }
        }

        if (info.name.isEmpty()) info.name = entryId;
        if (info.providerLane.isEmpty()) {
            if (info.isOpenGlProvider()) info.providerLane = "freedreno-opengl";
            else if (info.isTurnipProvider()) info.providerLane = "turnip-vulkan";
        }
        if (info.graphicsStackProfile.isEmpty()) info.graphicsStackProfile = "vulkan-first-with-gl-secondary path";
        if (info.archiveFormat.isEmpty()) info.archiveFormat = "adrenotools";
        if (info.installSurface.isEmpty()) info.installSurface = "graphics-center";
        return info;
    }

    private String optTrim(JSONObject object, String key) {
        if (object == null || key == null) return "";
        return object.optString(key, "").trim();
    }

    private interface StringConsumer {
        void accept(String value);
    }

    private void fillIfEmpty(String currentValue, StringConsumer consumer, String candidate) {
        if (consumer == null) return;
        if (currentValue != null && !currentValue.trim().isEmpty()) return;
        if (candidate == null || candidate.trim().isEmpty()) return;
        consumer.accept(candidate.trim());
    }

    private void appendStringArray(JSONArray array, ArrayList<String> out) {
        if (array == null || out == null) return;
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "").trim();
            if (value.isEmpty() || out.contains(value)) continue;
            out.add(value);
        }
    }

    private void appendFileMappings(JSONArray array, ArrayList<DriverFileMapping> out) {
        if (array == null || out == null) return;
        for (int i = 0; i < array.length(); i++) {
            JSONObject fileObject = array.optJSONObject(i);
            if (fileObject == null) continue;
            DriverFileMapping mapping = new DriverFileMapping(
                    optTrim(fileObject, ContentProfile.MARK_FILE_SOURCE),
                    optTrim(fileObject, ContentProfile.MARK_FILE_TARGET)
            );
            if (!mapping.isUsable()) continue;
            boolean exists = false;
            for (DriverFileMapping existing : out) {
                if (existing.source.equals(mapping.source) && existing.target.equals(mapping.target)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) out.add(mapping);
        }
    }

    private void reloadContainers(String adrenoToolsDriverId) {
        DriverPackageInfo info = getDriverPackageInfo(adrenoToolsDriverId);
        String driverName = info == null ? "" : info.name;
        String driverEntry = info == null ? adrenoToolsDriverId : info.entryId;
        String preferredWrapperDriver = getPreferredWrapperDriverId();
        ContainerManager containerManager = new ContainerManager(mContext);
        for (Container container : containerManager.getContainers()) {
            HashMap<String, String> config = GraphicsDriverConfigDialog.parseGraphicsDriverConfig(container.getGraphicsDriverConfig());
            String configuredVersion = config.get("version");
            if (matchesDriverReference(configuredVersion, driverEntry, driverName)) {
                config.put("version", preferredWrapperDriver);
                container.setGraphicsDriverConfig(GraphicsDriverConfigDialog.toGraphicsDriverConfig(config));
                container.saveData();
            }
        }
        for (Shortcut shortcut : containerManager.loadShortcuts()) {
            HashMap<String, String> config = GraphicsDriverConfigDialog.parseGraphicsDriverConfig(
                    shortcut.getExtra("graphicsDriverConfig", shortcut.container.getGraphicsDriverConfig())
            );
            String configuredVersion = config.get("version");
            if (matchesDriverReference(configuredVersion, driverEntry, driverName)) {
                config.put("version", preferredWrapperDriver);
                shortcut.putExtra("graphicsDriverConfig", GraphicsDriverConfigDialog.toGraphicsDriverConfig(config));
                shortcut.saveData();
            }
        }
    }

    private boolean matchesDriverReference(String configuredVersion, String driverEntry, String driverName) {
        if (configuredVersion == null || configuredVersion.trim().isEmpty()) return false;
        String normalizedConfigured = normalizeToken(configuredVersion);
        if (!driverEntry.isEmpty() && normalizedConfigured.equals(normalizeToken(driverEntry))) return true;
        if (!driverName.isEmpty() && normalizedConfigured.equals(normalizeToken(driverName))) return true;
        return !driverName.isEmpty() && configuredVersion.contains(driverName);
    }

    private String normalizeToken(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }

    public void removeDriver(String adrenoToolsDriverId) {
        Log.d(TAG, "Removing driver " + adrenoToolsDriverId);
        File driverPath = new File(adrenotoolsContentDir, adrenoToolsDriverId);
        reloadContainers(adrenoToolsDriverId);
        FileUtils.delete(driverPath);
    }

    public ArrayList<String> enumarateInstalledDrivers() {
        ArrayList<String> driversList = new ArrayList<>();
        for (DriverPackageInfo info : enumerateInstalledDriverPackages()) {
            if (info != null && !info.fromResources) driversList.add(info.entryId);
        }
        return driversList;
    }

    public boolean matchesDriverReference(String configuredVersion, DriverPackageInfo info) {
        if (info == null) return false;
        return matchesDriverReference(configuredVersion, info.entryId, info.name);
    }

    public boolean isFromResources(String adrenotoolsDriverId) {
        if (adrenotoolsDriverId == null || adrenotoolsDriverId.trim().isEmpty()) return false;
        String driver = "graphics_driver/adrenotools-" + adrenotoolsDriverId + ".tzst";
        AssetManager am = mContext.getResources().getAssets();
        try (InputStream ignored = am.open(driver)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean extractDriverFromResources(String adrenotoolsDriverId) {
        String src = "graphics_driver/adrenotools-" + adrenotoolsDriverId + ".tzst";
        File dst = new File(adrenotoolsContentDir, adrenotoolsDriverId);
        if (dst.exists()) return true;

        dst.mkdirs();
        Log.d(TAG, "Extracting " + src + " to " + dst.getAbsolutePath());
        boolean hasExtracted = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, mContext, src, dst);
        if (!hasExtracted) dst.delete();
        return hasExtracted;
    }

    private boolean matchesInstalledDriverProfile(ContentProfile remoteProfile, DriverPackageInfo info) {
        return remoteProfile != null && matchesInstalledDriverProfile(remoteProfile.type, remoteProfile, info);
    }

    private boolean matchesInstalledDriverProfile(ContentProfile.ContentType type, DriverPackageInfo info) {
        return matchesInstalledDriverProfile(type, null, info);
    }

    private boolean matchesInstalledDriverProfile(ContentProfile.ContentType type,
                                                 @Nullable ContentProfile remoteProfile,
                                                 DriverPackageInfo info) {
        if (type != ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER
                && type != ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER) {
            return false;
        }
        if (info == null) return false;

        String expectedLane = type == ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER
                ? "freedreno-opengl"
                : "turnip-vulkan";
        if (!info.providerLane.isEmpty() && !expectedLane.equalsIgnoreCase(info.providerLane)) return false;
        if (remoteProfile == null) return true;

        String remoteNameToken = normalizeDriverToken(remoteProfile.verName);
        String remoteTagToken = normalizeDriverToken(remoteProfile.releaseTag);
        String remoteDescToken = normalizeDriverToken(remoteProfile.desc);

        ArrayList<String> localTokens = new ArrayList<>();
        localTokens.add(normalizeDriverToken(info.entryId));
        localTokens.add(normalizeDriverToken(info.name));
        localTokens.add(normalizeDriverToken(stripZipSuffix(info.artifactName)));
        localTokens.add(normalizeDriverToken(info.releaseTag));
        localTokens.add(normalizeDriverToken(info.driverVersion));

        for (String localToken : localTokens) {
            if (localToken.isEmpty()) continue;
            if (!remoteNameToken.isEmpty() && localToken.equals(remoteNameToken)) return true;
            if (!remoteTagToken.isEmpty() && localToken.equals(remoteTagToken)) return true;
        }

        boolean sameSource = info.sourceRepo != null
                && remoteProfile.sourceRepo != null
                && !info.sourceRepo.trim().isEmpty()
                && info.sourceRepo.equalsIgnoreCase(remoteProfile.sourceRepo);

        for (String localToken : localTokens) {
            if (localToken.isEmpty()) continue;
            if (remoteNameToken.length() >= 8 && (localToken.contains(remoteNameToken) || remoteNameToken.contains(localToken))) {
                return true;
            }
            if (sameSource && remoteDescToken.length() >= 10
                    && (localToken.contains(remoteDescToken) || remoteDescToken.contains(localToken))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeDriverToken(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }

    private String stripZipSuffix(String value) {
        if (value == null) return "";
        String out = value.trim();
        if (out.toLowerCase(Locale.US).endsWith(".zip")) {
            out = out.substring(0, out.length() - 4);
        }
        return out;
    }

    public String installDriver(Uri driverUri) {
        return installDriver(driverUri, null);
    }

    public String installDriver(Uri driverUri, ContentProfile remoteHint) {
        File tmpDir = new File(adrenotoolsContentDir, "tmp");
        if (tmpDir.exists()) FileUtils.delete(tmpDir);
        if (!tmpDir.mkdirs()) return "";

        String installEntryId = "";
        try (InputStream is = mContext.getContentResolver().openInputStream(driverUri)) {
            if (is == null) {
                FileUtils.delete(tmpDir);
                return "";
            }
            if (!extractZipSafely(is, tmpDir)) {
                Log.d(TAG, "Failed to install driver, invalid zip payload");
                FileUtils.delete(tmpDir);
                return "";
            }

            File packageRoot = findDriverPackageRoot(tmpDir);
            if (packageRoot == null) {
                Log.d(TAG, "Failed to install driver, meta.json is missing");
                FileUtils.delete(tmpDir);
                return "";
            }

            String driverName = readDriverName(packageRoot);
            if (driverName.isEmpty()) {
                Log.d(TAG, "Failed to install driver, package meta has empty name");
                FileUtils.delete(tmpDir);
                return "";
            }

            installEntryId = resolveInstallEntryId(driverName, remoteHint);
            if (installEntryId.isEmpty()) {
                Log.d(TAG, "Failed to install driver, unable to resolve install entry id");
                FileUtils.delete(tmpDir);
                return "";
            }

            File dst = new File(adrenotoolsContentDir, installEntryId);
            if (dst.exists()) FileUtils.delete(dst);
            if (!FileUtils.copy(packageRoot, dst)) {
                Log.d(TAG, "Failed to install driver, unable to copy payload");
                installEntryId = "";
            }
        } catch (IOException e) {
            Log.d(TAG, "Failed to install driver, invalid payload");
            installEntryId = "";
        }

        FileUtils.delete(tmpDir);
        return installEntryId;
    }

    private String resolveInstallEntryId(String driverName, ContentProfile remoteHint) {
        if (remoteHint != null) {
            String remoteEntry = ContentsManager.getEntryName(remoteHint);
            if (remoteEntry != null && !remoteEntry.trim().isEmpty()) return remoteEntry.trim();
        }
        return driverName == null ? "" : driverName.trim();
    }

    private boolean extractZipSafely(InputStream inputStream, File outputDir) {
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                File dstFile = getSafeZipEntryFile(outputDir, entry);
                if (dstFile == null) return false;
                if (entry.isDirectory()) {
                    if (!dstFile.exists() && !dstFile.mkdirs()) return false;
                } else {
                    File parent = dstFile.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) return false;
                    Files.copy(zis, dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                entry = zis.getNextEntry();
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private File getSafeZipEntryFile(File rootDir, ZipEntry entry) throws IOException {
        return FileUtils.resolveSafeArchiveEntry(rootDir, entry.getName());
    }

    private File findDriverPackageRoot(File rootDir) {
        if (rootDir == null || !rootDir.isDirectory()) return null;
        if (new File(rootDir, "meta.json").isFile()) return rootDir;
        File[] files = rootDir.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (!file.isDirectory()) continue;
            File nested = findDriverPackageRoot(file);
            if (nested != null) return nested;
        }
        return null;
    }

    private String readDriverName(File packageRoot) {
        if (packageRoot == null) return "";
        try {
            File metaProfile = new File(packageRoot, "meta.json");
            JSONObject jsonObject = new JSONObject(FileUtils.readString(metaProfile));
            return jsonObject.optString("name", "").trim();
        } catch (JSONException e) {
            return "";
        }
    }

    public DriverPackageInfo setDriverById(EnvVars envVars, ImageFs imagefs, String adrenotoolsDriverId) {
        DriverPackageInfo info = getDriverPackageInfo(adrenotoolsDriverId);
        setDriverByInfo(envVars, imagefs, info);
        return info;
    }

    public void setDriverByInfo(EnvVars envVars, ImageFs imagefs, DriverPackageInfo info) {
        if (envVars == null || imagefs == null || info == null || info.isSystemSelection()) return;
        if (info.libraryName == null || info.libraryName.trim().isEmpty()) return;

        String driverPath = getDriverPath(info.entryId);
        envVars.put("ADRENOTOOLS_DRIVER_PATH", driverPath);
        String nativeHooksDir = AppUtils.getNativeLibDir(mContext);
        envVars.put("ADRENOTOOLS_HOOKS_PATH",
                nativeHooksDir == null || nativeHooksDir.trim().isEmpty()
                        ? imagefs.getLibDir()
                        : nativeHooksDir);
        envVars.put("ADRENOTOOLS_DRIVER_NAME", info.libraryName);

        putIfNotEmpty(envVars, "AERO_GRAPHICS_PROVIDER_ENTRY", info.entryId);
        putIfNotEmpty(envVars, "AERO_GRAPHICS_PROVIDER_PACKAGE", info.name);
        putIfNotEmpty(envVars, "AERO_GRAPHICS_PROVIDER_VERSION", info.driverVersion);
        putIfNotEmpty(envVars, "AERO_GRAPHICS_PROVIDER_LANE", info.providerLane);
        putIfNotEmpty(envVars, "AERO_GRAPHICS_DRIVER_ROUTE", info.driverRoute);
        putIfNotEmpty(envVars, "AERO_GRAPHICS_STACK_PROFILE", info.graphicsStackProfile);
        putIfNotEmpty(envVars, "AERO_GRAPHICS_SOURCE_REPO", info.sourceRepo);
        putIfNotEmpty(envVars, "AERO_GRAPHICS_RELEASE_TAG", info.releaseTag);
        putIfNotEmpty(envVars, "AERO_GRAPHICS_ARCHIVE_FORMAT", info.archiveFormat);
        putIfNotEmpty(envVars, "AERO_GRAPHICS_ARCHIVE_LAYOUT", info.archiveLayout);
        putIfNotEmpty(envVars, "AERO_GRAPHICS_INSTALL_SURFACE", info.installSurface);
        putIfNotEmpty(envVars, "AERO_GRAPHICS_TRANSLATION_LAYERS", joinCsv(info.translationLayers));
        putIfNotEmpty(envVars, "AERO_GRAPHICS_API_FOCUS", joinCsv(info.apiFocus));
        putIfNotEmpty(envVars, "AERO_GRAPHICS_FORENSIC_ENV_PREFIXES", joinCsv(info.forensicEnvPrefixes));
        putIfNotEmpty(envVars, "AERO_GRAPHICS_FORENSIC_LOG_PREFIXES", joinCsv(info.forensicLogPrefixes));

        if (info.isTurnipProvider()) {
            putIfNotEmpty(envVars, "AERO_TURNIP_PACKAGE", info.name);
            putIfNotEmpty(envVars, "AERO_TURNIP_VERSION", info.driverVersion);
            putIfNotEmpty(envVars, "AERO_TURNIP_SOURCE_REPO", info.sourceRepo);
            putIfNotEmpty(envVars, "AERO_TURNIP_RELEASE_TAG", info.releaseTag);
            putIfNotEmpty(envVars, "AERO_TURNIP_MESA_MAIN_COMMIT", info.mesaMainCommit);
            putIfNotEmpty(envVars, "AERO_TURNIP_MESA_STABLE_TAG", info.mesaStableTag);
            putIfNotEmpty(envVars, "AERO_TURNIP_ROUTE", info.driverRoute);
            putIfNotEmpty(envVars, "AERO_TURNIP_GALLIUM_BRIDGE", info.preferredGalliumDriver);
            putIfNotEmpty(envVars, "AERO_TURNIP_TRANSLATION_LAYERS", joinCsv(info.translationLayers));
            putIfNotEmpty(envVars, "AERO_TURNIP_API_FOCUS", joinCsv(info.apiFocus));
            putIfNotEmpty(envVars, "AERO_TURNIP_FORENSIC_LOG_PREFIXES", joinCsv(info.forensicLogPrefixes));
            putIfNotEmpty(envVars, "AERO_TURNIP_COMPANION_PROVIDER", info.companionProviderLane);
        }

        if (info.isOpenGlProvider()) {
            putIfNotEmpty(envVars, "AERO_OPENGL_PACKAGE", info.name);
            putIfNotEmpty(envVars, "AERO_OPENGL_VERSION", info.driverVersion);
            putIfNotEmpty(envVars, "AERO_OPENGL_SOURCE_REPO", info.sourceRepo);
            putIfNotEmpty(envVars, "AERO_OPENGL_RELEASE_TAG", info.releaseTag);
            putIfNotEmpty(envVars, "AERO_OPENGL_MESA_MAIN_COMMIT", info.mesaMainCommit);
            putIfNotEmpty(envVars, "AERO_OPENGL_MESA_STABLE_TAG", info.mesaStableTag);
            putIfNotEmpty(envVars, "AERO_OPENGL_ROUTE", info.driverRoute);
            putIfNotEmpty(envVars, "AERO_OPENGL_GALLIUM_DRIVER", info.preferredGalliumDriver);
            putIfNotEmpty(envVars, "AERO_OPENGL_TRANSLATION_LAYERS", joinCsv(info.translationLayers));
            putIfNotEmpty(envVars, "AERO_OPENGL_API_FOCUS", joinCsv(info.apiFocus));
            putIfNotEmpty(envVars, "AERO_OPENGL_FORENSIC_LOG_PREFIXES", joinCsv(info.forensicLogPrefixes));
            putIfNotEmpty(envVars, "AERO_OPENGL_COMPANION_PROVIDER", info.companionProviderLane);
        }

        File winlatorDir = new File(SettingsFragment.DEFAULT_WINLATOR_PATH);
        File qglConfig = new File(winlatorDir, "qgl_config.txt");
        if (qglConfig.exists()) {
            envVars.put("ADRENOTOOLS_REDIRECT_DIR", winlatorDir.getAbsolutePath() + "/");
        }
    }

    public void restoreManagedOverlay(ImageFs imagefs) {
        if (imagefs == null) return;
        File backupRoot = new File(adrenotoolsContentDir, OVERLAY_BACKUP_DIR);
        File manifestFile = new File(backupRoot, OVERLAY_MANIFEST);
        if (!manifestFile.isFile()) return;

        LinkedHashMap<String, String> manifest = readOverlayManifest(manifestFile);
        if (manifest.isEmpty()) return;

        File rootDir = imagefs.getRootDir();
        for (Map.Entry<String, String> entry : manifest.entrySet()) {
            String relativePath = entry.getKey();
            String state = entry.getValue();
            File targetFile = new File(rootDir, relativePath);
            if ("missing".equals(state)) {
                if (targetFile.exists()) FileUtils.delete(targetFile);
                continue;
            }
            File backupFile = new File(backupRoot, relativePath);
            if (backupFile.exists()) FileUtils.copy(backupFile, targetFile);
        }
    }

    public boolean applyManagedOverlay(ImageFs imagefs, DriverPackageInfo info) {
        if (imagefs == null || info == null || !info.hasOverlayPayload()) return false;

        File backupRoot = new File(adrenotoolsContentDir, OVERLAY_BACKUP_DIR);
        if (!backupRoot.exists() && !backupRoot.mkdirs()) return false;
        File manifestFile = new File(backupRoot, OVERLAY_MANIFEST);
        LinkedHashMap<String, String> manifest = readOverlayManifest(manifestFile);
        File packageRoot = new File(getDriverPath(info.entryId));
        File rootDir = imagefs.getRootDir();

        boolean copied = false;
        for (DriverFileMapping mapping : info.fileMappings) {
            if (mapping == null || !mapping.isUsable()) continue;

            File sourceFile = new File(packageRoot, mapping.source);
            if (!sourceFile.exists()) continue;

            String relativeTarget = resolveRelativeTargetPath(rootDir, mapping.target);
            if (relativeTarget.isEmpty()) continue;
            File targetFile = new File(rootDir, relativeTarget);
            File backupFile = new File(backupRoot, relativeTarget);

            if (!manifest.containsKey(relativeTarget)) {
                if (targetFile.exists()) {
                    FileUtils.copy(targetFile, backupFile);
                    manifest.put(relativeTarget, "backup");
                } else {
                    manifest.put(relativeTarget, "missing");
                }
            }

            FileUtils.copy(sourceFile, targetFile);
            copied = true;
        }

        writeOverlayManifest(manifestFile, manifest);
        return copied;
    }

    private LinkedHashMap<String, String> readOverlayManifest(File manifestFile) {
        LinkedHashMap<String, String> manifest = new LinkedHashMap<>();
        if (manifestFile == null || !manifestFile.isFile()) return manifest;
        String content = FileUtils.readString(manifestFile);
        if (content == null || content.trim().isEmpty()) return manifest;
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            int separator = line.indexOf('=');
            if (separator <= 0 || separator >= line.length() - 1) continue;
            manifest.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
        }
        return manifest;
    }

    private void writeOverlayManifest(File manifestFile, LinkedHashMap<String, String> manifest) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : manifest.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append('\n');
            builder.append(entry.getKey().trim()).append('=').append(entry.getValue() == null ? "backup" : entry.getValue().trim());
        }
        FileUtils.writeString(manifestFile, builder.toString());
    }

    private void putIfNotEmpty(EnvVars envVars, String key, String value) {
        if (envVars == null || key == null || key.trim().isEmpty()) return;
        if (value == null || value.trim().isEmpty()) return;
        envVars.put(key, value.trim());
    }

    private String joinCsv(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append(',');
            builder.append(value.trim());
        }
        return builder.toString();
    }

    private boolean isSystemDriverId(String adrenotoolsDriverId) {
        return adrenotoolsDriverId == null
                || adrenotoolsDriverId.trim().isEmpty()
                || "System".equalsIgnoreCase(adrenotoolsDriverId.trim());
    }

    private String resolveRelativeTargetPath(File rootDir, String targetTemplate) {
        if (targetTemplate == null || targetTemplate.trim().isEmpty()) return "";
        String normalized = targetTemplate.trim();
        String driveCRelative = "home/xuser/.wine/drive_c";
        try {
            File driveCRoot = WineUtils.resolveHostWineDriveCRoot(rootDir);
            driveCRelative = rootDir.toPath()
                    .toAbsolutePath()
                    .normalize()
                    .relativize(driveCRoot.toPath().toAbsolutePath().normalize())
                    .toString()
                    .replace(File.separatorChar, '/');
        } catch (Exception ignored) {
        }
        normalized = normalized.replace("${libdir}/", "usr/lib/");
        normalized = normalized.replace("${bindir}/", "usr/bin/");
        normalized = normalized.replace("${sharedir}/", "usr/share/");
        normalized = normalized.replace("${system32}/", driveCRelative + "/windows/system32/");
        normalized = normalized.replace("${syswow64}/", driveCRelative + "/windows/syswow64/");
        return normalized.replaceFirst("^/+", "");
    }
}
