package com.winlator.cmod.container;

import com.winlator.cmod.core.EnvVars;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class ContainerNormalizer {
    private ContainerNormalizer() {}

    public static NormalizationResult normalizeForLaunch(Container container, Shortcut shortcut) {
        if (container == null) {
            return new NormalizationResult(
                    "", "", "", "", "", "", "",
                    "", "", "", "", "", "",
                    new ArrayList<>());
        }

        List<String> changed = new ArrayList<>();

        String baseWineVersion = safe(container.getWineVersion());
        String baseEmulator = safe(container.getEmulator());
        String baseGraphicsDriver = safe(container.getGraphicsDriver());
        String baseAudioDriver = safe(container.getAudioDriver());
        String baseDxWrapper = safe(container.getDXWrapper());
        String baseDxWrapperConfig = safe(container.getDXWrapperConfig());
        String baseEnvVars = safe(container.getEnvVars());
        String baseBox64Preset = safe(container.getBox64Preset());
        String baseFexcorePreset = safe(container.getFEXCorePreset());
        String baseRuntimeProfile = safe(container.getExtra("runtimeProfile", ""));
        String baseBox64Version = safe(container.getBox64Version());
        String baseFexcoreVersion = safe(container.getFEXCoreVersion());
        String baseLcAll = safe(container.getLC_ALL());

        String wineVersion = override(shortcut, "wineVersion", baseWineVersion, changed, "wineVersion");
        String emulator = override(shortcut, "emulator", baseEmulator, changed, "emulator");
        String graphicsDriver = override(shortcut, "graphicsDriver", baseGraphicsDriver, changed, "graphicsDriver");
        String audioDriver = override(shortcut, "audioDriver", baseAudioDriver, changed, "audioDriver");
        String dxWrapper = override(shortcut, "dxwrapper", baseDxWrapper, changed, "dxwrapper");
        String dxWrapperConfig = override(shortcut, "dxwrapperConfig", baseDxWrapperConfig, changed, "dxwrapperConfig");
        String box64Preset = override(shortcut, "box64Preset", baseBox64Preset, changed, "box64Preset");
        String fexcorePreset = override(shortcut, "fexcorePreset", baseFexcorePreset, changed, "fexcorePreset");
        String runtimeProfile = override(shortcut, "runtimeProfile", baseRuntimeProfile, changed, "runtimeProfile");
        String box64Version = override(shortcut, "box64Version", baseBox64Version, changed, "box64Version");
        String fexcoreVersion = override(shortcut, "fexcoreVersion", baseFexcoreVersion, changed, "fexcoreVersion");
        String lcAll = override(shortcut, "lc_all", baseLcAll, changed, "lc_all");

        String envVars = mergeEnvVars(baseEnvVars, shortcut != null ? shortcut.getExtra("envVars", "") : "", changed);

        return new NormalizationResult(
                wineVersion, emulator, graphicsDriver, audioDriver, dxWrapper, dxWrapperConfig, envVars,
                box64Preset, fexcorePreset, runtimeProfile, box64Version, fexcoreVersion, lcAll,
                changed);
    }

    private static String mergeEnvVars(String baseEnvVars, String shortcutEnvVars, List<String> changed) {
        EnvVars merged = new EnvVars();
        if (!safe(baseEnvVars).isEmpty()) merged.putAll(baseEnvVars);
        String normalizedShortcut = safe(shortcutEnvVars);
        if (!normalizedShortcut.isEmpty()) {
            merged.putAll(normalizedShortcut);
            if (!normalizedShortcut.equals(safe(baseEnvVars))) changed.add("envVars");
        }
        return merged.toString();
    }

    private static String override(
            Shortcut shortcut,
            String key,
            String fallback,
            List<String> changed,
            String changedName) {
        if (shortcut == null) return safe(fallback);
        String value = safe(shortcut.getExtra(key, fallback));
        String baseline = safe(fallback);
        if (!value.equals(baseline)) changed.add(changedName);
        return value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class NormalizationResult {
        public final String wineVersion;
        public final String emulator;
        public final String graphicsDriver;
        public final String audioDriver;
        public final String dxwrapper;
        public final String dxwrapperConfig;
        public final String envVars;
        public final String box64Preset;
        public final String fexcorePreset;
        public final String runtimeProfile;
        public final String box64Version;
        public final String fexcoreVersion;
        public final String lcAll;

        private final List<String> changedFields;

        private NormalizationResult(
                String wineVersion,
                String emulator,
                String graphicsDriver,
                String audioDriver,
                String dxwrapper,
                String dxwrapperConfig,
                String envVars,
                String box64Preset,
                String fexcorePreset,
                String runtimeProfile,
                String box64Version,
                String fexcoreVersion,
                String lcAll,
                List<String> changedFields) {
            this.wineVersion = safe(wineVersion);
            this.emulator = safe(emulator);
            this.graphicsDriver = safe(graphicsDriver);
            this.audioDriver = safe(audioDriver);
            this.dxwrapper = safe(dxwrapper);
            this.dxwrapperConfig = safe(dxwrapperConfig);
            this.envVars = safe(envVars);
            this.box64Preset = safe(box64Preset);
            this.fexcorePreset = safe(fexcorePreset);
            this.runtimeProfile = safe(runtimeProfile);
            this.box64Version = safe(box64Version);
            this.fexcoreVersion = safe(fexcoreVersion);
            this.lcAll = safe(lcAll);
            this.changedFields = changedFields == null ? new ArrayList<>() : changedFields;
        }

        public boolean hasDrift() {
            return !changedFields.isEmpty();
        }

        public JSONArray changedFieldsJson() {
            JSONArray array = new JSONArray();
            for (String field : changedFields) array.put(field);
            return array;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("wine_version", wineVersion);
                obj.put("emulator", emulator);
                obj.put("graphics_driver", graphicsDriver);
                obj.put("audio_driver", audioDriver);
                obj.put("dxwrapper", dxwrapper);
                obj.put("dxwrapper_config", dxwrapperConfig);
                obj.put("env_vars", envVars);
                obj.put("box64_preset", box64Preset);
                obj.put("fexcore_preset", fexcorePreset);
                obj.put("runtime_profile", runtimeProfile);
                obj.put("box64_version", box64Version);
                obj.put("fexcore_version", fexcoreVersion);
                obj.put("lc_all", lcAll);
                obj.put("changed_fields", changedFieldsJson());
            }
            catch (JSONException ignored) { /* best-effort path; keep surrounding flow intact. */ }
            return obj;
        }
    }
}
