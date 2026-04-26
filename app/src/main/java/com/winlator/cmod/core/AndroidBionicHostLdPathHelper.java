package com.winlator.cmod.core;

import java.util.LinkedHashSet;
import java.util.Set;

public final class AndroidBionicHostLdPathHelper {
    private static final String[] SYSTEM_TAIL_PATHS = new String[] {
            "/system/lib64",
            "/apex/com.android.runtime/lib64",
            "/apex/com.android.art/lib64"
    };

    private AndroidBionicHostLdPathHelper() {
    }

    public static String buildDirectGuestLdLibraryPath(
            String currentLdLibraryPath,
            String guestLibDir,
            String guestLib64Dir,
            String hostLibDir
    ) {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        appendFilteredSegments(paths, currentLdLibraryPath, guestLibDir, guestLib64Dir);
        appendPath(paths, hostLibDir);
        for (String systemPath : SYSTEM_TAIL_PATHS) {
            appendPath(paths, systemPath);
        }
        return String.join(":", paths);
    }

    private static void appendFilteredSegments(
            Set<String> paths,
            String ldLibraryPath,
            String guestLibDir,
            String guestLib64Dir
    ) {
        if (ldLibraryPath == null || ldLibraryPath.trim().isEmpty()) return;
        String normalizedGuestLibDir = normalizePath(guestLibDir);
        String normalizedGuestLib64Dir = normalizePath(guestLib64Dir);

        String[] segments = ldLibraryPath.split(":");
        for (String segment : segments) {
            String normalizedSegment = normalizePath(segment);
            if (normalizedSegment.isEmpty()) continue;
            if (normalizedSegment.equals(normalizedGuestLibDir)) continue;
            if (normalizedSegment.equals(normalizedGuestLib64Dir)) continue;
            if (isSystemTailPath(normalizedSegment)) continue;
            paths.add(normalizedSegment);
        }
    }

    private static boolean isSystemTailPath(String path) {
        if (path == null || path.isEmpty()) return false;
        for (String candidate : SYSTEM_TAIL_PATHS) {
            if (path.equals(candidate)) return true;
        }
        return false;
    }

    private static void appendPath(Set<String> paths, String path) {
        String normalized = normalizePath(path);
        if (!normalized.isEmpty()) {
            paths.add(normalized);
        }
    }

    private static String normalizePath(String path) {
        if (path == null) return "";
        String normalized = path.trim();
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
