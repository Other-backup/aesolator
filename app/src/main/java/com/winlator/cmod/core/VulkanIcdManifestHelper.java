package com.winlator.cmod.core;

import org.json.JSONObject;

public final class VulkanIcdManifestHelper {
    private VulkanIcdManifestHelper() {
    }

    public static String rewriteLibraryPath(String manifestJson, String libraryPath, String apiVersionOverride) throws Exception {
        if (manifestJson == null || manifestJson.trim().isEmpty()) {
            throw new IllegalArgumentException("manifestJson is empty");
        }
        String normalizedLibraryPath = libraryPath == null ? "" : libraryPath.trim();
        if (normalizedLibraryPath.isEmpty()) {
            throw new IllegalArgumentException("libraryPath is empty");
        }

        JSONObject root = new JSONObject(manifestJson);
        JSONObject icd = root.optJSONObject("ICD");
        if (icd == null) {
            icd = new JSONObject();
            root.put("ICD", icd);
        }
        icd.put("library_path", normalizedLibraryPath);
        if (apiVersionOverride != null && !apiVersionOverride.trim().isEmpty()) {
            icd.put("api_version", apiVersionOverride.trim());
        }
        return root.toString(2);
    }

    public static int readApiMinor(String manifestJson) throws Exception {
        if (manifestJson == null || manifestJson.trim().isEmpty()) return 0;
        JSONObject root = new JSONObject(manifestJson);
        JSONObject icd = root.optJSONObject("ICD");
        if (icd == null) return 0;
        String apiVersion = icd.optString("api_version", "").trim();
        if (apiVersion.isEmpty()) return 0;
        String[] parts = apiVersion.split("\\.");
        if (parts.length < 2) return 0;
        return Integer.parseInt(parts[1]);
    }
}
