package com.winlator.cmod.contents;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RemoteProfileFeedMerger {
    private RemoteProfileFeedMerger() {}

    public static String mergePayloads(List<String> payloads) {
        Map<String, JsonObject> bestByEntry = new LinkedHashMap<>();
        if (payloads == null) return "[]";

        for (String payload : payloads) {
            if (payload == null || payload.trim().isEmpty()) continue;
            try {
                JsonElement parsed = JsonParser.parseString(payload);
                if (!parsed.isJsonArray()) continue;

                JsonArray array = parsed.getAsJsonArray();
                for (JsonElement element : array) {
                    if (element == null || !element.isJsonObject()) continue;
                    JsonObject candidate = element.getAsJsonObject();
                    String key = buildMergeEntryKey(candidate);
                    JsonObject currentBest = bestByEntry.get(key);
                    if (currentBest == null || isBetterRemoteCandidate(candidate, currentBest)) {
                        bestByEntry.put(key, candidate.deepCopy());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        JsonArray merged = new JsonArray();
        for (JsonObject selected : bestByEntry.values()) {
            merged.add(selected);
        }
        return merged.toString();
    }

    public static String classifySourceMode(String sourceFeed, String sourceRepo, String sourceLabel, String remoteUrl) {
        String joined = (
                (sourceFeed == null ? "" : sourceFeed) + " " +
                        (sourceRepo == null ? "" : sourceRepo) + " " +
                        (sourceLabel == null ? "" : sourceLabel) + " " +
                        (remoteUrl == null ? "" : remoteUrl)
        ).toLowerCase(Locale.US);

        if (joined.contains("wcp-graphics-lanes") || joined.contains("wcp graphics lanes")
                || joined.contains("wcp-runtime-lanes") || joined.contains("wcp runtime lanes")
                || joined.contains("ae.solator") || joined.contains("aesolator")) {
            return RuntimeFeedRegistry.SOURCE_MODE_ARCHIVE;
        }
        if (RuntimeFeedRegistry.looksLikeNightliesSource(joined)) {
            return RuntimeFeedRegistry.SOURCE_MODE_NIGHTLIES;
        }
        if (RuntimeFeedRegistry.looksLikeGameNativeProtonSource(joined)) {
            return RuntimeFeedRegistry.SOURCE_MODE_GAMENATIVE_PROTON;
        }
        if (RuntimeFeedRegistry.looksLikeAndreVtoProtonSource(joined)) {
            return RuntimeFeedRegistry.SOURCE_MODE_ANDREVTO_PROTON;
        }
        if (joined.contains("gamehub-components") || joined.contains("gamehub")) {
            return RuntimeFeedRegistry.SOURCE_MODE_GAMEHUB;
        }
        if (RuntimeFeedRegistry.looksLikeWcpHubSource(joined)) {
            return RuntimeFeedRegistry.SOURCE_MODE_WCPHUB;
        }
        if (RuntimeFeedRegistry.looksLikeCommunitySource(joined)) {
            return RuntimeFeedRegistry.SOURCE_MODE_COMMUNITY;
        }
        return "";
    }

    private static String buildMergeEntryKey(JsonObject object) {
        String type = optString(object, "type").trim().toLowerCase(Locale.US);
        String verName = optString(object, "verName").trim().toLowerCase(Locale.US);
        String channel = optString(object, ContentProfile.MARK_CHANNEL).trim().toLowerCase(Locale.US);
        String displayCategory = optString(object, ContentProfile.MARK_DISPLAY_CATEGORY).trim().toLowerCase(Locale.US);
        String archHint = resolveRemoteArchHint(object);
        return type + "|" + verName + "|" + channel + "|" + displayCategory + "|" + archHint;
    }

    private static boolean isBetterRemoteCandidate(JsonObject candidate, JsonObject currentBest) {
        long candidatePublishedAt = resolveRemotePublishedAtKey(candidate);
        long currentPublishedAt = resolveRemotePublishedAtKey(currentBest);
        if (candidatePublishedAt != currentPublishedAt) return candidatePublishedAt > currentPublishedAt;

        int candidateVerCode = parseRemoteVerCode(candidate);
        int currentVerCode = parseRemoteVerCode(currentBest);
        if (candidateVerCode != currentVerCode) return candidateVerCode > currentVerCode;

        int candidateSourcePriority = resolveRemoteSourcePriority(candidate);
        int currentSourcePriority = resolveRemoteSourcePriority(currentBest);
        if (candidateSourcePriority != currentSourcePriority) return candidateSourcePriority > currentSourcePriority;

        int candidateChannelPriority = resolveChannelPriority(optString(candidate, ContentProfile.MARK_CHANNEL));
        int currentChannelPriority = resolveChannelPriority(optString(currentBest, ContentProfile.MARK_CHANNEL));
        if (candidateChannelPriority != currentChannelPriority) return candidateChannelPriority > currentChannelPriority;

        int candidateFormatPriority = resolveRemotePackageFormatPriority(
                optString(candidate, "type"),
                optString(candidate, "remoteUrl")
        );
        int currentFormatPriority = resolveRemotePackageFormatPriority(
                optString(currentBest, "type"),
                optString(currentBest, "remoteUrl")
        );
        if (candidateFormatPriority != currentFormatPriority) return candidateFormatPriority > currentFormatPriority;

        String candidateUrl = optString(candidate, "remoteUrl");
        String currentUrl = optString(currentBest, "remoteUrl");
        return candidateUrl.compareToIgnoreCase(currentUrl) < 0;
    }

    private static int parseRemoteVerCode(JsonObject object) {
        JsonElement raw = optValue(object, "verCode");
        if (raw == null || raw.isJsonNull()) return 0;
        if (raw.isJsonPrimitive()) {
            JsonPrimitive primitive = raw.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                try {
                    return primitive.getAsInt();
                } catch (Exception ignored) {
                }
            }
            if (primitive.isString()) {
                try {
                    return Integer.parseInt(primitive.getAsString().trim());
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }

    private static long resolveRemotePublishedAtKey(JsonObject object) {
        return parsePublishedAtKey(optString(object, ContentProfile.MARK_PUBLISHED_AT));
    }

    private static long parsePublishedAtKey(String value) {
        if (value == null) return 0L;
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0L;
        if (digits.length() > 14) digits = digits.substring(0, 14);
        try {
            return Long.parseLong(digits);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static int resolveRemoteSourcePriority(JsonObject object) {
        String sourceMode = classifySourceMode(
                optString(object, ContentProfile.MARK_SOURCE_FEED),
                optString(object, ContentProfile.MARK_SOURCE_REPO),
                optString(object, ContentProfile.MARK_SOURCE_LABEL),
                optString(object, "remoteUrl")
        );
        if (RuntimeFeedRegistry.SOURCE_MODE_ARCHIVE.equals(sourceMode)) return 300;
        if (RuntimeFeedRegistry.SOURCE_MODE_NIGHTLIES.equals(sourceMode)) return 275;
        if (RuntimeFeedRegistry.SOURCE_MODE_GAMEHUB.equals(sourceMode)) {
            String sourceRepo = optString(object, ContentProfile.MARK_SOURCE_REPO).trim().toLowerCase(Locale.US);
            if (sourceRepo.contains("releases")) return 255;
            if (sourceRepo.contains("raw")) return 245;
            return 250;
        }
        if (RuntimeFeedRegistry.SOURCE_MODE_ANDREVTO_PROTON.equals(sourceMode)) return 242;
        if (RuntimeFeedRegistry.SOURCE_MODE_GAMENATIVE_PROTON.equals(sourceMode)) return 244;
        if (RuntimeFeedRegistry.SOURCE_MODE_COMMUNITY.equals(sourceMode)) {
            String joined = (
                    optString(object, ContentProfile.MARK_SOURCE_REPO) + " " +
                            optString(object, ContentProfile.MARK_SOURCE_LABEL) + " " +
                            optString(object, "remoteUrl")
            ).toLowerCase(Locale.US);
            if (joined.contains("xnick417x/winlator-bionic-nightly-wcp")) return 240;
            if (joined.contains("alexoqool/winlator-bionic-build")) return 238;
            if (joined.contains("waim908/wine-winlator")) return 234;
            if (joined.contains("moze30/winlator-wcp")) return 230;
            if (joined.contains("ludashi")) return 120;
            return 225;
        }
        if (RuntimeFeedRegistry.SOURCE_MODE_WCPHUB.equals(sourceMode)) return 200;
        String joined = (
                optString(object, ContentProfile.MARK_SOURCE_REPO) + " " +
                        optString(object, "remoteUrl")
        ).toLowerCase(Locale.US);
        if (joined.contains("stevenmxz") || joined.contains("winlator-contents")) return 100;
        return 50;
    }

    private static int resolveChannelPriority(String channel) {
        String normalized = channel == null ? "" : channel.trim().toLowerCase(Locale.US);
        if (ContentProfile.CHANNEL_STABLE.equals(normalized)) return 30;
        if (ContentProfile.CHANNEL_BETA.equals(normalized)) return 20;
        if (ContentProfile.CHANNEL_NIGHTLY.equals(normalized)) return 10;
        return 0;
    }

    private static int resolveRemotePackageFormatPriority(String type, String remoteUrl) {
        String normalizedType = type == null ? "" : type.trim().toLowerCase(Locale.US);
        String normalizedUrl = remoteUrl == null ? "" : remoteUrl.trim().toLowerCase(Locale.US);

        if ("wine".equals(normalizedType) || "proton".equals(normalizedType)) {
            if (normalizedUrl.endsWith(".wcp") || normalizedUrl.endsWith(".wcp.xz") || normalizedUrl.endsWith(".wcp.zst")) return 60;
            if (normalizedUrl.endsWith(".zip")) return 50;
            return 40;
        }
        if ("dxvk".equals(normalizedType) || "vkd3d".equals(normalizedType)) {
            if (normalizedUrl.endsWith(".wcp")) return 40;
            if (normalizedUrl.endsWith(".zip")) return 30;
        }
        return 10;
    }

    private static String resolveRemoteArchHint(JsonObject object) {
        String normalizedType = optString(object, "type").trim().toLowerCase(Locale.US);
        String combined = (
                optString(object, "verName") + " "
                        + optString(object, "description") + " "
                        + optString(object, "remoteUrl") + " "
                        + optString(object, ContentProfile.MARK_RELEASE_TAG)
        ).toLowerCase(Locale.US);
        if (("dxvk".equals(normalizedType) || "vkd3d".equals(normalizedType)) && combined.contains("native")) return "x86_64";
        if (combined.contains("arm64ec") || combined.contains("arm64-ec")) return "arm64ec";
        if (combined.contains("x86_64") || combined.contains("x86-64") || combined.contains("amd64")) return "x86_64";
        if (combined.contains("arm64")) return "arm64";
        return "generic";
    }

    private static JsonElement optValue(JsonObject object, String key) {
        if (object == null || key == null || key.trim().isEmpty() || !object.has(key)) return null;
        return object.get(key);
    }

    private static String optString(JsonObject object, String key) {
        JsonElement value = optValue(object, key);
        if (value == null || value.isJsonNull()) return "";
        if (!value.isJsonPrimitive()) return value.toString();
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (primitive.isString()) return primitive.getAsString();
        if (primitive.isBoolean()) return primitive.getAsBoolean() ? "true" : "false";
        if (primitive.isNumber()) return primitive.getAsString();
        return "";
    }
}
