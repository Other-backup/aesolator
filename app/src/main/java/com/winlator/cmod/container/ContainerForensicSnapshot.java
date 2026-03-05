package com.winlator.cmod.container;

import com.winlator.cmod.core.ForensicLogger;

import org.json.JSONException;
import org.json.JSONObject;

public final class ContainerForensicSnapshot {
    private ContainerForensicSnapshot() {}

    public static JSONObject create(Container container, Shortcut shortcut, ContainerNormalizer.NormalizationResult normalized, String routeSource) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("container_id", container != null ? container.id : 0);
            obj.put("container_name", container != null ? container.getName() : "");
            obj.put("container_dir", container != null && container.getRootDir() != null ? container.getRootDir().getAbsolutePath() : "");
            obj.put("route", routeSource == null ? "" : routeSource);
            obj.put("shortcut_path", shortcut != null && shortcut.file != null ? shortcut.file.getAbsolutePath() : "");

            if (normalized != null) {
                obj.put("wine_version", normalized.wineVersion);
                obj.put("emulator", normalized.emulator);
                obj.put("graphics_driver", normalized.graphicsDriver);
                obj.put("audio_driver", normalized.audioDriver);
                obj.put("dxwrapper", normalized.dxwrapper);
                obj.put("dxwrapper_config_hash", ForensicLogger.sha256Hex(normalized.dxwrapperConfig));
                obj.put("env_hash", ForensicLogger.sha256Hex(normalized.envVars));
                obj.put("box64_preset", normalized.box64Preset);
                obj.put("fexcore_preset", normalized.fexcorePreset);
                obj.put("runtime_profile", normalized.runtimeProfile);
                obj.put("box64_version", normalized.box64Version);
                obj.put("fexcore_version", normalized.fexcoreVersion);
                obj.put("lc_all", normalized.lcAll);
                obj.put("runtime_drift_fields", normalized.changedFieldsJson());
            }
        }
        catch (JSONException ignored) {}
        return obj;
    }
}
