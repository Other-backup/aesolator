package com.winlator.cmod.contents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class RuntimeFeedRegistry {
    public static final String SOURCE_MODE_ARCHIVE = "archive";
    public static final String SOURCE_MODE_NIGHTLIES = "nightlies";
    public static final String SOURCE_MODE_GAMEHUB = "gamehub";
    public static final String SOURCE_MODE_WCPHUB = "wcphub";
    public static final String SOURCE_MODE_COMMUNITY = "community";
    public static final String SOURCE_MODE_GAMENATIVE_PROTON = "gamenative_proton";
    public static final String SOURCE_MODE_ANDREVTO_PROTON = "andrevto_proton";

    public static final String COMMUNITY_WAIM_RELEASES_URL = "https://api.github.com/repos/Waim908/wine-winlator/releases?per_page=100";
    public static final String COMMUNITY_MOZE_WCP_RELEASES_URL = "https://api.github.com/repos/moze30/winlator-wcp/releases?per_page=100";
    public static final String COMMUNITY_ALEXOQOOL_BIONIC_RELEASES_URL = "https://api.github.com/repos/Alexoqool/winlator-bionic-build/releases?per_page=100";
    public static final String COMMUNITY_XNICK_BIONIC_RELEASES_URL = "https://api.github.com/repos/Xnick417x/Winlator-Bionic-Nightly-wcp/releases?per_page=100";
    public static final String GAMENATIVE_PROTON_RELEASES_URL = "https://api.github.com/repos/GameNative/proton-wine/releases?per_page=100";
    public static final String ANDREVTO_PROTON_RELEASES_URL = "https://api.github.com/repos/AndreVto/proton-wine/releases?per_page=100";

    public enum FeedFormat {
        JSON_INDEX,
        GITHUB_RELEASES
    }

    public static final class FeedSpec {
        public final String sourceMode;
        public final String sourceFeedId;
        public final String sourceLabel;
        public final String sourceRepo;
        public final String descriptionPrefix;
        public final String url;
        public final String matchPrefix;
        public final FeedFormat format;
        private final List<ContentProfile.ContentType> supportedTypes;

        FeedSpec(String sourceMode,
                 String sourceFeedId,
                 String sourceLabel,
                 String sourceRepo,
                 String descriptionPrefix,
                 String url,
                 String matchPrefix,
                 FeedFormat format,
                 ContentProfile.ContentType... supportedTypes) {
            this.sourceMode = sourceMode;
            this.sourceFeedId = sourceFeedId;
            this.sourceLabel = sourceLabel;
            this.sourceRepo = sourceRepo;
            this.descriptionPrefix = descriptionPrefix;
            this.url = url;
            this.matchPrefix = matchPrefix;
            this.format = format;
            this.supportedTypes = Arrays.asList(supportedTypes);
        }

        public boolean supportsType(ContentProfile.ContentType type) {
            return type != null && supportedTypes.contains(type);
        }

        public boolean matchesUrl(String candidateUrl) {
            String normalizedUrl = normalize(candidateUrl);
            return !normalizedUrl.isEmpty() && normalizedUrl.startsWith(normalize(matchPrefix));
        }
    }

    private static final FeedSpec FEED_WAIM_WINE_RELEASES = new FeedSpec(
            SOURCE_MODE_COMMUNITY,
            "community-waim",
            "Waim Wine Releases",
            "Waim908/wine-winlator Releases",
            "Community glibc runtime package",
            COMMUNITY_WAIM_RELEASES_URL,
            "https://api.github.com/repos/Waim908/wine-winlator/releases",
            FeedFormat.GITHUB_RELEASES,
            ContentProfile.ContentType.CONTENT_TYPE_WINE,
            ContentProfile.ContentType.CONTENT_TYPE_PROTON
    );

    private static final FeedSpec FEED_ALEXOQOOL_BIONIC_RELEASES = new FeedSpec(
            SOURCE_MODE_COMMUNITY,
            "community-alexoqool-bionic",
            "Alexoqool Bionic Releases",
            "Alexoqool/winlator-bionic-build Releases",
            "Community bionic runtime package",
            COMMUNITY_ALEXOQOOL_BIONIC_RELEASES_URL,
            "https://api.github.com/repos/Alexoqool/winlator-bionic-build/releases",
            FeedFormat.GITHUB_RELEASES,
            ContentProfile.ContentType.CONTENT_TYPE_WINE
    );

    private static final FeedSpec FEED_MOZE_WCP_RELEASES = new FeedSpec(
            SOURCE_MODE_COMMUNITY,
            "community-moze-wcp",
            "moze WCP Releases",
            "moze30/winlator-wcp Releases",
            "Community glibc WCP package",
            COMMUNITY_MOZE_WCP_RELEASES_URL,
            "https://api.github.com/repos/moze30/winlator-wcp/releases",
            FeedFormat.GITHUB_RELEASES,
            ContentProfile.ContentType.CONTENT_TYPE_WINE,
            ContentProfile.ContentType.CONTENT_TYPE_PROTON
    );

    private static final FeedSpec FEED_XNICK_BIONIC_RELEASES = new FeedSpec(
            SOURCE_MODE_COMMUNITY,
            "community-xnick-bionic",
            "Xnick Bionic Nightlies",
            "Xnick417x/Winlator-Bionic-Nightly-wcp Releases",
            "Community bionic nightly package",
            COMMUNITY_XNICK_BIONIC_RELEASES_URL,
            "https://api.github.com/repos/Xnick417x/Winlator-Bionic-Nightly-wcp/releases",
            FeedFormat.GITHUB_RELEASES,
            ContentProfile.ContentType.CONTENT_TYPE_WINE,
            ContentProfile.ContentType.CONTENT_TYPE_DXVK,
            ContentProfile.ContentType.CONTENT_TYPE_VKD3D,
            ContentProfile.ContentType.CONTENT_TYPE_BOX64,
            ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64,
            ContentProfile.ContentType.CONTENT_TYPE_FEXCORE
    );

    private static final FeedSpec FEED_ANDREVTO_PROTON_RELEASES = new FeedSpec(
            SOURCE_MODE_ANDREVTO_PROTON,
            "andrevto-proton11",
            "AndreVto Bionic Proton 11",
            "AndreVto/proton-wine Releases",
            "AndreVto bionic Proton 11 package",
            ANDREVTO_PROTON_RELEASES_URL,
            "https://api.github.com/repos/AndreVto/proton-wine/releases",
            FeedFormat.GITHUB_RELEASES,
            ContentProfile.ContentType.CONTENT_TYPE_PROTON
    );

    private static final FeedSpec FEED_GAMENATIVE_PROTON_RELEASES = new FeedSpec(
            SOURCE_MODE_GAMENATIVE_PROTON,
            "gamenative-proton-wine",
            "GameNative Proton/Wine Releases",
            "GameNative/proton-wine Releases",
            "GameNative bionic Proton/Wine package",
            GAMENATIVE_PROTON_RELEASES_URL,
            "https://api.github.com/repos/GameNative/proton-wine/releases",
            FeedFormat.GITHUB_RELEASES,
            ContentProfile.ContentType.CONTENT_TYPE_WINE,
            ContentProfile.ContentType.CONTENT_TYPE_PROTON
    );

    private static final FeedSpec FEED_THE412BANNER_NIGHTLIES = new FeedSpec(
            SOURCE_MODE_NIGHTLIES,
            GamehubFeedNormalizer.NIGHTLIES_FEED_ID,
            GamehubFeedNormalizer.NIGHTLIES_LABEL,
            GamehubFeedNormalizer.NIGHTLIES_REPO_RELEASES,
            "The412Banner nightly package",
            ContentsManager.REMOTE_THE412BANNER_NIGHTLIES_RELEASES,
            "https://api.github.com/repos/The412Banner/Nightlies/releases",
            FeedFormat.GITHUB_RELEASES,
            ContentProfile.ContentType.CONTENT_TYPE_PROTON,
            ContentProfile.ContentType.CONTENT_TYPE_DXVK,
            ContentProfile.ContentType.CONTENT_TYPE_VKD3D,
            ContentProfile.ContentType.CONTENT_TYPE_BOX64,
            ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64,
            ContentProfile.ContentType.CONTENT_TYPE_FEXCORE
    );

    private static final FeedSpec FEED_ARIHANY_WCPHUB = new FeedSpec(
            SOURCE_MODE_WCPHUB,
            "wcphub",
            "WCPHub",
            "Arihany/WinlatorWCPHub",
            "WCPHub package",
            ContentsManager.REMOTE_PROFILES,
            ContentsManager.REMOTE_PROFILES,
            FeedFormat.JSON_INDEX,
            ContentProfile.ContentType.CONTENT_TYPE_WINE,
            ContentProfile.ContentType.CONTENT_TYPE_PROTON,
            ContentProfile.ContentType.CONTENT_TYPE_DXVK,
            ContentProfile.ContentType.CONTENT_TYPE_VKD3D,
            ContentProfile.ContentType.CONTENT_TYPE_BOX64,
            ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64,
            ContentProfile.ContentType.CONTENT_TYPE_FEXCORE
    );

    private RuntimeFeedRegistry() {}

    public static ArrayList<FeedSpec> getFeedsForSourceMode(String sourceMode, ContentProfile.ContentType type) {
        ArrayList<FeedSpec> feeds = new ArrayList<>();
        String normalizedMode = normalize(sourceMode);
        if (SOURCE_MODE_COMMUNITY.equals(normalizedMode)) {
            addIfSupported(feeds, FEED_ALEXOQOOL_BIONIC_RELEASES, type);
            addIfSupported(feeds, FEED_XNICK_BIONIC_RELEASES, type);
            addIfSupported(feeds, FEED_GAMENATIVE_PROTON_RELEASES, type);
            addIfSupported(feeds, FEED_ANDREVTO_PROTON_RELEASES, type);
            addIfSupported(feeds, FEED_WAIM_WINE_RELEASES, type);
            addIfSupported(feeds, FEED_MOZE_WCP_RELEASES, type);
            addIfSupported(feeds, FEED_ARIHANY_WCPHUB, type);
            addIfSupported(feeds, FEED_THE412BANNER_NIGHTLIES, type);
            return feeds;
        }
        if (SOURCE_MODE_ANDREVTO_PROTON.equals(normalizedMode)) {
            addIfSupported(feeds, FEED_ANDREVTO_PROTON_RELEASES, type);
            return feeds;
        }
        if (SOURCE_MODE_GAMENATIVE_PROTON.equals(normalizedMode)) {
            addIfSupported(feeds, FEED_GAMENATIVE_PROTON_RELEASES, type);
            return feeds;
        }
        if (SOURCE_MODE_WCPHUB.equals(normalizedMode)) {
            addIfSupported(feeds, FEED_ARIHANY_WCPHUB, type);
            return feeds;
        }
        if (SOURCE_MODE_NIGHTLIES.equals(normalizedMode)) {
            addIfSupported(feeds, FEED_THE412BANNER_NIGHTLIES, type);
            return feeds;
        }
        return feeds;
    }

    public static ArrayList<FeedSpec> getLaunchHydrationFeeds(String runtimeModel, String wineVersion) {
        ArrayList<FeedSpec> feeds = new ArrayList<>();
        String normalizedRuntimeModel = ContentProfile.normalizeRuntimeModel(runtimeModel);
        String normalizedWineVersion = normalize(wineVersion);
        boolean wantsBionicCommunity = ContentProfile.RUNTIME_MODEL_BIONIC.equals(normalizedRuntimeModel)
                || normalizedWineVersion.contains("freewine")
                || normalizedWineVersion.contains("bionic")
                || normalizedWineVersion.contains("android-native");
        boolean wantsGlibcCommunity = ContentProfile.RUNTIME_MODEL_GLIBC.equals(normalizedRuntimeModel)
                || normalizedWineVersion.startsWith("proton-");
        if (!wantsBionicCommunity && !wantsGlibcCommunity) return feeds;

        if (wantsBionicCommunity) {
            feeds.add(FEED_GAMENATIVE_PROTON_RELEASES);
            feeds.add(FEED_ANDREVTO_PROTON_RELEASES);
            feeds.add(FEED_ALEXOQOOL_BIONIC_RELEASES);
            feeds.add(FEED_XNICK_BIONIC_RELEASES);
        }
        if (wantsGlibcCommunity) {
            feeds.add(FEED_WAIM_WINE_RELEASES);
            feeds.add(FEED_MOZE_WCP_RELEASES);
        }
        feeds.add(FEED_ARIHANY_WCPHUB);
        feeds.add(FEED_THE412BANNER_NIGHTLIES);
        return feeds;
    }

    public static FeedSpec findByUrl(String url) {
        for (FeedSpec feed : getAllFeeds()) {
            if (feed.matchesUrl(url)) return feed;
        }
        return null;
    }

    public static boolean looksLikeNightliesSource(String value) {
        String normalized = normalize(value);
        return normalized.contains("the412banner/nightlies")
                || normalized.contains("nightlies releases")
                || normalized.contains("nightlies by the412banner");
    }

    public static boolean looksLikeAndreVtoProtonSource(String value) {
        String normalized = normalize(value);
        return normalized.contains("andrevto/proton-wine")
                || normalized.contains("andrevto bionic proton 11")
                || normalized.contains("andrevto-proton11");
    }

    public static boolean looksLikeGameNativeProtonSource(String value) {
        String normalized = normalize(value);
        return normalized.contains("gamenative/proton-wine")
                || normalized.contains("gamenative proton/wine")
                || normalized.contains("gamenative-proton-wine");
    }

    public static boolean looksLikeWcpHubSource(String value) {
        String normalized = normalize(value);
        return normalized.contains("open-wine-components")
                || normalized.contains("wcphub")
                || normalized.contains("arihany")
                || normalized.contains("winlatorwcphub");
    }

    public static boolean looksLikeCommunitySource(String value) {
        String normalized = normalize(value);
        return normalized.contains("community-waim")
                || normalized.contains("waim wine releases")
                || normalized.contains("waim908/wine-winlator")
                || normalized.contains("community glibc runtime package")
                || normalized.contains("moze30/winlator-wcp")
                || normalized.contains("community glibc wcp package")
                || normalized.contains("alexoqool/winlator-bionic-build")
                || normalized.contains("community bionic runtime package")
                || normalized.contains("xnick417x/winlator-bionic-nightly-wcp")
                || normalized.contains("community bionic nightly package")
                || looksLikeGameNativeProtonSource(normalized)
                || looksLikeAndreVtoProtonSource(normalized)
                || normalized.contains("ludashi")
                || normalized.contains("ciore cmod ludashi");
    }

    private static List<FeedSpec> getAllFeeds() {
        return Arrays.asList(
                FEED_ALEXOQOOL_BIONIC_RELEASES,
                FEED_XNICK_BIONIC_RELEASES,
                FEED_GAMENATIVE_PROTON_RELEASES,
                FEED_ANDREVTO_PROTON_RELEASES,
                FEED_WAIM_WINE_RELEASES,
                FEED_MOZE_WCP_RELEASES,
                FEED_THE412BANNER_NIGHTLIES,
                FEED_ARIHANY_WCPHUB
        );
    }

    private static void addIfSupported(ArrayList<FeedSpec> feeds, FeedSpec feed, ContentProfile.ContentType type) {
        if (feed.supportsType(type)) feeds.add(feed);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }
}
