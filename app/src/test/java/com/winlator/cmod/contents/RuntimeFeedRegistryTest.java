package com.winlator.cmod.contents;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class RuntimeFeedRegistryTest {
    @Test
    public void communityWineFeedsIncludeBionicAndGlibcDonors() {
        ArrayList<RuntimeFeedRegistry.FeedSpec> feeds = RuntimeFeedRegistry.getFeedsForSourceMode(
                RuntimeFeedRegistry.SOURCE_MODE_COMMUNITY,
                ContentProfile.ContentType.CONTENT_TYPE_WINE
        );

        String joinedIds = feeds.stream().map(feed -> feed.sourceFeedId).collect(Collectors.joining(" "));

        assertTrue(joinedIds.contains("community-alexoqool-bionic"));
        assertTrue(joinedIds.contains("community-xnick-bionic"));
        assertTrue(joinedIds.contains("gamenative-proton-wine"));
        assertTrue(joinedIds.contains("community-waim"));
        assertTrue(joinedIds.contains("community-moze-wcp"));
    }

    @Test
    public void protonSourceModesIncludeAndreVtoDedicatedLane() {
        ArrayList<RuntimeFeedRegistry.FeedSpec> feeds = RuntimeFeedRegistry.getFeedsForSourceMode(
                RuntimeFeedRegistry.SOURCE_MODE_ANDREVTO_PROTON,
                ContentProfile.ContentType.CONTENT_TYPE_PROTON
        );
        String joinedIds = feeds.stream().map(feed -> feed.sourceFeedId).collect(Collectors.joining(" "));

        assertTrue(joinedIds.contains("andrevto-proton11"));
    }

    @Test
    public void gamenativeProtonSourceModeIncludesWineAndProtonFeeds() {
        ArrayList<RuntimeFeedRegistry.FeedSpec> protonFeeds = RuntimeFeedRegistry.getFeedsForSourceMode(
                RuntimeFeedRegistry.SOURCE_MODE_GAMENATIVE_PROTON,
                ContentProfile.ContentType.CONTENT_TYPE_PROTON
        );
        ArrayList<RuntimeFeedRegistry.FeedSpec> wineFeeds = RuntimeFeedRegistry.getFeedsForSourceMode(
                RuntimeFeedRegistry.SOURCE_MODE_GAMENATIVE_PROTON,
                ContentProfile.ContentType.CONTENT_TYPE_WINE
        );

        assertTrue(protonFeeds.stream().anyMatch(feed -> "gamenative-proton-wine".equals(feed.sourceFeedId)));
        assertTrue(wineFeeds.stream().anyMatch(feed -> "gamenative-proton-wine".equals(feed.sourceFeedId)));
    }

    @Test
    public void xnickBionicNightliesExposeComponentFeeds() {
        assertTrue(RuntimeFeedRegistry.getFeedsForSourceMode(
                RuntimeFeedRegistry.SOURCE_MODE_COMMUNITY,
                ContentProfile.ContentType.CONTENT_TYPE_DXVK
        ).stream().anyMatch(feed -> "community-xnick-bionic".equals(feed.sourceFeedId)));
        assertTrue(RuntimeFeedRegistry.getFeedsForSourceMode(
                RuntimeFeedRegistry.SOURCE_MODE_COMMUNITY,
                ContentProfile.ContentType.CONTENT_TYPE_VKD3D
        ).stream().anyMatch(feed -> "community-xnick-bionic".equals(feed.sourceFeedId)));
        assertTrue(RuntimeFeedRegistry.getFeedsForSourceMode(
                RuntimeFeedRegistry.SOURCE_MODE_COMMUNITY,
                ContentProfile.ContentType.CONTENT_TYPE_FEXCORE
        ).stream().anyMatch(feed -> "community-xnick-bionic".equals(feed.sourceFeedId)));
        assertTrue(RuntimeFeedRegistry.getFeedsForSourceMode(
                RuntimeFeedRegistry.SOURCE_MODE_COMMUNITY,
                ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64
        ).stream().anyMatch(feed -> "community-xnick-bionic".equals(feed.sourceFeedId)));
        assertTrue(RuntimeFeedRegistry.getFeedsForSourceMode(
                RuntimeFeedRegistry.SOURCE_MODE_COMMUNITY,
                ContentProfile.ContentType.CONTENT_TYPE_BOX64
        ).stream().anyMatch(feed -> "community-xnick-bionic".equals(feed.sourceFeedId)));
    }

    @Test
    public void launchHydrationPrefersBionicFeedsForBionicRuntime() {
        ArrayList<RuntimeFeedRegistry.FeedSpec> feeds = RuntimeFeedRegistry.getLaunchHydrationFeeds("bionic", "freewine11");
        String joinedIds = feeds.stream().map(feed -> feed.sourceFeedId).collect(Collectors.joining(" "));

        assertTrue(joinedIds.contains("gamenative-proton-wine"));
        assertTrue(joinedIds.contains("andrevto-proton11"));
        assertTrue(joinedIds.contains("community-alexoqool-bionic"));
        assertTrue(joinedIds.contains("community-xnick-bionic"));
    }

    @Test
    public void launchHydrationIncludesCurrentProtonFeedsForGlibcRuntime() {
        ArrayList<RuntimeFeedRegistry.FeedSpec> feeds = RuntimeFeedRegistry.getLaunchHydrationFeeds("glibc", "proton-10.0");
        String joinedIds = feeds.stream().map(feed -> feed.sourceFeedId).collect(Collectors.joining(" "));

        assertTrue(joinedIds.contains("gamenative-proton-wine"));
        assertTrue(joinedIds.contains("andrevto-proton11"));
        assertTrue(joinedIds.contains("community-waim"));
        assertTrue(joinedIds.contains("community-moze-wcp"));
    }
}
