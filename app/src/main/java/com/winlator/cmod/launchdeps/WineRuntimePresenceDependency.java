package com.winlator.cmod.launchdeps;

import android.content.Context;

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
        ContentProfile profile = manager.getProfileByEntryName(selectedWine.trim());
        if (profile != null && profile.isWineProtonFamily()) {
            return hasValidProfileLayout(context, profile);
        }

        File fallbackOptPath = new File(ImageFs.find(context).getRootDir(), "opt/" + selectedWine.trim());
        if (fallbackOptPath.isDirectory()) {
            File wineBinary = new File(fallbackOptPath, "bin/wine");
            return wineBinary.isFile();
        }

        if (WineInfo.isMainWineVersion(selectedWine)) {
            File mainOptPath = new File(ImageFs.find(context).getRootDir(), "opt/" + WineInfo.MAIN_WINE_VERSION.identifier());
            File mainWineBinary = new File(mainOptPath, "bin/wine");
            return mainWineBinary.isFile();
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
        callbacks.setLoadingMessage("Missing Wine/Proton runtime package: " + selectedWine);
        callbacks.setLoadingProgress(0f);
        throw new IllegalStateException("Runtime package is missing or incomplete: " + selectedWine);
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
