package com.winlator.cmod.runtimeprofile;

import android.content.Context;
import android.os.Build;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.winlator.cmod.core.GPUInformation;
import com.winlator.cmod.core.EnvVars;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RuntimeProfileManager {
    private static final Pattern ADRENO_PATTERN = Pattern.compile("adreno\\s*(\\d{3,4})", Pattern.CASE_INSENSITIVE);

    private RuntimeProfileManager() {}

    public static ArrayList<RuntimeProfile> getProfiles(Context context) {
        ArrayList<RuntimeProfile> profiles = new ArrayList<>();
        profiles.add(new RuntimeProfile(RuntimeProfile.AUTO, "Auto (Balanced)"));
        profiles.add(new RuntimeProfile(RuntimeProfile.LEGACY_LOW_2026, "Legacy / Low-end 2026"));
        profiles.add(new RuntimeProfile(RuntimeProfile.MID_2026, "Mid-range 2026"));
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

        // Baseline shared knobs (independent from FEX/Box profile overlays).
        envVars.put("WINEESYNC", "1");
        envVars.put("WINEFSYNC", "0");
        envVars.put("DXVK_LOG_LEVEL", "none");
        envVars.put("vblank_mode", "0");
        envVars.put("MESA_SHADER_CACHE_DISABLE", "0");
        envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "512M");

        switch (id) {
            case RuntimeProfile.LEGACY_LOW_2026 -> {
                envVars.put("mesa_glthread", "false");
                envVars.put("MESA_NO_ERROR", "0");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "256M");
                envVars.put("DXVK_ASYNC", "0");
            }
            case RuntimeProfile.MID_2026 -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "0");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "512M");
                envVars.put("DXVK_ASYNC", "0");
            }
            case RuntimeProfile.UPPER_MID_2026 -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "1");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "1G");
                envVars.put("DXVK_ASYNC", "1");
            }
            case RuntimeProfile.FLAGSHIP_2026 -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "1");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "2G");
                envVars.put("DXVK_ASYNC", "1");
            }
            case RuntimeProfile.S8G1_BALANCED -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "0");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "1G");
                envVars.put("DXVK_ASYNC", "1");
            }
            case RuntimeProfile.S8G1_SUPER -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "1");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "2G");
                envVars.put("DXVK_ASYNC", "1");
                envVars.put("WINE_LARGE_ADDRESS_AWARE", "1");
            }
            case RuntimeProfile.AUTO -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "0");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "768M");
                envVars.put("DXVK_ASYNC", "0");
            }
            default -> {
                envVars.put("mesa_glthread", "true");
                envVars.put("MESA_NO_ERROR", "0");
                envVars.put("MESA_SHADER_CACHE_MAX_SIZE", "768M");
                envVars.put("DXVK_ASYNC", "0");
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
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, profiles));
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
        String renderer = "";
        try {
            renderer = GPUInformation.getRenderer(null, context);
        } catch (Throwable ignored) {
            renderer = "";
        }
        String normalized = renderer == null ? "" : renderer.toLowerCase(Locale.ENGLISH);
        if (normalized.contains("adreno")) {
            Matcher matcher = ADRENO_PATTERN.matcher(normalized);
            if (matcher.find()) {
                int generation = parseIntSafe(matcher.group(1));
                if (generation >= 700) return "adreno-7xx";
                if (generation >= 600) return "adreno-6xx";
            }
            return "adreno-legacy";
        }
        if (normalized.contains("xclipse")) return "xclipse";
        if (normalized.contains("mali")) {
            if (normalized.contains("g7") || normalized.contains("g8") || normalized.contains("g9")) {
                return "mali-g7xx-or-newer";
            }
            return "mali-legacy";
        }
        return "unknown";
    }

    private static String readBuildField(String fieldName) {
        try {
            Object value = Build.class.getField(fieldName).get(null);
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return -1;
        }
    }
}
