package com.winlator.cmod.contents;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ContentProfileIdentity {
    private ContentProfileIdentity() {
    }

    static boolean isRemoteProfileIdentityMismatch(@Nullable ContentProfile profile, @Nullable ContentProfile remoteHint) {
        if (profile == null || remoteHint == null) return false;
        boolean runtimeFamily = profile.isWineProtonFamily() && remoteHint.isWineProtonFamily();
        if (profile.type != null && remoteHint.type != null && profile.type != remoteHint.type && !runtimeFamily) {
            return true;
        }

        String actualVersion = normalizeIdentityToken(profile.verName);
        String expectedVersion = normalizeIdentityToken(remoteHint.verName);
        if (actualVersion.isEmpty() || expectedVersion.isEmpty()) return false;
        if (runtimeFamily) {
            return !runtimeVersionMatches(profile, remoteHint);
        }
        return !actualVersion.equalsIgnoreCase(expectedVersion);
    }

    static boolean areEquivalentProfiles(@Nullable ContentProfile left, @Nullable ContentProfile right) {
        if (left == null || right == null || left.type == null || right.type == null) return false;
        if (left.sameEntry(right)) return true;
        if (!left.isWineProtonFamily() || !right.isWineProtonFamily()) return false;
        if (!runtimeModelMatches(left, right)) return false;
        if (!runtimeArchMatches(left, right)) return false;
        return !isRemoteProfileIdentityMismatch(left, right);
    }

    static boolean areRuntimePayloadCompatibleProfiles(@Nullable ContentProfile left, @Nullable ContentProfile right) {
        if (left == null || right == null || left.type == null || right.type == null) return false;
        if (!left.isWineProtonFamily() || !right.isWineProtonFamily()) return false;
        if (!runtimeFamilyKindMatches(left, right)) return false;
        if (!runtimeModelMatches(left, right)) return false;
        if (!runtimeArchMatches(left, right)) return false;
        return runtimeVersionMatches(left, right);
    }

    static boolean isRuntimeAliasEquivalent(@Nullable ContentProfile left, @Nullable ContentProfile right) {
        if (left == null || right == null) return false;
        if (!left.isWineProtonFamily() || !right.isWineProtonFamily()) return false;
        if (!areEquivalentProfiles(left, right)) return false;
        return !normalizeIdentityToken(left.verName).equalsIgnoreCase(normalizeIdentityToken(right.verName));
    }

    static String describeProfile(@Nullable ContentProfile profile) {
        if (profile == null) return "type=-,ver=-,aliases=[],arch=-,model=-";
        List<String> aliases = new ArrayList<>(buildRuntimeAliases(profile));
        return "type=" + (profile.type == null ? "-" : profile.type)
                + ",ver=" + normalizeIdentityToken(profile.verName)
                + ",aliases=" + aliases
                + ",arch=" + resolveRuntimeArchHint(profile)
                + ",model=" + normalizedIdentityToken(profile.getRuntimeModel());
    }

    private static String normalizeIdentityToken(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean runtimeVersionMatches(@Nullable ContentProfile actualProfile, @Nullable ContentProfile expectedProfile) {
        Set<String> actualAliases = buildRuntimeAliases(actualProfile);
        Set<String> expectedAliases = buildRuntimeAliases(expectedProfile);
        if (actualAliases.isEmpty() || expectedAliases.isEmpty()) {
            String actualVersion = actualProfile == null ? "" : actualProfile.verName;
            String expectedVersion = expectedProfile == null ? "" : expectedProfile.verName;
            String normalizedActual = normalizeRuntimeVersionToken(actualVersion);
            String normalizedExpected = normalizeRuntimeVersionToken(expectedVersion);
            if (normalizedActual.isEmpty() || normalizedExpected.isEmpty()) {
                return normalizedIdentityToken(actualVersion).equalsIgnoreCase(normalizedIdentityToken(expectedVersion));
            }
            if (normalizedActual.equalsIgnoreCase(normalizedExpected)) return true;
            return isRuntimeBuildVariant(normalizedActual, normalizedExpected)
                    || isRuntimeBuildVariant(normalizedExpected, normalizedActual);
        }
        for (String actualAlias : actualAliases) {
            if (expectedAliases.contains(actualAlias)) return true;
            for (String expectedAlias : expectedAliases) {
                if (isRuntimeBuildVariant(actualAlias, expectedAlias)
                        || isRuntimeBuildVariant(expectedAlias, actualAlias)) {
                    return true;
                }
            }
        }
        return rollingBleedingEdgeAliasMatches(actualProfile, expectedProfile)
                || rollingBleedingEdgeAliasMatches(expectedProfile, actualProfile);
    }

    private static boolean isRuntimeBuildVariant(String longerVersion, String shorterVersion) {
        if (longerVersion.equalsIgnoreCase(shorterVersion)) return true;
        if (!longerVersion.regionMatches(true, 0, shorterVersion, 0, shorterVersion.length())) return false;
        if (longerVersion.length() <= shorterVersion.length()) return false;
        return longerVersion.charAt(shorterVersion.length()) == '-';
    }

    private static String normalizeRuntimeVersionToken(String value) {
        String normalized = normalizeIdentityToken(value).toLowerCase();
        normalized = normalized.replaceAll("(?i)\\.(wcp\\.xz|wcp\\.zst|wcp|zip|tar\\.xz|tar\\.zst|tar|txz|tzst)$", "");
        normalized = normalized.replaceAll("^(proton-wine-|wine-proton-|freewine-|proton-|wine-)+", "");
        normalized = normalized.replace("x86-64", "x86_64");
        normalized = normalized.replace("amd64", "x86_64");
        normalized = normalized.replace("aarch64", "arm64");
        return normalizedIdentityToken(normalized);
    }

    private static Set<String> buildRuntimeAliases(@Nullable ContentProfile profile) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        if (profile == null) return aliases;
        addRuntimeAliases(aliases, profile.verName);
        addRuntimeAliases(aliases, profile.artifactName);
        return aliases;
    }

    private static void addRuntimeAliases(Set<String> aliases, @Nullable String value) {
        String normalized = normalizeRuntimeVersionToken(value);
        if (normalized.isEmpty()) return;
        aliases.add(normalized);

        String strippedBuild = stripTrailingBuildStamp(normalized);
        if (!strippedBuild.isEmpty()) aliases.add(strippedBuild);

        String stableAlias = collapseStableRuntimePatchLane(strippedBuild);
        if (!stableAlias.isEmpty()) aliases.add(stableAlias);
    }

    private static String stripTrailingBuildStamp(String value) {
        String normalized = normalizedIdentityToken(value);
        normalized = normalized.replaceAll("-(20\\d{6}(?:[._-]\\d{3,6})?)$", "");
        normalized = normalized.replaceAll("-(\\d{8}[._]\\d{3,6})$", "");
        return normalizedIdentityToken(normalized);
    }

    private static String collapseStableRuntimePatchLane(String value) {
        String normalized = normalizedIdentityToken(value);
        normalized = normalized.replaceAll(
                "^([0-9]+(?:\\.[0-9]+)*)(?:-[0-9]+)?-(arm64ec|arm64|x86_64|x86)$",
                "$1-$2"
        );
        return normalizedIdentityToken(normalized);
    }

    private static boolean rollingBleedingEdgeAliasMatches(@Nullable ContentProfile concreteProfile,
                                                           @Nullable ContentProfile aliasProfile) {
        String concreteVersion = concreteProfile == null ? "" : normalizeRuntimeVersionToken(concreteProfile.verName);
        if (!concreteVersion.matches("^[0-9]+(?:\\.[0-9]+)*\\.99-(arm64ec|arm64|x86_64|x86)(?:-.+)?$")) return false;
        String aliasSurface = buildAliasSurface(aliasProfile);
        if (!aliasSurface.contains("bleeding-edge")) return false;
        String concreteArch = trailingArchToken(concreteVersion);
        String aliasArch = resolveRuntimeArchHint(aliasProfile);
        return concreteArch.isEmpty() || aliasArch.isEmpty() || concreteArch.equals(aliasArch);
    }

    private static String buildAliasSurface(@Nullable ContentProfile profile) {
        if (profile == null) return "";
        return (((profile.verName == null ? "" : profile.verName) + " "
                + (profile.artifactName == null ? "" : profile.artifactName) + " "
                + (profile.releaseTag == null ? "" : profile.releaseTag) + " "
                + (profile.remoteUrl == null ? "" : profile.remoteUrl))).toLowerCase();
    }

    private static boolean runtimeModelMatches(ContentProfile left, ContentProfile right) {
        String leftModel = normalizedIdentityToken(left.getRuntimeModel());
        String rightModel = normalizedIdentityToken(right.getRuntimeModel());
        return leftModel.isEmpty() || rightModel.isEmpty() || leftModel.equals(rightModel);
    }

    private static boolean runtimeFamilyKindMatches(ContentProfile left, ContentProfile right) {
        if (left.type == right.type) return true;
        return left.isProtonLike() == right.isProtonLike();
    }

    private static boolean runtimeArchMatches(ContentProfile left, ContentProfile right) {
        String leftArch = resolveRuntimeArchHint(left);
        String rightArch = resolveRuntimeArchHint(right);
        return leftArch.isEmpty() || rightArch.isEmpty() || leftArch.equals(rightArch);
    }

    private static String resolveRuntimeArchHint(@Nullable ContentProfile profile) {
        String surface = buildAliasSurface(profile);
        if (surface.contains("arm64ec") || surface.contains("arm64-ec")) return "arm64ec";
        if (surface.contains("x86_64") || surface.contains("x86-64") || surface.contains("amd64")) return "x86_64";
        if (surface.contains("arm64") || surface.contains("aarch64")) return "arm64";
        if (surface.contains("x86")) return "x86";
        return "";
    }

    private static String trailingArchToken(String normalizedVersion) {
        String normalized = normalizedIdentityToken(normalizedVersion);
        if (normalized.matches(".*-arm64ec(?:-.+)?$")) return "arm64ec";
        if (normalized.matches(".*-x86_64(?:-.+)?$")) return "x86_64";
        if (normalized.matches(".*-arm64(?:-.+)?$")) return "arm64";
        if (normalized.matches(".*-x86(?:-.+)?$")) return "x86";
        return "";
    }

    private static String normalizedIdentityToken(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9._-]+", "-");
        normalized = normalized.replaceAll("-{2,}", "-");
        while (normalized.startsWith("-")) normalized = normalized.substring(1);
        while (normalized.endsWith("-")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }
}
