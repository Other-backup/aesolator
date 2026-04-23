package com.winlator.cmod.launchdeps;

import android.content.Context;

import androidx.annotation.Nullable;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class EmulatorRuntimePresenceDependency implements LaunchDependency {
    @Override
    public String getId() {
        return "emulator_runtime_presence";
    }

    @Override
    public boolean appliesTo(Container container, @Nullable Shortcut shortcut, @Nullable String appId) {
        return container != null;
    }

    @Override
    public boolean isSatisfied(LaunchDependencyContext dependencyContext, Container container, @Nullable Shortcut shortcut, @Nullable String appId) {
        return collectMissingRequirements(dependencyContext, container, shortcut).isEmpty();
    }

    @Override
    public String getLoadingMessage(LaunchDependencyContext dependencyContext, Container container, @Nullable Shortcut shortcut, @Nullable String appId) {
        List<ManifestDependencyInstaller.RequiredContent> missing = collectMissingRequirements(dependencyContext, container, shortcut);
        if (missing.isEmpty()) return "Validating emulator runtime payloads";
        return "Missing emulator runtime payloads: " + ManifestDependencyInstaller.formatMissing(missing);
    }

    @Override
    public void install(LaunchDependencyContext dependencyContext, Container container, @Nullable Shortcut shortcut, @Nullable String appId, LaunchDependencyCallbacks callbacks) {
        List<ManifestDependencyInstaller.RequiredContent> missing = collectMissingRequirements(dependencyContext, container, shortcut);
        if (!missing.isEmpty()) {
            ManifestDependencyInstaller.installAvailable(
                    dependencyContext,
                    container == null ? "" : container.getContainerVariant(),
                    missing,
                    callbacks
            );
        }
        List<ManifestDependencyInstaller.RequiredContent> unresolved = collectMissingRequirements(dependencyContext, container, shortcut);
        if (unresolved.isEmpty()) {
            callbacks.setLoadingProgress(1f);
            return;
        }
        String detail = "Missing emulator runtime payloads: " + ManifestDependencyInstaller.formatMissing(unresolved);
        callbacks.setLoadingMessage(detail);
        callbacks.setLoadingProgress(0f);
        throw new IllegalStateException(detail);
    }

    private List<ManifestDependencyInstaller.RequiredContent> collectMissingRequirements(LaunchDependencyContext dependencyContext, Container container, @Nullable Shortcut shortcut) {
        ArrayList<ManifestDependencyInstaller.RequiredContent> missing = new ArrayList<>();
        Context context = dependencyContext.getContext();
        ContentsManager manager = dependencyContext.getContentsManager();

        String selectedWine = resolveWineIdentifier(container, shortcut);
        if (selectedWine.isEmpty()) return missing;

        String requestedRuntimeModel = ContentProfile.normalizeRuntimeModel(container.getContainerVariant());
        String inferredRuntimeModel = ContentProfile.inferRuntimeModelFromEntryName(selectedWine);
        if (!inferredRuntimeModel.isEmpty()) {
            requestedRuntimeModel = inferredRuntimeModel;
        }
        WineInfo wineInfo = WineInfo.fromIdentifier(context, manager, selectedWine, requestedRuntimeModel);
        ImageFs imageFs = ImageFs.find(context);
        File imageFsRoot = imageFs.getRootDir();
        File winePrefixDir = container != null && container.getRootDir() != null
                ? new File(container.getRootDir(), ".wine")
                : imageFs.getWinePrefixDir();

        if (wineInfo.isArm64EC()) {
            String wowbox64Version = resolveBox64Version(container, shortcut, true);
            String fexcoreVersion = resolveFexcoreVersion(container, shortcut);

            boolean wowbox64Ready = manager.hasInstalledVersion(ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64, wowbox64Version, true)
                    || hasEmbeddedArchive(context, "wowbox64/wowbox64-" + wowbox64Version + ".tzst")
                    || new File(winePrefixDir, "drive_c/windows/system32/wowbox64.dll").isFile();
            if (!wowbox64Ready) {
                missing.add(new ManifestDependencyInstaller.RequiredContent(
                        ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64,
                        wowbox64Version,
                        "wowbox64-" + wowbox64Version
                ));
            }

            boolean fexcoreReady = manager.hasInstalledVersion(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE, fexcoreVersion, true)
                    || hasEmbeddedArchive(context, "fexcore/fexcore-" + fexcoreVersion + ".tzst")
                    || new File(winePrefixDir, "drive_c/windows/system32/libwow64fex.dll").isFile()
                    || new File(winePrefixDir, "drive_c/windows/system32/libarm64ecfex.dll").isFile();
            if (!fexcoreReady) {
                missing.add(new ManifestDependencyInstaller.RequiredContent(
                        ContentProfile.ContentType.CONTENT_TYPE_FEXCORE,
                        fexcoreVersion,
                        "fexcore-" + fexcoreVersion
                ));
            }
            return missing;
        }

        String box64Version = resolveBox64Version(container, shortcut, false);
        boolean box64Ready = manager.hasInstalledVersion(ContentProfile.ContentType.CONTENT_TYPE_BOX64, box64Version, true)
                || hasEmbeddedArchive(context, "box64/box64-" + box64Version + ".tzst")
                || new File(imageFsRoot, "usr/bin/box64").isFile();
        if (!box64Ready) {
            missing.add(new ManifestDependencyInstaller.RequiredContent(
                    ContentProfile.ContentType.CONTENT_TYPE_BOX64,
                    box64Version,
                    "box64-" + box64Version
            ));
        }
        return missing;
    }

    private boolean hasEmbeddedArchive(Context context, String assetPath) {
        return FileUtils.getSize(context, assetPath) > 0;
    }

    private String resolveWineIdentifier(Container container, @Nullable Shortcut shortcut) {
        String value = shortcut != null
                ? shortcut.getExtra("wineVersion", container.getWineVersion())
                : container.getWineVersion();
        if (AppUtils.isMissingComponentValue(value)) return "";
        return value == null ? "" : value.trim();
    }

    private String resolveBox64Version(Container container, @Nullable Shortcut shortcut, boolean arm64ecLane) {
        String value = shortcut != null
                ? shortcut.getExtra("box64Version", container.getBox64Version())
                : container.getBox64Version();
        if (AppUtils.isMissingComponentValue(value)) {
            return arm64ecLane ? DefaultVersion.WOWBOX64 : DefaultVersion.BOX64;
        }
        return value == null || value.trim().isEmpty()
                ? (arm64ecLane ? DefaultVersion.WOWBOX64 : DefaultVersion.BOX64)
                : value.trim();
    }

    private String resolveFexcoreVersion(Container container, @Nullable Shortcut shortcut) {
        String value = shortcut != null
                ? shortcut.getExtra("fexcoreVersion", container.getFEXCoreVersion())
                : container.getFEXCoreVersion();
        if (AppUtils.isMissingComponentValue(value)) return DefaultVersion.FEXCORE;
        return value == null || value.trim().isEmpty() ? DefaultVersion.FEXCORE : value.trim();
    }
}
