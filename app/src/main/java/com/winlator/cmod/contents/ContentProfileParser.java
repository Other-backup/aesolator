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
            String typeName = firstNonBlank(
                    optString(profileJSONObject, ContentProfile.MARK_TYPE),
                    optString(profileJSONObject, "contentType"),
                    optString(profileJSONObject, "componentType"),
                    optString(profileJSONObject, "packageType"),
                    optString(profileJSONObject, "profileType"),
                    optString(profileJSONObject, "category")
            );
            if (typeName.isEmpty()) {
                if (profileJSONObject.optJSONObject(ContentProfile.MARK_PROTON) != null) typeName = "Proton";
                else if (profileJSONObject.optJSONObject(ContentProfile.MARK_WINE) != null) typeName = "Wine";
            }
            ContentProfile.ContentType resolvedType = ContentProfile.ContentType.getTypeByName(typeName);
            if (resolvedType == null) return null;

            profile.type = resolvedType;
            profile.verName = firstNonBlank(
                    optString(profileJSONObject, ContentProfile.MARK_VERSION_NAME),
                    optString(profileJSONObject, "verName"),
                    optString(profileJSONObject, "version"),
                    optString(profileJSONObject, "version_name"),
                    optString(profileJSONObject, "version-name"),
                    optString(profileJSONObject, "displayVersion"),
                    optString(profileJSONObject, "buildVersion"),
                    optString(profileJSONObject, "releaseName"),
                    optString(profileJSONObject, "name")
            );
            profile.verCode = firstOptionalInt(
                    profileJSONObject,
                    0,
                    ContentProfile.MARK_VERSION_CODE,
                    "verCode",
                    "version_code",
                    "code",
                    "build"
            );
            profile.desc = firstNonBlank(
                    optString(profileJSONObject, ContentProfile.MARK_DESC),
                    optString(profileJSONObject, "desc"),
                    optString(profileJSONObject, "summary"),
                    optString(profileJSONObject, "title"),
                    optString(profileJSONObject, "name")
            );
            profile.channel = firstNonBlank(optString(profileJSONObject, ContentProfile.MARK_CHANNEL), ContentProfile.CHANNEL_STABLE);
            profile.delivery = firstNonBlank(optString(profileJSONObject, ContentProfile.MARK_DELIVERY), optString(profileJSONObject, "deliveryMode"), ContentProfile.DELIVERY_EMBEDDED);
            profile.displayCategory = firstNonBlank(optString(profileJSONObject, ContentProfile.MARK_DISPLAY_CATEGORY), optString(profileJSONObject, "display_category"));
            profile.sourceRepo = firstNonBlank(optString(profileJSONObject, ContentProfile.MARK_SOURCE_REPO), optString(profileJSONObject, "source_repo"), optString(profileJSONObject, "repo"), optString(profileJSONObject, "repository"));
            profile.sourceFeed = firstNonBlank(optString(profileJSONObject, ContentProfile.MARK_SOURCE_FEED), optString(profileJSONObject, "source_feed"), optString(profileJSONObject, "feed"));
            profile.sourceLabel = firstNonBlank(optString(profileJSONObject, ContentProfile.MARK_SOURCE_LABEL), optString(profileJSONObject, "source_label"), optString(profileJSONObject, "source"));
            profile.releaseTag = firstNonBlank(optString(profileJSONObject, ContentProfile.MARK_RELEASE_TAG), optString(profileJSONObject, "release_tag"), optString(profileJSONObject, "tag"), optString(profileJSONObject, "tagName"));
            profile.artifactName = firstNonBlank(optString(profileJSONObject, ContentProfile.MARK_ARTIFACT_NAME), optString(profileJSONObject, "artifact"), optString(profileJSONObject, "assetName"), optString(profileJSONObject, "fileName"));
            profile.publishedAt = firstNonBlank(optString(profileJSONObject, ContentProfile.MARK_PUBLISHED_AT), optString(profileJSONObject, "published_at"), optString(profileJSONObject, "createdAt"), optString(profileJSONObject, "date"));
            profile.releaseNotes = firstNonBlank(optString(profileJSONObject, ContentProfile.MARK_RELEASE_NOTES), optString(profileJSONObject, "release_notes"), optString(profileJSONObject, "notes"), optString(profileJSONObject, "changelog"));
            profile.runtimeModel = readRuntimeModelHint(profileJSONObject);
            profile.vulkanApiMin = profileJSONObject.optInt(ContentProfile.MARK_VULKAN_API_MIN, 0);
            profile.vulkanApiMax = profileJSONObject.optInt(ContentProfile.MARK_VULKAN_API_MAX, 0);
            profile.remoteSha256 = normalizeSha256(firstNonBlank(
                    optString(profileJSONObject, ContentProfile.MARK_SHA256),
                    optString(profileJSONObject, "sha"),
                    optString(profileJSONObject, "checksum"),
                    optString(profileJSONObject, "digest")
            ));
            profile.setInstalledLocally(true);

            List<ContentProfile.ContentFile> fileList = new ArrayList<>();
            JSONArray fileJSONArray = firstArray(profileJSONObject,
                    ContentProfile.MARK_FILE_LIST,
                    "fileList",
                    "installFiles",
                    "installedFiles",
                    "payloadFiles",
                    "payloads",
                    "artifacts"
            );
            if (fileJSONArray != null) {
                for (int i = 0; i < fileJSONArray.length(); i++) {
                    JSONObject contentFileJSONObject = fileJSONArray.optJSONObject(i);
                    if (contentFileJSONObject == null) continue;
                    ContentProfile.ContentFile contentFile = new ContentProfile.ContentFile();
                    contentFile.source = firstNonBlank(
                            contentFileJSONObject.optString(ContentProfile.MARK_FILE_SOURCE, ""),
                            contentFileJSONObject.optString("src", ""),
                            contentFileJSONObject.optString("sourcePath", ""),
                            contentFileJSONObject.optString("path", ""),
                            contentFileJSONObject.optString("file", ""),
                            contentFileJSONObject.optString("fileName", ""),
                            contentFileJSONObject.optString("from", ""),
                            contentFileJSONObject.optString("relativePath", "")
                    );
                    contentFile.target = firstNonBlank(
                            contentFileJSONObject.optString(ContentProfile.MARK_FILE_TARGET, ""),
                            contentFileJSONObject.optString("dst", ""),
                            contentFileJSONObject.optString("targetPath", ""),
                            contentFileJSONObject.optString("target", ""),
                            contentFileJSONObject.optString("destination", ""),
                            contentFileJSONObject.optString("installPath", ""),
                            contentFileJSONObject.optString("to", "")
                    );
                    if (contentFile.source.isEmpty() || contentFile.target.isEmpty()) continue;
                    fileList.add(contentFile);
                }
            }
            profile.fileList = fileList;

            if (resolvedType == ContentProfile.ContentType.CONTENT_TYPE_WINE
                    || resolvedType == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
                JSONObject runtimeJSONObject = firstObject(profileJSONObject,
                        ContentProfile.MARK_PROTON,
                        ContentProfile.MARK_WINE,
                        "runtime",
                        "payload",
                        "wineRuntime"
                );
                profile.wineLibPath = firstNonBlank(
                        runtimeJSONObject != null ? runtimeJSONObject.optString(ContentProfile.MARK_WINE_LIBPATH, "") : "",
                        runtimeJSONObject != null ? runtimeJSONObject.optString("wineLibPath", "") : "",
                        runtimeJSONObject != null ? runtimeJSONObject.optString("lib", "") : "",
                        runtimeJSONObject != null ? runtimeJSONObject.optString("libs", "") : "",
                        runtimeJSONObject != null ? runtimeJSONObject.optString("libDir", "") : "",
                        runtimeJSONObject != null ? runtimeJSONObject.optString("libraryPath", "") : "",
                        profileJSONObject.optString(ContentProfile.MARK_WINE_LIBPATH, ""),
                        profileJSONObject.optString("wineLibPath", ""),
                        profileJSONObject.optString("libPath", ""),
                        profileJSONObject.optString("lib", ""),
                        profileJSONObject.optString("libs", ""),
                        profileJSONObject.optString("libDir", "")
                );
                profile.wineBinPath = firstNonBlank(
                        runtimeJSONObject != null ? runtimeJSONObject.optString(ContentProfile.MARK_WINE_BINPATH, "") : "",
                        runtimeJSONObject != null ? runtimeJSONObject.optString("wineBinPath", "") : "",
                        runtimeJSONObject != null ? runtimeJSONObject.optString("bin", "") : "",
                        runtimeJSONObject != null ? runtimeJSONObject.optString("binDir", "") : "",
                        runtimeJSONObject != null ? runtimeJSONObject.optString("binaryPath", "") : "",
                        profileJSONObject.optString(ContentProfile.MARK_WINE_BINPATH, ""),
                        profileJSONObject.optString("wineBinPath", ""),
                        profileJSONObject.optString("binPath", ""),
                        profileJSONObject.optString("bin", ""),
                        profileJSONObject.optString("binDir", ""),
                        profileJSONObject.optString("bindir", "")
                );
                profile.winePrefixPack = firstNonBlank(
                        runtimeJSONObject != null ? runtimeJSONObject.optString(ContentProfile.MARK_WINE_PREFIX_PACK, "") : "",
                        runtimeJSONObject != null ? runtimeJSONObject.optString("prefixPackPath", "") : "",
                        runtimeJSONObject != null ? runtimeJSONObject.optString("prefix_pack", "") : "",
                        runtimeJSONObject != null ? runtimeJSONObject.optString("prefix", "") : "",
                        runtimeJSONObject != null ? runtimeJSONObject.optString("prefixArchive", "") : "",
                        profileJSONObject.optString(ContentProfile.MARK_WINE_PREFIX_PACK, ""),
                        profileJSONObject.optString("prefixPackPath", ""),
                        profileJSONObject.optString("prefix_pack", ""),
                        profileJSONObject.optString("prefix", ""),
                        profileJSONObject.optString("prefixArchive", "")
                );
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
        String explicit = firstNonBlank(
                profileJSONObject.optString(ContentProfile.MARK_RUNTIME_MODEL, ""),
                profileJSONObject.optString("runtime_model", ""),
                profileJSONObject.optString("runtimeClass", ""),
                profileJSONObject.optString("runtimeClassTarget", ""),
                profileJSONObject.optString("runtimeClassDetected", ""),
                profileJSONObject.optString("runtime", ""),
                profileJSONObject.optString("targetRuntime", "")
        );
        explicit = ContentProfile.normalizeRuntimeModel(explicit);
        if (!explicit.isEmpty()) return explicit;

        JSONObject proton = firstObject(profileJSONObject, ContentProfile.MARK_PROTON, "runtime", "payload", "wineRuntime");
        if (proton != null) {
            explicit = ContentProfile.normalizeRuntimeModel(firstNonBlank(
                    proton.optString(ContentProfile.MARK_RUNTIME_MODEL, ""),
                    proton.optString("runtime_model", ""),
                    proton.optString("runtimeClass", ""),
                    proton.optString("runtimeClassTarget", ""),
                    proton.optString("runtimeClassDetected", ""),
                    proton.optString("target", ""),
                    proton.optString("model", "")
            ));
            if (!explicit.isEmpty()) return explicit;
        }

        JSONObject wine = profileJSONObject.optJSONObject(ContentProfile.MARK_WINE);
        if (wine != null) {
            explicit = ContentProfile.normalizeRuntimeModel(firstNonBlank(
                    wine.optString(ContentProfile.MARK_RUNTIME_MODEL, ""),
                    wine.optString("runtimeClassTarget", ""),
                    wine.optString("runtimeClassDetected", ""),
                    wine.optString("target", "")
            ));
            if (!explicit.isEmpty()) return explicit;
        }
        return "";
    }

    private static JSONArray firstArray(JSONObject object, String... keys) {
        if (object == null || keys == null) return null;
        for (String key : keys) {
            if (key == null || key.isEmpty()) continue;
            JSONArray array = object.optJSONArray(key);
            if (array != null) return array;
        }
        return null;
    }

    private static JSONObject firstObject(JSONObject object, String... keys) {
        if (object == null || keys == null) return null;
        for (String key : keys) {
            if (key == null || key.isEmpty()) continue;
            JSONObject nested = object.optJSONObject(key);
            if (nested != null) return nested;
        }
        return null;
    }

    private static String optString(JSONObject object, String key) {
        if (object == null || key == null || key.isEmpty()) return "";
        Object value = object.opt(key);
        if (value == null || value == JSONObject.NULL) return "";
        if (value instanceof JSONObject || value instanceof JSONArray) return "";
        return value.toString();
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

    private static int firstOptionalInt(JSONObject object, int defaultValue, String... keys) {
        if (object == null || keys == null) return defaultValue;
        for (String key : keys) {
            if (key == null || key.isEmpty() || !object.has(key)) continue;
            return parseOptionalInt(object.opt(key), defaultValue);
        }
        return defaultValue;
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
