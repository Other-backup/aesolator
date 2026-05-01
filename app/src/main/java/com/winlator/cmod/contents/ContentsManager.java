package com.winlator.cmod.contents;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.WineUtils;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ContentsManager {
    public static final String PROFILE_NAME = "profile.json";
    public static final String REMOTE_SCOPE_COMMUNITY = "community";
    public static final String REMOTE_SCOPE_ARCHIVE = "archive";
    public static final String REMOTE_SCOPE_GAMEHUB = "gamehub";
    public static final String REMOTE_SCOPE_NIGHTLIES = "nightlies";
    public static final String REMOTE_SCOPE_WCPHUB = "wcphub";
    public static final String REMOTE_SCOPE_GAMENATIVE_PROTON = "gamenative_proton";
    public static final String REMOTE_SCOPE_ANDREVTO_PROTON = "andrevto_proton";
    public static final String REMOTE_SCOPE_HYDRATED_RUNTIME = "hydrated_runtime";
    private static final String FALLBACK_PREFIX_PACK_NAME = "prefixPack.tzst";
    private static final String FALLBACK_PREFIX_PACK_COMMON_ASSET = "container_pattern_common.tzst";
    private static final String FALLBACK_PREFIX_PACK_GAMENATIVE_ASSET = "container_pattern_gamenative.tzst";
    public static final String REMOTE_PROFILES = "https://raw.githubusercontent.com/Arihany/WinlatorWCPHub/main/pack.json";
    public static final String REMOTE_PROFILES_AE = "https://raw.githubusercontent.com/kosoymiki/aesolator/main/contents/contents.json";
    public static final String REMOTE_GAMEHUB_RELEASES = "https://api.github.com/repos/The412Banner/Gamehub-Components/releases?per_page=100";
    public static final String REMOTE_GAMEHUB_COMPONENTS = "https://raw.githubusercontent.com/The412Banner/Gamehub-Components/main/sp_winemu_all_components12.xml";
    public static final String REMOTE_THE412BANNER_NIGHTLIES_RELEASES = "https://api.github.com/repos/The412Banner/Nightlies/releases?per_page=100";
    public static final String REMOTE_THE412BANNER_NIGHTLIES_RELEASES_ATOM = "https://github.com/The412Banner/Nightlies/releases.atom";
    public static final String REMOTE_WINE_PROTON_OVERLAY = REMOTE_PROFILES_AE;
    private static final String TAG = "ContentsManager";
    private static final long MAIN_THREAD_RUNTIME_REPAIR_COOLDOWN_MS = 5 * 60 * 1000L;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean RUNTIME_REPAIR_RUNNING = new AtomicBoolean(false);
    private static final ExecutorService RUNTIME_REPAIR_EXECUTOR = Executors.newSingleThreadExecutor((runnable) -> {
        Thread thread = new Thread(runnable, "contents-runtime-overlay-repair");
        thread.setDaemon(true);
        return thread;
    });
    private static volatile long lastRuntimeRepairFinishedAtMs;
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
    private static final String[] DGVOODOO_PACKAGE_RUNTIME_ARCHES = {"x86", "x64", "arm64", "arm64ec"};
    private static final String[] DGVOODOO_PACKAGE_RUNTIME_FILES = {
            "D3D8.dll", "D3D9.dll", "D3DImm.dll", "DDraw.dll",
            "Glide.dll", "Glide2x.dll", "Glide3x.dll", "Glide3xNapalm.dll"
    };
    private static final String[] DGVOODOO_PACKAGE_ROOT_FILES = {
            "dgVoodoo.conf", "dgVoodooCpl.exe"
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

    public static final class InstalledProfileState {
        public final boolean present;
        public final boolean usable;
        public final String brokenReason;

        private InstalledProfileState(boolean present, boolean usable, @Nullable String brokenReason) {
            this.present = present;
            this.usable = usable;
            this.brokenReason = brokenReason == null ? "" : brokenReason.trim();
        }

        public boolean isBroken() {
            return present && !usable;
        }
    }

    public static final class InstalledProfileDiagnostics {
        public final InstalledProfileState state;
        public final String entryName;
        public final String requestedIdentity;
        public final String type;
        public final String runtimeModel;
        public final String canonicalInstallDir;
        public final String resolvedInstallDir;
        public final String runtimeRoot;
        public final String profileJsonPath;
        public final boolean profileJsonPresent;
        public final boolean runtimeRootPresent;
        public final boolean runtimePayloadPresent;
        public final boolean aliasResolved;

        private InstalledProfileDiagnostics(@NonNull InstalledProfileState state,
                                           @NonNull String entryName,
                                           @NonNull String requestedIdentity,
                                           @NonNull String type,
                                           @NonNull String runtimeModel,
                                           @NonNull String canonicalInstallDir,
                                           @NonNull String resolvedInstallDir,
                                           @NonNull String runtimeRoot,
                                           @NonNull String profileJsonPath,
                                           boolean profileJsonPresent,
                                           boolean runtimeRootPresent,
                                           boolean runtimePayloadPresent,
                                           boolean aliasResolved) {
            this.state = state;
            this.entryName = entryName;
            this.requestedIdentity = requestedIdentity;
            this.type = type;
            this.runtimeModel = runtimeModel;
            this.canonicalInstallDir = canonicalInstallDir;
            this.resolvedInstallDir = resolvedInstallDir;
            this.runtimeRoot = runtimeRoot;
            this.profileJsonPath = profileJsonPath;
            this.profileJsonPresent = profileJsonPresent;
            this.runtimeRootPresent = runtimeRootPresent;
            this.runtimePayloadPresent = runtimePayloadPresent;
            this.aliasResolved = aliasResolved;
        }
    }

    private final Context context;

    private HashMap<ContentProfile.ContentType, List<ContentProfile>> profilesMap;

    private final LinkedHashMap<String, ArrayList<ContentProfile>> remoteProfilesByScope = new LinkedHashMap<>();
    private final LinkedHashMap<String, InstalledRuntimeRoot> installedRuntimeRootByKey = new LinkedHashMap<>();
    private final LinkedHashMap<String, InstalledProfileState> installedProfileStateByKey = new LinkedHashMap<>();

    public ContentsManager(Context context) {
        Context applicationContext = context != null ? context.getApplicationContext() : null;
        this.context = applicationContext != null ? applicationContext : context;
        this.preferences = context.getSharedPreferences("contents_manager_prefs", Context.MODE_PRIVATE);
    }

    private void clearRuntimeResolutionCaches() {
        installedRuntimeRootByKey.clear();
        installedProfileStateByKey.clear();
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
        setRemoteProfilesForScope(REMOTE_SCOPE_COMMUNITY, json, includeBeta, ignoreRepoManaged, false, false);
    }

    public void setHubRemoteProfiles(String json) {
        // WCPHub must stay visible for overlapping families too; the source
        // selector, not the parser, is what keeps archive/hub provenance apart.
        setRemoteProfilesForScope(REMOTE_SCOPE_WCPHUB, json, false, false, false, true);
    }

    public void setArchiveRemoteProfiles(String json) {
        setRemoteProfilesForScope(REMOTE_SCOPE_ARCHIVE, json, false, false, true, true);
    }

    public void setGamehubRemoteProfiles(String json) {
        setRemoteProfilesForScope(REMOTE_SCOPE_GAMEHUB, json, false, false, false, true);
    }

    public void setNightliesRemoteProfiles(String json) {
        setRemoteProfilesForScope(REMOTE_SCOPE_NIGHTLIES, json, false, false, false, true);
    }

    public void setGameNativeProtonRemoteProfiles(String json) {
        setRemoteProfilesForScope(REMOTE_SCOPE_GAMENATIVE_PROTON, json, false, false, false, true);
    }

    public void setAndreVtoProtonRemoteProfiles(String json) {
        setRemoteProfilesForScope(REMOTE_SCOPE_ANDREVTO_PROTON, json, false, false, false, true);
    }

    public void setHydratedRuntimeProfiles(String json) {
        setRemoteProfilesForScope(REMOTE_SCOPE_HYDRATED_RUNTIME, json, false, false, false, true);
    }

    public boolean hasRemoteProfilesForScope(@Nullable String scopeKey) {
        synchronized (remoteProfilesByScope) {
            ArrayList<ContentProfile> scoped = remoteProfilesByScope.get(normalizeRemoteScopeKey(scopeKey));
            return scoped != null && !scoped.isEmpty();
        }
    }

    public int getRemoteProfileCountForScope(@Nullable String scopeKey) {
        synchronized (remoteProfilesByScope) {
            ArrayList<ContentProfile> scoped = remoteProfilesByScope.get(normalizeRemoteScopeKey(scopeKey));
            return scoped == null ? 0 : scoped.size();
        }
    }

    private void setRemoteProfilesForScope(
            @Nullable String scopeKey,
            String json,
            boolean includeBeta,
            boolean ignoreRepoManaged,
            boolean onlyRepoManaged,
            boolean keepAllChannels
    ) {
        ArrayList<ContentProfile> parsedProfiles = parseRemoteProfiles(json, includeBeta, ignoreRepoManaged, onlyRepoManaged, keepAllChannels);
        synchronized (remoteProfilesByScope) {
            remoteProfilesByScope.put(normalizeRemoteScopeKey(scopeKey), parsedProfiles);
        }
        syncContents();
    }

    private String normalizeRemoteScopeKey(@Nullable String scopeKey) {
        String normalized = scopeKey == null ? "" : scopeKey.trim().toLowerCase(Locale.US);
        return normalized.isEmpty() ? REMOTE_SCOPE_COMMUNITY : normalized;
    }

    private ArrayList<ContentProfile> collectRemoteProfilesSnapshot() {
        ArrayList<ContentProfile> snapshot = new ArrayList<>();
        synchronized (remoteProfilesByScope) {
            for (ArrayList<ContentProfile> scopedProfiles : remoteProfilesByScope.values()) {
                if (scopedProfiles == null || scopedProfiles.isEmpty()) continue;
                snapshot.addAll(scopedProfiles);
            }
        }
        return snapshot;
    }

    private ArrayList<ContentProfile> parseRemoteProfiles(
            String json,
            boolean includeBeta,
            boolean ignoreRepoManaged,
            boolean onlyRepoManaged,
            boolean keepAllChannels
    ) {
        ArrayList<ContentProfile> parsedProfiles = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return parsedProfiles;
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
                    remoteProfile.runtimeModel = readRuntimeModelHint(object);
                    remoteProfile.vulkanApiMin = parseOptionalInt(object.opt(ContentProfile.MARK_VULKAN_API_MIN), 0);
                    remoteProfile.vulkanApiMax = parseOptionalInt(object.opt(ContentProfile.MARK_VULKAN_API_MAX), 0);
                    remoteProfile.delivery = object.optString(ContentProfile.MARK_DELIVERY, ContentProfile.DELIVERY_REMOTE).trim();
                    remoteProfile.channel = object.optString(ContentProfile.MARK_CHANNEL, "").trim().toLowerCase(Locale.US);
                    remoteProfile.setInstalledLocally(false);

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

                    parsedProfiles.add(remoteProfile);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse remote profile row", e);
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse remote profile feed", e);
        }
        return parsedProfiles;
    }

    private String readRuntimeModelHint(JSONObject object) {
        if (object == null) return "";

        String runtimeModel = ContentProfile.normalizeRuntimeModel(object.optString(ContentProfile.MARK_RUNTIME_MODEL, ""));
        if (!runtimeModel.isEmpty()) return runtimeModel;

        runtimeModel = ContentProfile.normalizeRuntimeModel(object.optString("runtimeClassTarget", ""));
        if (!runtimeModel.isEmpty()) return runtimeModel;

        runtimeModel = ContentProfile.normalizeRuntimeModel(object.optString("runtimeClassDetected", ""));
        if (!runtimeModel.isEmpty()) return runtimeModel;

        JSONObject runtimeObject = object.optJSONObject("runtime");
        if (runtimeObject != null) {
            runtimeModel = ContentProfile.normalizeRuntimeModel(runtimeObject.optString("runtimeClassTarget", ""));
            if (!runtimeModel.isEmpty()) return runtimeModel;

            runtimeModel = ContentProfile.normalizeRuntimeModel(runtimeObject.optString("runtimeClassDetected", ""));
            if (!runtimeModel.isEmpty()) return runtimeModel;

            runtimeModel = ContentProfile.normalizeRuntimeModel(runtimeObject.optString("target", ""));
            if (!runtimeModel.isEmpty()) return runtimeModel;
        }

        return "";
    }

    public void syncContents() {
        syncContents(true);
    }

    public void syncContentsForLaunch() {
        repairPackageRuntimeRootProfiles(false);
        syncContents(false);
    }

    private void syncContents(boolean repairRuntimeOverlays) {
        if (repairRuntimeOverlays) {
            repairInstalledRuntimeOverlaysForCurrentThread();
        }
        clearRuntimeResolutionCaches();
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
                            classifyRuntimeProfileFromPayload(file, profile);
                            profile.setInstalledLocally(true);
                            registerInstalledRuntimeRoot(file, profile);
                            profiles.add(profile);
                            profileByEntry.put(getEntryName(profile), profile);
                            profileFileByProfile.put(profile, proFile);
                        }
                    }
                }
            }

            List<ContentProfile> remoteProfiles = collectRemoteProfilesSnapshot();
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

    public void syncContentsAsync(@Nullable Runnable onFinished) {
        if (!RUNTIME_REPAIR_RUNNING.compareAndSet(false, true)) {
            MAIN_HANDLER.post(() -> {
                syncContents();
                if (onFinished != null) onFinished.run();
            });
            return;
        }

        Context repairContext = context;
        RUNTIME_REPAIR_EXECUTOR.execute(() -> {
            try {
                new ContentsManager(repairContext).repairInstalledRuntimeOverlays();
                lastRuntimeRepairFinishedAtMs = System.currentTimeMillis();
            } catch (Exception e) {
                Log.w(TAG, "Async runtime overlay repair failed", e);
            } finally {
                RUNTIME_REPAIR_RUNNING.set(false);
            }
            MAIN_HANDLER.post(() -> {
                syncContents();
                if (onFinished != null) onFinished.run();
            });
        });
    }

    private void repairInstalledRuntimeOverlaysForCurrentThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            scheduleInstalledRuntimeOverlayRepair();
            return;
        }
        repairInstalledRuntimeOverlays();
    }

    private void scheduleInstalledRuntimeOverlayRepair() {
        long lastFinishedAt = lastRuntimeRepairFinishedAtMs;
        long now = System.currentTimeMillis();
        if (lastFinishedAt > 0 && now - lastFinishedAt < MAIN_THREAD_RUNTIME_REPAIR_COOLDOWN_MS) return;
        if (!RUNTIME_REPAIR_RUNNING.compareAndSet(false, true)) return;

        Context repairContext = context;
        RUNTIME_REPAIR_EXECUTOR.execute(() -> {
            try {
                new ContentsManager(repairContext).repairInstalledRuntimeOverlays();
                lastRuntimeRepairFinishedAtMs = System.currentTimeMillis();
            } catch (Exception e) {
                Log.w(TAG, "Background runtime overlay repair failed", e);
            } finally {
                RUNTIME_REPAIR_RUNNING.set(false);
            }
        });
    }

    public void extraContentFile(Uri uri, OnInstallFinishedCallback callback) {
        extraContentFile(uri, null, callback);
    }

    public void extraContentFile(Uri uri, @Nullable ContentProfile remoteHint, OnInstallFinishedCallback callback) {
        cleanTmpDir(context);

        File file = getTmpDir(context);
        String importDisplayName = resolveImportDisplayName(uri);

        String archiveFormat = extractContentArchive(uri, file, importDisplayName);
        if (archiveFormat.isEmpty()) {
            logInstallFailure("CONTENTS_IMPORT_EXTRACTION_FAILED", "contents_import", InstallFailedReason.ERROR_BADTAR, null, remoteHint, file, null, null);
            callback.onFailed(InstallFailedReason.ERROR_BADTAR, null);
            return;
        }
        logContentArchiveExtraction("CONTENTS_IMPORT_ARCHIVE_EXTRACTED", importDisplayName, archiveFormat, file);

        normalizeExtractedImportRoot(file, remoteHint, importDisplayName);
        File proFile = resolveExtractedProfileFile(file);
        ContentProfile profile = repairImportedProfile(
                file,
                proFile != null && proFile.isFile() ? readProfile(proFile) : null,
                remoteHint,
                importDisplayName
        );
        if (profile == null) {
            ContentProfile synthesizedProfile = synthesizeProfileFromExtractedPayload(file, remoteHint, importDisplayName);
            if (synthesizedProfile != null) {
                profile = repairImportedProfile(file, synthesizedProfile, remoteHint, importDisplayName);
                logImportRecovery("CONTENTS_PROFILE_SYNTHESIS_APPLIED", file, remoteHint, profile);
            } else {
                logImportRecovery("CONTENTS_PROFILE_SYNTHESIS_MISS", file, remoteHint, null);
            }
        } else {
            logImportRecovery("CONTENTS_PROFILE_RECOVERY_APPLIED", file, remoteHint, profile);
        }
        if (profile == null) {
            logInstallFailure(
                    "CONTENTS_IMPORT_PROFILE_REJECTED",
                    "contents_import",
                    proFile != null && proFile.isFile() ? InstallFailedReason.ERROR_BADPROFILE : InstallFailedReason.ERROR_NOPROFILE,
                    null,
                    remoteHint,
                    file,
                    null,
                    null
            );
            callback.onFailed(proFile != null && proFile.isFile() ? InstallFailedReason.ERROR_BADPROFILE : InstallFailedReason.ERROR_NOPROFILE, null);
            return;
        }
        if (!writeProfileSnapshot(file, profile)) {
            logInstallFailure("CONTENTS_IMPORT_PROFILE_WRITE_FAILED", "contents_import", InstallFailedReason.ERROR_BADPROFILE, profile, remoteHint, file, null, null);
            callback.onFailed(InstallFailedReason.ERROR_BADPROFILE, null);
            return;
        }
        profile = readProfile(new File(file, PROFILE_NAME));
        if (profile == null) {
            logInstallFailure("CONTENTS_IMPORT_PROFILE_READ_FAILED", "contents_import", InstallFailedReason.ERROR_BADPROFILE, null, remoteHint, file, null, null);
            callback.onFailed(InstallFailedReason.ERROR_BADPROFILE, null);
            return;
        }
        profile = normalizeImportedProfile(profile, remoteHint);
        if (ContentProfileIdentity.isRemoteProfileIdentityMismatch(profile, remoteHint)) {
            logInstallFailure("CONTENTS_IMPORT_IDENTITY_MISMATCH", "contents_import", InstallFailedReason.ERROR_BADPROFILE, profile, remoteHint, file, null, null);
            callback.onFailed(InstallFailedReason.ERROR_BADPROFILE, null);
            return;
        }

        String imagefsPath = context.getFilesDir().getAbsolutePath() + "/imagefs";
        for (ContentProfile.ContentFile contentFile : profile.fileList) {
            File tmpFile = new File(file, contentFile.source);
            if (!tmpFile.exists() || !tmpFile.isFile() || !isSubPath(file.getAbsolutePath(), tmpFile.getAbsolutePath())) {
                logInstallFailure("CONTENTS_IMPORT_MISSING_PAYLOAD", "contents_import", InstallFailedReason.ERROR_MISSINGFILES, profile, remoteHint, file, null, null);
                callback.onFailed(InstallFailedReason.ERROR_MISSINGFILES, null);
                return;
            }

            if (!isTrustedInstallTarget(profile, contentFile.target, imagefsPath)) {
                logInstallFailure("CONTENTS_IMPORT_UNTRUSTED_TARGET", "contents_import", InstallFailedReason.ERROR_UNTRUSTPROFILE, profile, remoteHint, file, null, null);
                callback.onFailed(InstallFailedReason.ERROR_UNTRUSTPROFILE, null);
                return;
            }
        }

        if (profile.isWineProtonFamily() && !hasResolvedRuntimePayload(file, profile)) {
            logInstallFailure("CONTENTS_IMPORT_RUNTIME_INCOMPLETE", "contents_import", InstallFailedReason.ERROR_MISSINGFILES, profile, remoteHint, file, null, null);
            callback.onFailed(InstallFailedReason.ERROR_MISSINGFILES, null);
            return;
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
        return FileUtils.resolveSafeArchiveEntry(rootDir, entry.getName());
    }

    @NonNull
    private String extractContentArchive(Uri uri, File destination, @Nullable String importDisplayName) {
        for (String format : resolveArchiveProbeOrder(importDisplayName)) {
            if (!prepareExtractionDestination(destination)) continue;
            boolean extracted = switch (format) {
                case "zip" -> extractZipSafely(uri, destination);
                case "xz-tar" -> TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, context, uri, destination);
                case "zstd-tar" -> TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, uri, destination);
                case "tar" -> TarCompressorUtils.extractTar(context, uri, destination);
                default -> false;
            };
            if (extracted) return format;
        }
        FileUtils.delete(destination);
        destination.mkdirs();
        return "";
    }

    private boolean prepareExtractionDestination(File destination) {
        if (destination == null) return false;
        if (destination.exists() && !FileUtils.clear(destination)) {
            FileUtils.delete(destination);
        }
        return destination.isDirectory() || destination.mkdirs();
    }

    private List<String> resolveArchiveProbeOrder(@Nullable String importDisplayName) {
        String name = importDisplayName == null ? "" : importDisplayName.trim().toLowerCase(Locale.US);
        ArrayList<String> probes = new ArrayList<>();
        if (name.endsWith(".zip")) {
            addArchiveProbe(probes, "zip");
            addArchiveProbe(probes, "xz-tar");
            addArchiveProbe(probes, "zstd-tar");
            addArchiveProbe(probes, "tar");
            return probes;
        }
        if (name.endsWith(".wcp.xz") || name.endsWith(".txz") || name.endsWith(".tar.xz")) {
            addArchiveProbe(probes, "xz-tar");
            addArchiveProbe(probes, "zstd-tar");
            addArchiveProbe(probes, "zip");
            addArchiveProbe(probes, "tar");
            return probes;
        }
        if (name.endsWith(".wcp.zst") || name.endsWith(".wcp.zstd")
                || name.endsWith(".tzst") || name.endsWith(".tar.zst") || name.endsWith(".tar.zstd")) {
            addArchiveProbe(probes, "zstd-tar");
            addArchiveProbe(probes, "xz-tar");
            addArchiveProbe(probes, "zip");
            addArchiveProbe(probes, "tar");
            return probes;
        }
        if (name.endsWith(".wcp")) {
            addArchiveProbe(probes, "zstd-tar");
            addArchiveProbe(probes, "xz-tar");
            addArchiveProbe(probes, "zip");
            addArchiveProbe(probes, "tar");
            return probes;
        }
        addArchiveProbe(probes, "zip");
        addArchiveProbe(probes, "xz-tar");
        addArchiveProbe(probes, "zstd-tar");
        addArchiveProbe(probes, "tar");
        return probes;
    }

    private void addArchiveProbe(List<String> probes, String format) {
        if (probes == null || format == null || probes.contains(format)) return;
        probes.add(format);
    }

    public void finishInstallContent(ContentProfile profile, OnInstallFinishedCallback callback) {
        File installPath = getInstallDir(context, profile);
        File tmpPath = getTmpDir(context);
        if (!tmpPath.exists() || !tmpPath.isDirectory()) {
            logInstallFailure("CONTENTS_INSTALL_TMP_MISSING", "contents_install", InstallFailedReason.ERROR_UNKNOWN, profile, null, tmpPath, installPath, null);
            callback.onFailed(InstallFailedReason.ERROR_UNKNOWN, null);
            return;
        }

        File typeDir = installPath.getParentFile();
        if (typeDir == null || (!typeDir.exists() && !typeDir.mkdirs())) {
            logInstallFailure("CONTENTS_INSTALL_TYPE_DIR_MISSING", "contents_install", InstallFailedReason.ERROR_UNKNOWN, profile, null, tmpPath, installPath, null);
            callback.onFailed(InstallFailedReason.ERROR_UNKNOWN, null);
            return;
        }

        recoverInterruptedInstall(typeDir, installPath);
        File stageMarker = getInstallStageMarker(typeDir, installPath);
        clearInstallStageMarker(stageMarker);

        if (profile.isWineProtonFamily()) {
            InstalledRuntimeRoot equivalentInstalledRoot = findEquivalentInstalledRuntimeRoot(profile, true);
            File equivalentInstallRoot = resolveMatchedInstalledRuntimeInstallDir(profile, equivalentInstalledRoot, true, true);
            if (equivalentInstalledRoot != null && equivalentInstallRoot != null) {
                File installedProfileFile = new File(equivalentInstallRoot, PROFILE_NAME);
                ContentProfile installedProfile = equivalentInstalledRoot.profile;
                if (installedProfile == null) {
                    installedProfile = normalizeImportedProfile(readProfile(installedProfileFile), profile);
                }
                if (installedProfile != null) {
                    installedProfile.setInstalledLocally(true);
                    installedProfile.mergeRemoteMetadata(profile);
                    persistProfileMetadata(installedProfileFile, installedProfile);
                    FileUtils.delete(tmpPath);
                    logExistingRuntimeReuse(installedProfile, profile, equivalentInstallRoot);
                    callback.onSucceed(installedProfile);
                    return;
                }
            }
        }

        File backupPath = null;
        if (profile.isWineProtonFamily()
                && FileUtils.isSymlink(installPath)
                && !WineUtils.hasRuntimeCorePayload(installPath)) {
            logRuntimeInstallStaleSymlinkRemoved(profile, installPath);
            FileUtils.delete(installPath);
        }
        if (installPath.exists()) {
            if (!isUpdatableLane(profile.type)) {
                logInstallFailure("CONTENTS_INSTALL_ALREADY_EXISTS", "contents_install", InstallFailedReason.ERROR_EXIST, profile, null, tmpPath, installPath, null);
                callback.onFailed(InstallFailedReason.ERROR_EXIST, null);
                return;
            }
            backupPath = new File(typeDir, installPath.getName() + ".bak-" + UUID.randomUUID().toString().replace("-", ""));
            if (!installPath.renameTo(backupPath)) {
                logInstallFailure("CONTENTS_INSTALL_BACKUP_RENAME_FAILED", "contents_install", InstallFailedReason.ERROR_UNKNOWN, profile, null, tmpPath, installPath, null);
                callback.onFailed(InstallFailedReason.ERROR_UNKNOWN, null);
                return;
            }
            if (!writeInstallStageMarker(stageMarker, installPath, backupPath)) {
                backupPath.renameTo(installPath);
                logInstallFailure("CONTENTS_INSTALL_STAGE_MARKER_FAILED", "contents_install", InstallFailedReason.ERROR_UNKNOWN, profile, null, tmpPath, installPath, null);
                callback.onFailed(InstallFailedReason.ERROR_UNKNOWN, null);
                return;
            }
        }

        boolean moved = tmpPath.renameTo(installPath);
        if (!moved) {
            // degrade to recursive copy for filesystems where renameTo can fail.
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
            logInstallFailure("CONTENTS_INSTALL_MOVE_FAILED", "contents_install", InstallFailedReason.ERROR_UNKNOWN, profile, null, tmpPath, installPath, null);
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
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) {
            pruneSupersededDgVoodooInstalls(typeDir, installPath, profile);
        }

        callback.onSucceed(profile);
    }

    private void logRuntimeInstallStaleSymlinkRemoved(ContentProfile profile, File installPath) {
        ForensicLogger.logEvent(
                context,
                "warn",
                "CONTENTS_RUNTIME_INSTALL_STALE_SYMLINK_REMOVED",
                null,
                "contents_install",
                "runtime_install_stale_symlink_removed",
                ForensicLogger.fields(
                        "entry_name", profile != null ? getEntryName(profile) : "",
                        "requested_identity", ContentProfileIdentity.describeProfile(profile),
                        "install_path", installPath != null ? installPath.getAbsolutePath() : "",
                        "symlink_target", installPath != null && FileUtils.isSymlink(installPath)
                                ? FileUtils.readSymlink(installPath)
                                : ""
                )
        );
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
            return ContentProfileParser.parse(FileUtils.readString(file));
        } catch (Exception e) {
            return null;
        }
    }

    public List<ContentProfile> getProfiles(ContentProfile.ContentType type) {
        if (profilesMap != null)
            return profilesMap.get(type);
        return null;
    }

    @Nullable
    public ContentProfile findInstallableRemoteProfile(@Nullable ContentProfile requestedProfile) {
        if (requestedProfile == null || requestedProfile.type == null) return null;
        if (requestedProfile.isRemoteDownloadable()) return requestedProfile;
        List<ContentProfile> profiles = profilesMap != null ? profilesMap.get(requestedProfile.type) : null;
        if (profiles == null || profiles.isEmpty()) return null;

        ContentProfile best = null;
        for (ContentProfile candidate : profiles) {
            if (candidate == null || !candidate.isRemoteDownloadable()) continue;
            if (candidate == requestedProfile) return candidate;
            if (candidate.sameEntry(requestedProfile)
                    || requestedProfile.sameEntry(candidate)
                    || ContentProfileIdentity.areEquivalentProfiles(candidate, requestedProfile)
                    || matchesInstallableRemoteSemanticIdentity(candidate, requestedProfile)) {
                best = pickPreferredRemoteInstallCandidate(best, candidate);
            }
        }
        return best;
    }

    public static File getInstallDir(Context context, ContentProfile profile) {
        if (profile != null && profile.isWineProtonFamily()) {
            return new File(getRuntimeContentTypeDir(context, profile), buildRuntimeInstallRootName(profile));
        }
        return new File(getContentTypeDir(context, profile.type), profile.verName + "-" + profile.verCode);
    }

    private static File getRuntimeContentTypeDir(Context context, ContentProfile profile) {
        String runtimeModel = profile != null ? profile.getRuntimeModel() : "";
        String runtimeIdentity = profile != null ? getEntryName(profile) : "";
        File runtimeRoot = ImageFs.getRuntimeRootDir(context, runtimeModel, runtimeIdentity);
        return new File(runtimeRoot, "opt");
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
            for (File runtimeOptDir : getRuntimeOptDirs()) {
                collectInstallRoots(roots, runtimeOptDir);
            }
            collectPackageRuntimeRoots(roots);
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
            try {
                roots.put(file.getCanonicalPath(), file);
            } catch (Exception ignored) {
                roots.put(file.getAbsolutePath(), file);
            }
        }
    }

    private void collectPackageRuntimeRoots(LinkedHashMap<String, File> roots) {
        if (roots == null) return;
        for (File rootDir : ImageFs.getKnownRootDirs(context)) {
            if (rootDir == null || !rootDir.isDirectory()) continue;
            String name = rootDir.getName();
            if (name == null || !name.startsWith(ImageFs.PACKAGE_ROOT_PREFIX)) continue;
            try {
                roots.put(rootDir.getCanonicalPath(), rootDir);
            } catch (Exception ignored) {
                roots.put(rootDir.getAbsolutePath(), rootDir);
            }
        }
    }

    private List<File> getRuntimeOptDirs() {
        LinkedHashMap<String, File> dirs = new LinkedHashMap<>();
        addRuntimeOptDir(dirs, ImageFs.find(context).getOptDir());
        for (File rootDir : ImageFs.getKnownRootDirs(context)) {
            addRuntimeOptDir(dirs, new File(rootDir, "opt"));
        }
        addRuntimeOptDir(dirs, new File(ImageFs.getRuntimeRootDir(context, ContentProfile.RUNTIME_MODEL_BIONIC), "opt"));
        addRuntimeOptDir(dirs, new File(ImageFs.getRuntimeRootDir(context, ContentProfile.RUNTIME_MODEL_GLIBC), "opt"));
        return new ArrayList<>(dirs.values());
    }

    private void addRuntimeOptDir(LinkedHashMap<String, File> dirs, File dir) {
        if (dirs == null || dir == null) return;
        try {
            dirs.put(dir.getCanonicalPath(), dir);
        } catch (Exception ignored) {
            dirs.put(dir.getAbsolutePath(), dir);
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
        File installDir = resolveInstalledInstallDir(profile, false);
        if (installDir == null) installDir = getInstallDir(context, profile);
        return resolveWineRuntimeRoot(installDir, profile);
    }

    public InstalledProfileState resolveInstalledProfileState(@Nullable ContentProfile profile) {
        if (profile == null) {
            return new InstalledProfileState(false, false, "missing_profile");
        }
        String cacheKey = buildInstalledProfileStateKey(profile);
        InstalledProfileState cached = installedProfileStateByKey.get(cacheKey);
        if (cached != null) return cached;
        InstalledProfileState resolved = resolveInstalledProfileStateUncached(profile);
        installedProfileStateByKey.put(cacheKey, resolved);
        return resolved;
    }

    private InstalledProfileState resolveInstalledProfileStateUncached(@NonNull ContentProfile profile) {
        File canonicalInstallDir = getInstallDir(context, profile);
        File installDir = resolveInstalledInstallDir(profile, true);
        if (installDir == null && canonicalInstallDir.isDirectory()) {
            installDir = canonicalInstallDir;
        }

        boolean present = installDir != null && installDir.isDirectory();
        if (!present) {
            return profile.isInstalledLocally()
                    ? new InstalledProfileState(true, false, "missing_install_dir")
                    : new InstalledProfileState(false, false, "not_installed");
        }
        if (!profile.isWineProtonFamily()) {
            repairPayloadProfileForRoot(installDir, profile, "installed_state");
            File profileJson = new File(installDir, PROFILE_NAME);
            if (!profileJson.isFile() && hasUsablePayloadProfile(installDir, profile)) {
                boolean written = writeProfileSnapshot(installDir, profile);
                logPayloadProfileRepair(
                        installDir,
                        profile,
                        "installed_state_missing_profile_json",
                        written ? "profile_snapshot_written" : "profile_snapshot_write_failed",
                        profile.fileList != null ? profile.fileList.size() : 0,
                        profile.fileList != null ? profile.fileList.size() : 0
                );
            }
            if (!hasUsablePayloadProfile(installDir, profile)) {
                return new InstalledProfileState(true, false, "missing_payload_files");
            }
            return new InstalledProfileState(true, true, "");
        }

        File profileJson = new File(installDir, PROFILE_NAME);
        if (!profileJson.isFile()) {
            repairWineFamilyProfile(installDir, profile, profile, profile.artifactName);
            if (hasResolvedRuntimePayload(installDir, profile)) {
                boolean written = writeProfileSnapshot(installDir, profile);
                logPayloadProfileRepair(
                        installDir,
                        profile,
                        "installed_runtime_missing_profile_json",
                        written ? "runtime_profile_snapshot_written" : "runtime_profile_snapshot_write_failed",
                        0,
                        profile.fileList != null ? profile.fileList.size() : 0
                );
            }
            if (!profileJson.isFile()) {
                return new InstalledProfileState(true, false, "missing_profile_json");
            }
        }

        File runtimeRoot = resolveWineRuntimeRoot(installDir, profile);
        if (runtimeRoot == null || !runtimeRoot.isDirectory()) {
            return new InstalledProfileState(true, false, "missing_runtime_root");
        }
        if (!WineUtils.hasRuntimeCorePayload(runtimeRoot)) {
            return new InstalledProfileState(true, false, "missing_runtime_payload");
        }

        return new InstalledProfileState(true, true, "");
    }

    @NonNull
    public InstalledProfileDiagnostics resolveInstalledProfileDiagnostics(@Nullable ContentProfile profile) {
        InstalledProfileState state = resolveInstalledProfileState(profile);
        if (profile == null) {
            return new InstalledProfileDiagnostics(
                    state,
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    false,
                    false,
                    false,
                    false
            );
        }

        File canonicalInstallDir = getInstallDir(context, profile);
        File resolvedInstallDir = resolveInstalledInstallDir(profile, true);
        if (resolvedInstallDir == null && canonicalInstallDir.isDirectory()) {
            resolvedInstallDir = canonicalInstallDir;
        }

        File installProbeRoot = resolvedInstallDir != null ? resolvedInstallDir : canonicalInstallDir;
        File profileJson = installProbeRoot != null ? new File(installProbeRoot, PROFILE_NAME) : null;
        File runtimeRoot = profile.isWineProtonFamily() ? resolveWineRuntimeRoot(installProbeRoot, profile) : null;
        boolean runtimeRootPresent = runtimeRoot != null && runtimeRoot.isDirectory();
        boolean runtimePayloadPresent = runtimeRootPresent && (!profile.isWineProtonFamily() || WineUtils.hasRuntimeCorePayload(runtimeRoot));
        boolean aliasResolved = resolvedInstallDir != null
                && canonicalInstallDir != null
                && !canonicalInstallDir.equals(resolvedInstallDir);

        return new InstalledProfileDiagnostics(
                state,
                sanitizeInstallToken(getEntryName(profile)),
                ContentProfileIdentity.describeProfile(profile),
                profile.type != null ? profile.type.toString() : "-",
                profile.getRuntimeModel(),
                normalizePath(canonicalInstallDir),
                normalizePath(resolvedInstallDir),
                normalizePath(runtimeRoot),
                normalizePath(profileJson),
                profileJson != null && profileJson.isFile(),
                runtimeRootPresent,
                runtimePayloadPresent,
                aliasResolved
        );
    }

    @NonNull
    public JSONObject buildInstalledProfileForensicFields(@Nullable InstalledProfileDiagnostics diagnostics) {
        InstalledProfileDiagnostics safe = diagnostics != null
                ? diagnostics
                : resolveInstalledProfileDiagnostics(null);
        return ForensicLogger.fields(
                "entry_name", safe.entryName,
                "requested_identity", safe.requestedIdentity,
                "type", safe.type,
                "runtime_model", safe.runtimeModel,
                "state_present", safe.state.present ? "1" : "0",
                "state_usable", safe.state.usable ? "1" : "0",
                "broken_reason", safe.state.brokenReason.isEmpty() ? "-" : safe.state.brokenReason,
                "expected_install_root", safe.canonicalInstallDir,
                "resolved_install_root", safe.resolvedInstallDir,
                "runtime_root", safe.runtimeRoot,
                "profile_json_path", safe.profileJsonPath,
                "profile_json_present", safe.profileJsonPresent ? "1" : "0",
                "runtime_root_present", safe.runtimeRootPresent ? "1" : "0",
                "runtime_payload_present", safe.runtimePayloadPresent ? "1" : "0",
                "alias_resolved", safe.aliasResolved ? "1" : "0"
        );
    }

    @NonNull
    public String buildInstalledProfileUiSummary(@Nullable InstalledProfileDiagnostics diagnostics) {
        if (diagnostics == null) return "";
        if (!diagnostics.state.isBroken() && !diagnostics.aliasResolved) return "";
        ArrayList<String> lines = new ArrayList<>();
        if (!diagnostics.entryName.equals("-")) lines.add("entry=" + diagnostics.entryName);
        if (!diagnostics.canonicalInstallDir.equals("-")) lines.add("expected=" + diagnostics.canonicalInstallDir);
        if (diagnostics.aliasResolved && !diagnostics.resolvedInstallDir.equals("-")) {
            lines.add("resolved=" + diagnostics.resolvedInstallDir);
        }
        if (!diagnostics.runtimeRoot.equals("-")) lines.add("runtime=" + diagnostics.runtimeRoot);
        return String.join("\n", lines);
    }

    @Nullable
    private File resolveInstalledInstallDir(@Nullable ContentProfile profile, boolean allowRepair) {
        if (profile == null) return null;
        if (!profile.isWineProtonFamily()) {
            File installDir = getInstallDir(context, profile);
            return installDir.isDirectory() ? installDir : null;
        }
        return resolveInstalledRuntimeInstallDir(profile, allowRepair, false);
    }

    @Nullable
    private File resolveInstalledRuntimeInstallDir(@Nullable ContentProfile requestedProfile,
                                                   boolean allowRepair,
                                                   boolean logResolution) {
        if (requestedProfile == null || !requestedProfile.isWineProtonFamily()) return null;
        InstalledRuntimeRoot matchedRoot = findEquivalentInstalledRuntimeRoot(requestedProfile, logResolution);
        if (matchedRoot == null) {
            return resolvePackageRuntimeLegacyInstallDir(requestedProfile, allowRepair, logResolution);
        }
        return resolveMatchedInstalledRuntimeInstallDir(requestedProfile, matchedRoot, allowRepair, logResolution);
    }

    @Nullable
    private File resolveMatchedInstalledRuntimeInstallDir(@Nullable ContentProfile requestedProfile,
                                                          @Nullable InstalledRuntimeRoot matchedRoot,
                                                          boolean allowRepair,
                                                          boolean logResolution) {
        if (requestedProfile == null || matchedRoot == null || matchedRoot.installRoot == null) return null;
        File resolvedRoot = matchedRoot.installRoot;
        if (!resolvedRoot.isDirectory()) return null;

        File canonicalRoot = getInstallDir(context, requestedProfile);
        if (canonicalRoot == null || canonicalRoot.equals(resolvedRoot)) {
            return resolvedRoot;
        }
        if (!allowRepair) {
            return resolvedRoot;
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            scheduleInstalledRuntimeOverlayRepair();
            if (logResolution) {
                logRuntimeInstallRootResolution(requestedProfile, matchedRoot.profile, resolvedRoot, canonicalRoot,
                        "scheduled_async_repair_keep_existing");
            }
            return resolvedRoot;
        }

        File migratedRoot = migrateRuntimeInstallRoot(resolvedRoot, canonicalRoot);
        if (migratedRoot != null && migratedRoot.isDirectory()) {
            ContentProfile persistedProfile = matchedRoot.profile != null ? matchedRoot.profile : requestedProfile;
            postProcessWineRuntimeInstall(migratedRoot, persistedProfile);
            persistProfileMetadata(new File(migratedRoot, PROFILE_NAME), persistedProfile);
            if (logResolution) {
                logRuntimeInstallRootResolution(requestedProfile, persistedProfile, resolvedRoot, canonicalRoot,
                        migratedRoot.equals(canonicalRoot) ? "migrated_to_canonical" : "migration_kept_existing_root");
            }
            return migratedRoot;
        }

        if (logResolution) {
            logRuntimeInstallRootResolution(requestedProfile, matchedRoot.profile, resolvedRoot, canonicalRoot,
                    "migration_failed_keep_existing");
        }
        return resolvedRoot;
    }

    @Nullable
    private File resolvePackageRuntimeLegacyInstallDir(@Nullable ContentProfile requestedProfile,
                                                       boolean allowRepair,
                                                       boolean logResolution) {
        if (requestedProfile == null || !requestedProfile.isWineProtonFamily()) return null;
        File packageOptDir = getRuntimeContentTypeDir(context, requestedProfile);
        if (packageOptDir == null || !packageOptDir.isDirectory()) return null;

        for (File legacyRoot : buildPackageRuntimeLegacyCandidates(packageOptDir, requestedProfile)) {
            if (legacyRoot == null || !legacyRoot.isDirectory()) continue;
            ContentProfile materializedProfile = requestedProfile;
            repairWineFamilyProfile(legacyRoot, materializedProfile, requestedProfile, requestedProfile.artifactName);
            if (!hasResolvedRuntimePayload(legacyRoot, materializedProfile)) continue;
            logPackageRuntimePayloadArchDrift(requestedProfile, legacyRoot);

            if (!allowRepair) {
                return legacyRoot;
            }

            writeProfileSnapshot(legacyRoot, materializedProfile);
            File canonicalRoot = getInstallDir(context, materializedProfile);
            File resolvedRoot = materializeCanonicalPackageRuntimeRoot(legacyRoot, canonicalRoot);
            File registeredRoot = resolvedRoot != null ? resolvedRoot : legacyRoot;
            registerInstalledRuntimeRoot(registeredRoot, materializedProfile);
            installedProfileStateByKey.remove(buildInstalledProfileStateKey(materializedProfile));

            if (logResolution) {
                logRuntimeInstallRootResolution(
                        requestedProfile,
                        materializedProfile,
                        legacyRoot,
                        canonicalRoot,
                        registeredRoot.equals(canonicalRoot)
                                ? "legacy_package_root_materialized_to_canonical"
                                : "legacy_package_root_kept_existing"
                );
            }
            return registeredRoot;
        }
        return null;
    }

    private ArrayList<File> buildPackageRuntimeLegacyCandidates(File packageOptDir, ContentProfile requestedProfile) {
        ArrayList<File> candidates = new ArrayList<>();
        if (packageOptDir == null || requestedProfile == null) return candidates;

        String arch = resolveRuntimeArchHint(requestedProfile);
        if (arch.isEmpty()) arch = requestedProfile.getArchitectureTag();
        addPackageRuntimeLegacyCandidate(candidates, packageOptDir, archToLegacyWineDir(arch));
        if ("arm64ec".equalsIgnoreCase(arch)) {
            addPackageRuntimeLegacyCandidate(candidates, packageOptDir, "arm64ec-wine");
        } else if ("x86_64".equalsIgnoreCase(arch)) {
            addPackageRuntimeLegacyCandidate(candidates, packageOptDir, "x86_64-wine");
        }
        addPackageRuntimeLegacyCandidate(candidates, packageOptDir, "wine");
        addPackageRuntimePayloadCandidates(candidates, packageOptDir);
        return candidates;
    }

    private void addPackageRuntimeLegacyCandidate(ArrayList<File> candidates, File packageOptDir, String childName) {
        if (candidates == null || packageOptDir == null || childName == null || childName.trim().isEmpty()) return;
        File candidate = new File(packageOptDir, childName.trim());
        for (File existing : candidates) {
            try {
                if (existing.getCanonicalPath().equals(candidate.getCanonicalPath())) return;
            } catch (Exception ignored) {
                if (existing.getAbsolutePath().equals(candidate.getAbsolutePath())) return;
            }
        }
        candidates.add(candidate);
    }

    private void addPackageRuntimePayloadCandidates(ArrayList<File> candidates, File packageOptDir) {
        if (candidates == null || packageOptDir == null || !packageOptDir.isDirectory()) return;
        File[] children = packageOptDir.listFiles();
        if (children == null) return;
        Arrays.sort(children, (left, right) -> {
            String leftName = left != null ? left.getName() : "";
            String rightName = right != null ? right.getName() : "";
            return leftName.compareToIgnoreCase(rightName);
        });
        for (File child : children) {
            if (!isRecoverablePackageRuntimePayloadRoot(child)) continue;
            addPackageRuntimeLegacyCandidate(candidates, packageOptDir, child.getName());
        }
    }

    private boolean isRecoverablePackageRuntimePayloadRoot(File candidate) {
        if (candidate == null || !candidate.isDirectory()) return false;
        String name = candidate.getName();
        if (name == null || !name.toLowerCase(Locale.US).contains("wine")) return false;
        File binDir = WineUtils.resolveRuntimeBinDir(candidate);
        File libDir = WineUtils.resolveRuntimeLibDir(candidate);
        return binDir != null
                && new File(binDir, "wine").isFile()
                && libDir != null
                && new File(libDir, "wine").isDirectory();
    }

    private String archToLegacyWineDir(String arch) {
        if ("arm64ec".equalsIgnoreCase(arch) || "arm64".equalsIgnoreCase(arch)) return "arm64ec-wine";
        if ("x86_64".equalsIgnoreCase(arch) || "amd64".equalsIgnoreCase(arch)) return "x86_64-wine";
        return "";
    }

    private void logPackageRuntimePayloadArchDrift(@Nullable ContentProfile requestedProfile, @Nullable File payloadRoot) {
        if (context == null || requestedProfile == null || payloadRoot == null) return;
        String requestedArch = normalizeRuntimeArchHint(resolveRuntimeArchHint(requestedProfile));
        if (requestedArch.isEmpty()) requestedArch = normalizeRuntimeArchHint(requestedProfile.getArchitectureTag());
        String payloadArch = inferPackageRuntimePayloadArch(payloadRoot);
        if (requestedArch.isEmpty() || payloadArch.isEmpty() || requestedArch.equals(payloadArch)) return;

        ForensicLogger.logEvent(
                context,
                "warn",
                "CONTENTS_RUNTIME_PACKAGE_ARCH_DRIFT",
                null,
                "contents",
                "package_runtime_payload_arch_drift_recovered",
                ForensicLogger.fields(
                        "requested_entry", getEntryName(requestedProfile),
                        "requested_runtime_model", requestedProfile.getRuntimeModel(),
                        "requested_arch", requestedArch,
                        "payload_arch", payloadArch,
                        "payload_root", payloadRoot.getAbsolutePath(),
                        "action", "using_existing_payload_for_requested_package_root"
                )
        );
    }

    private String inferPackageRuntimePayloadArch(File payloadRoot) {
        if (payloadRoot == null) return "";
        String nameArch = normalizeRuntimeArchHint(payloadRoot.getName());
        if (!nameArch.isEmpty()) return nameArch;

        File wineLibDir = WineUtils.resolveRuntimeWineLibDir(payloadRoot);
        File[] children = wineLibDir != null ? wineLibDir.listFiles() : null;
        if (children == null) return "";
        boolean hasAarch64 = false;
        boolean hasX86_64 = false;
        boolean hasX86 = false;
        for (File child : children) {
            if (child == null || !child.isDirectory()) continue;
            String name = child.getName().toLowerCase(Locale.US);
            hasAarch64 |= name.contains("aarch64") || name.contains("arm64");
            hasX86_64 |= name.contains("x86_64") || name.contains("amd64");
            hasX86 |= !hasX86_64 && (name.contains("i386") || name.contains("i686") || name.equals("x86-unix") || name.equals("x86-windows"));
        }
        if (hasAarch64) return "arm64ec";
        if (hasX86_64) return "x86_64";
        if (hasX86) return "x86";
        return "";
    }

    private String normalizeRuntimeArchHint(@Nullable String arch) {
        String lower = arch == null ? "" : arch.trim().toLowerCase(Locale.US);
        if (lower.equals("amd64") || lower.equals("x64") || lower.equals("x86-64")) return "x86_64";
        if (lower.equals("aarch64") || lower.equals("arm64-ec")) return "arm64ec";
        if (lower.contains("arm64ec") || lower.contains("arm64-ec")) return "arm64ec";
        if (lower.contains("x86_64") || lower.contains("x86-64") || lower.contains("amd64")) return "x86_64";
        if (lower.contains("aarch64") || lower.contains("arm64")) return "arm64";
        if (lower.contains("x86")) return "x86";
        return lower;
    }

    @Nullable
    private File materializeCanonicalPackageRuntimeRoot(File legacyRoot, File canonicalRoot) {
        if (legacyRoot == null || canonicalRoot == null || !legacyRoot.isDirectory()) return null;
        try {
            if (legacyRoot.getCanonicalPath().equals(canonicalRoot.getCanonicalPath())) return legacyRoot;
        } catch (Exception ignored) {
            if (legacyRoot.equals(canonicalRoot)) return legacyRoot;
        }

        if ((canonicalRoot.exists() || FileUtils.isSymlink(canonicalRoot))
                && !WineUtils.hasRuntimeCorePayload(canonicalRoot)) {
            FileUtils.delete(canonicalRoot);
        }
        File parent = canonicalRoot.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        if (!canonicalRoot.exists()) {
            FileUtils.symlink(legacyRoot, canonicalRoot);
        }
        if (canonicalRoot.isDirectory() && WineUtils.hasRuntimeCorePayload(canonicalRoot)) {
            return canonicalRoot;
        }
        return legacyRoot;
    }

    public boolean isInstalledProfilePresent(@Nullable ContentProfile profile) {
        return resolveInstalledProfileState(profile).present;
    }

    public boolean isInstalledProfileUsable(@Nullable ContentProfile profile) {
        return resolveInstalledProfileState(profile).usable;
    }

    private boolean hasInstalledRuntimeProfilePayload(@Nullable ContentProfile profile) {
        return profile != null
                && profile.isWineProtonFamily()
                && resolveInstalledProfileState(profile).usable;
    }

    private boolean matchesInstalledRequirement(@Nullable ContentProfile profile, boolean requireUsable) {
        if (requireUsable) {
            return isInstalledProfileUsable(profile);
        }
        return isInstalledProfilePresent(profile);
    }

    private boolean hasResolvedRuntimePayload(@Nullable File installPath, @Nullable ContentProfile profile) {
        if (installPath == null || profile == null || !profile.isWineProtonFamily()) return false;
        File runtimeRoot = resolveWineRuntimeRoot(installPath, profile);
        return runtimeRoot != null && runtimeRoot.isDirectory() && WineUtils.hasRuntimeCorePayload(runtimeRoot);
    }

    private File resolveWineRuntimeRoot(File installPath, @Nullable ContentProfile profile) {
        if (installPath == null || profile == null || !profile.isWineProtonFamily()) {
            return installPath;
        }

        File canonicalInstallRoot = WineUtils.resolveCanonicalRuntimeRoot(installPath);
        if (canonicalInstallRoot != null && WineUtils.hasRuntimeCorePayload(canonicalInstallRoot)) {
            return canonicalInstallRoot;
        }

        String commonRoot = resolveSharedRuntimeRoot(profile);
        if (commonRoot.isEmpty()) {
            return canonicalInstallRoot != null ? canonicalInstallRoot : installPath;
        }

        File candidate = WineUtils.resolveCanonicalRuntimeRoot(new File(installPath, commonRoot));
        if (candidate != null && candidate.isDirectory() && WineUtils.hasRuntimeCorePayload(candidate)) {
            return candidate;
        }
        return canonicalInstallRoot != null ? canonicalInstallRoot : installPath;
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
        rebindWineFamilyProfilePaths(installPath, profile);
        sanitizeWineRuntimeRunpath(installPath, profile);
        sanitizeWineRuntimeElfInterpreters(installPath, profile);
        relocateWineRuntimeImageFsPaths(installPath, profile);

        File binDir = WineUtils.resolveRuntimeBinDir(installPath);
        if (binDir != null && binDir.isDirectory()) {
            setExecutablePermissionsRecursive(binDir);
        }
    }

    private void rebindWineFamilyProfilePaths(File installPath, ContentProfile profile) {
        if (installPath == null || profile == null || !profile.isWineProtonFamily()) return;

        File binDir = WineUtils.resolveRuntimeBinDir(installPath);
        File libDir = WineUtils.resolveRuntimeLibDir(installPath);
        File prefixPack = WineUtils.resolveRuntimePrefixPack(installPath);
        if (binDir == null || libDir == null || prefixPack == null) return;

        profile.wineBinPath = relativizePath(installPath, binDir);
        profile.wineLibPath = relativizePath(installPath, libDir);
        profile.winePrefixPack = relativizePath(installPath, prefixPack);
        classifyRuntimeProfileFromPayload(installPath, profile);
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
            if (isDgVoodooPackageLocalTarget(profile, contentFile.target)) {
                continue;
            }
            String normalizedTarget = Paths.get(getPathFromTemplate(contentFile.target)).toAbsolutePath().normalize().toString();
            if (!trustedFilesMap.get(profile.type).contains(normalizedTarget)) {
                files.add(contentFile);
            }
        }
        return files;
    }

    private boolean isSubPath(String parent, String child) {
        return Paths.get(child).toAbsolutePath().normalize().startsWith(Paths.get(parent).toAbsolutePath().normalize());
    }

    private boolean isTrustedInstallTarget(ContentProfile profile, String target, String imagefsPath) {
        if (isDgVoodooPackageLocalTarget(profile, target)) return true;
        String realPath = getPathFromTemplate(target);
        return isSubPath(imagefsPath, realPath)
                && !isSubPath(ContentsManager.getContentDir(context).getAbsolutePath(), realPath)
                && !realPath.contains("dosdevices");
    }

    private boolean isDgVoodooPackageLocalTarget(ContentProfile profile, String target) {
        if (profile == null || profile.type != ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) return false;
        String normalized = normalizePackageLocalTarget(target);
        if (normalized.isEmpty()) return false;

        for (String rootFile : DGVOODOO_PACKAGE_ROOT_FILES) {
            if (rootFile.equalsIgnoreCase(normalized)) return true;
        }

        String[] parts = normalized.split("/");
        if (parts.length != 4) return false;
        if (!"payload".equals(parts[0]) || !"runtime".equals(parts[1])) return false;
        if (!containsToken(DGVOODOO_PACKAGE_RUNTIME_ARCHES, parts[2])) return false;
        return containsToken(DGVOODOO_PACKAGE_RUNTIME_FILES, parts[3]);
    }

    private String normalizePackageLocalTarget(String target) {
        if (target == null) return "";
        String normalized = target.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        if (normalized.isEmpty() || normalized.startsWith("/") || normalized.contains(":")) return "";
        String[] parts = normalized.split("/");
        ArrayList<String> safeParts = new ArrayList<>();
        for (String part : parts) {
            if (part == null || part.isEmpty() || ".".equals(part) || "..".equals(part)) return "";
            safeParts.add(part);
        }
        return String.join("/", safeParts);
    }

    private boolean containsToken(String[] values, String candidate) {
        if (values == null || candidate == null) return false;
        for (String value : values) {
            if (value.equalsIgnoreCase(candidate)) return true;
        }
        return false;
    }

    private void createDirTemplateMap() {
        dirTemplateMap = new HashMap<>();
        ImageFs imageFs = ImageFs.find(context);
        File rootDir = imageFs.getRootDir();
        String imagefsPath = rootDir.getAbsolutePath();
        File driveCRoot = WineUtils.resolveHostWineDriveCRoot(rootDir);
        if (!driveCRoot.isDirectory()) {
            driveCRoot = new File(imageFs.getWinePrefixDir(), "drive_c");
        }
        String drivecPath = driveCRoot.getAbsolutePath();
        dirTemplateMap.put("${libdir}", imagefsPath + "/usr/lib");
        dirTemplateMap.put("${system32}", drivecPath + "/windows/system32");
        dirTemplateMap.put("${syswow64}", drivecPath + "/windows/syswow64");
        dirTemplateMap.put("${localbin}", imagefsPath + "/usr/local/bin");
        dirTemplateMap.put("${bindir}", imagefsPath + "/usr/bin");
        dirTemplateMap.put("${sharedir}", imagefsPath + "/usr/share");
    }

    private void createTrustedFilesMap() {
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

    private String getPathFromTemplate(String path) {
        createDirTemplateMap();
        String realPath = path == null ? "" : path;
        for (String key : dirTemplateMap.keySet()) {
            realPath = realPath.replace(key, dirTemplateMap.get(key));
        }
        return realPath;
    }

    public void removeContent(ContentProfile profile) {
        if (profilesMap.get(profile.type).contains(profile)) {
            File installDir = resolveInstalledInstallDir(profile, false);
            File canonicalInstallDir = getInstallDir(context, profile);
            if (installDir != null && installDir.exists()) {
                FileUtils.delete(installDir);
            }
            if (canonicalInstallDir != null
                    && !canonicalInstallDir.equals(installDir)
                    && canonicalInstallDir.exists()) {
                FileUtils.delete(canonicalInstallDir);
            }
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
            if (direct != null && isInstalledProfileUsable(direct) && direct.isRuntimeModelCompatible(requestedRuntimeModel)) {
                return direct;
            }
            return null;
        }
        requested = requested.withRuntimeModel(resolveRequestedRuntimeModel(requestedRuntimeModel, requested));
        ContentProfile requestedProfile = requested.toProfile();

        ContentProfile protonBest = null;
        ContentProfile wineBest = null;
        ContentProfile protonCompatibleBest = null;
        ContentProfile wineCompatibleBest = null;
        for (ContentProfile.ContentType type : new ContentProfile.ContentType[] {
                ContentProfile.ContentType.CONTENT_TYPE_PROTON,
                ContentProfile.ContentType.CONTENT_TYPE_WINE
        }) {
            List<ContentProfile> profiles = profilesMap != null ? profilesMap.get(type) : null;
            if (profiles == null) continue;
            for (ContentProfile profile : profiles) {
                if (!hasInstalledRuntimeProfilePayload(profile)) continue;
                boolean exactVersion = profile.verName != null && requested.versionName.equalsIgnoreCase(profile.verName);
                boolean compatiblePayload = ContentProfileIdentity.areRuntimePayloadCompatibleProfiles(profile, requestedProfile);
                if (!exactVersion && !compatiblePayload) continue;
                boolean strictRuntimeModel = profile.isRuntimeModelCompatible(requested.runtimeModel);
                if (!strictRuntimeModel && !compatiblePayload) continue;
                if (strictRuntimeModel && profile.isProtonLike()) {
                    protonBest = pickBetterRuntimeCandidate(protonBest, profile, requested);
                } else if (strictRuntimeModel) {
                    wineBest = pickBetterRuntimeCandidate(wineBest, profile, requested);
                } else if (profile.isProtonLike()) {
                    protonCompatibleBest = pickBetterRuntimeCandidate(protonCompatibleBest, profile, requested);
                } else {
                    wineCompatibleBest = pickBetterRuntimeCandidate(wineCompatibleBest, profile, requested);
                }
            }
        }

        if (requested.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
            if (protonBest != null) return protonBest;
            if (protonCompatibleBest != null) return protonCompatibleBest;
        } else {
            if (wineBest != null) return wineBest;
            if (wineCompatibleBest != null) return wineCompatibleBest;
        }

        ContentProfile exact = getProfileByEntryName(entryName);
        if (exact != null
                && hasInstalledRuntimeProfilePayload(exact)
                && (exact.isRuntimeModelCompatible(requested.runtimeModel)
                || ContentProfileIdentity.areRuntimePayloadCompatibleProfiles(exact, requestedProfile))) {
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
            ContentProfile requestedProfile = runtimeEntry.toProfile();
            ContentProfile compatibleFallback = null;

            for (ContentProfile profile : profiles) {
                if (profile == null) continue;
                boolean exactEntry = profile.verCode == runtimeEntry.versionCode
                        && profile.verName != null
                        && runtimeEntry.versionName.equalsIgnoreCase(profile.verName);
                if (exactEntry && profile.isRuntimeModelCompatible(runtimeEntry.runtimeModel)) {
                    return profile;
                }
                if (ContentProfileIdentity.areRuntimePayloadCompatibleProfiles(profile, requestedProfile)) {
                    compatibleFallback = pickBetterRuntimeCandidate(compatibleFallback, profile, runtimeEntry);
                }
            }
            return compatibleFallback;
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
        if (direct != null && direct.type == type && (!installedOnly || matchesInstalledRequirement(direct, true))) {
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
            if (installedOnly && !matchesInstalledRequirement(profile, true)) continue;
            if (profile.verName == null || !normalizedVersion.equalsIgnoreCase(profile.verName)) continue;
            best = pickPreferredVersionCandidate(best, profile);
        }
        return best;
    }

    @Nullable
    public ContentProfile findInstalledProfileByVersion(ContentProfile.ContentType type, String versionName, boolean requireUsable) {
        if (type == null || versionName == null) return null;
        String normalizedVersion = versionName.trim();
        if (normalizedVersion.isEmpty()) return null;

        String typePrefix = type.toString() + "-";
        if (normalizedVersion.regionMatches(true, 0, typePrefix, 0, typePrefix.length())) {
            normalizedVersion = normalizedVersion.substring(typePrefix.length()).trim();
        }

        List<ContentProfile> profiles = profilesMap != null ? profilesMap.get(type) : null;
        if (profiles == null) return null;

        ContentProfile best = null;
        for (ContentProfile profile : profiles) {
            if (profile == null) continue;
            if (!matchesInstalledRequirement(profile, requireUsable)) continue;
            if (!matchesInstalledVersion(profile, normalizedVersion)) continue;
            best = pickPreferredVersionCandidate(best, profile);
        }
        return best;
    }

    public boolean hasInstalledVersion(ContentProfile.ContentType type, String versionName) {
        return hasInstalledVersion(type, versionName, true);
    }

    public boolean hasInstalledVersion(ContentProfile.ContentType type, String versionName, boolean requireUsable) {
        return findInstalledProfileByVersion(type, versionName, requireUsable) != null;
    }

    @Nullable
    public ContentProfile findBestInstalledProfile(ContentProfile.ContentType type,
                                                   @Nullable String requestedRuntimeModel,
                                                   @Nullable String requestedArch,
                                                   boolean requireUsable) {
        if (type == null) return null;
        List<ContentProfile> profiles = profilesMap != null ? profilesMap.get(type) : null;
        if (profiles == null) return null;
        String normalizedRuntimeModel = ContentProfile.normalizeRuntimeModel(requestedRuntimeModel);
        String normalizedArch = requestedArch == null ? "" : requestedArch.trim().toLowerCase(Locale.US);

        ContentProfile bestStrict = null;
        ContentProfile bestCompatible = null;
        for (ContentProfile profile : profiles) {
            if (profile == null) continue;
            if (!matchesInstalledRequirement(profile, requireUsable)) continue;
            boolean runtimeCompatible = normalizedRuntimeModel.isEmpty()
                    || profile.isRuntimeModelCompatible(normalizedRuntimeModel);
            boolean archCompatible = normalizedArch.isEmpty() || profile.matchesArchitectureFilter(normalizedArch);
            if (runtimeCompatible && archCompatible) {
                bestStrict = pickPreferredVersionCandidate(bestStrict, profile);
            } else if (runtimeCompatible || archCompatible) {
                bestCompatible = pickPreferredVersionCandidate(bestCompatible, profile);
            }
        }
        return bestStrict != null ? bestStrict : bestCompatible;
    }

    public int countInstalledProfiles(ContentProfile.ContentType type, boolean requireUsable) {
        List<ContentProfile> profiles = profilesMap != null ? profilesMap.get(type) : null;
        if (profiles == null) return 0;

        int count = 0;
        for (ContentProfile profile : profiles) {
            if (profile == null) continue;
            if (!matchesInstalledRequirement(profile, requireUsable)) continue;
            count++;
        }
        return count;
    }

    public List<String> getInstalledVersionNames(ContentProfile.ContentType type, boolean requireUsable) {
        LinkedHashSet<String> versions = new LinkedHashSet<>();
        List<ContentProfile> profiles = profilesMap != null ? profilesMap.get(type) : null;
        if (profiles == null) return new ArrayList<>();

        for (ContentProfile profile : profiles) {
            if (profile == null) continue;
            if (!matchesInstalledRequirement(profile, requireUsable)) continue;
            String version = profile.verName == null ? "" : profile.verName.trim();
            if (!version.isEmpty()) versions.add(version);
        }
        return new ArrayList<>(versions);
    }

    public List<String> getInstalledRuntimeEntries(@Nullable String requestedRuntimeModel,
                                                   boolean requireUsable,
                                                   ContentProfile.ContentType... types) {
        LinkedHashSet<String> entries = new LinkedHashSet<>();
        if (types == null || types.length == 0) return new ArrayList<>();

        String normalizedRuntimeModel = ContentProfile.normalizeRuntimeModel(requestedRuntimeModel);
        for (ContentProfile.ContentType type : types) {
            List<ContentProfile> profiles = profilesMap != null ? profilesMap.get(type) : null;
            if (profiles == null) continue;

            for (ContentProfile profile : profiles) {
                if (profile == null || !profile.isWineProtonFamily()) continue;
                if (!matchesInstalledRequirement(profile, requireUsable)) continue;
                if (!normalizedRuntimeModel.isEmpty() && !profile.isRuntimeModelCompatible(normalizedRuntimeModel)) {
                    continue;
                }

                String runtimeEntry = getEntryName(profile);
                if (runtimeEntry != null && !runtimeEntry.trim().isEmpty()) {
                    entries.add(runtimeEntry);
                }
            }
        }
        return new ArrayList<>(entries);
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

    private ContentProfile pickPreferredRemoteInstallCandidate(ContentProfile currentBest, ContentProfile candidate) {
        if (candidate == null) return currentBest;
        if (currentBest == null) return candidate;

        int publishedCompare = comparePublishedAt(candidate.publishedAt, currentBest.publishedAt);
        if (publishedCompare > 0) return candidate;
        if (publishedCompare < 0) return currentBest;

        if (candidate.verCode > currentBest.verCode) return candidate;
        if (candidate.remoteSha256 != null && !candidate.remoteSha256.trim().isEmpty()
                && (currentBest.remoteSha256 == null || currentBest.remoteSha256.trim().isEmpty())) {
            return candidate;
        }
        return currentBest;
    }

    private boolean matchesInstallableRemoteSemanticIdentity(@Nullable ContentProfile remote,
                                                            @Nullable ContentProfile requested) {
        if (remote == null || requested == null || remote.type == null || requested.type == null) return false;
        if (remote.type != requested.type) return false;

        String remoteVersion = normalizeInstallIdentityToken(remote.verName);
        String requestedVersion = normalizeInstallIdentityToken(requested.verName);
        if (remoteVersion.isEmpty() || requestedVersion.isEmpty()) return false;
        if (!remoteVersion.equals(requestedVersion)) return false;

        String remoteArch = remote.getArchitectureTag();
        String requestedArch = requested.getArchitectureTag();
        boolean archMatches = remoteArch == null || requestedArch == null
                || remoteArch.isEmpty()
                || requestedArch.isEmpty()
                || "generic".equalsIgnoreCase(remoteArch)
                || "generic".equalsIgnoreCase(requestedArch)
                || "bundle".equalsIgnoreCase(remoteArch)
                || "bundle".equalsIgnoreCase(requestedArch)
                || remoteArch.equalsIgnoreCase(requestedArch);
        if (!archMatches) return false;

        return remote.getChannel().equalsIgnoreCase(requested.getChannel())
                || remote.isBetaLike() == requested.isBetaLike();
    }

    private String normalizeInstallIdentityToken(@Nullable String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.US);
        normalized = normalized.replaceAll("(?i)\\.(wcp\\.xz|wcp\\.zst|wcp|zip|tar\\.xz|tar\\.zst|tar|txz|tzst)$", "");
        normalized = normalized.replaceAll("[^a-z0-9._-]+", "-");
        normalized = normalized.replaceAll("-{2,}", "-");
        while (normalized.startsWith("-")) normalized = normalized.substring(1);
        while (normalized.endsWith("-")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    private boolean matchesInstalledVersion(@Nullable ContentProfile profile, String versionName) {
        if (profile == null || versionName == null || versionName.trim().isEmpty()) return false;

        String normalizedVersion = versionName.trim().toLowerCase(Locale.US);
        String normalizedVersionId = StringUtils.parseIdentifier(normalizedVersion);
        String profileVersion = profile.verName == null ? "" : profile.verName.trim().toLowerCase(Locale.US);
        if (profileVersion.isEmpty()) return false;

        if (profileVersion.equals(normalizedVersion)) return true;

        String profileVersionId = StringUtils.parseIdentifier(profileVersion);
        if (profileVersionId.equals(normalizedVersionId)) return true;

        if (profileVersion.startsWith(normalizedVersion + "-")
                || normalizedVersion.startsWith(profileVersion + "-")) {
            return true;
        }

        return profileVersionId.startsWith(normalizedVersionId + "-")
                || normalizedVersionId.startsWith(profileVersionId + "-");
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
        if (profile == null || profile.type == null) return false;
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_WINE
                || profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
            return true;
        }
        File installDir = resolveInstalledInstallDir(profile, false);
        if (installDir == null) installDir = getInstallDir(context, profile);

        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) {
            boolean ready = installDir != null && installDir.isDirectory() && hasUsablePayloadProfile(installDir, profile);
            ForensicLogger.logEvent(
                    context,
                    ready ? "info" : "warn",
                    "CONTENTS_PAYLOAD_APPLY_RESULT",
                    null,
                    "contents",
                    ready ? "payload_apply_package_managed" : "payload_apply_package_missing",
                    ForensicLogger.fields(
                            "type", profile.type.toString(),
                            "ver_name", profile.verName == null ? "" : profile.verName,
                            "ver_code", profile.verCode,
                            "install_root", normalizePath(installDir)
                    )
            );
            return ready;
        }

        repairPayloadProfileForRoot(installDir, profile, "apply");

        if (profile.fileList == null || profile.fileList.isEmpty()) {
            ForensicLogger.logEvent(
                    context,
                    "warn",
                    "CONTENTS_PAYLOAD_APPLY_RESULT",
                    null,
                    "contents",
                    "payload_apply_empty_file_list",
                    ForensicLogger.fields(
                            "type", profile.type != null ? profile.type.toString() : "-",
                            "ver_name", profile.verName == null ? "" : profile.verName,
                            "ver_code", profile.verCode,
                            "install_root", normalizePath(installDir)
                    )
            );
            return false;
        }

        String installRootPath = installDir != null ? installDir.getAbsolutePath() : "";
        String imagefsPath = ImageFs.find(context).getRootDir().getAbsolutePath();
        int copiedCount = 0;
        int missingSourceCount = 0;
        int failedCount = 0;
        int guardFailedCount = 0;
        ArrayList<String> samples = new ArrayList<>();
        for (ContentProfile.ContentFile contentFile : profile.fileList) {
            if (contentFile == null || isBlank(contentFile.source) || isBlank(contentFile.target)) {
                guardFailedCount++;
                addPayloadApplySample(samples, "blank_mapping");
                continue;
            }

            String normalizedSource = normalizeRelativePath(contentFile.source);
            File sourceFile = new File(installDir, normalizedSource);
            File targetFile = new File(getPathFromTemplate(contentFile.target));
            if (installRootPath.isEmpty()
                    || !isSubPath(installRootPath, sourceFile.getAbsolutePath())
                    || !isTrustedInstallTarget(profile, contentFile.target, imagefsPath)) {
                guardFailedCount++;
                addPayloadApplySample(samples, "guard_failed:" + normalizedSource + "->" + contentFile.target);
                continue;
            }

            boolean copied = false;
            if (!sourceFile.isFile()) {
                missingSourceCount++;
                addPayloadApplySample(samples, "missing_source:" + normalizedSource);
            } else {
                targetFile.delete();
                copied = FileUtils.copy(sourceFile, targetFile)
                        && targetFile.isFile()
                        && targetFile.length() == sourceFile.length();
                if (!copied) {
                    failedCount++;
                    addPayloadApplySample(samples, "copy_failed:" + normalizedSource + "->" + contentFile.target);
                } else {
                    copiedCount++;
                }
            }

            if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_BOX64) {
                FileUtils.chmod(targetFile, 0771);
            }
        }
        boolean success = missingSourceCount == 0 && failedCount == 0 && guardFailedCount == 0;
        ForensicLogger.logEvent(
                context,
                success ? "info" : "warn",
                "CONTENTS_PAYLOAD_APPLY_RESULT",
                null,
                "contents",
                success ? "payload_apply_complete" : "payload_apply_incomplete",
                ForensicLogger.fields(
                        "type", profile.type != null ? profile.type.toString() : "-",
                        "ver_name", profile.verName == null ? "" : profile.verName,
                        "ver_code", profile.verCode,
                        "install_root", normalizePath(installDir),
                        "file_count", profile.fileList.size(),
                        "copied", copiedCount,
                        "missing_source", missingSourceCount,
                        "guard_failed", guardFailedCount,
                        "failed", failedCount,
                        "sample_count", samples.size(),
                        "samples", String.join(" | ", samples)
                )
        );
        return success;
    }

    private void addPayloadApplySample(ArrayList<String> samples, String sample) {
        if (samples == null || sample == null || sample.trim().isEmpty()) return;
        if (samples.size() < 12) samples.add(sample);
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
            if ("asset".equals(normalizedScheme)) {
                if (uri.getUserInfo() != null && !uri.getUserInfo().trim().isEmpty()) return false;
                String assetPath = ((uri.getHost() == null ? "" : uri.getHost()) + "/" + (uri.getPath() == null ? "" : uri.getPath()))
                        .replace('\\', '/')
                        .replaceFirst("^/*", "");
                return hasAllowedArchiveSuffix(assetPath);
            }
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

    private void logInstallFailure(@NonNull String eventId,
                                   @NonNull String stage,
                                   @NonNull InstallFailedReason reason,
                                   @Nullable ContentProfile profile,
                                   @Nullable ContentProfile remoteHint,
                                   @Nullable File tempRoot,
                                   @Nullable File installPath,
                                   @Nullable Exception error) {
        if (context == null) return;
        ContentProfile diagnosticProfile = profile != null ? profile : remoteHint;
        InstalledProfileDiagnostics diagnostics = resolveInstalledProfileDiagnostics(diagnosticProfile);
        JSONObject fields = buildInstalledProfileForensicFields(diagnostics);
        try {
            fields.put("reason", reason.name().toLowerCase(Locale.US));
            fields.put("temp_root", normalizePath(tempRoot));
            fields.put("temp_exists", tempRoot != null && tempRoot.exists() ? "1" : "0");
            fields.put("install_path", normalizePath(installPath));
            fields.put("install_exists", installPath != null && installPath.exists() ? "1" : "0");
            fields.put("remote_hint_identity", remoteHint != null ? ContentProfileIdentity.describeProfile(remoteHint) : "-");
            fields.put("remote_hint_entry", remoteHint != null ? sanitizeInstallToken(getEntryName(remoteHint)) : "-");
            fields.put("temp_root_shape", summarizeRootShape(tempRoot, 24));
            fields.put("runtime_payload_classifier", tempRoot != null
                    ? ImportedContentHeuristics.describeRuntimePayload(tempRoot, diagnosticProfile, remoteHint,
                    diagnosticProfile != null ? diagnosticProfile.artifactName : "")
                    : "-");
        } catch (JSONException ignored) {
        }
        ForensicLogger.error(
                context,
                eventId,
                null,
                stage,
                "content_install_failed",
                error,
                fields
        );
    }

    private void logContentArchiveExtraction(@NonNull String eventId,
                                             @Nullable String importDisplayName,
                                             @NonNull String archiveFormat,
                                             @Nullable File rootDir) {
        if (context == null) return;
        ForensicLogger.logEvent(
                context,
                "info",
                eventId,
                null,
                "contents_import",
                "archive_extracted",
                ForensicLogger.fields(
                        "file_name", importDisplayName == null ? "-" : importDisplayName,
                        "archive_format", archiveFormat,
                        "root_file_count", rootDir != null && rootDir.isDirectory() && rootDir.listFiles() != null ? rootDir.listFiles().length : -1,
                        "root_shape", summarizeRootShape(rootDir, 32)
                )
        );
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
                || type == ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER
                || type == ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER
                || type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO
                || type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D;
    }

    private boolean isUpdatableLane(ContentProfile.ContentType type) {
        return type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO
                || type == ContentProfile.ContentType.CONTENT_TYPE_DXVK
                || type == ContentProfile.ContentType.CONTENT_TYPE_VKD3D
                || type == ContentProfile.ContentType.CONTENT_TYPE_TURNIP_DRIVER
                || type == ContentProfile.ContentType.CONTENT_TYPE_OPENGL_DRIVER;
    }

    private ContentProfile findEquivalentProfile(List<ContentProfile> profiles, ContentProfile remote) {
        if (profiles == null || remote == null) return null;
        ContentProfile runtimePayloadCompatible = null;
        for (ContentProfile profile : profiles) {
            if (profile == null) continue;
            if (profile.sameEntry(remote)) return profile;
            if (ContentProfileIdentity.areEquivalentProfiles(profile, remote)) return profile;
            if (runtimePayloadCompatible == null
                    && ContentProfileIdentity.areRuntimePayloadCompatibleProfiles(profile, remote)) {
                runtimePayloadCompatible = profile;
            }
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
        if (runtimePayloadCompatible != null) return runtimePayloadCompatible;
        return null;
    }

    private void repairInstalledRuntimeOverlays() {
        repairPackageRuntimeRootProfiles(true);
        for (File sharedRuntimeDir : getRuntimeOptDirs()) {
            if (!sharedRuntimeDir.exists()) sharedRuntimeDir.mkdirs();

            migrateLegacyRuntimeDir(getLegacyRuntimeTypeDir(context, ContentProfile.ContentType.CONTENT_TYPE_WINE), sharedRuntimeDir);
            migrateLegacyRuntimeDir(getLegacyRuntimeTypeDir(context, ContentProfile.ContentType.CONTENT_TYPE_PROTON), sharedRuntimeDir);

            File[] installedRoots = sharedRuntimeDir.listFiles();
            if (installedRoots == null) continue;
            for (File installRoot : installedRoots) {
                if (installRoot == null || !installRoot.isDirectory()) continue;
                File profileFile = new File(installRoot, PROFILE_NAME);
                if (!profileFile.isFile()) continue;
                ContentProfile profile = normalizeImportedProfile(readProfile(profileFile), null);
                if (profile == null || !profile.isWineProtonFamily()) continue;
                classifyRuntimeProfileFromPayload(installRoot, profile);
                File normalizedRoot = migrateRuntimeInstallRoot(installRoot, getInstallDir(context, profile));
                postProcessWineRuntimeInstall(normalizedRoot, profile);
                persistProfileMetadata(new File(normalizedRoot, PROFILE_NAME), profile);
            }

            pruneSupersededWineRuntimeInstalls(sharedRuntimeDir);
        }
    }

    private void repairPackageRuntimeRootProfiles(boolean postProcessDirectPayloadRoots) {
        for (File rootDir : ImageFs.getKnownRootDirs(context)) {
            ContentProfile profile = synthesizePackageRuntimeRootProfile(rootDir);
            if (profile == null) continue;

            if (WineUtils.hasRuntimeCorePayload(rootDir)) {
                repairWineFamilyProfile(rootDir, profile, profile, profile.artifactName);
                if (hasResolvedRuntimePayload(rootDir, profile)) {
                    if (postProcessDirectPayloadRoots) {
                        postProcessWineRuntimeInstall(rootDir, profile);
                    }
                    writeProfileSnapshot(rootDir, profile);
                    registerInstalledRuntimeRoot(rootDir, profile);
                }
            }

            resolvePackageRuntimeLegacyInstallDir(profile, true, false);
        }
    }

    @Nullable
    private ContentProfile synthesizePackageRuntimeRootProfile(@Nullable File rootDir) {
        if (rootDir == null || !rootDir.isDirectory()) return null;
        String name = rootDir.getName();
        if (name == null || !name.startsWith(ImageFs.PACKAGE_ROOT_PREFIX)) return null;
        String payload = name.substring(ImageFs.PACKAGE_ROOT_PREFIX.length());
        int modelDash = payload.indexOf('-');
        if (modelDash <= 0 || modelDash >= payload.length() - 1) return null;

        String rootRuntimeModel = ContentProfile.normalizeRuntimeModel(payload.substring(0, modelDash));
        String entryName = payload.substring(modelDash + 1);
        RuntimeEntryParts parts = RuntimeEntryParts.parse(entryName);
        if (parts == null) return null;
        if (!rootRuntimeModel.isEmpty()
                && (parts.runtimeModel == null || parts.runtimeModel.isEmpty())) {
            parts = parts.withRuntimeModel(rootRuntimeModel);
        }

        ContentProfile profile = parts.toProfile();
        profile.channel = ContentProfile.CHANNEL_STABLE;
        profile.delivery = ContentProfile.DELIVERY_EMBEDDED;
        profile.displayCategory = profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON ? "Proton" : "Wine";
        profile.sourceLabel = "package-rootfs";
        profile.artifactName = name;
        profile.runtimeModel = profile.getRuntimeModel();
        profile.setInstalledLocally(true);
        return profile;
    }

    @Nullable
    private InstalledRuntimeRoot findEquivalentInstalledRuntimeRoot(@Nullable ContentProfile requestedProfile) {
        return findEquivalentInstalledRuntimeRoot(requestedProfile, true);
    }

    @Nullable
    private InstalledRuntimeRoot findEquivalentInstalledRuntimeRoot(@Nullable ContentProfile requestedProfile,
                                                                   boolean logResult) {
        if (requestedProfile == null || !requestedProfile.isWineProtonFamily()) return null;
        InstalledRuntimeRoot indexedRoot = findRegisteredInstalledRuntimeRoot(requestedProfile);
        if (indexedRoot != null) {
            if (logResult) {
                logEquivalentRuntimeLookup(requestedProfile, indexedRoot, installedRuntimeRootByKey.size(), 1);
            }
            return indexedRoot;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            scheduleInstalledRuntimeOverlayRepair();
            if (logResult) {
                logEquivalentRuntimeLookup(requestedProfile, null, installedRuntimeRootByKey.size(), 0);
            }
            return null;
        }
        List<File> installedRoots = getInstalledRootsForType(requestedProfile.type);
        if (installedRoots == null || installedRoots.isEmpty()) {
            if (logResult) {
                logEquivalentRuntimeLookup(requestedProfile, null, 0, 0);
            }
            return null;
        }

        InstalledRuntimeRoot bestStrict = null;
        InstalledRuntimeRoot bestCompatible = null;
        int scannedRoots = 0;
        int matchedRoots = 0;
        for (File installRoot : installedRoots) {
            if (installRoot == null || !installRoot.isDirectory()) continue;
            scannedRoots++;
            File profileFile = new File(installRoot, PROFILE_NAME);
            if (!profileFile.isFile()) continue;

            ContentProfile installedProfile = normalizeImportedProfile(readProfile(profileFile), requestedProfile);
            if (installedProfile == null || !installedProfile.isWineProtonFamily()) continue;
            classifyRuntimeProfileFromPayload(installRoot, installedProfile, requestedProfile, requestedProfile.artifactName);
            boolean strictMatch = ContentProfileIdentity.areEquivalentProfiles(installedProfile, requestedProfile);
            boolean compatiblePayload = strictMatch
                    || ContentProfileIdentity.areRuntimePayloadCompatibleProfiles(installedProfile, requestedProfile);
            if (!compatiblePayload) continue;

            matchedRoots++;
            InstalledRuntimeRoot candidate = new InstalledRuntimeRoot(installRoot, installedProfile);
            registerInstalledRuntimeRoot(installRoot, installedProfile);
            if (strictMatch) {
                if (shouldPreferInstalledRuntimeRoot(candidate, bestStrict)) {
                    bestStrict = candidate;
                }
            } else if (shouldPreferInstalledRuntimeRoot(candidate, bestCompatible)) {
                bestCompatible = candidate;
            }
        }
        InstalledRuntimeRoot best = bestStrict != null ? bestStrict : bestCompatible;
        if (logResult) {
            logEquivalentRuntimeLookup(requestedProfile, best, scannedRoots, matchedRoots);
        }
        return best;
    }

    private void registerInstalledRuntimeRoot(@Nullable File installRoot, @Nullable ContentProfile profile) {
        if (installRoot == null || profile == null || !profile.isWineProtonFamily()) return;
        InstalledRuntimeRoot root = new InstalledRuntimeRoot(installRoot, profile);
        for (String key : buildInstalledRuntimeLookupKeys(profile)) {
            putInstalledRuntimeRoot(key, root);
        }
        installedProfileStateByKey.remove(buildInstalledProfileStateKey(profile));
    }

    @Nullable
    private InstalledRuntimeRoot findRegisteredInstalledRuntimeRoot(@Nullable ContentProfile requestedProfile) {
        if (requestedProfile == null || !requestedProfile.isWineProtonFamily()) return null;
        for (String key : buildInstalledRuntimeLookupKeys(requestedProfile)) {
            InstalledRuntimeRoot direct = installedRuntimeRootByKey.get(key);
            if (direct != null) return direct;
        }

        InstalledRuntimeRoot bestStrict = null;
        InstalledRuntimeRoot bestCompatible = null;
        LinkedHashSet<InstalledRuntimeRoot> roots = new LinkedHashSet<>(installedRuntimeRootByKey.values());
        for (InstalledRuntimeRoot candidate : roots) {
            if (candidate == null || candidate.profile == null) continue;
            boolean strictMatch = ContentProfileIdentity.areEquivalentProfiles(candidate.profile, requestedProfile);
            boolean compatiblePayload = strictMatch
                    || ContentProfileIdentity.areRuntimePayloadCompatibleProfiles(candidate.profile, requestedProfile);
            if (!compatiblePayload) continue;
            if (strictMatch) {
                if (shouldPreferInstalledRuntimeRoot(candidate, bestStrict)) bestStrict = candidate;
            } else if (shouldPreferInstalledRuntimeRoot(candidate, bestCompatible)) {
                bestCompatible = candidate;
            }
        }
        return bestStrict != null ? bestStrict : bestCompatible;
    }

    private void putInstalledRuntimeRoot(String key, InstalledRuntimeRoot root) {
        if (key == null || key.isEmpty() || root == null) return;
        InstalledRuntimeRoot current = installedRuntimeRootByKey.get(key);
        if (shouldPreferInstalledRuntimeRoot(root, current)) {
            installedRuntimeRootByKey.put(key, root);
        }
    }

    private ArrayList<String> buildInstalledRuntimeLookupKeys(@Nullable ContentProfile profile) {
        ArrayList<String> keys = new ArrayList<>();
        if (profile == null || !profile.isWineProtonFamily()) return keys;
        addRuntimeLookupKey(keys, "entry", getEntryName(profile));
        addRuntimeLookupKey(keys, "identity", ContentProfileIdentity.describeProfile(profile));
        addRuntimeLookupKey(keys, "payload", buildInstalledRuntimePayloadKey(profile));
        return keys;
    }

    private void addRuntimeLookupKey(ArrayList<String> keys, String scope, String value) {
        if (keys == null || scope == null || value == null) return;
        String normalized = value.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) return;
        keys.add(scope + "|" + normalized);
    }

    private String buildInstalledRuntimePayloadKey(@Nullable ContentProfile profile) {
        if (profile == null) return "";
        String archHint = resolveRuntimeArchHint(profile);
        if (archHint.isEmpty()) archHint = resolveArchHint(profile);
        return (profile.isProtonLike() ? "proton" : "wine")
                + "|" + normalizeFamilyKeyToken(profile.getRuntimeModel())
                + "|" + normalizeFamilyKeyToken(archHint)
                + "|" + normalizeFamilyKeyToken(profile.verName)
                + "|" + normalizeFamilyKeyToken(profile.artifactName);
    }

    private String buildInstalledProfileStateKey(@Nullable ContentProfile profile) {
        if (profile == null) return "null";
        return ContentProfileIdentity.describeProfile(profile)
                + "|entry=" + getEntryName(profile)
                + "|local=" + (profile.isInstalledLocally() ? "1" : "0")
                + "|payload=" + buildInstalledRuntimePayloadKey(profile);
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
            classifyRuntimeProfileFromPayload(installRoot, profile);

            File targetRoot = migrateRuntimeInstallRoot(installRoot, getInstallDir(context, profile));
            postProcessWineRuntimeInstall(targetRoot, profile);
            persistProfileMetadata(new File(targetRoot, PROFILE_NAME), profile);
        }
    }

    private void sanitizeWineRuntimeRunpath(File installPath, ContentProfile profile) {
        if (installPath == null || profile == null || !profile.isWineProtonFamily()) return;
        ImageFs imageFs = resolvePackageRuntimeImageFs(profile);
        WineRuntimeRunpathSanitizer.Result result = WineRuntimeRunpathSanitizer.sanitizeTree(
                resolveWineRuntimeRoot(installPath, profile),
                imageFs.getLibDir()
        );
        if (result.hasSignal()) {
            Log.i("ContentsManager", "Wine runtime RUNPATH sanitize: " + result.toSummary());
        }
    }

    private void sanitizeWineRuntimeElfInterpreters(File installPath, ContentProfile profile) {
        if (installPath == null || profile == null || !profile.isWineProtonFamily()) return;
        ImageFs imageFs = resolvePackageRuntimeImageFs(profile);
        WineRuntimeElfInterpreterSanitizer.Result result =
                WineRuntimeElfInterpreterSanitizer.sanitizeWineRuntime(installPath, profile, imageFs);
        if (result.hasSignal()) {
            Log.i("ContentsManager", "Wine runtime ELF interpreter sanitize: " + result.toSummary());
            WineRuntimeElfInterpreterSanitizer.logResult(
                    context,
                    "CONTENTS_RUNTIME_ELF_INTERPRETER_REBIND",
                    result,
                    resolveWineRuntimeRoot(installPath, profile)
            );
        }
    }

    private void relocateWineRuntimeImageFsPaths(File installPath, ContentProfile profile) {
        if (installPath == null || profile == null || !profile.isWineProtonFamily()) return;
        ImageFs imageFs = resolvePackageRuntimeImageFs(profile);
        ImageFsPathRelocator.Result result =
                ImageFsPathRelocator.relocateWineRuntime(installPath, profile, imageFs);
        if (result.hasSignal()) {
            Log.i("ContentsManager", "Wine runtime imagefs path relocate: " + result.toSummary());
            ImageFsPathRelocator.logResult(
                    context,
                    "CONTENTS_RUNTIME_IMAGEFS_PATH_RELOCATE",
                    result,
                    resolveWineRuntimeRoot(installPath, profile)
            );
        }
    }

    private ImageFs resolvePackageRuntimeImageFs(ContentProfile profile) {
        if (profile != null && profile.isWineProtonFamily()) {
            File rootDir = ImageFs.getRuntimeRootDir(context, profile.getRuntimeModel(), getEntryName(profile));
            if (rootDir != null) return ImageFs.find(rootDir);
        }
        return ImageFs.find(context);
    }

    private void pruneSupersededWineRuntimeInstalls(File sharedRuntimeDir) {
        if (sharedRuntimeDir == null || !sharedRuntimeDir.isDirectory()) return;

        LinkedHashMap<String, InstalledRuntimeRoot> bestByFamily = new LinkedHashMap<>();
        ArrayList<InstalledRuntimeRoot> staleRoots = new ArrayList<>();
        File[] installedRoots = sharedRuntimeDir.listFiles();
        if (installedRoots == null) return;

        for (File installRoot : installedRoots) {
            if (installRoot == null || !installRoot.isDirectory()) continue;
            File profileFile = new File(installRoot, PROFILE_NAME);
            if (!profileFile.isFile()) continue;

            ContentProfile profile = normalizeImportedProfile(readProfile(profileFile), null);
            if (profile == null || !profile.isWineProtonFamily()) continue;

            InstalledRuntimeRoot candidate = new InstalledRuntimeRoot(installRoot, profile);
            String familyKey = buildInstalledRuntimeFamilyKey(profile);
            InstalledRuntimeRoot currentBest = bestByFamily.get(familyKey);
            if (shouldPreferInstalledRuntimeRoot(candidate, currentBest)) {
                if (currentBest != null) staleRoots.add(currentBest);
                bestByFamily.put(familyKey, candidate);
            } else if (currentBest != null) {
                staleRoots.add(candidate);
            }
        }

        for (InstalledRuntimeRoot stale : staleRoots) {
            if (stale == null || stale.installRoot == null || !stale.installRoot.exists()) continue;
            String installName = stale.installRoot.getName();
            if (FileUtils.delete(stale.installRoot)) {
                Log.i("ContentsManager", "Pruned superseded Wine runtime install: " + installName);
            } else {
                Log.w("ContentsManager", "Failed to prune superseded Wine runtime install: " + installName);
            }
        }
    }

    private void pruneSupersededDgVoodooInstalls(File typeDir, File currentInstallPath, ContentProfile installedProfile) {
        if (typeDir == null || currentInstallPath == null || installedProfile == null) return;
        File[] installedRoots = typeDir.listFiles();
        if (installedRoots == null) return;

        for (File installedRoot : installedRoots) {
            if (installedRoot == null || !installedRoot.isDirectory()) continue;
            if (installedRoot.equals(currentInstallPath)) continue;

            ContentProfile candidate = readProfile(new File(installedRoot, PROFILE_NAME));
            if (candidate == null || candidate.type != ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) continue;
            if (DgVoodooManager.compareVersionNames(installedProfile.verName, candidate.verName) > 0) {
                FileUtils.delete(installedRoot);
            }
        }
    }

    private String buildInstalledRuntimeFamilyKey(ContentProfile profile) {
        if (profile == null) return "";
        String archHint = resolveRuntimeArchHint(profile);
        if (archHint.isEmpty()) archHint = resolveArchHint(profile);
        return (profile.type == null ? "" : profile.type.toString().toLowerCase(Locale.US))
                + "|" + normalizeFamilyKeyToken(profile.getRuntimeModel())
                + "|" + normalizeFamilyKeyToken(profile.getChannel())
                + "|" + normalizeFamilyKeyToken(archHint)
                + "|" + normalizeFamilyKeyToken(profile.verName);
    }

    private boolean shouldPreferInstalledRuntimeRoot(@Nullable InstalledRuntimeRoot candidate,
                                                     @Nullable InstalledRuntimeRoot currentBest) {
        if (candidate == null) return false;
        if (currentBest == null) return true;

        ContentProfile candidateProfile = candidate.profile;
        ContentProfile currentProfile = currentBest.profile;
        if (candidateProfile.verCode != currentProfile.verCode) {
            return candidateProfile.verCode > currentProfile.verCode;
        }

        int candidateMetadataScore = computeInstalledRuntimeMetadataScore(candidateProfile);
        int currentMetadataScore = computeInstalledRuntimeMetadataScore(currentProfile);
        if (candidateMetadataScore != currentMetadataScore) {
            return candidateMetadataScore > currentMetadataScore;
        }

        boolean candidateCanonical = candidate.installRoot.equals(getInstallDir(context, candidateProfile));
        boolean currentCanonical = currentBest.installRoot.equals(getInstallDir(context, currentProfile));
        if (candidateCanonical != currentCanonical) {
            return candidateCanonical;
        }

        return candidate.installRoot.getName().compareToIgnoreCase(currentBest.installRoot.getName()) > 0;
    }

    private int computeInstalledRuntimeMetadataScore(ContentProfile profile) {
        if (profile == null) return 0;
        int score = 0;
        if (profile.remoteUrl != null && !profile.remoteUrl.trim().isEmpty()) score += 4;
        if (profile.releaseTag != null && !profile.releaseTag.trim().isEmpty()) score += 3;
        if (profile.artifactName != null && !profile.artifactName.trim().isEmpty()) score += 3;
        if (profile.sourceRepo != null && !profile.sourceRepo.trim().isEmpty()) score += 2;
        if (profile.sourceFeed != null && !profile.sourceFeed.trim().isEmpty()) score += 1;
        if (profile.sourceLabel != null && !profile.sourceLabel.trim().isEmpty()) score += 1;
        if (profile.publishedAt != null && !profile.publishedAt.trim().isEmpty()) score += 1;
        return score;
    }

    private String normalizeFamilyKeyToken(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
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

        private ContentProfile toProfile() {
            ContentProfile profile = new ContentProfile();
            profile.type = type;
            profile.verName = versionName;
            profile.verCode = versionCode;
            profile.runtimeModel = runtimeModel;
            profile.artifactName = versionName;
            return profile;
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

    private static final class InstalledRuntimeRoot {
        private final File installRoot;
        private final ContentProfile profile;

        private InstalledRuntimeRoot(File installRoot, ContentProfile profile) {
            this.installRoot = installRoot;
            this.profile = profile;
        }
    }

    @Nullable
    private ContentProfile synthesizeProfileFromExtractedPayload(File rootDir,
                                                                 @Nullable ContentProfile remoteHint,
                                                                 @Nullable String importDisplayName) {
        if (rootDir == null) return null;
        ContentProfile.ContentType resolvedType = ImportedContentHeuristics.inferContentType(rootDir, null, remoteHint, importDisplayName);
        if (resolvedType == null) return null;

        ContentProfile profile = new ContentProfile();
        profile.type = resolvedType;
        profile.verName = remoteHint != null ? remoteHint.verName : "";
        profile.verCode = remoteHint != null ? remoteHint.verCode : 0;
        profile.desc = remoteHint != null ? remoteHint.desc : "";
        profile.remoteUrl = remoteHint != null ? remoteHint.remoteUrl : "";
        profile.remoteSha256 = remoteHint != null ? remoteHint.remoteSha256 : "";
        profile.channel = remoteHint != null ? remoteHint.getChannel() : ContentProfile.CHANNEL_STABLE;
        profile.delivery = remoteHint != null && !remoteHint.getDelivery().isEmpty()
                ? remoteHint.getDelivery()
                : ContentProfile.DELIVERY_REMOTE;
        profile.displayCategory = remoteHint != null ? remoteHint.getDisplayCategory() : "";
        profile.sourceRepo = remoteHint != null ? remoteHint.sourceRepo : "";
        profile.sourceFeed = remoteHint != null ? remoteHint.sourceFeed : "";
        profile.sourceLabel = remoteHint != null ? remoteHint.sourceLabel : "";
        profile.releaseTag = remoteHint != null ? remoteHint.releaseTag : "";
        profile.artifactName = remoteHint != null ? remoteHint.artifactName : importDisplayName;
        profile.publishedAt = remoteHint != null ? remoteHint.publishedAt : "";
        profile.releaseNotes = remoteHint != null ? remoteHint.releaseNotes : "";
        profile.runtimeModel = ImportedContentHeuristics.inferRuntimeModel(rootDir, profile, remoteHint, importDisplayName);
        profile.vulkanApiMin = remoteHint != null ? remoteHint.vulkanApiMin : 0;
        profile.vulkanApiMax = remoteHint != null ? remoteHint.vulkanApiMax : 0;
        switch (resolvedType) {
            case CONTENT_TYPE_DXVK -> profile.fileList = synthesizeDxvkFiles(rootDir);
            case CONTENT_TYPE_VKD3D -> profile.fileList = synthesizeVkd3dFiles(rootDir);
            case CONTENT_TYPE_DGVOODOO -> profile.fileList = synthesizeDgVoodooFiles(rootDir);
            case CONTENT_TYPE_BOX64 -> profile.fileList = synthesizeBox64Files(rootDir);
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
            profile.verName = ImportedContentHeuristics.deriveVersionName(importDisplayName, resolvedType, deriveVersionNameFromUrl(profile.remoteUrl));
        }
        if (profile.verCode <= 0) profile.verCode = 1;
        return profile;
    }

    private boolean writeProfileSnapshot(File rootDir, ContentProfile profile) {
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

    private boolean writeSyntheticProfile(File rootDir, ContentProfile profile) {
        return writeProfileSnapshot(rootDir, profile);
    }

    private void normalizeExtractedImportRoot(File rootDir,
                                              @Nullable ContentProfile remoteHint,
                                              @Nullable String importDisplayName) {
        if (rootDir == null || !rootDir.isDirectory()) return;
        File candidate = rootDir;
        for (int depth = 0; depth < 4; depth++) {
            File next = singleNestedDirectory(candidate);
            if (next == null) break;
            boolean nestedHasRuntime = WineUtils.hasRuntimeCorePayload(next);
            boolean nestedHasProfile = new File(next, PROFILE_NAME).isFile();
            boolean nestedHasRecoverablePayload = ImportedContentHeuristics.hasRecoverablePayload(next, null, remoteHint, importDisplayName);
            if (!nestedHasRuntime && !nestedHasProfile && !nestedHasRecoverablePayload) break;
            if (!promoteNestedDirectory(rootDir, next)) break;
            candidate = rootDir;
        }
    }

    @Nullable
    private File resolveExtractedProfileFile(File rootDir) {
        if (rootDir == null || !rootDir.isDirectory()) return null;
        File profileFile = new File(rootDir, PROFILE_NAME);
        if (profileFile.isFile()) return profileFile;
        String relative = findRelativeFile(rootDir, PROFILE_NAME);
        return relative == null ? null : new File(rootDir, relative);
    }

    @Nullable
    private File singleNestedDirectory(File rootDir) {
        if (rootDir == null || !rootDir.isDirectory()) return null;
        File[] children = rootDir.listFiles();
        if (children == null || children.length != 1) return null;
        File onlyChild = children[0];
        return onlyChild != null && onlyChild.isDirectory() ? onlyChild : null;
    }

    private boolean promoteNestedDirectory(File rootDir, File nestedDir) {
        if (rootDir == null || nestedDir == null || !nestedDir.isDirectory()) return false;
        File[] children = nestedDir.listFiles();
        if (children == null || children.length == 0) return false;
        for (File child : children) {
            File target = new File(rootDir, child.getName());
            if (target.exists()) return false;
        }
        boolean movedAny = false;
        for (File child : children) {
            File target = new File(rootDir, child.getName());
            boolean moved = child.renameTo(target);
            if (!moved) {
                moved = FileUtils.copy(child, target);
                if (moved) FileUtils.delete(child);
            }
            if (!moved) return false;
            movedAny = true;
        }
        if (movedAny) FileUtils.delete(nestedDir);
        return movedAny;
    }

    private void synthesizeWineFamilyProfile(File rootDir, ContentProfile profile) {
        File binDir = WineUtils.resolveRuntimeBinDir(rootDir);
        File libDir = WineUtils.resolveRuntimeLibDir(rootDir);
        File prefixPack = WineUtils.resolveRuntimePrefixPack(rootDir);
        if (prefixPack == null && binDir != null && libDir != null) {
            prefixPack = materializeFallbackPrefixPack(rootDir, profile);
        }

        String binPath = binDir != null ? relativizePath(rootDir, binDir) : null;
        String libPath = libDir != null ? relativizePath(rootDir, libDir) : null;
        String prefixPackPath = prefixPack != null ? relativizePath(rootDir, prefixPack) : null;
        if (binPath == null || libPath == null || prefixPackPath == null) return;

        profile.wineBinPath = binPath;
        profile.wineLibPath = libPath;
        profile.winePrefixPack = prefixPackPath;
        classifyRuntimeProfileFromPayload(rootDir, profile);
        profile.fileList = new ArrayList<>();
    }

    private void classifyRuntimeProfileFromPayload(@Nullable File rootDir, @Nullable ContentProfile profile) {
        classifyRuntimeProfileFromPayload(rootDir, profile, null, profile != null ? profile.artifactName : "");
    }

    private void classifyRuntimeProfileFromPayload(@Nullable File rootDir,
                                                   @Nullable ContentProfile profile,
                                                   @Nullable ContentProfile remoteHint,
                                                   @Nullable String importDisplayName) {
        if (rootDir == null || profile == null || !profile.isWineProtonFamily()) return;
        String existingModel = ContentProfile.normalizeRuntimeModel(profile.runtimeModel);
        String payloadModel = ImportedContentHeuristics.inferRuntimeModel(rootDir, profile, remoteHint, importDisplayName);
        if (!payloadModel.isEmpty()) {
            profile.runtimeModel = payloadModel;
        } else if (!existingModel.isEmpty()) {
            profile.runtimeModel = existingModel;
        } else {
            profile.runtimeModel = profile.getRuntimeModel();
        }
    }

    @Nullable
    private File materializeFallbackPrefixPack(File rootDir, ContentProfile profile) {
        if (context == null || rootDir == null || profile == null || !profile.isWineProtonFamily()) return null;
        File fallback = new File(rootDir, FALLBACK_PREFIX_PACK_NAME);
        if (fallback.isFile() && fallback.length() > 0) return fallback;

        String runtimeModel = profile.getRuntimeModel();
        String assetName = ContentProfile.RUNTIME_MODEL_GLIBC.equals(runtimeModel)
                ? FALLBACK_PREFIX_PACK_GAMENATIVE_ASSET
                : FALLBACK_PREFIX_PACK_COMMON_ASSET;
        FileUtils.copy(context, assetName, fallback);
        if (fallback.isFile() && fallback.length() > 0) return fallback;

        if (!FALLBACK_PREFIX_PACK_GAMENATIVE_ASSET.equals(assetName)) {
            FileUtils.copy(context, FALLBACK_PREFIX_PACK_GAMENATIVE_ASSET, fallback);
            if (fallback.isFile() && fallback.length() > 0) return fallback;
        }
        FileUtils.delete(fallback);
        return null;
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

    @Nullable
    private ContentProfile repairImportedProfile(File rootDir,
                                                 @Nullable ContentProfile parsedProfile,
                                                 @Nullable ContentProfile remoteHint,
                                                 @Nullable String importDisplayName) {
        ContentProfile profile = parsedProfile != null ? parsedProfile : new ContentProfile();
        ContentProfile.ContentType resolvedType = ImportedContentHeuristics.inferContentType(rootDir, parsedProfile, remoteHint, importDisplayName);
        if (resolvedType == null) return null;

        profile.type = resolvedType;
        if (isBlank(profile.verName)) {
            if (remoteHint != null && !isBlank(remoteHint.verName)) {
                profile.verName = remoteHint.verName;
            } else {
                profile.verName = ImportedContentHeuristics.deriveVersionName(importDisplayName, resolvedType, deriveVersionNameFromUrl(profile.remoteUrl));
            }
        }
        boolean preferRemoteVersionCode = parsedProfile == null
                || (resolvedType != ContentProfile.ContentType.CONTENT_TYPE_WINE
                && resolvedType != ContentProfile.ContentType.CONTENT_TYPE_PROTON);
        if (preferRemoteVersionCode && profile.verCode <= 0 && remoteHint != null && remoteHint.verCode > 0) {
            profile.verCode = remoteHint.verCode;
        }
        if (profile.verCode <= 0) profile.verCode = 1;
        if ((profile.desc == null || profile.desc.trim().isEmpty()) && remoteHint != null) {
            profile.desc = remoteHint.desc;
        }
        if (profile.fileList == null) {
            profile.fileList = new ArrayList<>();
        }
        classifyRuntimeProfileFromPayload(rootDir, profile, remoteHint, importDisplayName);

        if (resolvedType == ContentProfile.ContentType.CONTENT_TYPE_WINE
                || resolvedType == ContentProfile.ContentType.CONTENT_TYPE_PROTON) {
            repairWineFamilyProfile(rootDir, profile, remoteHint, importDisplayName);
            if (isBlank(profile.wineBinPath) || isBlank(profile.wineLibPath) || isBlank(profile.winePrefixPack)) {
                return null;
            }
        } else {
            profile.fileList = repairContentFiles(rootDir, resolvedType, profile.fileList);
            if (profile.fileList.isEmpty()) {
                return null;
            }
        }

        profile = normalizeImportedProfile(profile, remoteHint);
        if (ContentProfileIdentity.isRuntimeAliasEquivalent(profile, remoteHint)) {
            logRuntimeAliasAccepted(profile, remoteHint);
        }
        if ((profile.verName == null || profile.verName.trim().isEmpty()) && profile.remoteUrl != null) {
            profile.verName = deriveVersionNameFromUrl(profile.remoteUrl);
        }
        return profile;
    }

    private void repairWineFamilyProfile(File rootDir,
                                         ContentProfile profile,
                                         @Nullable ContentProfile remoteHint,
                                         @Nullable String importDisplayName) {
        if (rootDir == null || profile == null || !profile.isWineProtonFamily()) return;
        if (isBlank(profile.wineBinPath) || isBlank(profile.wineLibPath) || isBlank(profile.winePrefixPack)
                || !hasResolvedRuntimePayload(rootDir, profile)) {
            synthesizeWineFamilyProfile(rootDir, profile);
        }
        classifyRuntimeProfileFromPayload(rootDir, profile, remoteHint, importDisplayName);
        if (profile.fileList == null) {
            profile.fileList = new ArrayList<>();
        }
    }

    private List<ContentProfile.ContentFile> repairContentFiles(File rootDir,
                                                                ContentProfile.ContentType type,
                                                                @Nullable List<ContentProfile.ContentFile> currentFiles) {
        LinkedHashMap<String, ContentProfile.ContentFile> filesByTarget = new LinkedHashMap<>();
        addExistingContentFiles(rootDir, type, currentFiles, filesByTarget);
        addExistingContentFiles(rootDir, type, synthesizeFilesForType(rootDir, type), filesByTarget);
        return new ArrayList<>(filesByTarget.values());
    }

    private boolean hasUsablePayloadProfile(@Nullable File rootDir, @Nullable ContentProfile profile) {
        if (rootDir == null || profile == null || profile.type == null || !rootDir.isDirectory()) return false;
        if (profile.isWineProtonFamily()) return hasResolvedRuntimePayload(rootDir, profile);
        if (hasExistingUsablePayloadFiles(rootDir, profile.type, profile.fileList)) return true;
        List<ContentProfile.ContentFile> repaired = repairContentFiles(rootDir, profile.type, profile.fileList);
        return repaired != null && !repaired.isEmpty();
    }

    private boolean hasExistingUsablePayloadFiles(@Nullable File rootDir,
                                                  @Nullable ContentProfile.ContentType type,
                                                  @Nullable List<ContentProfile.ContentFile> currentFiles) {
        if (rootDir == null || type == null || currentFiles == null || currentFiles.isEmpty()) return false;
        for (ContentProfile.ContentFile currentFile : currentFiles) {
            if (currentFile == null || isBlank(currentFile.source) || isBlank(currentFile.target)) continue;
            String normalizedSource = normalizeRelativePath(currentFile.source);
            File sourceFile = new File(rootDir, normalizedSource);
            if (!sourceFile.isFile() || !isSubPath(rootDir.getAbsolutePath(), sourceFile.getAbsolutePath())) continue;
            if (isTrustedPayloadTarget(type, currentFile.target)) return true;
        }
        return false;
    }

    private boolean repairPayloadProfileForRoot(@Nullable File rootDir,
                                                @Nullable ContentProfile profile,
                                                @NonNull String reason) {
        if (rootDir == null || profile == null || profile.type == null || profile.isWineProtonFamily()) return false;
        if (!rootDir.isDirectory()) {
            logPayloadProfileRepair(rootDir, profile, reason, "missing_root", 0, profile.fileList != null ? profile.fileList.size() : 0);
            return false;
        }

        int beforeCount = profile.fileList != null ? profile.fileList.size() : 0;
        String beforeSignature = payloadFileListSignature(profile.fileList);
        List<ContentProfile.ContentFile> repaired = repairContentFiles(rootDir, profile.type, profile.fileList);
        int afterCount = repaired != null ? repaired.size() : 0;
        if (afterCount <= 0) {
            logPayloadProfileRepair(rootDir, profile, reason, "empty_repair", beforeCount, afterCount);
            return false;
        }

        profile.fileList = repaired;
        String afterSignature = payloadFileListSignature(repaired);
        if (!beforeSignature.equals(afterSignature)) {
            boolean written = writeProfileSnapshot(rootDir, profile);
            installedProfileStateByKey.remove(buildInstalledProfileStateKey(profile));
            logPayloadProfileRepair(rootDir, profile, reason, written ? "profile_repaired" : "write_failed", beforeCount, afterCount);
            return written;
        }
        return true;
    }

    private void logPayloadProfileRepair(@Nullable File rootDir,
                                         @Nullable ContentProfile profile,
                                         @NonNull String reason,
                                         @NonNull String status,
                                         int beforeCount,
                                         int afterCount) {
        if (context == null || profile == null) return;
        ForensicLogger.logEvent(
                context,
                status.endsWith("failed") || status.startsWith("empty") || status.startsWith("missing") ? "warn" : "info",
                "CONTENTS_PAYLOAD_PROFILE_REPAIR",
                null,
                "contents",
                status,
                ForensicLogger.fields(
                        "reason", reason,
                        "type", profile.type != null ? profile.type.toString() : "-",
                        "ver_name", profile.verName == null ? "" : profile.verName,
                        "ver_code", profile.verCode,
                        "install_root", normalizePath(rootDir),
                        "before_file_count", beforeCount,
                        "after_file_count", afterCount,
                        "root_shape", summarizeRootShape(rootDir, 24)
                )
        );
    }

    private String payloadFileListSignature(@Nullable List<ContentProfile.ContentFile> files) {
        if (files == null || files.isEmpty()) return "";
        ArrayList<String> rows = new ArrayList<>();
        for (ContentProfile.ContentFile file : files) {
            if (file == null) continue;
            rows.add(normalizeRelativePath(file.source) + "->" + (file.target == null ? "" : file.target.trim()));
        }
        rows.sort(String::compareTo);
        return String.join("\n", rows);
    }

    private void addExistingContentFiles(File rootDir,
                                         ContentProfile.ContentType type,
                                         @Nullable List<ContentProfile.ContentFile> candidates,
                                         Map<String, ContentProfile.ContentFile> filesByTarget) {
        if (rootDir == null || candidates == null || filesByTarget == null) return;
        for (ContentProfile.ContentFile candidate : candidates) {
            if (candidate == null || isBlank(candidate.source) || isBlank(candidate.target)) continue;
            String normalizedSource = normalizeRelativePath(candidate.source);
            File sourceFile = new File(rootDir, normalizedSource);
            if (!sourceFile.isFile() || !isSubPath(rootDir.getAbsolutePath(), sourceFile.getAbsolutePath())) continue;
            if (!isTrustedPayloadTarget(type, candidate.target)) continue;
            if (filesByTarget.containsKey(candidate.target)) continue;
            ContentProfile.ContentFile normalized = new ContentProfile.ContentFile();
            normalized.source = normalizedSource;
            normalized.target = candidate.target.trim();
            filesByTarget.put(normalized.target, normalized);
        }
    }

    private boolean isTrustedPayloadTarget(@Nullable ContentProfile.ContentType type, @Nullable String target) {
        if (type == null || isBlank(target)) return false;
        ContentProfile probe = new ContentProfile();
        probe.type = type;
        String imagefsPath = ImageFs.find(context).getRootDir().getAbsolutePath();
        return isTrustedInstallTarget(probe, target, imagefsPath);
    }

    private List<ContentProfile.ContentFile> synthesizeFilesForType(File rootDir, ContentProfile.ContentType type) {
        if (rootDir == null || type == null) return new ArrayList<>();
        return switch (type) {
            case CONTENT_TYPE_DXVK -> synthesizeDxvkFiles(rootDir);
            case CONTENT_TYPE_VKD3D -> synthesizeVkd3dFiles(rootDir);
            case CONTENT_TYPE_DGVOODOO -> synthesizeDgVoodooFiles(rootDir);
            case CONTENT_TYPE_BOX64 -> synthesizeBox64Files(rootDir);
            case CONTENT_TYPE_WOWBOX64 -> synthesizeWowBox64Files(rootDir);
            case CONTENT_TYPE_FEXCORE -> synthesizeFexCoreFiles(rootDir);
            default -> new ArrayList<>();
        };
    }

    private void logImportRecovery(String eventId,
                                   File rootDir,
                                   @Nullable ContentProfile remoteHint,
                                   @Nullable ContentProfile profile) {
        if (context == null) return;
        ForensicLogger.logEvent(
                context,
                profile == null ? "warn" : "info",
                eventId,
                null,
                "contents_import",
                profile == null ? "profile_recovery_miss" : "profile_recovery_applied",
                ForensicLogger.fields(
                        "remote_type", remoteHint != null && remoteHint.type != null ? remoteHint.type.toString() : "-",
                        "remote_ver_name", remoteHint != null ? remoteHint.verName : "-",
                        "detected_type", profile != null && profile.type != null ? profile.type.toString() : "-",
                        "detected_ver_name", profile != null ? profile.verName : "-",
                        "detected_runtime_model", profile != null ? profile.getRuntimeModel() : "-",
                        "runtime_payload_classifier", ImportedContentHeuristics.describeRuntimePayload(rootDir, profile, remoteHint,
                                profile != null ? profile.artifactName : ""),
                        "root_shape", summarizeRootShape(rootDir, 32),
                        "root_file_count", rootDir != null && rootDir.isDirectory() && rootDir.listFiles() != null ? rootDir.listFiles().length : -1,
                        "has_profile_json", rootDir != null && new File(rootDir, PROFILE_NAME).isFile(),
                        "has_runtime_payload", profile != null && profile.isWineProtonFamily() && hasResolvedRuntimePayload(rootDir, profile),
                        "file_count", profile != null && profile.fileList != null ? profile.fileList.size() : 0
                )
        );
    }

    private void logExistingRuntimeReuse(@Nullable ContentProfile installedProfile,
                                         @Nullable ContentProfile incomingProfile,
                                         @Nullable File installRoot) {
        if (context == null) return;
        ForensicLogger.logEvent(
                context,
                "info",
                "CONTENTS_RUNTIME_INSTALL_REUSED",
                null,
                "contents",
                "install_reused_existing_runtime",
                ForensicLogger.fields(
                        "type", installedProfile != null && installedProfile.type != null ? installedProfile.type.toString() : "-",
                        "installed_ver_name", installedProfile != null ? installedProfile.verName : "-",
                        "incoming_ver_name", incomingProfile != null ? incomingProfile.verName : "-",
                        "incoming_artifact_name", incomingProfile != null ? incomingProfile.artifactName : "-",
                        "install_root", installRoot != null ? installRoot.getAbsolutePath() : "-"
                )
        );
    }

    private void logEquivalentRuntimeLookup(@Nullable ContentProfile requestedProfile,
                                            @Nullable InstalledRuntimeRoot matchedRoot,
                                            int scannedRoots,
                                            int matchedRoots) {
        if (context == null || requestedProfile == null || !requestedProfile.isWineProtonFamily()) return;
        boolean found = matchedRoot != null && matchedRoot.profile != null;
        ForensicLogger.logEvent(
                context,
                found ? "info" : "warn",
                found ? "CONTENTS_RUNTIME_EQUIVALENT_MATCH" : "CONTENTS_RUNTIME_EQUIVALENT_MISS",
                null,
                "contents_import",
                found ? "equivalent_runtime_match" : "equivalent_runtime_miss",
                ForensicLogger.fields(
                        "requested_ver_name", requestedProfile.verName == null ? "-" : requestedProfile.verName,
                        "requested_identity", ContentProfileIdentity.describeProfile(requestedProfile),
                        "scanned_roots", scannedRoots,
                        "matched_roots", matchedRoots,
                        "installed_ver_name", found && matchedRoot.profile.verName != null ? matchedRoot.profile.verName : "-",
                        "installed_identity", found ? ContentProfileIdentity.describeProfile(matchedRoot.profile) : "-",
                        "install_root", found && matchedRoot.installRoot != null ? matchedRoot.installRoot.getAbsolutePath() : "-"
                )
        );
    }

    private void logRuntimeInstallRootResolution(@Nullable ContentProfile requestedProfile,
                                                 @Nullable ContentProfile installedProfile,
                                                 @Nullable File resolvedRoot,
                                                 @Nullable File canonicalRoot,
                                                 @NonNull String action) {
        if (context == null || requestedProfile == null) return;
        ForensicLogger.logEvent(
                context,
                "info",
                "CONTENTS_RUNTIME_INSTALL_ROOT_RECONCILED",
                null,
                "contents",
                "runtime_install_root_reconciled",
                ForensicLogger.fields(
                        "requested_ver_name", requestedProfile.verName == null ? "-" : requestedProfile.verName,
                        "requested_identity", ContentProfileIdentity.describeProfile(requestedProfile),
                        "installed_ver_name", installedProfile != null && installedProfile.verName != null ? installedProfile.verName : "-",
                        "installed_identity", installedProfile != null ? ContentProfileIdentity.describeProfile(installedProfile) : "-",
                        "resolved_root", resolvedRoot != null ? resolvedRoot.getAbsolutePath() : "-",
                        "canonical_root", canonicalRoot != null ? canonicalRoot.getAbsolutePath() : "-",
                        "action", action
                )
        );
    }

    private void logRuntimeAliasAccepted(@Nullable ContentProfile profile,
                                         @Nullable ContentProfile remoteHint) {
        if (context == null || profile == null || remoteHint == null) return;
        ForensicLogger.logEvent(
                context,
                "info",
                "CONTENTS_RUNTIME_ALIAS_ACCEPTED",
                null,
                "contents_import",
                "runtime_alias_accepted",
                ForensicLogger.fields(
                        "resolved_ver_name", profile.verName == null ? "-" : profile.verName,
                        "resolved_identity", ContentProfileIdentity.describeProfile(profile),
                        "remote_ver_name", remoteHint.verName == null ? "-" : remoteHint.verName,
                        "remote_identity", ContentProfileIdentity.describeProfile(remoteHint)
                )
        );
    }

    private boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    @NonNull
    private String resolveImportDisplayName(@Nullable Uri uri) {
        if (uri == null || context == null) return "";
        String displayName = FileUtils.getUriFileName(context, uri);
        if (displayName != null && !displayName.trim().isEmpty()) return displayName.trim();
        String lastPathSegment = uri.getLastPathSegment();
        return lastPathSegment == null ? "" : FileUtils.getName(lastPathSegment);
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

    private List<ContentProfile.ContentFile> synthesizeBox64Files(File rootDir) {
        ArrayList<ContentProfile.ContentFile> files = new ArrayList<>();
        String relative = findRelativeFile(rootDir, "box64");
        if (relative == null) return files;
        for (String target : BOX64_TRUST_FILES) {
            ContentProfile.ContentFile item = new ContentProfile.ContentFile();
            item.source = relative;
            item.target = target;
            files.add(item);
        }
        return files;
    }

    private List<ContentProfile.ContentFile> synthesizeDgVoodooFiles(File rootDir) {
        String[][] mappings = {
                {"dgvoodoo.conf", "dgVoodoo.conf"},
                {"dgvoodoocpl.exe", "dgVoodooCpl.exe"},
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
        return synthesizeMappedFiles(rootDir, mappings);
    }

    private List<ContentProfile.ContentFile> synthesizeMappedFiles(File rootDir, String[][] mappings) {
        ArrayList<ContentProfile.ContentFile> files = new ArrayList<>();
        for (String[] mapping : mappings) {
            String relative = resolveMappedSourceFile(rootDir, mapping[0], mapping[1]);
            if (relative == null) continue;
            ContentProfile.ContentFile item = new ContentProfile.ContentFile();
            item.source = relative;
            item.target = mapping[1];
            files.add(item);
        }
        return files;
    }

    @Nullable
    private String resolveMappedSourceFile(File rootDir, String preferredPath, String targetPath) {
        if (rootDir == null || preferredPath == null || preferredPath.trim().isEmpty()) return null;
        String normalizedPreferred = normalizeRelativePath(preferredPath);
        File direct = new File(rootDir, normalizedPreferred);
        if (direct.isFile()) return normalizedPreferred;

        String fileName = new File(normalizedPreferred).getName();
        return findRelativeFileWithPreferredFragments(
                rootDir,
                fileName,
                buildSourcePreferenceFragments(normalizedPreferred, targetPath)
        );
    }

    private String[] buildSourcePreferenceFragments(String preferredPath, String targetPath) {
        ArrayList<String> fragments = new ArrayList<>();
        addPreferenceFragment(fragments, parentRelativePath(preferredPath));

        String target = targetPath == null ? "" : targetPath.toLowerCase(Locale.US);
        if (target.contains("${syswow64}")) {
            addPreferenceFragment(fragments, "syswow64");
            addPreferenceFragment(fragments, "payload/runtime/x86");
            addPreferenceFragment(fragments, "ms/x86");
            addPreferenceFragment(fragments, "release/x86");
            addPreferenceFragment(fragments, "bin/x86");
            addPreferenceFragment(fragments, "x86");
            addPreferenceFragment(fragments, "x32");
            addPreferenceFragment(fragments, "i386");
            addPreferenceFragment(fragments, "lib/wine/i386-windows");
        } else if (target.contains("${system32}")) {
            addPreferenceFragment(fragments, "system32");
            addPreferenceFragment(fragments, "payload/runtime/arm64ec");
            addPreferenceFragment(fragments, "ms/arm64ec");
            addPreferenceFragment(fragments, "release/arm64ec");
            addPreferenceFragment(fragments, "bin/arm64ec");
            addPreferenceFragment(fragments, "arm64ec");
            addPreferenceFragment(fragments, "arm64-ec");
            addPreferenceFragment(fragments, "payload/runtime/arm64");
            addPreferenceFragment(fragments, "ms/arm64");
            addPreferenceFragment(fragments, "aarch64");
            addPreferenceFragment(fragments, "payload/runtime/x64");
            addPreferenceFragment(fragments, "ms/x64");
            addPreferenceFragment(fragments, "x64");
            addPreferenceFragment(fragments, "x86_64");
            addPreferenceFragment(fragments, "amd64");
            addPreferenceFragment(fragments, "lib/wine/aarch64-windows");
            addPreferenceFragment(fragments, "lib/wine/x86_64-windows");
        }
        return fragments.toArray(new String[0]);
    }

    private void addPreferenceFragment(List<String> fragments, @Nullable String fragment) {
        if (fragments == null || fragment == null) return;
        String normalized = normalizeRelativePath(fragment).toLowerCase(Locale.US);
        if (normalized.isEmpty() || ".".equals(normalized)) return;
        if (!fragments.contains(normalized)) fragments.add(normalized);
    }

    @Nullable
    private String findRelativeFileWithPreferredFragments(File rootDir,
                                                          String fileName,
                                                          @Nullable String[] preferredFragments) {
        if (rootDir == null || fileName == null || fileName.trim().isEmpty()) return null;
        ArrayList<String> matches = new ArrayList<>();
        collectRelativeFileMatches(rootDir, rootDir, fileName.trim().toLowerCase(Locale.US), matches);
        if (matches.isEmpty()) return null;
        matches.sort((left, right) -> {
            int scoreCompare = Integer.compare(
                    scoreRelativeFileMatch(right, preferredFragments),
                    scoreRelativeFileMatch(left, preferredFragments)
            );
            if (scoreCompare != 0) return scoreCompare;
            int lengthCompare = Integer.compare(left.length(), right.length());
            if (lengthCompare != 0) return lengthCompare;
            return left.compareToIgnoreCase(right);
        });
        return matches.get(0);
    }

    private void collectRelativeFileMatches(File rootDir, File current, String normalizedName, List<String> matches) {
        File[] children = current.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                collectRelativeFileMatches(rootDir, child, normalizedName, matches);
                continue;
            }
            if (child.getName().trim().toLowerCase(Locale.US).equals(normalizedName)) {
                String relative = relativizePath(rootDir, child);
                if (relative != null && !relative.trim().isEmpty()) matches.add(normalizeRelativePath(relative));
            }
        }
    }

    private int scoreRelativeFileMatch(String relativePath, @Nullable String[] preferredFragments) {
        String normalized = normalizeRelativePath(relativePath).toLowerCase(Locale.US);
        if (!normalized.contains("/")) return 50;
        int score = 0;
        if (preferredFragments != null) {
            for (int i = 0; i < preferredFragments.length; i++) {
                String fragment = preferredFragments[i];
                if (fragment == null || fragment.isEmpty()) continue;
                int weight = Math.max(1, 1000 - (i * 12));
                if (normalized.equals(fragment)) score = Math.max(score, weight);
                if (normalized.startsWith(fragment + "/")) score = Math.max(score, weight);
                if (normalized.contains("/" + fragment + "/")) score = Math.max(score, weight - 120);
            }
        }
        return score;
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

    private static String normalizePath(@Nullable File file) {
        return file != null ? file.getAbsolutePath() : "-";
    }

    private static String summarizeRootShape(@Nullable File rootDir, int limit) {
        if (rootDir == null || !rootDir.isDirectory()) return "-";
        File[] children = rootDir.listFiles();
        if (children == null || children.length == 0) return "";
        ArrayList<String> names = new ArrayList<>();
        int max = Math.max(1, limit);
        for (File child : children) {
            if (child == null) continue;
            names.add(child.getName() + (child.isDirectory() ? "/" : ""));
            if (names.size() >= max) break;
        }
        if (children.length > names.size()) names.add("+" + (children.length - names.size()) + "_more");
        return String.join(",", names);
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
