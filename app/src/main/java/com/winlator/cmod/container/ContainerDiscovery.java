package com.winlator.cmod.container;

import com.winlator.cmod.xenvironment.ImageFs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class ContainerDiscovery {
    private ContainerDiscovery() {}

    public static ParseResult parseContainerDirName(String dirName) {
        if (dirName == null || dirName.trim().isEmpty()) {
            return ParseResult.invalid("empty_name");
        }

        String prefix = ImageFs.USER + "-";
        if (!dirName.startsWith(prefix)) {
            return ParseResult.invalid("prefix_mismatch");
        }

        String suffix = dirName.substring(prefix.length()).trim();
        if (suffix.isEmpty()) {
            return ParseResult.invalid("missing_numeric_suffix");
        }

        try {
            int id = Integer.parseInt(suffix);
            if (id <= 0) {
                return ParseResult.invalid("non_positive_id");
            }
            return ParseResult.valid(id);
        }
        catch (NumberFormatException e) {
            return ParseResult.invalid("invalid_numeric_suffix");
        }
    }

    public static final class ParseResult {
        public final boolean valid;
        public final int id;
        public final String reason;

        private ParseResult(boolean valid, int id, String reason) {
            this.valid = valid;
            this.id = id;
            this.reason = reason == null ? "" : reason;
        }

        public static ParseResult valid(int id) {
            return new ParseResult(true, id, "");
        }

        public static ParseResult invalid(String reason) {
            return new ParseResult(false, 0, reason);
        }
    }

    public static final class LoadReport {
        public int scannedDirectories = 0;
        public int ignoredDirectories = 0;
        public int duplicateIds = 0;
        public int missingConfig = 0;
        public int validContainers = 0;
        public int invalidConfig = 0;
        public int parseExceptions = 0;

        private final List<String> warnings = new ArrayList<>();

        public void addWarning(String warning) {
            if (warning == null || warning.trim().isEmpty()) return;
            if (warnings.size() < 256) warnings.add(warning.trim());
        }

        public String toSummaryString() {
            return "scanned=" + scannedDirectories
                    + ",valid=" + validContainers
                    + ",ignored=" + ignoredDirectories
                    + ",duplicate=" + duplicateIds
                    + ",missing_config=" + missingConfig
                    + ",invalid_config=" + invalidConfig
                    + ",exceptions=" + parseExceptions
                    + ",warnings=" + warnings.size();
        }

        public JSONArray warningsJson() {
            JSONArray array = new JSONArray();
            for (String warning : warnings) array.put(warning);
            return array;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("scanned_directories", scannedDirectories);
                obj.put("ignored_directories", ignoredDirectories);
                obj.put("duplicate_ids", duplicateIds);
                obj.put("missing_config", missingConfig);
                obj.put("valid_containers", validContainers);
                obj.put("invalid_config", invalidConfig);
                obj.put("parse_exceptions", parseExceptions);
                obj.put("warnings", warningsJson());
                obj.put("summary", toSummaryString());
            }
            catch (JSONException ignored) {}
            return obj;
        }
    }
}
