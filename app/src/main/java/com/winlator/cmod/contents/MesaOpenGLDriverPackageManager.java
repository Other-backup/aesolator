package com.winlator.cmod.contents;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.winlator.cmod.container.GraphicsDrivers;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.TarCompressorUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MesaOpenGLDriverPackageManager {
    private static final String TAG = "MesaOpenGLDriverPkg";
    private static final String CUSTOM_PREFIX = "custom:";

    public static final class PackageInfo {
        public final String entryId;
        public String name = "";
        public String version = "";
        public String providerLane = "";
        public String sourceRepo = "";
        public String artifactName = "";
        public String driverKind = "";
        public String transport = "";
        public String notes = "";
        public String supportClass = "";
        public String kernelEvidenceClass = "";
        public String transportRequirements = "";
        public String ownerLane = "";
        public String routeId = "";
        public String rankedKernelDonors = "";
        public String diagnosticKeys = "";
        public String graphicsStackProfile = "";
        public String preferredGalliumDriver = "";
        public String fallbackGalliumDriver = "";
        public boolean requiresRenderNode = false;
        public boolean experimental = false;

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
    private final String graphicsDriver;
    private final File contentDir;

    public MesaOpenGLDriverPackageManager(Context context, String graphicsDriver) {
        this.context = context.getApplicationContext();
        this.graphicsDriver = GraphicsDrivers.normalize(graphicsDriver);
        this.contentDir = new File(
                this.context.getFilesDir(),
                "contents/mesa_opengl_driver/" + normalizeEntryId(this.graphicsDriver)
        );
        if (!contentDir.exists()) contentDir.mkdirs();
    }

    public static boolean isCustomPackageEntry(String entry) {
        return entry != null && entry.trim().toLowerCase(Locale.US).startsWith(CUSTOM_PREFIX);
    }

    public static String toCustomEntry(String entryId) {
        String normalized = normalizeEntryId(entryId);
        return normalized.isEmpty() ? "" : CUSTOM_PREFIX + normalized;
    }

    public static String toEntryId(String entry) {
        if (entry == null) return "";
        String trimmed = entry.trim();
        if (isCustomPackageEntry(trimmed)) return normalizeEntryId(trimmed.substring(CUSTOM_PREFIX.length()));
        return normalizeEntryId(trimmed);
    }

    public ArrayList<String> getSelectablePackageEntries() {
        ArrayList<String> entries = GraphicsDrivers.getBundledDriverVersions(context, graphicsDriver);
        for (PackageInfo info : enumerateCustomPackages()) {
            String entry = toCustomEntry(info.entryId);
            if (!entry.isEmpty() && !entries.contains(entry)) entries.add(entry);
        }
        return entries;
    }

    public ArrayList<PackageInfo> enumerateCustomPackages() {
        ArrayList<PackageInfo> packages = new ArrayList<>();
        File[] files = contentDir.listFiles();
        if (files == null) return packages;
        for (File file : files) {
            if (file == null || !file.isDirectory() || "tmp".equals(file.getName())) continue;
            if (isBundledPackageEntry(file.getName())) continue;
            PackageInfo info = getPackageInfo(toCustomEntry(file.getName()));
            if (info != null) packages.add(info);
        }
        return packages;
    }

    public PackageInfo getPackageInfo(String packageEntry) {
        String entryId = toEntryId(packageEntry);
        if (entryId.isEmpty()) entryId = GraphicsDrivers.getBundledDriverAsset(context, graphicsDriver).version;
        if (entryId.isEmpty()) return null;

        if (!isCustomPackageEntry(packageEntry) && !extractBundledPackageFromResources(entryId)) return null;
        File packageRoot = new File(contentDir, storageDirName(packageEntry, entryId));
        if (!packageRoot.isDirectory()) return null;
        return parsePackageInfo(entryId, packageRoot);
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
            if (packageRoot == null || !hasInstallSurface(packageRoot)) {
                FileUtils.delete(tmpDir);
                if (!tmpDir.mkdirs()) return "";
                if (!TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, driverUri, tmpDir)) return "";
                packageRoot = findPackageRoot(tmpDir);
            }
            if (packageRoot == null || !hasInstallSurface(packageRoot)) return "";

            PackageInfo info = parsePackageInfo("", packageRoot);
            entryId = resolveInstallEntryId(info);
            if (entryId.isEmpty()) return "";

            File dst = new File(contentDir, entryId);
            if (dst.exists()) FileUtils.delete(dst);
            if (!FileUtils.copy(packageRoot, dst)) entryId = "";
        } catch (IOException e) {
            Log.w(TAG, "Failed to install Mesa OpenGL package", e);
            entryId = "";
        } finally {
            FileUtils.delete(tmpDir);
        }
        return entryId;
    }

    public boolean removeDriver(String packageEntry) {
        if (!isCustomPackageEntry(packageEntry)) return false;
        String entryId = toEntryId(packageEntry);
        if (entryId.isEmpty() || "tmp".equals(entryId)) return false;
        return FileUtils.delete(new File(contentDir, entryId));
    }

    public boolean deployPackageToRoot(File rootDir, String packageEntry) {
        if (rootDir == null || !isCustomPackageEntry(packageEntry)) return false;
        PackageInfo info = getPackageInfo(packageEntry);
        if (info == null) return false;
        String entryId = toEntryId(packageEntry);
        File packageRoot = new File(contentDir, entryId);
        if (!packageRoot.isDirectory()) return false;
        try {
            copyPayload(packageRoot, rootDir, packageRoot);
            return true;
        } catch (IOException e) {
            Log.w(TAG, "Failed to deploy Mesa OpenGL package " + entryId, e);
            return false;
        }
    }

    private boolean extractBundledPackageFromResources(String entryId) {
        GraphicsDrivers.BundledDriverAsset asset = GraphicsDrivers.resolveBundledDriverAsset(
                context,
                graphicsDriver,
                entryId
        );
        if (asset == null) return false;

        File dst = new File(contentDir, asset.version);
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
            info.driverKind = json.optString("driverKind", "").trim();
            info.transport = json.optString("transport", "").trim();
            info.notes = firstNonEmpty(json.optString("notes", ""), json.optString("description", ""));
            info.supportClass = firstNonEmpty(json.optString("supportClass", ""), json.optString("aemaliSupportClass", ""));
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
            info.graphicsStackProfile = json.optString("graphicsStackProfile", "").trim();
            info.preferredGalliumDriver = json.optString("preferredGalliumDriver", "").trim();
            info.fallbackGalliumDriver = json.optString("fallbackGalliumDriver", "").trim();
            info.requiresRenderNode = json.optBoolean("requiresRenderNode", json.optBoolean("requireRenderNode", false));
            info.experimental = json.optBoolean("experimental", false);
        } catch (JSONException e) {
            info.name = entryId;
        }
        return info;
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

    private boolean hasInstallSurface(File packageRoot) {
        return new File(packageRoot, "usr/lib").isDirectory()
                || new File(packageRoot, "usr/share").isDirectory()
                || new File(packageRoot, "lib").isDirectory();
    }

    private String resolveInstallEntryId(PackageInfo info) {
        String raw = firstNonEmpty(info.name, info.artifactName, graphicsDriver + "-custom");
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

    private boolean isBundledPackageEntry(String entryId) {
        if (entryId == null || entryId.trim().isEmpty()) return false;
        return GraphicsDrivers.resolveBundledDriverAsset(context, graphicsDriver, entryId) != null;
    }

    private static String storageDirName(String packageEntry, String entryId) {
        return isCustomPackageEntry(packageEntry) ? entryId : normalizeEntryId(entryId);
    }

    private static String readStringOrArray(JSONObject json, String key) throws JSONException {
        if (!json.has(key) || json.isNull(key)) return "";
        Object value = json.get(key);
        if (value instanceof org.json.JSONArray) {
            org.json.JSONArray array = (org.json.JSONArray) value;
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

    private static String normalizeEntryId(String value) {
        if (value == null) return "";
        return value.trim()
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }
}
