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

    @Test
    public void andreVtoReleasePayloadSplitsProtonTypeAndWineTypeRuntimeModels() {
        String releasesJson = "[" +
                "{" +
                "\"tag_name\":\"build-20260427-1-sdk35\"," +
                "\"target_commitish\":\"proton_11.0\"," +
                "\"published_at\":\"2026-04-27T16:36:13Z\"," +
                "\"assets\":[" +
                "{" +
                "\"name\":\"proton-11.0-1-x86_64.wcp\"," +
                "\"browser_download_url\":\"https://github.com/AndreVto/proton-wine/releases/download/build-20260427-1-sdk35/proton-11.0-1-x86_64.wcp\"" +
                "}," +
                "{" +
                "\"name\":\"proton-wine-11.0-1-x86_64.wcp.xz\"," +
                "\"digest\":\"sha256:c6a2b2bccb65db42ba39700ac1a3c124102732c4113592c4c22900273d68309a\"," +
                "\"browser_download_url\":\"https://github.com/AndreVto/proton-wine/releases/download/build-20260427-1-sdk35/proton-wine-11.0-1-x86_64.wcp.xz\"" +
                "}" +
                "]" +
                "}" +
                "]";

        RemoteFeedPayloadLoader.FeedLoadResult result = RemoteFeedPayloadLoader.loadNormalizedFeed(
                RuntimeFeedRegistry.ANDREVTO_PROTON_RELEASES_URL,
                url -> new Downloader.StringResponse(url, 200, releasesJson, "", null)
        );

        assertTrue(result.payload, result.hasPayload());
        assertTrue(result.payload.contains("\"type\":\"Proton\""));
        assertTrue(result.payload.contains("proton-11.0-1-x86_64.wcp"));
        assertTrue(result.payload.contains("\"runtimeModel\":\"bionic\""));
        assertTrue(result.payload.contains("proton-wine-11.0-1-x86_64.wcp.xz"));
        assertTrue(result.payload.contains("\"runtimeModel\":\"glibc\""));
        assertTrue(result.payload.contains("\"sourceFeed\":\"andrevto-proton11\""));
        assertTrue(result.payload.contains("\"sha256\":\"sha256:c6a2b2bccb65db42ba39700ac1a3c124102732c4113592c4c22900273d68309a\""));
    }

    @Test
    public void gamenativeProtonWineReleasePayloadSplitsBionicAndGlibcPackages() {
        String releasesJson = "[" +
                "{" +
                "\"tag_name\":\"build-20260430-1-sdk35\"," +
                "\"published_at\":\"2026-04-30T12:18:33Z\"," +
                "\"assets\":[" +
                "{" +
                "\"name\":\"proton-10.0-4-arm64ec.wcp\"," +
                "\"browser_download_url\":\"https://github.com/GameNative/proton-wine/releases/download/build-20260430-1-sdk35/proton-10.0-4-arm64ec.wcp\"" +
                "}," +
                "{" +
                "\"name\":\"proton-wine-10.0-4-x86_64.wcp.xz\"," +
                "\"browser_download_url\":\"https://github.com/GameNative/proton-wine/releases/download/build-20260430-1-sdk35/proton-wine-10.0-4-x86_64.wcp.xz\"" +
                "}" +
                "]" +
                "}" +
                "]";

        RemoteFeedPayloadLoader.FeedLoadResult result = RemoteFeedPayloadLoader.loadNormalizedFeed(
                RuntimeFeedRegistry.GAMENATIVE_PROTON_RELEASES_URL,
                url -> new Downloader.StringResponse(url, 200, releasesJson, "", null)
        );

        assertTrue(result.payload, result.hasPayload());
        assertTrue(result.payload.contains("\"type\":\"Proton\""));
        assertTrue(result.payload.contains("proton-10.0-4-arm64ec.wcp"));
        assertTrue(result.payload.contains("proton-wine-10.0-4-x86_64.wcp.xz"));
        assertTrue(result.payload.contains("\"runtimeModel\":\"bionic\""));
        assertTrue(result.payload.contains("\"runtimeModel\":\"glibc\""));
        assertTrue(result.payload.contains("\"sourceFeed\":\"gamenative-proton-wine\""));
        assertTrue(result.payload.contains("\"sourceRepo\":\"GameNative/proton-wine Releases\""));
    }

    @Test
    public void xnickBionicNightlyPayloadClassifiesTagOwnedComponents() {
        String releasesJson = "[" +
                "{" +
                "\"tag_name\":\"dxvk-nightly-ec9111c0\"," +
                "\"name\":\"DXVK Nightly ec9111c0\"," +
                "\"published_at\":\"2026-05-01T04:50:50Z\"," +
                "\"assets\":[{" +
                "\"name\":\"2.7.1-gplasync-ec9111c0.wcp\"," +
                "\"browser_download_url\":\"https://github.com/Xnick417x/Winlator-Bionic-Nightly-wcp/releases/download/dxvk-nightly-ec9111c0/2.7.1-gplasync-ec9111c0.wcp\"" +
                "}]" +
                "}," +
                "{" +
                "\"tag_name\":\"vk3dk-arm64ec-nightly-497357c0\"," +
                "\"name\":\"Vk3dk arm64ec Nightly 497357c0\"," +
                "\"published_at\":\"2026-05-01T04:38:44Z\"," +
                "\"assets\":[{" +
                "\"name\":\"Vk3dk-3.0b-arm64ec-497357c0.wcp\"," +
                "\"browser_download_url\":\"https://github.com/Xnick417x/Winlator-Bionic-Nightly-wcp/releases/download/vk3dk-arm64ec-nightly-497357c0/Vk3dk-3.0b-arm64ec-497357c0.wcp\"" +
                "}]" +
                "}" +
                "]";

        RemoteFeedPayloadLoader.FeedLoadResult result = RemoteFeedPayloadLoader.loadNormalizedFeed(
                RuntimeFeedRegistry.COMMUNITY_XNICK_BIONIC_RELEASES_URL,
                url -> new Downloader.StringResponse(url, 200, releasesJson, "", null)
        );

        assertTrue(result.payload, result.hasPayload());
        assertTrue(result.payload.contains("\"type\":\"DXVK\""));
        assertTrue(result.payload.contains("\"type\":\"VKD3D\""));
        assertTrue(result.payload.contains("\"sourceFeed\":\"community-xnick-bionic\""));
        assertTrue(result.payload.contains("\"channel\":\"nightly\""));
    }

    @Test
    public void nightliesReleasePayloadClassifiesCurrentTranslatorGraphicsAndTurnipAssets() {
        String releasesJson = "[" +
                "{" +
                "\"tag_name\":\"nightly-20260502-094707\"," +
                "\"name\":\"Emulation Nightly Build - 2026-05-02 09:47:07 UTC\"," +
                "\"published_at\":\"2026-05-02T09:47:21Z\"," +
                "\"assets\":[" +
                "{\"name\":\"Box64-0.4.3-234105ff8-Bionic.wcp\",\"browser_download_url\":\"https://github.com/The412Banner/Nightlies/releases/download/nightly-20260502-094707/Box64-0.4.3-234105ff8-Bionic.wcp\"}," +
                "{\"name\":\"Box64-Hybrid-WOW64-d40503fa6.wcp\",\"browser_download_url\":\"https://github.com/The412Banner/Nightlies/releases/download/nightly-20260502-094707/Box64-Hybrid-WOW64-d40503fa6.wcp\"}," +
                "{\"name\":\"FEX-2604-Nightly-8ab00758b.wcp\",\"browser_download_url\":\"https://github.com/The412Banner/Nightlies/releases/download/nightly-20260502-094707/FEX-2604-Nightly-8ab00758b.wcp\"}," +
                "{\"name\":\"dxvk-gplasync-d1b0151c.wcp\",\"browser_download_url\":\"https://github.com/The412Banner/Nightlies/releases/download/nightly-20260502-094707/dxvk-gplasync-d1b0151c.wcp\"}," +
                "{\"name\":\"VKD3D-Proton-arm64ec-3.0b-497357c0.wcp\",\"browser_download_url\":\"https://github.com/The412Banner/Nightlies/releases/download/nightly-20260502-094707/VKD3D-Proton-arm64ec-3.0b-497357c0.wcp\"}," +
                "{\"name\":\"Turnip-v26.2.0-20260502-r2-A8xx.zip\",\"browser_download_url\":\"https://github.com/The412Banner/Nightlies/releases/download/nightly-20260502-094707/Turnip-v26.2.0-20260502-r2-A8xx.zip\"}" +
                "]" +
                "}" +
                "]";

        RemoteFeedPayloadLoader.FeedLoadResult result = RemoteFeedPayloadLoader.loadNormalizedFeed(
                ContentsManager.REMOTE_THE412BANNER_NIGHTLIES_RELEASES,
                url -> new Downloader.StringResponse(url, 200, releasesJson, "", null)
        );

        assertTrue(result.payload, result.hasPayload());
        assertTrue(result.payload.contains("\"type\":\"Box64\""));
        assertTrue(result.payload.contains("\"type\":\"WOWBox64\""));
        assertTrue(result.payload.contains("\"type\":\"FEXCore\""));
        assertTrue(result.payload.contains("\"type\":\"DXVK\""));
        assertTrue(result.payload.contains("\"type\":\"VKD3D\""));
        assertTrue(result.payload.contains("\"type\":\"TurnipDriver\""));
        assertTrue(result.payload.contains("\"displayCategory\":\"Turnip\""));
        assertTrue(result.payload.contains("Turnip-v26.2.0-20260502-r2-A8xx.zip"));
    }
}
