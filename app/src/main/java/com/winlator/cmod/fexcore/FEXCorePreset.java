package com.winlator.cmod.fexcore;

import androidx.annotation.NonNull;

public class FEXCorePreset {
    public static final String STABILITY = "STABILITY";
    public static final String COMPATIBILITY = "COMPATIBILITY";
    public static final String INTERMEDIATE = "INTERMEDIATE";
    public static final String PERFORMANCE = "PERFORMANCE";
    public static final String SD662_SAFE = "SD662_SAFE";
    public static final String SD662_BALANCED = "SD662_BALANCED";
    public static final String S8G1_BALANCED = "S8G1_BALANCED";
    public static final String S8G1_SUPER = "S8G1_SUPER";
    public static final String CUSTOM = "CUSTOM";
    public final String id;
    public final String name;

    public FEXCorePreset(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public boolean isCustom() {
        return id.startsWith(CUSTOM);
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
