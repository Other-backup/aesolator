package com.winlator.cmod.contents;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.TarCompressorUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ContentsManager {
    public static final String PROFILE_NAME = "profile.json";
    public static final String REMOTE_PROFILES = "https://raw.githubusercontent.com/Arihany/WinlatorWCPHub/main/pack.json";
    public static final String REMOTE_PROFILES_FALLBACK = "https://raw.githubusercontent.com/StevenMXZ/Winlator-Contents/main/contents.json";
    public static final String REMOTE_PROFILES_AE = "https://raw.githubusercontent.com/kosoymiki/wcp-graphics-lanes/main/contents/contents.json";
    public static final String REMOTE_WINE_PROTON_OVERLAY = REMOTE_PROFILES_AE;
    public static final String[] DXVK_TRUST_FILES = {"${system32}/d3d8.dll", "${system32}/d3d9.dll", "${system32}/d3d10.dll", "${system32}/d3d10_1.dll",
            "${system32}/d3d10core.dll", "${system32}/d3d11.dll", "${system32}/dxgi.dll", "${syswow64}/d3d8.dll", "${syswow64}/d3d9.dll", "${syswow64}/d3d10.dll",
            "${syswow64}/d3d10_1.dll", "${syswow64}/d3d10core.dll", "${syswow64}/d3d11.dll", "${syswow64}/dxgi.dll"};
    public static final String[] VKD3D_TRUST_FILES = {"${system32}/d3d12core.dll", "${system32}/d3d12.dll",
            "${syswow64}/d3d12core.dll", "${syswow64}/d3d12.dll"};
    public static final String[] BOX64_TRUST_FILES = {"${bindir}/box64"};
    public static final String[] WOWBOX64_TRUST_FILES = {"${system32}/wowbox64.dll"};
    public static final String[] FEXCORE_TRUST_FILES = {"${system32}/libwow64fex.dll", "${system32}/libarm64ecfex.dll"};
    public static final String[] VULKAN_SDK_TRUST_PREFIXES = {"${sharedir}/vulkan", "${sharedir}/vulkan-sdk"};
    private static final String[] CONTENT_ARCHIVE_SUFFIXES = {
            ".wcp", ".zip", ".tar", ".txz", ".tzst", ".tar.xz", ".tar.zst"
    };
    private static final String INSTALL_STAGE_MARKER_SUFFIX = ".install-stage.json";
    private Map<String, String> dirTemplateMap;
    private Map<ContentProfile.ContentType, List<String>> trustedFilesMap;

    private SharedPreferences preferences;

    public enum InstallFailedReason {
        ERROR_NOSPACE,
        ERROR_BADTAR,
        ERROR_NOPROFILE,
        ERROR_BADPROFILE,
        ERROR_MISSINGFILES,
        ERROR_EXIST,
        ERROR_UNTRUSTPROFILE,
        ERROR_UNKNOWN
    }

    public enum ContentDirName {
        CONTENT_MAIN_DIR_NAME("contents"),
        CONTENT_WINE_DIR_NAME("wine"),
        CONTENT_DXVK_DIR_NAME("dxvk"),
        CONTENT_VKD3D_DIR_NAME("vkd3d"),
        CONTENT_BOX64_DIR_NAME("box64");

        private String name;

        ContentDirName(String name) {
            this.name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    private final Context context;

    private HashMap<ContentProfile.ContentType, List<ContentProfile>> profilesMap;

    private ArrayList<ContentProfile> remoteProfiles;

    public ContentsManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences("contents_manager_prefs", Context.MODE_PRIVATE);
    }

    // Method to mark the graphics driver as installed
    public void setGraphicsDriverInstalled(String driverVersion, boolean installed) {
        preferences.edit().putBoolean("graphics_driver_installed_" + driverVersion, installed).apply();
    }

    public interface OnInstallFinishedCallback {
        void onFailed(InstallFailedReason reason, Exception e);

        void onSucceed(ContentProfile profile);
    }

    public void setRemoteProfiles(String json) {
        setRemoteProfiles(json, false, false);
    }

    public void setRemoteProfiles(String json, boolean includeBeta, boolean ignoreRepoManaged) {
        remoteProfiles = new ArrayList<>();
        appendRemoteProfiles(json, includeBeta, ignoreRepoManaged, false, false);
        syncContents();
    }

    public void setCompositeRemoteProfiles(String hubJson, String repoOverlayJson, boolean showNightlyOnly) {
        remoteProfiles = new ArrayList<>();
        // Hub feed: keep all channels available in-memory, UI can filter by toggles.
        appendRemoteProfiles(hubJson, showNightlyOnly, true, false, true);
        // Overlay feed: repo-managed lanes only.
        appendRemoteProfiles(repoOverlayJson, false, false, true, false);
        syncContents();
    }

    private void appendRemoteProfiles(String json, boolean includeBeta, boolean ignoreRepoManaged, boolean onlyRepoManaged, boolean keepAllChannels) {
        if (json == null || json.trim().isEmpty()) return;
        try {
            JSONArray content = new JSONArray(json);
            for (int i = 0; i < content.length(); i++) {
                try {
                    JSONObject object = content.getJSONObject(i);
                    ContentProfile remoteProfile = new ContentProfile();
                    remoteProfile.remoteUrl = readRemoteUrl(object);
                    if (remoteProfile.remoteUrl == null || remoteProfile.remoteUrl.isEmpty()) continue;
                    remoteProfile.remoteSha256 = readRemoteSha256(object);

                    remoteProfile.type = ContentProfile.ContentType.getTypeByName(object.optString("type"));
                    if (remoteProfile.type == ContentProfile.ContentType.CONTENT_TYPE_WINE) {
                        String internalType = object.optString("internalType", "").trim().toLowerCase(Locale.US);
                        if (internalType.contains("proton")) {
                            remoteProfile.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
                        }
                    }
                    if (remoteProfile.type == null) continue;

                    if (onlyRepoManaged && !isRepoManagedOverlayType(remoteProfile.type)) continue;
                    if (ignoreRepoManaged && isRepoManagedOverlayType(remoteProfile.type)) continue;

                    remoteProfile.verName = object.optString("verName", "").trim();
                    remoteProfile.verCode = parseVerCode(object);
                    remoteProfile.desc = object.optString("description", "").trim();
                    remoteProfile.displayCategory = object.optString(ContentProfile.MARK_DISPLAY_CATEGORY, "").trim();
                    remoteProfile.sourceRepo = object.optString(ContentProfile.MARK_SOURCE_REPO, "").trim();
                    remoteProfile.releaseTag = object.optString(ContentProfile.MARK_RELEASE_TAG, "").trim();
                    remoteProfile.vulkanApiMin = parseOptionalInt(object.opt(ContentProfile.MARK_VULKAN_API_MIN), 0);
                    remoteProfile.vulkanApiMax = parseOptionalInt(object.opt(ContentProfile.MARK_VULKAN_API_MAX), 0);
                    remoteProfile.vulkanSdkVersion = object.optString(ContentProfile.MARK_VULKAN_SDK_VERSION, "").trim();
                    remoteProfile.delivery = object.optString(ContentProfile.MARK_DELIVERY, ContentProfile.DELIVERY_REMOTE).trim();
                    remoteProfile.channel = object.optString(ContentProfile.MARK_CHANNEL, "").trim().toLowerCase(Locale.US);
                    remoteProfile.locallyInstalled = false;

                    if (remoteProfile.verName.isEmpty()) {
                        remoteProfile.verName = deriveVersionNameFromUrl(remoteProfile.remoteUrl);
                    }
                    if (remoteProfile.desc.isEmpty()) {
                        remoteProfile.desc = object.optString("name", "").trim();
                    }
                    if (remoteProfile.desc.isEmpty()) {
                        remoteProfile.desc = remoteProfile.verName != null ? remoteProfile.verName : "";
                    }
                    if (remoteProfile.channel.isEmpty()) {
                        remoteProfile.channel = deriveLegacyChannel(object, remoteProfile.verName, remoteProfile.remoteUrl);
                    }

                    boolean isBeta = ContentProfile.CHANNEL_BETA.equals(remoteProfile.channel)
                            || ContentProfile.CHANNEL_NIGHTLY.equals(remoteProfile.channel);
                    if (!keepAllChannels) {
                        if (includeBeta && !isBeta) continue;
                        if (!includeBeta && isBeta) continue;
                    }

                    remoteProfiles.add(remoteProfile);
                } catch (Exception e) {
                    Log.w("ContentsManager", "Failed to parse remote profile row", e);
                }
            }
        } catch (JSONException e) {
            Log.w("ContentsManager", "Failed to parse remote profile feed", e);
        }
    }

    public void syncContents() {
        profilesMap = new HashMap<>();
        for (ContentProfile.ContentType type : ContentProfile.ContentType.values()) {
            profilesMap.put(type, new LinkedList<>());
        }

        for (ContentProfile.ContentType type : ContentProfile.ContentType.values()) {
            List<ContentProfile> profiles = profilesMap.get(type);
            HashMap<String, ContentProfile> profileByEntry = new HashMap<>();

            File typeFile = getContentTypeDir(context, type);
            File[] fileList = typeFile.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    File proFile = new File(file, PROFILE_NAME);
                    if (proFile.exists() && proFile.isFile()) {
                        ContentProfile profile = readProfile(proFile);
                        if (profile != null) {
                            profile.locallyInstalled = true;
                            profiles.add(profile);
                            profileByEntry.put(getEntryName(profile), profile);
                        }
                    }
                }
            }

            if (remoteProfiles != null) {
                for (ContentProfile remote : remoteProfiles) {
                    if (remote.type != type) continue;
                    ContentProfile existing = profileByEntry.get(getEntryName(remote));
                    if (existing == null) {
                        existing = findEquivalentProfile(profiles, remote);
                    }
                    if (existing != null) {
                        existing.mergeRemoteMetadata(remote);
                    } else {
                        profiles.add(remote);
                        profileByEntry.put(getEntryName(remote), remote);
                    }
                }
            }
        }
    }

    public void extraContentFile(Uri uri, OnInstallFinishedCallback callback) {
        cleanTmpDir(context);

        File file = getTmpDir(context);

        boolean ret;
        ret = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, context, uri, file);
        if (!ret)
            ret = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, uri, file);
        if (!ret) {
            callback.onFailed(InstallFailedReason.ERROR_BADTAR, null);
            return;
        }

        File proFile = new File(file, PROFILE_NAME);
        if (!proFile.exists()) {
            callback.onFailed(InstallFailedReason.ERROR_NOPROFILE, null);
            return;
        }

        ContentProfile profile = readProfile(proFile);
        if (profile == null) {
            callback.onFailed(InstallFailedReason.ERROR_BADPROFILE, null);
            return;
        }

        String imagefsPath = context.getFilesDir().getAbsolutePath() + "/imagefs";
        for (ContentProfile.ContentFile contentFile : profile.fileList) {
            File tmpFile = new File(file, contentFile.source);
            if (!tmpFile.exists() || !tmpFile.isFile() || !isSubPath(file.getAbsolutePath(), tmpFile.getAbsolutePath())) {
                callback.onFailed(InstallFailedReason.ERROR_MISSINGFILES, null);
                return;
            }

            String realPath = getPathFromTemplate(contentFile.target);
            if (!isSubPath(imagefsPath, realPath) || isSubPath(ContentsManager.getContentDir(context).getAbsolutePath(), realPath) || realPath.contains("dosdevices")) {
                callback.onFailed(InstallFailedReason.ERROR_UNTRUSTPROFILE, null);
                return;
            }
        }

        if (profile.isWineProtonFamily()) {
            File bin = new File(file, profile.wineBinPath);
            File lib = new File(file, profile.wineLibPath);
            File cp = new File(file, profile.winePrefixPack);

            if (!bin.exists() || !bin.isDirectory() || !lib.exists() || !lib.isDirectory() || !cp.exists() || !cp.isFile()) {
                callback.onFailed(InstallFailedReason.ERROR_MISSINGFILES, null);
                return;
            }
        }

        callback.onSucceed(profile);
    }

    public void finishInstallContent(ContentProfile profile, OnInstallFinishedCallback callback) {
        File installPath = getInstallDir(context, profile);
        File tmpPath = getTmpDir(context);
        if (!tmpPath.exists() || !tmpPath.isDirectory()) {
            callback.onFailed(InstallFailedReason.ERROR_UNKNOWN, null);
            return;
        }

        File typeDir = installPath.getParentFile();
        if (typeDir == null || (!typeDir.exists() && !typeDir.mkdirs())) {
            callback.onFailed(InstallFailedReason.ERROR_UNKNOWN, null);
            return;
        }

        recoverInterruptedInstall(typeDir, installPath);
        File stageMarker = getInstallStageMarker(typeDir, installPath);
        clearInstallStageMarker(stageMarker);

        File backupPath = null;
        if (installPath.exists()) {
            if (!isUpdatableLane(profile.type)) {
                callback.onFailed(InstallFailedReason.ERROR_EXIST, null);
                return;
            }
            backupPath = new File(typeDir, installPath.getName() + ".bak-" + UUID.randomUUID().toString().replace("-", ""));
            if (!installPath.renameTo(backupPath)) {
                callback.onFailed(InstallFailedReason.ERROR_UNKNOWN, null);
                return;
            }
            if (!writeInstallStageMarker(stageMarker, installPath, backupPath)) {
                backupPath.renameTo(installPath);
                callback.onFailed(InstallFailedReason.ERROR_UNKNOWN, null);
                return;
            }
        }

        boolean moved = tmpPath.renameTo(installPath);
        if (!moved) {
            // Fallback to recursive copy for filesystems where renameTo can fail.
            moved = FileUtils.copy(tmpPath, installPath);
            if (moved) {
                FileUtils.delete(tmpPath);
            }
        }

        if (!moved) {
            if (backupPath != null && backupPath.exists() && !installPath.exists()) {
                backupPath.renameTo(installPath);
            }
            clearInstallStageMarker(stageMarker);
            callback.onFailed(InstallFailedReason.ERROR_UNKNOWN, null);
            return;
        }

        if (backupPath != null && backupPath.exists()) {
            FileUtils.delete(backupPath);
        }
        clearInstallStageMarker(stageMarker);

        callback.onSucceed(profile);
    }

    private File getInstallStageMarker(File typeDir, File installPath) {
        return new File(typeDir, installPath.getName() + INSTALL_STAGE_MARKER_SUFFIX);
    }

    private boolean writeInstallStageMarker(File markerFile, File installPath, File backupPath) {
        try {
            JSONObject marker = new JSONObject();
            marker.put("target", installPath.getAbsolutePath());
            marker.put("backup", backupPath.getAbsolutePath());
            marker.put("ts", System.currentTimeMillis());
            return FileUtils.writeString(markerFile, marker.toString());
        } catch (Exception e) {
            return false;
        }
    }

    private void clearInstallStageMarker(File markerFile) {
        if (markerFile != null && markerFile.exists()) {
            markerFile.delete();
        }
    }

    private void recoverInterruptedInstall(File typeDir, File installPath) {
        File markerFile = getInstallStageMarker(typeDir, installPath);
        if (!markerFile.isFile()) return;
        try {
            JSONObject marker = new JSONObject(FileUtils.readString(markerFile));
            File target = new File(marker.optString("target", installPath.getAbsolutePath()));
            File backup = new File(marker.optString("backup", ""));
            if (!backup.exists()) {
                clearInstallStageMarker(markerFile);
                return;
            }
            if (target.exists()) {
                FileUtils.delete(backup);
                clearInstallStageMarker(markerFile);
                return;
            }
            if (backup.renameTo(target)) {
                clearInstallStageMarker(markerFile);
            }
        } catch (Exception ignored) {
        }
    }

    public ContentProfile readProfile(File file) {
        try {
            ContentProfile profile = new ContentProfile();
            JSONObject profileJSONObject = new JSONObject(FileUtils.readString(file));
            String typeName = profileJSONObject.optString(ContentProfile.MARK_TYPE, profileJSONObject.optString("contentType", ""));
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
            profile.releaseTag = profileJSONObject.optString(ContentProfile.MARK_RELEASE_TAG, "");
            profile.vulkanApiMin = profileJSONObject.optInt(ContentProfile.MARK_VULKAN_API_MIN, 0);
            profile.vulkanApiMax = profileJSONObject.optInt(ContentProfile.MARK_VULKAN_API_MAX, 0);
            profile.vulkanSdkVersion = profileJSONObject.optString(ContentProfile.MARK_VULKAN_SDK_VERSION, "");
            profile.remoteSha256 = normalizeSha256(profileJSONObject.optString(ContentProfile.MARK_SHA256, ""));
            profile.locallyInstalled = true;

            JSONArray fileJSONArray = profileJSONObject.optJSONArray(ContentProfile.MARK_FILE_LIST);
            if (fileJSONArray == null) {
                fileJSONArray = profileJSONObject.optJSONArray("fileList");
            }
            if (fileJSONArray == null) {
                return null;
            }
            List<ContentProfile.ContentFile> fileList = new ArrayList<>();
            for (int i = 0; i < fileJSONArray.length(); i++) {
                JSONObject contentFileJSONObject = fileJSONArray.getJSONObject(i);
                ContentProfile.ContentFile contentFile = new ContentProfile.ContentFile();
                contentFile.source = contentFileJSONObject.optString(ContentProfile.MARK_FILE_SOURCE, contentFileJSONObject.optString("src", ""));
                contentFile.target = contentFileJSONObject.optString(ContentProfile.MARK_FILE_TARGET, contentFileJSONObject.optString("dst", ""));
                if (contentFile.source.isEmpty() || contentFile.target.isEmpty()) continue;
                fileList.add(contentFile);
            }
            if (fileList.isEmpty()) return null;
            profile.fileList = fileList;

            if (resolvedType == ContentProfile.ContentType.CONTENT_TYPE_WINE
                    || resolvedType == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
                JSONObject wineJSONObject = profileJSONObject.optJSONObject(ContentProfile.MARK_PROTON);
                if (wineJSONObject == null) {
                    wineJSONObject = profileJSONObject.optJSONObject(ContentProfile.MARK_WINE);
                }
                if (wineJSONObject != null) {
                    profile.wineLibPath = wineJSONObject.optString(ContentProfile.MARK_WINE_LIBPATH, "");
                    profile.wineBinPath = wineJSONObject.optString(ContentProfile.MARK_WINE_BINPATH, "");
                    profile.winePrefixPack = wineJSONObject.optString(ContentProfile.MARK_WINE_PREFIX_PACK, "");
                }
            }
            return profile;
        } catch (Exception e) {
            return null;
        }
    }

    public List<ContentProfile> getProfiles(ContentProfile.ContentType type) {
        if (profilesMap != null)
            return profilesMap.get(type);
        return null;
    }

    public static File getInstallDir(Context context, ContentProfile profile) {
        return new File(getContentTypeDir(context, profile.type), profile.verName + "-" + profile.verCode);
    }

    public static File getContentDir(Context context) {
        return new File(context.getFilesDir(), ContentDirName.CONTENT_MAIN_DIR_NAME.toString());
    }

    public static File getContentTypeDir(Context context, ContentProfile.ContentType type) {
        return new File(getContentDir(context), type.toString());
    }

    public static File getTmpDir(Context context) {
        return new File(context.getFilesDir(), "tmp/" + ContentDirName.CONTENT_MAIN_DIR_NAME);
    }

    public static File getSourceFile(Context context, ContentProfile profile, String path) {
        return new File(getInstallDir(context, profile), path);
    }

    public static void cleanTmpDir(Context context) {
        File file = getTmpDir(context);
        FileUtils.delete(file);
        file.mkdirs();
    }

    public List<ContentProfile.ContentFile> getUnTrustedContentFiles(ContentProfile profile) {
        createTrustedFilesMap();
        List<ContentProfile.ContentFile> files = new ArrayList<>();
        for (ContentProfile.ContentFile contentFile : profile.fileList) {
            String normalizedTarget = Paths.get(getPathFromTemplate(contentFile.target)).toAbsolutePath().normalize().toString();
            if (!trustedFilesMap.get(profile.type).contains(normalizedTarget)
                    && !isTrustedByPrefix(profile.type, normalizedTarget)) {
                files.add(contentFile);
            }
        }
        return files;
    }

    private boolean isSubPath(String parent, String child) {
        return Paths.get(child).toAbsolutePath().normalize().startsWith(Paths.get(parent).toAbsolutePath().normalize());
    }

    private void createDirTemplateMap() {
        if (dirTemplateMap == null) {
            dirTemplateMap = new HashMap<>();
            String imagefsPath = context.getFilesDir().getAbsolutePath() + "/imagefs";
            String drivecPath = imagefsPath + "/home/xuser/.wine/drive_c";
            dirTemplateMap.put("${libdir}", imagefsPath + "/usr/lib");
            dirTemplateMap.put("${system32}", drivecPath + "/windows/system32");
            dirTemplateMap.put("${syswow64}", drivecPath + "/windows/syswow64");
            dirTemplateMap.put("${bindir}", imagefsPath + "/usr/bin");
            dirTemplateMap.put("${sharedir}", imagefsPath + "/usr/share");
        }
    }

    private void createTrustedFilesMap() {
        if (trustedFilesMap == null) {
            trustedFilesMap = new HashMap<>();
            for (ContentProfile.ContentType type : ContentProfile.ContentType.values()) {
                List<String> pathList = new ArrayList<>();
                trustedFilesMap.put(type, pathList);

                String[] paths = switch (type) {
                    case CONTENT_TYPE_DXVK -> DXVK_TRUST_FILES;
                    case CONTENT_TYPE_VKD3D -> VKD3D_TRUST_FILES;
                    case CONTENT_TYPE_BOX64 -> BOX64_TRUST_FILES;
                    case CONTENT_TYPE_WOWBOX64 -> WOWBOX64_TRUST_FILES;
                    case CONTENT_TYPE_FEXCORE -> FEXCORE_TRUST_FILES;
                    default -> new String[0];
                };
                for (String path : paths)
                    pathList.add(Paths.get(getPathFromTemplate(path)).toAbsolutePath().normalize().toString());
            }
        }
    }

    private String getPathFromTemplate(String path) {
        createDirTemplateMap();
        String realPath = path;
        for (String key : dirTemplateMap.keySet()) {
            realPath = realPath.replace(key, dirTemplateMap.get(key));
        }
        return realPath;
    }

    public void removeContent(ContentProfile profile) {
        if (profilesMap.get(profile.type).contains(profile)) {
            FileUtils.delete(getInstallDir(context, profile));
            profilesMap.get(profile.type).remove(profile);
            syncContents();
        }
    }

    public static String getEntryName(ContentProfile profile) {
        return profile.type.toString() + '-' + profile.verName + '-' + profile.verCode;
    }

    public ContentProfile getProfileByEntryName(String entryName) {
        int firstDashIndex = entryName.indexOf('-');
        int lastDashIndex = entryName.lastIndexOf('-');

        try {
            String typeName = entryName.substring(0, firstDashIndex);
            String versionName = entryName.substring(firstDashIndex + 1, lastDashIndex);
            String versionCode = entryName.substring(lastDashIndex + 1);
            ContentProfile.ContentType contentType = ContentProfile.ContentType.getTypeByName(typeName);
            List<ContentProfile> profiles = contentType != null ? profilesMap.get(contentType) : null;
            if (profiles == null) return null;

            for (ContentProfile profile : profiles) {
                if (versionName.equals(profile.verName) && Integer.parseInt(versionCode) == profile.verCode)
                    return profile;
            }
        } catch (Exception e) {
        }

        return null;
    }

    public boolean applyContent(ContentProfile profile) {
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_WINE) {
            return true;
        }

        for (ContentProfile.ContentFile contentFile : profile.fileList) {
            File targetFile = new File(getPathFromTemplate(contentFile.target));
            File sourceFile = new File(getInstallDir(context, profile), contentFile.source);

            targetFile.delete();
            FileUtils.copy(sourceFile, targetFile);

            if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_BOX64) {
                FileUtils.chmod(targetFile, 0771);
            }
        }
        return true;
    }

    private String readRemoteUrl(JSONObject object) {
        if (object == null) return "";
        String[] keys = {"remoteUrl", "url", "browser_download_url", "downloadUrl", "assetUrl"};
        for (String key : keys) {
            String value = object.optString(key, "").trim();
            if (!value.isEmpty() && isAllowedRemoteUrl(value)) return value;
        }
        return "";
    }

    private String readRemoteSha256(JSONObject object) {
        if (object == null) return "";
        String[] keys = {
                ContentProfile.MARK_SHA256,
                "sha256sum",
                "checksum",
                "checksumSha256",
                "assetSha256",
                "digest"
        };
        for (String key : keys) {
            String value = normalizeSha256(object.optString(key, ""));
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private boolean isAllowedRemoteUrl(String value) {
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            String normalizedScheme = scheme.trim().toLowerCase(Locale.US);
            if (!"https".equals(normalizedScheme) && !"http".equals(normalizedScheme)) return false;
            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) return false;
            String normalizedHost = host.trim().toLowerCase(Locale.US);
            if ("http".equals(normalizedScheme) && !isLocalhostHost(normalizedHost)) return false;
            // Reject credential-in-URL patterns.
            if (uri.getUserInfo() != null && !uri.getUserInfo().trim().isEmpty()) return false;
            String path = uri.getPath();
            return hasAllowedArchiveSuffix(path);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isLocalhostHost(String host) {
        return "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    private boolean hasAllowedArchiveSuffix(String path) {
        if (path == null || path.trim().isEmpty()) return false;
        String normalizedPath = path.trim().toLowerCase(Locale.US);
        for (String suffix : CONTENT_ARCHIVE_SUFFIXES) {
            if (normalizedPath.endsWith(suffix)) return true;
        }
        return false;
    }

    private String normalizeSha256(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.US).replaceAll("[^0-9a-f]", "");
        return normalized.length() == 64 ? normalized : "";
    }

    private int parseVerCode(JSONObject object) {
        if (object == null) return 0;
        Object raw = object.opt("verCode");
        if (raw instanceof Number) return ((Number) raw).intValue();
        if (raw != null) {
            try {
                return Integer.parseInt(String.valueOf(raw).replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private int parseOptionalInt(Object raw, int fallback) {
        if (raw == null) return fallback;
        if (raw instanceof Number) return ((Number) raw).intValue();
        try {
            String normalized = String.valueOf(raw).trim();
            if (normalized.isEmpty()) return fallback;
            return Integer.parseInt(normalized.replaceAll("[^0-9-]", ""));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String deriveVersionNameFromUrl(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty()) return "";
        String path = remoteUrl.trim();
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;
        return fileName.replaceAll("\\.(wcp|zip|tar|txz|tzst)$", "");
    }

    private String deriveLegacyChannel(JSONObject object, String verName, String remoteUrl) {
        String combined = (
                object.optString("name", "") + " "
                        + object.optString("description", "") + " "
                        + (verName == null ? "" : verName) + " "
                        + (remoteUrl == null ? "" : remoteUrl)
        ).toLowerCase(Locale.US);
        if (combined.contains("nightly")) return ContentProfile.CHANNEL_NIGHTLY;
        if (combined.contains("beta") || combined.contains("rc")) return ContentProfile.CHANNEL_BETA;
        return ContentProfile.CHANNEL_STABLE;
    }

    private boolean isWineFamilyType(ContentProfile.ContentType type) {
        return type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                || type == ContentProfile.ContentType.CONTENT_TYPE_PROTON;
    }

    private boolean isRepoManagedOverlayType(ContentProfile.ContentType type) {
        return isWineFamilyType(type)
                || type == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK
                || type == ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER
                || type == ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER
                || type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO
                || type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D;
    }

    private boolean isUpdatableLane(ContentProfile.ContentType type) {
        return type == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK
                || type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO
                || type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D
                || type == ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER
                || type == ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER;
    }

    private boolean isTrustedByPrefix(ContentProfile.ContentType type, String normalizedTarget) {
        if (type != ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK) return false;
        for (String prefix : VULKAN_SDK_TRUST_PREFIXES) {
            String resolvedPrefix = Paths.get(getPathFromTemplate(prefix)).toAbsolutePath().normalize().toString();
            if (normalizedTarget.startsWith(resolvedPrefix)) return true;
        }
        return false;
    }

    private ContentProfile findEquivalentProfile(List<ContentProfile> profiles, ContentProfile remote) {
        if (profiles == null || remote == null) return null;
        for (ContentProfile profile : profiles) {
            if (profile == null) continue;
            if (profile.sameEntry(remote)) return profile;
            if (isUpdatableLane(profile.type)
                    && profile.type == remote.type
                    && profile.verName != null
                    && remote.verName != null
                    && profile.verName.equalsIgnoreCase(remote.verName)
                    && profile.getChannel().equalsIgnoreCase(remote.getChannel())
                    && resolveArchHint(profile).equals(resolveArchHint(remote))) {
                // Updatable lanes can publish with bumped versionCode while keeping the same semantic version.
                // Treat them as equivalent to preserve installed-state markers in Contents.
                return profile;
            }
        }
        return null;
    }

    private String resolveArchHint(ContentProfile profile) {
        if (profile == null) return "generic";
        String combined = (
                (profile.verName == null ? "" : profile.verName) + " "
                        + (profile.desc == null ? "" : profile.desc) + " "
                        + (profile.remoteUrl == null ? "" : profile.remoteUrl) + " "
                        + (profile.releaseTag == null ? "" : profile.releaseTag)
        ).toLowerCase(Locale.US);
        if (combined.contains("arm64ec") || combined.contains("arm64-ec")) return "arm64ec";
        if (combined.contains("x86_64") || combined.contains("x86-64") || combined.contains("amd64")) return "x86_64";
        if (combined.contains("arm64") || combined.contains("aarch64")) return "arm64";
        return "generic";
    }
}
