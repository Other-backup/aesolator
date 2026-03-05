package com.winlator.cmod.runtimeprofile;

import androidx.annotation.NonNull;

public final class RuntimeProfile {
    public static final String AUTO = "AUTO";
    public static final String LEGACY_LOW_2026 = "LEGACY_LOW_2026";
    public static final String MID_2026 = "MID_2026";
    public static final String UPPER_MID_2026 = "UPPER_MID_2026";
    public static final String FLAGSHIP_2026 = "FLAGSHIP_2026";
    public static final String S8G1_BALANCED = "S8G1_BALANCED";
    public static final String S8G1_SUPER = "S8G1_SUPER";

    public final String id;
    public final String name;

    public RuntimeProfile(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
