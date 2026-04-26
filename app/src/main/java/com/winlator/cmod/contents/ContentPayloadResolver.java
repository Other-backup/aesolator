package com.winlator.cmod.contents;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.winlator.cmod.core.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;

public final class ContentPayloadResolver {
    private static final int BUFFER_SIZE = 64 * 1024;

    private ContentPayloadResolver() {
    }

    public static boolean materialize(@NonNull Context context,
                                      @Nullable ContentProfile profile,
                                      @NonNull File outputFile) {
        if (profile == null || profile.remoteUrl == null || profile.remoteUrl.trim().isEmpty()) return false;
        return materialize(context, profile.remoteUrl, outputFile);
    }

    public static boolean materialize(@NonNull Context context,
                                      @NonNull String source,
                                      @NonNull File outputFile) {
        String normalizedSource = source.trim();
        if (normalizedSource.isEmpty()) return false;
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) return false;

        URI uri = parseUri(normalizedSource);
        String scheme = uri != null && uri.getScheme() != null
                ? uri.getScheme().trim().toLowerCase(Locale.US)
                : "";
        if ("asset".equals(scheme)) {
            return copyAsset(context, resolveAssetPath(uri), outputFile);
        }
        if ("file".equals(scheme)) {
            String path = uri.getPath();
            return path != null && FileUtils.copy(new File(path), outputFile);
        }
        if (scheme.isEmpty()) {
            File sourceFile = new File(normalizedSource);
            if (sourceFile.isFile()) return FileUtils.copy(sourceFile, outputFile);
        }
        return Downloader.downloadFile(normalizedSource, outputFile);
    }

    public static boolean isBundledAssetUrl(@Nullable String source) {
        URI uri = parseUri(source);
        return uri != null
                && uri.getScheme() != null
                && "asset".equals(uri.getScheme().trim().toLowerCase(Locale.US));
    }

    @NonNull
    private static String resolveAssetPath(@Nullable URI uri) {
        if (uri == null) return "";
        StringBuilder path = new StringBuilder();
        String host = uri.getHost();
        if (host != null && !host.trim().isEmpty()) path.append(host.trim());
        String rawPath = uri.getPath();
        if (rawPath != null && !rawPath.trim().isEmpty()) {
            String normalizedPath = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
            if (path.length() > 0 && !normalizedPath.isEmpty()) path.append('/');
            path.append(normalizedPath);
        }
        if (path.length() == 0) {
            String ssp = uri.getSchemeSpecificPart();
            if (ssp != null) path.append(ssp.replaceFirst("^/*", ""));
        }
        return path.toString();
    }

    private static boolean copyAsset(@NonNull Context context,
                                     @NonNull String assetPath,
                                     @NonNull File outputFile) {
        if (assetPath.trim().isEmpty()) return false;
        try (InputStream input = context.getAssets().open(assetPath);
             FileOutputStream output = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            output.flush();
            return outputFile.isFile() && outputFile.length() > 0L;
        } catch (Exception ignored) {
            if (outputFile.exists()) outputFile.delete();
            return false;
        }
    }

    @Nullable
    private static URI parseUri(@Nullable String source) {
        if (source == null || source.trim().isEmpty()) return null;
        try {
            return new URI(source.trim());
        } catch (Exception ignored) {
            return null;
        }
    }
}
