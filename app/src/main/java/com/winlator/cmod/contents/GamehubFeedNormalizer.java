package com.winlator.cmod.contents;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Locale;

public final class GamehubFeedNormalizer {
    public static final String SOURCE_FEED_ID = "gamehub";
    public static final String SOURCE_LABEL = "GameHub";
    public static final String SOURCE_REPO = "The412Banner/Gamehub-Components";
    public static final String SOURCE_REPO_RELEASES = SOURCE_REPO + " Releases";
    public static final String SOURCE_REPO_RAW = SOURCE_REPO + " Raw Feed";

    private GamehubFeedNormalizer() {
    }

    public static String normalizeReleaseFeed(String json) {
        JSONArray normalized = new JSONArray();
        if (json == null || json.trim().isEmpty()) return normalized.toString();

        HashSet<String> seen = new HashSet<>();
        try {
            JSONArray releases = new JSONArray(json);
            for (int i = 0; i < releases.length(); i++) {
                JSONObject release = releases.optJSONObject(i);
                if (release == null) continue;
                JSONArray assets = release.optJSONArray("assets");
                if (assets == null) continue;

                for (int j = 0; j < assets.length(); j++) {
                    JSONObject asset = assets.optJSONObject(j);
                    JSONObject candidate = normalizeReleaseAsset(release, asset);
                    if (candidate == null) continue;
                    String key = buildFeedKey(candidate);
                    if (seen.add(key)) normalized.put(candidate);
                }
            }
        } catch (Exception ignored) {
        }
        return normalized.toString();
    }

    public static String normalizeComponentXml(String xml) {
        JSONArray normalized = new JSONArray();
        if (xml == null || xml.trim().isEmpty()) return normalized.toString();

        HashSet<String> seen = new HashSet<>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "string".equals(parser.getName())) {
                    String itemName = parser.getAttributeValue(null, "name");
                    String payload = parser.nextText();
                    JSONObject candidate = normalizeComponentEntry(itemName, payload);
                    if (candidate != null) {
                        String key = buildFeedKey(candidate);
                        if (seen.add(key)) normalized.put(candidate);
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception ignored) {
        }
        return normalized.toString();
    }

