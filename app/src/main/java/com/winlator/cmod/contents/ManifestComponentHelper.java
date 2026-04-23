package com.winlator.cmod.contents;

import android.content.Context;

import com.winlator.cmod.core.GPUHelper;
import com.winlator.cmod.core.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class ManifestComponentHelper {
    public static final class InstalledContentLists {
        public final List<String> dxvk;
        public final List<String> vkd3d;
        public final List<String> box64;
        public final List<String> wowBox64;
        public final List<String> fexcore;
        public final List<String> wine;
        public final List<String> proton;

        public InstalledContentLists(
                List<String> dxvk,
                List<String> vkd3d,
                List<String> box64,
                List<String> wowBox64,
                List<String> fexcore,
                List<String> wine,
                List<String> proton
        ) {
            this.dxvk = dxvk;
            this.vkd3d = vkd3d;
            this.box64 = box64;
            this.wowBox64 = wowBox64;
            this.fexcore = fexcore;
            this.wine = wine;
            this.proton = proton;
        }
    }

    public static final class InstalledContentListsAndDrivers {
        public final InstalledContentLists installed;
        public final List<String> installedDrivers;

        public InstalledContentListsAndDrivers(InstalledContentLists installed, List<String> installedDrivers) {
            this.installed = installed;
            this.installedDrivers = installedDrivers;
        }
    }

    public static final class ComponentAvailability {
        public final ManifestData manifest;
        public final InstalledContentLists installed;
        public final List<String> installedDrivers;

        public ComponentAvailability(ManifestData manifest, InstalledContentLists installed, List<String> installedDrivers) {
            this.manifest = manifest;
            this.installed = installed;
            this.installedDrivers = installedDrivers;
        }
    }

    public static final class VersionOption {
        public final String label;
        public final String id;
        public final boolean isManifest;
        public final boolean isInstalled;

        public VersionOption(String label, String id, boolean isManifest, boolean isInstalled) {
            this.label = label;
            this.id = id;
            this.isManifest = isManifest;
            this.isInstalled = isInstalled;
        }
    }

    public static final class VersionOptionList {
        public final List<String> labels;
        public final List<String> ids;
        public final List<Boolean> muted;

        public VersionOptionList(List<String> labels, List<String> ids, List<Boolean> muted) {
            this.labels = labels;
            this.ids = ids;
            this.muted = muted;
        }
    }

    public static final class DxvkContext {
        public final boolean isVortekLike;
        public final List<String> labels;
        public final List<String> ids;
        public final List<Boolean> muted;

        public DxvkContext(boolean isVortekLike, List<String> labels, List<String> ids, List<Boolean> muted) {
            this.isVortekLike = isVortekLike;
            this.labels = labels;
            this.ids = ids;
            this.muted = muted;
        }
    }

    public static List<ManifestEntry> filterManifestByVariant(List<ManifestEntry> entries, String variant) {
        ArrayList<ManifestEntry> filtered = new ArrayList<>();
        if (entries == null) return filtered;
        String normalizedVariant = variant == null ? "" : variant.trim().toLowerCase(Locale.ENGLISH);
        for (ManifestEntry entry : entries) {
            if (entry == null) continue;
            String entryVariant = entry.variant == null ? "" : entry.variant.trim().toLowerCase(Locale.ENGLISH);
            if (entryVariant.equals(normalizedVariant)) filtered.add(entry);
        }
        return filtered;
    }

    public static InstalledContentListsAndDrivers loadInstalledContentLists(Context context) {
        ArrayList<String> installedDrivers = new AdrenotoolsManager(context).enumarateInstalledDrivers();
        InstalledContentLists installedContent;
        try {
            ContentsManager manager = new ContentsManager(context);
            manager.syncContents();
            installedContent = new InstalledContentLists(
                    profilesToDisplay(manager, manager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_DXVK)),
                    profilesToDisplay(manager, manager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_VKD3D)),
                    profilesToDisplay(manager, manager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_BOX64)),
                    profilesToDisplay(manager, manager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64)),
                    profilesToDisplay(manager, manager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE)),
                    profilesToDisplay(manager, manager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_WINE)),
                    profilesToDisplay(manager, manager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_PROTON))
            );
        } catch (Exception ignored) {
            installedContent = new InstalledContentLists(
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        }
        return new InstalledContentListsAndDrivers(installedContent, installedDrivers);
    }

    public static ComponentAvailability loadComponentAvailability(Context context) {
        InstalledContentListsAndDrivers installed = loadInstalledContentLists(context);
        ManifestData manifest = ManifestRepository.loadManifest(context);
        return new ComponentAvailability(manifest, installed.installed, installed.installedDrivers);
    }

    public static List<String> buildAvailableVersions(List<String> base, List<String> installed, List<ManifestEntry> manifest) {
        LinkedHashSet<String> versions = new LinkedHashSet<>();
        if (base != null) versions.addAll(base);
        if (installed != null) versions.addAll(installed);
        if (manifest != null) {
            for (ManifestEntry entry : manifest) {
                if (entry != null && !entry.id.isEmpty()) versions.add(entry.id);
            }
        }
        return new ArrayList<>(versions);
    }

    public static String toManifestKey(ContentProfile.ContentType type) {
        if (type == null) return "";
        return switch (type) {
            case CONTENT_TYPE_DXVK -> ManifestContentTypes.DXVK;
            case CONTENT_TYPE_VKD3D -> ManifestContentTypes.VKD3D;
            case CONTENT_TYPE_BOX64 -> ManifestContentTypes.BOX64;
            case CONTENT_TYPE_WOWBOX64 -> ManifestContentTypes.WOWBOX64;
            case CONTENT_TYPE_FEXCORE -> ManifestContentTypes.FEXCORE;
            case CONTENT_TYPE_WINE -> ManifestContentTypes.WINE;
            case CONTENT_TYPE_PROTON -> ManifestContentTypes.PROTON;
            default -> "";
        };
    }

    public static List<ManifestEntry> getManifestEntriesForType(
            ManifestData manifest,
            ContentProfile.ContentType type,
            String variant
    ) {
        ArrayList<ManifestEntry> filtered = new ArrayList<>();
        if (manifest == null || type == null) return filtered;

        String manifestKey = toManifestKey(type);
        if (manifestKey.isEmpty()) return filtered;

        List<ManifestEntry> entries = manifest.getItems(manifestKey);
        String normalizedVariant = ContentProfile.normalizeRuntimeModel(variant);
        if (normalizedVariant.isEmpty()) {
            normalizedVariant = variant == null ? "" : variant.trim().toLowerCase(Locale.ENGLISH);
        }
        if (normalizedVariant.isEmpty()) {
            filtered.addAll(entries);
            return filtered;
        }

        for (ManifestEntry entry : entries) {
            if (entry == null) continue;
            String entryVariant = entry.variant == null ? "" : entry.variant.trim().toLowerCase(Locale.ENGLISH);
            if (normalizedVariant.equals(entryVariant)) filtered.add(entry);
        }
        if (!filtered.isEmpty()) return filtered;

        for (ManifestEntry entry : entries) {
            if (entry == null) continue;
            String entryVariant = entry.variant == null ? "" : entry.variant.trim().toLowerCase(Locale.ENGLISH);
            if (entryVariant.isEmpty()) filtered.add(entry);
        }
        if (!filtered.isEmpty()) return filtered;

        filtered.addAll(entries);
        return filtered;
    }

    public static VersionOptionList buildVersionOptionList(List<String> base, List<String> installed, List<ManifestEntry> manifest) {
        LinkedHashMap<String, VersionOption> options = new LinkedHashMap<>();
        LinkedHashSet<String> installedIds = new LinkedHashSet<>();
        if (installed != null) installedIds.addAll(installed);

        if (base != null) {
            for (String label : base) {
                if (label == null || label.trim().isEmpty()) continue;
                options.put(label, new VersionOption(label, label, false, installedIds.contains(label) || !installedIds.isEmpty()));
            }
        }
        if (installed != null) {
            for (String label : installed) {
                if (label == null || label.trim().isEmpty()) continue;
                options.put(label, new VersionOption(label, label, false, true));
            }
        }
        if (manifest != null) {
            for (ManifestEntry entry : manifest) {
                if (entry == null || entry.id.isEmpty()) continue;
                if (!options.containsKey(entry.id)) {
                    boolean isInstalled = versionExists(entry.id, installed)
                            || versionExists(entry.getDisplayName(), installed);
                    options.put(entry.id, new VersionOption(entry.id, entry.id, true, isInstalled));
                }
            }
        }

        ArrayList<String> labels = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        ArrayList<Boolean> muted = new ArrayList<>();
        for (VersionOption option : options.values()) {
            labels.add(option.label);
            ids.add(option.id);
            muted.add(option.isManifest && !option.isInstalled);
        }
        return new VersionOptionList(labels, ids, muted);
    }

    public static DxvkContext buildDxvkContext(
            String containerVariant,
            List<String> graphicsDrivers,
            int graphicsDriverIndex,
            List<String> dxWrappers,
            int dxWrapperIndex,
            boolean inspectionMode,
            boolean isBionicVariant,
            List<String> dxvkVersionsBase,
            VersionOptionList dxvkOptions
    ) {
        String driverType = StringUtils.parseIdentifier(graphicsDrivers != null && graphicsDriverIndex >= 0 && graphicsDriverIndex < graphicsDrivers.size()
                ? graphicsDrivers.get(graphicsDriverIndex)
                : "");
        boolean isVortekLike = "glibc".equalsIgnoreCase(containerVariant)
                && ("vortek".equals(driverType) || "adreno".equals(driverType) || "sd-8-elite".equals(driverType));

        String dxWrapper = StringUtils.parseIdentifier(dxWrappers != null && dxWrapperIndex >= 0 && dxWrapperIndex < dxWrappers.size()
                ? dxWrappers.get(dxWrapperIndex)
                : "");
        boolean isVKD3D = "vkd3d".equals(dxWrapper);

        List<String> constrainedLabels = List.of("1.10.3", "1.10.9-sarek", "1.9.2", "async-1.10.3");
        List<String> constrainedIds = new ArrayList<>();
        for (String label : constrainedLabels) constrainedIds.add(StringUtils.parseIdentifier(label));
        boolean useConstrained = !inspectionMode
                && isVortekLike
                && GPUHelper.vkGetApiVersionSafe() < GPUHelper.vkMakeVersion(1, 3, 0);

        List<String> labels;
        List<String> ids;
        List<Boolean> muted;
        if (useConstrained) {
            labels = constrainedLabels;
            ids = constrainedIds;
            muted = List.of(false, false, false, false);
        } else if (isBionicVariant) {
            labels = dxvkOptions.labels;
            ids = dxvkOptions.ids;
            muted = dxvkOptions.muted;
        } else {
            labels = dxvkVersionsBase;
            ArrayList<String> normalizedIds = new ArrayList<>();
            for (String label : dxvkVersionsBase) normalizedIds.add(StringUtils.parseIdentifier(label));
            ids = normalizedIds;
            muted = new ArrayList<>();
        }

        if (isVKD3D) {
            return new DxvkContext(isVortekLike, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        return new DxvkContext(isVortekLike, labels, ids, muted);
    }

    public static boolean versionExists(String version, List<String> available) {
        if (version == null || version.trim().isEmpty() || available == null) return false;
        String trimmed = version.trim();
        String trimmedId = StringUtils.parseIdentifier(trimmed);
        for (String candidate : available) {
            if (candidate == null) continue;
            if (candidate.equalsIgnoreCase(trimmed)) return true;
            String candidateId = StringUtils.parseIdentifier(candidate);
            if (candidateId.equals(trimmedId)) return true;
            if (candidate.toLowerCase(Locale.ENGLISH).startsWith(trimmed.toLowerCase(Locale.ENGLISH) + "-")
                    || trimmed.toLowerCase(Locale.ENGLISH).startsWith(candidate.toLowerCase(Locale.ENGLISH) + "-")) {
                return true;
            }
            if (candidateId.startsWith(trimmedId + "-") || trimmedId.startsWith(candidateId + "-")) {
                return true;
            }
        }
        return false;
    }

    public static ManifestEntry findManifestEntryForVersion(String version, List<ManifestEntry> entries) {
        if (version == null || version.trim().isEmpty() || entries == null) return null;
        String normalized = version.trim();
        String normalizedId = StringUtils.parseIdentifier(normalized);
        ManifestEntry best = null;
        int bestScore = -1;
        for (ManifestEntry entry : entries) {
            if (entry == null) continue;
            int score = scoreManifestEntryVersionMatch(normalized, normalizedId, entry);
            if (score > bestScore) {
                best = entry;
                bestScore = score;
            }
        }
        return best;
    }

    public static ManifestEntry findManifestEntryForTypeVersion(
            ManifestData manifest,
            ContentProfile.ContentType type,
            String version,
            String variant
    ) {
        return findManifestEntryForVersion(version, getManifestEntriesForType(manifest, type, variant));
    }

    private static int scoreManifestEntryVersionMatch(String normalized, String normalizedId, ManifestEntry entry) {
        if (entry == null) return -1;

        String entryId = entry.id == null ? "" : entry.id.trim();
        String entryName = entry.getDisplayName().trim();
        String entryIdNormalized = StringUtils.parseIdentifier(entryId);
        String entryNameNormalized = StringUtils.parseIdentifier(entryName);

        if (normalized.equalsIgnoreCase(entryId) || normalized.equalsIgnoreCase(entryName)) return 500;
        if (normalizedId.equals(entryIdNormalized) || normalizedId.equals(entryNameNormalized)) return 450;
        if (entryIdNormalized.startsWith(normalizedId + "-") || entryNameNormalized.startsWith(normalizedId + "-")) return 400;
        if (normalizedId.startsWith(entryIdNormalized + "-") || normalizedId.startsWith(entryNameNormalized + "-")) return 350;
        if (!entryIdNormalized.isEmpty() && entryIdNormalized.contains(normalizedId)) return 300;
        if (!entryNameNormalized.isEmpty() && entryNameNormalized.contains(normalizedId)) return 250;
        return -1;
    }

    private static ArrayList<String> profilesToDisplay(ContentsManager manager, List<ContentProfile> profiles) {
        ArrayList<String> display = new ArrayList<>();
        if (profiles == null) return display;
        for (ContentProfile profile : profiles) {
            if (profile == null) continue;
            if (manager == null || !manager.isInstalledProfileUsable(profile)) {
                continue;
            }
            String version = profile.verName == null ? "" : profile.verName.trim();
            if (!version.isEmpty()) display.add(version);
        }
        return display;
    }
}
