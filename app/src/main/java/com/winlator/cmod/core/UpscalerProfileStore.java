package com.winlator.cmod.core;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UpscalerProfileStore {
    public static final String PREF_UPSCALER_PROFILES_JSON = "upscaler_profiles_json";
    public static final String PREF_UPSCALER_SELECTED_PROFILE = "upscaler_selected_profile";

    public static final String DEFAULT_PROFILE_ID = "global_default";
    public static final String DEFAULT_PROFILE_NAME = "Global Default";

    private UpscalerProfileStore() {}

    public static final class Profile {
        public String id = DEFAULT_PROFILE_ID;
        public String name = DEFAULT_PROFILE_NAME;
        public String preset = "auto";
        public String backend = "mobfgsr";
        public String effect = "fsr";
        public int scalePercent = 100;
        public boolean frameGeneration = true;
        public int generatedFrames = 1;
        public String fgSource = "native";
        public String fgOutput = "mobfgsr";
        public String framegenMode = "balanced";
        public boolean thermalGuard = true;
        public int targetFps = 60;
        public int interpolationFactor = 50;
        public boolean debugOverlay = false;
        public boolean debugTearLines = false;
        public boolean interpolatedOnly = false;
        public boolean vulkanValidationLayer = false;
        public int sharpness = 100;
        public int denoise = 100;

        public Profile copy() {
            Profile out = new Profile();
            out.id = id;
            out.name = name;
            out.preset = preset;
            out.backend = backend;
            out.effect = effect;
            out.scalePercent = scalePercent;
            out.frameGeneration = frameGeneration;
            out.generatedFrames = generatedFrames;
            out.fgSource = fgSource;
            out.fgOutput = fgOutput;
            out.framegenMode = framegenMode;
            out.thermalGuard = thermalGuard;
            out.targetFps = targetFps;
            out.interpolationFactor = interpolationFactor;
            out.debugOverlay = debugOverlay;
            out.debugTearLines = debugTearLines;
            out.interpolatedOnly = interpolatedOnly;
            out.vulkanValidationLayer = vulkanValidationLayer;
            out.sharpness = sharpness;
            out.denoise = denoise;
            return out;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", safe(id, DEFAULT_PROFILE_ID));
                obj.put("name", safe(name, DEFAULT_PROFILE_NAME));
                obj.put("preset", safe(preset, "auto"));
                obj.put("backend", safe(backend, "mobfgsr"));
                obj.put("effect", safe(effect, "fsr"));
                obj.put("scalePercent", clamp(scalePercent, 100, 100, 200));
                obj.put("frameGeneration", frameGeneration);
                obj.put("generatedFrames", clamp(generatedFrames, 1, 1, 3));
                obj.put("fgSource", safe(fgSource, "native"));
                obj.put("fgOutput", safe(fgOutput, "mobfgsr"));
                obj.put("framegenMode", safe(framegenMode, "balanced"));
                obj.put("thermalGuard", thermalGuard);
                obj.put("targetFps", clamp(targetFps, 60, 30, 144));
                obj.put("interpolationFactor", clamp(interpolationFactor, 50, 0, 100));
                obj.put("debugOverlay", debugOverlay);
                obj.put("debugTearLines", debugTearLines);
                obj.put("interpolatedOnly", interpolatedOnly);
                obj.put("vulkanValidationLayer", vulkanValidationLayer);
                obj.put("sharpness", clamp(sharpness, 100, 0, 100));
                obj.put("denoise", clamp(denoise, 100, 0, 100));
            } catch (Exception ignored) {
            }
            return obj;
        }

        public static Profile fromJson(JSONObject obj) {
            Profile out = defaults();
            if (obj == null) return out;
            out.id = sanitizeId(obj.optString("id", out.id));
            out.name = safe(obj.optString("name", out.name), DEFAULT_PROFILE_NAME);
            out.preset = normalizePreset(obj.optString("preset", out.preset));
            out.backend = normalizeBackend(obj.optString("backend", out.backend));
            out.effect = normalizeEffect(obj.optString("effect", out.effect));
            out.scalePercent = clamp(obj.optInt("scalePercent", out.scalePercent), 100, 100, 200);
            out.frameGeneration = obj.optBoolean("frameGeneration", out.frameGeneration);
            out.generatedFrames = clamp(obj.optInt("generatedFrames", out.generatedFrames), 1, 1, 3);
            out.fgSource = normalizeFgSource(obj.optString("fgSource", out.fgSource));
            out.fgOutput = normalizeFgOutput(obj.optString("fgOutput", out.fgOutput));
            out.framegenMode = normalizeFramegenMode(obj.optString("framegenMode", out.framegenMode));
            out.thermalGuard = obj.optBoolean("thermalGuard", out.thermalGuard);
            out.targetFps = clamp(obj.optInt("targetFps", out.targetFps), 60, 30, 144);
            out.interpolationFactor = clamp(obj.optInt("interpolationFactor", out.interpolationFactor), 50, 0, 100);
            out.debugOverlay = obj.optBoolean("debugOverlay", out.debugOverlay);
            out.debugTearLines = obj.optBoolean("debugTearLines", out.debugTearLines);
            out.interpolatedOnly = obj.optBoolean("interpolatedOnly", out.interpolatedOnly);
            out.vulkanValidationLayer = obj.optBoolean("vulkanValidationLayer", out.vulkanValidationLayer);
            out.sharpness = clamp(obj.optInt("sharpness", out.sharpness), 100, 0, 100);
            out.denoise = clamp(obj.optInt("denoise", out.denoise), 100, 0, 100);
            return out;
        }
    }

    public static Profile defaults() {
        return new Profile();
    }

    public static List<Profile> loadProfiles(SharedPreferences preferences) {
        ArrayList<Profile> profiles = new ArrayList<>();
        if (preferences == null) {
            profiles.add(defaults());
            return profiles;
        }
        String raw = preferences.getString(PREF_UPSCALER_PROFILES_JSON, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) continue;
                Profile profile = Profile.fromJson(obj);
                if (!containsId(profiles, profile.id)) profiles.add(profile);
            }
        } catch (Exception ignored) {
        }
        if (!containsId(profiles, DEFAULT_PROFILE_ID)) {
            profiles.add(0, defaults());
        }
        return profiles;
    }

    public static void saveProfiles(SharedPreferences preferences, List<Profile> profiles) {
        if (preferences == null) return;
        JSONArray out = new JSONArray();
        if (profiles != null) {
            for (Profile profile : profiles) {
                if (profile == null) continue;
                Profile normalized = normalize(profile);
                out.put(normalized.toJson());
            }
        }
        preferences.edit().putString(PREF_UPSCALER_PROFILES_JSON, out.toString()).apply();
    }

    public static String getSelectedProfileId(SharedPreferences preferences) {
        if (preferences == null) return DEFAULT_PROFILE_ID;
        String value = preferences.getString(PREF_UPSCALER_SELECTED_PROFILE, DEFAULT_PROFILE_ID);
        String normalized = sanitizeId(value);
        if (normalized.isEmpty()) return DEFAULT_PROFILE_ID;
        return normalized;
    }

    public static void setSelectedProfileId(SharedPreferences preferences, String profileId) {
        if (preferences == null) return;
        preferences.edit().putString(PREF_UPSCALER_SELECTED_PROFILE, sanitizeId(profileId)).apply();
    }

    public static Profile getSelectedProfile(SharedPreferences preferences) {
        List<Profile> profiles = loadProfiles(preferences);
        String selectedId = getSelectedProfileId(preferences);
        for (Profile profile : profiles) {
            if (profile != null && selectedId.equals(profile.id)) {
                return normalize(profile);
            }
        }
        return defaults();
    }

    public static Profile normalize(Profile profile) {
        if (profile == null) return defaults();
        Profile out = profile.copy();
        out.id = sanitizeId(out.id);
        if (out.id.isEmpty()) out.id = DEFAULT_PROFILE_ID;
        out.name = safe(out.name, DEFAULT_PROFILE_NAME);
        out.preset = normalizePreset(out.preset);
        out.backend = normalizeBackend(out.backend);
        out.effect = normalizeEffect(out.effect);
        out.scalePercent = clamp(out.scalePercent, 100, 100, 200);
        out.generatedFrames = clamp(out.generatedFrames, 1, 1, 3);
        out.fgSource = normalizeFgSource(out.fgSource);
        out.fgOutput = normalizeFgOutput(out.fgOutput);
        out.framegenMode = normalizeFramegenMode(out.framegenMode);
        out.targetFps = clamp(out.targetFps, 60, 30, 144);
        out.interpolationFactor = clamp(out.interpolationFactor, 50, 0, 100);
        out.sharpness = clamp(out.sharpness, 100, 0, 100);
        out.denoise = clamp(out.denoise, 100, 0, 100);
        return out;
    }

    public static String normalizePreset(String value) {
        String normalized = parseIdentifier(value);
        return switch (normalized) {
            case "conservative" -> "conservative";
            case "balanced" -> "balanced";
            case "aggressive" -> "aggressive";
            default -> "auto";
        };
    }

    public static String normalizeBackend(String value) {
        String normalized = parseIdentifier(value);
        return switch (normalized) {
            case "vkbasalt" -> "vkbasalt";
            case "mobfgsr" -> "mobfgsr";
            default -> "off";
        };
    }

    public static String normalizeEffect(String value) {
        String normalized = parseIdentifier(value);
        return switch (normalized) {
            case "cas" -> "cas";
            case "dls" -> "dls";
            case "fsr" -> "fsr";
            case "nis" -> "nis";
            default -> "none";
        };
    }

    public static String normalizeFramegenMode(String value) {
        String normalized = parseIdentifier(value);
        return switch (normalized) {
            case "quality" -> "quality";
            case "lowlatency", "low_latency", "low-latency" -> "low_latency";
            default -> "balanced";
        };
    }

    public static String normalizeFgSource(String value) {
        String normalized = parseIdentifier(value);
        return "opti_fg".equals(normalized) || "optifg".equals(normalized) ? "opti_fg" : "native";
    }

    public static String normalizeFgOutput(String value) {
        String normalized = parseIdentifier(value);
        if ("mobfgsr".equals(normalized)) return "mobfgsr";
        if ("dlssg_to_fsr3".equals(normalized) || "dlssgtofsr3".equals(normalized) || "dlssg-to-fsr3".equals(normalized)) {
            // Keep legacy values readable but converge to native framegen lane.
            return "mobfgsr";
        }
        return "auto";
    }

    public static String sanitizeId(String id) {
        String normalized = parseIdentifier(id);
        if (normalized.isEmpty()) return DEFAULT_PROFILE_ID;
        return normalized.toLowerCase(Locale.US);
    }

    private static boolean containsId(List<Profile> profiles, String id) {
        if (profiles == null || profiles.isEmpty()) return false;
        String target = sanitizeId(id);
        for (Profile profile : profiles) {
            if (profile != null && target.equals(sanitizeId(profile.id))) return true;
        }
        return false;
    }

    private static int clamp(int value, int fallback, int min, int max) {
        int parsed = value;
        if (parsed < min || parsed > max) parsed = fallback;
        return Math.max(min, Math.min(max, parsed));
    }

    private static String safe(String value, String fallback) {
        if (value == null) return fallback;
        String out = value.trim();
        return out.isEmpty() ? fallback : out;
    }

    private static String parseIdentifier(String value) {
        if (value == null) return "";
        return value
                .trim()
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9_\\-]+", "");
    }
}
