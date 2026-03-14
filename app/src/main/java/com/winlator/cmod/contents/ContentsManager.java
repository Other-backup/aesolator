package com.winlator.cmod.contents;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.TarCompressorUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ContentsManager {
    public static final String PROFILE_NAME = "profile.json";
    public static final String REMOTE_PROFILES = "https://raw.githubusercontent.com/Arihany/WinlatorWCPHub/main/pack.json";
    public static final String REMOTE_PROFILES_AE = "https://raw.githubusercontent.com/kosoymiki/aesolator/main/contents/contents.json";
    public static final String REMOTE_GAMEHUB_RELEASES = "https://api.github.com/repos/The412Banner/Gamehub-Components/releases?per_page=100";
    public static final String REMOTE_GAMEHUB_COMPONENTS = "https://raw.githubusercontent.com/The412Banner/Gamehub-Components/main/sp_winemu_all_components12.xml";
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
            ".wcp", ".wcp.xz", ".wcp.zst", ".zip", ".tar", ".txz", ".tzst", ".tar.xz", ".tar.zst"
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

    public void setHubRemoteProfiles(String json) {
        remoteProfiles = new ArrayList<>();
        // WCPHub must stay visible for overlapping families too; the source
        // selector, not the parser, is what keeps archive/hub provenance apart.
        appendRemoteProfiles(json, false, false, false, true);
        syncContents();
    }

    public void setArchiveRemoteProfiles(String json) {
        remoteProfiles = new ArrayList<>();
        appendRemoteProfiles(json, false, false, true, true);
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
                    remoteProfile.sourceFeed = object.optString(ContentProfile.MARK_SOURCE_FEED, "").trim();
                    remoteProfile.sourceLabel = object.optString(ContentProfile.MARK_SOURCE_LABEL, "").trim();
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
            HashMap<ContentProfile, File> profileFileByProfile = new HashMap<>();

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
                            profileFileByProfile.put(profile, proFile);
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
                        persistProfileMetadata(profileFileByProfile.get(existing), existing);
                    } else {
                        profiles.add(remote);
                        profileByEntry.put(getEntryName(remote), remote);
                    }
                }
            }
        }
    }

    public void extraContentFile(Uri uri, OnInstallFinishedCallback callback) {
        extraContentFile(uri, null, callback);
    }

    public void extraContentFile(Uri uri, @Nullable ContentProfile remoteHint, OnInstallFinishedCallback callback) {
        cleanTmpDir(context);

        File file = getTmpDir(context);

        boolean ret = extractZipSafely(uri, file);
        if (!ret) {
            FileUtils.delete(file);
            if (!file.exists() && !file.mkdirs()) {
                callback.onFailed(InstallFailedReason.ERROR_UNKNOWN, null);
                return;
            }
        }
        if (!ret) ret = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, context, uri, file);
        if (!ret)
            ret = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, uri, file);
        if (!ret) {
            callback.onFailed(InstallFailedReason.ERROR_BADTAR, null);
            return;
        }

        File proFile = new File(file, PROFILE_NAME);
        if (!proFile.exists() && remoteHint != null) {
            ContentProfile synthesizedProfile = synthesizeProfileFromExtractedPayload(file, remoteHint);
            if (synthesizedProfile != null && writeSyntheticProfile(file, synthesizedProfile)) {
                proFile = new File(file, PROFILE_NAME);
            }
        }
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

    private boolean extractZipSafely(Uri uri, File destination) {
        if (uri == null || destination == null) return false;
        if (destination.exists() && !FileUtils.clear(destination)) {
            FileUtils.delete(destination);
        }
        if (!destination.exists() && !destination.mkdirs()) return false;

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             ZipInputStream zis = inputStream == null ? null : new ZipInputStream(inputStream)) {
            if (zis == null) return false;
            ZipEntry entry;
            boolean extractedAnything = false;
            while ((entry = zis.getNextEntry()) != null) {
                File target = getSafeZipEntryFile(destination, entry);
                if (target == null) {
                    FileUtils.delete(destination);
                    return false;
                }
                if (entry.isDirectory()) {
                    if (!target.exists() && !target.mkdirs()) {
                        FileUtils.delete(destination);
                        return false;
                    }
                    continue;
                }

                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    FileUtils.delete(destination);
                    return false;
                }
                Files.copy(zis, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                extractedAnything = true;
            }
            if (!extractedAnything) {
                FileUtils.delete(destination);
            }
            return extractedAnything;
        } catch (IOException e) {
            FileUtils.delete(destination);
            return false;
        }
    }

    private File getSafeZipEntryFile(File rootDir, ZipEntry entry) throws IOException {
        File dstFile = new File(rootDir, entry.getName());
        String rootPath = rootDir.getCanonicalPath() + File.separator;
        String dstPath = dstFile.getCanonicalPath();
        if (!dstPath.startsWith(rootPath)) return null;
        return dstFile;
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
        persistProfileMetadata(new File(installPath, PROFILE_NAME), profile);

        callback.onSucceed(profile);
    }

    private void persistProfileMetadata(File profileFile, ContentProfile profile) {
        if (profileFile == null || profile == null || !profileFile.isFile()) return;
        try {
            JSONObject object = new JSONObject(FileUtils.readString(profileFile));
            boolean changed = false;

            changed |= putProfileField(object, ContentProfile.MARK_CHANNEL, profile.getChannel());
            changed |= putProfileField(object, ContentProfile.MARK_DELIVERY, profile.getDelivery());
            changed |= putProfileField(object, ContentProfile.MARK_DISPLAY_CATEGORY, profile.getDisplayCategory());
            changed |= putProfileField(object, ContentProfile.MARK_SOURCE_REPO, profile.sourceRepo);
            changed |= putProfileField(object, ContentProfile.MARK_SOURCE_FEED, profile.sourceFeed);
            changed |= putProfileField(object, ContentProfile.MARK_SOURCE_LABEL, profile.sourceLabel);
            changed |= putProfileField(object, ContentProfile.MARK_RELEASE_TAG, profile.releaseTag);
            changed |= putProfileField(object, ContentProfile.MARK_SHA256, profile.remoteSha256);

            if (profile.vulkanApiMin > 0 && object.optInt(ContentProfile.MARK_VULKAN_API_MIN, 0) != profile.vulkanApiMin) {
                object.put(ContentProfile.MARK_VULKAN_API_MIN, profile.vulkanApiMin);
                changed = true;
            }
            if (profile.vulkanApiMax > 0 && object.optInt(ContentProfile.MARK_VULKAN_API_MAX, 0) != profile.vulkanApiMax) {
                object.put(ContentProfile.MARK_VULKAN_API_MAX, profile.vulkanApiMax);
                changed = true;
            }
            if (profile.vulkanSdkVersion != null && !profile.vulkanSdkVersion.trim().isEmpty()
                    && !profile.vulkanSdkVersion.equals(object.optString(ContentProfile.MARK_VULKAN_SDK_VERSION, ""))) {
                object.put(ContentProfile.MARK_VULKAN_SDK_VERSION, profile.vulkanSdkVersion.trim());
                changed = true;
            }

            if (changed) {
                FileUtils.writeString(profileFile, object.toString());
            }
        } catch (Exception ignored) {
        }
    }

    private boolean putProfileField(JSONObject object, String key, String value) throws JSONException {
        String normalized = value == null ? "" : value.trim();
        String current = object.optString(key, "");
        if (normalized.isEmpty() || normalized.equals(current)) return false;
        object.put(key, normalized);
        return true;
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
            profile.sourceFeed = profileJSONObject.optString(ContentProfile.MARK_SOURCE_FEED, "");
            profile.sourceLabel = profileJSONObject.optString(ContentProfile.MARK_SOURCE_LABEL, "");
            profile.releaseTag = profileJSONObject.optString(ContentProfile.MARK_RELEASE_TAG, "");
            profile.vulkanApiMin = profileJSONObject.optInt(ContentProfile.MARK_VULKAN_API_MIN, 0);
            profile.vulkanApiMax = profileJSONObject.optInt(ContentProfile.MARK_VULKAN_API_MAX, 0);
            profile.vulkanSdkVersion = profileJSONObject.optString(ContentProfile.MARK_VULKAN_SDK_VERSION, "");
            profile.remoteSha256 = normalizeSha256(profileJSONObject.optString(ContentProfile.MARK_SHA256, ""));
            profile.locallyInstalled = true;

            List<ContentProfile.ContentFile> fileList = new ArrayList<>();
            JSONArray fileJSONArray = profileJSONObject.optJSONArray(ContentProfile.MARK_FILE_LIST);
            if (fileJSONArray == null) {
                fileJSONArray = profileJSONObject.optJSONArray("fileList");
            }
            if (fileJSONArray != null) {
                for (int i = 0; i < fileJSONArray.length(); i++) {
                    JSONObject contentFileJSONObject = fileJSONArray.getJSONObject(i);
                    ContentProfile.ContentFile contentFile = new ContentProfile.ContentFile();
                    contentFile.source = contentFileJSONObject.optString(ContentProfile.MARK_FILE_SOURCE, contentFileJSONObject.optString("src", ""));
                    contentFile.target = contentFileJSONObject.optString(ContentProfile.MARK_FILE_TARGET, contentFileJSONObject.optString("dst", ""));
                    if (contentFile.source.isEmpty() || contentFile.target.isEmpty()) continue;
                    fileList.add(contentFile);
                }
            }
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
                if (profile.wineLibPath.isEmpty() || profile.wineBinPath.isEmpty() || profile.winePrefixPack.isEmpty()) {
                    return null;
                }
            } else if (fileList.isEmpty()) {
                return null;
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
        return fileName.replaceAll("(?i)\\.(wcp\\.xz|wcp\\.zst|wcp|zip|tar|txz|tzst)$", "");
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

    @Nullable
    private ContentProfile synthesizeProfileFromExtractedPayload(File rootDir, ContentProfile remoteHint) {
        if (rootDir == null || remoteHint == null || remoteHint.type == null) return null;

        ContentProfile profile = new ContentProfile();
        profile.type = remoteHint.type;
        profile.verName = remoteHint.verName;
        profile.verCode = remoteHint.verCode;
        profile.desc = remoteHint.desc;
        profile.remoteUrl = remoteHint.remoteUrl;
        profile.remoteSha256 = remoteHint.remoteSha256;
        profile.channel = remoteHint.getChannel();
        profile.delivery = remoteHint.getDelivery().isEmpty() ? ContentProfile.DELIVERY_REMOTE : remoteHint.getDelivery();
        profile.displayCategory = remoteHint.getDisplayCategory();
        profile.sourceRepo = remoteHint.sourceRepo;
        profile.sourceFeed = remoteHint.sourceFeed;
        profile.sourceLabel = remoteHint.sourceLabel;
        profile.releaseTag = remoteHint.releaseTag;
        profile.vulkanApiMin = remoteHint.vulkanApiMin;
        profile.vulkanApiMax = remoteHint.vulkanApiMax;
        profile.vulkanSdkVersion = remoteHint.vulkanSdkVersion;

        switch (remoteHint.type) {
            case CONTENT_TYPE_DXVK -> profile.fileList = synthesizeDxvkFiles(rootDir);
            case CONTENT_TYPE_VKD3D -> profile.fileList = synthesizeVkd3dFiles(rootDir);
            case CONTENT_TYPE_BOX64 -> profile.fileList = synthesizeSingleFile(rootDir, "box64", "${bindir}/box64");
            case CONTENT_TYPE_WOWBOX64 -> profile.fileList = synthesizeWowBox64Files(rootDir);
            case CONTENT_TYPE_FEXCORE -> profile.fileList = synthesizeFexCoreFiles(rootDir);
            case CONTENT_TYPE_WINE, CONTENT_TYPE_PROTON -> synthesizeWineFamilyProfile(rootDir, profile);
            default -> {
                return null;
            }
        }

        boolean hasPayloadFiles = profile.fileList != null && !profile.fileList.isEmpty();
        if (!hasPayloadFiles && !profile.isWineProtonFamily()) return null;
        if (profile.verName == null || profile.verName.trim().isEmpty()) {
            profile.verName = deriveVersionNameFromUrl(profile.remoteUrl);
        }
        return profile;
    }

    private boolean writeSyntheticProfile(File rootDir, ContentProfile profile) {
        if (rootDir == null || profile == null) return false;
        boolean hasPayloadFiles = profile.fileList != null && !profile.fileList.isEmpty();
        if (!hasPayloadFiles && !profile.isWineProtonFamily()) return false;
        try {
            JSONObject object = new JSONObject();
            object.put(ContentProfile.MARK_TYPE, profile.type.toString());
            object.put(ContentProfile.MARK_VERSION_NAME, profile.verName == null ? "" : profile.verName);
            object.put(ContentProfile.MARK_VERSION_CODE, profile.verCode);
            object.put(ContentProfile.MARK_DESC, profile.desc == null ? "" : profile.desc);
            object.put(ContentProfile.MARK_CHANNEL, profile.getChannel());
            object.put(ContentProfile.MARK_DELIVERY, profile.getDelivery().isEmpty() ? ContentProfile.DELIVERY_REMOTE : profile.getDelivery());
            object.put(ContentProfile.MARK_DISPLAY_CATEGORY, profile.getDisplayCategory());
            object.put(ContentProfile.MARK_SOURCE_REPO, profile.sourceRepo == null ? "" : profile.sourceRepo);
            object.put(ContentProfile.MARK_SOURCE_FEED, profile.sourceFeed == null ? "" : profile.sourceFeed);
            object.put(ContentProfile.MARK_SOURCE_LABEL, profile.sourceLabel == null ? "" : profile.sourceLabel);
            object.put(ContentProfile.MARK_RELEASE_TAG, profile.releaseTag == null ? "" : profile.releaseTag);
            if (profile.remoteSha256 != null && !profile.remoteSha256.trim().isEmpty()) {
                object.put(ContentProfile.MARK_SHA256, profile.remoteSha256.trim());
            }
            if (profile.vulkanApiMin > 0) object.put(ContentProfile.MARK_VULKAN_API_MIN, profile.vulkanApiMin);
            if (profile.vulkanApiMax > 0) object.put(ContentProfile.MARK_VULKAN_API_MAX, profile.vulkanApiMax);
            if (profile.vulkanSdkVersion != null && !profile.vulkanSdkVersion.trim().isEmpty()) {
                object.put(ContentProfile.MARK_VULKAN_SDK_VERSION, profile.vulkanSdkVersion.trim());
            }

            JSONArray files = new JSONArray();
            if (profile.fileList != null) {
                for (ContentProfile.ContentFile contentFile : profile.fileList) {
                    JSONObject fileObject = new JSONObject();
                    fileObject.put(ContentProfile.MARK_FILE_SOURCE, contentFile.source);
                    fileObject.put(ContentProfile.MARK_FILE_TARGET, contentFile.target);
                    files.put(fileObject);
                }
            }
            object.put(ContentProfile.MARK_FILE_LIST, files);

            if (profile.isWineProtonFamily()) {
                JSONObject wineObject = new JSONObject();
                wineObject.put(ContentProfile.MARK_WINE_BINPATH, profile.wineBinPath);
                wineObject.put(ContentProfile.MARK_WINE_LIBPATH, profile.wineLibPath);
                wineObject.put(ContentProfile.MARK_WINE_PREFIX_PACK, profile.winePrefixPack);
                object.put(profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON
                        ? ContentProfile.MARK_PROTON
                        : ContentProfile.MARK_WINE, wineObject);
            }
            return FileUtils.writeString(new File(rootDir, PROFILE_NAME), object.toString());
        } catch (Exception e) {
            return false;
        }
    }

    private void synthesizeWineFamilyProfile(File rootDir, ContentProfile profile) {
        String binPath = findRelativeDirectory(rootDir, "bin");
        String libPath = findRelativeDirectory(rootDir, "lib");
        String prefixPackPath = findRelativeFile(rootDir, "prefixPack.txz");
        if (binPath == null || libPath == null || prefixPackPath == null) return;

        profile.wineBinPath = binPath;
        profile.wineLibPath = libPath;
        profile.winePrefixPack = prefixPackPath;
        profile.fileList = new ArrayList<>();
    }

    private List<ContentProfile.ContentFile> synthesizeDxvkFiles(File rootDir) {
        String[][] mappings = {
                {"system32/d3d8.dll", "${system32}/d3d8.dll"},
                {"system32/d3d9.dll", "${system32}/d3d9.dll"},
                {"system32/d3d10.dll", "${system32}/d3d10.dll"},
                {"system32/d3d10_1.dll", "${system32}/d3d10_1.dll"},
                {"system32/d3d10core.dll", "${system32}/d3d10core.dll"},
                {"system32/d3d11.dll", "${system32}/d3d11.dll"},
                {"system32/dxgi.dll", "${system32}/dxgi.dll"},
                {"syswow64/d3d8.dll", "${syswow64}/d3d8.dll"},
                {"syswow64/d3d9.dll", "${syswow64}/d3d9.dll"},
                {"syswow64/d3d10.dll", "${syswow64}/d3d10.dll"},
                {"syswow64/d3d10_1.dll", "${syswow64}/d3d10_1.dll"},
                {"syswow64/d3d10core.dll", "${syswow64}/d3d10core.dll"},
                {"syswow64/d3d11.dll", "${syswow64}/d3d11.dll"},
                {"syswow64/dxgi.dll", "${syswow64}/dxgi.dll"}
        };
        return synthesizeMappedFiles(rootDir, mappings);
    }

    private List<ContentProfile.ContentFile> synthesizeVkd3dFiles(File rootDir) {
        String[][] mappings = {
                {"system32/d3d12.dll", "${system32}/d3d12.dll"},
                {"system32/d3d12core.dll", "${system32}/d3d12core.dll"},
                {"syswow64/d3d12.dll", "${syswow64}/d3d12.dll"},
                {"syswow64/d3d12core.dll", "${syswow64}/d3d12core.dll"}
        };
        return synthesizeMappedFiles(rootDir, mappings);
    }

    private List<ContentProfile.ContentFile> synthesizeWowBox64Files(File rootDir) {
        ArrayList<ContentProfile.ContentFile> files = new ArrayList<>();
        String relative = findRelativeFile(rootDir, "wowbox64.dll");
        if (relative == null) return files;
        ContentProfile.ContentFile item = new ContentProfile.ContentFile();
        item.source = relative;
        item.target = "${system32}/wowbox64.dll";
        files.add(item);
        return files;
    }

    private List<ContentProfile.ContentFile> synthesizeFexCoreFiles(File rootDir) {
        ArrayList<ContentProfile.ContentFile> files = new ArrayList<>();
        String arm64ec = findRelativeFile(rootDir, "libarm64ecfex.dll");
        String wow64 = findRelativeFile(rootDir, "libwow64fex.dll");
        if (arm64ec == null || wow64 == null) return files;

        ContentProfile.ContentFile arm64ecFile = new ContentProfile.ContentFile();
        arm64ecFile.source = arm64ec;
        arm64ecFile.target = "${system32}/libarm64ecfex.dll";
        files.add(arm64ecFile);

        ContentProfile.ContentFile wow64File = new ContentProfile.ContentFile();
        wow64File.source = wow64;
        wow64File.target = "${system32}/libwow64fex.dll";
        files.add(wow64File);
        return files;
    }

    private List<ContentProfile.ContentFile> synthesizeSingleFile(File rootDir, String fileName, String targetPath) {
        ArrayList<ContentProfile.ContentFile> files = new ArrayList<>();
        String relative = findRelativeFile(rootDir, fileName);
        if (relative == null) return files;
        ContentProfile.ContentFile item = new ContentProfile.ContentFile();
        item.source = relative;
        item.target = targetPath;
        files.add(item);
        return files;
    }

    private List<ContentProfile.ContentFile> synthesizeMappedFiles(File rootDir, String[][] mappings) {
        ArrayList<ContentProfile.ContentFile> files = new ArrayList<>();
        for (String[] mapping : mappings) {
            File file = new File(rootDir, mapping[0]);
            if (!file.isFile()) continue;
            ContentProfile.ContentFile item = new ContentProfile.ContentFile();
            item.source = mapping[0];
            item.target = mapping[1];
            files.add(item);
        }
        return files;
    }

    @Nullable
    private String findRelativeDirectory(File rootDir, String dirName) {
        if (rootDir == null || dirName == null || dirName.trim().isEmpty()) return null;
        File candidate = new File(rootDir, dirName);
        if (candidate.isDirectory()) return dirName;
        return findRelativeDirectoryRecursive(rootDir, rootDir, dirName.trim().toLowerCase(Locale.US));
    }

    @Nullable
    private String findRelativeDirectoryRecursive(File rootDir, File current, String normalizedName) {
        File[] children = current.listFiles();
        if (children == null) return null;
        for (File child : children) {
            if (!child.isDirectory()) continue;
            if (child.getName().trim().toLowerCase(Locale.US).equals(normalizedName)) {
                return relativizePath(rootDir, child);
            }
            String nested = findRelativeDirectoryRecursive(rootDir, child, normalizedName);
            if (nested != null) return nested;
        }
        return null;
    }

    @Nullable
    private String findRelativeFile(File rootDir, String fileName) {
        if (rootDir == null || fileName == null || fileName.trim().isEmpty()) return null;
        File candidate = new File(rootDir, fileName);
        if (candidate.isFile()) return fileName;
        return findRelativeFileRecursive(rootDir, rootDir, fileName.trim().toLowerCase(Locale.US));
    }

    @Nullable
    private String findRelativeFileRecursive(File rootDir, File current, String normalizedName) {
        File[] children = current.listFiles();
        if (children == null) return null;
        for (File child : children) {
            if (child.isDirectory()) {
                String nested = findRelativeFileRecursive(rootDir, child, normalizedName);
                if (nested != null) return nested;
                continue;
            }
            if (child.getName().trim().toLowerCase(Locale.US).equals(normalizedName)) {
                return relativizePath(rootDir, child);
            }
        }
        return null;
    }

    private String relativizePath(File rootDir, File file) {
        String rootPath = rootDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (!filePath.startsWith(rootPath)) return file.getName();
        String relative = filePath.substring(rootPath.length()).replace('\\', '/');
        while (relative.startsWith("/")) relative = relative.substring(1);
        return relative;
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
