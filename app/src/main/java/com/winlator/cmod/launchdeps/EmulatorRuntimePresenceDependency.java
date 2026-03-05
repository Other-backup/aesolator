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
    public boolean isSatisfied(Context context, Container container, @Nullable Shortcut shortcut, @Nullable String appId) {
        return collectMissingParts(context, container, shortcut).isEmpty();
    }

    @Override
    public String getLoadingMessage(Context context, Container container, @Nullable Shortcut shortcut, @Nullable String appId) {
        List<String> missing = collectMissingParts(context, container, shortcut);
        if (missing.isEmpty()) return "Validating emulator runtime payloads";
        return "Missing emulator runtime payloads: " + String.join(", ", missing);
    }

    @Override
    public void install(Context context, Container container, @Nullable Shortcut shortcut, @Nullable String appId, LaunchDependencyCallbacks callbacks) {
        List<String> missing = collectMissingParts(context, container, shortcut);
        String detail = missing.isEmpty()
                ? "Emulator runtime validation failed"
                : "Missing emulator runtime payloads: " + String.join(", ", missing);
        callbacks.setLoadingMessage(detail);
        callbacks.setLoadingProgress(0f);
        throw new IllegalStateException(detail);
    }

    private List<String> collectMissingParts(Context context, Container container, @Nullable Shortcut shortcut) {
        ArrayList<String> missing = new ArrayList<>();
        ContentsManager manager = new ContentsManager(context);
        manager.syncContents();

        String selectedWine = resolveWineIdentifier(container, shortcut);
        if (selectedWine.isEmpty()) return missing;

        WineInfo wineInfo = WineInfo.fromIdentifier(context, manager, selectedWine);
        File imageFsRoot = ImageFs.find(context).getRootDir();

        if (wineInfo.isArm64EC()) {
            String wowbox64Version = resolveBox64Version(container, shortcut, true);
            String fexcoreVersion = resolveFexcoreVersion(container, shortcut);

            boolean wowbox64Ready = hasInstalledVersion(manager, ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64, wowbox64Version)
                    || hasEmbeddedArchive(context, "wowbox64/wowbox64-" + wowbox64Version + ".tzst")
                    || new File(imageFsRoot, "home/xuser/.wine/drive_c/windows/system32/wowbox64.dll").isFile();
            if (!wowbox64Ready) {
                missing.add("wowbox64-" + wowbox64Version);
            }

            boolean fexcoreReady = hasInstalledVersion(manager, ContentProfile.ContentType.CONTENT_TYPE_FEXCORE, fexcoreVersion)
                    || hasEmbeddedArchive(context, "fexcore/fexcore-" + fexcoreVersion + ".tzst")
                    || new File(imageFsRoot, "home/xuser/.wine/drive_c/windows/system32/libwow64fex.dll").isFile()
                    || new File(imageFsRoot, "home/xuser/.wine/drive_c/windows/system32/libarm64ecfex.dll").isFile();
            if (!fexcoreReady) {
                missing.add("fexcore-" + fexcoreVersion);
            }
            return missing;
        }

        String box64Version = resolveBox64Version(container, shortcut, false);
        boolean box64Ready = hasInstalledVersion(manager, ContentProfile.ContentType.CONTENT_TYPE_BOX64, box64Version)
                || hasEmbeddedArchive(context, "box64/box64-" + box64Version + ".tzst")
                || new File(imageFsRoot, "usr/bin/box64").isFile();
        if (!box64Ready) {
            missing.add("box64-" + box64Version);
        }
        return missing;
    }

    private boolean hasInstalledVersion(ContentsManager manager, ContentProfile.ContentType type, String version) {
        List<ContentProfile> profiles = manager.getProfiles(type);
        if (profiles == null || version == null || version.trim().isEmpty()) return false;
        for (ContentProfile profile : profiles) {
            if (profile == null || !profile.locallyInstalled) continue;
            if (version.equalsIgnoreCase(profile.verName)) return true;
        }
        return false;
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
