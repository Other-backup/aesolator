package com.winlator.cmod.contents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RemoteFeedPayloadLoaderTest {
    @Test
    public void loadNormalizedFeedAnnotatesCommunityGitHubReleasePayload() {
        String releasesJson = "[" +
                "{" +
                "\"tag_name\":\"2026-04-14\"," +
                "\"published_at\":\"2026-04-14T00:00:00Z\"," +
                "\"assets\":[" +
                "{" +
                "\"name\":\"freewine11-arm64ec.wcp\"," +
                "\"browser_download_url\":\"https://example.com/freewine11-arm64ec.wcp\"" +
                "}" +
                "]" +
                "}" +
                "]";

        RemoteFeedPayloadLoader.FeedLoadResult result = RemoteFeedPayloadLoader.loadNormalizedFeed(
                RuntimeFeedRegistry.COMMUNITY_ALEXOQOOL_BIONIC_RELEASES_URL,
                url -> new Downloader.StringResponse(url, 200, releasesJson, "", null)
        );

        assertTrue(result.payload, result.hasPayload());
        assertTrue(result.payload.contains("\"sourceFeed\":\"community-alexoqool-bionic\""));
        assertTrue(result.payload.contains("\"sourceLabel\":\"Alexoqool Bionic Releases\""));
    }

    @Test
    public void loadNightliesPayloadFallsBackToAtomOnGitHubRateLimit() {
        String atom = "<feed>" +
                "<entry>" +
                "<updated>2026-04-14T00:00:00Z</updated>" +
                "<link rel=\"alternate\" href=\"https://github.com/The412Banner/Nightlies/releases/tag/nightly-20260414-000000\" />" +
                "<content type=\"html\">Nightly notes</content>" +
                "</entry>" +
                "</feed>";
        String expandedAssetsHtml = "<li class=\"Box-row\">" +
                "<a href=\"/The412Banner/Nightlies/releases/download/nightly-20260414-000000/freewine11-arm64ec.wcp\">download</a>" +
                "<span class=\"Truncate-text text-bold\">freewine11-arm64ec.wcp</span>" +
                "<relative-time datetime=\"2026-04-14T00:00:00Z\"></relative-time>" +
                "</li>";

        RemoteFeedPayloadLoader.FeedLoadResult result = RemoteFeedPayloadLoader.loadNightliesPayload(url -> {
            if (ContentsManager.REMOTE_THE412BANNER_NIGHTLIES_RELEASES.equals(url)) {
                return new Downloader.StringResponse(url, 403, "", "{\"message\":\"API rate limit exceeded\"}", null);
            }
            if (ContentsManager.REMOTE_THE412BANNER_NIGHTLIES_RELEASES_ATOM.equals(url)) {
                return new Downloader.StringResponse(url, 200, atom, "", null);
            }
            if (url.contains("/releases/expanded_assets/nightly-20260414-000000")) {
                return new Downloader.StringResponse(url, 200, expandedAssetsHtml, "", null);
            }
            return new Downloader.StringResponse(url, 404, "", "", null);
        });

        assertTrue(result.payload, result.hasPayload());
        assertTrue(result.fallbackUsed);
        assertEquals(403, RemoteFeedPayloadLoader.loadNormalizedFeed(
                ContentsManager.REMOTE_THE412BANNER_NIGHTLIES_RELEASES,
                url -> new Downloader.StringResponse(url, 403, "", "{\"message\":\"API rate limit exceeded\"}", null)
        ).statusCode);
        assertTrue(result.payload.contains("freewine11-arm64ec.wcp"));
        assertTrue(result.payload.contains("\"sourceFeed\":\"nightlies\""));
    }
}
