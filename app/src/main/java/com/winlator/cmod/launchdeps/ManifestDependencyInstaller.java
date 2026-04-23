package com.winlator.cmod.launchdeps;

import androidx.annotation.Nullable;

import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.contents.ManifestComponentHelper;
import com.winlator.cmod.contents.ManifestEntry;
import com.winlator.cmod.contents.ManifestInstallResult;
import com.winlator.cmod.contents.ManifestInstaller;

import java.util.List;

final class ManifestDependencyInstaller {
    static final class RequiredContent {
        final ContentProfile.ContentType type;
        final String version;
        final String label;

        RequiredContent(ContentProfile.ContentType type, String version, String label) {
            this.type = type;
            this.version = version == null ? "" : version.trim();
            this.label = label == null || label.trim().isEmpty() ? this.version : label.trim();
        }
    }

    private ManifestDependencyInstaller() {
    }

    static String formatMissing(List<RequiredContent> required) {
        if (required == null || required.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (RequiredContent item : required) {
            if (item == null || item.label.isEmpty()) continue;
            if (builder.length() > 0) builder.append(", ");
            builder.append(item.label);
        }
        return builder.toString();
    }

    static void installAvailable(
            LaunchDependencyContext dependencyContext,
            @Nullable String variant,
            List<RequiredContent> required,
            LaunchDependencyCallbacks callbacks
    ) {
        if (dependencyContext == null || required == null || required.isEmpty()) return;

        ContentsManager manager = dependencyContext.getContentsManager();
        int total = required.size();
        for (int i = 0; i < required.size(); i++) {
            RequiredContent item = required.get(i);
            if (item == null || item.type == null || item.version.isEmpty()) continue;
            if (manager.hasInstalledVersion(item.type, item.version, true)) continue;

            ManifestEntry entry = ManifestComponentHelper.findManifestEntryForTypeVersion(
                    dependencyContext.getManifest(),
                    item.type,
                    item.version,
                    variant
            );
            if (entry == null) continue;

            int index = i;
            callbacks.setLoadingMessage("Installing " + item.label + " from manifest");
            callbacks.setLoadingProgress(total <= 0 ? 0f : (float) index / (float) total);
            ManifestInstallResult result = ManifestInstaller.downloadAndInstallContent(
                    dependencyContext.getContext(),
                    entry,
                    item.type,
                    progress -> {
                        float bounded = Math.max(0f, Math.min(progress, 1f));
                        callbacks.setLoadingProgress(((float) index + bounded) / (float) total);
                    }
            );
            if (!result.success) {
                throw new IllegalStateException(result.message.isEmpty()
                        ? "Failed to install " + item.label
                        : result.message);
            }
            manager.syncContents();
        }
        callbacks.setLoadingProgress(1f);
    }
}
