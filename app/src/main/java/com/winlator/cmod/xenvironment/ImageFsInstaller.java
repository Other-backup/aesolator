package com.winlator.cmod.xenvironment;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.winlator.cmod.MainActivity;
import com.winlator.cmod.R;
import com.winlator.cmod.SettingsFragment;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.DownloadProgressDialog;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.WineInfo;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ImageFsInstaller {
    public static final byte LATEST_VERSION = 26;
    private static final String LEGACY_IMAGEFS_ARCHIVE = "imagefs.txz";
    private static final String GLIBC_IMAGEFS_ARCHIVE = "imagefs_gamenative.txz";
    private static final String BIONIC_IMAGEFS_ARCHIVE = "imagefs_bionic.txz";
    private static final String REDIRECT_ARCHIVE = "redirect.tzst";
    private static final String EXTRAS_ARCHIVE = "extras.tzst";

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
        if (container != null) return normalizeContainerVariant(container.getContainerVariant());
        if (imageFs != null && !imageFs.getVariant().isEmpty()) {
            return normalizeContainerVariant(imageFs.getVariant());
        }
        return Container.DEFAULT_VARIANT;
    }

    private static String[] getBundledWineEntries(Context context, String containerVariant) {
        int resId = Container.GLIBC.equalsIgnoreCase(containerVariant)
                ? R.array.glibc_wine_entries
                : R.array.bionic_wine_entries;
        String[] bundled = context.getResources().getStringArray(resId);
        return bundled.length > 0 ? bundled : context.getResources().getStringArray(R.array.wine_entries);
    }

    private static String resolveImageFsArchiveName(Context context, ImageFs imageFs, String containerVariant) {
        String preferred = Container.GLIBC.equalsIgnoreCase(containerVariant)
                ? GLIBC_IMAGEFS_ARCHIVE
                : BIONIC_IMAGEFS_ARCHIVE;
        File downloaded = new File(imageFs.getFilesDir(), preferred);
        if (assetExists(context, preferred) || downloaded.isFile()) return preferred;
        return LEGACY_IMAGEFS_ARCHIVE;
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

        chmodIfExists(new File(rootDir, "usr/lib/libredirect.so"));
        chmodIfExists(new File(rootDir, "usr/lib/libredirect-bionic.so"));
        chmodIfExists(new File(rootDir, "generate_interfaces_file.exe"));
        chmodIfExists(new File(rootDir, "Steamless/Steamless.CLI.exe"));
        chmodIfExists(new File(rootDir, "opt/mono-gecko-offline/wine-mono-9.0.0-x86.msi"));
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
            File downloaded = new File(imageFs.getFilesDir(), imagefsArchive);

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
            boolean success = useAssetArchive
                    ? TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, context, imagefsArchive, rootDir, (file, size) -> {
                        if (size > 0 && onProgress != null && contentLength > 0) {
                            long totalSize = totalSizeRef.addAndGet(size);
                            onProgress.call((int) (((float) totalSize / contentLength) * 100));
                        }
                        return file;
                    })
                    : TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, downloaded, rootDir, (file, size) -> {
                        if (size > 0 && onProgress != null && contentLength > 0) {
                            long totalSize = totalSizeRef.addAndGet(size);
                            onProgress.call((int) (((float) totalSize / contentLength) * 100));
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
                imageFs.createImgVersionFile(LATEST_VERSION);
                imageFs.createVariantFile(containerVariant);
                if (container != null && container.getWineVersion() != null && !container.getWineVersion().trim().isEmpty()) {
                    imageFs.createArchFile(container.getWineVersion());
                }
                resetContainerImgVersions(context);
            } else if (downloaded.exists() && !useAssetArchive && !LEGACY_IMAGEFS_ARCHIVE.equals(imagefsArchive)) {
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
        ImageFs imageFs = ImageFs.find(context);
        String requestedVariant = resolveInstallVariant(imageFs, container);
        boolean variantMismatch = !imageFs.getVariant().isEmpty() && !requestedVariant.equalsIgnoreCase(imageFs.getVariant());
        if (!imageFs.isValid() || imageFs.getVersion() < LATEST_VERSION || variantMismatch) {
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
}
