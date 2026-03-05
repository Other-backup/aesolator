package com.winlator.cmod.runtimeprofile;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.winlator.cmod.core.EnvVars;

import java.util.ArrayList;

public final class RuntimeProfileManager {
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
        String id = profileId != null && !profileId.trim().isEmpty() ? profileId.trim() : RuntimeProfile.AUTO;
        EnvVars envVars = new EnvVars();

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
}
