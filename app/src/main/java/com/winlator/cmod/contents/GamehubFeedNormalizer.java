package com.winlator.cmod.contents;

import android.os.Build;
import android.text.Html;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GamehubFeedNormalizer {
    public static final String SOURCE_FEED_ID = "gamehub";
    public static final String SOURCE_LABEL = "GameHub";
    public static final String SOURCE_REPO = "The412Banner/Gamehub-Components";
    public static final String SOURCE_REPO_RELEASES = SOURCE_REPO + " Releases";
    public static final String SOURCE_REPO_RAW = SOURCE_REPO + " Raw Feed";
    public static final String NIGHTLIES_FEED_ID = "nightlies";
    public static final String NIGHTLIES_LABEL = "Nightlies";
    public static final String NIGHTLIES_REPO = "The412Banner/Nightlies";
    public static final String NIGHTLIES_REPO_RELEASES = NIGHTLIES_REPO + " Releases";
    private static final Pattern EXPANDED_ASSET_ROW_PATTERN =
            Pattern.compile("(?is)<li[^>]*Box-row[^>]*>(.*?)</li>");
    private static final Pattern EXPANDED_ASSET_HREF_PATTERN =
            Pattern.compile("href=\"([^\"]+/releases/download/[^\"]+)\"");
    private static final Pattern EXPANDED_ASSET_NAME_PATTERN =
            Pattern.compile("(?is)<span[^>]*Truncate-text\\s+text-bold[^>]*>([^<]+)</span>");
    private static final Pattern EXPANDED_ASSET_DIGEST_PATTERN =
            Pattern.compile("sha256:([0-9a-fA-F]{64})");
    private static final Pattern EXPANDED_ASSET_DATETIME_PATTERN =
            Pattern.compile("datetime=\"([^\"]+)\"");
    private static final Pattern ATOM_ENTRY_PATTERN =
            Pattern.compile("(?is)<entry\\b[^>]*>(.*?)</entry>");
    private static final Pattern ATOM_UPDATED_PATTERN =
            Pattern.compile("(?is)<updated\\b[^>]*>(.*?)</updated>");
    private static final Pattern ATOM_CONTENT_PATTERN =
            Pattern.compile("(?is)<content\\b[^>]*>(.*?)</content>");
    private static final Pattern ATOM_LINK_TAG_PATTERN =
            Pattern.compile("(?is)<link\\b([^>]*)/?>");
    private static final Pattern ATOM_HREF_ATTR_PATTERN =
            Pattern.compile("(?is)href\\s*=\\s*\"([^\"]+)\"");

    private GamehubFeedNormalizer() {
    }

    public static final class ReleaseFeedEntry {
        public final String tag;
        public final String publishedAt;
        public final String releaseNotes;

        public ReleaseFeedEntry(String tag, String publishedAt, String releaseNotes) {
            this.tag = tag == null ? "" : tag.trim();
            this.publishedAt = publishedAt == null ? "" : publishedAt.trim();
            this.releaseNotes = releaseNotes == null ? "" : releaseNotes.trim();
        }

        public boolean isValid() {
            return !tag.isEmpty();
        }
    }

    public static String normalizeReleaseFeed(String json) {
        return normalizeReleaseFeed(
                json,
                SOURCE_FEED_ID,
                SOURCE_LABEL,
                SOURCE_REPO_RELEASES,
                "GameHub release package"
        );
    }

    public static String normalizeNightliesReleaseFeed(String json) {
        return normalizeReleaseFeed(
                json,
                NIGHTLIES_FEED_ID,
                NIGHTLIES_LABEL,
                NIGHTLIES_REPO_RELEASES,
                "The412Banner nightly package"
        );
    }

    public static String normalizeGitHubReleaseFeed(String json,
                                                    String sourceFeedId,
                                                    String sourceLabel,
                                                    String sourceRepo,
                                                    String descriptionPrefix) {
        return normalizeReleaseFeed(json, sourceFeedId, sourceLabel, sourceRepo, descriptionPrefix);
    }

    public static List<ReleaseFeedEntry> parseGitHubReleaseAtom(String atom, String repoPath) {
        ArrayList<ReleaseFeedEntry> entries = new ArrayList<>();
        if (atom == null || atom.trim().isEmpty()) return entries;

        HashSet<String> seen = new HashSet<>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(atom));

            boolean insideEntry = false;
            String updatedAt = "";
            String alternateLink = "";
            String content = "";
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                if (eventType == XmlPullParser.START_TAG) {
                    if ("entry".equals(tagName)) {
                        insideEntry = true;
                        updatedAt = "";
                        alternateLink = "";
                        content = "";
                    } else if (insideEntry && "updated".equals(tagName)) {
                        updatedAt = parser.nextText();
                    } else if (insideEntry && "content".equals(tagName)) {
                        content = parser.nextText();
                    } else if (insideEntry && "link".equals(tagName)) {
                        String rel = parser.getAttributeValue(null, "rel");
                        String href = parser.getAttributeValue(null, "href");
                        if ("alternate".equalsIgnoreCase(rel) && href != null) {
                            alternateLink = href.trim();
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && "entry".equals(tagName)) {
                    insideEntry = false;
                    String tag = extractReleaseTagFromLink(alternateLink, repoPath);
                    if (!tag.isEmpty() && seen.add(tag)) {
                        entries.add(new ReleaseFeedEntry(tag, updatedAt, sanitizeReleaseNotes(content)));
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception ignored) {
        }
        if (entries.isEmpty()) {
            parseGitHubReleaseAtomRegex(atom, repoPath, entries, seen);
        }
        return entries;
    }

    public static String normalizeExpandedAssetsHtml(String html,
                                                    ReleaseFeedEntry entry,
                                                    String sourceFeedId,
                                                    String sourceLabel,
                                                    String sourceRepo,
                                                    String descriptionPrefix) {
        JsonArray normalized = new JsonArray();
        if (html == null || html.trim().isEmpty() || entry == null || !entry.isValid()) {
            return normalized.toString();
        }

        HashSet<String> seen = new HashSet<>();
        try {
            Matcher rowMatcher = EXPANDED_ASSET_ROW_PATTERN.matcher(html);
            while (rowMatcher.find()) {
                String rowHtml = rowMatcher.group(1);
                String href = findFirstGroup(EXPANDED_ASSET_HREF_PATTERN, rowHtml);
                String assetName = decodeHtmlEntities(findFirstGroup(EXPANDED_ASSET_NAME_PATTERN, rowHtml));
                if (href.isEmpty() || assetName.isEmpty()) continue;

                String downloadUrl = href.startsWith("http") ? href : "https://github.com" + href;
                if (!looksLikeArchive(assetName)) continue;

                ContentProfile.ContentType type = resolveTypeFromReleaseAsset(assetName);
                if (type == null) continue;

                String publishedAt = findFirstGroup(EXPANDED_ASSET_DATETIME_PATTERN, rowHtml);
                if (publishedAt.isEmpty()) publishedAt = entry.publishedAt;
                int versionCode = deriveReleaseVersionCode(publishedAt, assetName);

                JsonObject candidate = new JsonObject();
                candidate.addProperty("type", type.toString());
                candidate.addProperty("verName", stripArchiveSuffix(assetName));
                candidate.addProperty("verCode", versionCode);
                candidate.addProperty("description", buildReleaseDescription(assetName, entry.tag, type, descriptionPrefix));
                candidate.addProperty("remoteUrl", downloadUrl);
                candidate.addProperty(ContentProfile.MARK_CHANNEL, deriveReleaseChannel((JsonObject) null, entry.tag, assetName));
                candidate.addProperty(ContentProfile.MARK_DELIVERY, ContentProfile.DELIVERY_REMOTE);
                candidate.addProperty(ContentProfile.MARK_DISPLAY_CATEGORY, resolveDisplayCategory(type, assetName));
                String runtimeModel = resolveRuntimeModel(type, assetName, entry.tag, sourceRepo, sourceLabel, descriptionPrefix);
                if (!runtimeModel.isEmpty()) candidate.addProperty(ContentProfile.MARK_RUNTIME_MODEL, runtimeModel);
                candidate.addProperty(ContentProfile.MARK_SOURCE_REPO, sourceRepo);
                candidate.addProperty(ContentProfile.MARK_SOURCE_FEED, sourceFeedId);
                candidate.addProperty(ContentProfile.MARK_SOURCE_LABEL, sourceLabel);
                candidate.addProperty(ContentProfile.MARK_RELEASE_TAG, entry.tag);
                candidate.addProperty(ContentProfile.MARK_ARTIFACT_NAME, assetName);
                if (!publishedAt.isEmpty()) candidate.addProperty(ContentProfile.MARK_PUBLISHED_AT, publishedAt);
                if (!entry.releaseNotes.isEmpty()) candidate.addProperty(ContentProfile.MARK_RELEASE_NOTES, entry.releaseNotes);

                String digest = findFirstGroup(EXPANDED_ASSET_DIGEST_PATTERN, rowHtml);
                if (!digest.isEmpty()) candidate.addProperty(ContentProfile.MARK_SHA256, digest);

                String key = buildFeedKey(candidate);
                if (seen.add(key)) normalized.add(candidate);
            }
        } catch (Exception ignored) {
        }
        return normalized.toString();
    }

    private static String normalizeReleaseFeed(String json,
                                               String sourceFeedId,
                                               String sourceLabel,
                                               String sourceRepo,
                                               String descriptionPrefix) {
        JsonArray normalized = new JsonArray();
        if (json == null || json.trim().isEmpty()) return normalized.toString();

        HashSet<String> seen = new HashSet<>();
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonArray()) return normalized.toString();
            JsonArray releases = parsed.getAsJsonArray();
            for (JsonElement releaseElement : releases) {
                if (!releaseElement.isJsonObject()) continue;
                JsonObject release = releaseElement.getAsJsonObject();
                JsonElement assetsElement = release.get("assets");
                if (assetsElement == null || !assetsElement.isJsonArray()) continue;

                for (JsonElement assetElement : assetsElement.getAsJsonArray()) {
                    if (!assetElement.isJsonObject()) continue;
                    JsonObject candidate = normalizeReleaseAsset(
                            release,
                            assetElement.getAsJsonObject(),
                            sourceFeedId,
                            sourceLabel,
                            sourceRepo,
                            descriptionPrefix
                    );
                    if (candidate == null) continue;
                    String key = buildFeedKey(candidate);
                    if (seen.add(key)) normalized.add(candidate);
                }
            }
        } catch (Exception ignored) {
        }
        return normalized.toString();
    }

    public static String normalizeComponentXml(String xml) {
        JSONArray normalized = new JSONArray();
        if (xml == null || xml.trim().isEmpty()) return JsonPayloadSerializer.toJson(normalized);

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
        return JsonPayloadSerializer.toJson(normalized);
    }

    private static JsonObject normalizeReleaseAsset(JsonObject release,
                                                    JsonObject asset,
                                                    String sourceFeedId,
                                                    String sourceLabel,
                                                    String sourceRepo,
                                                    String descriptionPrefix) {
        if (asset == null) return null;
        String assetName = optString(asset, "name").trim();
        String downloadUrl = optString(asset, "browser_download_url").trim();
        if (assetName.isEmpty() || downloadUrl.isEmpty() || !looksLikeArchive(assetName)) return null;

        ContentProfile.ContentType type = resolveTypeFromReleaseAsset(assetName);
        if (type == null) return null;

        String releaseTag = optString(release, "tag_name").trim();
        String versionName = stripArchiveSuffix(assetName);
        String channel = deriveReleaseChannel(release, releaseTag, assetName);
        String publishedAt = optString(release, "published_at", optString(asset, "updated_at")).trim();
        String releaseNotes = optString(release, "body").trim();
        int versionCode = deriveReleaseVersionCode(
                publishedAt,
                assetName
        );

        JsonObject normalized = new JsonObject();
        normalized.addProperty("type", type.toString());
        normalized.addProperty("verName", versionName);
        normalized.addProperty("verCode", versionCode);
        normalized.addProperty("description", buildReleaseDescription(assetName, releaseTag, type, descriptionPrefix));
        normalized.addProperty("remoteUrl", downloadUrl);
        normalized.addProperty(ContentProfile.MARK_CHANNEL, channel);
        normalized.addProperty(ContentProfile.MARK_DELIVERY, ContentProfile.DELIVERY_REMOTE);
        normalized.addProperty(ContentProfile.MARK_DISPLAY_CATEGORY, resolveDisplayCategory(type, assetName));
        String runtimeModel = resolveRuntimeModel(type, assetName, releaseTag, sourceRepo, sourceLabel, descriptionPrefix);
        if (!runtimeModel.isEmpty()) normalized.addProperty(ContentProfile.MARK_RUNTIME_MODEL, runtimeModel);
        normalized.addProperty(ContentProfile.MARK_SOURCE_REPO, sourceRepo);
        normalized.addProperty(ContentProfile.MARK_SOURCE_FEED, sourceFeedId);
        normalized.addProperty(ContentProfile.MARK_SOURCE_LABEL, sourceLabel);
        if (!releaseTag.isEmpty()) normalized.addProperty(ContentProfile.MARK_RELEASE_TAG, releaseTag);
        if (!assetName.isEmpty()) normalized.addProperty(ContentProfile.MARK_ARTIFACT_NAME, assetName);
        if (!publishedAt.isEmpty()) normalized.addProperty(ContentProfile.MARK_PUBLISHED_AT, publishedAt);
        if (!releaseNotes.isEmpty()) normalized.addProperty(ContentProfile.MARK_RELEASE_NOTES, releaseNotes);
        String digest = optString(asset, "digest").trim();
        if (!digest.isEmpty()) normalized.addProperty(ContentProfile.MARK_SHA256, digest);
        return normalized;
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
            String runtimeModel = resolveRuntimeModel(type, itemName, fileName, version, SOURCE_REPO_RAW);
            if (!runtimeModel.isEmpty()) normalized.put(ContentProfile.MARK_RUNTIME_MODEL, runtimeModel);
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
        if (lower.startsWith("freewine") || lower.contains("freewine")) return ContentProfile.ContentType.CONTENT_TYPE_WINE;
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

    private static String resolveRuntimeModel(ContentProfile.ContentType type, String... hints) {
        if (type != ContentProfile.ContentType.CONTENT_TYPE_WINE
                && type != ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
            return "";
        }
        return ContentProfile.inferRuntimeModel(type, hints);
    }

    private static String buildReleaseDescription(String assetName,
                                                  String releaseTag,
                                                  ContentProfile.ContentType type,
                                                  String descriptionPrefix) {
        StringBuilder builder = new StringBuilder(descriptionPrefix == null || descriptionPrefix.trim().isEmpty()
                ? "Release package"
                : descriptionPrefix.trim());
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

    private static String deriveReleaseChannel(JsonObject release, String releaseTag, String assetName) {
        String derived = deriveChannel(releaseTag + " " + assetName);
        if (!ContentProfile.CHANNEL_STABLE.equals(derived)) return derived;
        if (release != null && optBoolean(release, "prerelease", false)) {
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

    private static String buildFeedKey(JsonObject object) {
        return optString(object, "type").trim() + "|"
                + optString(object, "verName").trim() + "|"
                + optString(object, "remoteUrl").trim();
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

    private static String extractReleaseTagFromLink(String link, String repoPath) {
        String normalizedLink = link == null ? "" : link.trim();
        String normalizedRepo = repoPath == null ? "" : repoPath.trim();
        if (normalizedLink.isEmpty() || normalizedRepo.isEmpty()) return "";
        String marker = "/" + normalizedRepo + "/releases/tag/";
        int index = normalizedLink.indexOf(marker);
        if (index < 0) return "";
        return normalizedLink.substring(index + marker.length()).trim();
    }

    private static String sanitizeReleaseNotes(String value) {
        String normalized = decodeHtmlEntities(value);
        if (normalized.isEmpty()) return "";
        normalized = normalized.replaceAll("(?is)<br\\s*/?>", "\n");
        normalized = normalized.replaceAll("(?is)</p>", "\n\n");
        normalized = normalized.replaceAll("(?is)</li>", "\n");
        normalized = normalized.replaceAll("(?is)<[^>]+>", " ");
        normalized = normalized.replace('\u00a0', ' ');
        normalized = normalized.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        return normalized.trim();
    }

    private static String decodeHtmlEntities(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        String normalized = value.trim();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Html.fromHtml(normalized, Html.FROM_HTML_MODE_LEGACY).toString().trim();
            }
            return Html.fromHtml(normalized).toString().trim();
        } catch (Throwable ignored) {
            return normalized
                    .replace("&nbsp;", " ")
                    .replace("&#160;", " ")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .trim();
        }
    }

    private static String findFirstGroup(Pattern pattern, String value) {
        if (pattern == null || value == null || value.isEmpty()) return "";
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find() || matcher.groupCount() < 1) return "";
        String group = matcher.group(1);
        return group == null ? "" : group.trim();
    }

    private static void parseGitHubReleaseAtomRegex(String atom,
                                                    String repoPath,
                                                    List<ReleaseFeedEntry> entries,
                                                    HashSet<String> seen) {
        Matcher entryMatcher = ATOM_ENTRY_PATTERN.matcher(atom);
        while (entryMatcher.find()) {
            String entryBlock = entryMatcher.group(1);
            String updatedAt = findFirstGroup(ATOM_UPDATED_PATTERN, entryBlock);
            String content = findFirstGroup(ATOM_CONTENT_PATTERN, entryBlock);
            String alternateLink = findAlternateLink(entryBlock);
            String tag = extractReleaseTagFromLink(alternateLink, repoPath);
            if (!tag.isEmpty() && seen.add(tag)) {
                entries.add(new ReleaseFeedEntry(tag, updatedAt, sanitizeReleaseNotes(content)));
            }
        }
    }

    private static String findAlternateLink(String entryBlock) {
        Matcher linkMatcher = ATOM_LINK_TAG_PATTERN.matcher(entryBlock == null ? "" : entryBlock);
        while (linkMatcher.find()) {
            String attributes = linkMatcher.group(1);
            if (attributes == null) continue;
            String normalized = attributes.toLowerCase(Locale.US);
            if (!normalized.contains("rel=\"alternate\"") && !normalized.contains("rel='alternate'")) continue;
            String href = findFirstGroup(ATOM_HREF_ATTR_PATTERN, attributes);
            if (!href.isEmpty()) return href;
        }
        return "";
    }

    private static String optString(JsonObject object, String key) {
        return optString(object, key, "");
    }

    private static String optString(JsonObject object, String key, String fallback) {
        if (object == null || key == null || !object.has(key)) return fallback == null ? "" : fallback;
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return fallback == null ? "" : fallback;
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return fallback == null ? "" : fallback;
        }
    }

    private static boolean optBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || key == null || !object.has(key)) return fallback;
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
