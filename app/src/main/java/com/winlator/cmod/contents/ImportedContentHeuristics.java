package com.winlator.cmod.contents;

import androidx.annotation.Nullable;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.WineUtils;

import java.io.File;
import java.util.Locale;

final class ImportedContentHeuristics {
    private static final String[] ARCHIVE_SUFFIXES = {
            ".wcp.xz", ".wcp.zst", ".wcp", ".tar.xz", ".tar.zst", ".tzst", ".txz", ".zip", ".tar"
    };
    private ImportedContentHeuristics() {
    }

    @Nullable
    static ContentProfile.ContentType inferContentType(File rootDir,
                                                       @Nullable ContentProfile parsedProfile,
                                                       @Nullable ContentProfile remoteHint,
                                                       @Nullable String importDisplayName) {
        if (parsedProfile != null && parsedProfile.type != null) return parsedProfile.type;
        if (remoteHint != null && remoteHint.type != null) return remoteHint.type;
        if (rootDir == null || !rootDir.isDirectory()) return null;

        if (WineUtils.hasRuntimePayload(rootDir)) {
            return surfaceLooksLikeProton(parsedProfile, remoteHint, importDisplayName)
                    ? ContentProfile.ContentType.CONTENT_TYPE_PROTON
                    : ContentProfile.ContentType.CONTENT_TYPE_WINE;
        }
        if (findRelativeFile(rootDir, "wowbox64.dll") != null) return ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64;
        if (findRelativeFile(rootDir, "libarm64ecfex.dll") != null
                && findRelativeFile(rootDir, "libwow64fex.dll") != null) {
            return ContentProfile.ContentType.CONTENT_TYPE_FEXCORE;
        }
        if (findRelativeFile(rootDir, "box64") != null) return ContentProfile.ContentType.CONTENT_TYPE_BOX64;
        if (looksLikeDgVoodooPayload(rootDir)) {
            return ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO;
        }
        if (findRelativeFile(rootDir, "d3d11.dll") != null
                && findRelativeFile(rootDir, "dxgi.dll") != null) {
            return ContentProfile.ContentType.CONTENT_TYPE_DXVK;
        }
        if (findRelativeFile(rootDir, "d3d12.dll") != null
                || findRelativeFile(rootDir, "d3d12core.dll") != null) {
            return ContentProfile.ContentType.CONTENT_TYPE_VKD3D;
        }
        return null;
    }

    static boolean hasRecoverablePayload(File rootDir,
                                         @Nullable ContentProfile parsedProfile,
                                         @Nullable ContentProfile remoteHint,
                                         @Nullable String importDisplayName) {
        return inferContentType(rootDir, parsedProfile, remoteHint, importDisplayName) != null;
    }

    static String inferRuntimeModel(File rootDir,
                                    @Nullable ContentProfile parsedProfile,
                                    @Nullable ContentProfile remoteHint,
                                    @Nullable String importDisplayName) {
        return RuntimePayloadClassifier.classify(rootDir, parsedProfile, remoteHint, importDisplayName).runtimeModel;
    }

    static String describeRuntimePayload(File rootDir,
                                         @Nullable ContentProfile parsedProfile,
                                         @Nullable ContentProfile remoteHint,
                                         @Nullable String importDisplayName) {
        return RuntimePayloadClassifier.describe(rootDir, parsedProfile, remoteHint, importDisplayName);
    }

    static String deriveVersionName(@Nullable String importDisplayName,
                                    @Nullable ContentProfile.ContentType type,
                                    @Nullable String fallbackName) {
        String normalized = stripArchiveSuffix(firstNonBlank(importDisplayName, fallbackName));
        if (normalized.isEmpty()) return "";
        String lower = normalized.toLowerCase(Locale.US);
        if (type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
            lower = stripLeadingRuntimePrefix(lower, "proton-wine-");
            lower = stripLeadingRuntimePrefix(lower, "wine-proton-");
            lower = stripLeadingRuntimePrefix(lower, "protonwine-");
            lower = stripLeadingRuntimePrefix(lower, "proton-");
            return lower;
        }
        if (type == ContentProfile.ContentType.CONTENT_TYPE_WINE) {
            lower = stripLeadingRuntimePrefix(lower, "freewine-");
            lower = stripLeadingRuntimePrefix(lower, "wine-");
            return lower;
        }
        return lower;
    }

    private static String stripLeadingRuntimePrefix(String value, String prefix) {
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static String stripArchiveSuffix(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.US);
        for (String suffix : ARCHIVE_SUFFIXES) {
            if (normalized.endsWith(suffix)) {
                return normalized.substring(0, normalized.length() - suffix.length());
            }
        }
        return normalized;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private static boolean surfaceLooksLikeProton(@Nullable ContentProfile parsedProfile,
                                                  @Nullable ContentProfile remoteHint,
                                                  @Nullable String importDisplayName) {
        String surface = (
                firstNonBlank(importDisplayName, "") + " "
                        + firstNonBlank(parsedProfile != null ? parsedProfile.verName : "", "") + " "
                        + firstNonBlank(parsedProfile != null ? parsedProfile.desc : "", "") + " "
                        + firstNonBlank(remoteHint != null ? remoteHint.verName : "", "") + " "
                        + firstNonBlank(remoteHint != null ? remoteHint.desc : "", "") + " "
                        + firstNonBlank(remoteHint != null ? remoteHint.artifactName : "", "")
        ).toLowerCase(Locale.US);
        return surface.contains("proton");
    }

    private static boolean looksLikeDgVoodooPayload(File rootDir) {
        if (findRelativeFile(rootDir, "d3d8_dgvoodoo.dll") != null
                || findRelativeFile(rootDir, "d3d9_dgvoodoo.dll") != null
                || findRelativeFile(rootDir, "ddraw_dgvoodoo.dll") != null) {
            return true;
        }
        boolean hasDgVoodooMarker = findRelativeFile(rootDir, "dgvoodoo.conf") != null
                || findRelativeFile(rootDir, "dgvoodoocpl.exe") != null;
        if (!hasDgVoodooMarker) return false;
        return findRelativeFile(rootDir, "ddraw.dll") != null
                || findRelativeFile(rootDir, "d3d8.dll") != null
                || findRelativeFile(rootDir, "d3d9.dll") != null
                || findRelativeFile(rootDir, "glide.dll") != null
                || findRelativeFile(rootDir, "glide2x.dll") != null
                || findRelativeFile(rootDir, "glide3x.dll") != null;
    }

    @Nullable
    private static String findRelativeFile(File rootDir, String fileName) {
        if (rootDir == null || fileName == null || fileName.trim().isEmpty()) return null;
        File candidate = new File(rootDir, fileName);
        if (candidate.isFile()) return fileName;
        return findRelativeFileRecursive(rootDir, rootDir, fileName.trim().toLowerCase(Locale.US));
    }

    @Nullable
    private static String findRelativeFileRecursive(File rootDir, File current, String normalizedName) {
        File[] children = current.listFiles();
        if (children == null) return null;
        for (File child : children) {
            if (child.isDirectory()) {
                String nested = findRelativeFileRecursive(rootDir, child, normalizedName);
                if (nested != null) return nested;
                continue;
            }
            if (child.getName().trim().toLowerCase(Locale.US).equals(normalizedName)) {
                return FileUtils.toRelativePath(rootDir.getAbsolutePath(), child.getAbsolutePath()).replace('\\', '/');
            }
        }
        return null;
    }
}
