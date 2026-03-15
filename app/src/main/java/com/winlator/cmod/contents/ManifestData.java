package com.winlator.cmod.contents;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ManifestData {
    public final int version;
    public final String updatedAt;
    public final LinkedHashMap<String, List<ManifestEntry>> items;

    public ManifestData(int version, @Nullable String updatedAt, @Nullable Map<String, List<ManifestEntry>> items) {
        this.version = version;
        this.updatedAt = updatedAt == null ? "" : updatedAt.trim();
        this.items = new LinkedHashMap<>();
        if (items != null) {
            for (Map.Entry<String, List<ManifestEntry>> entry : items.entrySet()) {
                this.items.put(entry.getKey(), entry.getValue() == null ? Collections.emptyList() : entry.getValue());
            }
        }
    }

    @NonNull
    public static ManifestData empty() {
        return new ManifestData(0, "", new LinkedHashMap<>());
    }

    @Nullable
    public static ManifestData parse(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(jsonString);
            JSONObject itemsObject = root.optJSONObject("items");
            LinkedHashMap<String, List<ManifestEntry>> items = new LinkedHashMap<>();
            if (itemsObject != null) {
                Iterator<String> keys = itemsObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONArray array = itemsObject.optJSONArray(key);
                    ArrayList<ManifestEntry> entries = new ArrayList<>();
                    if (array != null) {
                        for (int i = 0; i < array.length(); i++) {
                            ManifestEntry entry = ManifestEntry.fromJson(array.optJSONObject(i));
                            if (entry != null) entries.add(entry);
                        }
                    }
                    items.put(key, entries);
                }
            }
            return new ManifestData(root.optInt("version", 0), root.optString("updatedAt", ""), items);
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    public List<ManifestEntry> getItems(String key) {
        List<ManifestEntry> list = items.get(key);
        return list == null ? Collections.emptyList() : list;
    }
}
