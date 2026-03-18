package com.winlator.cmod.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SocClassifier {
    public enum Tier {
        ADRENO_7XX,
        ADRENO_6XX,
        ADRENO_LEGACY,
        XCLIPSE_RDNA_MOBILE,
        MALI_G7XX_OR_NEWER,
        MALI_LEGACY,
        UNKNOWN
    }

    private static final Pattern ADRENO_PATTERN =
            Pattern.compile("adreno[^0-9]*(\\d{3,4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern MALI_PATTERN =
            Pattern.compile("mali[-\\s]*g?(\\d{2,4})", Pattern.CASE_INSENSITIVE);

    private SocClassifier() {}

    public static Tier detect(
            String renderer,
            String socModel,
            String hardware,
            String boardPlatform,
            String productBoard
    ) {
        String normalizedRenderer = normalize(renderer);
        String hardwareBlob = normalize(socModel) + " "
                + normalize(hardware) + " "
                + normalize(boardPlatform) + " "
                + normalize(productBoard);

        if (normalizedRenderer.contains("adreno")) {
            Integer generation = extractGeneration(ADRENO_PATTERN, normalizedRenderer);
            if (generation != null) {
                if (generation >= 700) return Tier.ADRENO_7XX;
                if (generation >= 600) return Tier.ADRENO_6XX;
            }
            Tier inferredQualcommTier = inferQualcommTier(hardwareBlob);
            return inferredQualcommTier != Tier.UNKNOWN ? inferredQualcommTier : Tier.ADRENO_LEGACY;
        }

        Tier inferredQualcommTier = inferQualcommTier(hardwareBlob);
        if (inferredQualcommTier != Tier.UNKNOWN) {
            return inferredQualcommTier;
        }

        if (normalizedRenderer.contains("xclipse") || hardwareBlob.contains("xclipse")) {
            return Tier.XCLIPSE_RDNA_MOBILE;
        }

        if (normalizedRenderer.contains("mali") || hardwareBlob.contains("mali")) {
            Integer generation = extractGeneration(MALI_PATTERN, normalizedRenderer + " " + hardwareBlob);
            if (generation != null && generation >= 700) {
                return Tier.MALI_G7XX_OR_NEWER;
            }
            return Tier.MALI_LEGACY;
        }

        return Tier.UNKNOWN;
    }

    private static Tier inferQualcommTier(String normalizedBlob) {
        if (normalizedBlob.isEmpty()) return Tier.UNKNOWN;

        if (containsAny(
                normalizedBlob,
                "sm8475",
                "sm8450",
                "sm8550",
                "sm8635",
                "sm8650",
                "sm8750",
                "taro",
                "kalama",
                "pineapple",
                "sun"
        )) {
            return Tier.ADRENO_7XX;
        }

        if (containsAny(
                normalizedBlob,
                "sm8350",
                "sm8250",
                "sm8150",
                "sm7325",
                "sm7250",
                "sm7225",
                "sm7150",
                "sm7125",
                "lahaina",
                "kona",
                "waipio",
                "shima",
                "lito",
                "atoll",
                "bengal"
        )) {
            return Tier.ADRENO_6XX;
        }

        return Tier.UNKNOWN;
    }

    private static Integer extractGeneration(Pattern pattern, String value) {
        if (value == null || value.trim().isEmpty()) return null;
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) return null;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }
}
