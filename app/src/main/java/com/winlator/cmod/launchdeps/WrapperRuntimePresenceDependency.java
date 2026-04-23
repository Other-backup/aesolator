package com.winlator.cmod.launchdeps;

import android.content.Context;

import androidx.annotation.Nullable;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.GraphicsDrivers;
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
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineUtils;

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
    public boolean isSatisfied(LaunchDependencyContext dependencyContext, Container container, @Nullable Shortcut shortcut, @Nullable String appId) {
        return collectMissingRequirements(dependencyContext, container, shortcut).isEmpty();
    }

    @Override
    public String getLoadingMessage(LaunchDependencyContext dependencyContext, Container container, @Nullable Shortcut shortcut, @Nullable String appId) {
        List<ManifestDependencyInstaller.RequiredContent> missing = collectMissingRequirements(dependencyContext, container, shortcut);
        if (missing.isEmpty()) return "Validating wrapper runtime payloads";
        return "Missing wrapper runtime payloads: " + ManifestDependencyInstaller.formatMissing(missing);
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
        String detail = "Missing wrapper runtime payloads: " + ManifestDependencyInstaller.formatMissing(unresolved);
        callbacks.setLoadingMessage(detail);
        callbacks.setLoadingProgress(0f);
        throw new IllegalStateException(detail);
    }

    private List<ManifestDependencyInstaller.RequiredContent> collectMissingRequirements(LaunchDependencyContext dependencyContext, Container container, @Nullable Shortcut shortcut) {
        ArrayList<ManifestDependencyInstaller.RequiredContent> missing = new ArrayList<>();
        Context context = dependencyContext.getContext();
        ContentsManager manager = dependencyContext.getContentsManager();

        String wrapper = resolveDxWrapper(container, shortcut).toLowerCase(Locale.ENGLISH);
        if (wrapper.isEmpty()) return missing;

        if (wrapper.contains("dxvk")) {
            KeyValueSet config = DXVKConfigDialog.parseConfig(resolveDxWrapperConfig(container, shortcut));
            String dxvkVersion = sanitizeVersion(config.get("version"), DefaultVersion.DXVK);
            if (!hasDxvkPayload(context, manager, dxvkVersion)) {
                missing.add(new ManifestDependencyInstaller.RequiredContent(
                        ContentProfile.ContentType.CONTENT_TYPE_DXVK,
                        dxvkVersion,
                        "dxvk-" + dxvkVersion
                ));
            }

            String vkd3dVersion = sanitizeVersion(config.get("vkd3dVersion"), "None");
            if (!"None".equalsIgnoreCase(vkd3dVersion) && !hasVkd3dPayload(context, manager, vkd3dVersion)) {
                missing.add(new ManifestDependencyInstaller.RequiredContent(
                        ContentProfile.ContentType.CONTENT_TYPE_VKD3D,
                        vkd3dVersion,
                        "vkd3d-" + vkd3dVersion
                ));
            }
        }

        if (wrapper.contains("dgvoodoo")) {
            DgVoodooManager dgVoodooManager = new DgVoodooManager(context);
            KeyValueSet config = DgVoodooConfigDialog.parseConfig(resolveDxWrapperConfig(container, shortcut));
            WineUtils.WindowsLaunchTarget launchTarget = WineUtils.resolveWindowsLaunchTarget(
                    container != null ? container.getRootDir() : null,
                    shortcut != null ? shortcut.path : ""
            );
            String wineVersion = resolveWineVersion(container, shortcut);
            WineInfo wineInfo = WineInfo.fromIdentifier(
                    context,
                    manager,
                    wineVersion,
                    ContentProfile.inferRuntimeModelFromEntryName(wineVersion)
            );
            String resolvedArch = dgVoodooManager.resolvePreferredArch(launchTarget, config.get("dgvoodooArch"), wineInfo);
            if (!dgVoodooManager.isArchInstalled(resolvedArch)) {
                String packageLane = DgVoodooManager.resolvePackageLaneForRuntimeArch(resolvedArch);
                missing.add(new ManifestDependencyInstaller.RequiredContent(
                        ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO,
                        packageLane,
                        "dgvoodoo-" + packageLane
                ));
            }

            String graphicsDriver = resolveGraphicsDriver(container, shortcut);
            if (GraphicsDrivers.isVortek(graphicsDriver)) {
                String dxvkVersion = DgVoodooConfigDialog.resolveCompanionDxvkVersion(
                        config,
                        resolvedArch,
                        true,
                        manager.getInstalledVersionNames(ContentProfile.ContentType.CONTENT_TYPE_DXVK, true)
                );
                if (!hasDxvkPayload(context, manager, dxvkVersion)) {
                    missing.add(new ManifestDependencyInstaller.RequiredContent(
                        ContentProfile.ContentType.CONTENT_TYPE_DXVK,
                        dxvkVersion,
                        "dxvk-" + dxvkVersion
                    ));
                }
            }
        }

        return missing;
    }

    private boolean hasDxvkPayload(Context context, ContentsManager manager, String version) {
        return manager.hasInstalledVersion(ContentProfile.ContentType.CONTENT_TYPE_DXVK, version, true)
                || hasEmbeddedArchive(context, "dxwrapper/dxvk-" + version + ".tzst");
    }

    private boolean hasVkd3dPayload(Context context, ContentsManager manager, String version) {
        return manager.hasInstalledVersion(ContentProfile.ContentType.CONTENT_TYPE_VKD3D, version, true)
                || hasEmbeddedArchive(context, "dxwrapper/vkd3d-" + version + ".tzst");
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

    private String resolveGraphicsDriver(Container container, @Nullable Shortcut shortcut) {
        String value = shortcut != null
                ? shortcut.getExtra("graphicsDriver", container.getGraphicsDriver())
                : container.getGraphicsDriver();
        return value == null ? "" : value.trim();
    }

    private String resolveWineVersion(Container container, @Nullable Shortcut shortcut) {
        String value = shortcut != null
                ? shortcut.getExtra("wineVersion", container.getWineVersion())
                : container.getWineVersion();
        return value == null ? "" : value.trim();
    }

    private String sanitizeVersion(String value, String fallback) {
        if (AppUtils.isMissingComponentValue(value)) return fallback;
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }
}
