package com.winlator.cmod.runtimeprofile;

import android.content.Context;
import android.os.Build;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.winlator.cmod.core.GPUInformation;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.SocClassifier;
import com.winlator.cmod.core.SpinnerAdapters;

import java.util.ArrayList;
import java.util.Locale;

public final class RuntimeProfileManager {
    private RuntimeProfileManager() {}

    public static ArrayList<RuntimeProfile> getProfiles(Context context) {
        ArrayList<RuntimeProfile> profiles = new ArrayList<>();
        profiles.add(new RuntimeProfile(RuntimeProfile.AUTO, "Auto (Balanced)"));
        profiles.add(new RuntimeProfile(RuntimeProfile.LEGACY_LOW_2026, "Legacy / Low-end 2026"));
        profiles.add(new RuntimeProfile(RuntimeProfile.MID_2026, "Mid-range 2026"));
        profiles.add(new RuntimeProfile(RuntimeProfile.SD662_SAFE, context.getString(com.winlator.cmod.R.string.runtime_profile_sd662_safe)));
        profiles.add(new RuntimeProfile(RuntimeProfile.SD662_BALANCED, context.getString(com.winlator.cmod.R.string.runtime_profile_sd662_balanced)));
        profiles.add(new RuntimeProfile(RuntimeProfile.UPPER_MID_2026, "Upper-mid 2026"));
        profiles.add(new RuntimeProfile(RuntimeProfile.FLAGSHIP_2026, "Flagship 2026"));
        profiles.add(new RuntimeProfile(RuntimeProfile.S8G1_BALANCED, "Snapdragon 8+ Gen1 (Balanced)"));
        profiles.add(new RuntimeProfile(RuntimeProfile.S8G1_SUPER, "Snapdragon 8+ Gen1 (Super)"));
        return profiles;
    }

    public static EnvVars getEnvVars(Context context, String profileId) {
        String requestedId = profileId != null && !profileId.trim().isEmpty() ? profileId.trim() : RuntimeProfile.AUTO;
        String id = resolveEffectiveProfileId(context, requestedId);
        EnvVars envVars = new EnvVars();
        envVars.put("AERO_RUNTIME_PROFILE_REQUESTED", requestedId);
        envVars.put("AERO_RUNTIME_PROFILE_EFFECTIVE", id);
        envVars.put("AERO_RUNTIME_SOC_CLASS", detectSoCClass(context));
        envVars.put("AERO_RUNTIME_POLICY_SOURCE", "runtime_profile_manager");

        // Baseline shared knobs (independent from FEX/Box profile overlays).
        envVars.put("WINEESYNC", "1");
        envVars.put("WINEFSYNC", "0");
        envVars.put("DXVK_LOG_LEVEL", "none");
        envVars.put("vblank_mode", "0");
        envVars.put("MESA_SHADER_CACHE_DISABLE", "0");
        envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "512M");
        envVars.put("AERO_RUNTIME_OOM_CLASS", "balanced");
        envVars.put("AERO_RUNTIME_DRI3_POLICY", "auto");
        envVars.put("BOX64_DYNAREC_BIGBLOCK", "1");
        envVars.put("BOX64_DYNAREC_STRONGMEM", "1");
        envVars.put("BOX64_DYNAREC_SAFEFLAGS", "1");

