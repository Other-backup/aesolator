package com.winlator.cmod.contents;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.winlator.cmod.R;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class ManifestInstaller {
    private static final long INSTALL_TIMEOUT_SECONDS = 240L;

    public interface ProgressListener {
        void onProgress(float progress);
    }

    public static ManifestInstallResult installManifestEntry(
            Context context,
            ManifestEntry entry,
            boolean isDriver,
            @Nullable ContentProfile.ContentType contentType,
            @Nullable ProgressListener progressListener
    ) {
        if (isDriver) {
            return downloadAndInstallDriver(context, entry, progressListener);
        }
        if (contentType == null) {
            throw new IllegalArgumentException("contentType must be provided when installing manifest content");
        }
        return downloadAndInstallContent(context, entry, contentType, progressListener);
    }

    public static ManifestInstallResult downloadAndInstallDriver(
            Context context,
            ManifestEntry entry,
            @Nullable ProgressListener progressListener
    ) {
        File destFile = null;
        try {
            if (progressListener != null) progressListener.onProgress(0f);
            destFile = new File(context.getCacheDir(), buildCacheName(entry));
            if (!Downloader.downloadFile(entry.url, destFile)) {
                return new ManifestInstallResult(
                        false,
                        context.getString(R.string.manifest_download_failed, entry.getDisplayName())
                );
            }
            if (progressListener != null) progressListener.onProgress(1f);

            String name = new AdrenotoolsManager(context).installDriver(Uri.fromFile(destFile));
            if (name == null || name.trim().isEmpty()) {
                return new ManifestInstallResult(
                        false,
                        context.getString(R.string.manifest_install_failed, entry.getDisplayName())
                );
            }
            return new ManifestInstallResult(
                    true,
                    context.getString(R.string.manifest_install_success, entry.getDisplayName())
            );
        } catch (Exception e) {
            return new ManifestInstallResult(
                    false,
                    context.getString(R.string.manifest_download_failed, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
            );
        } finally {
            if (destFile != null && destFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                destFile.delete();
            }
        }
    }

    public static ManifestInstallResult downloadAndInstallContent(
            Context context,
            ManifestEntry entry,
            ContentProfile.ContentType expectedType,
            @Nullable ProgressListener progressListener
    ) {
        File destFile = null;
        try {
            if (progressListener != null) progressListener.onProgress(0f);
            destFile = new File(context.getCacheDir(), buildCacheName(entry));
            if (!Downloader.downloadFile(entry.url, destFile)) {
                return new ManifestInstallResult(
                        false,
                        context.getString(R.string.manifest_download_failed, entry.getDisplayName())
                );
            }

            ContentsManager manager = new ContentsManager(context);
            manager.syncContents();
            ContentProfile remoteHint = buildRemoteHint(entry, expectedType);
            ContentProfile profile = extractContent(manager, Uri.fromFile(destFile), remoteHint);
            if (profile == null) {
                return new ManifestInstallResult(
                        false,
                        context.getString(R.string.manifest_install_failed, entry.getDisplayName())
                );
            }
            if (!finishInstall(manager, profile)) {
                return new ManifestInstallResult(
                        false,
                        context.getString(R.string.manifest_install_failed, entry.getDisplayName())
                );
            }
            if (progressListener != null) progressListener.onProgress(1f);
            return new ManifestInstallResult(
                    true,
                    context.getString(R.string.manifest_install_success, entry.getDisplayName())
            );
        } catch (Exception e) {
            return new ManifestInstallResult(
                    false,
                    context.getString(R.string.manifest_download_failed, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
            );
        } finally {
            if (destFile != null && destFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                destFile.delete();
            }
        }
    }

    @Nullable
    private static ContentProfile extractContent(ContentsManager manager, Uri uri, @Nullable ContentProfile remoteHint) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final ContentProfile[] profileHolder = new ContentProfile[1];
        manager.extraContentFile(uri, remoteHint, new ContentsManager.OnInstallFinishedCallback() {
            @Override
            public void onFailed(ContentsManager.InstallFailedReason reason, Exception e) {
                latch.countDown();
            }

            @Override
            public void onSucceed(ContentProfile profile) {
                profileHolder[0] = profile;
                latch.countDown();
            }
        });
        if (!latch.await(INSTALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            return null;
        }
        return profileHolder[0];
    }

    private static boolean finishInstall(ContentsManager manager, ContentProfile profile) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = new boolean[1];
        manager.finishInstallContent(profile, new ContentsManager.OnInstallFinishedCallback() {
            @Override
            public void onFailed(ContentsManager.InstallFailedReason reason, Exception e) {
                latch.countDown();
            }

            @Override
            public void onSucceed(ContentProfile profileArg) {
                success[0] = true;
                latch.countDown();
            }
        });
        return latch.await(INSTALL_TIMEOUT_SECONDS, TimeUnit.SECONDS) && success[0];
    }

    private static String buildCacheName(ManifestEntry entry) {
        String fromUrl = entry.url == null ? "" : entry.url.substring(entry.url.lastIndexOf('/') + 1).trim();
        if (!fromUrl.isEmpty()) return fromUrl;
        return entry.id.isEmpty() ? "manifest-component.pkg" : entry.id + ".pkg";
    }

    private static ContentProfile buildRemoteHint(ManifestEntry entry, ContentProfile.ContentType contentType) {
        ContentProfile profile = new ContentProfile();
        profile.type = contentType;
        profile.verName = entry.id;
        profile.verCode = 0;
        profile.desc = entry.getDisplayName();
        profile.remoteUrl = entry.url;
        profile.channel = deriveChannel(entry.id, entry.url);
        profile.delivery = ContentProfile.DELIVERY_REMOTE;
        profile.displayCategory = switch (contentType) {
            case CONTENT_TYPE_PROTON -> "Proton";
            case CONTENT_TYPE_WINE -> "Wine";
            case CONTENT_TYPE_VULKAN_SDK -> "Vulkan SDK";
            case CONTENT_TYPE_DGVOODOO -> "dgVoodoo";
            default -> contentType.toString();
        };
        profile.sourceRepo = "utkarshdalal/GameNative";
        profile.sourceFeed = ManifestRepository.GAMENATIVE_MANIFEST_URL;
        profile.sourceLabel = "GameNative Manifest";
        profile.artifactName = buildCacheName(entry);
        profile.releaseTag = extractReleaseTag(entry.url);
        if (contentType == ContentProfile.ContentType.CONTENT_TYPE_VULKAN_SDK) {
            profile.vulkanSdkVersion = entry.id;
        }
        return profile;
    }

    private static String deriveChannel(String id, String url) {
        String combined = ((id == null ? "" : id) + " " + (url == null ? "" : url)).toLowerCase(Locale.US);
        if (combined.contains("nightly") || combined.contains("bleeding-edge")) {
            return ContentProfile.CHANNEL_NIGHTLY;
        }
        if (combined.contains("beta") || combined.contains("rc")) {
            return ContentProfile.CHANNEL_BETA;
        }
        return ContentProfile.CHANNEL_STABLE;
    }

    private static String extractReleaseTag(String url) {
        if (url == null || url.isEmpty()) return "";
        String marker = "/releases/download/";
        int index = url.indexOf(marker);
        if (index < 0) return "";
        int start = index + marker.length();
        int slash = url.indexOf('/', start);
        if (slash < 0) return "";
        return url.substring(start, slash).trim();
    }
}
