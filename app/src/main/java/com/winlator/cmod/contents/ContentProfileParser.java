package com.winlator.cmod.contents;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class ContentProfileParser {
    private ContentProfileParser() {
    }

    @Nullable
    static ContentProfile parse(@Nullable String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) return null;
        try {
            ContentProfile profile = new ContentProfile();
            JSONObject profileJSONObject = new JSONObject(rawJson);
            String typeName = profileJSONObject.optString(
                    ContentProfile.MARK_TYPE,
                    profileJSONObject.optString("contentType", "")
            );
            ContentProfile.ContentType resolvedType = ContentProfile.ContentType.getTypeByName(typeName);
            if (resolvedType == null) return null;

            profile.type = resolvedType;
            profile.verName = profileJSONObject.optString(
                    ContentProfile.MARK_VERSION_NAME,
                    profileJSONObject.optString("verName", profileJSONObject.optString("versionName", ""))
            );
            profile.verCode = parseOptionalInt(
                    profileJSONObject.opt(ContentProfile.MARK_VERSION_CODE),
                    parseOptionalInt(profileJSONObject.opt("verCode"), 0)
            );
            profile.desc = profileJSONObject.optString(ContentProfile.MARK_DESC, profileJSONObject.optString("name", ""));
            profile.channel = profileJSONObject.optString(ContentProfile.MARK_CHANNEL, ContentProfile.CHANNEL_STABLE);
            profile.delivery = profileJSONObject.optString(ContentProfile.MARK_DELIVERY, ContentProfile.DELIVERY_EMBEDDED);
            profile.displayCategory = profileJSONObject.optString(ContentProfile.MARK_DISPLAY_CATEGORY, "");
            profile.sourceRepo = profileJSONObject.optString(ContentProfile.MARK_SOURCE_REPO, "");
            profile.sourceFeed = profileJSONObject.optString(ContentProfile.MARK_SOURCE_FEED, "");
            profile.sourceLabel = profileJSONObject.optString(ContentProfile.MARK_SOURCE_LABEL, "");
            profile.releaseTag = profileJSONObject.optString(ContentProfile.MARK_RELEASE_TAG, "");
            profile.artifactName = profileJSONObject.optString(ContentProfile.MARK_ARTIFACT_NAME, "");
            profile.publishedAt = profileJSONObject.optString(ContentProfile.MARK_PUBLISHED_AT, "");
            profile.releaseNotes = profileJSONObject.optString(ContentProfile.MARK_RELEASE_NOTES, "");
            profile.runtimeModel = readRuntimeModelHint(profileJSONObject);
            profile.vulkanApiMin = profileJSONObject.optInt(ContentProfile.MARK_VULKAN_API_MIN, 0);
            profile.vulkanApiMax = profileJSONObject.optInt(ContentProfile.MARK_VULKAN_API_MAX, 0);
            profile.remoteSha256 = normalizeSha256(profileJSONObject.optString(ContentProfile.MARK_SHA256, ""));
            profile.setInstalledLocally(true);

            List<ContentProfile.ContentFile> fileList = new ArrayList<>();
            JSONArray fileJSONArray = profileJSONObject.optJSONArray(ContentProfile.MARK_FILE_LIST);
            if (fileJSONArray == null) {
                fileJSONArray = profileJSONObject.optJSONArray("fileList");
            }
            if (fileJSONArray != null) {
                for (int i = 0; i < fileJSONArray.length(); i++) {
                    JSONObject contentFileJSONObject = fileJSONArray.optJSONObject(i);
                    if (contentFileJSONObject == null) continue;
                    ContentProfile.ContentFile contentFile = new ContentProfile.ContentFile();
                    contentFile.source = firstNonBlank(
                            contentFileJSONObject.optString(ContentProfile.MARK_FILE_SOURCE, ""),
                            contentFileJSONObject.optString("src", ""),
                            contentFileJSONObject.optString("sourcePath", ""),
                            contentFileJSONObject.optString("path", "")
                    );
                    contentFile.target = firstNonBlank(
                            contentFileJSONObject.optString(ContentProfile.MARK_FILE_TARGET, ""),
                            contentFileJSONObject.optString("dst", ""),
                            contentFileJSONObject.optString("targetPath", ""),
                            contentFileJSONObject.optString("target", "")
                    );
                    if (contentFile.source.isEmpty() || contentFile.target.isEmpty()) continue;
                    fileList.add(contentFile);
                }
            }
            profile.fileList = fileList;

            if (resolvedType == ContentProfile.ContentType.CONTENT_TYPE_WINE
                    || resolvedType == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
                JSONObject runtimeJSONObject = profileJSONObject.optJSONObject(ContentProfile.MARK_PROTON);
                if (runtimeJSONObject == null) {
                    runtimeJSONObject = profileJSONObject.optJSONObject(ContentProfile.MARK_WINE);
                }
                profile.wineLibPath = firstNonBlank(
                        runtimeJSONObject != null ? runtimeJSONObject.optString(ContentProfile.MARK_WINE_LIBPATH, "") : "",
                        profileJSONObject.optString(ContentProfile.MARK_WINE_LIBPATH, ""),
                        profileJSONObject.optString("libPath", "")
                );
                profile.wineBinPath = firstNonBlank(
                        runtimeJSONObject != null ? runtimeJSONObject.optString(ContentProfile.MARK_WINE_BINPATH, "") : "",
                        profileJSONObject.optString(ContentProfile.MARK_WINE_BINPATH, ""),
                        profileJSONObject.optString("binPath", "")
                );
                profile.winePrefixPack = firstNonBlank(
                        runtimeJSONObject != null ? runtimeJSONObject.optString(ContentProfile.MARK_WINE_PREFIX_PACK, "") : "",
                        profileJSONObject.optString(ContentProfile.MARK_WINE_PREFIX_PACK, ""),
                        profileJSONObject.optString("prefixPack", "")
                );
                if (profile.wineLibPath.isEmpty() && profile.wineBinPath.isEmpty() && profile.winePrefixPack.isEmpty()) {
                    return null;
                }
                profile.runtimeModel = profile.getRuntimeModel();
            } else if (fileList.isEmpty()) {
                return null;
            }
            return profile;
        } catch (Exception e) {
            return null;
        }
    }

    private static String readRuntimeModelHint(JSONObject profileJSONObject) {
        String explicit = ContentProfile.normalizeRuntimeModel(
                profileJSONObject.optString(ContentProfile.MARK_RUNTIME_MODEL, "")
        );
        if (!explicit.isEmpty()) return explicit;

        JSONObject proton = profileJSONObject.optJSONObject(ContentProfile.MARK_PROTON);
        if (proton != null) {
            explicit = ContentProfile.normalizeRuntimeModel(proton.optString(ContentProfile.MARK_RUNTIME_MODEL, ""));
            if (!explicit.isEmpty()) return explicit;
        }

        JSONObject wine = profileJSONObject.optJSONObject(ContentProfile.MARK_WINE);
        if (wine != null) {
            explicit = ContentProfile.normalizeRuntimeModel(wine.optString(ContentProfile.MARK_RUNTIME_MODEL, ""));
            if (!explicit.isEmpty()) return explicit;
        }
        return "";
    }

    private static int parseOptionalInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            String normalized = value.toString().trim();
            return normalized.isEmpty() ? defaultValue : Integer.parseInt(normalized);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String normalizeSha256(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase().replaceAll("[^0-9a-f]", "");
        return normalized.length() == 64 ? normalized : "";
    }
}
