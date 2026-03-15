package com.winlator.cmod.xenvironment;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.winlator.cmod.MainActivity;
import com.winlator.cmod.R;
import com.winlator.cmod.SettingsFragment;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.DownloadProgressDialog;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.WineInfo;

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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ImageFsInstaller {
    public static final byte LATEST_VERSION = 26;
    private static final String GLIBC_IMAGEFS_ARCHIVE = "imagefs_gamenative.txz";
    private static final String BIONIC_IMAGEFS_ARCHIVE = "imagefs_bionic.txz";
    private static final String GLIBC_PATCH_ARCHIVE = "imagefs_patches_gamenative.tzst";
    private static final String REDIRECT_ARCHIVE = "redirect.tzst";
    private static final String EXTRAS_ARCHIVE = "extras.tzst";
    private static final String[] APP_NATIVE_GUEST_LIBS = {
            "libevshim.so",
            "libdummyvk.so"
    };
    private static final String ROOTFS_PRIMARY_BASE_URL = "https://downloads.gamenative.app/";
    private static final String ROOTFS_FALLBACK_BASE_URL = "https://pub-9fcd5294bd0d4b85a9d73615bf98f3b5.r2.dev/";
    private static final int DOWNLOAD_CONNECT_TIMEOUT_MS = 15000;
    private static final int DOWNLOAD_READ_TIMEOUT_MS = 30000;
    private static final int DOWNLOAD_BUFFER_SIZE = 64 * 1024;
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
        boolean variantMismatch = !imageFs.getVariant().isEmpty() && !requestedVariant.equalsIgnoreCase(imageFs.getVariant());
        boolean providerMismatch = !ImageFs.ROOTFS_PROVIDER_GAMENATIVE.equalsIgnoreCase(imageFs.getRootfsProvider());
        boolean layoutMismatch = !ImageFs.ROOTFS_LAYOUT_UBUNTUFS.equalsIgnoreCase(imageFs.getRootfsLayout());
        return !imageFs.isValid()
                || imageFs.getVersion() < LATEST_VERSION
                || variantMismatch
                || providerMismatch
                || layoutMismatch;
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
        return ImageFs.ROOTFS_PROVIDER_GAMENATIVE;
    }

    private static String resolveInstalledRootfsLayout(String archiveName) {
        return ImageFs.ROOTFS_LAYOUT_UBUNTUFS;
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
        ensureDirectory(imageFs.getLocalBinDir(), 0771);

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

        Log.w("ImageFsInstaller", "Primary download failed for " + archiveName + ", retrying with fallback");
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
        String[] adrenotoolsAssetDrivers = activity.getResources().getStringArray(R.array.wrapper_graphics_driver_version_entries);

        for (String driver : adrenotoolsAssetDrivers)
            adrenotoolsManager.extractDriverFromResources(driver);
    }

    private static void chmodIfExists(File file) {
        if (file != null && file.exists()) {
            FileUtils.chmod(file, 0755);
        }
    }

    private static void installAppNativeGuestLibs(Context context, File rootDir) {
        String nativeLibDir = AppUtils.getNativeLibDir(context);
        if (nativeLibDir == null || nativeLibDir.trim().isEmpty()) return;

        File nativeLibRoot = new File(nativeLibDir);
        if (!nativeLibRoot.isDirectory()) return;

        File guestLibDir = new File(rootDir, "usr/lib");
        if (!guestLibDir.exists() && !guestLibDir.mkdirs()) return;

        for (String libraryName : APP_NATIVE_GUEST_LIBS) {
            File source = new File(nativeLibRoot, libraryName);
            if (!source.isFile()) continue;

            File destination = new File(guestLibDir, libraryName);
            boolean needsCopy = !destination.isFile() || destination.length() != source.length();
            if (needsCopy) {
                FileUtils.copy(source, destination);
            }
            chmodIfExists(destination);
        }
    }

    private static void installGuestLibs(Context context, File rootDir) {
        if (assetExists(context, REDIRECT_ARCHIVE)) {
            if (!TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, REDIRECT_ARCHIVE, rootDir)) {
                Log.e("ImageFsInstaller", "redirect overlay deploy failed");
            }
        }

        if (assetExists(context, EXTRAS_ARCHIVE)) {
            if (!TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, EXTRAS_ARCHIVE, rootDir)) {
                Log.e("ImageFsInstaller", "extras overlay deploy failed");
            }
        }

        installAppNativeGuestLibs(context, rootDir);

        chmodIfExists(new File(rootDir, "usr/lib/libredirect.so"));
        chmodIfExists(new File(rootDir, "usr/lib/libredirect-bionic.so"));
        chmodIfExists(new File(rootDir, "usr/lib/libevshim.so"));
        chmodIfExists(new File(rootDir, "usr/lib/libdummyvk.so"));
        chmodIfExists(new File(rootDir, "generate_interfaces_file.exe"));
        chmodIfExists(new File(rootDir, "Steamless/Steamless.CLI.exe"));
        chmodIfExists(new File(rootDir, "opt/mono-gecko-offline/wine-mono-9.0.0-x86.msi"));
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
        boolean providerMismatch = !ImageFs.ROOTFS_PROVIDER_GAMENATIVE.equalsIgnoreCase(imageFs.getRootfsProvider());
        boolean layoutMismatch = !ImageFs.ROOTFS_LAYOUT_UBUNTUFS.equalsIgnoreCase(imageFs.getRootfsLayout());
        boolean variantMismatch = !imageFs.getVariant().isEmpty() && !requestedVariant.equalsIgnoreCase(imageFs.getVariant());
        if (!imageFs.isValid() || imageFs.getVersion() < LATEST_VERSION || variantMismatch || providerMismatch || layoutMismatch) {
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

    private static void clearOptDir(Context context, File optDir) {
        File[] files = optDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            String fileName = file.getName();
            if (fileName.equals("installed-wine")) continue;
            if (isImportedWineProton(context, fileName)) continue;
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
            File wineSystem32Dir = new File(mainWineDir, "lib/wine/x86_64-windows");
            File wineSysWoW64Dir = new File(mainWineDir, "lib/wine/i386-windows");

            File containerPatternDir = new File(context.getCacheDir(), "container_pattern_gamenative");
            FileUtils.delete(containerPatternDir);
            if (!TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, assetManager, "container_pattern_gamenative.tzst", containerPatternDir)) {
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
}