    private static JSONObject normalizeReleaseAsset(JSONObject release, JSONObject asset) {
        if (asset == null) return null;
        String assetName = asset.optString("name", "").trim();
        String downloadUrl = asset.optString("browser_download_url", "").trim();
        if (assetName.isEmpty() || downloadUrl.isEmpty() || !looksLikeArchive(assetName)) return null;

        ContentProfile.ContentType type = resolveTypeFromReleaseAsset(assetName);
        if (type == null) return null;

        String releaseTag = release == null ? "" : release.optString("tag_name", "").trim();
        String versionName = stripArchiveSuffix(assetName);
        String channel = deriveReleaseChannel(release, releaseTag, assetName);
        String publishedAt = release == null ? "" : release.optString("published_at", asset.optString("updated_at", "")).trim();
        String releaseNotes = release == null ? "" : release.optString("body", "").trim();
        int versionCode = deriveReleaseVersionCode(
                publishedAt,
                assetName
        );

        JSONObject normalized = new JSONObject();
        try {
            normalized.put("type", type.toString());
            normalized.put("verName", versionName);
            normalized.put("verCode", versionCode);
            normalized.put("description", buildReleaseDescription(assetName, releaseTag, type));
            normalized.put("remoteUrl", downloadUrl);
            normalized.put(ContentProfile.MARK_CHANNEL, channel);
            normalized.put(ContentProfile.MARK_DELIVERY, ContentProfile.DELIVERY_REMOTE);
            normalized.put(ContentProfile.MARK_DISPLAY_CATEGORY, resolveDisplayCategory(type, assetName));
            normalized.put(ContentProfile.MARK_SOURCE_REPO, SOURCE_REPO_RELEASES);
            normalized.put(ContentProfile.MARK_SOURCE_FEED, SOURCE_FEED_ID);
            normalized.put(ContentProfile.MARK_SOURCE_LABEL, SOURCE_LABEL);
            if (!releaseTag.isEmpty()) normalized.put(ContentProfile.MARK_RELEASE_TAG, releaseTag);
            if (!assetName.isEmpty()) normalized.put(ContentProfile.MARK_ARTIFACT_NAME, assetName);
            if (!publishedAt.isEmpty()) normalized.put(ContentProfile.MARK_PUBLISHED_AT, publishedAt);
            if (!releaseNotes.isEmpty()) normalized.put(ContentProfile.MARK_RELEASE_NOTES, releaseNotes);
            return normalized;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JSONObject normalizeComponentEntry(String itemName, String payload) {
        if (payload == null || payload.trim().isEmpty()) return null;
        try {
            JSONObject object = new JSONObject(payload);
            JSONObject entry = object.optJSONObject("entry");
            if (entry == null) return null;

            String downloadUrl = entry.optString("download_url", "").trim();
            String fileName = entry.optString("file_name", "").trim();
            if (downloadUrl.isEmpty() || fileName.isEmpty() || !looksLikeArchive(fileName)) return null;

            ContentProfile.ContentType type = resolveTypeFromComponent(itemName, entry);
            if (type == null) return null;
            if ((type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                    || type == ContentProfile.ContentType.CONTENT_TYPE_PROTON)
                    && !looksLikePackagedRuntime(fileName)) {
                return null;
            }

            String versionName = resolveRawVersionName(itemName, entry);
            String version = object.optString("version", entry.optString("version", "")).trim();
            int versionCode = entry.optInt("version_code", 0);

            JSONObject normalized = new JSONObject();
            normalized.put("type", type.toString());
            normalized.put("verName", versionName);
            normalized.put("verCode", versionCode);
            normalized.put("description", buildRawDescription(itemName, fileName, version, type));
            normalized.put("remoteUrl", downloadUrl);
            normalized.put(ContentProfile.MARK_CHANNEL, deriveChannel(itemName + " " + fileName + " " + version));
            normalized.put(ContentProfile.MARK_DELIVERY, ContentProfile.DELIVERY_REMOTE);
            normalized.put(ContentProfile.MARK_DISPLAY_CATEGORY, resolveDisplayCategory(type, itemName));
            normalized.put(ContentProfile.MARK_SOURCE_REPO, SOURCE_REPO_RAW);
            normalized.put(ContentProfile.MARK_SOURCE_FEED, SOURCE_FEED_ID);
            normalized.put(ContentProfile.MARK_SOURCE_LABEL, SOURCE_LABEL);
            if (!version.isEmpty()) normalized.put(ContentProfile.MARK_RELEASE_TAG, version);
            if (!fileName.isEmpty()) normalized.put(ContentProfile.MARK_ARTIFACT_NAME, fileName);
            return normalized;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ContentProfile.ContentType resolveTypeFromReleaseAsset(String assetName) {
        String lower = assetName == null ? "" : assetName.trim().toLowerCase(Locale.US);
        if (lower.contains("vkd3d-proton")) return ContentProfile.ContentType.CONTENT_TYPE_VKD3D;
        if (lower.contains("dxvk")) return ContentProfile.ContentType.CONTENT_TYPE_DXVK;
        if (lower.contains("fex")) return ContentProfile.ContentType.CONTENT_TYPE_FEXCORE;
        if (lower.contains("wowbox64")) return ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64;
        if (lower.contains("wow64") && lower.contains("box64")) return ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64;
        if (lower.contains("box64")) return ContentProfile.ContentType.CONTENT_TYPE_BOX64;
        if (lower.startsWith("proton-") || lower.contains("-proton-")) return ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        if (lower.startsWith("wine-") || lower.contains("-wine-")) return ContentProfile.ContentType.CONTENT_TYPE_WINE;
        return null;
    }

    private static ContentProfile.ContentType resolveTypeFromComponent(String itemName, JSONObject entry) {
        String lower = ((itemName == null ? "" : itemName) + " "
                + entry.optString("file_name", "") + " "
                + entry.optString("name", "")).toLowerCase(Locale.US);
        int rawType = entry.optInt("type", -1);

        if (rawType == 3) return ContentProfile.ContentType.CONTENT_TYPE_DXVK;
        if (rawType == 4) return ContentProfile.ContentType.CONTENT_TYPE_VKD3D;
        if (rawType == 1) {
            if (lower.contains("fex")) return ContentProfile.ContentType.CONTENT_TYPE_FEXCORE;
            if (lower.contains("wow64") || lower.contains("wowbox64")) return ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64;
            if (lower.contains("box64")) return ContentProfile.ContentType.CONTENT_TYPE_BOX64;
        }
        if (rawType == 5) {
            if (lower.contains("proton")) return ContentProfile.ContentType.CONTENT_TYPE_PROTON;
            if (lower.contains("wine")) return ContentProfile.ContentType.CONTENT_TYPE_WINE;
        }
        return null;
    }

    private static String resolveDisplayCategory(ContentProfile.ContentType type, String rawName) {
        if (type == null) return "";
        String lower = rawName == null ? "" : rawName.trim().toLowerCase(Locale.US);
        if (type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D && lower.contains("proton")) return "VKD3D-Proton";
        if (type == ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64) return "WOWBox64";
        if (type == ContentProfile.ContentType.CONTENT_TYPE_FEXCORE) return "FEXCore";
        return type.toString();
    }

    private static String buildReleaseDescription(String assetName, String releaseTag, ContentProfile.ContentType type) {
        StringBuilder builder = new StringBuilder("GameHub release package");
        if (type != null) builder.append(" • ").append(type.toString());
        if (releaseTag != null && !releaseTag.trim().isEmpty()) builder.append(" • ").append(releaseTag.trim());
        if (assetName != null && !assetName.trim().isEmpty()) builder.append(" • ").append(stripArchiveSuffix(assetName));
        return builder.toString();
    }

    private static String buildRawDescription(String itemName, String fileName, String version, ContentProfile.ContentType type) {
        StringBuilder builder = new StringBuilder("GameHub raw component");
        if (type != null) builder.append(" • ").append(type.toString());
        if (itemName != null && !itemName.trim().isEmpty()) builder.append(" • ").append(itemName.trim());
        if (version != null && !version.trim().isEmpty()) builder.append(" • feed ").append(version.trim());
        if (fileName != null && !fileName.trim().isEmpty()) builder.append(" • ").append(fileName.trim());
        return builder.toString();
    }

    private static String resolveRawVersionName(String itemName, JSONObject entry) {
        String item = itemName == null ? "" : itemName.trim();
        if (!item.isEmpty()) return item;
        String entryName = entry.optString("name", "").trim();
        if (!entryName.isEmpty()) return entryName;
        return stripArchiveSuffix(entry.optString("file_name", "").trim());
    }

    private static String deriveChannel(String value) {
        String lower = value == null ? "" : value.trim().toLowerCase(Locale.US);
        if (lower.contains("nightly")) return ContentProfile.CHANNEL_NIGHTLY;
        if (lower.contains("beta") || lower.contains("rc")) return ContentProfile.CHANNEL_BETA;
        return ContentProfile.CHANNEL_STABLE;
    }

    private static String deriveReleaseChannel(JSONObject release, String releaseTag, String assetName) {
        String derived = deriveChannel(releaseTag + " " + assetName);
        if (!ContentProfile.CHANNEL_STABLE.equals(derived)) return derived;
        if (release != null && release.optBoolean("prerelease", false)) {
            return ContentProfile.CHANNEL_NIGHTLY;
        }
        return derived;
    }

    private static int stableVersionCode(String seed) {
        String normalized = seed == null ? "" : seed.trim();
        return Math.max(1, normalized.hashCode() & 0x7fffffff);
    }

    private static int deriveReleaseVersionCode(String publishedAt, String assetName) {
        String digits = publishedAt == null ? "" : publishedAt.replaceAll("[^0-9]", "");
        if (digits.length() >= 10) {
            try {
                return Integer.parseInt(digits.substring(0, 10));
            } catch (Exception ignored) {
            }
        }
        return stableVersionCode(assetName + "|" + publishedAt);
    }

    private static String buildFeedKey(JSONObject object) {
        return object.optString("type", "").trim() + "|"
                + object.optString("verName", "").trim() + "|"
                + object.optString("remoteUrl", "").trim();
    }

    private static boolean looksLikePackagedRuntime(String fileName) {
        String lower = fileName == null ? "" : fileName.trim().toLowerCase(Locale.US);
        return lower.endsWith(".wcp")
                || lower.endsWith(".wcp.xz")
                || lower.endsWith(".wcp.zst")
                || lower.endsWith(".zip");
    }

    private static boolean looksLikeArchive(String fileName) {
        String lower = fileName == null ? "" : fileName.trim().toLowerCase(Locale.US);
        return lower.endsWith(".wcp")
                || lower.endsWith(".wcp.xz")
                || lower.endsWith(".wcp.zst")
                || lower.endsWith(".zip")
                || lower.endsWith(".txz")
                || lower.endsWith(".tzst")
                || lower.endsWith(".tar.xz")
                || lower.endsWith(".tar.zst");
    }

    private static String stripArchiveSuffix(String name) {
        if (name == null) return "";
        return name.trim().replaceAll("(?i)\\.(wcp\\.xz|wcp\\.zst|tar\\.xz|tar\\.zst|wcp|zip|txz|tzst)$", "");
    }
}
