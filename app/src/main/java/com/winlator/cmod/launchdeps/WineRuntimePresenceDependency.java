package com.winlator.cmod.launchdeps;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.contents.ManifestComponentHelper;
import com.winlator.cmod.contents.ManifestEntry;
import com.winlator.cmod.contents.ManifestInstallResult;
import com.winlator.cmod.contents.ManifestInstaller;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.File;

final class WineRuntimePresenceDependency implements LaunchDependency {
    private static final String TAG = "WineRuntimePresence";

    @Override
    public String getId() {
        return "wine_runtime_presence";
    }

    @Override
    public boolean appliesTo(Container container, @Nullable Shortcut shortcut, @Nullable String appId) {
        return container != null;
    }

    @Override
    public boolean isSatisfied(LaunchDependencyContext dependencyContext, Container container, @Nullable Shortcut shortcut, @Nullable String appId) {
        Context context = dependencyContext.getContext();
        String selectedWine = resolveWineIdentifier(container, shortcut);
        if (selectedWine == null || selectedWine.trim().isEmpty()) {
            return true;
        }

        ContentsManager manager = dependencyContext.getContentsManager();
        String requestedRuntimeModel = ContentProfile.normalizeRuntimeModel(container.getContainerVariant());
        String inferredRuntimeModel = ContentProfile.inferRuntimeModelFromEntryName(selectedWine.trim());
        if (!inferredRuntimeModel.isEmpty()) {
            requestedRuntimeModel = inferredRuntimeModel;
        }
        String canonicalEntry = manager.resolveBestRuntimeEntry(selectedWine.trim(), requestedRuntimeModel);
        ContentProfile profile = manager.resolveBestRuntimeProfile(canonicalEntry, requestedRuntimeModel);
        if (profile != null && profile.isWineProtonFamily()) {
            return hasValidProfileLayout(context, manager, profile);
        }

        ImageFs imageFs = ImageFs.find(context, requestedRuntimeModel, canonicalEntry);
        File fallbackOptPath = WineUtils.resolveCanonicalRuntimeRoot(new File(imageFs.getRootDir(), "opt/" + canonicalEntry));
        if (fallbackOptPath.isDirectory()) {
            return WineUtils.hasRuntimeCorePayload(fallbackOptPath);
        }

        File mainOptPath = imageFs.getMainWineDir();
        if (WineUtils.hasRuntimeCorePayload(mainOptPath)) {
            if (!WineInfo.isMainWineVersion(canonicalEntry)) {
                Log.w(TAG, "Custom runtime missing, falling back to canonical main wine: " + canonicalEntry);
            }
            return true;
        }

        return false;
    }

    @Override
    public String getLoadingMessage(LaunchDependencyContext dependencyContext, Container container, @Nullable Shortcut shortcut, @Nullable String appId) {
        return "Validating Wine/Proton runtime package";
    }

    @Override
    public void install(LaunchDependencyContext dependencyContext, Container container, @Nullable Shortcut shortcut, @Nullable String appId, LaunchDependencyCallbacks callbacks) {
        Context context = dependencyContext.getContext();
        String selectedWine = resolveWineIdentifier(container, shortcut);
        ContentsManager manager = dependencyContext.getContentsManager();
        String requestedRuntimeModel = ContentProfile.normalizeRuntimeModel(container.getContainerVariant());
        String inferredRuntimeModel = ContentProfile.inferRuntimeModelFromEntryName(selectedWine);
        if (!inferredRuntimeModel.isEmpty()) {
            requestedRuntimeModel = inferredRuntimeModel;
        }
        String canonicalEntry = manager.resolveBestRuntimeEntry(selectedWine, requestedRuntimeModel);
        ContentProfile.ContentType manifestType = resolveManifestType(manager, canonicalEntry, selectedWine);
        ManifestEntry entry = ManifestComponentHelper.findManifestEntryForTypeVersion(
                dependencyContext.getManifest(),
                manifestType,
                canonicalEntry,
                requestedRuntimeModel
        );
        if (entry != null) {
            callbacks.setLoadingMessage("Installing runtime package from manifest: " + canonicalEntry);
            callbacks.setLoadingProgress(0f);
            ManifestInstallResult result = ManifestInstaller.downloadAndInstallContent(context, entry, manifestType, callbacks::setLoadingProgress);
            manager.syncContents();
            if (result.success) {
                return;
            }
            throw new IllegalStateException(result.message.isEmpty()
                    ? "Runtime package install failed: " + canonicalEntry
                    : result.message);
        }

        callbacks.setLoadingMessage("Missing Wine/Proton runtime package: " + canonicalEntry);
        callbacks.setLoadingProgress(0f);
        throw new IllegalStateException("Runtime package is missing or incomplete: " + canonicalEntry);
    }

    private String resolveWineIdentifier(Container container, @Nullable Shortcut shortcut) {
        String value = shortcut != null
                ? shortcut.getExtra("wineVersion", container.getWineVersion())
                : container.getWineVersion();
        if (AppUtils.isMissingComponentValue(value)) return "";
        return value == null ? "" : value.trim();
    }

    private boolean hasValidProfileLayout(Context context, ContentsManager manager, ContentProfile profile) {
        if (profile != null && profile.isWineProtonFamily()) {
            return manager.isInstalledProfileUsable(profile);
        }

        File installDir = ContentsManager.getInstallDir(context, profile);
        if (!installDir.isDirectory()) return false;

        File profileJson = new File(installDir, ContentsManager.PROFILE_NAME);
        if (!profileJson.isFile()) return false;

        return true;
    }

    private ContentProfile.ContentType resolveManifestType(ContentsManager manager, String canonicalEntry, String selectedWine) {
        ContentProfile resolvedProfile = manager.getProfileByEntryName(canonicalEntry);
        if (resolvedProfile != null && resolvedProfile.type != null) {
            return resolvedProfile.type;
        }
        String combined = ((canonicalEntry == null ? "" : canonicalEntry) + " " + (selectedWine == null ? "" : selectedWine))
                .toLowerCase();
        return combined.contains("proton")
                ? ContentProfile.ContentType.CONTENT_TYPE_PROTON
                : ContentProfile.ContentType.CONTENT_TYPE_WINE;
    }
}
