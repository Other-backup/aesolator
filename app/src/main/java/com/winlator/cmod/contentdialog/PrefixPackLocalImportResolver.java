package com.winlator.cmod.contentdialog;

import com.winlator.cmod.contents.PrefixPackCatalog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class PrefixPackLocalImportResolver {
    private static final String[] XNA_REDIST_DIRS = {
            "_CommonRedist/xnafx",
            "_CommonRedist/XNA_40",
            "redist",
            "Redist"
    };
    private static final int XNA_SCAN_MAX_DEPTH = 5;

    private PrefixPackLocalImportResolver() {
    }

    static Map<String, File> resolveXnaPayloads(List<PrefixPackCatalog.Entry> entries, String installPath, String aDrivePath) {
        if (entries == null || entries.isEmpty()) return Collections.emptyMap();
        List<File> searchDirs = collectXnaSearchDirs(collectGameRoots(installPath, aDrivePath));
        if (searchDirs.isEmpty()) return Collections.emptyMap();

        LinkedHashMap<String, File> matches = new LinkedHashMap<>();
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry == null || entry.fileName == null || entry.fileName.trim().isEmpty()) continue;
            File match = findFile(searchDirs, entry.fileName, XNA_SCAN_MAX_DEPTH);
            if (match != null) {
                matches.put(normalizeFileKey(entry.fileName), match);
            }
        }
        return matches.isEmpty() ? Collections.emptyMap() : matches;
    }

    static List<File> collectGameRoots(String installPath, String aDrivePath) {
        ArrayList<File> roots = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        addUniqueRoot(roots, seen, installPath);
        addUniqueRoot(roots, seen, aDrivePath);
        return roots;
    }

    static List<File> collectXnaSearchDirs(List<File> roots) {
        if (roots == null || roots.isEmpty()) return Collections.emptyList();
        ArrayList<File> searchDirs = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (File root : roots) {
            if (root == null || !root.isDirectory()) continue;
            for (String relativeDir : XNA_REDIST_DIRS) {
                File candidate = new File(root, relativeDir);
                if (!candidate.isDirectory()) continue;
                String normalized = normalizePathKey(candidate);
                if (seen.add(normalized)) {
                    searchDirs.add(candidate);
                }
            }
        }
        return searchDirs;
    }

    static String normalizeFileKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    private static void addUniqueRoot(List<File> roots, LinkedHashSet<String> seen, String path) {
        File normalized = normalizeRoot(path);
        if (normalized == null) return;
        String key = normalizePathKey(normalized);
        if (seen.add(key)) {
            roots.add(normalized);
        }
    }

    private static File normalizeRoot(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        File candidate = new File(path.trim());
        if (!candidate.exists()) return null;
        if (candidate.isDirectory()) return candidate;
        File parent = candidate.getParentFile();
        return parent != null && parent.isDirectory() ? parent : null;
    }

    private static File findFile(List<File> searchDirs, String expectedName, int maxDepth) {
        String normalizedName = normalizeFileKey(expectedName);
        if (normalizedName.isEmpty()) return null;
        for (File searchDir : searchDirs) {
            File match = scanForFile(searchDir, normalizedName, maxDepth);
            if (match != null) return match;
        }
        return null;
    }

    private static File scanForFile(File directory, String normalizedName, int depthRemaining) {
        if (directory == null || !directory.isDirectory() || depthRemaining < 0) return null;
        File[] children = directory.listFiles();
        if (children == null || children.length == 0) return null;

        for (File child : children) {
            if (child != null && child.isFile() && normalizedName.equals(normalizeFileKey(child.getName()))) {
                return child;
            }
        }
        if (depthRemaining == 0) return null;

        for (File child : children) {
            if (child == null || !child.isDirectory()) continue;
            File nested = scanForFile(child, normalizedName, depthRemaining - 1);
            if (nested != null) return nested;
        }
        return null;
    }

    private static String normalizePathKey(File file) {
        return file.getAbsolutePath().toLowerCase(Locale.US);
    }
}
