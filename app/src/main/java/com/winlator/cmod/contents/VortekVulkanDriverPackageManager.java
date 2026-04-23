package com.winlator.cmod.contents;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.winlator.cmod.container.GraphicsDrivers;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.graphics.GraphicsElfCompatibility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class VortekVulkanDriverPackageManager {
    private static final String TAG = "VortekVulkanDriverPkg";
    public static final String SYSTEM_ENTRY = "system";
    public static final String AEMALI_PANVK_PREFIX = "aemali-panvk:";
    private static final String CUSTOM_PREFIX = "custom:";

    public static final class PackageInfo {
        public final String entryId;
        public boolean builtin = false;
        public String name = "";
        public String version = "";
        public String providerLane = "";
        public String sourceRepo = "";
        public String artifactName = "";
        public String libraryName = "";
        public String rootLibraryPath = "";
        public String driverKind = "";
        public String transport = "";
        public String notes = "";
        public String supportClass = "";
        public String vulkanApiCeiling = "";
        public String kernelEvidenceClass = "";
        public String transportRequirements = "";
        public String ownerLane = "";
        public String routeId = "";
        public String rankedKernelDonors = "";
        public String diagnosticKeys = "";
        public boolean requiresRenderNode = false;
        public boolean experimental = false;
        public boolean bionicCompatible = true;
        public String incompatibilityReason = "";

        PackageInfo(String entryId) {
            this.entryId = entryId == null ? "" : entryId.trim();
        }

        public String getDisplayLabel() {
            String label = name == null || name.trim().isEmpty() ? entryId : name.trim();
            if (version != null && !version.trim().isEmpty()) label += " " + version.trim();
            return label;
        }
    }

    private final Context context;
    private final File contentDir;

    public VortekVulkanDriverPackageManager(Context context) {
        this.context = context.getApplicationContext();
        this.contentDir = new File(this.context.getFilesDir(), "contents/vortek_vulkan_driver");
        if (!contentDir.exists()) contentDir.mkdirs();
    }

    public static boolean isSystemEntry(String entry) {
        return SYSTEM_ENTRY.equals(normalizeEntry(entry));
    }

    public static boolean isCustomPackageEntry(String entry) {
        return entry != null && entry.trim().toLowerCase(Locale.US).startsWith(CUSTOM_PREFIX);
    }

    public static boolean isBundledAeMaliPackageEntry(String entry) {
        return entry != null && entry.trim().toLowerCase(Locale.US).startsWith(AEMALI_PANVK_PREFIX);
    }

    public static String toCustomEntry(String entryId) {
        String normalized = normalizeEntryId(entryId);
        return normalized.isEmpty() ? "" : CUSTOM_PREFIX + normalized;
    }

    public static String toBundledAeMaliEntry(String version) {
        String normalized = normalizeEntryId(version);
        return normalized.isEmpty() ? "" : AEMALI_PANVK_PREFIX + normalized;
    }

    public static String toEntryId(String entry) {
        if (entry == null) return "";
        String trimmed = entry.trim();
        if (isCustomPackageEntry(trimmed)) return normalizeEntryId(trimmed.substring(CUSTOM_PREFIX.length()));
        if (isBundledAeMaliPackageEntry(trimmed)) return normalizeEntryId(trimmed.substring(AEMALI_PANVK_PREFIX.length()));
        return normalizeEntryId(trimmed);
    }

    public static String normalizeEntry(String entry) {
        if (entry == null) return SYSTEM_ENTRY;
        String trimmed = entry.trim();
        if (trimmed.isEmpty()) return SYSTEM_ENTRY;
        if (isCustomPackageEntry(trimmed)) return toCustomEntry(toEntryId(trimmed));
        if (isBundledAeMaliPackageEntry(trimmed)) return toBundledAeMaliEntry(toEntryId(trimmed));
        return trimmed.toLowerCase(Locale.US);
    }

    public ArrayList<String> getSelectableDriverEntries() {
        ArrayList<String> entries = new ArrayList<>();
        entries.add(SYSTEM_ENTRY);
        if (GraphicsDrivers.resolveBundledDriverAsset(context, GraphicsDrivers.AEMALI_PANVK) != null) {
            for (String version : GraphicsDrivers.getBundledDriverVersions(context, GraphicsDrivers.AEMALI_PANVK)) {
                String entry = toBundledAeMaliEntry(version);
                if (!entry.isEmpty() && !entries.contains(entry)) entries.add(entry);
            }
        }
        for (PackageInfo info : enumerateCustomPackages()) {
            String entry = toCustomEntry(info.entryId);
            if (!entries.contains(entry)) entries.add(entry);
        }
        return entries;
    }

    public ArrayList<PackageInfo> enumerateCustomPackages() {
        ArrayList<PackageInfo> packages = new ArrayList<>();
        File[] files = contentDir.listFiles();
        if (files == null) return packages;
        for (File file : files) {
            if (file == null || !file.isDirectory() || "tmp".equals(file.getName())) continue;
            if (isBundledAeMaliStorageDir(file.getName())) continue;
            PackageInfo info = getPackageInfo(toCustomEntry(file.getName()));
            if (info != null) packages.add(info);
        }
        return packages;
    }

    public PackageInfo getPackageInfo(String driverEntry) {
        String normalized = normalizeEntry(driverEntry);
        if (isSystemEntry(normalized)) return buildSystemInfo();
        if (!isCustomPackageEntry(normalized) && !isBundledAeMaliPackageEntry(normalized)) return null;

        String entryId = toEntryId(normalized);
        if (entryId.isEmpty()) return null;
        if (isBundledAeMaliPackageEntry(normalized) && !extractBundledAeMaliPackageFromResources(entryId)) return null;
        File packageRoot = new File(contentDir, storageDirName(normalized));
        if (!packageRoot.isDirectory()) return null;
        return parsePackageInfo(entryId, packageRoot);
    }

    public String resolveLibraryPath(String driverEntry) {
        String normalized = normalizeEntry(driverEntry);
        if (isSystemEntry(normalized)
                || (!isCustomPackageEntry(normalized) && !isBundledAeMaliPackageEntry(normalized))) return null;

        PackageInfo info = getPackageInfo(normalized);
        if (info == null) return null;
        File packageRoot = new File(contentDir, storageDirName(normalized));
        if (!packageRoot.isDirectory()) return null;
        File libraryFile = resolveLibraryFile(packageRoot, info);
        if (libraryFile == null || !libraryFile.isFile() || !GraphicsElfCompatibility.isBionicCompatibleLibrary(libraryFile)) {
            return null;
        }
        return libraryFile.getAbsolutePath();
    }

    public String resolveRootLibraryPath(String driverEntry) {
        String normalized = normalizeEntry(driverEntry);
        if (isSystemEntry(normalized)
                || (!isCustomPackageEntry(normalized) && !isBundledAeMaliPackageEntry(normalized))) return "";

        PackageInfo info = getPackageInfo(normalized);
        if (info == null) return "";
        File packageRoot = new File(contentDir, storageDirName(normalized));
        if (!packageRoot.isDirectory()) return "";
        return resolveRootLibraryPath(info, packageRoot);
    }

    public String installDriver(Uri driverUri) {
        File tmpDir = new File(contentDir, "tmp");
        if (tmpDir.exists()) FileUtils.delete(tmpDir);
        if (!tmpDir.mkdirs()) return "";

        String entryId = "";
        try {
            File packageRoot;
            try (InputStream inputStream = context.getContentResolver().openInputStream(driverUri)) {
                if (inputStream == null || !extractZipSafely(inputStream, tmpDir)) return "";
            }
            packageRoot = findPackageRoot(tmpDir);
            if (packageRoot == null) {
                FileUtils.delete(tmpDir);
                if (!tmpDir.mkdirs()) return "";
                if (!TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, driverUri, tmpDir)) return "";
                packageRoot = findPackageRoot(tmpDir);
            }
            if (packageRoot == null) return "";

            PackageInfo info = parsePackageInfo("", packageRoot);
            File libraryFile = resolveLibraryFile(packageRoot, info);
            if (libraryFile == null || !GraphicsElfCompatibility.isBionicCompatibleLibrary(libraryFile)) return "";

            entryId = resolveInstallEntryId(info);
            if (entryId.isEmpty()) return "";

            File dst = new File(contentDir, entryId);
            if (dst.exists()) FileUtils.delete(dst);
            if (!FileUtils.copy(packageRoot, dst)) entryId = "";
        } catch (IOException e) {
            Log.w(TAG, "Failed to install Vortek Vulkan package", e);
            entryId = "";
        } finally {
            FileUtils.delete(tmpDir);
        }
        return entryId;
    }

    public boolean removeDriver(String driverEntry) {
        if (!isCustomPackageEntry(driverEntry)) return false;
        String entryId = toEntryId(driverEntry);
        if (entryId.isEmpty() || "tmp".equals(entryId)) return false;
        return FileUtils.delete(new File(contentDir, entryId));
    }

    public boolean deployPackageToRoot(File rootDir, String driverEntry) {
        String normalized = normalizeEntry(driverEntry);
        if (rootDir == null
                || (!isCustomPackageEntry(normalized) && !isBundledAeMaliPackageEntry(normalized))) return false;
        PackageInfo info = getPackageInfo(normalized);
        if (info == null || !info.bionicCompatible) return false;
        File packageRoot = new File(contentDir, storageDirName(normalized));
        if (!packageRoot.isDirectory()) return false;
        try {
            copyPayload(packageRoot, rootDir, packageRoot);
            return true;
        } catch (IOException e) {
            Log.w(TAG, "Failed to deploy Vortek Vulkan package " + toEntryId(normalized), e);
            return false;
        }
    }

    private PackageInfo buildSystemInfo() {
        PackageInfo info = new PackageInfo(SYSTEM_ENTRY);
        info.builtin = true;
        info.name = "System HAL";
        info.providerLane = "android-system-vulkan";
        info.sourceRepo = "https://source.android.com/docs/core/graphics/implement-vulkan";
        info.driverKind = "android-hal";
        info.transport = "android-hal";
        info.routeId = "android-hal-vulkan-mali";
        info.supportClass = "separate-transport";
        info.kernelEvidenceClass = "android-vendor-hal";
        info.transportRequirements = "android-system-vulkan-loader,vendor-hwvulkan-module,no-mesa-panvk-identity";
        info.notes = "Android Vulkan loader resolves the active vendor HAL from /vendor/lib[64]/hw/vulkan.<soc>.so.";
        return info;
    }

    private boolean extractBundledAeMaliPackageFromResources(String entryId) {
        GraphicsDrivers.BundledDriverAsset asset = GraphicsDrivers.resolveBundledDriverAsset(
                context,
                GraphicsDrivers.AEMALI_PANVK,
                entryId
        );
        if (asset == null) return false;

        File dst = new File(contentDir, storageDirName(toBundledAeMaliEntry(asset.version)));
        if (dst.isDirectory() && new File(dst, "meta.json").isFile()) return true;
        if (dst.exists()) FileUtils.delete(dst);
        if (!dst.mkdirs()) return false;
        boolean extracted = TarCompressorUtils.extract(
                TarCompressorUtils.Type.ZSTD,
                context,
                asset.assetPath,
                dst
        );
        if (!extracted) FileUtils.delete(dst);
        return extracted;
    }

    private PackageInfo parsePackageInfo(String entryId, File packageRoot) {
        PackageInfo info = new PackageInfo(entryId);
        File metaFile = new File(packageRoot, "meta.json");
        if (!metaFile.isFile()) {
            info.name = entryId;
            annotateBionicCompatibility(info, packageRoot);
            return info;
        }

        try {
            String rawJson = FileUtils.readString(metaFile);
            if (rawJson == null || rawJson.trim().isEmpty()) return info;
            JSONObject json = new JSONObject(rawJson);
            info.name = firstNonEmpty(json.optString("name", ""), entryId);
            info.version = firstNonEmpty(
                    json.optString("driverVersion", ""),
                    json.optString("version", ""),
                    json.optString("sourceVersion", "")
            );
            info.providerLane = firstNonEmpty(json.optString("providerLane", ""), json.optString("driverRoute", ""));
            info.sourceRepo = json.optString("sourceRepo", "").trim();
            info.artifactName = json.optString("artifactName", "").trim();
            info.libraryName = firstNonEmpty(json.optString("libraryName", ""), json.optString("hostLibraryPath", ""));
            info.rootLibraryPath = firstNonEmpty(json.optString("rootLibraryPath", ""), json.optString("containerLibraryPath", ""));
            info.driverKind = json.optString("driverKind", "").trim();
            info.transport = json.optString("transport", "").trim();
            info.notes = firstNonEmpty(json.optString("notes", ""), json.optString("description", ""));
            info.supportClass = firstNonEmpty(json.optString("supportClass", ""), json.optString("aemaliSupportClass", ""));
            info.vulkanApiCeiling = firstNonEmpty(json.optString("vulkanApiCeiling", ""), json.optString("apiCeiling", ""));
            info.kernelEvidenceClass = firstNonEmpty(
                    readStringOrArray(json, "kernelEvidenceClass"),
                    readStringOrArray(json, "kernelEvidenceClasses")
            );
            info.transportRequirements = firstNonEmpty(
                    readStringOrArray(json, "transportRequirements"),
                    readStringOrArray(json, "runtimeRequirements")
            );
            info.ownerLane = firstNonEmpty(json.optString("ownerLane", ""), json.optString("donorOwnerLane", ""));
            info.routeId = firstNonEmpty(json.optString("routeId", ""), json.optString("supportRouteId", ""));
            info.rankedKernelDonors = firstNonEmpty(
                    readStringOrArray(json, "rankedKernelDonors"),
                    readStringOrArray(json, "kernelDonorRanking")
            );
            info.diagnosticKeys = firstNonEmpty(
                    readStringOrArray(json, "diagnosticKeys"),
                    readStringOrArray(json, "forensicKeys")
            );
            info.requiresRenderNode = json.optBoolean("requiresRenderNode", json.optBoolean("requireRenderNode", false));
            info.experimental = json.optBoolean("experimental", false);
        } catch (JSONException e) {
            info.name = entryId;
        }
        annotateBionicCompatibility(info, packageRoot);
        return info;
    }

    private void annotateBionicCompatibility(PackageInfo info, File packageRoot) {
        File libraryFile = resolveLibraryFile(packageRoot, info);
        if (libraryFile == null || !GraphicsElfCompatibility.hasForbiddenBionicToken(libraryFile)) return;
        info.bionicCompatible = false;
        info.incompatibilityReason = "forbidden_bionic_dependency";
    }

    private static String readStringOrArray(JSONObject json, String key) throws JSONException {
        if (!json.has(key) || json.isNull(key)) return "";
        Object value = json.get(key);
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            StringBuilder joined = new StringBuilder();
            for (int i = 0; i < array.length(); i++) {
                String item = array.optString(i, "").trim();
                if (item.isEmpty()) continue;
                if (joined.length() > 0) joined.append(",");
                joined.append(item);
            }
            return joined.toString();
        }
        return String.valueOf(value).trim();
    }

    private boolean extractZipSafely(InputStream inputStream, File outputDir) {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File dstFile = FileUtils.resolveSafeArchiveEntry(outputDir, entry.getName());
                if (dstFile == null) return false;
                if (entry.isDirectory()) {
                    if (!dstFile.exists() && !dstFile.mkdirs()) return false;
                } else {
                    File parent = dstFile.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) return false;
                    Files.copy(zipInputStream, dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                zipInputStream.closeEntry();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private File findPackageRoot(File rootDir) {
        if (rootDir == null || !rootDir.isDirectory()) return null;
        if (new File(rootDir, "meta.json").isFile()) return rootDir;
        File[] files = rootDir.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (!file.isDirectory()) continue;
            File nested = findPackageRoot(file);
            if (nested != null) return nested;
        }
        return null;
    }

    private File resolveLibraryFile(File packageRoot, PackageInfo info) {
        if (packageRoot == null || info == null) return null;

        String[] explicitCandidates = {
                normalizeRelativePath(info.rootLibraryPath),
                normalizeRelativePath(info.libraryName)
        };
        for (String candidate : explicitCandidates) {
            if (candidate.isEmpty()) continue;
            File exact = new File(packageRoot, candidate);
            if (exact.isFile()) return exact;
            File foundByName = findLibraryFile(packageRoot, new File(candidate).getName());
            if (foundByName != null) return foundByName;
        }

        File preferred = findLibraryFile(packageRoot, "libvulkan_wrapper.so");
        if (preferred != null) return preferred;
        preferred = findLibraryFile(packageRoot, "libvulkan_vortek.so");
        if (preferred != null) return preferred;
        return findFirstMatchingLibrary(packageRoot);
    }

    private String resolveRootLibraryPath(PackageInfo info, File packageRoot) {
        String explicit = normalizeRelativePath(info.rootLibraryPath);
        if (!explicit.isEmpty()) return explicit;

        String libraryName = normalizeRelativePath(info.libraryName);
        if (!libraryName.isEmpty()) {
            File direct = new File(packageRoot, libraryName);
            if (direct.isFile()) return libraryName;
            File found = findLibraryFile(packageRoot, new File(libraryName).getName());
            if (found != null) return relativize(packageRoot, found);
            return "usr/lib/" + new File(libraryName).getName();
        }

        File found = findFirstMatchingLibrary(packageRoot);
        return found == null ? "" : relativize(packageRoot, found);
    }

    private File findFirstMatchingLibrary(File root) {
        if (root == null || !root.isDirectory()) return null;
        File[] files = root.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (file.isDirectory()) {
                File nested = findFirstMatchingLibrary(file);
                if (nested != null) return nested;
                continue;
            }
            String name = file.getName().toLowerCase(Locale.US);
            if (name.startsWith("libvulkan") && name.endsWith(".so")) return file;
        }
        return null;
    }

    private File findLibraryFile(File root, String fileName) {
        if (root == null || !root.isDirectory() || fileName == null || fileName.trim().isEmpty()) return null;
        String normalized = fileName.trim();
        File[] files = root.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (file.isDirectory()) {
                File nested = findLibraryFile(file, normalized);
                if (nested != null) return nested;
                continue;
            }
            if (normalized.equals(file.getName())) return file;
        }
        return null;
    }

    private String resolveInstallEntryId(PackageInfo info) {
        String raw = firstNonEmpty(info.name, info.artifactName, "vortek-vulkan-custom");
        String version = info.version == null ? "" : info.version.trim();
        String combined = raw + (version.isEmpty() ? "" : "-" + version);
        return normalizeEntryId(combined);
    }

    private void copyPayload(File src, File dst, File packageRoot) throws IOException {
        String relativePath = packageRoot.toPath().relativize(src.toPath()).toString().replace(File.separatorChar, '/');
        if (isPackageMetadata(relativePath)) return;
        if (src.isDirectory()) {
            if (!dst.exists() && !dst.mkdirs()) throw new IOException("Unable to create " + dst.getAbsolutePath());
            File[] files = src.listFiles();
            if (files == null) return;
            for (File file : files) copyPayload(file, new File(dst, file.getName()), packageRoot);
        } else {
            File parent = dst.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Unable to create " + parent.getAbsolutePath());
            }
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean isPackageMetadata(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return false;
        String normalized = relativePath.replace('\\', '/');
        return "meta.json".equals(normalized)
                || "profile.json".equals(normalized)
                || "ae-runtime-contract.json".equals(normalized);
    }

    private String relativize(File root, File file) {
        if (root == null || file == null) return "";
        Path rootPath = root.toPath();
        Path filePath = file.toPath();
        return normalizeRelativePath(rootPath.relativize(filePath).toString());
    }

    private static String normalizeEntryId(String value) {
        if (value == null) return "";
        return value.trim()
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }

    private static boolean isBundledAeMaliStorageDir(String name) {
        return name != null && name.startsWith(GraphicsDrivers.AEMALI_PANVK + "-");
    }

    private static String storageDirName(String normalizedEntry) {
        if (isBundledAeMaliPackageEntry(normalizedEntry)) {
            return GraphicsDrivers.AEMALI_PANVK + "-" + toEntryId(normalizedEntry);
        }
        return toEntryId(normalizedEntry);
    }

    private String normalizeRelativePath(String path) {
        if (path == null) return "";
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("./")) normalized = normalized.substring(2);
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }
}
