package com.winlator.cmod.contents;

import androidx.annotation.Nullable;

public abstract class ManifestContentTypes {
    public static final String DRIVER = "driver";
    public static final String DXVK = "dxvk";
    public static final String VKD3D = "vkd3d";
    public static final String BOX64 = "box64";
    public static final String WOWBOX64 = "wowbox64";
    public static final String FEXCORE = "fexcore";
    public static final String WINE = "wine";
    public static final String PROTON = "proton";

    @Nullable
    public static ContentProfile.ContentType toContentType(String manifestType) {
        if (manifestType == null) return null;
        return switch (manifestType.trim().toLowerCase()) {
            case DXVK -> ContentProfile.ContentType.CONTENT_TYPE_DXVK;
            case VKD3D -> ContentProfile.ContentType.CONTENT_TYPE_VKD3D;
            case BOX64 -> ContentProfile.ContentType.CONTENT_TYPE_BOX64;
            case WOWBOX64 -> ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64;
            case FEXCORE -> ContentProfile.ContentType.CONTENT_TYPE_FEXCORE;
            case WINE -> ContentProfile.ContentType.CONTENT_TYPE_WINE;
            case PROTON -> ContentProfile.ContentType.CONTENT_TYPE_PROTON;
            default -> null;
        };
    }
}
