package com.winlator.cmod.contents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RemoteProfileFeedMergerTest {
    @Test
    public void classifySourceModeRecognizesBionicCommunityDonors() {
        assertEquals(
                RuntimeFeedRegistry.SOURCE_MODE_COMMUNITY,
                RemoteProfileFeedMerger.classifySourceMode(
                        "",
                        "Alexoqool/winlator-bionic-build Releases",
                        "Alexoqool Bionic Releases",
                        "https://api.github.com/repos/Alexoqool/winlator-bionic-build/releases?per_page=100"
                )
        );

        assertEquals(
                RuntimeFeedRegistry.SOURCE_MODE_COMMUNITY,
                RemoteProfileFeedMerger.classifySourceMode(
                        "",
                        "Xnick417x/Winlator-Bionic-Nightly-wcp Releases",
                        "Xnick Bionic Nightlies",
                        "https://api.github.com/repos/Xnick417x/Winlator-Bionic-Nightly-wcp/releases?per_page=100"
                )
        );
    }

    @Test
    public void classifySourceModeRecognizesLudashiAsCommunityArchiveSignal() {
        assertEquals(
                RuntimeFeedRegistry.SOURCE_MODE_COMMUNITY,
                RemoteProfileFeedMerger.classifySourceMode(
                        "",
                        "safaaking554-maker/Winlator-ciore-cmod-ludashi-",
                        "Ludashi archive lane",
                        "https://github.com/safaaking554-maker/Winlator-ciore-cmod-ludashi-"
                )
        );
    }

    @Test
    public void mergePayloadsPrefersHigherPriorityCommunityBionicDonorOverWcpHub() {
        String wcphubPayload = "[" +
                "{\"type\":\"Wine\",\"verName\":\"11.4\",\"verCode\":1140," +
                "\"channel\":\"stable\",\"displayCategory\":\"Wine\"," +
                "\"sourceRepo\":\"Arihany/WinlatorWCPHub\",\"sourceLabel\":\"WCPHub\"," +
                "\"publishedAt\":\"2026-04-14T00:00:00Z\",\"remoteUrl\":\"https://example.com/wcphub.wcp\"}" +
                "]";
        String xnickPayload = "[" +
                "{\"type\":\"Wine\",\"verName\":\"11.4\",\"verCode\":1140," +
                "\"channel\":\"stable\",\"displayCategory\":\"Wine\"," +
                "\"sourceRepo\":\"Xnick417x/Winlator-Bionic-Nightly-wcp Releases\",\"sourceLabel\":\"Xnick Bionic Nightlies\"," +
                "\"publishedAt\":\"2026-04-14T00:00:00Z\",\"remoteUrl\":\"https://example.com/xnick.wcp\"}" +
                "]";

        String merged = RemoteProfileFeedMerger.mergePayloads(java.util.Arrays.asList(wcphubPayload, xnickPayload));
        assertTrue(merged, merged.contains("https://example.com/xnick.wcp"));
        assertTrue(merged, !merged.contains("https://example.com/wcphub.wcp"));
    }
}
