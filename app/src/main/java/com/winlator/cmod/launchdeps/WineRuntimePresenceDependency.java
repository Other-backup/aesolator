package com.winlator.cmod.launchdeps;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.WineInfo;
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
    public boolean isSatisfied(Context context, Container container, @Nullable Shortcut shortcut, @Nullable String appId) {
        String selectedWine = resolveWineIdentifier(container, shortcut);
        if (selectedWine == null || selectedWine.trim().isEmpty()) {
            return true;
        }

        ContentsManager manager = new ContentsManager(context);
        manager.syncContents();
        String requestedRuntimeModel = ContentProfile.normalizeRuntimeModel(container.getContainerVariant());
        String inferredRuntimeModel = ContentProfile.inferRuntimeModelFromEntryName(selectedWine.trim());
        if (!inferredRuntimeModel.isEmpty()) {
            requestedRuntimeModel = inferredRuntimeModel;
        }
        String canonicalEntry = manager.resolveBestRuntimeEntry(selectedWine.trim(), requestedRuntimeModel);
        ContentProfile profile = manager.resolveBestRuntimeProfile(canonicalEntry, requestedRuntimeModel);
        if (profile != null && profile.isWineProtonFamily()) {
            return hasValidProfileLayout(context, profile);
        }

        File fallbackOptPath = new File(ImageFs.find(context).getRootDir(), "opt/" + canonicalEntry);
        if (fallbackOptPath.isDirectory()) {
            File wineBinary = new File(fallbackOptPath, "bin/wine");
            return wineBinary.isFile();
        }

        File mainOptPath = ImageFs.find(context).getMainWineDir();
        File mainWineBinary = new File(mainOptPath, "bin/wine");
        if (mainWineBinary.isFile()) {
            if (!WineInfo.isMainWineVersion(canonicalEntry)) {
                Log.w(TAG, "Custom runtime missing, falling back to canonical main wine: " + canonicalEntry);
            }
            return true;
        }

        return false;
    }

    @Override
    public String getLoadingMessage(Context context, Container container, @Nullable Shortcut shortcut, @Nullable String appId) {
        return "Validating Wine/Proton runtime package";
    }

    @Override
    public void install(Context context, Container container, @Nullable Shortcut shortcut, @Nullable String appId, LaunchDependencyCallbacks callbacks) {
        String selectedWine = resolveWineIdentifier(container, shortcut);
        ContentsManager manager = new ContentsManager(context);
        manager.syncContents();
        String requestedRuntimeModel = ContentProfile.normalizeRuntimeModel(container.getContainerVariant());
        String inferredRuntimeModel = ContentProfile.inferRuntimeModelFromEntryName(selectedWine);
        if (!inferredRuntimeModel.isEmpty()) {
            requestedRuntimeModel = inferredRuntimeModel;
        }
        String canonicalEntry = manager.resolveBestRuntimeEntry(selectedWine, requestedRuntimeModel);
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

    private boolean hasValidProfileLayout(Context context, ContentProfile profile) {
        File installDir = ContentsManager.getInstallDir(context, profile);
        if (!installDir.isDirectory()) return false;

        File profileJson = new File(installDir, ContentsManager.PROFILE_NAME);
        if (!profileJson.isFile()) return false;

        if (!profile.isWineProtonFamily()) return true;

        File binDir = new File(installDir, profile.wineBinPath == null ? "" : profile.wineBinPath);
        File libDir = new File(installDir, profile.wineLibPath == null ? "" : profile.wineLibPath);
        File prefixPack = new File(installDir, profile.winePrefixPack == null ? "" : profile.winePrefixPack);

        return binDir.isDirectory() && libDir.isDirectory() && prefixPack.isFile();
    }
}
