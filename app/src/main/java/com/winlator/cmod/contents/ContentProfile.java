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
    public static final String MARK_PROTON = "proton";
    public static final String MARK_WINE_BINPATH = "binPath";
    public static final String MARK_WINE_LIBPATH = "libPath";
    public static final String MARK_WINE_PREFIX_PACK = "prefixPack";
    public static final String MARK_CHANNEL = "channel";
    public static final String MARK_DELIVERY = "delivery";
    public static final String MARK_DISPLAY_CATEGORY = "displayCategory";
    public static final String MARK_SOURCE_REPO = "sourceRepo";
    public static final String MARK_SOURCE_FEED = "sourceFeed";
    public static final String MARK_SOURCE_LABEL = "sourceLabel";
    public static final String MARK_RELEASE_TAG = "releaseTag";
    public static final String MARK_ARTIFACT_NAME = "artifactName";
    public static final String MARK_PUBLISHED_AT = "publishedAt";
    public static final String MARK_RELEASE_NOTES = "releaseNotes";
    public static final String MARK_SHA256 = "sha256";
    public static final String MARK_RUNTIME_MODEL = "runtimeModel";
    public static final String MARK_VULKAN_API_MIN = "vulkanApiMin";
    public static final String MARK_VULKAN_API_MAX = "vulkanApiMax";
    public static final String CHANNEL_STABLE = "stable";
    public static final String CHANNEL_BETA = "beta";
    public static final String CHANNEL_NIGHTLY = "nightly";
    public static final String DELIVERY_REMOTE = "remote";
    public static final String DELIVERY_EMBEDDED = "embedded";
    public static final String RUNTIME_MODEL_GLIBC = "glibc";
    public static final String RUNTIME_MODEL_BIONIC = "bionic";

    public enum ContentType {
        CONTENT_TYPE_WINE("Wine"),
        CONTENT_TYPE_PROTON("Proton"),
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
    public String remoteSha256 = "";
    public String channel = CHANNEL_STABLE;
    public String delivery = "";
    public String displayCategory = "";
    public String sourceRepo = "";
    public String sourceFeed = "";
    public String sourceLabel = "";
    public String releaseTag = "";
    public String artifactName = "";
    public String publishedAt = "";
    public String releaseNotes = "";
    public String runtimeModel = "";
    public int vulkanApiMin = 0;
    public int vulkanApiMax = 0;
    private boolean locallyInstalled = false;

    public static String normalizeRuntimeModel(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        String normalized = value.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.contains(RUNTIME_MODEL_GLIBC) || normalized.contains("gamenative") || normalized.contains("ubuntufs")) {
            return RUNTIME_MODEL_GLIBC;
        }
        if (normalized.contains(RUNTIME_MODEL_BIONIC) || normalized.contains("native")) {
            return RUNTIME_MODEL_BIONIC;
        }
        return "";
    }

    public static String inferRuntimeModel(ContentType type, String... hints) {
        StringBuilder combined = new StringBuilder();
        if (hints != null) {
            for (String hint : hints) {
                if (hint == null || hint.trim().isEmpty()) continue;
                if (combined.length() > 0) combined.append(' ');
                combined.append(hint.trim().toLowerCase(Locale.ENGLISH));
            }
        }

        String normalizedHint = combined.toString();
        if (normalizedHint.contains("glibc")
                || normalizedHint.contains("gamenative")
                || normalizedHint.contains("ubuntufs")
                || normalizedHint.contains("ubuntu")) {
            return RUNTIME_MODEL_GLIBC;
        }
        if (normalizedHint.contains("bionic")
                || normalizedHint.contains("freewine")
                || normalizedHint.contains("android native")
                || normalizedHint.contains("native")) {
            return RUNTIME_MODEL_BIONIC;
        }
        if (type == ContentType.CONTENT_TYPE_PROTON) return RUNTIME_MODEL_GLIBC;
        if (type == ContentType.CONTENT_TYPE_WINE) return RUNTIME_MODEL_BIONIC;
        return "";
    }

    public static String inferRuntimeModelFromEntryName(String entryName) {
        if (entryName == null || entryName.trim().isEmpty()) return "";
        String normalized = entryName.trim();
        int firstDash = normalized.indexOf('-');
        if (firstDash <= 0) return inferRuntimeModel(null, normalized);

        ContentType type = ContentType.getTypeByName(normalized.substring(0, firstDash));
        String remainder = normalized.substring(firstDash + 1);
        int secondDash = remainder.indexOf('-');
        if (secondDash > 0) {
            String explicit = normalizeRuntimeModel(remainder.substring(0, secondDash));
            if (!explicit.isEmpty()) return explicit;
        }
        return inferRuntimeModel(type, normalized);
    }

    public String getChannel() {
        if (channel == null || channel.trim().isEmpty()) return CHANNEL_STABLE;
        return channel.trim().toLowerCase(Locale.ENGLISH);
    }

    public String getDelivery() {
        return delivery == null ? "" : delivery.trim().toLowerCase(Locale.ENGLISH);
    }

    public String getRuntimeModel() {
        String explicit = normalizeRuntimeModel(runtimeModel);
        if (!explicit.isEmpty()) return explicit;
        return inferRuntimeModel(type, verName, desc, displayCategory, sourceRepo, sourceFeed, sourceLabel, releaseTag, artifactName, remoteUrl);
    }

    public boolean isRuntimeModelCompatible(String requestedRuntimeModel) {
        String requested = normalizeRuntimeModel(requestedRuntimeModel);
        if (requested.isEmpty()) return true;
        String actual = getRuntimeModel();
        return actual.isEmpty() || requested.equals(actual);
    }

    public String getDisplayCategory() {
        if (displayCategory != null && !displayCategory.trim().isEmpty()) return displayCategory.trim();
        if (isWineProtonFamily()) {
            if (isProtonLike()) return "Proton";
            if (type == ContentType.CONTENT_TYPE_WINE) return "Wine";
        }
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

    public void setInstalledLocally(boolean locallyInstalled) {
        this.locallyInstalled = locallyInstalled;
    }

    private String buildArchitectureHintSurface() {
        StringBuilder builder = new StringBuilder();
        appendArchitectureHint(builder, verName);
        appendArchitectureHint(builder, desc);
        appendArchitectureHint(builder, displayCategory);
        appendArchitectureHint(builder, sourceRepo);
        appendArchitectureHint(builder, sourceFeed);
        appendArchitectureHint(builder, sourceLabel);
        appendArchitectureHint(builder, releaseTag);
        appendArchitectureHint(builder, artifactName);
        appendArchitectureHint(builder, remoteUrl);
        if (fileList != null) {
            for (ContentFile contentFile : fileList) {
                if (contentFile == null) continue;
                appendArchitectureHint(builder, contentFile.source);
                appendArchitectureHint(builder, contentFile.target);
            }
        }
        return builder.toString();
    }

    private void appendArchitectureHint(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (builder.length() > 0) builder.append(' ');
        builder.append(value.trim().toLowerCase(Locale.ENGLISH));
    }

    private boolean containsArchitectureToken(String surface, String... tokens) {
        if (surface == null || surface.isEmpty() || tokens == null) return false;
        for (String token : tokens) {
            if (token != null && !token.isEmpty() && surface.contains(token)) return true;
        }
        return false;
    }

    public boolean isBundleLikeArchitecture() {
        String surface = buildArchitectureHintSurface();
        if (surface.isEmpty()) return false;
        if (containsArchitectureToken(surface,
                "bundle", "unified", "all-arch", "all arch", "multi-arch", "multiarch", "universal")) {
            return true;
        }

        boolean hasArm64 = containsArchitectureToken(surface, " arm64 ", "/arm64/", "-arm64", "aarch64");
        boolean hasArm64Ec = containsArchitectureToken(surface, "arm64ec", "arm64-ec");
        boolean hasX64 = containsArchitectureToken(surface, "x86_64", "x86-64", "amd64", "/x64/", "/amd64/", "-x64");
        return (hasArm64 || hasArm64Ec) && hasX64;
    }

    public String getArchitectureTag() {
        String surface = buildArchitectureHintSurface();
        if (type == ContentType.CONTENT_TYPE_DGVOODOO) {
            String dgVoodooLane = resolveDgVoodooPackageLane(surface);
            if (!dgVoodooLane.isEmpty()) return dgVoodooLane;
        }

        if (isBundleLikeArchitecture()) return "bundle";

        if (containsArchitectureToken(surface, "arm64ec", "arm64-ec")) return "arm64ec";
        if (containsArchitectureToken(surface, "x86_64", "x86-64", "amd64", "/x64/", "/amd64/", "-x64")) return "x86_64";
        if (containsArchitectureToken(surface, " arm64 ", "/arm64/", "-arm64", "aarch64")) return "arm64";
        if (containsArchitectureToken(surface, " x86 ", "/x86/", "-x86")) return "x86";

        if (type == ContentType.CONTENT_TYPE_DXVK
                || type == ContentType.CONTENT_TYPE_VKD3D
                || type == ContentType.CONTENT_TYPE_DGVOODOO
                || type == ContentType.CONTENT_TYPE_WINE
                || type == ContentType.CONTENT_TYPE_PROTON) {
            return "x86_64";
        }
        return "generic";
    }

    private String resolveDgVoodooPackageLane(String surface) {
        if (surface == null || surface.isEmpty()) return "";
        if (containsArchitectureToken(surface,
                "dgvoodoo-arm64ec", "2.87.1-arm64ec", " arm64ec", "-arm64ec", "/arm64ec/", "arm64-ec")) {
            return "arm64ec";
        }
        if (containsArchitectureToken(surface,
                "dgvoodoo-x86_64", "dgvoodoo-x86-64", "2.87.1-x86_64", " x86_64", "-x86_64", "x86-64", "amd64")) {
            return "x86_64";
        }
        return "";
    }

    public boolean matchesArchitectureFilter(String requestedArch) {
        if (requestedArch == null || requestedArch.trim().isEmpty() || "all".equalsIgnoreCase(requestedArch)) {
            return true;
        }
        String requested = requestedArch.trim().toLowerCase(Locale.ENGLISH);
        String actual = getArchitectureTag();
        if (requested.equalsIgnoreCase(actual)) return true;
        if ("bundle".equalsIgnoreCase(actual)) {
            return "bundle".equalsIgnoreCase(requested)
                    || "arm64".equalsIgnoreCase(requested)
                    || "arm64ec".equalsIgnoreCase(requested)
                    || "x86_64".equalsIgnoreCase(requested)
                    || "x86".equalsIgnoreCase(requested);
        }
        return false;
    }

    public boolean sameEntry(ContentProfile other) {
        if (other == null || type == null || other.type == null) return false;
        if (verCode != other.verCode) return false;
        if (verName == null || other.verName == null) return false;
        if (type != other.type) return false;
        if (isWineProtonFamily()) {
            String leftRuntimeModel = getRuntimeModel();
            String rightRuntimeModel = other.getRuntimeModel();
            if (!leftRuntimeModel.isEmpty() && !rightRuntimeModel.isEmpty()
                    && !leftRuntimeModel.equalsIgnoreCase(rightRuntimeModel)) {
                return false;
            }
        }
        return verName.equalsIgnoreCase(other.verName);
    }

    public void mergeRemoteMetadata(ContentProfile remoteProfile) {
        if (remoteProfile == null) return;
        boolean crossFamilyRepair = isWineProtonFamily() && remoteProfile.isWineProtonFamily() && type != remoteProfile.type;
        if (!isRemoteDownloadable() && remoteProfile.isRemoteDownloadable()) remoteUrl = remoteProfile.remoteUrl;
        if ((remoteSha256 == null || remoteSha256.trim().isEmpty()) && remoteProfile.remoteSha256 != null) {
            remoteSha256 = remoteProfile.remoteSha256;
        }
        if ((channel == null || channel.trim().isEmpty()) && remoteProfile.channel != null) channel = remoteProfile.channel;
        if ((crossFamilyRepair || displayCategory == null || displayCategory.trim().isEmpty()) && remoteProfile.displayCategory != null) {
            displayCategory = remoteProfile.displayCategory;
        }
        if ((sourceRepo == null || sourceRepo.trim().isEmpty()) && remoteProfile.sourceRepo != null) {
            sourceRepo = remoteProfile.sourceRepo;
        }
        if ((sourceFeed == null || sourceFeed.trim().isEmpty()) && remoteProfile.sourceFeed != null) {
            sourceFeed = remoteProfile.sourceFeed;
        }
        if ((sourceLabel == null || sourceLabel.trim().isEmpty()) && remoteProfile.sourceLabel != null) {
            sourceLabel = remoteProfile.sourceLabel;
        }
        if ((releaseTag == null || releaseTag.trim().isEmpty()) && remoteProfile.releaseTag != null) {
            releaseTag = remoteProfile.releaseTag;
        }
        if ((artifactName == null || artifactName.trim().isEmpty()) && remoteProfile.artifactName != null) {
            artifactName = remoteProfile.artifactName;
        }
        if ((publishedAt == null || publishedAt.trim().isEmpty()) && remoteProfile.publishedAt != null) {
            publishedAt = remoteProfile.publishedAt;
        }
        if ((releaseNotes == null || releaseNotes.trim().isEmpty()) && remoteProfile.releaseNotes != null) {
            releaseNotes = remoteProfile.releaseNotes;
        }
        if ((runtimeModel == null || runtimeModel.trim().isEmpty()) && remoteProfile.runtimeModel != null) {
            runtimeModel = remoteProfile.runtimeModel;
        }
        if (vulkanApiMin <= 0 && remoteProfile.vulkanApiMin > 0) {
            vulkanApiMin = remoteProfile.vulkanApiMin;
        }
        if (vulkanApiMax <= 0 && remoteProfile.vulkanApiMax > 0) {
            vulkanApiMax = remoteProfile.vulkanApiMax;
        }
        if ((crossFamilyRepair || desc == null || desc.trim().isEmpty() || desc.trim().equalsIgnoreCase(verName))
                && remoteProfile.desc != null && !remoteProfile.desc.trim().isEmpty()) {
            desc = remoteProfile.desc;
        }
    }
}
