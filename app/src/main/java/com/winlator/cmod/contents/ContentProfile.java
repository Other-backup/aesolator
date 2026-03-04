package com.winlator.cmod.contents;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Locale;

public class ContentProfile {
    public static final String MARK_TYPE = "type";
    public static final String MARK_VERSION_NAME = "versionName";
    public static final String MARK_VERSION_CODE = "versionCode";
    public static final String MARK_DESC = "description";
    public static final String MARK_FILE_LIST = "files";
    public static final String MARK_FILE_SOURCE = "source";
    public static final String MARK_FILE_TARGET = "target";
    public static final String MARK_WINE = "wine";
    public static final String MARK_WINE_BINPATH = "binPath";
    public static final String MARK_WINE_LIBPATH = "libPath";
    public static final String MARK_WINE_PREFIX_PACK = "prefixPack";

    public enum ContentType {
        CONTENT_TYPE_WINE("Wine"),
        CONTENT_TYPE_PROTON("Proton"),
        CONTENT_TYPE_DXVK("DXVK"),
        CONTENT_TYPE_VKD3D("VKD3D"),
        CONTENT_TYPE_VULKAN_SDK("VulkanSDK"),
        CONTENT_TYPE_TURNIP_DRIVER("TurnipDriver"),
        CONTENT_TYPE_OPENGL_DRIVER("OpenGLDriver"),
        CONTENT_TYPE_DGVOODOO("DgVoodoo"),
        CONTENT_TYPE_BOX64("Box64"),
        CONTENT_TYPE_WOWBOX64("WOWBox64"),
        CONTENT_TYPE_FEXCORE("FEXCore");

        final String typeName;

        ContentType(String typeName) {
            this.typeName = typeName;
        }

        @NonNull
        @Override
        public String toString() {
            return typeName;
        }

        public static ContentType getTypeByName(String name) {
            if (name == null) return null;
            String normalized = name.trim().toLowerCase(Locale.ENGLISH);

            // Keep Wine/Proton compatible, but allow explicit proton markers.
            if (normalized.equals("wine") ||
                    normalized.equals("wine/proton") ||
                    normalized.equals("proton") ||
                    normalized.equals("protonge") ||
                    normalized.equals("proton-ge") ||
                    normalized.equals("protonwine") ||
                    normalized.equals("proton-wine") ||
                    normalized.equals("proton wine")) {
                return normalized.contains("proton") ? CONTENT_TYPE_PROTON : CONTENT_TYPE_WINE;
            }

            if (normalized.equals("vulkansdk") || normalized.equals("vulkan sdk")) return CONTENT_TYPE_VULKAN_SDK;
            if (normalized.equals("turnipdriver") || normalized.equals("turnip driver") || normalized.equals("turnip")) return CONTENT_TYPE_TURNIP_DRIVER;
            if (normalized.equals("opengldriver") || normalized.equals("opengl driver")) return CONTENT_TYPE_OPENGL_DRIVER;
            if (normalized.equals("dgvoodoo") || normalized.equals("dgvoodoo2")) return CONTENT_TYPE_DGVOODOO;

            for (ContentType type : ContentType.values())
                if (type.typeName.toLowerCase(Locale.ENGLISH).equals(normalized))
                    return type;
            return null;
        }
    }

    public static class ContentFile {
        public String source;
        public String target;
    }

    public ContentType type;
    public String verName;
    public int verCode;
    public String desc;
    public List<ContentFile> fileList;
    public String wineLibPath;
    public String wineBinPath;
    public String winePrefixPack;
    public String remoteUrl;
}
