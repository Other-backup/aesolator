package com.winlator.cmod.contents;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.xenvironment.ImageFs;

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
import java.util.LinkedHashMap;
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
    public static final String REMOTE_THE412BANNER_NIGHTLIES_RELEASES = "https://api.github.com/repos/The412Banner/Nightlies/releases?per_page=100";
    public static final String REMOTE_THE412BANNER_NIGHTLIES_RELEASES_ATOM = "https://github.com/The412Banner/Nightlies/releases.atom";
    public static final String REMOTE_WINE_PROTON_OVERLAY = REMOTE_PROFILES_AE;
    public static final String[] DXVK_TRUST_FILES = {"${system32}/d3d8.dll", "${system32}/d3d9.dll", "${system32}/d3d10.dll", "${system32}/d3d10_1.dll",
            "${system32}/d3d10core.dll", "${system32}/d3d11.dll", "${system32}/dxgi.dll", "${syswow64}/d3d8.dll", "${syswow64}/d3d9.dll", "${syswow64}/d3d10.dll",
            "${syswow64}/d3d10_1.dll", "${syswow64}/d3d10core.dll", "${syswow64}/d3d11.dll", "${syswow64}/dxgi.dll"};
    public static final String[] VKD3D_TRUST_FILES = {"${system32}/d3d12core.dll", "${system32}/d3d12.dll",
            "${syswow64}/d3d12core.dll", "${syswow64}/d3d12.dll"};
    public static final String[] BOX64_TRUST_FILES = {"${localbin}/box64", "${bindir}/box64"};
    public static final String[] WOWBOX64_TRUST_FILES = {"${system32}/wowbox64.dll"};
    public static final String[] FEXCORE_TRUST_FILES = {"${system32}/libwow64fex.dll", "${system32}/libarm64ecfex.dll"};
    public static final String[] DGVOODOO_TRUST_FILES = {
            "${system32}/D3D8.dll", "${system32}/D3D8_dgvoodoo.dll",
            "${system32}/D3D9.dll", "${system32}/D3D9_dgvoodoo.dll",
            "${system32}/D3DImm.dll", "${system32}/D3DImm_dgvoodoo.dll",
            "${system32}/DDraw.dll", "${system32}/DDraw_dgvoodoo.dll",
            "${system32}/Glide.dll", "${system32}/Glide2x.dll",
            "${system32}/Glide3x.dll", "${system32}/Glide3xNapalm.dll"
    };
    public static final String[] VULKAN_SDK_TRUST_PREFIXES = {
            "${sharedir}/vulkan",
            "${sharedir}/vulkan-sdk",
            "${libdir}/vulkan",
            "${libdir}/vulkan-sdk"
    };
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

    public void setGamehubRemoteProfiles(String json) {
        remoteProfiles = new ArrayList<>();
        appendRemoteProfiles(json, false, false, false, true);
        syncContents();
    }

    public void setNightliesRemoteProfiles(String json) {
        remoteProfiles = new ArrayList<>();
        appendRemoteProfiles(json, false, false, false, true);
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
                    remoteProfile.artifactName = object.optString(ContentProfile.MARK_ARTIFACT_NAME, "").trim();
                    remoteProfile.publishedAt = object.optString(ContentProfile.MARK_PUBLISHED_AT, "").trim();
                    remoteProfile.releaseNotes = object.optString(ContentProfile.MARK_RELEASE_NOTES, "").trim();
                    remoteProfile.runtimeModel = ContentProfile.normalizeRuntimeModel(
                            object.optString(ContentProfile.MARK_RUNTIME_MODEL, "")
                    );
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
                    remoteProfile.runtimeModel = remoteProfile.getRuntimeModel();

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
        repairInstalledRuntimeOverlays();
        profilesMap = new HashMap<>();
        for (ContentProfile.ContentType type : ContentProfile.ContentType.values()) {
            profilesMap.put(type, new LinkedList<>());
        }

        for (ContentProfile.ContentType type : ContentProfile.ContentType.values()) {
            List<ContentProfile> profiles = profilesMap.get(type);
            HashMap<String, ContentProfile> profileByEntry = new HashMap<>();
            HashMap<ContentProfile, File> profileFileByProfile = new HashMap<>();

            List<File> installedRoots = getInstalledRootsForType(type);
            if (installedRoots != null) {
                for (File file : installedRoots) {
                    File proFile = new File(file, PROFILE_NAME);
                    if (proFile.exists() && proFile.isFile()) {
                        ContentProfile profile = normalizeImportedProfile(readProfile(proFile), null);
                        if (profile != null && profile.type == type) {
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
        profile = normalizeImportedProfile(profile, remoteHint);

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
        if (profile.isWineProtonFamily()) {
            postProcessWineRuntimeInstall(installPath, profile);
        }
        persistProfileMetadata(new File(installPath, PROFILE_NAME), profile);

        callback.onSucceed(profile);
    }

    private void persistProfileMetadata(File profileFile, ContentProfile profile) {
        if (profileFile == null || profile == null || !profileFile.isFile()) return;
        try {
            JSONObject object = new JSONObject(FileUtils.readString(profileFile));
            boolean changed = false;

            String normalizedType = profile.type != null ? profile.type.toString() : "";
            if (!normalizedType.isEmpty() && !normalizedType.equals(object.optString(ContentProfile.MARK_TYPE, ""))) {
                object.put(ContentProfile.MARK_TYPE, normalizedType);
                changed = true;
            }

            changed |= putProfileField(object, ContentProfile.MARK_CHANNEL, profile.getChannel());
            changed |= putProfileField(object, ContentProfile.MARK_DELIVERY, profile.getDelivery());
            changed |= putProfileField(object, ContentProfile.MARK_DISPLAY_CATEGORY, profile.getDisplayCategory());
            changed |= putProfileField(object, ContentProfile.MARK_SOURCE_REPO, profile.sourceRepo);
            changed |= putProfileField(object, ContentProfile.MARK_SOURCE_FEED, profile.sourceFeed);
            changed |= putProfileField(object, ContentProfile.MARK_SOURCE_LABEL, profile.sourceLabel);
            changed |= putProfileField(object, ContentProfile.MARK_RELEASE_TAG, profile.releaseTag);
            changed |= putProfileField(object, ContentProfile.MARK_ARTIFACT_NAME, profile.artifactName);
            changed |= putProfileField(object, ContentProfile.MARK_PUBLISHED_AT, profile.publishedAt);
            changed |= putProfileField(object, ContentProfile.MARK_RELEASE_NOTES, profile.releaseNotes);
            changed |= putProfileField(object, ContentProfile.MARK_SHA256, profile.remoteSha256);
            if (profile.isWineProtonFamily()) {
                changed |= putProfileField(object, ContentProfile.MARK_RUNTIME_MODEL, profile.getRuntimeModel());
            }

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
            if (profile.isWineProtonFamily()) {
                JSONObject runtimeObject = new JSONObject();
                runtimeObject.put(ContentProfile.MARK_WINE_LIBPATH, profile.wineLibPath == null ? "" : profile.wineLibPath);
                runtimeObject.put(ContentProfile.MARK_WINE_BINPATH, profile.wineBinPath == null ? "" : profile.wineBinPath);
                runtimeObject.put(ContentProfile.MARK_WINE_PREFIX_PACK, profile.winePrefixPack == null ? "" : profile.winePrefixPack);
                if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
                    object.put(ContentProfile.MARK_PROTON, runtimeObject);
                    object.remove(ContentProfile.MARK_WINE);
                } else {
                    object.put(ContentProfile.MARK_WINE, runtimeObject);
                    object.remove(ContentProfile.MARK_PROTON);
                }
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
            profile.artifactName = profileJSONObject.optString(ContentProfile.MARK_ARTIFACT_NAME, "");
            profile.publishedAt = profileJSONObject.optString(ContentProfile.MARK_PUBLISHED_AT, "");
            profile.releaseNotes = profileJSONObject.optString(ContentProfile.MARK_RELEASE_NOTES, "");
            profile.runtimeModel = ContentProfile.normalizeRuntimeModel(
                    profileJSONObject.optString(ContentProfile.MARK_RUNTIME_MODEL, "")
            );
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
                profile.runtimeModel = profile.getRuntimeModel();
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
        if (profile != null && profile.isWineProtonFamily()) {
            return new File(getContentTypeDir(context, profile.type), buildRuntimeInstallRootName(profile));
        }
        return new File(getContentTypeDir(context, profile.type), profile.verName + "-" + profile.verCode);
    }

    public static File getContentDir(Context context) {
        return new File(context.getFilesDir(), ContentDirName.CONTENT_MAIN_DIR_NAME.toString());
    }

    public static File getContentTypeDir(Context context, ContentProfile.ContentType type) {
        if (type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                || type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
            return ImageFs.find(context).getOptDir();
        }
        return new File(getContentDir(context), type.toString());
    }

    private static File getLegacyRuntimeTypeDir(Context context, ContentProfile.ContentType type) {
        return new File(getContentDir(context), type.toString());
    }

    private List<File> getInstalledRootsForType(ContentProfile.ContentType type) {
        LinkedHashMap<String, File> roots = new LinkedHashMap<>();
        if (isWineFamilyType(type)) {
            collectInstallRoots(roots, ImageFs.find(context).getOptDir());
            collectInstallRoots(roots, getLegacyRuntimeTypeDir(context, ContentProfile.ContentType.CONTENT_TYPE_WINE));
            collectInstallRoots(roots, getLegacyRuntimeTypeDir(context, ContentProfile.ContentType.CONTENT_TYPE_PROTON));
        } else {
            collectInstallRoots(roots, getContentTypeDir(context, type));
        }
        return new ArrayList<>(roots.values());
    }

    private void collectInstallRoots(LinkedHashMap<String, File> roots, File dir) {
        if (roots == null || dir == null || !dir.isDirectory()) return;
        File[] fileList = dir.listFiles();
        if (fileList == null) return;
        for (File file : fileList) {
            if (file == null || !file.isDirectory()) continue;
            roots.put(file.getAbsolutePath(), file);
        }
    }

    public static File getTmpDir(Context context) {
        return new File(context.getFilesDir(), "tmp/" + ContentDirName.CONTENT_MAIN_DIR_NAME);
    }

    public static File getSourceFile(Context context, ContentProfile profile, String path) {
        return new File(getInstallDir(context, profile), path);
    }

    private static String buildRuntimeInstallRootName(ContentProfile profile) {
        String runtimeModel = profile == null ? "" : profile.getRuntimeModel();
        String family = profile != null && profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON ? "proton" : "wine";
        String versionToken = sanitizeInstallToken(profile == null ? "" : profile.verName);
        return "runtime-" + (runtimeModel.isEmpty() ? "generic" : runtimeModel) + "-" + family + "-" + versionToken + "-" + (profile == null ? 0 : profile.verCode);
    }

    private static String sanitizeInstallToken(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.US);
        normalized = normalized.replaceAll("[^a-z0-9._-]+", "_");
        return normalized.isEmpty() ? "unknown" : normalized;
    }

    public static void cleanTmpDir(Context context) {
        File file = getTmpDir(context);
        FileUtils.delete(file);
        file.mkdirs();
    }

    public File getRuntimeRootDir(@Nullable ContentProfile profile) {
        if (profile == null) return null;
        return resolveWineRuntimeRoot(getInstallDir(context, profile), profile);
    }

    private File resolveWineRuntimeRoot(File installPath, @Nullable ContentProfile profile) {
        if (installPath == null || profile == null || !profile.isWineProtonFamily()) {
            return installPath;
        }

        String commonRoot = resolveSharedRuntimeRoot(profile);
        if (commonRoot.isEmpty()) {
            return installPath;
        }

        File candidate = new File(installPath, commonRoot);
        if (!candidate.isDirectory()) {
            return installPath;
        }

        File binDir = new File(installPath, normalizeRelativePath(profile.wineBinPath));
        File libDir = new File(installPath, normalizeRelativePath(profile.wineLibPath));
        File prefixPack = new File(installPath, normalizeRelativePath(profile.winePrefixPack));
        if (!binDir.isDirectory() || !libDir.isDirectory() || !prefixPack.isFile()) {
            return installPath;
        }
        return candidate;
    }

    private String resolveSharedRuntimeRoot(@Nullable ContentProfile profile) {
        if (profile == null) return "";
        String sharedRoot = parentRelativePath(profile.wineBinPath);
        sharedRoot = commonRelativeDirectory(sharedRoot, parentRelativePath(profile.wineLibPath));
        sharedRoot = commonRelativeDirectory(sharedRoot, parentRelativePath(profile.winePrefixPack));
        return sharedRoot;
    }

    private String normalizeRelativePath(@Nullable String path) {
        if (path == null) return "";
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        while (normalized.endsWith("/") && normalized.length() > 1) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    private String parentRelativePath(@Nullable String path) {
        String normalized = normalizeRelativePath(path);
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash <= 0 ? "" : normalized.substring(0, lastSlash);
    }

    private String commonRelativeDirectory(@Nullable String left, @Nullable String right) {
        String normalizedLeft = normalizeRelativePath(left);
        String normalizedRight = normalizeRelativePath(right);
        if (normalizedLeft.isEmpty()) return normalizedRight;
        if (normalizedRight.isEmpty()) return normalizedLeft;

        String[] leftParts = normalizedLeft.split("/");
        String[] rightParts = normalizedRight.split("/");
        int max = Math.min(leftParts.length, rightParts.length);
        StringBuilder shared = new StringBuilder();
        for (int i = 0; i < max; i++) {
            if (!leftParts[i].equals(rightParts[i])) break;
            if (shared.length() > 0) shared.append('/');
            shared.append(leftParts[i]);
        }
        return shared.toString();
    }

    private void postProcessWineRuntimeInstall(File installPath, ContentProfile profile) {
        if (installPath == null || profile == null || !profile.isWineProtonFamily()) return;
        ensureRuntimePrefixPackAtRoot(installPath, profile);
        normalizeWineLibraryStructure(installPath, profile);

        File binDir = new File(installPath, normalizeRelativePath(profile.wineBinPath));
        if (binDir.isDirectory()) {
            setExecutablePermissionsRecursive(binDir);
        }
    }

    private void ensureRuntimePrefixPackAtRoot(File installPath, ContentProfile profile) {
        if (installPath == null || profile == null) return;

        String prefixPackPath = normalizeRelativePath(profile.winePrefixPack);
        if (prefixPackPath.isEmpty()) return;

        File actualPrefixPack = new File(installPath, prefixPackPath);
        if (!actualPrefixPack.isFile()) return;

        File runtimeRoot = resolveWineRuntimeRoot(installPath, profile);
        if (runtimeRoot == null || !runtimeRoot.isDirectory()) return;

        File runtimePrefixPack = new File(runtimeRoot, actualPrefixPack.getName());
        if (runtimePrefixPack.equals(actualPrefixPack) || runtimePrefixPack.exists()) return;

        FileUtils.symlink(actualPrefixPack, runtimePrefixPack);
        if (runtimePrefixPack.exists()) return;

        FileUtils.copy(actualPrefixPack, runtimePrefixPack);
    }

    private void normalizeWineLibraryStructure(File installPath, ContentProfile profile) {
        if (installPath == null || profile == null) return;

        String libPath = normalizeRelativePath(profile.wineLibPath);
        if (libPath.isEmpty()) libPath = "lib";

        File actualLibDir = new File(installPath, libPath);
        if (!actualLibDir.isDirectory()) {
            Log.w("ContentsManager", "Skipping Wine library normalization, lib path missing: " + actualLibDir.getAbsolutePath());
            return;
        }

        File canonicalLibDir = actualLibDir;
        if ("wine".equals(actualLibDir.getName()) && actualLibDir.getParentFile() != null) {
            canonicalLibDir = actualLibDir.getParentFile();
        }

        File canonicalWineDir = new File(canonicalLibDir, "wine");
        File[] archDirs = canonicalLibDir.listFiles(file ->
                file.isDirectory()
                        && isWineArchitectureDirectory(file.getName())
                        && !file.getName().equals("wine"));
        if (archDirs == null || archDirs.length == 0) {
            return;
        }

        if (!canonicalWineDir.exists() && !canonicalWineDir.mkdirs()) {
            Log.e("ContentsManager", "Failed to create canonical wine library directory: " + canonicalWineDir.getAbsolutePath());
            return;
        }

        for (File archDir : archDirs) {
            File dest = new File(canonicalWineDir, archDir.getName());
            if (dest.exists()) continue;

            boolean moved = archDir.renameTo(dest);
            if (!moved) {
                moved = FileUtils.copy(archDir, dest);
                if (moved) FileUtils.delete(archDir);
            }

            if (moved) {
                Log.d("ContentsManager", "Moved " + archDir.getAbsolutePath() + " -> " + dest.getAbsolutePath());
            } else {
                Log.e("ContentsManager", "Failed to normalize Wine library path for " + archDir.getAbsolutePath());
            }
        }
    }

    private boolean isWineArchitectureDirectory(@Nullable String name) {
        if (name == null) return false;
        return name.equals("i386-windows")
                || name.equals("x86_64-windows")
                || name.equals("aarch64-windows")
                || name.equals("i386-unix")
                || name.equals("x86_64-unix")
                || name.equals("aarch64-unix");
    }

    private void setExecutablePermissionsRecursive(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                setExecutablePermissionsRecursive(file);
                continue;
            }
            FileUtils.chmod(file, 0755);
        }
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
            dirTemplateMap.put("${localbin}", imagefsPath + "/usr/local/bin");
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
                    case CONTENT_TYPE_DGVOODOO -> DGVOODOO_TRUST_FILES;
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
        if (profile != null && profile.isWineProtonFamily()) {
            String runtimeModel = profile.getRuntimeModel();
            if (!runtimeModel.isEmpty()) {
                return profile.type.toString() + '-' + runtimeModel + '-' + profile.verName + '-' + profile.verCode;
            }
        }
        return profile.type.toString() + '-' + profile.verName + '-' + profile.verCode;
    }

    @Nullable
    public ContentProfile resolveBestRuntimeProfile(String entryName) {
        return resolveBestRuntimeProfile(entryName, null);
    }

    @Nullable
    public ContentProfile resolveBestRuntimeProfile(String entryName, @Nullable String requestedRuntimeModel) {
        RuntimeEntryParts requested = RuntimeEntryParts.parse(entryName);
        if (requested == null) {
            ContentProfile direct = getProfileByEntryName(entryName);
            if (direct != null && direct.locallyInstalled && direct.isRuntimeModelCompatible(requestedRuntimeModel)) {
                return direct;
            }
            return null;
        }
        requested = requested.withRuntimeModel(resolveRequestedRuntimeModel(requestedRuntimeModel, requested));

        ContentProfile protonBest = null;
        ContentProfile wineBest = null;
        for (ContentProfile.ContentType type : new ContentProfile.ContentType[] {
                ContentProfile.ContentType.CONTENT_TYPE_PROTON,
                ContentProfile.ContentType.CONTENT_TYPE_WINE
        }) {
            List<ContentProfile> profiles = profilesMap != null ? profilesMap.get(type) : null;
            if (profiles == null) continue;
            for (ContentProfile profile : profiles) {
                if (profile == null || !profile.locallyInstalled || !profile.isWineProtonFamily()) continue;
                if (profile.verName == null || !requested.versionName.equalsIgnoreCase(profile.verName)) continue;
                if (!profile.isRuntimeModelCompatible(requested.runtimeModel)) continue;
                if (profile.isProtonLike()) {
                    protonBest = pickBetterRuntimeCandidate(protonBest, profile, requested);
                } else {
                    wineBest = pickBetterRuntimeCandidate(wineBest, profile, requested);
                }
            }
        }

        if (requested.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
            if (protonBest != null) return protonBest;
        } else {
            if (wineBest != null) return wineBest;
        }

        ContentProfile exact = getProfileByEntryName(entryName);
        if (exact != null && exact.locallyInstalled && exact.isRuntimeModelCompatible(requested.runtimeModel)) {
            return exact;
        }
        return null;
    }

    public String resolveBestRuntimeEntry(String entryName) {
        return resolveBestRuntimeEntry(entryName, null);
    }

    public String resolveBestRuntimeEntry(String entryName, @Nullable String requestedRuntimeModel) {
        ContentProfile profile = resolveBestRuntimeProfile(entryName, requestedRuntimeModel);
        return profile != null ? getEntryName(profile) : entryName;
    }

    public ContentProfile getProfileByEntryName(String entryName) {
        if (entryName == null || entryName.trim().isEmpty()) return null;
        RuntimeEntryParts runtimeEntry = RuntimeEntryParts.parse(entryName);
        if (runtimeEntry != null) {
            List<ContentProfile> profiles = profilesMap != null ? profilesMap.get(runtimeEntry.type) : null;
            if (profiles == null) return null;

            for (ContentProfile profile : profiles) {
                if (profile == null) continue;
                if (profile.verCode != runtimeEntry.versionCode) continue;
                if (profile.verName == null || !runtimeEntry.versionName.equalsIgnoreCase(profile.verName)) continue;
                if (!profile.isRuntimeModelCompatible(runtimeEntry.runtimeModel)) continue;
                return profile;
            }
            return null;
        }

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
                if (versionName.equals(profile.verName) && Integer.parseInt(versionCode) == profile.verCode) {
                    return profile;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    @Nullable
    public ContentProfile findProfileByVersion(ContentProfile.ContentType type, String versionName, boolean installedOnly) {
        if (type == null || versionName == null) return null;
        String normalizedVersion = versionName.trim();
        if (normalizedVersion.isEmpty()) return null;

        ContentProfile direct = getProfileByEntryName(normalizedVersion);
        if (direct != null && direct.type == type && (!installedOnly || direct.locallyInstalled)) {
            return direct;
        }

        String typePrefix = type.toString() + "-";
        if (normalizedVersion.regionMatches(true, 0, typePrefix, 0, typePrefix.length())) {
            normalizedVersion = normalizedVersion.substring(typePrefix.length()).trim();
        }

        List<ContentProfile> profiles = profilesMap != null ? profilesMap.get(type) : null;
        if (profiles == null) return null;

        ContentProfile best = null;
        for (ContentProfile profile : profiles) {
            if (profile == null) continue;
            if (installedOnly && !profile.locallyInstalled) continue;
            if (profile.verName == null || !normalizedVersion.equalsIgnoreCase(profile.verName)) continue;
            best = pickPreferredVersionCandidate(best, profile);
        }
        return best;
    }

    private ContentProfile pickPreferredVersionCandidate(ContentProfile currentBest, ContentProfile candidate) {
        if (candidate == null) return currentBest;
        if (currentBest == null) return candidate;

        int publishedCompare = comparePublishedAt(candidate.publishedAt, currentBest.publishedAt);
        if (publishedCompare > 0) return candidate;
        if (publishedCompare < 0) return currentBest;

        if (candidate.verCode > currentBest.verCode) return candidate;
        return currentBest;
    }

    private int comparePublishedAt(String left, String right) {
        String normalizedLeft = left == null ? "" : left.trim();
        String normalizedRight = right == null ? "" : right.trim();
        if (normalizedLeft.isEmpty() && normalizedRight.isEmpty()) return 0;
        if (normalizedLeft.isEmpty()) return -1;
        if (normalizedRight.isEmpty()) return 1;
        return normalizedLeft.compareToIgnoreCase(normalizedRight);
    }

    public boolean applyContent(ContentProfile profile) {
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                || profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON
                || profile.type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) {
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

    private void repairInstalledRuntimeOverlays() {
        File sharedRuntimeDir = ImageFs.find(context).getOptDir();
        if (!sharedRuntimeDir.exists()) sharedRuntimeDir.mkdirs();

        migrateLegacyRuntimeDir(getLegacyRuntimeTypeDir(context, ContentProfile.ContentType.CONTENT_TYPE_WINE), sharedRuntimeDir);
        migrateLegacyRuntimeDir(getLegacyRuntimeTypeDir(context, ContentProfile.ContentType.CONTENT_TYPE_PROTON), sharedRuntimeDir);

        File[] installedRoots = sharedRuntimeDir.listFiles();
        if (installedRoots == null) return;
        for (File installRoot : installedRoots) {
            if (installRoot == null || !installRoot.isDirectory()) continue;
            File profileFile = new File(installRoot, PROFILE_NAME);
            if (!profileFile.isFile()) continue;
            ContentProfile profile = normalizeImportedProfile(readProfile(profileFile), null);
            if (profile == null || !profile.isWineProtonFamily()) continue;
            File normalizedRoot = migrateRuntimeInstallRoot(installRoot, getInstallDir(context, profile));
            persistProfileMetadata(new File(normalizedRoot, PROFILE_NAME), profile);
        }
    }

    private void migrateLegacyRuntimeDir(File legacyDir, File sharedRuntimeDir) {
        if (legacyDir == null || sharedRuntimeDir == null || !legacyDir.isDirectory()) return;
        File[] installedRoots = legacyDir.listFiles();
        if (installedRoots == null) return;

        for (File installRoot : installedRoots) {
            if (installRoot == null || !installRoot.isDirectory()) continue;
            File profileFile = new File(installRoot, PROFILE_NAME);
            if (!profileFile.isFile()) continue;
            ContentProfile profile = normalizeImportedProfile(readProfile(profileFile), null);
            if (profile == null || !profile.isWineProtonFamily()) continue;

            File targetRoot = migrateRuntimeInstallRoot(installRoot, getInstallDir(context, profile));
            persistProfileMetadata(new File(targetRoot, PROFILE_NAME), profile);
        }
    }

    private File migrateRuntimeInstallRoot(File installRoot, File canonicalRoot) {
        if (installRoot == null) return canonicalRoot;
        if (canonicalRoot == null || installRoot.equals(canonicalRoot)) return installRoot;
        if (canonicalRoot.exists()) {
            FileUtils.delete(installRoot);
            return canonicalRoot;
        }

        File parent = canonicalRoot.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        boolean moved = installRoot.renameTo(canonicalRoot);
        if (!moved) {
            moved = FileUtils.copy(installRoot, canonicalRoot);
            if (moved) FileUtils.delete(installRoot);
        }
        return moved ? canonicalRoot : installRoot;
    }

    private boolean hasCanonicalProtonInstall(File protonDir, String versionName, int versionCode) {
        File[] installedRoots = protonDir.listFiles();
        if (installedRoots == null) return false;
        for (File installRoot : installedRoots) {
            if (installRoot == null || !installRoot.isDirectory()) continue;
            File profileFile = new File(installRoot, PROFILE_NAME);
            if (!profileFile.isFile()) continue;
            ContentProfile profile = readProfile(profileFile);
            if (profile == null || profile.type != ContentProfile.ContentType.CONTENT_TYPE_PROTON) continue;
            if (profile.verName == null || !profile.verName.equalsIgnoreCase(versionName)) continue;
            if (profile.verCode >= versionCode) return true;
        }
        return false;
    }

    private ContentProfile normalizeImportedProfile(ContentProfile profile, @Nullable ContentProfile remoteHint) {
        if (profile == null) return null;
        if (remoteHint != null) {
            if (profile.isWineProtonFamily() && remoteHint.isWineProtonFamily() && profile.type != remoteHint.type) {
                profile.type = remoteHint.type;
            }
            profile.mergeRemoteMetadata(remoteHint);
            if (profile.channel == null || profile.channel.trim().isEmpty()) {
                profile.channel = remoteHint.getChannel();
            }
            if (profile.delivery == null || profile.delivery.trim().isEmpty()) {
                profile.delivery = remoteHint.getDelivery();
            }
        }
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_WINE && profile.isProtonLike()) {
            profile.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        }
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON && (profile.displayCategory == null || profile.displayCategory.trim().isEmpty())) {
            profile.displayCategory = "Proton";
        }
        profile.runtimeModel = profile.getRuntimeModel();
        return profile;
    }

    private ContentProfile pickBetterRuntimeCandidate(ContentProfile current, ContentProfile candidate, RuntimeEntryParts requested) {
        if (candidate == null) return current;
        if (current == null) return candidate;

        int currentScore = computeRuntimeCandidateScore(current, requested);
        int candidateScore = computeRuntimeCandidateScore(candidate, requested);
        if (candidateScore != currentScore) {
            return candidateScore > currentScore ? candidate : current;
        }
        if (candidate.verCode != current.verCode) {
            return candidate.verCode > current.verCode ? candidate : current;
        }
        return candidate.getDisplayCategory().compareToIgnoreCase(current.getDisplayCategory()) < 0 ? candidate : current;
    }

    private int computeRuntimeCandidateScore(ContentProfile profile, RuntimeEntryParts requested) {
        int score = 0;
        String requestedArch = requested.archHint;
        String profileArch = resolveRuntimeArchHint(profile);
        if (!requestedArch.isEmpty() && requestedArch.equalsIgnoreCase(profileArch)) score += 6;
        if (!requested.runtimeModel.isEmpty() && requested.runtimeModel.equalsIgnoreCase(profile.getRuntimeModel())) score += 8;
        if (requested.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON && profile.isProtonLike()) score += 4;
        if (requested.type == ContentProfile.ContentType.CONTENT_TYPE_WINE && profile.isWineLike()) score += 4;
        if (requested.type == profile.type) score += 2;
        return score;
    }

    private String resolveRequestedRuntimeModel(@Nullable String requestedRuntimeModel, RuntimeEntryParts requested) {
        if (requested != null && requested.runtimeModel != null && !requested.runtimeModel.isEmpty()) {
            return requested.runtimeModel;
        }
        return ContentProfile.normalizeRuntimeModel(requestedRuntimeModel);
    }

    private String resolveRuntimeArchHint(ContentProfile profile) {
        if (profile == null) return "";
        String combined = ((profile.verName == null ? "" : profile.verName) + " "
                + (profile.desc == null ? "" : profile.desc) + " "
                + (profile.artifactName == null ? "" : profile.artifactName)).toLowerCase(Locale.US);
        if (combined.contains("arm64ec") || combined.contains("arm64-ec")) return "arm64ec";
        if (combined.contains("x86_64") || combined.contains("x86-64") || combined.contains("amd64")) return "x86_64";
        if (combined.contains("arm64") || combined.contains("aarch64")) return "arm64";
        if (combined.contains("x86")) return "x86";
        return "";
    }

    private static final class RuntimeEntryParts {
        private final ContentProfile.ContentType type;
        private final String versionName;
        private final int versionCode;
        private final String archHint;
        private final String runtimeModel;

        private RuntimeEntryParts(ContentProfile.ContentType type, String versionName, int versionCode, String archHint, String runtimeModel) {
            this.type = type;
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.archHint = archHint;
            this.runtimeModel = runtimeModel;
        }

        private RuntimeEntryParts withRuntimeModel(String requestedRuntimeModel) {
            return new RuntimeEntryParts(type, versionName, versionCode, archHint, requestedRuntimeModel);
        }

        @Nullable
        private static RuntimeEntryParts parse(String entryName) {
            if (entryName == null || entryName.trim().isEmpty()) return null;
            int firstDashIndex = entryName.indexOf('-');
            int lastDashIndex = entryName.lastIndexOf('-');
            if (firstDashIndex <= 0 || lastDashIndex <= firstDashIndex) return null;

            String typeName = entryName.substring(0, firstDashIndex);
            ContentProfile.ContentType type = ContentProfile.ContentType.getTypeByName(typeName);
            if (type == null || (type != ContentProfile.ContentType.CONTENT_TYPE_WINE
                    && type != ContentProfile.ContentType.CONTENT_TYPE_PROTON)) {
                return null;
            }

            int versionCode;
            try {
                versionCode = Integer.parseInt(entryName.substring(lastDashIndex + 1).trim());
            } catch (Exception ignored) {
                return null;
            }

            String versionName = entryName.substring(firstDashIndex + 1, lastDashIndex).trim();
            if (versionName.isEmpty()) return null;

            String runtimeModel = "";
            int nestedDashIndex = versionName.indexOf('-');
            if (nestedDashIndex > 0) {
                String explicitRuntimeModel = ContentProfile.normalizeRuntimeModel(versionName.substring(0, nestedDashIndex));
                if (!explicitRuntimeModel.isEmpty()) {
                    runtimeModel = explicitRuntimeModel;
                    versionName = versionName.substring(nestedDashIndex + 1).trim();
                }
            }
            if (runtimeModel.isEmpty()) {
                runtimeModel = ContentProfile.inferRuntimeModel(type, versionName, entryName);
            }

            String lowerVersion = versionName.toLowerCase(Locale.US);
            String archHint = "";
            if (lowerVersion.contains("arm64ec") || lowerVersion.contains("arm64-ec")) archHint = "arm64ec";
            else if (lowerVersion.contains("x86_64") || lowerVersion.contains("x86-64") || lowerVersion.contains("amd64")) archHint = "x86_64";
            else if (lowerVersion.contains("arm64") || lowerVersion.contains("aarch64")) archHint = "arm64";
            else if (lowerVersion.contains("x86")) archHint = "x86";

            return new RuntimeEntryParts(type, versionName, versionCode, archHint, runtimeModel);
        }
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
        profile.artifactName = remoteHint.artifactName;
        profile.publishedAt = remoteHint.publishedAt;
        profile.releaseNotes = remoteHint.releaseNotes;
        profile.runtimeModel = remoteHint.getRuntimeModel();
        profile.vulkanApiMin = remoteHint.vulkanApiMin;
        profile.vulkanApiMax = remoteHint.vulkanApiMax;
        profile.vulkanSdkVersion = remoteHint.vulkanSdkVersion;

        switch (remoteHint.type) {
            case CONTENT_TYPE_DXVK -> profile.fileList = synthesizeDxvkFiles(rootDir);
            case CONTENT_TYPE_VKD3D -> profile.fileList = synthesizeVkd3dFiles(rootDir);
            case CONTENT_TYPE_VULKAN_SDK -> profile.fileList = synthesizeVulkanSdkFiles(rootDir);
            case CONTENT_TYPE_DGVOODOO -> profile.fileList = synthesizeDgVoodooFiles(rootDir);
            case CONTENT_TYPE_BOX64 -> profile.fileList = synthesizeSingleFile(rootDir, "box64", "${localbin}/box64");
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
            object.put(ContentProfile.MARK_ARTIFACT_NAME, profile.artifactName == null ? "" : profile.artifactName);
            object.put(ContentProfile.MARK_PUBLISHED_AT, profile.publishedAt == null ? "" : profile.publishedAt);
            object.put(ContentProfile.MARK_RELEASE_NOTES, profile.releaseNotes == null ? "" : profile.releaseNotes);
            if (profile.isWineProtonFamily()) {
                object.put(ContentProfile.MARK_RUNTIME_MODEL, profile.getRuntimeModel());
            }
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
        String prefixPackPath = findRelativeFile(rootDir, "prefixPack.tzst");
        if (prefixPackPath == null) {
            prefixPackPath = findRelativeFile(rootDir, "prefixPack.txz");
        }
        if (binPath == null || libPath == null || prefixPackPath == null) return;

        profile.wineBinPath = binPath;
        profile.wineLibPath = libPath;
        profile.winePrefixPack = prefixPackPath;
        profile.runtimeModel = profile.getRuntimeModel();
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

    private List<ContentProfile.ContentFile> synthesizeVulkanSdkFiles(File rootDir) {
        LinkedHashMap<String, ContentProfile.ContentFile> filesByTarget = new LinkedHashMap<>();
        collectTreeMappings(rootDir, "usr/share/vulkan", "${sharedir}/vulkan", filesByTarget);
        collectTreeMappings(rootDir, "usr/share/vulkan-sdk", "${sharedir}/vulkan-sdk", filesByTarget);
        collectTreeMappings(rootDir, "share/vulkan", "${sharedir}/vulkan", filesByTarget);
        collectTreeMappings(rootDir, "share/vulkan-sdk", "${sharedir}/vulkan-sdk", filesByTarget);
        collectTreeMappings(rootDir, "usr/lib/vulkan", "${libdir}/vulkan", filesByTarget);
        collectTreeMappings(rootDir, "usr/lib/vulkan-sdk", "${libdir}/vulkan-sdk", filesByTarget);
        collectTreeMappings(rootDir, "lib/vulkan", "${libdir}/vulkan", filesByTarget);
        collectTreeMappings(rootDir, "lib/vulkan-sdk", "${libdir}/vulkan-sdk", filesByTarget);
        return new ArrayList<>(filesByTarget.values());
    }

    private List<ContentProfile.ContentFile> synthesizeDgVoodooFiles(File rootDir) {
        String[][] mappings = {
                {"d3d8.dll", "${system32}/D3D8.dll"},
                {"d3d8_dgvoodoo.dll", "${system32}/D3D8_dgvoodoo.dll"},
                {"d3d9.dll", "${system32}/D3D9.dll"},
                {"d3d9_dgvoodoo.dll", "${system32}/D3D9_dgvoodoo.dll"},
                {"d3dimm.dll", "${system32}/D3DImm.dll"},
                {"d3dimm_dgvoodoo.dll", "${system32}/D3DImm_dgvoodoo.dll"},
                {"ddraw.dll", "${system32}/DDraw.dll"},
                {"ddraw_dgvoodoo.dll", "${system32}/DDraw_dgvoodoo.dll"},
                {"glide.dll", "${system32}/Glide.dll"},
                {"glide2x.dll", "${system32}/Glide2x.dll"},
                {"glide3x.dll", "${system32}/Glide3x.dll"},
                {"glide3xnapalm.dll", "${system32}/Glide3xNapalm.dll"}
        };

        ArrayList<ContentProfile.ContentFile> files = new ArrayList<>();
        for (String[] mapping : mappings) {
            String relative = findRelativeFile(rootDir, mapping[0]);
            if (relative == null) continue;
            ContentProfile.ContentFile item = new ContentProfile.ContentFile();
            item.source = relative;
            item.target = mapping[1];
            files.add(item);
        }
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

    private void collectTreeMappings(
            File rootDir,
            String sourcePrefix,
            String targetPrefix,
            Map<String, ContentProfile.ContentFile> filesByTarget
    ) {
        if (rootDir == null || sourcePrefix == null || targetPrefix == null || filesByTarget == null) return;
        File sourceRoot = new File(rootDir, sourcePrefix);
        if (!sourceRoot.isDirectory()) return;
        collectTreeMappingsRecursive(rootDir, sourceRoot, sourceRoot, targetPrefix, filesByTarget);
    }

    private void collectTreeMappingsRecursive(
            File rootDir,
            File sourceRoot,
            File current,
            String targetPrefix,
            Map<String, ContentProfile.ContentFile> filesByTarget
    ) {
        File[] children = current.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                collectTreeMappingsRecursive(rootDir, sourceRoot, child, targetPrefix, filesByTarget);
                continue;
            }
            String relative = relativizePath(rootDir, child);
            String relativeFromPrefix = relativizePath(sourceRoot, child);
            String normalizedTarget = targetPrefix;
            if (relativeFromPrefix != null && !relativeFromPrefix.trim().isEmpty()) {
                normalizedTarget += "/" + relativeFromPrefix;
            }
            if (filesByTarget.containsKey(normalizedTarget)) continue;
            ContentProfile.ContentFile item = new ContentProfile.ContentFile();
            item.source = relative;
            item.target = normalizedTarget;
            filesByTarget.put(normalizedTarget, item);
        }
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
