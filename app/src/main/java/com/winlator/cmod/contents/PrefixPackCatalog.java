package com.winlator.cmod.contents;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PrefixPackCatalog {
    public static final String MODE_DOWNLOAD = "download";
    public static final String MODE_MANUAL_PAGE = "manual_page";

    private PrefixPackCatalog() {}

    public static List<Entry> parse(String rawCatalog) {
        if (rawCatalog == null || rawCatalog.trim().isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<Entry> entries = new ArrayList<>();
        String[] lines = rawCatalog.split("\\r?\\n");
        for (String rawLine : lines) {
            if (rawLine == null) continue;
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = rawLine.split("\\t", -1);
            if (parts.length < 7) continue;

            String id = normalize(parts[0]);
            String fileName = normalize(parts[1]);
            String mode = normalize(parts[2]).toLowerCase(Locale.US);
            String url = normalize(parts[3]);
            String group = normalize(parts[4]);
            String source = normalize(parts[5]);
            String sourcePageUrl = "";
            String installCommand = "";
            String summary;
            String sha256 = "";

            if (parts.length >= 9) {
                sourcePageUrl = normalize(parts[6]);
                installCommand = normalize(parts[7]);
                summary = normalize(parts[8]);
                if (parts.length >= 10) sha256 = normalizeSha256(parts[9]);
            } else {
                summary = normalize(parts[6]);
            }

            if (id.isEmpty() || fileName.isEmpty() || mode.isEmpty()) continue;
            entries.add(new Entry(id, fileName, mode, url, group, source, sourcePageUrl, installCommand, summary, sha256));
        }
        return entries;
    }

    public static int countByMode(List<Entry> entries, String mode) {
        if (entries == null || entries.isEmpty() || mode == null || mode.trim().isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Entry entry : entries) {
            if (entry == null) continue;
            if (mode.equalsIgnoreCase(entry.mode)) count++;
        }
        return count;
    }

    public static Entry findById(List<Entry> entries, String id) {
        if (entries == null || entries.isEmpty() || id == null || id.trim().isEmpty()) {
            return null;
        }
        for (Entry entry : entries) {
            if (entry != null && id.equalsIgnoreCase(entry.id)) return entry;
        }
        return null;
    }

    private static String normalize(String value) {
        return value != null ? value.trim() : "";
    }

    public static String normalizeSha256(String value) {
        String normalized = normalize(value).toLowerCase(Locale.US);
        if (normalized.length() != 64) return "";
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (!hex) return "";
        }
        return normalized;
    }

    public static final class Entry {
        public final String id;
        public final String fileName;
        public final String mode;
        public final String sourceUrl;
        public final String installGroup;
        public final String sourceLabel;
        public final String sourcePageUrl;
        public final String installCommand;
        public final String summary;
        public final String sha256;

        private Entry(
                String id,
                String fileName,
                String mode,
                String sourceUrl,
                String installGroup,
                String sourceLabel,
                String sourcePageUrl,
                String installCommand,
                String summary,
                String sha256
        ) {
            this.id = id;
            this.fileName = fileName;
            this.mode = mode;
            this.sourceUrl = sourceUrl;
            this.installGroup = installGroup;
            this.sourceLabel = sourceLabel;
            this.sourcePageUrl = sourcePageUrl;
            this.installCommand = installCommand;
            this.summary = summary;
            this.sha256 = sha256;
        }

        public boolean isDownloadable() {
            return MODE_DOWNLOAD.equalsIgnoreCase(mode) && !sourceUrl.isEmpty();
        }

        public boolean hasSha256() {
            return !sha256.isEmpty();
        }

        public boolean matchesSha256(String value) {
            return hasSha256() && sha256.equals(normalizeSha256(value));
        }

        public boolean isPresentFile(File file) {
            return file != null && file.isFile() && file.length() > 0L;
        }

        public boolean isValidFile(File file) {
            if (!isPresentFile(file)) return false;
            if (!hasSha256()) return true;
            return matchesSha256(Downloader.sha256Hex(file));
        }
    }
}
