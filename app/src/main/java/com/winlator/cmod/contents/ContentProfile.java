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
    public static final String MARK_CHANNEL = "channel";
    public static final String MARK_DELIVERY = "delivery";
    public static final String MARK_DISPLAY_CATEGORY = "displayCategory";
    public static final String MARK_SOURCE_REPO = "sourceRepo";
    public static final String MARK_RELEASE_TAG = "releaseTag";
    public static final String CHANNEL_STABLE = "stable";
    public static final String CHANNEL_BETA = "beta";
    public static final String CHANNEL_NIGHTLY = "nightly";
    public static final String DELIVERY_REMOTE = "remote";
    public static final String DELIVERY_EMBEDDED = "embedded";

    public enum ContentType {
        CONTENT_TYPE_WINE("Wine"),
        CONTENT_TYPE_PROTON("Proton"),
        CONTENT_TYPE_VULKAN_SDK("VulkanSDK"),
        CONTENT_TYPE_TURNIP_DRIVER("TurnipDriver"),
        CONTENT_TYPE_OPENGL_DRIVER("OpenGLDriver"),
        CONTENT_TYPE_DGVOODOO("DgVoodoo"),
        CONTENT_TYPE_DXVK("DXVK"),
        CONTENT_TYPE_VKD3D("VKD3D"),
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

            if (normalized.equals("wine")
                    || normalized.equals("wine/proton")
                    || normalized.equals("proton")
                    || normalized.equals("protonge")
                    || normalized.equals("proton-ge")
                    || normalized.equals("protonwine")
                    || normalized.equals("proton-wine")
                    || normalized.equals("proton wine")) {
                return normalized.contains("proton") ? CONTENT_TYPE_PROTON : CONTENT_TYPE_WINE;
            }

            if (normalized.equals("vulkansdk") || normalized.equals("vulkan sdk")) return CONTENT_TYPE_VULKAN_SDK;
            if (normalized.equals("turnipdriver") || normalized.equals("turnip driver") || normalized.equals("turnip")) {
                return CONTENT_TYPE_TURNIP_DRIVER;
            }
            if (normalized.equals("opengldriver") || normalized.equals("opengl driver")) return CONTENT_TYPE_OPENGL_DRIVER;
            if (normalized.equals("dgvoodoo") || normalized.equals("dgvoodoo2")) return CONTENT_TYPE_DGVOODOO;

            for (ContentType type : ContentType.values()) {
                if (type.typeName.toLowerCase(Locale.ENGLISH).equals(normalized)) return type;
            }
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
    public String channel = CHANNEL_STABLE;
    public String delivery = "";
    public String displayCategory = "";
    public String sourceRepo = "";
    public String releaseTag = "";
    public boolean locallyInstalled = false;

    public String getChannel() {
        if (channel == null || channel.trim().isEmpty()) return CHANNEL_STABLE;
        return channel.trim().toLowerCase(Locale.ENGLISH);
    }

    public String getDelivery() {
        return delivery == null ? "" : delivery.trim().toLowerCase(Locale.ENGLISH);
    }

    public String getDisplayCategory() {
        if (displayCategory != null && !displayCategory.trim().isEmpty()) return displayCategory.trim();
        if (isProtonLike()) return "Proton";
        if (type == ContentType.CONTENT_TYPE_WINE) return "Wine";
        if (type == ContentType.CONTENT_TYPE_VULKAN_SDK) return "Vulkan SDK";
        if (type == ContentType.CONTENT_TYPE_TURNIP_DRIVER) return "Turnip";
        if (type == ContentType.CONTENT_TYPE_OPENGL_DRIVER) return "OpenGL Driver";
        if (type == ContentType.CONTENT_TYPE_DGVOODOO) return "dgVoodoo";
        return type != null ? type.toString() : "";
    }

    public boolean isBetaLike() {
        String ch = getChannel();
        return CHANNEL_BETA.equals(ch) || CHANNEL_NIGHTLY.equals(ch);
    }

    public boolean isWineProtonFamily() {
        return type == ContentType.CONTENT_TYPE_WINE || type == ContentType.CONTENT_TYPE_PROTON;
    }

    public boolean isProtonLike() {
        if (type == ContentType.CONTENT_TYPE_PROTON) return true;
        String combined = ((verName != null ? verName : "") + " "
                + (desc != null ? desc : "") + " "
                + (displayCategory != null ? displayCategory : "") + " "
                + (sourceRepo != null ? sourceRepo : "") + " "
                + (releaseTag != null ? releaseTag : "") + " "
                + (remoteUrl != null ? remoteUrl : "")).toLowerCase(Locale.ENGLISH);
        return combined.contains("proton");
    }

    public boolean isWineLike() {
        return isWineProtonFamily() && !isProtonLike();
    }

    public boolean isTurnipDriverType() {
        return type == ContentType.CONTENT_TYPE_TURNIP_DRIVER;
    }

    public boolean isOpenGlDriverType() {
        return type == ContentType.CONTENT_TYPE_OPENGL_DRIVER;
    }

    public boolean isGraphicsProviderPackage() {
        return isTurnipDriverType() || isOpenGlDriverType();
    }

    public boolean isRemoteDownloadable() {
        return remoteUrl != null && !remoteUrl.trim().isEmpty();
    }

    public boolean isInstalledLocally() {
        return locallyInstalled;
    }

    public boolean sameEntry(ContentProfile other) {
        if (other == null || type == null || other.type == null) return false;
        if (verCode != other.verCode) return false;
        if (verName == null || other.verName == null) return false;
        if (type != other.type) {
            if (!(isWineProtonFamily() && other.isWineProtonFamily())) return false;
        }
        return verName.equalsIgnoreCase(other.verName);
    }

    public void mergeRemoteMetadata(ContentProfile remoteProfile) {
        if (remoteProfile == null) return;
        boolean crossFamilyRepair = isWineProtonFamily() && remoteProfile.isWineProtonFamily() && type != remoteProfile.type;
        if (!isRemoteDownloadable() && remoteProfile.isRemoteDownloadable()) remoteUrl = remoteProfile.remoteUrl;
        if ((channel == null || channel.trim().isEmpty()) && remoteProfile.channel != null) channel = remoteProfile.channel;
        if ((crossFamilyRepair || displayCategory == null || displayCategory.trim().isEmpty()) && remoteProfile.displayCategory != null) {
            displayCategory = remoteProfile.displayCategory;
        }
        if ((sourceRepo == null || sourceRepo.trim().isEmpty()) && remoteProfile.sourceRepo != null) {
            sourceRepo = remoteProfile.sourceRepo;
        }
        if ((releaseTag == null || releaseTag.trim().isEmpty()) && remoteProfile.releaseTag != null) {
            releaseTag = remoteProfile.releaseTag;
        }
        if ((crossFamilyRepair || desc == null || desc.trim().isEmpty() || desc.trim().equalsIgnoreCase(verName))
                && remoteProfile.desc != null && !remoteProfile.desc.trim().isEmpty()) {
            desc = remoteProfile.desc;
        }
    }
}
