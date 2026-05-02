package com.winlator.cmod.contents;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class RuntimeLaunchPolicy {
    private static final int GLIBC_PROMOTION_SCORE_GAP = 20;

    private RuntimeLaunchPolicy() {}

    public static int computePreferredLaunchRuntimeScore(@NonNull ContentProfile profile,
                                                         boolean installedUsable,
                                                         boolean installedPresent) {
        int score = 0;
        if (installedUsable) score += 40;
        else if (installedPresent) score += 10;
        else if (profile.isRemoteDownloadable()) score += 30;
        if (profile.isProtonLike()) score += 20;
        String archTag = profile.getArchitectureTag();
        if ("arm64ec".equalsIgnoreCase(archTag)) score += 12;
        else if ("bundle".equalsIgnoreCase(archTag)) score += 10;
        else if ("x86_64".equalsIgnoreCase(archTag)) score += 6;
        else if ("generic".equalsIgnoreCase(archTag)) score += 2;
        if (ContentProfile.CHANNEL_NIGHTLY.equals(profile.getChannel())) score += 1;
        return score;
    }

    public static boolean shouldPromoteGlibcRuntime(@Nullable ContentProfile current,
                                                    boolean currentUsable,
                                                    boolean currentPresent,
                                                    @Nullable ContentProfile candidate,
                                                    boolean candidateUsable,
                                                    boolean candidatePresent,
                                                    String requestedEntry,
                                                    String runtimeModel) {
        if (!ContentProfile.RUNTIME_MODEL_GLIBC.equals(ContentProfile.normalizeRuntimeModel(runtimeModel))) {
            return false;
        }
        if (!isLaunchCandidateReady(candidate, candidateUsable, candidatePresent)) {
            return false;
        }
        if (current == null) {
            return true;
        }
        if (!current.isWineProtonFamily() || !candidate.isWineProtonFamily()) {
            return false;
        }
        if (sameRuntimeEntry(current, candidate)) {
            return false;
        }

        int currentScore = computePreferredLaunchRuntimeScore(current, currentUsable, currentPresent);
        int candidateScore = computePreferredLaunchRuntimeScore(candidate, candidateUsable, candidatePresent);
        String currentSurface = buildProfileSurface(current, requestedEntry);
        if (isKnownLegacyGlibcLaunchRisk(current, requestedEntry) || isLegacyWineMajor(currentSurface)) {
            return candidateScore >= currentScore;
        }
        return candidateScore >= currentScore + GLIBC_PROMOTION_SCORE_GAP;
    }

    public static boolean shouldPersistPromotedGlibcRuntime(String requestedEntry,
                                                           String resolvedEntry,
                                                           String runtimeModel) {
        String normalizedRequested = trimLower(requestedEntry);
        String normalizedResolved = trimLower(resolvedEntry);
        if (normalizedResolved.isEmpty()
                || normalizedRequested.isEmpty()
                || normalizedRequested.equals(normalizedResolved)) {
            return false;
        }
        if (!ContentProfile.RUNTIME_MODEL_GLIBC.equals(ContentProfile.normalizeRuntimeModel(runtimeModel))) {
            return false;
        }
        return isKnownLegacyGlibcLaunchRisk(null, normalizedRequested)
                || isLegacyWineMajor(normalizedRequested)
                || normalizedRequested.startsWith("wine-glibc-")
                || normalizedRequested.startsWith("wine-10.")
                || normalizedRequested.startsWith("wine-9.");
    }

    public static boolean isKnownLegacyGlibcLaunchRisk(@Nullable ContentProfile profile, String requestedEntry) {
        String surface = buildProfileSurface(profile, requestedEntry);
        if (surface.isEmpty()) return false;
        return surface.contains("moze30/winlator-wcp")
                || surface.contains("zmod")
                || surface.contains("wine-10.10")
                || surface.contains("10.10-arm64")
                || surface.contains("wine-9.2")
                || surface.contains("9.2-custom")
                || surface.contains("winlator-glibc7.1")
                || surface.contains("wine-glibc-10.")
                || surface.contains("wine-glibc-9.");
    }

    public static String resolvePromotionReason(@Nullable ContentProfile current,
                                                boolean currentUsable,
                                                boolean currentPresent,
                                                @Nullable ContentProfile candidate,
                                                boolean candidateUsable,
                                                boolean candidatePresent,
                                                String requestedEntry,
                                                String runtimeModel) {
        if (!ContentProfile.RUNTIME_MODEL_GLIBC.equals(ContentProfile.normalizeRuntimeModel(runtimeModel))) {
            return "";
        }
        if (!isLaunchCandidateReady(candidate, candidateUsable, candidatePresent)) {
            return "";
        }
        if (current == null) {
            return "missing_requested_glibc_runtime";
        }
        if (sameRuntimeEntry(current, candidate)) {
            return "";
        }
        if (isKnownLegacyGlibcLaunchRisk(current, requestedEntry)) {
            return "legacy_glibc_runtime_risk";
        }
        if (isLegacyWineMajor(buildProfileSurface(current, requestedEntry))) {
            return "legacy_glibc_wine_major";
        }
        int currentScore = computePreferredLaunchRuntimeScore(current, currentUsable, currentPresent);
        int candidateScore = computePreferredLaunchRuntimeScore(candidate, candidateUsable, candidatePresent);
        if (candidateScore >= currentScore + GLIBC_PROMOTION_SCORE_GAP) {
            return "higher_scored_glibc_runtime";
        }
        return "";
    }

    private static boolean isLaunchCandidateReady(@Nullable ContentProfile candidate,
                                                  boolean candidateUsable,
                                                  boolean candidatePresent) {
        return candidate != null
                && candidate.isWineProtonFamily()
                && (candidateUsable || candidate.isRemoteDownloadable());
    }

    private static boolean sameRuntimeEntry(@NonNull ContentProfile left, @NonNull ContentProfile right) {
        String leftEntry = buildEntryName(left);
        String rightEntry = buildEntryName(right);
        return !leftEntry.isEmpty() && leftEntry.equalsIgnoreCase(rightEntry);
    }

    private static String buildEntryName(@NonNull ContentProfile profile) {
        if (profile.type == null || profile.verName == null || profile.verName.trim().isEmpty()) {
            return "";
        }
        String runtimeModel = profile.isWineProtonFamily() ? profile.getRuntimeModel() : "";
        if (!runtimeModel.isEmpty()) {
            return profile.type.toString() + '-' + runtimeModel + '-' + profile.verName + '-' + profile.verCode;
        }
        return profile.type.toString() + '-' + profile.verName + '-' + profile.verCode;
    }

    private static String buildProfileSurface(@Nullable ContentProfile profile, String requestedEntry) {
        StringBuilder builder = new StringBuilder();
        appendSurface(builder, requestedEntry);
        if (profile != null) {
            appendSurface(builder, profile.verName);
            appendSurface(builder, profile.desc);
            appendSurface(builder, profile.displayCategory);
            appendSurface(builder, profile.sourceRepo);
            appendSurface(builder, profile.sourceFeed);
            appendSurface(builder, profile.sourceLabel);
            appendSurface(builder, profile.releaseTag);
            appendSurface(builder, profile.artifactName);
            appendSurface(builder, profile.remoteUrl);
        }
        return builder.toString().toLowerCase(Locale.ENGLISH);
    }

    private static void appendSurface(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (builder.length() > 0) builder.append(' ');
        builder.append(value.trim());
    }

    private static boolean isLegacyWineMajor(String value) {
        String normalized = trimLower(value);
        return normalized.startsWith("wine-10.")
                || normalized.startsWith("wine-9.")
                || normalized.contains(" wine-10.")
                || normalized.contains(" wine-9.");
    }

    private static String trimLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }
}
