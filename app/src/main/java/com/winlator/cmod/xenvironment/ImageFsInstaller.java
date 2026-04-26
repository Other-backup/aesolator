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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ImageFsInstaller {
    public static final byte LATEST_VERSION = 27;
    private static final String GLIBC_IMAGEFS_ARCHIVE = "imagefs_gamenative.txz";
    private static final String BIONIC_IMAGEFS_ARCHIVE = GLIBC_IMAGEFS_ARCHIVE;
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
            {"libc++_shared.so", "libc++_shared.so"}
    };
    private static final String[][] APP_NATIVE_ANDROID_HOST_LIBS = {
            {"libaero_redirect_bionic.so", "libredirect-bionic.so"},
            {"libaero_android_sysvshm.so", "libandroid-sysvshm.so"},
            {"libaero_evshim.so", "libevshim.so"},
            {"libdummyvk.so", "libdummyvk.so"},
            {"libc++_shared.so", "libc++_shared.so"}
    };
    private static final String[] VULKAN_MANIFEST_DIRS = {
            "usr/share/vulkan/icd.d",
            "usr/share/vulkan/implicit_layer.d",
            "usr/share/vulkan/explicit_layer.d"
    };
    private static final String ROOTFS_PRIMARY_BASE_URL = "https://downloads.gamenative.app/";
    private static final String ROOTFS_FALLBACK_BASE_URL = "https://pub-9fcd5294bd0d4b85a9d73615bf98f3b5.r2.dev/";
    private static final int DOWNLOAD_CONNECT_TIMEOUT_MS = 15000;
    private static final int DOWNLOAD_READ_TIMEOUT_MS = 30000;
    private static final int DOWNLOAD_BUFFER_SIZE = 64 * 1024;
    private static final String IMAGEFS_LIB_RUNPATH_MARKER = ".elf_runpath_sanitizer_version";
    private static final String IMAGEFS_LIB_RUNPATH_MARKER_VERSION = "2";
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
        ImageFs imageFs = ImageFs.find(context);
        String requestedVariant = resolveInstallVariant(imageFs, container, requestedRuntimeModel);
        boolean universalGameNativeRootfs = imageFs.isGameNativeRootfs()
                && GLIBC_IMAGEFS_ARCHIVE.equals(BIONIC_IMAGEFS_ARCHIVE);
        boolean variantMismatch = !universalGameNativeRootfs
                && !imageFs.getVariant().isEmpty()
                && !requestedVariant.equalsIgnoreCase(imageFs.getVariant());
        return !imageFs.isValid()
                || imageFs.getVersion() < LATEST_VERSION
                || variantMismatch;
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
        if (linkFile == null || linkFile.exists()) return;
        FileUtils.symlink(linkTarget, linkFile.getAbsolutePath());
        if (!linkFile.exists()) ensureDirectory(linkFile, mode);
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

        File canonicalWine = new File(rootDir, "/opt/wine");
        File mainWineVersionDir = new File(rootDir, "/opt/" + WineInfo.MAIN_WINE_VERSION.identifier());
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

    private static boolean isRemoteDeliverableArchive(String archiveName) {
        return GLIBC_IMAGEFS_ARCHIVE.equals(archiveName)
                || BIONIC_IMAGEFS_ARCHIVE.equals(archiveName)
                || GLIBC_PATCH_ARCHIVE.equals(archiveName);
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
        if (downloadFile(ROOTFS_PRIMARY_BASE_URL + archiveName, destination, onProgress, progressStart, progressSpan)) {
            return true;
        }

        Log.w("ImageFsInstaller", "Primary download failed for " + archiveName + ", retrying with secondary path");
        if (downloadFile(ROOTFS_FALLBACK_BASE_URL + archiveName, destination, onProgress, progressStart, progressSpan)) {
            return true;
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
        if (downloaded.isFile() && downloaded.length() > 0L) return true;
        return fetchArchiveWithFallback(imageFs, archiveName, onProgress, progressStart, progressSpan);
    }

    private static boolean isArchiveAvailable(Context context, ImageFs imageFs, String archiveName) {
        if (assetExists(context, archiveName)) return true;
        File downloaded = resolveDownloadedArchive(imageFs, archiveName);
        return downloaded.isFile() && downloaded.length() > 0L;
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
        String[] versions = getBundledWineEntries(context, containerVariant);
        File rootDir = ImageFs.find(context).getRootDir();
        for (String version : versions) {
            if (!assetExists(context, version + ".txz")) continue;
            File outFile = new File(rootDir, "/opt/" + version);
            outFile.mkdirs();
            TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, context, version + ".txz", outFile);
        }
    }

    public static void installWineFromDownloads(final Context context, String containerVariant) {
        String[] versions = getBundledWineEntries(context, containerVariant);
        File rootDir = ImageFs.find(context).getRootDir();
        ImageFs imageFs = ImageFs.find(context);
        for (String version : versions) {
            File downloaded = new File(imageFs.getFilesDir(), version + ".txz");
            if (!downloaded.isFile()) continue;
            File outFile = new File(rootDir, "/opt/" + version);
            outFile.mkdirs();
            TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, downloaded, outFile);
        }
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
            boolean needsCopy = !destination.isFile() || !FileUtils.contentEquals(source, destination);
            if (needsCopy) {
                FileUtils.copy(source, destination);
            }
            chmodIfExists(destination);
        }

        for (String[] librarySpec : APP_NATIVE_ANDROID_HOST_LIBS) {
            if (librarySpec == null || librarySpec.length < 2) continue;

            String sourceName = librarySpec[0];
            String destinationName = librarySpec[1];
            File source = new File(nativeLibRoot, sourceName);
            if (!source.isFile()) continue;

            File destination = new File(androidHostLibDir, destinationName);
            boolean needsCopy = !destination.isFile() || !FileUtils.contentEquals(source, destination);
            if (needsCopy) {
                FileUtils.copy(source, destination);
            }
            chmodIfExists(destination);
        }

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
        logVulkanRuntimeClosure(context, ImageFs.find(rootDir), removedVulkanResidue);
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
    }

    private static boolean hasBionicHostSupportClosure(File hostLibDir) {
        if (hostLibDir == null || !hostLibDir.isDirectory()) return false;
        String[] required = {
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
                "libvulkan.so.1"
        };
        for (String name : required) {
            if (!new File(hostLibDir, name).exists()) return false;
        }
        return true;
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
        ImageFs imageFs = ImageFs.find(context);
        final File rootDir = imageFs.getRootDir();
        final String containerVariant = resolveInstallVariant(imageFs, container);

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
            if (!assetExists(context, preferredArchive)
                    && !resolveDownloadedArchive(imageFs, preferredArchive).isFile()
                    && isRemoteDeliverableArchive(preferredArchive)) {
                downloadedBaseThisPass = fetchArchiveWithFallback(imageFs, preferredArchive, onProgress, 0, 35);
                if (downloadedBaseThisPass) {
                    imagefsArchive = preferredArchive;
                }
            }

            if (!assetExists(context, imagefsArchive) && !resolveDownloadedArchive(imageFs, imagefsArchive).isFile()) {
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
            } else if (downloaded.isFile()) {
                useAssetArchive = false;
                contentLength = (long) (downloaded.length() * (100.0f / compressionRatio));
            } else {
                Log.e("ImageFsInstaller", "No imagefs archive available for " + imagefsArchive);
                return false;
            }

            AtomicLong totalSizeRef = new AtomicLong();
            int extractProgressStart = downloadedSupportThisPass ? 45 : (downloadedBaseThisPass ? 35 : 0);
            int extractProgressSpan = Math.max(1, 100 - extractProgressStart);
            boolean success = useAssetArchive
                    ? TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, context, imagefsArchive, rootDir, (file, size) -> {
                        if (size > 0 && onProgress != null && contentLength > 0) {
                            long totalSize = totalSizeRef.addAndGet(size);
                            int progress = extractProgressStart + (int) (((float) totalSize / contentLength) * extractProgressSpan);
                            onProgress.call(Math.min(100, progress));
                        }
                        return file;
                    })
                    : TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, downloaded, rootDir, (file, size) -> {
                        if (size > 0 && onProgress != null && contentLength > 0) {
                            long totalSize = totalSizeRef.addAndGet(size);
                            int progress = extractProgressStart + (int) (((float) totalSize / contentLength) * extractProgressSpan);
                            onProgress.call(Math.min(100, progress));
                        }
                        return file;
                    });

            if (success) {
                installWineFromAssets(context, containerVariant);
                installWineFromDownloads(context, containerVariant);
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
        ImageFs imageFs = ImageFs.find(context);
        if (isInstallRequired(context, container, requestedRuntimeModel)) {
            return installFromAssetsFuture(context, assetManager, container, onProgress);
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
