package com.winlator.cmod.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VulkanVersionInfo {
    public static final String LATEST_SDK_VERSION = "1.4.341.1";
    public static final String LATEST_API_LANE = "1.4";
    public static final String DEFAULT_REQUESTED_LANE = "auto";

    private static final Pattern SEMVER_LOOSE =
            Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final List<String> SELECTABLE_LANES = Arrays.asList(
            DEFAULT_REQUESTED_LANE,
            "1.1",
            "1.2",
            "1.3",
            LATEST_API_LANE
    );

    private VulkanVersionInfo() {
    }

    @NonNull
    public static List<String> getSelectableLanes() {
        return new ArrayList<>(SELECTABLE_LANES);
    }

    @NonNull
    public static String normalizeRequestedLane(@Nullable String rawValue) {
        String trimmed = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty() || trimmed.startsWith(DEFAULT_REQUESTED_LANE)) {
            return DEFAULT_REQUESTED_LANE;
        }

        Matcher matcher = SEMVER_LOOSE.matcher(trimmed);
        String major = null;
        String minor = null;
        while (matcher.find()) {
            major = matcher.group(1);
            minor = matcher.group(2);
        }

        if (major == null || minor == null) {
            return DEFAULT_REQUESTED_LANE;
        }

        String lane = Integer.parseInt(major) + "." + Integer.parseInt(minor);
        return SELECTABLE_LANES.contains(lane) ? lane : DEFAULT_REQUESTED_LANE;
    }

    @NonNull
    public static Resolution resolve(@Nullable String requestedLane, @Nullable String detectedVersion) {
        String normalizedLane = normalizeRequestedLane(requestedLane);
        Version sdkVersion = Version.parse(LATEST_SDK_VERSION);
        Version detected = Version.parse(detectedVersion);
        Version requested = DEFAULT_REQUESTED_LANE.equals(normalizedLane)
                ? sdkVersion
                : Version.forLane(normalizedLane, normalizedLane.equals(LATEST_API_LANE) ? sdkVersion.patch : 0);

        Version effective = requested;
        String reason = DEFAULT_REQUESTED_LANE.equals(normalizedLane) ? "auto_latest_sdk" : "explicit_lane";

        if (detected.isValid()) {
            int laneCmp = detected.compareLane(requested);
            if (laneCmp < 0) {
                effective = detected;
                reason = DEFAULT_REQUESTED_LANE.equals(normalizedLane)
                        ? "auto_driver_cap"
                        : "explicit_driver_cap";
            }
            else if (laneCmp == 0 && detected.compareTo(requested) < 0) {
                effective = detected;
                reason = DEFAULT_REQUESTED_LANE.equals(normalizedLane)
                        ? "auto_driver_patch_cap"
                        : "explicit_driver_patch_cap";
            }
        }
        else {
            reason = DEFAULT_REQUESTED_LANE.equals(normalizedLane)
                    ? "no_driver_detection_auto_sdk"
                    : "no_driver_detection_explicit_lane";
        }

        return new Resolution(
                LATEST_SDK_VERSION,
                normalizedLane,
                detected.isValid() ? detected.toString() : "",
                effective.toString(),
                effective.major + "." + effective.minor,
                reason
        );
    }

    public static final class Resolution {
        public final String sdkLatest;
        public final String requestedLane;
        public final String detectedVersion;
        public final String effectiveVersion;
        public final String effectiveLane;
        public final String reason;

        private Resolution(
                String sdkLatest,
                String requestedLane,
                String detectedVersion,
                String effectiveVersion,
                String effectiveLane,
                String reason
        ) {
            this.sdkLatest = sdkLatest;
            this.requestedLane = requestedLane;
            this.detectedVersion = detectedVersion;
            this.effectiveVersion = effectiveVersion;
            this.effectiveLane = effectiveLane;
            this.reason = reason;
        }
    }

    private static final class Version {
        final int major;
        final int minor;
        final int patch;

        private Version(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        static Version parse(@Nullable String rawValue) {
            if (rawValue == null) {
                return new Version(0, 0, 0);
            }

            Matcher matcher = SEMVER_LOOSE.matcher(rawValue);
            String major = null;
            String minor = null;
            String patch = null;
            while (matcher.find()) {
                major = matcher.group(1);
                minor = matcher.group(2);
                patch = matcher.group(3);
            }

            if (major == null || minor == null) {
                return new Version(0, 0, 0);
            }

            return new Version(
                    safeParseInt(major),
                    safeParseInt(minor),
                    safeParseInt(patch)
            );
        }

        static Version forLane(@NonNull String lane, int patch) {
            String[] parts = lane.split("\\.");
            return new Version(safeParseInt(parts[0]), safeParseInt(parts[1]), patch);
        }

        boolean isValid() {
            return major > 0;
        }

        int compareLane(@NonNull Version other) {
            if (major != other.major) return major - other.major;
            return minor - other.minor;
        }

        int compareTo(@NonNull Version other) {
            int laneCmp = compareLane(other);
            if (laneCmp != 0) return laneCmp;
            return patch - other.patch;
        }

        @NonNull
        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }

        private static int safeParseInt(@Nullable String rawValue) {
            if (rawValue == null || rawValue.trim().isEmpty()) {
                return 0;
            }
            try {
                return Integer.parseInt(rawValue.trim());
            }
            catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }
}
