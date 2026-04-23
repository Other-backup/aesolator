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
        assertTrue(joinedIds.contains("community-waim"));
        assertTrue(joinedIds.contains("community-moze-wcp"));
    }

    @Test
    public void launchHydrationPrefersBionicFeedsForBionicRuntime() {
        ArrayList<RuntimeFeedRegistry.FeedSpec> feeds = RuntimeFeedRegistry.getLaunchHydrationFeeds("bionic", "freewine11");
        String joinedIds = feeds.stream().map(feed -> feed.sourceFeedId).collect(Collectors.joining(" "));

        assertTrue(joinedIds.contains("community-alexoqool-bionic"));
        assertTrue(joinedIds.contains("community-xnick-bionic"));
    }

    @Test
    public void launchHydrationKeepsWaimForGlibcRuntime() {
        ArrayList<RuntimeFeedRegistry.FeedSpec> feeds = RuntimeFeedRegistry.getLaunchHydrationFeeds("glibc", "proton-10.0");
        String joinedIds = feeds.stream().map(feed -> feed.sourceFeedId).collect(Collectors.joining(" "));

        assertTrue(joinedIds.contains("community-waim"));
        assertTrue(joinedIds.contains("community-moze-wcp"));
    }
}
