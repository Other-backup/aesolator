package com.winlator.cmod.contents;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

public final class ManifestEntry {
    public final String id;
    public final String name;
    public final String url;
    public final String variant;
    public final String arch;

    public ManifestEntry(String id, String name, String url, @Nullable String variant, @Nullable String arch) {
        this.id = id == null ? "" : id.trim();
        this.name = name == null ? "" : name.trim();
        this.url = url == null ? "" : url.trim();
        this.variant = variant == null ? "" : variant.trim();
        this.arch = arch == null ? "" : arch.trim();
    }

    public boolean isUsable() {
        return !id.isEmpty() && !url.isEmpty();
    }

    public String getDisplayName() {
        return name.isEmpty() ? id : name;
    }

    @NonNull
    @Override
    public String toString() {
        return getDisplayName();
    }

    @Nullable
    public static ManifestEntry fromJson(JSONObject object) {
        if (object == null) return null;
        ManifestEntry entry = new ManifestEntry(
                object.optString("id", ""),
                object.optString("name", ""),
                object.optString("url", ""),
                object.optString("variant", ""),
                object.optString("arch", "")
        );
        return entry.isUsable() ? entry : null;
    }
}
