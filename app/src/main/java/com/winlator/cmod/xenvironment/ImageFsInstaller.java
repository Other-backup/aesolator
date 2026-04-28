package com.winlator.cmod.xenvironment;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.winlator.cmod.MainActivity;
import com.winlator.cmod.R;
import com.winlator.cmod.SettingsFragment;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.WineRuntimeRunpathSanitizer;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.DownloadProgressDialog;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.contents.PrefixPackCatalog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ImageFsInstaller {
    public static final byte LATEST_VERSION = 27;
    private static final String GLIBC_IMAGEFS_ARCHIVE = "imagefs-glibc2.39-r0.tzst";
    private static final String BIONIC_IMAGEFS_ARCHIVE = "imagefs_gamenative.txz";
    private static final String GLIBC_PATCH_ARCHIVE = "imagefs_patches_gamenative.tzst";
    private static final String EXTRAS_ARCHIVE = "extras.tzst";
    private static final String BIONIC_HOST_SUPPORT_ARCHIVE = "bionic_host_support.tzst";
    private static final String[][] APP_NATIVE_GUEST_LIBS = {
            {"libaero_redirect_glibc.so", "libredirect.so"},
            {"libaero_redirect_bionic.so", "libredirect-bionic.so"},
            {"libaero_android_sysvshm.so", "libandroid-sysvshm.so"},
            {"libaero_evshim.so", "libevshim.so"},
            {"libaero_fakeinput.so", "libfakeinput.so"},
            {"libdummyvk.so", "libdummyvk.so"},
            {"libadrenotools.so", "libadrenotools.so"},
            {"libnativewindow.so", "libnativewindow.so"},
            {"libc++_shared.so", "libc++_shared.so"}
    };
    private static final String[][] APP_NATIVE_ANDROID_HOST_LIBS = {
            {"libaero_redirect_bionic.so", "libredirect-bionic.so"},
            {"libaero_android_sysvshm.so", "libandroid-sysvshm.so"},
            {"libaero_evshim.so", "libevshim.so"},
            {"libdummyvk.so", "libdummyvk.so"},
            {"libadrenotools.so", "libadrenotools.so"},
            {"libnativewindow.so", "libnativewindow.so"},
            {"libc++_shared.so", "libc++_shared.so"}
    };
    private static final String[][] APP_NATIVE_GUEST_TOOLS = {
            {"libproot.so", "usr/bin/proot"},
            {"libproot-loader.so", "usr/lib/proot-loader.so"}
    };
    private static final String[] ANDROID_VULKAN_STUB_SYSTEM_LIBS = {
            "libcutils.so",
            "libhardware.so",
            "liblog.so",
            "libnativewindow.so",
            "libsync.so"
    };
    private static final String[] ANDROID_VULKAN_STUB_REQUIRED_LIBS = {
            "libc++_shared.so",
            "libcutils.so",
            "libdrm.so",
            "libhardware.so",
            "liblog.so",
            "libnativewindow.so",
            "libsync.so",
            "libz.so.1"
    };
    private static final String[] ANDROID_SYSTEM_LIB_SEARCH_DIRS = {
            "/system/lib64",
            "/system_ext/lib64",
            "/product/lib64",
            "/vendor/lib64",
            "/apex/com.android.runtime/lib64/bionic",
            "/apex/com.android.vndk.current/lib64",
            "/apex/com.android.vndk.v35/lib64",
            "/apex/com.android.vndk.v34/lib64",
            "/apex/com.android.vndk.v33/lib64",
            "/apex/com.android.vndk.v32/lib64",
            "/apex/com.android.vndk.v31/lib64",
            "/apex/com.android.vndk.v30/lib64"
    };
    private static final String[] BIONIC_HOST_SUPPORT_REQUIRED_LIBS = {
            "libX11.so",
            "libXext.so",
            "libxcb.so",
            "libxshmfence.so",
            "libandroid-support.so",
            "libfontconfig.so",
            "libfreetype.so",
            "libexpat.so.1",
            "libz.so.1",
            "libbz2.so.1.0",
            "libpng16.so",
            "libbrotlidec.so",
            "libbrotlicommon.so",
            "libvulkan.so",
            "libvulkan.so.1",
            "libdrm.so",
            "libdrm_freedreno.so",
            "libEGL.so",
            "libEGL.so.1",
            "libGL.so",
            "libGL.so.1",
            "libGLX.so.0",
            "libGLdispatch.so.0",
            "libXcursor.so",
            "libXfixes.so",
            "libXinerama.so",
            "libXss.so",
            "libXcomposite.so",
            "libXxf86vm.so",
            "wine-x11-egl-stub/libEGL.so",
            "wine-x11-egl-stub/libEGL.so.1"
    };
    private static final String[] VULKAN_MANIFEST_DIRS = {
            "usr/share/vulkan/icd.d",
            "usr/share/vulkan/implicit_layer.d",
            "usr/share/vulkan/explicit_layer.d"
    };
    private static final String ROOTFS_PRIMARY_BASE_URL = "https://downloads.gamenative.app/";
    private static final String ROOTFS_FALLBACK_BASE_URL = "https://pub-9fcd5294bd0d4b85a9d73615bf98f3b5.r2.dev/";
    private static final String GLIBC_IMAGEFS_SOURCE_URL =
            "https://github.com/Waim908/rootfs-winlator/releases/download/rootfs-glibc2.39-r0/imagefs-glibc2.39-r0.tzst";
    private static final String GLIBC_IMAGEFS_SHA256 =
            "9868bb7233720d65e7fd33c472b412668b198baa00a71f618e7e71e40d3f8290";
    private static final int DOWNLOAD_CONNECT_TIMEOUT_MS = 15000;
    private static final int DOWNLOAD_READ_TIMEOUT_MS = 30000;
    private static final int DOWNLOAD_BUFFER_SIZE = 64 * 1024;
    private static final String IMAGEFS_LIB_RUNPATH_MARKER = ".elf_runpath_sanitizer_version";
    private static final String IMAGEFS_LIB_RUNPATH_MARKER_VERSION = "2";
    private static final String[][] ROOTFS_CANONICAL_LINKS = {
            {"etc", "usr/etc"},
            {"bin", "usr/bin"},
            {"lib", "usr/lib"},
            {"lib64", "usr/lib"},
            {"var", "usr/var"},
            {"share", "usr/share"}
    };
    private static final String[] ROOTFS_CANONICAL_DIRECTORIES = {
            "tmp",
            "usr",
            "home",
            "opt",
            "dev",
            "storage"
    };
    private static final String PREFIX_PACK_ASSET_ROOT = "prefixpack";
    private static final String PREFIX_PACK_VERSION_ASSET = PREFIX_PACK_ASSET_ROOT + "/VERSION";
    private static final String PREFIX_PACK_CATALOG_ASSET = PREFIX_PACK_ASSET_ROOT + "/catalog.tsv";
    private static final String PREFIX_PACK_ROOTFS_DIR = "opt/ae/prefix-pack";
    private static final String DOWNLOAD_USER_AGENT =
            "Mozilla/5.0 (Android 14; Mobile; rv:124.0) Gecko/124.0 Ae.solator/ImageFsInstaller";

    private static void resetContainerImgVersions(Context context) {
        ContainerManager manager = new ContainerManager(context);
        for (Container container : manager.getContainers()) {
            String imgVersion = container.getExtra("imgVersion");
            String wineVersion = container.getWineVersion();
            if (!imgVersion.isEmpty() && WineInfo.isMainWineVersion(wineVersion) && Short.parseShort(imgVersion) <= 5) {
                container.putExtra("wineprefixNeedsUpdate", "t");
            }

            container.putExtra("imgVersion", null);
            container.putExtra("dxwrapper", null);
            container.putExtra("appVersion", null);
            container.saveData();
        }
    }

    private static String normalizeContainerVariant(String containerVariant) {
        return Container.GLIBC.equalsIgnoreCase(containerVariant) ? Container.GLIBC : Container.BIONIC;
    }

    private static boolean assetExists(Context context, String assetName) {
        try {
            String[] assets = context.getAssets().list("");
            return assets != null && Arrays.asList(assets).contains(assetName);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String readAssetString(Context context, String assetPath) {
        try {
            return FileUtils.readString(context, assetPath);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String resolveInstallVariant(ImageFs imageFs, Container container) {
        return resolveInstallVariant(imageFs, container, null);
    }

    private static String resolveInstallVariant(ImageFs imageFs, Container container, String requestedRuntimeModel) {
        String explicitRuntimeModel = ContentProfile.normalizeRuntimeModel(requestedRuntimeModel);
        if (!explicitRuntimeModel.isEmpty()) {
            return Container.GLIBC.equalsIgnoreCase(explicitRuntimeModel) ? Container.GLIBC : Container.BIONIC;
        }
        if (container != null) {
            String entryRuntimeModel = ContentProfile.inferRuntimeModelFromEntryName(container.getWineVersion());
            if (!entryRuntimeModel.isEmpty()) {
                return Container.GLIBC.equalsIgnoreCase(entryRuntimeModel) ? Container.GLIBC : Container.BIONIC;
            }
            return normalizeContainerVariant(container.getContainerVariant());
        }
        if (imageFs != null && !imageFs.getVariant().isEmpty()) {
            return normalizeContainerVariant(imageFs.getVariant());
        }
        return Container.DEFAULT_VARIANT;
    }

    public static boolean isInstallRequired(Context context, Container container) {
        return isInstallRequired(context, container, null);
    }

    public static boolean isInstallRequired(Context context, Container container, String requestedRuntimeModel) {
        return isInstallRequired(context, container, requestedRuntimeModel, container != null ? container.getWineVersion() : "");
    }

    public static boolean isInstallRequired(Context context, Container container, String requestedRuntimeModel, String requestedRuntimeIdentity) {
        String explicitRuntimeModel = ContentProfile.normalizeRuntimeModel(requestedRuntimeModel);
        if (explicitRuntimeModel.isEmpty() && container != null) {
            explicitRuntimeModel = ContentProfile.inferRuntimeModelFromEntryName(container.getWineVersion());
            if (explicitRuntimeModel.isEmpty()) {
                explicitRuntimeModel = ContentProfile.normalizeRuntimeModel(container.getContainerVariant());
            }
        }
        ImageFs imageFs = ImageFs.find(context, explicitRuntimeModel, requestedRuntimeIdentity);
        String requestedVariant = resolveInstallVariant(imageFs, container, requestedRuntimeModel);
        boolean universalGameNativeRootfs = imageFs.isGameNativeRootfs()
                && GLIBC_IMAGEFS_ARCHIVE.equals(BIONIC_IMAGEFS_ARCHIVE);
        boolean glibcRootfsRequired = Container.GLIBC.equalsIgnoreCase(requestedVariant)
                && imageFs.isGameNativeRootfs()
                && !GLIBC_IMAGEFS_ARCHIVE.equals(BIONIC_IMAGEFS_ARCHIVE);
        boolean variantMismatch = !universalGameNativeRootfs
                && !imageFs.getVariant().isEmpty()
                && !requestedVariant.equalsIgnoreCase(imageFs.getVariant());
        return !imageFs.isValid()
                || imageFs.getVersion() < LATEST_VERSION
                || variantMismatch
                || glibcRootfsRequired;
    }

    private static String[] getBundledWineEntries(Context context, String containerVariant) {
        int resId = Container.GLIBC.equalsIgnoreCase(containerVariant)
                ? R.array.glibc_wine_entries
                : R.array.bionic_wine_entries;
        String[] bundled = context.getResources().getStringArray(resId);
        return bundled.length > 0 ? bundled : context.getResources().getStringArray(R.array.wine_entries);
    }

    private static String resolveImageFsArchiveName(Context context, ImageFs imageFs, String containerVariant) {
        return Container.GLIBC.equalsIgnoreCase(containerVariant)
                ? GLIBC_IMAGEFS_ARCHIVE
                : BIONIC_IMAGEFS_ARCHIVE;
    }

    private static String resolveInstalledRootfsProvider(String archiveName) {
        String normalized = archiveName == null ? "" : archiveName.trim().toLowerCase(Locale.US);
        if (GLIBC_IMAGEFS_ARCHIVE.equals(archiveName)) return ImageFs.ROOTFS_PROVIDER_WAIM;
        if (normalized.contains("gamenative")) return ImageFs.ROOTFS_PROVIDER_GAMENATIVE;
        if (normalized.contains("waim")) return ImageFs.ROOTFS_PROVIDER_WAIM;
        if (normalized.contains("moze")) return ImageFs.ROOTFS_PROVIDER_MOZE;
        if (normalized.contains("rootfs")) return ImageFs.ROOTFS_PROVIDER_ROOTFS_WINLATOR;
        if (normalized.contains("community") || normalized.contains("nightly")
                || normalized.contains("alexoqool") || normalized.contains("xnick")) {
            return ImageFs.ROOTFS_PROVIDER_COMMUNITY;
        }
        return ImageFs.ROOTFS_PROVIDER_CUSTOM;
    }

    private static String resolveInstalledRootfsLayout(String archiveName) {
        String normalized = archiveName == null ? "" : archiveName.trim().toLowerCase(Locale.US);
        if (normalized.contains("imagefs")) return ImageFs.ROOTFS_LAYOUT_IMAGEFS;
        if (normalized.contains("ubuntufs") || normalized.contains("ubuntu")) return ImageFs.ROOTFS_LAYOUT_UBUNTUFS;
        return ImageFs.ROOTFS_LAYOUT_CUSTOM;
    }

    private static void ensureDirectory(File directory, int mode) {
        if (directory == null) return;
        if (!directory.exists()) directory.mkdirs();
        if (directory.exists()) FileUtils.chmod(directory, mode);
    }

    private static void ensureSymlinkOrDirectory(String linkTarget, File linkFile, int mode) {
        if (linkFile == null) return;
        if (isSelfReferentialSymlink(linkFile)) {
            linkFile.delete();
        }
        if (linkFile.exists() && !Files.isSymbolicLink(linkFile.toPath())) return;
        if (Files.isSymbolicLink(linkFile.toPath())) {
            String currentTarget = FileUtils.readSymlink(linkFile);
            if (sameSymlinkTarget(linkFile, linkTarget, currentTarget)) return;
            linkFile.delete();
        }
        if (isSelfTarget(linkFile, linkTarget)) {
            ensureDirectory(linkFile, mode);
            return;
        }
        File parent = linkFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        FileUtils.symlink(linkTarget, linkFile.getAbsolutePath());
        if (!linkFile.exists() && !Files.isSymbolicLink(linkFile.toPath())) ensureDirectory(linkFile, mode);
    }

    private static void ensureHybridRootfsLayout(ImageFs imageFs, File rootDir) {
        ensureDirectory(imageFs.getTmpDir(), 0771);
        ensureDirectory(imageFs.getCompatTmpDir().getParentFile(), 0771);
        ensureSymlinkOrDirectory("../tmp", imageFs.getCompatTmpDir(), 0771);
        ensureSymlinkOrDirectory("usr/etc", new File(rootDir, "etc"), 0771);
        ensureSymlinkOrDirectory("usr/bin", new File(rootDir, "bin"), 0771);
        ensureSymlinkOrDirectory("usr/lib", new File(rootDir, "lib"), 0771);
        ensureSymlinkOrDirectory("usr/lib", new File(rootDir, "lib64"), 0771);
        ensureDirectory(imageFs.getLocalBinDir(), 0771);
        ensureDirectory(imageFs.getAndroidHostLibDir(), 0771);

        File canonicalWine = new File(rootDir, "opt/wine");
        File mainWineVersionDir = new File(rootDir, "opt/" + WineInfo.MAIN_WINE_VERSION.identifier());
        if (!canonicalWine.exists() && mainWineVersionDir.isDirectory()) {
            FileUtils.symlink(mainWineVersionDir.getAbsolutePath(), canonicalWine.getAbsolutePath());
        }

        File bindirBox64 = new File(imageFs.getBinDir(), "box64");
        File localBinBox64 = new File(imageFs.getLocalBinDir(), "box64");
        if (localBinBox64.isFile() && !bindirBox64.exists()) {
            FileUtils.symlink(localBinBox64.getAbsolutePath(), bindirBox64.getAbsolutePath());
        } else if (bindirBox64.isFile() && !localBinBox64.exists()) {
            FileUtils.symlink(bindirBox64.getAbsolutePath(), localBinBox64.getAbsolutePath());
        }
    }

    public static void ensureRootfsLaunchLayout(Context context, ImageFs imageFs) {
        if (imageFs == null) return;
        File rootDir = imageFs.getRootDir();
        if (rootDir == null || !rootDir.isDirectory()) return;

        int repaired = 0;
        int failed = 0;
        ArrayList<String> repairedPaths = new ArrayList<>();
        ArrayList<String> failedPaths = new ArrayList<>();

        for (String name : ROOTFS_CANONICAL_DIRECTORIES) {
            RepairResult result = ensureRootfsDirectory(context, rootDir, name);
            if (result.repaired) {
                repaired++;
                repairedPaths.add(result.summary);
            }
            if (result.failed) {
                failed++;
                failedPaths.add(result.summary);
            }
        }

        for (String[] spec : ROOTFS_CANONICAL_LINKS) {
            if (spec == null || spec.length < 2) continue;
            RepairResult result = ensureRootfsAliasLink(context, rootDir, spec[0], spec[1]);
            if (result.repaired) {
                repaired++;
                repairedPaths.add(result.summary);
            }
            if (result.failed) {
                failed++;
                failedPaths.add(result.summary);
            }
        }

        ensureDirectory(new File(rootDir, "usr"), 0771);
        ensureSymlinkOrDirectory("../tmp", new File(rootDir, "usr/tmp"), 0771);
        ensureWinePrefixPrivatePermissions(context, imageFs);

        ForensicLogger.logEvent(
                context,
                failed == 0 ? "info" : "warn",
                "ROOTFS_LAUNCH_LAYOUT_CLOSURE",
                null,
                "rootfs",
                failed == 0 ? "rootfs_launch_layout_ready" : "rootfs_launch_layout_incomplete",
                ForensicLogger.fields(
                        "root_dir", rootDir.getAbsolutePath(),
                        "repaired_count", repaired,
                        "failed_count", failed,
                        "repaired", summarizeStrings(repairedPaths, 16),
                        "failed", summarizeStrings(failedPaths, 16),
                        "layout", imageFs.getRootfsLayout(),
                        "provider", imageFs.getRootfsProvider()
                )
        );
    }

    public static void ensureWinePrefixPrivatePermissions(Context context, ImageFs imageFs) {
        if (imageFs == null) return;
        ensureWinePrefixPrivatePermissions(context, imageFs.getRootDir(), imageFs.getHomeDir());
    }

    public static void ensureWinePrefixPrivatePermissions(Context context, File rootDir) {
        ensureWinePrefixPrivatePermissions(context, rootDir, null);
    }

    private static void ensureWinePrefixPrivatePermissions(Context context, File rootDir, File activeHomeDir) {
        if (rootDir == null || !rootDir.isDirectory()) return;

        ArrayList<File> prefixes = collectWinePrefixes(rootDir, activeHomeDir);
        int privateDirs = 0;
        int privateFiles = 0;
        int failed = 0;
        ArrayList<String> touched = new ArrayList<>();
        ArrayList<String> failures = new ArrayList<>();

        for (File prefix : prefixes) {
            PermissionRepair repair = ensureWinePrefixPrivate(prefix);
            privateDirs += repair.directories;
            privateFiles += repair.files;
            failed += repair.failures;
            if (repair.changed) touched.add(prefix.getPath());
            if (!repair.failureSummary.isEmpty()) failures.add(repair.failureSummary);
        }

        ForensicLogger.logEvent(
                context,
                failed == 0 ? "info" : "warn",
                "WINEPREFIX_PRIVACY_CLOSURE",
                null,
                "rootfs",
                failed == 0 ? "wineprefix_privacy_ready" : "wineprefix_privacy_incomplete",
                ForensicLogger.fields(
                        "root_dir", rootDir.getAbsolutePath(),
                        "prefix_count", prefixes.size(),
                        "private_dir_count", privateDirs,
                        "private_file_count", privateFiles,
                        "failed_count", failed,
                        "touched", summarizeStrings(touched, 12),
                        "failed", summarizeStrings(failures, 12)
                )
        );
    }

    private static ArrayList<File> collectWinePrefixes(File rootDir, File activeHomeDir) {
        ArrayList<File> result = new ArrayList<>();
        ArrayList<String> seen = new ArrayList<>();
        addWinePrefixCandidate(result, seen, new File(rootDir, ".wine"));
        if (activeHomeDir != null) addWinePrefixCandidate(result, seen, new File(activeHomeDir, ".wine"));

        File homeRoot = new File(rootDir, "home");
        File[] homes = homeRoot.listFiles();
        if (homes != null) {
            for (File home : homes) {
                if (home == null) continue;
                String name = home.getName();
                if (ImageFs.USER.equals(name) || name.startsWith(ImageFs.USER + "-")) {
                    addWinePrefixCandidate(result, seen, new File(home, ".wine"));
                }
            }
        }
        return result;
    }

    private static void addWinePrefixCandidate(ArrayList<File> result, ArrayList<String> seen, File prefix) {
        if (prefix == null || !prefix.exists() || !prefix.isDirectory()) return;
        String key;
        try {
            key = prefix.getCanonicalPath();
        } catch (Exception ignored) {
            key = prefix.getAbsolutePath();
        }
        if (seen.contains(key)) return;
        seen.add(key);
        result.add(prefix);
    }

    private static PermissionRepair ensureWinePrefixPrivate(File prefix) {
        PermissionRepair repair = new PermissionRepair();
        if (prefix == null || !prefix.isDirectory()) return repair;

        repair.merge(chmodPrivateDirectory(prefix.getParentFile(), 0700));
        repair.merge(chmodPrivateDirectory(prefix, 0700));
        repair.merge(chmodPrivateTree(new File(prefix, ".wineserver")));
        repair.merge(chmodPrivateFile(new File(prefix, ".update-timestamp"), 0600));
        return repair;
    }

    private static PermissionRepair chmodPrivateTree(File file) {
        PermissionRepair repair = new PermissionRepair();
        if (file == null || !file.exists()) return repair;
        if (Files.isSymbolicLink(file.toPath())) return repair;

        if (file.isDirectory()) {
            repair.merge(chmodPrivateDirectory(file, 0700));
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) repair.merge(chmodPrivateTree(child));
            }
            return repair;
        }
        return chmodPrivateFile(file, 0600);
    }

    private static PermissionRepair chmodPrivateDirectory(File directory, int mode) {
        PermissionRepair repair = new PermissionRepair();
        if (directory == null || !directory.exists()) return repair;
        if (!directory.isDirectory() || Files.isSymbolicLink(directory.toPath())) return repair;
        try {
            FileUtils.chmod(directory, mode);
            repair.directories++;
            repair.changed = true;
        } catch (Throwable error) {
            repair.failures++;
            repair.failureSummary = directory.getPath() + ":" + error.getClass().getSimpleName();
        }
        return repair;
    }

    private static PermissionRepair chmodPrivateFile(File file, int mode) {
        PermissionRepair repair = new PermissionRepair();
        if (file == null || !file.exists()) return repair;
        if (!file.isFile() || Files.isSymbolicLink(file.toPath())) return repair;
        try {
            FileUtils.chmod(file, mode);
            repair.files++;
            repair.changed = true;
        } catch (Throwable error) {
            repair.failures++;
            repair.failureSummary = file.getPath() + ":" + error.getClass().getSimpleName();
        }
        return repair;
    }

    private static RepairResult ensureRootfsDirectory(Context context, File rootDir, String name) {
        File directory = new File(rootDir, name);
        boolean symlink = Files.isSymbolicLink(directory.toPath());
        boolean existed = directory.exists() || symlink;
        String linkTarget = symlink ? FileUtils.readSymlink(directory) : "";
        try {
            if (symlink || (directory.exists() && !directory.isDirectory())) {
                if (!directory.delete()) {
                    logRootfsAliasRepair(context, false, directory, name, linkTarget, "delete_failed");
                    return RepairResult.failed(name + ":delete_failed");
                }
            }
            if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory()) {
                logRootfsAliasRepair(context, false, directory, name, linkTarget, "mkdirs_failed");
                return RepairResult.failed(name + ":mkdirs_failed");
            }
            FileUtils.chmod(directory, 0771);
            if (symlink || !existed) {
                String action = symlink ? "replaced_symlink_with_directory" : "created_directory";
                logRootfsAliasRepair(context, true, directory, name, linkTarget, action);
                return RepairResult.repaired(name + ":" + action);
            }
            return RepairResult.clean();
        } catch (Throwable error) {
            logRootfsAliasRepair(context, false, directory, name, linkTarget, error.getClass().getSimpleName());
            return RepairResult.failed(name + ":" + error.getClass().getSimpleName());
        }
    }

    private static RepairResult ensureRootfsAliasLink(Context context, File rootDir, String name, String targetRelativePath) {
        File link = new File(rootDir, name);
        File target = new File(rootDir, targetRelativePath);
        ensureDirectory(target, 0771);

        boolean symlink = Files.isSymbolicLink(link.toPath());
        boolean existed = link.exists() || symlink;
        String linkTarget = symlink ? FileUtils.readSymlink(link) : "";
        try {
            if (symlink && sameSymlinkTarget(link, targetRelativePath, linkTarget) && !isSelfReferentialSymlink(link)) {
                return RepairResult.clean();
            }
            if (link.exists() && link.isDirectory() && !symlink) {
                return RepairResult.clean();
            }
            if (existed && !link.delete()) {
                logRootfsAliasRepair(context, false, link, name, linkTarget, "delete_failed");
                return RepairResult.failed(name + ":delete_failed");
            }

            File parent = link.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!FileUtils.symlink(targetRelativePath, link.getAbsolutePath())) {
                ensureDirectory(link, 0771);
                if (!link.isDirectory()) {
                    logRootfsAliasRepair(context, false, link, name, linkTarget, "symlink_and_directory_fallback_failed");
                    return RepairResult.failed(name + ":symlink_failed");
                }
            }

            String action = symlink
                    ? "normalized_symlink"
                    : (existed ? "replaced_non_directory_with_symlink" : "created_symlink");
            logRootfsAliasRepair(context, true, link, name, linkTarget, action + "->" + targetRelativePath);
            return RepairResult.repaired(name + ":" + action);
        } catch (Throwable error) {
            logRootfsAliasRepair(context, false, link, name, linkTarget, error.getClass().getSimpleName());
            return RepairResult.failed(name + ":" + error.getClass().getSimpleName());
        }
    }

    private static boolean isSelfReferentialSymlink(File link) {
        if (link == null || !Files.isSymbolicLink(link.toPath())) return false;
        return isSelfTarget(link, FileUtils.readSymlink(link));
    }

    private static boolean isSelfTarget(File link, String linkTarget) {
        if (link == null || linkTarget == null || linkTarget.trim().isEmpty()) return false;
        File targetFile = new File(linkTarget);
        File resolved = targetFile.isAbsolute()
                ? targetFile
                : new File(link.getParentFile(), linkTarget);
        return link.getAbsolutePath().equals(resolved.getAbsolutePath());
    }

    private static boolean sameSymlinkTarget(File link, String expectedTarget, String actualTarget) {
        if (expectedTarget == null || actualTarget == null) return false;
        if (expectedTarget.equals(actualTarget)) return true;
        if (link == null || isSelfTarget(link, actualTarget)) return false;
        try {
            File expected = new File(expectedTarget);
            if (!expected.isAbsolute()) expected = new File(link.getParentFile(), expectedTarget);
            File actual = new File(actualTarget);
            if (!actual.isAbsolute()) actual = new File(link.getParentFile(), actualTarget);
            return expected.getCanonicalPath().equals(actual.getCanonicalPath());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void logRootfsAliasRepair(Context context, boolean success, File path, String alias,
                                             String previousTarget, String action) {
        ForensicLogger.logEvent(
                context,
                success ? "info" : "warn",
                success ? "ROOTFS_ALIAS_REPAIRED" : "ROOTFS_ALIAS_REPAIR_FAILED",
                null,
                "rootfs",
                "rootfs_alias_repair",
                ForensicLogger.fields(
                        "path", path != null ? path.getAbsolutePath() : "",
                        "alias", alias != null ? alias : "",
                        "previous_target", previousTarget != null ? previousTarget : "",
                        "action", action != null ? action : ""
                )
        );
    }

    private static String summarizeStrings(List<String> values, int limit) {
        if (values == null || values.isEmpty() || limit <= 0) return "";
        ArrayList<String> out = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            out.add(value.trim());
            if (out.size() >= limit) break;
        }
        return String.join(" | ", out);
    }

    private static final class RepairResult {
        final boolean repaired;
        final boolean failed;
        final String summary;

        private RepairResult(boolean repaired, boolean failed, String summary) {
            this.repaired = repaired;
            this.failed = failed;
            this.summary = summary == null ? "" : summary;
        }

        static RepairResult clean() {
            return new RepairResult(false, false, "");
        }

        static RepairResult repaired(String summary) {
            return new RepairResult(true, false, summary);
        }

        static RepairResult failed(String summary) {
            return new RepairResult(false, true, summary);
        }
    }

    private static final class PermissionRepair {
        int directories;
        int files;
        int failures;
        boolean changed;
        String failureSummary = "";

        void merge(PermissionRepair other) {
            if (other == null) return;
            directories += other.directories;
            files += other.files;
            failures += other.failures;
            changed = changed || other.changed;
            if (!other.failureSummary.isEmpty()) {
                if (!failureSummary.isEmpty()) failureSummary += " | ";
                failureSummary += other.failureSummary;
            }
        }
    }

    private static boolean isRemoteDeliverableArchive(String archiveName) {
        return GLIBC_IMAGEFS_ARCHIVE.equals(archiveName)
                || BIONIC_IMAGEFS_ARCHIVE.equals(archiveName)
                || GLIBC_PATCH_ARCHIVE.equals(archiveName);
    }

    private static String[] resolveRemoteArchiveUrls(String archiveName) {
        if (GLIBC_IMAGEFS_ARCHIVE.equals(archiveName)) {
            return new String[] {GLIBC_IMAGEFS_SOURCE_URL};
        }
        return new String[] {
                ROOTFS_PRIMARY_BASE_URL + archiveName,
                ROOTFS_FALLBACK_BASE_URL + archiveName
        };
    }

    private static String resolveRemoteArchiveSha256(String archiveName) {
        return GLIBC_IMAGEFS_ARCHIVE.equals(archiveName) ? GLIBC_IMAGEFS_SHA256 : "";
    }

    private static TarCompressorUtils.Type resolveArchiveType(String archiveName) {
        String normalized = archiveName == null ? "" : archiveName.trim().toLowerCase(Locale.US);
        return normalized.endsWith(".tzst") || normalized.endsWith(".zst") || normalized.endsWith(".tar.zst")
                ? TarCompressorUtils.Type.ZSTD
                : TarCompressorUtils.Type.XZ;
    }

    private static File resolveDownloadedArchive(ImageFs imageFs, String archiveName) {
        return new File(imageFs.getFilesDir(), archiveName);
    }

    private static boolean fetchArchiveWithFallback(
            ImageFs imageFs,
            String archiveName,
            Callback<Integer> onProgress,
            int progressStart,
            int progressSpan
    ) {
        if (archiveName == null || archiveName.trim().isEmpty() || !isRemoteDeliverableArchive(archiveName)) {
            return false;
        }

        File destination = resolveDownloadedArchive(imageFs, archiveName);
        destination.getParentFile().mkdirs();
        String[] urls = resolveRemoteArchiveUrls(archiveName);
        for (int i = 0; i < urls.length; i++) {
            String url = urls[i];
            if (i > 0) {
                Log.w("ImageFsInstaller", "Download failed for " + archiveName + ", retrying with secondary path");
            }
            if (downloadFile(url, destination, onProgress, progressStart, progressSpan)
                    && verifyDownloadedArchive(archiveName, destination)) {
                return true;
            }
        }

        safeDelete(destination);
        return false;
    }

    private static boolean downloadFile(
            String address,
            File destination,
            Callback<Integer> onProgress,
            int progressStart,
            int progressSpan
    ) {
        HttpURLConnection connection = null;
        InputStream input = null;
        OutputStream output = null;
        File partFile = new File(destination.getAbsolutePath() + ".download");
        try {
            safeDelete(partFile);
            connection = (HttpURLConnection) (new URL(address)).openConnection();
            connection.setConnectTimeout(DOWNLOAD_CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(DOWNLOAD_READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", DOWNLOAD_USER_AGENT);
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty("Connection", "close");

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                Log.w("ImageFsInstaller", "HTTP " + responseCode + " while downloading " + address);
                return false;
            }

            long contentLength = connection.getContentLengthLong();
            long totalRead = 0L;
            input = connection.getInputStream();
            output = new FileOutputStream(partFile);
            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            if (onProgress != null && progressSpan > 0) {
                onProgress.call(Math.min(100, progressStart));
            }
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                if (bytesRead == 0) continue;
                output.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                if (onProgress != null && progressSpan > 0 && contentLength > 0L) {
                    int delta = (int) Math.min(progressSpan, (totalRead * progressSpan) / contentLength);
                    onProgress.call(Math.min(100, progressStart + delta));
                }
            }
            output.flush();

            if (contentLength > 0L && totalRead < contentLength) {
                Log.w("ImageFsInstaller", "Short read while downloading " + address + ": " + totalRead + "/" + contentLength);
                safeDelete(partFile);
                return false;
            }

            if (destination.exists() && !destination.delete()) {
                safeDelete(partFile);
                return false;
            }
            if (!partFile.renameTo(destination)) {
                copyFile(partFile, destination);
                safeDelete(partFile);
            }
            if (onProgress != null && progressSpan > 0) {
                onProgress.call(Math.min(100, progressStart + progressSpan));
            }
            return destination.isFile() && destination.length() > 0L;
        } catch (Exception e) {
            Log.w("ImageFsInstaller", "Download failed for " + address, e);
            safeDelete(partFile);
            return false;
        } finally {
            closeQuietly(output);
            closeQuietly(input);
            if (connection != null) connection.disconnect();
        }
    }

    private static void copyFile(File source, File destination) throws Exception {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(destination);
            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            output.flush();
        } finally {
            closeQuietly(output);
            closeQuietly(input);
        }
    }

    private static void closeQuietly(OutputStream output) {
        if (output == null) return;
        try {
            output.close();
        } catch (Exception ignored) {
        }
    }

    private static void closeQuietly(InputStream input) {
        if (input == null) return;
        try {
            input.close();
        } catch (Exception ignored) {
        }
    }

    private static void safeDelete(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private static boolean verifyDownloadedArchive(String archiveName, File archive) {
        String expected = resolveRemoteArchiveSha256(archiveName);
        if (expected.isEmpty()) return true;
        String actual = sha256(archive);
        if (expected.equalsIgnoreCase(actual)) return true;
        Log.e("ImageFsInstaller", "Archive sha256 mismatch for " + archiveName + ": expected=" + expected + " actual=" + actual);
        safeDelete(archive);
        return false;
    }

    private static String sha256(File file) {
        if (file == null || !file.isFile()) return "";
        try (InputStream input = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (read > 0) digest.update(buffer, 0, read);
            }
            byte[] bytes = digest.digest();
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) out.append(String.format(Locale.US, "%02x", value & 0xff));
            return out.toString();
        } catch (Exception e) {
            Log.e("ImageFsInstaller", "Unable to compute archive sha256 for " + file, e);
            return "";
        }
    }

    private static boolean ensureArchiveAvailable(
            Context context,
            ImageFs imageFs,
            String archiveName,
            Callback<Integer> onProgress,
            int progressStart,
            int progressSpan
    ) {
        if (assetExists(context, archiveName)) return true;
        File downloaded = resolveDownloadedArchive(imageFs, archiveName);
        if (downloaded.isFile() && downloaded.length() > 0L && verifyDownloadedArchive(archiveName, downloaded)) return true;
        return fetchArchiveWithFallback(imageFs, archiveName, onProgress, progressStart, progressSpan);
    }

    private static boolean isArchiveAvailable(Context context, ImageFs imageFs, String archiveName) {
        if (assetExists(context, archiveName)) return true;
        File downloaded = resolveDownloadedArchive(imageFs, archiveName);
        return downloaded.isFile() && downloaded.length() > 0L && verifyDownloadedArchive(archiveName, downloaded);
    }

    private static boolean prefetchVariantRuntimeSupportIfMissing(
            Context context,
            ImageFs imageFs,
            String containerVariant,
            Callback<Integer> onProgress
    ) {
        if (!Container.GLIBC.equalsIgnoreCase(containerVariant)) return false;
        if (isArchiveAvailable(context, imageFs, GLIBC_PATCH_ARCHIVE)) return false;
        return fetchArchiveWithFallback(imageFs, GLIBC_PATCH_ARCHIVE, onProgress, 35, 10);
    }

    public static void installWineFromAssets(final MainActivity activity) {
        installWineFromAssets(activity, resolveInstallVariant(ImageFs.find(activity), null));
    }

    public static void installWineFromAssets(final Context context, String containerVariant) {
        installWineFromAssets(context, containerVariant, ImageFs.find(context), "");
    }

    private static void installWineFromAssets(final Context context, String containerVariant, ImageFs imageFs, String requestedRuntimeIdentity) {
        String[] versions = getBundledWineEntries(context, containerVariant);
        File rootDir = imageFs.getRootDir();
        for (String version : versions) {
            if (!shouldInstallBundledWineEntry(version, requestedRuntimeIdentity)) continue;
            if (!assetExists(context, version + ".txz")) continue;
            File outFile = new File(rootDir, "opt/" + version);
            outFile.mkdirs();
            TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, context, version + ".txz", outFile);
        }
    }

    public static void installWineFromDownloads(final Context context, String containerVariant) {
        installWineFromDownloads(context, containerVariant, ImageFs.find(context), "");
    }

    private static void installWineFromDownloads(final Context context, String containerVariant, ImageFs imageFs, String requestedRuntimeIdentity) {
        String[] versions = getBundledWineEntries(context, containerVariant);
        File rootDir = imageFs.getRootDir();
        for (String version : versions) {
            if (!shouldInstallBundledWineEntry(version, requestedRuntimeIdentity)) continue;
            File downloaded = new File(imageFs.getFilesDir(), version + ".txz");
            if (!downloaded.isFile()) continue;
            File outFile = new File(rootDir, "opt/" + version);
            outFile.mkdirs();
            TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, downloaded, outFile);
        }
    }

    private static boolean shouldInstallBundledWineEntry(String bundledVersion, String requestedRuntimeIdentity) {
        String requested = ImageFs.normalizeRuntimeIdentity(requestedRuntimeIdentity);
        if (requested.isEmpty()) return true;
        String bundled = ImageFs.normalizeRuntimeIdentity(bundledVersion);
        return requested.equals(bundled);
    }

    public static void installDriversFromAssets(final MainActivity activity) {
        AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(activity);
        adrenotoolsManager.extractBundledDriverResources();
    }

    private static void chmodIfExists(File file) {
        if (file != null && file.exists()) {
            FileUtils.chmod(file, 0755);
        }
    }

    private static void chmodTree(File root, int mode) {
        if (root == null || !root.exists()) return;
        FileUtils.chmod(root, mode);
        if (!root.isDirectory()) return;
        File[] children = root.listFiles();
        if (children == null) return;
        for (File child : children) {
            chmodTree(child, mode);
        }
    }

    private static void installAppNativeGuestLibs(Context context, File rootDir) {
        String nativeLibDir = AppUtils.getNativeLibDir(context);
        if (nativeLibDir == null || nativeLibDir.trim().isEmpty()) return;

        File nativeLibRoot = new File(nativeLibDir);
        if (!nativeLibRoot.isDirectory()) return;

        File guestLibDir = new File(rootDir, "usr/lib");
        if (!guestLibDir.exists() && !guestLibDir.mkdirs()) return;
        File androidHostLibDir = new File(rootDir, "usr/lib/android-host");
        ensureDirectory(androidHostLibDir, 0771);

        for (String[] librarySpec : APP_NATIVE_GUEST_LIBS) {
            if (librarySpec == null || librarySpec.length < 2) continue;

            String sourceName = librarySpec[0];
            String destinationName = librarySpec[1];
            File source = new File(nativeLibRoot, sourceName);
            if (!source.isFile()) continue;

            File destination = new File(guestLibDir, destinationName);
            copyIfStale(source, destination);
            chmodIfExists(destination);
        }

        for (String[] librarySpec : APP_NATIVE_ANDROID_HOST_LIBS) {
            if (librarySpec == null || librarySpec.length < 2) continue;

            String sourceName = librarySpec[0];
            String destinationName = librarySpec[1];
            File source = new File(nativeLibRoot, sourceName);
            if (!source.isFile()) continue;

            File destination = new File(androidHostLibDir, destinationName);
            copyIfStale(source, destination);
            chmodIfExists(destination);
        }

        for (String[] toolSpec : APP_NATIVE_GUEST_TOOLS) {
            if (toolSpec == null || toolSpec.length < 2) continue;

            File source = new File(nativeLibRoot, toolSpec[0]);
            if (!source.isFile()) continue;

            File destination = new File(rootDir, toolSpec[1]);
            File parent = destination.getParentFile();
            if (parent != null) ensureDirectory(parent, 0771);
            copyIfStale(source, destination);
            chmodIfExists(destination);
        }

        ensureAndroidVulkanStubClosure(context, rootDir);
    }

    private static void ensureAndroidVulkanStubClosure(Context context, ImageFs imageFs) {
        if (imageFs == null) return;
        ensureAndroidVulkanStubClosure(context, imageFs.getRootDir());
    }

    private static void ensureAndroidVulkanStubClosure(Context context, File rootDir) {
        if (context == null || rootDir == null || !rootDir.isDirectory()) return;
        long startedAt = System.currentTimeMillis();
        ArrayList<File> stubDirs = resolveAndroidVulkanStubDirs(rootDir);
        StringBuilder stubDirList = new StringBuilder();
        StringBuilder missingByDir = new StringBuilder();

        for (File stubDir : stubDirs) {
            String missing = materializeAndroidVulkanStubDir(context, rootDir, stubDir);
            if (stubDirList.length() > 0) stubDirList.append(',');
            stubDirList.append(stubDir.getAbsolutePath());
            if (!missing.isEmpty()) {
                if (missingByDir.length() > 0) missingByDir.append(';');
                missingByDir.append(stubDir.getAbsolutePath()).append('=').append(missing);
            }
        }

        ForensicLogger.logEvent(
                context,
                missingByDir.length() == 0 ? "info" : "warn",
                "ANDROID_VULKAN_STUB_CLOSURE",
                null,
                "rootfs",
                missingByDir.length() == 0 ? "android_vulkan_stub_closure_ready" : "android_vulkan_stub_closure_incomplete",
                ForensicLogger.fields(
                        "stub_dirs", stubDirList.toString(),
                        "stub_dir_count", stubDirs.size(),
                        "required_libs", String.join(",", ANDROID_VULKAN_STUB_REQUIRED_LIBS),
                        "missing_by_dir", missingByDir.toString(),
                        "chmod_scope", "required_stub_files_only",
                        "elapsed_ms", System.currentTimeMillis() - startedAt
                )
        );
    }

    private static String materializeAndroidVulkanStubDir(Context context, File rootDir, File stubDir) {
        if (context == null || rootDir == null || stubDir == null) return "";
        ensureDirectory(stubDir, 0771);
        File hostLibDir = new File(rootDir, "usr/lib/android-host");
        String nativeLibDir = AppUtils.getNativeLibDir(context);
        File nativeLibRoot = nativeLibDir == null || nativeLibDir.trim().isEmpty()
                ? null
                : new File(nativeLibDir);

        copyFirstExisting(
                stubDir,
                "libc++_shared.so",
                new File(hostLibDir, "libc++_shared.so"),
                nativeLibRoot == null ? null : new File(nativeLibRoot, "libc++_shared.so")
        );
        copyFirstExisting(
                stubDir,
                "libdrm.so",
                new File(hostLibDir, "libdrm.so"),
                nativeLibRoot == null ? null : new File(nativeLibRoot, "libdrm.so")
        );
        copyFirstExisting(
                stubDir,
                "libz.so.1",
                new File(hostLibDir, "libz.so.1.3.2"),
                new File(hostLibDir, "libz.so.1"),
                findAndroidSystemLibrary("libz.so")
        );
        for (String libraryName : ANDROID_VULKAN_STUB_SYSTEM_LIBS) {
            copyFirstExisting(
                    stubDir,
                    libraryName,
                    findAndroidSystemLibrary(libraryName),
                    new File(hostLibDir, libraryName),
                    nativeLibRoot == null ? null : new File(nativeLibRoot, libraryName)
            );
        }
        chmodAndroidVulkanStubFiles(stubDir);
        return collectMissingFiles(stubDir, ANDROID_VULKAN_STUB_REQUIRED_LIBS);
    }

    private static void chmodAndroidVulkanStubFiles(File stubDir) {
        if (stubDir == null || !stubDir.isDirectory()) return;
        FileUtils.chmod(stubDir, 0755);
        for (String libraryName : ANDROID_VULKAN_STUB_REQUIRED_LIBS) {
            if (libraryName == null || libraryName.trim().isEmpty()) continue;
            chmodIfExists(new File(stubDir, libraryName));
        }
    }

    private static ArrayList<File> resolveAndroidVulkanStubDirs(File rootDir) {
        ArrayList<File> dirs = new ArrayList<>();
        addUniqueFile(dirs, new File(rootDir, "android_stub"));
        addUniqueFile(dirs, new File(rootDir, "opt/android_stub"));
        File optDir = new File(rootDir, "opt");
        File[] optChildren = optDir.listFiles();
        if (optChildren != null) {
            for (File child : optChildren) {
                if (child == null || !child.isDirectory()) continue;
                if (!child.getName().startsWith("runtime-")) continue;
                addUniqueFile(dirs, new File(child, "android_stub"));
                addUniqueFile(dirs, new File(child, "lib"));
                addUniqueFile(dirs, new File(child, "arm64-v8a/lib"));
            }
        }
        return dirs;
    }

    private static void addUniqueFile(ArrayList<File> files, File file) {
        if (files == null || file == null) return;
        String path = canonicalPath(file);
        for (File existing : files) {
            if (existing != null && path.equals(canonicalPath(existing))) return;
        }
        files.add(file);
    }

    private static String canonicalPath(File file) {
        if (file == null) return "";
        try {
            return file.getCanonicalPath();
        } catch (Exception ignored) {
            return file.getAbsolutePath();
        }
    }

    private static boolean copyFirstExisting(File destinationDir, String destinationName, File... candidates) {
        if (destinationDir == null || destinationName == null || destinationName.trim().isEmpty()) return false;
        if (!destinationDir.isDirectory() && !destinationDir.mkdirs()) return false;
        if (candidates == null) return false;
        for (File candidate : candidates) {
            File source = resolveRegularFile(candidate);
            if (source == null || !source.isFile()) continue;
            File destination = new File(destinationDir, destinationName);
            copyIfStale(source, destination);
            chmodIfExists(destination);
            return destination.isFile();
        }
        return false;
    }

    private static boolean needsCopyFromSource(File source, File destination) {
        if (source == null || destination == null || !source.isFile()) return false;
        if (!destination.isFile()) return true;
        if (source.length() != destination.length()) return true;
        long sourceModified = source.lastModified();
        long destinationModified = destination.lastModified();
        return sourceModified > 0L && destinationModified > 0L && destinationModified < sourceModified;
    }

    private static void copyIfStale(File source, File destination) {
        if (source == null || destination == null || !source.isFile()) return;
        if (!needsCopyFromSource(source, destination)) return;
        File parent = destination.getParentFile();
        if (parent != null) ensureDirectory(parent, 0771);
        FileUtils.copy(source, destination);
        long sourceModified = source.lastModified();
        if (sourceModified > 0L && destination.isFile()) destination.setLastModified(sourceModified);
    }

    private static File resolveRegularFile(File candidate) {
        if (candidate == null || !candidate.exists()) return null;
        try {
            File canonical = candidate.getCanonicalFile();
            if (canonical != null && canonical.isFile()) return canonical;
        } catch (Exception ignored) {
        }
        return candidate.isFile() ? candidate : null;
    }

    private static File findAndroidSystemLibrary(String libraryName) {
        if (libraryName == null || libraryName.trim().isEmpty()) return null;
        for (String dir : ANDROID_SYSTEM_LIB_SEARCH_DIRS) {
            File candidate = new File(dir, libraryName);
            if (candidate.isFile()) return candidate;
        }
        return null;
    }

    private static String collectMissingFiles(File directory, String[] names) {
        if (directory == null || names == null) return "";
        StringBuilder missing = new StringBuilder();
        for (String name : names) {
            if (name == null || name.trim().isEmpty()) continue;
            if (new File(directory, name).isFile()) continue;
            if (missing.length() > 0) missing.append(',');
            missing.append(name);
        }
        return missing.toString();
    }

    private static String collectMissingFilesByDir(ArrayList<File> directories, String[] names) {
        if (directories == null || directories.isEmpty()) return "";
        StringBuilder missing = new StringBuilder();
        for (File directory : directories) {
            String dirMissing = collectMissingFiles(directory, names);
            if (dirMissing.isEmpty()) continue;
            if (missing.length() > 0) missing.append(';');
            missing.append(directory.getAbsolutePath()).append('=').append(dirMissing);
        }
        return missing.toString();
    }

    private static String joinFilePaths(ArrayList<File> files) {
        if (files == null || files.isEmpty()) return "";
        StringBuilder joined = new StringBuilder();
        for (File file : files) {
            if (file == null) continue;
            if (joined.length() > 0) joined.append(',');
            joined.append(file.getAbsolutePath());
        }
        return joined.toString();
    }

    private static void installGuestLibs(Context context, File rootDir) {
        if (assetExists(context, EXTRAS_ARCHIVE)) {
            if (!TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, EXTRAS_ARCHIVE, rootDir)) {
                Log.e("ImageFsInstaller", "extras overlay deploy failed");
            }
        }

        if (assetExists(context, BIONIC_HOST_SUPPORT_ARCHIVE)) {
            File androidHostLibDir = new File(rootDir, "usr/lib/android-host");
            if (androidHostLibDir.exists()) FileUtils.delete(androidHostLibDir);
            ensureDirectory(androidHostLibDir, 0771);
            if (!TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, BIONIC_HOST_SUPPORT_ARCHIVE, rootDir)) {
                Log.e("ImageFsInstaller", "bionic host support overlay deploy failed");
            } else {
                chmodTree(androidHostLibDir, 0755);
            }
        }

        installAppNativeGuestLibs(context, rootDir);

        chmodIfExists(new File(rootDir, "usr/lib/libredirect.so"));
        chmodIfExists(new File(rootDir, "usr/lib/libredirect-bionic.so"));
        chmodIfExists(new File(rootDir, "usr/lib/libandroid-sysvshm.so"));
        chmodIfExists(new File(rootDir, "usr/lib/libevshim.so"));
        chmodIfExists(new File(rootDir, "usr/lib/libfakeinput.so"));
        chmodIfExists(new File(rootDir, "usr/lib/libdummyvk.so"));
        chmodIfExists(new File(rootDir, "usr/lib/libc++_shared.so"));
        chmodIfExists(new File(rootDir, "usr/lib/android-host/libredirect-bionic.so"));
        chmodIfExists(new File(rootDir, "usr/lib/android-host/libandroid-sysvshm.so"));
        chmodIfExists(new File(rootDir, "usr/lib/android-host/libevshim.so"));
        chmodIfExists(new File(rootDir, "usr/lib/android-host/libdummyvk.so"));
        chmodIfExists(new File(rootDir, "usr/lib/android-host/libc++_shared.so"));
        chmodTree(new File(rootDir, "usr/lib/android-host"), 0755);
        int removedVulkanResidue = sanitizeVulkanManifestResidue(context, rootDir);
        ImageFs installedImageFs = ImageFs.find(rootDir);
        ensureAndroidVulkanStubClosure(context, installedImageFs);
        logVulkanRuntimeClosure(context, installedImageFs, removedVulkanResidue);
        chmodIfExists(new File(rootDir, "generate_interfaces_file.exe"));
        chmodIfExists(new File(rootDir, "Steamless/Steamless.CLI.exe"));
        chmodIfExists(new File(rootDir, "opt/mono-gecko-offline/wine-mono-11.0.0-x86.msi"));
        chmodIfExists(new File(rootDir, "opt/mono-gecko-offline/wine-mono-9.0.0-x86.msi"));
        installPrefixPackToolkit(context, rootDir);
    }

    private static boolean copyAssetTree(Context context, String assetRoot, File destinationRoot) {
        try {
            AssetManager assetManager = context.getAssets();
            String[] children = assetManager.list(assetRoot);
            if (children == null) return false;
            if (!destinationRoot.exists()) destinationRoot.mkdirs();
            FileUtils.chmod(destinationRoot, 0771);

            if (children.length == 0) {
                try (InputStream inputStream = assetManager.open(assetRoot);
                     OutputStream outputStream = new FileOutputStream(destinationRoot)) {
                    byte[] buffer = new byte[16 * 1024];
                    int read;
                    while ((read = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, read);
                    }
                }
                return true;
            }

            for (String child : children) {
                String childAssetPath = assetRoot + "/" + child;
                String[] grandChildren = assetManager.list(childAssetPath);
                File destination = new File(destinationRoot, child);
                if (grandChildren != null && grandChildren.length > 0) {
                    if (!copyAssetTree(context, childAssetPath, destination)) return false;
                } else {
                    File parent = destination.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    try (InputStream inputStream = assetManager.open(childAssetPath);
                         OutputStream outputStream = new FileOutputStream(destination)) {
                        byte[] buffer = new byte[16 * 1024];
                        int read;
                        while ((read = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, read);
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            Log.e("ImageFsInstaller", "Unable to copy asset tree: " + assetRoot, e);
            return false;
        }
    }

    private static int countPresentPrefixPackCacheFiles(File cacheDir, List<PrefixPackCatalog.Entry> entries) {
        if (cacheDir == null || !cacheDir.isDirectory() || entries == null) return 0;
        int count = 0;
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry == null) continue;
            File cacheFile = new File(cacheDir, entry.fileName);
            if (cacheFile.isFile() && cacheFile.length() > 0L) count++;
        }
        return count;
    }

    private static void installPrefixPackToolkit(Context context, File rootDir) {
        if (context == null || rootDir == null) return;
        File toolkitDir = new File(rootDir, PREFIX_PACK_ROOTFS_DIR);
        if (toolkitDir.exists() && !toolkitDir.isDirectory()) {
            FileUtils.delete(toolkitDir);
        }
        ensureDirectory(toolkitDir, 0771);
        if (!copyAssetTree(context, PREFIX_PACK_ASSET_ROOT, toolkitDir)) {
            Log.e("ImageFsInstaller", "prefix-pack toolkit deploy failed");
            return;
        }

        chmodTree(new File(toolkitDir, "bin"), 0755);
        chmodIfExists(new File(toolkitDir, "README.txt"));
        chmodIfExists(new File(toolkitDir, "catalog.tsv"));
        chmodIfExists(new File(toolkitDir, "VERSION"));
        ensureDirectory(new File(toolkitDir, "cache"), 0771);
    }

    public static void ensurePrefixPackToolkit(Context context, ImageFs imageFs) {
        if (context == null || imageFs == null) return;

        String expectedVersion = readAssetString(context, PREFIX_PACK_VERSION_ASSET).trim();
        File toolkitDir = new File(imageFs.getRootDir(), PREFIX_PACK_ROOTFS_DIR);
        File versionFile = new File(toolkitDir, "VERSION");
        String installedVersion = versionFile.isFile() ? FileUtils.readString(versionFile).trim() : "";

        if (!expectedVersion.isEmpty()
                && expectedVersion.equals(installedVersion)
                && new File(toolkitDir, "catalog.tsv").isFile()
                && new File(toolkitDir, "bin/prefixpack-prefetch.sh").isFile()
                && new File(toolkitDir, "windows/prefix-pack-common.cmd").isFile()
                && new File(toolkitDir, "windows/prefix-pack-loader.cmd").isFile()) {
            logPrefixPackToolkitReady(context, expectedVersion, toolkitDir);
            return;
        }

        installPrefixPackToolkit(context, imageFs.getRootDir());
        logPrefixPackToolkitReady(context, expectedVersion, toolkitDir);
    }

    private static void logPrefixPackToolkitReady(Context context, String expectedVersion, File toolkitDir) {
        File cacheDir = new File(toolkitDir, "cache");
        List<PrefixPackCatalog.Entry> entries = PrefixPackCatalog.parse(readAssetString(context, PREFIX_PACK_CATALOG_ASSET));
        int downloadableCount = PrefixPackCatalog.countByMode(entries, PrefixPackCatalog.MODE_DOWNLOAD);
        int manualCount = PrefixPackCatalog.countByMode(entries, PrefixPackCatalog.MODE_MANUAL_PAGE);
        int cachedCount = countPresentPrefixPackCacheFiles(cacheDir, entries);
        PrefixPackCatalog.Entry vcppEntry = PrefixPackCatalog.findById(entries, "vcpp_aio");

        ForensicLogger.logEvent(
                context,
                "info",
                "PREFIX_PACK_TOOLKIT_READY",
                null,
                "rootfs",
                "prefix_pack_toolkit_ready",
                ForensicLogger.fields(
                        "toolkit_version", expectedVersion,
                        "toolkit_dir", toolkitDir.getAbsolutePath(),
                        "cache_dir", cacheDir.getAbsolutePath(),
                        "catalog_entry_count", entries.size(),
                        "downloadable_entry_count", downloadableCount,
                        "manual_entry_count", manualCount,
                        "cached_entry_count", cachedCount,
                        "vcpp_release_url", vcppEntry != null ? vcppEntry.sourceUrl : ""
                )
        );
    }

    public static void ensureBionicHostSupport(Context context, ImageFs imageFs) {
        if (context == null || imageFs == null) return;
        if (!assetExists(context, BIONIC_HOST_SUPPORT_ARCHIVE)) return;

        File hostLibDir = imageFs.getAndroidHostLibDir();
        if (hasBionicHostSupportClosure(hostLibDir)) {
            ensureImageFsLibraryRunpathSanitized(imageFs);
            ensureAndroidVulkanStubClosure(context, imageFs);
            logBionicHostSupportClosure(context, hostLibDir);
            return;
        }

        if (hostLibDir.exists()) FileUtils.delete(hostLibDir);
        ensureDirectory(hostLibDir, 0771);
        if (!TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, BIONIC_HOST_SUPPORT_ARCHIVE, imageFs.getRootDir())) {
            Log.e("ImageFsInstaller", "Unable to refresh bionic host support overlay");
            return;
        }
        chmodTree(hostLibDir, 0755);
        ensureImageFsLibraryRunpathSanitized(imageFs);
        ensureAndroidVulkanStubClosure(context, imageFs);
        logBionicHostSupportClosure(context, hostLibDir);
    }

    private static boolean hasBionicHostSupportClosure(File hostLibDir) {
        if (hostLibDir == null || !hostLibDir.isDirectory()) return false;
        for (String name : BIONIC_HOST_SUPPORT_REQUIRED_LIBS) {
            if (!new File(hostLibDir, name).exists()) return false;
        }
        return true;
    }

    private static void logBionicHostSupportClosure(Context context, File hostLibDir) {
        if (context == null || hostLibDir == null) return;
        String missing = collectMissingFiles(hostLibDir, BIONIC_HOST_SUPPORT_REQUIRED_LIBS);
        ForensicLogger.logEvent(
                context,
                missing.isEmpty() ? "info" : "warn",
                "BIONIC_HOST_SUPPORT_CLOSURE",
                null,
                "rootfs",
                missing.isEmpty() ? "bionic_host_support_closure_ready" : "bionic_host_support_closure_incomplete",
                ForensicLogger.fields(
                        "host_lib_dir", hostLibDir.getAbsolutePath(),
                        "required_libs", String.join(",", BIONIC_HOST_SUPPORT_REQUIRED_LIBS),
                        "missing_libs", missing
                )
        );
    }

    public static void ensureAppNativeGuestLibs(Context context, ImageFs imageFs) {
        if (context == null || imageFs == null) return;
        File rootDir = imageFs.getRootDir();
        if (rootDir == null || !rootDir.isDirectory()) return;

        File guestLibDir = imageFs.getLibDir();
        ensureDirectory(guestLibDir, 0771);
        installAppNativeGuestLibs(context, rootDir);
        chmodIfExists(new File(guestLibDir, "libredirect.so"));
        chmodIfExists(new File(guestLibDir, "libredirect-bionic.so"));
        chmodIfExists(new File(guestLibDir, "libandroid-sysvshm.so"));
        chmodIfExists(new File(guestLibDir, "libevshim.so"));
        chmodIfExists(new File(guestLibDir, "libfakeinput.so"));
        chmodIfExists(new File(guestLibDir, "libdummyvk.so"));
        chmodIfExists(new File(guestLibDir, "libc++_shared.so"));
        File hostLibDir = imageFs.getAndroidHostLibDir();
        chmodIfExists(new File(hostLibDir, "libredirect-bionic.so"));
        chmodIfExists(new File(hostLibDir, "libandroid-sysvshm.so"));
        chmodIfExists(new File(hostLibDir, "libevshim.so"));
        chmodIfExists(new File(hostLibDir, "libdummyvk.so"));
        chmodIfExists(new File(hostLibDir, "libc++_shared.so"));
        int removedVulkanResidue = sanitizeVulkanManifestResidue(context, rootDir);
        ensureImageFsLibraryRunpathSanitized(imageFs);
        ensureAndroidVulkanStubClosure(context, imageFs);
        logVulkanRuntimeClosure(context, imageFs, removedVulkanResidue);
    }

    private static int sanitizeVulkanManifestResidue(Context context, File rootDir) {
        if (context == null || rootDir == null || !rootDir.isDirectory()) return 0;
        int removed = 0;
        for (String relativeDir : VULKAN_MANIFEST_DIRS) {
            File manifestDir = new File(rootDir, relativeDir);
            removed += deleteVulkanManifestResidue(manifestDir);
        }
        return removed;
    }

    private static int deleteVulkanManifestResidue(File directory) {
        if (directory == null || !directory.isDirectory()) return 0;
        int removed = 0;
        File[] children = directory.listFiles();
        if (children == null) return 0;
        for (File child : children) {
            if (child == null) continue;
            if (child.isDirectory()) {
                removed += deleteVulkanManifestResidue(child);
                continue;
            }
            String name = child.getName();
            if (name.startsWith("._") || ".DS_Store".equals(name)) {
                if (FileUtils.delete(child)) removed++;
            }
        }
        return removed;
    }

    private static int countVulkanManifestResidue(File rootDir) {
        if (rootDir == null || !rootDir.isDirectory()) return 0;
        int count = 0;
        for (String relativeDir : VULKAN_MANIFEST_DIRS) {
            count += countVulkanManifestResidueInDir(new File(rootDir, relativeDir));
        }
        return count;
    }

    private static int countVulkanManifestResidueInDir(File directory) {
        if (directory == null || !directory.isDirectory()) return 0;
        int count = 0;
        File[] children = directory.listFiles();
        if (children == null) return 0;
        for (File child : children) {
            if (child == null) continue;
            if (child.isDirectory()) {
                count += countVulkanManifestResidueInDir(child);
                continue;
            }
            String name = child.getName();
            if (name.startsWith("._") || ".DS_Store".equals(name)) count++;
        }
        return count;
    }

    private static void logVulkanRuntimeClosure(Context context, ImageFs imageFs, int removedVulkanResidue) {
        if (context == null || imageFs == null || imageFs.getRootDir() == null) return;
        File rootDir = imageFs.getRootDir();
        File guestLibDir = imageFs.getLibDir();
        File hostLibDir = imageFs.getAndroidHostLibDir();
        File androidStubDir = new File(rootDir, "android_stub");
        ArrayList<File> androidStubDirs = resolveAndroidVulkanStubDirs(rootDir);
        File wrapperIcd = new File(rootDir, "usr/share/vulkan/icd.d/wrapper_icd.aarch64.json");
        File wrapperLib = new File(guestLibDir, "libvulkan_wrapper.so");
        File guestLibcxx = new File(guestLibDir, "libc++_shared.so");
        File hostLibcxx = new File(hostLibDir, "libc++_shared.so");
        String nativeLibDir = AppUtils.getNativeLibDir(context);
        File nativeLibcxx = nativeLibDir == null || nativeLibDir.trim().isEmpty()
                ? null
                : new File(nativeLibDir, "libc++_shared.so");
        int remainingVulkanResidue = countVulkanManifestResidue(rootDir);

        ForensicLogger.logEvent(
                context,
                remainingVulkanResidue == 0 && (!wrapperLib.isFile() || guestLibcxx.isFile()) ? "info" : "warn",
                "VULKAN_ROOTFS_RUNTIME_CLOSURE",
                null,
                "rootfs",
                "vulkan_rootfs_runtime_closure",
                ForensicLogger.fields(
                        "wrapper_icd_present", wrapperIcd.isFile(),
                        "wrapper_icd_path", wrapperIcd.getAbsolutePath(),
                        "wrapper_lib_present", wrapperLib.isFile(),
                        "wrapper_lib_path", wrapperLib.getAbsolutePath(),
                        "guest_libcxx_present", guestLibcxx.isFile(),
                        "host_libcxx_present", hostLibcxx.isFile(),
                        "native_libcxx_present", nativeLibcxx != null && nativeLibcxx.isFile(),
                        "android_stub_dir", androidStubDir.getAbsolutePath(),
                        "android_stub_missing_libs", collectMissingFiles(androidStubDir, ANDROID_VULKAN_STUB_REQUIRED_LIBS),
                        "android_stub_dirs", joinFilePaths(androidStubDirs),
                        "android_stub_missing_by_dir", collectMissingFilesByDir(androidStubDirs, ANDROID_VULKAN_STUB_REQUIRED_LIBS),
                        "removed_vulkan_manifest_residue", removedVulkanResidue,
                        "remaining_vulkan_manifest_residue", remainingVulkanResidue
                )
        );
    }

    public static boolean extractSupportArchive(Context context, String archiveName, TarCompressorUtils.Type type, File outputDir) {
        if (assetExists(context, archiveName)) {
            return TarCompressorUtils.extract(type, context, archiveName, outputDir);
        }
        File downloaded = resolveDownloadedArchive(ImageFs.find(context), archiveName);
        if (downloaded.isFile()) {
            return TarCompressorUtils.extract(type, downloaded, outputDir);
        }
        return false;
    }

    private static Future<Boolean> installFromAssetsFuture(final Context context, AssetManager assetManager,
                                                           Container container, Callback<Integer> onProgress) {
        return installFromAssetsFuture(context, assetManager, container, null, onProgress);
    }

    private static Future<Boolean> installFromAssetsFuture(final Context context, AssetManager assetManager,
                                                           Container container, String requestedRuntimeModel,
                                                           Callback<Integer> onProgress) {
        return installFromAssetsFuture(context, assetManager, container, requestedRuntimeModel,
                container != null ? container.getWineVersion() : "", onProgress);
    }

    private static Future<Boolean> installFromAssetsFuture(final Context context, AssetManager assetManager,
                                                           Container container, String requestedRuntimeModel,
                                                           String requestedRuntimeIdentity,
                                                           Callback<Integer> onProgress) {
        ImageFs imageFs = ImageFs.find(context, requestedRuntimeModel, requestedRuntimeIdentity);
        final File rootDir = imageFs.getRootDir();
        final String containerVariant = resolveInstallVariant(imageFs, container, requestedRuntimeModel);

        if (context instanceof MainActivity) {
            SettingsFragment.resetEmulatorsVersion((MainActivity) context);
        }

        return Executors.newSingleThreadExecutor().submit(() -> {
            clearRootDir(context, rootDir);
            final byte compressionRatio = 22;
            String imagefsArchive = resolveImageFsArchiveName(context, imageFs, containerVariant);
            String preferredArchive = Container.GLIBC.equalsIgnoreCase(containerVariant)
                    ? GLIBC_IMAGEFS_ARCHIVE
                    : BIONIC_IMAGEFS_ARCHIVE;
            boolean downloadedBaseThisPass = false;
            if (!isArchiveAvailable(context, imageFs, preferredArchive)
                    && isRemoteDeliverableArchive(preferredArchive)) {
                downloadedBaseThisPass = fetchArchiveWithFallback(imageFs, preferredArchive, onProgress, 0, 35);
                if (downloadedBaseThisPass) {
                    imagefsArchive = preferredArchive;
                }
            }

            if (!isArchiveAvailable(context, imageFs, imagefsArchive)) {
                String remoteCandidate = Container.GLIBC.equalsIgnoreCase(containerVariant)
                        ? GLIBC_IMAGEFS_ARCHIVE
                        : BIONIC_IMAGEFS_ARCHIVE;
                if (ensureArchiveAvailable(context, imageFs, remoteCandidate, onProgress, 0, 35)) {
                    imagefsArchive = remoteCandidate;
                    downloadedBaseThisPass = !assetExists(context, remoteCandidate)
                            && resolveDownloadedArchive(imageFs, remoteCandidate).isFile();
                }
            }

            boolean downloadedSupportThisPass = false;
            if (!assetExists(context, imagefsArchive) && !resolveDownloadedArchive(imageFs, imagefsArchive).isFile()) {
                Log.e("ImageFsInstaller", "Donor rootfs archive is required but unavailable: " + preferredArchive);
                return false;
            }
            File downloaded = new File(imageFs.getFilesDir(), imagefsArchive);
            if (Container.GLIBC.equalsIgnoreCase(containerVariant)) {
                downloadedSupportThisPass = prefetchVariantRuntimeSupportIfMissing(context, imageFs, containerVariant, onProgress);
                if (!isArchiveAvailable(context, imageFs, GLIBC_PATCH_ARCHIVE)) {
                    Log.w("ImageFsInstaller", "glibc patch archive not available yet: " + GLIBC_PATCH_ARCHIVE);
                }
            }

            final long contentLength;
            final boolean useAssetArchive;
            if (assetExists(context, imagefsArchive)) {
                useAssetArchive = true;
                contentLength = (long) (FileUtils.getSize(context, imagefsArchive) * (100.0f / compressionRatio));
            } else if (downloaded.isFile() && verifyDownloadedArchive(imagefsArchive, downloaded)) {
                useAssetArchive = false;
                contentLength = (long) (downloaded.length() * (100.0f / compressionRatio));
            } else {
                Log.e("ImageFsInstaller", "No imagefs archive available for " + imagefsArchive);
                return false;
            }

            AtomicLong totalSizeRef = new AtomicLong();
            int extractProgressStart = downloadedSupportThisPass ? 45 : (downloadedBaseThisPass ? 35 : 0);
            int extractProgressSpan = Math.max(1, 100 - extractProgressStart);
            TarCompressorUtils.Type imagefsArchiveType = resolveArchiveType(imagefsArchive);
            ForensicLogger.logEvent(
                    context,
                    "info",
                    "ROOTFS_IMAGEFS_ARCHIVE_SELECTED",
                    null,
                    "rootfs",
                    "rootfs_imagefs_archive_selected",
                    ForensicLogger.fields(
                            "variant", containerVariant,
                            "archive", imagefsArchive,
                            "archive_type", imagefsArchiveType.name().toLowerCase(Locale.US),
                            "source", useAssetArchive ? "asset" : "download",
                            "provider", resolveInstalledRootfsProvider(imagefsArchive),
                            "expected_sha256", resolveRemoteArchiveSha256(imagefsArchive)
                    )
            );
            boolean success = useAssetArchive
                    ? TarCompressorUtils.extract(imagefsArchiveType, context, imagefsArchive, rootDir, (file, size) -> {
                        if (size > 0 && onProgress != null && contentLength > 0) {
                            long totalSize = totalSizeRef.addAndGet(size);
                            int progress = extractProgressStart + (int) (((float) totalSize / contentLength) * extractProgressSpan);
                            onProgress.call(Math.min(100, progress));
                        }
                        return file;
                    })
                    : TarCompressorUtils.extract(imagefsArchiveType, downloaded, rootDir, (file, size) -> {
                        if (size > 0 && onProgress != null && contentLength > 0) {
                            long totalSize = totalSizeRef.addAndGet(size);
                            int progress = extractProgressStart + (int) (((float) totalSize / contentLength) * extractProgressSpan);
                            onProgress.call(Math.min(100, progress));
                        }
                        return file;
                    });

            if (success) {
                installWineFromAssets(context, containerVariant, imageFs, requestedRuntimeIdentity);
                installWineFromDownloads(context, containerVariant, imageFs, requestedRuntimeIdentity);
                if (context instanceof MainActivity) {
                    installDriversFromAssets((MainActivity) context);
                }
                installGuestLibs(context, rootDir);
                ensureHybridRootfsLayout(imageFs, rootDir);
                ensureImageFsLibraryRunpathSanitized(imageFs);
                imageFs.createImgVersionFile(LATEST_VERSION);
                imageFs.createVariantFile(containerVariant);
                imageFs.createRootfsProviderFile(resolveInstalledRootfsProvider(imagefsArchive));
                imageFs.createRootfsLayoutFile(resolveInstalledRootfsLayout(imagefsArchive));
                if (container != null && container.getWineVersion() != null && !container.getWineVersion().trim().isEmpty()) {
                    imageFs.createArchFile(container.getWineVersion());
                }
                resetContainerImgVersions(context);
            } else if (downloaded.exists() && !useAssetArchive) {
                Log.w("ImageFsInstaller", "Deleting corrupt downloaded imagefs archive: " + downloaded.getPath());
                downloaded.delete();
            }
            return success;
        });
    }

    public static void installFromAssets(final MainActivity activity) {
        installFromAssets(activity, null);
    }

    public static void installFromAssets(final MainActivity activity, final Container container) {
        AppUtils.keepScreenOn(activity);
        final DownloadProgressDialog dialog = new DownloadProgressDialog(activity);
        dialog.show(R.string.installing_system_files);
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = false;
            try {
                success = installFromAssetsFuture(activity, activity.getAssets(), container,
                        progress -> activity.runOnUiThread(() -> dialog.setProgress(progress))
                ).get();
            } catch (Exception e) {
                Log.e("ImageFsInstaller", "Unable to install system files", e);
            }

            if (!success) {
                AppUtils.showToast(activity, R.string.unable_to_install_system_files);
            }

            dialog.closeOnUiThread();
        });
    }

    public static Future<Boolean> installIfNeededFuture(final Context context, AssetManager assetManager,
                                                        Container container, Callback<Integer> onProgress) {
        return installIfNeededFuture(context, assetManager, container, null, onProgress);
    }

    public static Future<Boolean> installIfNeededFuture(final Context context, AssetManager assetManager,
                                                        Container container, String requestedRuntimeModel,
                                                        Callback<Integer> onProgress) {
        return installIfNeededFuture(context, assetManager, container, requestedRuntimeModel,
                container != null ? container.getWineVersion() : "", onProgress);
    }

    public static Future<Boolean> installIfNeededFuture(final Context context, AssetManager assetManager,
                                                        Container container, String requestedRuntimeModel,
                                                        String requestedRuntimeIdentity,
                                                        Callback<Integer> onProgress) {
        if (isInstallRequired(context, container, requestedRuntimeModel, requestedRuntimeIdentity)) {
            return installFromAssetsFuture(context, assetManager, container, requestedRuntimeModel,
                    requestedRuntimeIdentity, onProgress);
        }
        return Executors.newSingleThreadExecutor().submit(() -> true);
    }

    public static void installIfNeeded(final MainActivity activity) {
        ImageFs imageFs = ImageFs.find(activity);
        String requestedVariant = resolveInstallVariant(imageFs, null);
        boolean variantMismatch = !imageFs.getVariant().isEmpty() && !requestedVariant.equalsIgnoreCase(imageFs.getVariant());
        if (!imageFs.isValid() || imageFs.getVersion() < LATEST_VERSION || variantMismatch) {
            installFromAssets(activity, null);
        }
    }

    private static boolean isImportedWineProton(Context context, String fileName) {
        String lowerName = fileName.toLowerCase();
        if (!lowerName.startsWith("wine-") && !lowerName.startsWith("proton-")) {
            return false;
        }

        for (String version : context.getResources().getStringArray(R.array.bionic_wine_entries)) {
            if (lowerName.equals(version.toLowerCase())) return false;
        }
        for (String version : context.getResources().getStringArray(R.array.glibc_wine_entries)) {
            if (lowerName.equals(version.toLowerCase())) return false;
        }
        for (String version : context.getResources().getStringArray(R.array.wine_entries)) {
            if (lowerName.equals(version.toLowerCase())) return false;
        }
        return true;
    }

    private static boolean shouldPreserveOptEntry(Context context, File file) {
        if (file == null) return false;
        String fileName = file.getName();
        if (fileName.equals("installed-wine")) return true;
        if (fileName.startsWith("runtime-")) return true;
        if (isImportedWineProton(context, fileName)) return true;
        return new File(file, "profile.json").isFile();
    }

    private static void clearOptDir(Context context, File optDir) {
        File[] files = optDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (shouldPreserveOptEntry(context, file)) continue;
            FileUtils.delete(file);
        }
    }

    private static void clearRootDir(Context context, File rootDir) {
        if (rootDir.isDirectory()) {
            File[] files = rootDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        String name = file.getName();
                        if (name.equals("home")) {
                            continue;
                        }
                        if (name.equals("opt")) {
                            clearOptDir(context, file);
                            continue;
                        }
                    }
                    FileUtils.delete(file);
                }
            }
        } else {
            rootDir.mkdirs();
        }
    }

    public static void generateCompactContainerPattern(final Context context, AssetManager assetManager) {
        Executors.newSingleThreadExecutor().execute(() -> {
            File[] srcFiles;
            File[] dstFiles;
            ImageFs imageFs = ImageFs.find(context);
            File mainWineDir = imageFs.getMainWineDir();
            File runtimeWineLibDir = WineUtils.resolveRuntimeWineLibDir(mainWineDir);
            if (runtimeWineLibDir == null) {
                Log.e("ImageFsInstaller", "Missing runtime lib/wine directory while compacting container pattern");
                return;
            }
            File wineSystem32Dir = new File(runtimeWineLibDir, "x86_64-windows");
            File wineSysWoW64Dir = new File(runtimeWineLibDir, "i386-windows");

            File containerPatternDir = new File(context.getCacheDir(), "container_pattern_gamenative");
            FileUtils.delete(containerPatternDir);
            if (!TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, "container_pattern_gamenative.tzst", containerPatternDir)) {
                Log.e("ImageFsInstaller", "Failed to extract container_pattern_gamenative.tzst for compaction");
                return;
            }

            File containerSystem32Dir = new File(containerPatternDir, ".wine/drive_c/windows/system32");
            File containerSysWoW64Dir = new File(containerPatternDir, ".wine/drive_c/windows/syswow64");

            dstFiles = containerSystem32Dir.listFiles();
            srcFiles = wineSystem32Dir.listFiles();
            if (dstFiles == null || srcFiles == null) {
                Log.e("ImageFsInstaller", "Unable to enumerate main runtime system32 files for compact container pattern generation");
                FileUtils.delete(containerPatternDir);
                return;
            }

            JSONArray system32JSONArray = new JSONArray();
            for (File dstFile : dstFiles) {
                for (File srcFile : srcFiles) {
                    if (!dstFile.getName().equals(srcFile.getName())) continue;
                    if (FileUtils.contentEquals(srcFile, dstFile)) {
                        FileUtils.delete(dstFile);
                        system32JSONArray.put(srcFile.getName());
                    }
                    break;
                }
            }

            dstFiles = containerSysWoW64Dir.listFiles();
            srcFiles = wineSysWoW64Dir.listFiles();
            if (dstFiles == null || srcFiles == null) {
                Log.e("ImageFsInstaller", "Unable to enumerate main runtime syswow64 files for compact container pattern generation");
                FileUtils.delete(containerPatternDir);
                return;
            }

            JSONArray syswow64JSONArray = new JSONArray();
            for (File dstFile : dstFiles) {
                for (File srcFile : srcFiles) {
                    if (!dstFile.getName().equals(srcFile.getName())) continue;
                    if (FileUtils.contentEquals(srcFile, dstFile)) {
                        FileUtils.delete(dstFile);
                        syswow64JSONArray.put(srcFile.getName());
                    }
                    break;
                }
            }

            try {
                JSONObject data = new JSONObject();
                data.put("system32", system32JSONArray);
                data.put("syswow64", syswow64JSONArray);
                FileUtils.writeString(new File(context.getCacheDir(), "common_dlls.json"), data.toString());

                File outputFile = new File(context.getCacheDir(), "container_pattern_gamenative.tzst");
                FileUtils.delete(outputFile);
                TarCompressorUtils.compress(
                        TarCompressorUtils.Type.ZSTD,
                        new File(containerPatternDir, ".wine"),
                        outputFile,
                        22
                );
            } catch (JSONException e) {
                Log.e("ImageFsInstaller", "Failed to build compact container pattern metadata", e);
            } finally {
                FileUtils.delete(containerPatternDir);
            }
        });
    }

    private static void ensureImageFsLibraryRunpathSanitized(ImageFs imageFs) {
        if (imageFs == null) return;

        File imageFsLibDir = imageFs.getLibDir();
        if (imageFsLibDir == null || !imageFsLibDir.isDirectory()) return;

        File configDir = imageFs.getConfigDir();
        ensureDirectory(configDir, 0771);
        File markerFile = new File(configDir, IMAGEFS_LIB_RUNPATH_MARKER);
        if (markerFile.isFile()) {
            String markerValue = FileUtils.readString(markerFile);
            if (IMAGEFS_LIB_RUNPATH_MARKER_VERSION.equals(markerValue != null ? markerValue.trim() : "")) {
                return;
            }
        }

        WineRuntimeRunpathSanitizer.Result result =
                WineRuntimeRunpathSanitizer.sanitizeTree(imageFsLibDir, imageFsLibDir);
        if (result.hasSignal()) {
            Log.i("ImageFsInstaller", "ImageFs lib RUNPATH sanitize: " + result.toSummary());
        }

        if (result.failedFiles == 0 && FileUtils.writeString(markerFile, IMAGEFS_LIB_RUNPATH_MARKER_VERSION)) {
            FileUtils.chmod(markerFile, 0660);
        }
    }
}
