package com.winlator.cmod.launchdeps;

import android.content.Context;

import androidx.annotation.Nullable;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contentdialog.DgVoodooConfigDialog;
import com.winlator.cmod.contentdialog.DXVKConfigDialog;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.contents.DgVoodooManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.KeyValueSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class WrapperRuntimePresenceDependency implements LaunchDependency {
    @Override
    public String getId() {
        return "wrapper_runtime_presence";
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
        if (missing.isEmpty()) return "Validating wrapper runtime payloads";
        return "Missing wrapper runtime payloads: " + String.join(", ", missing);
    }

    @Override
    public void install(Context context, Container container, @Nullable Shortcut shortcut, @Nullable String appId, LaunchDependencyCallbacks callbacks) {
        List<String> missing = collectMissingParts(context, container, shortcut);
        String detail = missing.isEmpty()
                ? "Wrapper runtime validation failed"
                : "Missing wrapper runtime payloads: " + String.join(", ", missing);
        callbacks.setLoadingMessage(detail);
        callbacks.setLoadingProgress(0f);
        throw new IllegalStateException(detail);
    }

    private List<String> collectMissingParts(Context context, Container container, @Nullable Shortcut shortcut) {
        ArrayList<String> missing = new ArrayList<>();
        ContentsManager manager = new ContentsManager(context);
        manager.syncContents();

        String wrapper = resolveDxWrapper(container, shortcut).toLowerCase(Locale.ENGLISH);
        if (wrapper.isEmpty()) return missing;

        if (wrapper.contains("dxvk")) {
            KeyValueSet config = DXVKConfigDialog.parseConfig(resolveDxWrapperConfig(container, shortcut));
            String dxvkVersion = sanitizeVersion(config.get("version"), DefaultVersion.DXVK);
            if (!hasDxvkPayload(context, manager, dxvkVersion)) {
                missing.add("dxvk-" + dxvkVersion);
            }

            String vkd3dVersion = sanitizeVersion(config.get("vkd3dVersion"), "None");
            if (!"None".equalsIgnoreCase(vkd3dVersion) && !hasVkd3dPayload(context, manager, vkd3dVersion)) {
                missing.add("vkd3d-" + vkd3dVersion);
            }
        }

        if (wrapper.contains("dgvoodoo")) {
            DgVoodooManager dgVoodooManager = new DgVoodooManager(context);
            KeyValueSet config = DgVoodooConfigDialog.parseConfig(resolveDxWrapperConfig(container, shortcut));
            String shortcutPath = shortcut != null ? shortcut.path : "";
            String resolvedArch = dgVoodooManager.resolvePreferredArch(shortcutPath, config.get("dgvoodooArch"));
            if (!dgVoodooManager.isArchInstalled(resolvedArch)) {
                missing.add("dgvoodoo-" + resolvedArch);
            }
        }

        return missing;
    }

    private boolean hasDxvkPayload(Context context, ContentsManager manager, String version) {
        return hasInstalledVersion(manager, ContentProfile.ContentType.CONTENT_TYPE_DXVK, version)
                || hasEmbeddedArchive(context, "dxwrapper/dxvk-" + version + ".tzst");
    }

    private boolean hasVkd3dPayload(Context context, ContentsManager manager, String version) {
        return hasInstalledVersion(manager, ContentProfile.ContentType.CONTENT_TYPE_VKD3D, version)
                || hasEmbeddedArchive(context, "dxwrapper/vkd3d-" + version + ".tzst");
    }

    private boolean hasInstalledVersion(ContentsManager manager, ContentProfile.ContentType type, String version) {
        List<ContentProfile> profiles = manager.getProfiles(type);
        if (profiles == null || version == null || version.trim().isEmpty()) return false;
        String versionLower = version.toLowerCase(Locale.ENGLISH);
        for (ContentProfile profile : profiles) {
            if (profile == null || !profile.locallyInstalled) continue;
            String profileVersion = profile.verName == null ? "" : profile.verName.toLowerCase(Locale.ENGLISH);
            if (profileVersion.equals(versionLower)) return true;
            if (!profileVersion.isEmpty() && versionLower.startsWith(profileVersion + "-")) return true;
            if (!versionLower.isEmpty() && profileVersion.startsWith(versionLower + "-")) return true;
        }
        return false;
    }

    private boolean hasEmbeddedArchive(Context context, String assetPath) {
        return FileUtils.getSize(context, assetPath) > 0;
    }

    private String resolveDxWrapper(Container container, @Nullable Shortcut shortcut) {
        String value = shortcut != null
                ? shortcut.getExtra("dxwrapper", container.getDXWrapper())
                : container.getDXWrapper();
        return value == null ? "" : value.trim();
    }

    private String resolveDxWrapperConfig(Container container, @Nullable Shortcut shortcut) {
        String value = shortcut != null
                ? shortcut.getExtra("dxwrapperConfig", container.getDXWrapperConfig())
                : container.getDXWrapperConfig();
        return value == null ? "" : value;
    }

    private String sanitizeVersion(String value, String fallback) {
        if (AppUtils.isMissingComponentValue(value)) return fallback;
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }
}
