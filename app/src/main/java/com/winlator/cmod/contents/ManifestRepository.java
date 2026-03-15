package com.winlator.cmod.contents;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class ManifestRepository {
    private static final String PREFS_NAME = "donor_manifest_cache";
    private static final String KEY_JSON = "component_manifest_json";
    private static final String KEY_FETCHED_AT = "component_manifest_fetched_at";
    private static final long ONE_DAY_MS = 24L * 60L * 60L * 1000L;

    public static final String GAMENATIVE_MANIFEST_URL =
            "https://raw.githubusercontent.com/utkarshdalal/GameNative/refs/heads/master/manifest.json";
    public static final String GAMENATIVE_MANIFEST_URL_FALLBACK =
            "https://raw.githubusercontent.com/utkarshdalal/GameNative/master/manifest.json";

    private static final String[] SOURCE_URLS = {
            GAMENATIVE_MANIFEST_URL,
            GAMENATIVE_MANIFEST_URL_FALLBACK
    };

    @NonNull
    public static ManifestData loadManifest(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedJson = prefs.getString(KEY_JSON, "");
        ManifestData cachedManifest = parseManifest(cachedJson);
        if (cachedManifest == null) cachedManifest = ManifestData.empty();

        long lastFetchedAt = prefs.getLong(KEY_FETCHED_AT, 0L);
        boolean stale = System.currentTimeMillis() - lastFetchedAt >= ONE_DAY_MS;
        if (!cachedJson.isEmpty() && !stale) {
            return cachedManifest;
        }

        String fetched = fetchManifestJson();
        ManifestData fetchedManifest = parseManifest(fetched);
        if (fetchedManifest != null) {
            prefs.edit()
                    .putString(KEY_JSON, fetched)
                    .putLong(KEY_FETCHED_AT, System.currentTimeMillis())
                    .apply();
            return fetchedManifest;
        }
        return cachedManifest;
    }

    @Nullable
    public static ManifestData parseManifest(String jsonString) {
        return ManifestData.parse(jsonString);
    }

    @Nullable
    private static String fetchManifestJson() {
        for (String url : SOURCE_URLS) {
            String payload = Downloader.downloadString(url);
            if (payload != null && !payload.trim().isEmpty()) return payload;
        }
        return null;
    }
}
