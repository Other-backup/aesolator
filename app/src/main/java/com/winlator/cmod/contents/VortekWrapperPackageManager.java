package com.winlator.cmod.contents;

import android.content.Context;

import com.winlator.cmod.container.GraphicsDrivers;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.graphics.GraphicsElfCompatibility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class VortekWrapperPackageManager {
    public static final class PackageInfo {
        public final String entryId;
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
        public String kernelEvidenceClass = "";
        public String transportRequirements = "";
        public String ownerLane = "";
        public String routeId = "";
        public String rankedKernelDonors = "";
        public String diagnosticKeys = "";
        public String graphicsStackProfile = "";
        public boolean requiresRenderNode = false;
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

    public VortekWrapperPackageManager(Context context) {
        this.context = context.getApplicationContext();
        this.contentDir = new File(this.context.getFilesDir(), "contents/vortek_wrapper_driver");
        if (!contentDir.exists()) contentDir.mkdirs();
    }

    public ArrayList<String> getSelectablePackageEntries() {
        return GraphicsDrivers.getBundledDriverVersions(context, GraphicsDrivers.VORTEK);
    }

    public PackageInfo getPackageInfo(String packageEntry) {
        String entryId = normalizeEntryId(packageEntry);
        if (entryId.isEmpty()) return null;
        if (!extractBundledPackageFromResources(entryId)) return null;
        File packageRoot = new File(contentDir, entryId);
        if (!packageRoot.isDirectory()) return null;
        return parsePackageInfo(entryId, packageRoot);
    }

    private boolean extractBundledPackageFromResources(String entryId) {
        GraphicsDrivers.BundledDriverAsset asset = GraphicsDrivers.resolveBundledDriverAsset(
                context,
                GraphicsDrivers.VORTEK,
                entryId
        );
        if (asset == null) return false;

        File dst = new File(contentDir, asset.version);
        if (dst.isDirectory() && new File(dst, "meta.json").isFile() && isExtractedPackageCompatible(dst)) return true;
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
            info.libraryName = firstNonEmpty(json.optString("libraryName", ""), json.optString("hostLibraryPath", ""));
            info.rootLibraryPath = firstNonEmpty(json.optString("rootLibraryPath", ""), json.optString("containerLibraryPath", ""));
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
            info.requiresRenderNode = json.optBoolean("requiresRenderNode", json.optBoolean("requireRenderNode", false));
        } catch (JSONException e) {
            info.name = entryId;
        }
        annotateBionicCompatibility(info, packageRoot);
        return info;
    }

    private boolean isExtractedPackageCompatible(File packageRoot) {
        PackageInfo info = parsePackageInfo("", packageRoot);
        return info.bionicCompatible;
    }

    private void annotateBionicCompatibility(PackageInfo info, File packageRoot) {
        File libraryFile = resolveLibraryFile(packageRoot, info);
        if (libraryFile == null || !GraphicsElfCompatibility.hasForbiddenBionicToken(libraryFile)) return;
        info.bionicCompatible = false;
        info.incompatibilityReason = "forbidden_bionic_dependency";
    }

    private File resolveLibraryFile(File packageRoot, PackageInfo info) {
        if (packageRoot == null || info == null) return null;
        String[] candidates = {
                normalizeRelativePath(info.rootLibraryPath),
                normalizeRelativePath(info.libraryName),
                "usr/lib/libvulkan_vortek.so"
        };
        for (String candidate : candidates) {
            if (candidate.isEmpty()) continue;
            File exact = new File(packageRoot, candidate);
            if (exact.isFile()) return exact;
            File found = findLibraryFile(packageRoot, new File(candidate).getName());
            if (found != null) return found;
        }
        return null;
    }

    private File findLibraryFile(File root, String fileName) {
        if (root == null || !root.isDirectory() || fileName == null || fileName.trim().isEmpty()) return null;
        File[] files = root.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (file.isDirectory()) {
                File nested = findLibraryFile(file, fileName);
                if (nested != null) return nested;
                continue;
            }
            if (fileName.equals(file.getName())) return file;
        }
        return null;
    }

    private String normalizeRelativePath(String path) {
        if (path == null) return "";
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("./")) normalized = normalized.substring(2);
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized.contains("..") ? "" : normalized;
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
