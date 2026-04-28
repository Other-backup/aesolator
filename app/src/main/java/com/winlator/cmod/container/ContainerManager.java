package com.winlator.cmod.container;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.winlator.cmod.R;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.MSLink;
import com.winlator.cmod.core.OnExtractFileListener;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.xenvironment.ImageFs;
import com.winlator.cmod.xenvironment.ImageFsInstaller;

import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Executors;

public class ContainerManager {
    private final ArrayList<Container> containers = new ArrayList<>();
    private int maxContainerId = 0;
    private final File homeDir;
    private final Context context;

    public ContainerManager(Context context) {
        this.context = context;
        File rootDir = ImageFs.find(context).getRootDir();
        homeDir = new File(rootDir, "home");
        loadContainers();
    }

    public ArrayList<Container> getContainers() {
        return containers;
    }

    // Load containers from the home directory
    private void loadContainers() {
        containers.clear();
        maxContainerId = 0;

        File[] files = homeDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!file.isDirectory()) continue;
            String dirName = file.getName();
            if (!dirName.startsWith(ImageFs.USER + "-")) continue;
            Integer containerId = parseContainerIdFromHomeDirName(dirName);
            if (containerId == null) {
                Log.d("ContainerManager", "Ignoring auxiliary container home: " + dirName);
                continue;
            }
            try {
                Container container = new Container(
                        containerId, this
                );
                container.setRootDir(file);
                File configFile = container.getConfigFile();
                if (!configFile.isFile()) {
                    if (tryRecoverOrphanContainer(container, file)) {
                        containers.add(container);
                        maxContainerId = Math.max(maxContainerId, container.id);
                        continue;
                    }
                    Log.w("ContainerManager", "Skipping container without config: " + file.getName());
                    continue;
                }
                String configContent = FileUtils.readString(configFile);
                if (configContent == null || configContent.trim().isEmpty()) {
                    Log.w("ContainerManager", "Skipping container with unreadable config: " + file.getName());
                    continue;
                }
                JSONObject data = new JSONObject(configContent);
                container.loadData(data);
                containers.add(container);
                maxContainerId = Math.max(maxContainerId, container.id);
            } catch (JSONException | NumberFormatException | NullPointerException e) {
                Log.e("ContainerManager", "Skipping broken container: " + file.getName(), e);
            }
        }
    }

    private Integer parseContainerIdFromHomeDirName(String dirName) {
        if (dirName == null) return null;
        String prefix = ImageFs.USER + "-";
        if (!dirName.startsWith(prefix)) return null;
        String suffix = dirName.substring(prefix.length());
        if (suffix.isEmpty()) return null;
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean tryRecoverOrphanContainer(Container container, File containerDir) {
        if (container == null || containerDir == null || !containerDir.isDirectory()) return false;

        File wineRoot = new File(containerDir, ".wine");
        if (!wineRoot.isDirectory()) return false;

        LocalRuntimeCandidate runtimeCandidate = findBestLocalRuntimeCandidate();
        if (runtimeCandidate == null) {
            ForensicLogger.logEvent(
                    context,
                    "warn",
                    "CONTAINER_CONFIG_RECOVERY_SKIPPED",
                    null,
                    "containers",
                    "missing_runtime_for_orphan_container",
                    ForensicLogger.fields(
                            "container_id", container.id,
                            "container_dir", containerDir.getAbsolutePath()
                    )
            );
            return false;
        }

        container.setName("Container-" + container.id);
        container.setEmulator(Container.DEFAULT_EMULATOR);
        container.setWineVersion(runtimeCandidate.entryName);
        container.saveData();

        ForensicLogger.logEvent(
                context,
                "warn",
                "CONTAINER_CONFIG_RECOVERED",
                null,
                "containers",
                "recovered_orphan_container_config",
                ForensicLogger.fields(
                        "container_id", container.id,
                        "container_dir", containerDir.getAbsolutePath(),
                        "runtime_entry", runtimeCandidate.entryName,
                        "runtime_type", runtimeCandidate.type.toString(),
                        "runtime_version_code", runtimeCandidate.verCode,
                        "runtime_last_modified", runtimeCandidate.lastModified
                )
        );
        Log.w("ContainerManager", "Recovered container config for " + containerDir.getName() + " using " + runtimeCandidate.entryName);
        return container.getConfigFile().isFile();
    }

    private LocalRuntimeCandidate findBestLocalRuntimeCandidate() {
        ArrayList<LocalRuntimeCandidate> candidates = new ArrayList<>();
        ContentsManager manager = new ContentsManager(context);
        manager.syncContents();
        collectRuntimeCandidates(
                manager,
                candidates,
                ContentProfile.ContentType.CONTENT_TYPE_PROTON,
                ContentsManager.getContentTypeDir(context, ContentProfile.ContentType.CONTENT_TYPE_PROTON)
        );
        collectRuntimeCandidates(
                manager,
                candidates,
                ContentProfile.ContentType.CONTENT_TYPE_WINE,
                ContentsManager.getContentTypeDir(context, ContentProfile.ContentType.CONTENT_TYPE_WINE)
        );
        if (candidates.isEmpty()) return null;
        candidates.sort(
                Comparator
                        .comparingInt((LocalRuntimeCandidate candidate) -> candidate.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON ? 1 : 0)
                        .thenComparingLong(candidate -> candidate.lastModified)
                        .thenComparingInt(candidate -> candidate.verCode)
                        .reversed()
        );
        return candidates.get(0);
    }

    private void collectRuntimeCandidates(ContentsManager manager, ArrayList<LocalRuntimeCandidate> out, ContentProfile.ContentType type, File typeDir) {
        if (out == null || type == null || typeDir == null || !typeDir.isDirectory()) return;

        File[] installedRoots = typeDir.listFiles();
        if (installedRoots == null) return;

        for (File installRoot : installedRoots) {
            if (installRoot == null || !installRoot.isDirectory()) continue;
            File profileFile = new File(installRoot, ContentsManager.PROFILE_NAME);
            if (!profileFile.isFile()) continue;
            ContentProfile profile = manager.readProfile(profileFile);
            if (profile == null || profile.type != type) continue;
            File runtimeRoot = WineUtils.resolveCanonicalRuntimeRoot(installRoot);
            if (runtimeRoot == null || !WineUtils.hasRuntimePayload(runtimeRoot)) continue;
            String dirName = installRoot.getName();
            int dashIndex = dirName.lastIndexOf('-');
            if (dashIndex <= 0 || dashIndex >= dirName.length() - 1) continue;
            try {
                int versionCode = Integer.parseInt(dirName.substring(dashIndex + 1));
                out.add(new LocalRuntimeCandidate(
                        profile.type,
                        profile.type.toString() + "-" + dirName,
                        versionCode,
                        installRoot.lastModified()
                ));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static final class LocalRuntimeCandidate {
        private final ContentProfile.ContentType type;
        private final String entryName;
        private final int verCode;
        private final long lastModified;

        private LocalRuntimeCandidate(ContentProfile.ContentType type, String entryName, int verCode, long lastModified) {
            this.type = type;
            this.entryName = entryName;
            this.verCode = verCode;
            this.lastModified = lastModified;
        }
    }


    public Context getContext() {
        return context;
    }


    public void activateContainer(Container container) {
        activateContainerHome(homeDir, container);
    }

    public static File resolveContainerHomeDir(File homeDir, int containerId) {
        return new File(homeDir, ImageFs.USER + "-" + containerId);
    }

    public static void activateContainerHome(File homeDir, Container container) {
        if (homeDir == null || container == null) return;

        File targetDir = resolveContainerHomeDir(homeDir, container.id);
        container.setRootDir(targetDir);
        if (!targetDir.exists()) targetDir.mkdirs();

        File activeHomeLink = new File(homeDir, ImageFs.USER);
        if (activeHomeLink.exists() && !FileUtils.isSymlink(activeHomeLink)) {
            migrateEssentialFiles(activeHomeLink, targetDir);
        }
        FileUtils.delete(activeHomeLink);
        FileUtils.symlink("./" + ImageFs.USER + "-" + container.id, activeHomeLink.getPath());
    }

    private static void migrateEssentialFiles(File sourceDir, File destDir) {
        if (sourceDir == null || destDir == null) return;
        String[] essentialPaths = {
                ".wine/drive_c/windows/winhandler.exe",
                ".wine/drive_c/windows/wfm.exe"
        };
        for (String path : essentialPaths) {
            File source = new File(sourceDir, path);
            File dest = new File(destDir, path);
            if (!source.isFile() || dest.exists()) continue;
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            FileUtils.copy(source, dest);
            Log.d("ContainerManager", "Migrated " + path + " to container");
        }
    }

    public void createContainerAsync(final JSONObject data, ContentsManager contentsManager, Callback<Container> callback) {
        final Handler handler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            final Container container;
            try {
                container = createContainer(data, contentsManager);
            } catch (Throwable t) {
                Log.e("ContainerManager", "Container creation failed", t);
                handler.post(() -> callback.call(null));
                return;
            }
            handler.post(() -> callback.call(container));
        });
    }

    public void duplicateContainerAsync(Container container, Runnable callback) {
        final Handler handler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            duplicateContainer(container);
            handler.post(callback);
        });
    }

    public void removeContainerAsync(Container container, Runnable callback) {
        final Handler handler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            removeContainer(container);
            handler.post(callback);
        });
    }

    private Container createContainer(JSONObject data, ContentsManager contentsManager) {
        try {
            int id = maxContainerId + 1;
            data.put("id", id);
            String requestedWineVersion = data.optString("wineVersion", "");
            String requestedVariant = data.optString("containerVariant", "");
            ForensicLogger.logEvent(
                    context,
                    "info",
                    "CONTAINER_CREATE_START",
                    null,
                    "containers",
                    "container_create_start",
                    ForensicLogger.fields(
                            "container_id", id,
                            "wine_version", requestedWineVersion,
                            "container_variant", requestedVariant
                    )
            );

            File containerDir = new File(homeDir, ImageFs.USER+"-"+id);
            if (!containerDir.mkdirs()) {
                ForensicLogger.logEvent(
                        context,
                        "error",
                        "CONTAINER_CREATE_FAILED",
                        null,
                        "containers",
                        "container_directory_create_failed",
                        ForensicLogger.fields(
                                "container_id", id,
                                "container_dir", containerDir.getAbsolutePath()
                        )
                );
                return null;
            }

            Container container = new Container(id, this);
            container.setRootDir(containerDir);
            container.loadData(data);

            container.setWineVersion(data.getString("wineVersion"));

            if (!extractContainerPatternFile(container, container.getWineVersion(), contentsManager, containerDir, null)) {
                FileUtils.delete(containerDir);
                ForensicLogger.logEvent(
                        context,
                        "error",
                        "CONTAINER_CREATE_FAILED",
                        null,
                        "containers",
                        "container_prefix_extract_failed",
                        ForensicLogger.fields(
                                "container_id", id,
                                "wine_version", container.getWineVersion(),
                                "container_variant", container.getContainerVariant(),
                                "container_dir", containerDir.getAbsolutePath()
                        )
                );
                return null;
            }

            container.setNeedsUnpacking(false);

//            // Extract the selected graphics driver files
//            String driverVersion = container.getGraphicsDriverVersion();
//            if (!extractGraphicsDriverFiles(driverVersion, containerDir, null)) {
//                FileUtils.delete(containerDir);
//                return null;
//            }

            container.saveData();
            maxContainerId++;
            containers.add(container);
            ForensicLogger.logEvent(
                    context,
                    "info",
                    "CONTAINER_CREATE_READY",
                    null,
                    "containers",
                    "container_create_ready",
                    ForensicLogger.fields(
                            "container_id", id,
                            "wine_version", container.getWineVersion(),
                            "container_variant", container.getContainerVariant(),
                            "container_dir", containerDir.getAbsolutePath(),
                            "prefix_valid", WineUtils.isPrefixValid(containerDir)
                    )
            );
            return container;
        } catch (Exception e) {
            Log.e("ContainerManager", "Failed to create container", e);
            ForensicLogger.logEvent(
                    context,
                    "error",
                    "CONTAINER_CREATE_EXCEPTION",
                    null,
                    "containers",
                    "container_create_exception",
                    ForensicLogger.fields(
                            "error_class", e.getClass().getName(),
                            "error_message", e.getMessage() == null ? "" : e.getMessage()
                    )
            );
        }
        return null;
    }


    private void duplicateContainer(Container srcContainer) {
        int id = maxContainerId + 1;

        File dstDir = new File(homeDir, ImageFs.USER + "-" + id);
        if (!dstDir.mkdirs()) return;

        // Use the refactored copy method that doesn't require a Context for File operations
        if (!FileUtils.copy(srcContainer.getRootDir(), dstDir, file -> FileUtils.chmod(file, 0771))) {
            FileUtils.delete(dstDir);
            return;
        }
        ImageFsInstaller.ensureWinePrefixPrivatePermissions(context, dstDir);

        Container dstContainer = new Container(id, this);
        dstContainer.setRootDir(dstDir);
        dstContainer.setName(srcContainer.getName() + " (" + context.getString(R.string._copy) + ")");
        dstContainer.setScreenSize(srcContainer.getScreenSize());
        dstContainer.setEnvVars(srcContainer.getEnvVars());
        dstContainer.setCPUList(srcContainer.getCPUList());
        dstContainer.setCPUListWoW64(srcContainer.getCPUListWoW64());
        dstContainer.setGraphicsDriver(srcContainer.getGraphicsDriver());
        dstContainer.setDXWrapper(srcContainer.getDXWrapper());
        dstContainer.setDXWrapperConfig(srcContainer.getDXWrapperConfig());
        dstContainer.setAudioDriver(srcContainer.getAudioDriver());
        dstContainer.setWinComponents(srcContainer.getWinComponents());
        dstContainer.setDrives(srcContainer.getDrives());
        dstContainer.setShowFPS(srcContainer.isShowFPS());
        dstContainer.setStartupSelection(srcContainer.getStartupSelection());
        dstContainer.setBox64Preset(srcContainer.getBox64Preset());
        dstContainer.setDesktopTheme(srcContainer.getDesktopTheme());
        dstContainer.setWineVersion(srcContainer.getWineVersion());
        dstContainer.setNeedsUnpacking(false);
        dstContainer.saveData();

        maxContainerId++;
        containers.add(dstContainer);
    }


    private void removeContainer(Container container) {
        if (FileUtils.delete(container.getRootDir())) containers.remove(container);
    }

    public ArrayList<Shortcut> loadShortcuts() {
        ArrayList<Shortcut> shortcuts = new ArrayList<>();
        for (Container container : containers) {
            File desktopDir = container.getDesktopDir();
            ArrayList<File> files = new ArrayList<>();
            if (desktopDir.exists())
                files.addAll(Arrays.asList(desktopDir.listFiles()));
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (fileName.endsWith(".lnk")) {
                        String filePath = file.getPath();
                        File desktopFile = new File(filePath.substring(0, filePath.lastIndexOf(".")) + ".desktop");
                        if (!desktopFile.exists()) {
                            MSLink.createDesktopFile(file, context);
                            shortcuts.add(new Shortcut(container, desktopFile));
                        }
                    }
                    else if (fileName.endsWith(".desktop")) shortcuts.add(new Shortcut(container, file));
                }
            }
        }

        shortcuts.sort(Comparator.comparing(a -> a.name));
        return shortcuts;
    }

    public int getNextContainerId() {
        return maxContainerId + 1;
    }

    public Container getContainerById(int id) {
        for (Container container : containers) if (container.id == id) return container;
        return null;
    }

    private void extractCommonDlls(WineInfo wineInfo, String srcName, String dstName, File containerDir, OnExtractFileListener onExtractFileListener) throws JSONException {
        File runtimeRoot = wineInfo.path == null ? null : new File(wineInfo.path);
        File wineLibDir = WineUtils.resolveRuntimeWineLibDir(runtimeRoot);
        File srcDir = wineLibDir == null ? null : new File(wineLibDir, srcName);

        if (srcDir == null || !srcDir.isDirectory()) {
            String expectedDir = srcDir != null
                    ? srcDir.getAbsolutePath()
                    : runtimeRoot == null
                    ? ""
                    : new File(runtimeRoot, "lib/wine/" + srcName).getAbsolutePath();
            throw new JSONException("Missing Wine runtime directory: " + expectedDir);
        }

        File[] srcfiles = srcDir.listFiles(file -> file.isFile());
        if (srcfiles == null) {
            throw new JSONException("Unable to enumerate runtime DLLs in " + srcDir.getAbsolutePath());
        }

        for (File file : srcfiles) {
            String dllName = file.getName();
            if (dllName.equals("iexplore.exe") && wineInfo.isArm64EC() && srcName.equals("aarch64-windows")) {
                File wow32Iexplore = wineLibDir == null ? null : new File(wineLibDir, "i386-windows/iexplore.exe");
                if (wow32Iexplore != null && wow32Iexplore.isFile()) file = wow32Iexplore;
            }
            if (dllName.equals("tabtip.exe") || dllName.equals("icu.dll"))
                continue;
            File dstFile = new File(containerDir, ".wine/drive_c/windows/" + dstName + "/" + dllName);
            if (dstFile.exists()) continue;
            if (onExtractFileListener != null ) {
                dstFile = onExtractFileListener.onExtractFile(dstFile, 0);
                if (dstFile == null) continue;
            }
            FileUtils.copy(file, dstFile);
        }
    }

    private static boolean extractPrefixPack(String wineInstallPath, File destinationDir) {
        if (wineInstallPath == null || wineInstallPath.trim().isEmpty()) {
            return false;
        }

        File runtimeRoot = new File(wineInstallPath);
        File prefixPack = WineUtils.resolveRuntimePrefixPack(runtimeRoot);
        if (prefixPack != null && prefixPack.isFile()) {
            TarCompressorUtils.Type archiveType = prefixPack.getName().endsWith(".tzst")
                    ? TarCompressorUtils.Type.ZSTD
                    : TarCompressorUtils.Type.XZ;
            return TarCompressorUtils.extract(archiveType, prefixPack, destinationDir);
        }

        return false;
    }

    public boolean extractContainerPatternFile(Container container, String wineVersion, ContentsManager contentsManager, File containerDir, OnExtractFileListener onExtractFileListener) {
        String requestedRuntimeModel = container != null
                ? container.getContainerVariant()
                : com.winlator.cmod.contents.ContentProfile.inferRuntimeModelFromEntryName(wineVersion);
        WineInfo wineInfo = WineInfo.fromIdentifier(context, contentsManager, wineVersion, requestedRuntimeModel);
        File runtimeRoot = wineInfo.path == null || wineInfo.path.trim().isEmpty() ? null : new File(wineInfo.path);
        ForensicLogger.logEvent(
                context,
                "info",
                "CONTAINER_PREFIX_RUNTIME_RESOLVED",
                null,
                "containers",
                "container_prefix_runtime_resolved",
                ForensicLogger.fields(
                        "container_id", container != null ? container.id : -1,
                        "requested_wine_version", wineVersion == null ? "" : wineVersion,
                        "requested_runtime_model", requestedRuntimeModel == null ? "" : requestedRuntimeModel,
                        "resolved_type", wineInfo.type,
                        "resolved_version", wineInfo.fullVersion(),
                        "resolved_arch", wineInfo.getArch(),
                        "resolved_path", wineInfo.path == null ? "" : wineInfo.path,
                        "runtime_root_exists", runtimeRoot != null && runtimeRoot.isDirectory(),
                        "runtime_payload_complete", runtimeRoot != null && WineUtils.hasRuntimePayload(runtimeRoot)
                )
        );
        if (wineInfo.path == null || wineInfo.path.trim().isEmpty()) {
            ForensicLogger.logEvent(
                    context,
                    "error",
                    "CONTAINER_PREFIX_RUNTIME_MISSING",
                    null,
                    "containers",
                    "container_prefix_runtime_missing",
                    ForensicLogger.fields(
                            "container_id", container != null ? container.id : -1,
                            "requested_wine_version", wineVersion == null ? "" : wineVersion,
                            "requested_runtime_model", requestedRuntimeModel == null ? "" : requestedRuntimeModel
                    )
            );
            return false;
        }
        boolean result;
        String patternSource;
        if (WineInfo.isMainWineVersion(wineVersion)) {
            patternSource = "container_pattern_gamenative.tzst";
            result = TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context,
                    patternSource,
                    containerDir,
                    onExtractFileListener
            );
            if (!result) {
                String containerPattern = wineVersion + "_container_pattern.tzst";
                patternSource = containerPattern;
                result = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, containerPattern, containerDir, onExtractFileListener);
            }
        } else {
            String containerPattern = wineVersion + "_container_pattern.tzst";
            patternSource = containerPattern;
            result = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, containerPattern, containerDir, onExtractFileListener);
        }
        ForensicLogger.logEvent(
                context,
                result ? "info" : "warn",
                "CONTAINER_PREFIX_PATTERN_EXTRACT_RESULT",
                null,
                "containers",
                result ? "container_prefix_pattern_extract_ready" : "container_prefix_pattern_extract_failed",
                ForensicLogger.fields(
                        "container_id", container != null ? container.id : -1,
                        "pattern_source", patternSource,
                        "result", result
                )
        );

        if (!result) {
            result = extractPrefixPack(wineInfo.path, containerDir);
            ForensicLogger.logEvent(
                    context,
                    result ? "info" : "error",
                    "CONTAINER_PREFIX_PACK_EXTRACT_RESULT",
                    null,
                    "containers",
                    result ? "container_prefix_pack_extract_ready" : "container_prefix_pack_extract_failed",
                    ForensicLogger.fields(
                            "container_id", container != null ? container.id : -1,
                            "runtime_root", wineInfo.path,
                            "prefix_pack_present", WineUtils.resolveRuntimePrefixPack(new File(wineInfo.path)) != null,
                            "result", result
                    )
            );
        }

        if (result) {
            try {
                if (wineInfo.usesAarch64WindowsTree())
                    extractCommonDlls(wineInfo, "aarch64-windows", "system32", containerDir, onExtractFileListener);
                else
                    extractCommonDlls(wineInfo, "x86_64-windows", "system32", containerDir, onExtractFileListener);

                extractCommonDlls(wineInfo, "i386-windows", "syswow64", containerDir, onExtractFileListener);
                String preferredGraphicsDriver = WineUtils.resolvePreferredGraphicsDriver(new File(wineInfo.path), wineInfo);
                if (Container.BIONIC.equalsIgnoreCase(requestedRuntimeModel) && !preferredGraphicsDriver.isEmpty()) {
                    WineUtils.ensureGraphicsDriverRegistry(containerDir, preferredGraphicsDriver);
                    boolean x11OpenGlBackendContractApplied = WineUtils.graphicsDriverIncludesX11(preferredGraphicsDriver)
                            && WineUtils.ensureX11OpenGlBackendRegistry(containerDir, true);
                    ForensicLogger.logEvent(
                            context,
                            "info",
                            "CONTAINER_PREFIX_GRAPHICS_REGISTRY_SEEDED",
                            null,
                            "containers",
                            "container_prefix_graphics_registry_seeded",
                            ForensicLogger.fields(
                                    "container_id", container != null ? container.id : -1,
                                    "graphics_driver", preferredGraphicsDriver,
                                    "x11_use_egl", x11OpenGlBackendContractApplied ? "N" : "",
                                    "x11_force_glx_registry", x11OpenGlBackendContractApplied,
                                    "runtime_root", wineInfo.path
                            )
                    );
                }
                ForensicLogger.logEvent(
                        context,
                        "info",
                        "CONTAINER_PREFIX_COMMON_DLLS_READY",
                        null,
                        "containers",
                        "container_prefix_common_dlls_ready",
                        ForensicLogger.fields(
                                "container_id", container != null ? container.id : -1,
                                "wine_arch", wineInfo.getArch(),
                                "uses_aarch64_windows_tree", wineInfo.usesAarch64WindowsTree(),
                                "prefix_valid", WineUtils.isPrefixValid(containerDir)
                        )
                );
            }
            catch (JSONException e) {
                ForensicLogger.logEvent(
                        context,
                        "error",
                        "CONTAINER_PREFIX_COMMON_DLLS_FAILED",
                        null,
                        "containers",
                        "container_prefix_common_dlls_failed",
                        ForensicLogger.fields(
                                "container_id", container != null ? container.id : -1,
                                "error_class", e.getClass().getName(),
                                "error_message", e.getMessage() == null ? "" : e.getMessage()
                        )
                );
                return false;
            }
        }

        if (result) ImageFsInstaller.ensureWinePrefixPrivatePermissions(context, containerDir);
        return result;
    }

    public boolean repairContainerWinePrefix(
            Container container,
            String wineVersion,
            ContentsManager contentsManager,
            OnExtractFileListener onExtractFileListener
    ) {
        File containerDir = container != null ? container.getRootDir() : null;
        if (containerDir == null || !containerDir.isDirectory()) return false;

        File tempDir = FileUtils.createTempFile(context.getCacheDir(), "wineprefix-repair");
        if (!tempDir.mkdirs()) {
            Log.e("ContainerManager", "repairContainerWinePrefix: failed to create temp dir " + tempDir.getAbsolutePath());
            return false;
        }

        try {
            boolean extracted = extractContainerPatternFile(
                    container,
                    wineVersion,
                    contentsManager,
                    tempDir,
                    onExtractFileListener
            );
            if (!extracted) {
                Log.e("ContainerManager", "repairContainerWinePrefix: failed to extract repair prefix for " + wineVersion);
                return false;
            }

            File repairedPrefixDir = new File(tempDir, ".wine");
            if (!WineUtils.isPrefixValid(tempDir) || !repairedPrefixDir.isDirectory()) {
                Log.e("ContainerManager", "repairContainerWinePrefix: extracted prefix is still invalid");
                return false;
            }

            File targetPrefixDir = new File(containerDir, ".wine");
            if (targetPrefixDir.exists() && !FileUtils.delete(targetPrefixDir)) {
                Log.e("ContainerManager", "repairContainerWinePrefix: failed to clear existing prefix " + targetPrefixDir.getAbsolutePath());
                return false;
            }

            if (!FileUtils.copy(repairedPrefixDir, targetPrefixDir)) {
                Log.e("ContainerManager", "repairContainerWinePrefix: failed to copy repaired prefix");
                return false;
            }

            String requestedRuntimeModel = container != null ? container.getContainerVariant() : null;
            WineInfo wineInfo = WineInfo.fromIdentifier(context, contentsManager, wineVersion, requestedRuntimeModel);
            container.putExtra("wineprefixArch", wineInfo.getArch());
            container.putExtra("wineprefixNeedsUpdate", null);
            container.putExtra("appVersion", null);
            container.putExtra("imgVersion", null);
            container.putExtra("dxwrapper", null);
            container.putExtra("wincomponents", null);
            container.putExtra("desktopTheme", null);
            container.putExtra("startupSelection", null);
            container.putExtra("mono_installed", null);
            container.putExtra("mono_version", null);
            container.saveData();
            return true;
        } finally {
            FileUtils.delete(tempDir);
        }
    }

    public Container getContainerForShortcut(Shortcut shortcut) {
        // Search for the container by its ID
        for (Container container : containers) {
            if (container.id == shortcut.getContainerId()) {
                return container;
            }
        }
        return null;  // Return null if no matching container is found
    }
}
