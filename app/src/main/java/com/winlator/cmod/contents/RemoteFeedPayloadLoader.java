package com.winlator.cmod.contents;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RemoteFeedPayloadLoader {
    interface ResponseFetcher {
        Downloader.StringResponse fetch(String url);
    }

    public static final class FeedLoadResult {
        public final String requestedUrl;
        public final String payload;
        public final int statusCode;
        public final String failureClass;
        public final boolean fallbackUsed;

        private FeedLoadResult(String requestedUrl, String payload, int statusCode, String failureClass, boolean fallbackUsed) {
            this.requestedUrl = requestedUrl == null ? "" : requestedUrl;
            this.payload = payload == null ? "" : payload;
            this.statusCode = statusCode;
            this.failureClass = failureClass == null ? "" : failureClass;
            this.fallbackUsed = fallbackUsed;
        }

        public boolean hasPayload() {
            return !isEmptyPayload(payload);
        }

        public boolean isRateLimited() {
            return "github_rate_limited".equals(failureClass);
        }

        private FeedLoadResult asFallback() {
            return new FeedLoadResult(requestedUrl, payload, statusCode, failureClass, true);
        }
    }

    private RemoteFeedPayloadLoader() {
    }

    public static FeedLoadResult loadNormalizedFeed(String feedUrl) {
        return loadNormalizedFeed(feedUrl, Downloader::downloadStringResponse);
    }

    public static FeedLoadResult loadNormalizedFeed(RuntimeFeedRegistry.FeedSpec feed) {
        return loadNormalizedFeed(feed == null ? "" : feed.url, Downloader::downloadStringResponse);
    }

    static FeedLoadResult loadNormalizedFeed(String feedUrl, ResponseFetcher fetcher) {
        String normalizedUrl = feedUrl == null ? "" : feedUrl.trim();
        if (normalizedUrl.isEmpty()) {
            return new FeedLoadResult("", "", 0, "missing_url", false);
        }

        Downloader.StringResponse response = fetch(fetcher, normalizedUrl);
        if (!response.isSuccessful()) {
            return new FeedLoadResult(normalizedUrl, "", response.statusCode, response.classifyFailure(), false);
        }

        String normalizedPayload = normalizePayload(response.body, normalizedUrl);
        if (isEmptyPayload(normalizedPayload)) {
            return new FeedLoadResult(normalizedUrl, "", response.statusCode, "empty_payload", false);
        }
        return new FeedLoadResult(normalizedUrl, normalizedPayload, response.statusCode, "", false);
    }

    public static FeedLoadResult loadNightliesPayload() {
        return loadNightliesPayload(Downloader::downloadStringResponse);
    }

    static FeedLoadResult loadNightliesPayload(ResponseFetcher fetcher) {
        FeedLoadResult releasesResult = loadNormalizedFeed(ContentsManager.REMOTE_THE412BANNER_NIGHTLIES_RELEASES, fetcher);
        if (releasesResult.hasPayload()) return releasesResult;

        FeedLoadResult atomFallbackResult = loadNightliesAtomFallbackPayload(fetcher);
        if (atomFallbackResult.hasPayload()) return atomFallbackResult.asFallback();
        if (releasesResult.statusCode != 0 || !releasesResult.failureClass.isEmpty()) return releasesResult;
        return atomFallbackResult;
    }

    public static FeedLoadResult loadNightliesAtomFallbackPayload() {
        return loadNightliesAtomFallbackPayload(Downloader::downloadStringResponse);
    }

    static FeedLoadResult loadNightliesAtomFallbackPayload(ResponseFetcher fetcher) {
        Downloader.StringResponse atomResponse = fetch(fetcher, ContentsManager.REMOTE_THE412BANNER_NIGHTLIES_RELEASES_ATOM);
        if (!atomResponse.isSuccessful()) {
            return new FeedLoadResult(
                    ContentsManager.REMOTE_THE412BANNER_NIGHTLIES_RELEASES_ATOM,
                    "",
                    atomResponse.statusCode,
                    atomResponse.classifyFailure(),
                    false
            );
        }

        List<GamehubFeedNormalizer.ReleaseFeedEntry> entries =
                GamehubFeedNormalizer.parseGitHubReleaseAtom(atomResponse.body, "The412Banner/Nightlies");
        ArrayList<String> payloads = new ArrayList<>();
        int consumed = 0;
        for (GamehubFeedNormalizer.ReleaseFeedEntry entry : entries) {
            if (entry == null || !entry.isValid()) continue;
            String expandedAssetsUrl = buildNightliesExpandedAssetsUrl(entry.tag);
            Downloader.StringResponse htmlResponse = fetch(fetcher, expandedAssetsUrl);
            if (!htmlResponse.isSuccessful()) continue;

            String normalizedPayload = GamehubFeedNormalizer.normalizeExpandedAssetsHtml(
                    htmlResponse.body,
                    entry,
                    GamehubFeedNormalizer.NIGHTLIES_FEED_ID,
                    GamehubFeedNormalizer.NIGHTLIES_LABEL,
                    GamehubFeedNormalizer.NIGHTLIES_REPO_RELEASES,
                    "The412Banner nightly package"
            );
            if (isEmptyPayload(normalizedPayload)) continue;
            payloads.add(normalizedPayload);
            consumed++;
            if (consumed >= 12) break;
        }

        String mergedPayload = RemoteProfileFeedMerger.mergePayloads(payloads);
        if (isEmptyPayload(mergedPayload)) {
            return new FeedLoadResult(
                    ContentsManager.REMOTE_THE412BANNER_NIGHTLIES_RELEASES_ATOM,
                    "",
                    atomResponse.statusCode,
                    "empty_payload",
                    false
            );
        }
        return new FeedLoadResult(ContentsManager.REMOTE_THE412BANNER_NIGHTLIES_RELEASES_ATOM, mergedPayload, atomResponse.statusCode, "", false);
    }

    public static String normalizePayload(String payload, String feedUrl) {
        String normalizedFeedUrl = feedUrl == null ? "" : feedUrl.trim().toLowerCase(Locale.US);
        String normalizedPayload = payload == null ? "" : payload;
        RuntimeFeedRegistry.FeedSpec registryFeed = RuntimeFeedRegistry.findByUrl(feedUrl);
        if (registryFeed != null && registryFeed.format == RuntimeFeedRegistry.FeedFormat.GITHUB_RELEASES) {
            normalizedPayload = GamehubFeedNormalizer.normalizeGitHubReleaseFeed(
                    normalizedPayload,
                    registryFeed.sourceFeedId,
                    registryFeed.sourceLabel,
                    registryFeed.sourceRepo,
                    registryFeed.descriptionPrefix
            );
        } else if (normalizedFeedUrl.contains("gamehub-components/main/sp_winemu_all_components12.xml")) {
            normalizedPayload = GamehubFeedNormalizer.normalizeComponentXml(normalizedPayload);
        } else if (normalizedFeedUrl.contains("api.github.com/repos/the412banner/nightlies/releases")) {
            normalizedPayload = GamehubFeedNormalizer.normalizeNightliesReleaseFeed(normalizedPayload);
        } else if (normalizedFeedUrl.contains("api.github.com/repos/the412banner/gamehub-components/releases")) {
            normalizedPayload = GamehubFeedNormalizer.normalizeReleaseFeed(normalizedPayload);
        }
        return injectFeedSourceMetadata(normalizedPayload, feedUrl);
    }

    public static boolean isEmptyPayload(String payload) {
        if (payload == null) return true;
        String normalized = payload.trim();
        if (normalized.isEmpty() || "[]".equals(normalized)) return true;
        try {
            JsonElement parsed = JsonParser.parseString(normalized);
            return parsed.isJsonArray() && parsed.getAsJsonArray().size() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Downloader.StringResponse fetch(ResponseFetcher fetcher, String url) {
        if (fetcher == null) return new Downloader.StringResponse(url, 0, "", "", null);
        Downloader.StringResponse response = fetcher.fetch(url);
        return response == null ? new Downloader.StringResponse(url, 0, "", "", null) : response;
    }

    private static String buildNightliesExpandedAssetsUrl(String releaseTag) {
        String normalizedTag = releaseTag == null ? "" : releaseTag.trim();
        return "https://github.com/The412Banner/Nightlies/releases/expanded_assets/" + normalizedTag;
    }

    private static String injectFeedSourceMetadata(String json, String feedUrl) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonArray()) return json;
            JsonArray array = parsed.getAsJsonArray();
            String sourceMode = deriveFeedSourceMode(feedUrl);
            String sourceFeedId = deriveFeedSourceId(feedUrl, sourceMode);
            String sourceLabel = deriveFeedSourceLabel(feedUrl, sourceMode);
            String releaseTag = deriveFeedReleaseTag(feedUrl);
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                if (optString(object, ContentProfile.MARK_SOURCE_REPO).trim().isEmpty()) {
                    object.addProperty(ContentProfile.MARK_SOURCE_REPO, sourceLabel);
                }
                if (optString(object, ContentProfile.MARK_SOURCE_FEED).trim().isEmpty()) {
                    object.addProperty(ContentProfile.MARK_SOURCE_FEED, sourceFeedId);
                }
                if (optString(object, ContentProfile.MARK_SOURCE_LABEL).trim().isEmpty()) {
                    object.addProperty(ContentProfile.MARK_SOURCE_LABEL, sourceLabel);
                }
                if (optString(object, ContentProfile.MARK_RELEASE_TAG).trim().isEmpty() && !releaseTag.isEmpty()) {
                    object.addProperty(ContentProfile.MARK_RELEASE_TAG, releaseTag);
                }
            }
            return array.toString();
        } catch (Exception ignored) {
            return json;
        }
    }

    private static String optString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) return "";
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return "";
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String deriveFeedSourceMode(String feedUrl) {
        RuntimeFeedRegistry.FeedSpec registryFeed = RuntimeFeedRegistry.findByUrl(feedUrl);
        if (registryFeed != null) return registryFeed.sourceMode;

        String lower = feedUrl == null ? "" : feedUrl.trim().toLowerCase(Locale.US);
        if (lower.contains("kosoymiki/wcp-graphics-lanes")
                || lower.contains("kosoymiki/wcp-runtime-lanes")
                || lower.contains("ae.solator")
                || lower.contains("aesolator")) {
            return RuntimeFeedRegistry.SOURCE_MODE_ARCHIVE;
        }
        if (RuntimeFeedRegistry.looksLikeNightliesSource(lower)) return RuntimeFeedRegistry.SOURCE_MODE_NIGHTLIES;
        if (RuntimeFeedRegistry.looksLikeGameNativeProtonSource(lower)) return RuntimeFeedRegistry.SOURCE_MODE_GAMENATIVE_PROTON;
        if (RuntimeFeedRegistry.looksLikeAndreVtoProtonSource(lower)) return RuntimeFeedRegistry.SOURCE_MODE_ANDREVTO_PROTON;
        if (lower.contains("the412banner/gamehub-components")
                || lower.contains("api.github.com/repos/the412banner/gamehub-components/releases")
                || lower.contains("gamehub-components")) {
            return RuntimeFeedRegistry.SOURCE_MODE_GAMEHUB;
        }
        if (RuntimeFeedRegistry.looksLikeWcpHubSource(lower)) return RuntimeFeedRegistry.SOURCE_MODE_WCPHUB;
        if (RuntimeFeedRegistry.looksLikeCommunitySource(lower)) return RuntimeFeedRegistry.SOURCE_MODE_COMMUNITY;
        return "remote";
    }

    private static String deriveFeedSourceId(String feedUrl, String sourceMode) {
        if (sourceMode == null || sourceMode.trim().isEmpty()) return "remote";
        if (!"remote".equals(sourceMode)) return sourceMode;
        return deriveFeedSourceFeedIdFromUrl(feedUrl);
    }

    private static String deriveFeedSourceFeedIdFromUrl(String feedUrl) {
        try {
            URI uri = new URI(feedUrl);
            String host = uri.getHost() == null ? "" : uri.getHost().trim().toLowerCase(Locale.US);
            if (!host.isEmpty()) return host;
        } catch (Exception ignored) {
        }
        return "remote-feed";
    }

    private static String deriveFeedSourceLabel(String feedUrl, String sourceMode) {
        RuntimeFeedRegistry.FeedSpec registryFeed = RuntimeFeedRegistry.findByUrl(feedUrl);
        if (registryFeed != null) return registryFeed.sourceLabel;
        if (RuntimeFeedRegistry.SOURCE_MODE_ARCHIVE.equals(sourceMode)) return "Ae.solator Archive";
        if (RuntimeFeedRegistry.SOURCE_MODE_COMMUNITY.equals(sourceMode)) return "Community";
        if (RuntimeFeedRegistry.SOURCE_MODE_NIGHTLIES.equals(sourceMode)) return "Nightlies";
        if (RuntimeFeedRegistry.SOURCE_MODE_GAMEHUB.equals(sourceMode)) return "GameHub";
        if (RuntimeFeedRegistry.SOURCE_MODE_WCPHUB.equals(sourceMode)) return "WCPHub";

        try {
            URI uri = new URI(feedUrl);
            String host = uri.getHost() == null ? "" : uri.getHost().trim().toLowerCase(Locale.US);
            String path = uri.getPath() == null ? "" : uri.getPath().trim();
            if ("raw.githubusercontent.com".equals(host) && !path.isEmpty()) {
                String[] parts = path.split("/");
                if (parts.length >= 3 && !parts[1].isEmpty() && !parts[2].isEmpty()) {
                    return parts[1] + "/" + parts[2];
                }
            }
            if (!host.isEmpty()) return host;
        } catch (Exception ignored) {
        }
        return "remote-feed";
    }

    private static String deriveFeedReleaseTag(String feedUrl) {
        String lower = feedUrl == null ? "" : feedUrl.toLowerCase(Locale.US);
        if (lower.contains("nightly")) return ContentProfile.CHANNEL_NIGHTLY;
        if (lower.contains("beta") || lower.contains("rc")) return ContentProfile.CHANNEL_BETA;
        return ContentProfile.CHANNEL_STABLE;
    }
}