        switch (id) {
            case RuntimeProfile.LEGACY_LOW_2026 -> {
                envVars.put("mesa_glthread", "false");
                envVars.put("MESA_NO_ERROR", "0");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "256M");
                envVars.put("DXVK_ASYNC", "0");
                envVars.put("AERO_RUNTIME_OOM_CLASS", "conservative");
                envVars.put("AERO_RUNTIME_DRI3_POLICY", "off");
                envVars.put("BOX64_DYNAREC_BIGBLOCK", "0");
                envVars.put("BOX64_DYNAREC_STRONGMEM", "2");
            }
            case RuntimeProfile.SD662_SAFE -> {
                envVars.put("mesa_glthread", "false");
                envVars.put("MESA_NO_ERROR", "0");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "256M");
                envVars.put("DXVK_ASYNC", "0");
                envVars.put("AERO_RUNTIME_OOM_CLASS", "conservative");
                envVars.put("AERO_RUNTIME_DRI3_POLICY", "off");
                envVars.put("BOX64_DYNAREC_BIGBLOCK", "0");
                envVars.put("BOX64_DYNAREC_STRONGMEM", "2");
            }
            case RuntimeProfile.SD662_BALANCED -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "0");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "384M");
                envVars.put("DXVK_ASYNC", "0");
                envVars.put("AERO_RUNTIME_OOM_CLASS", "balanced");
                envVars.put("AERO_RUNTIME_DRI3_POLICY", "auto");
                envVars.put("BOX64_DYNAREC_BIGBLOCK", "1");
                envVars.put("BOX64_DYNAREC_STRONGMEM", "1");
            }
            case RuntimeProfile.MID_2026 -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "0");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "512M");
                envVars.put("DXVK_ASYNC", "0");
                envVars.put("AERO_RUNTIME_OOM_CLASS", "balanced");
                envVars.put("BOX64_DYNAREC_BIGBLOCK", "1");
                envVars.put("BOX64_DYNAREC_STRONGMEM", "1");
            }
            case RuntimeProfile.UPPER_MID_2026 -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "1");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "1G");
                envVars.put("DXVK_ASYNC", "1");
                envVars.put("AERO_RUNTIME_OOM_CLASS", "performance");
                envVars.put("BOX64_DYNAREC_BIGBLOCK", "2");
                envVars.put("BOX64_DYNAREC_STRONGMEM", "1");
            }
            case RuntimeProfile.FLAGSHIP_2026 -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "1");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "2G");
                envVars.put("DXVK_ASYNC", "1");
                envVars.put("AERO_RUNTIME_OOM_CLASS", "performance_plus");
                envVars.put("BOX64_DYNAREC_BIGBLOCK", "3");
                envVars.put("BOX64_DYNAREC_STRONGMEM", "0");
            }
            case RuntimeProfile.S8G1_BALANCED -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "0");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "1G");
                envVars.put("DXVK_ASYNC", "1");
                envVars.put("AERO_RUNTIME_OOM_CLASS", "balanced");
                envVars.put("BOX64_DYNAREC_BIGBLOCK", "2");
                envVars.put("BOX64_DYNAREC_STRONGMEM", "1");
            }
            case RuntimeProfile.S8G1_SUPER -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "1");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "2G");
                envVars.put("DXVK_ASYNC", "1");
                envVars.put("WINE_LARGE_ADDRESS_AWARE", "1");
                envVars.put("AERO_RUNTIME_OOM_CLASS", "performance_plus");
                envVars.put("BOX64_DYNAREC_BIGBLOCK", "3");
                envVars.put("BOX64_DYNAREC_STRONGMEM", "0");
            }
            case RuntimeProfile.AUTO -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "0");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "768M");
                envVars.put("DXVK_ASYNC", "0");
                envVars.put("AERO_RUNTIME_OOM_CLASS", "balanced");
                envVars.put("BOX64_DYNAREC_BIGBLOCK", "1");
                envVars.put("BOX64_DYNAREC_STRONGMEM", "1");
            }
            default -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "0");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "768M");
                envVars.put("DXVK_ASYNC", "0");
                envVars.put("AERO_RUNTIME_OOM_CLASS", "balanced");
                envVars.put("BOX64_DYNAREC_BIGBLOCK", "1");
                envVars.put("BOX64_DYNAREC_STRONGMEM", "1");
            }
        }

        return envVars;
    }

    public static String resolveEffectiveProfileId(Context context, String profileId) {
        String requested = profileId != null && !profileId.trim().isEmpty() ? profileId.trim() : RuntimeProfile.AUTO;
        if (!RuntimeProfile.AUTO.equals(requested)) return requested;

        String socClass = detectSoCClass(context);
        String socModel = readBuildField("SOC_MODEL").toLowerCase(Locale.ENGLISH);
        String hardware = readBuildField("HARDWARE").toLowerCase(Locale.ENGLISH);
        String chipset = socModel + " " + hardware;

        if (chipset.contains("8 elite")
                || chipset.contains("8 gen 4")
                || chipset.contains("8 gen 3")
                || chipset.contains("dimensity 9300")
                || chipset.contains("dimensity 9400")) {
            return RuntimeProfile.FLAGSHIP_2026;
        }
        if (chipset.contains("8+ gen 1")) {
            return RuntimeProfile.S8G1_BALANCED;
        }
        if (chipset.contains("8 gen 2")
                || chipset.contains("xclipse 920")
                || chipset.contains("xclipse 940")
                || chipset.contains("xclipse 950")) {
            return RuntimeProfile.UPPER_MID_2026;
        }

        switch (socClass) {
            case "adreno-7xx":
            case "xclipse":
                return RuntimeProfile.UPPER_MID_2026;
            case "adreno-6xx":
            case "mali-g7xx-or-newer":
                return RuntimeProfile.MID_2026;
            case "adreno-legacy":
            case "mali-legacy":
                return RuntimeProfile.LEGACY_LOW_2026;
            default:
                return RuntimeProfile.MID_2026;
        }
    }

    public static void loadSpinner(Spinner spinner, String selectedId) {
        Context context = spinner.getContext();
        ArrayList<RuntimeProfile> profiles = getProfiles(context);
        int selectedPosition = 0;
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).id.equals(selectedId)) {
                selectedPosition = i;
                break;
            }
        }
        spinner.setAdapter(SpinnerAdapters.createGeneric(context, profiles));
        spinner.setSelection(selectedPosition);
    }

    public static String getSpinnerSelectedId(Spinner spinner) {
        SpinnerAdapter adapter = spinner.getAdapter();
        int selectedPosition = spinner.getSelectedItemPosition();
        if (adapter != null && adapter.getCount() > 0 && selectedPosition >= 0) {
            return ((RuntimeProfile) adapter.getItem(selectedPosition)).id;
        }
        return RuntimeProfile.AUTO;
    }

    private static String detectSoCClass(Context context) {
        String renderer;
        try {
            renderer = GPUInformation.getRenderer(null, context);
        } catch (Throwable ignored) {
            renderer = "";
        }
        SocClassifier.Tier tier = SocClassifier.detect(
                renderer,
                readBuildField("SOC_MODEL"),
                readBuildField("HARDWARE"),
                readSystemProperty("ro.board.platform"),
                readSystemProperty("ro.product.board")
        );
        return switch (tier) {
            case ADRENO_7XX -> "adreno-7xx";
            case ADRENO_6XX -> "adreno-6xx";
            case ADRENO_LEGACY -> "adreno-legacy";
            case XCLIPSE_RDNA_MOBILE -> "xclipse";
            case MALI_G7XX_OR_NEWER -> "mali-g7xx-or-newer";
            case MALI_LEGACY -> "mali-legacy";
            default -> "unknown";
        };
    }

    private static String readBuildField(String fieldName) {
        try {
            Object value = Build.class.getField(fieldName).get(null);
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String readSystemProperty(String key) {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Object value = systemPropertiesClass
                    .getMethod("get", String.class, String.class)
                    .invoke(null, key, "");
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable ignored) {
            return "";
        }
    }
}
