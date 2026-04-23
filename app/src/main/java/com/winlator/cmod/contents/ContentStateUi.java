package com.winlator.cmod.contents;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.winlator.cmod.R;

public final class ContentStateUi {
    private ContentStateUi() {
    }

    public static boolean isProfilePresentLocally(@NonNull Context context,
                                                  @Nullable ContentsManager manager,
                                                  @Nullable ContentProfile profile) {
        if (profile == null) return false;
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) {
            return new DgVoodooManager(context).matchesProfile(profile);
        }
        ContentsManager effectiveManager = resolveManager(context, manager);
        return effectiveManager != null && effectiveManager.isInstalledProfilePresent(profile);
    }

    public static boolean isProfileUsableLocally(@NonNull Context context,
                                                 @Nullable ContentsManager manager,
                                                 @Nullable ContentProfile profile) {
        if (profile == null) return false;
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) {
            return new DgVoodooManager(context).matchesProfile(profile);
        }
        ContentsManager effectiveManager = resolveManager(context, manager);
        return effectiveManager != null && effectiveManager.isInstalledProfileUsable(profile);
    }

    public static boolean isProfileBrokenLocally(@NonNull Context context,
                                                 @Nullable ContentsManager manager,
                                                 @Nullable ContentProfile profile) {
        if (profile == null || profile.type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) return false;
        ContentsManager effectiveManager = resolveManager(context, manager);
        return effectiveManager != null
                && effectiveManager.isInstalledProfilePresent(profile)
                && !effectiveManager.isInstalledProfileUsable(profile);
    }

    @NonNull
    public static String getStatusLabel(@NonNull Context context,
                                        @Nullable ContentsManager manager,
                                        @Nullable ContentProfile profile,
                                        boolean detailed) {
        if (profile == null) return context.getString(R.string.contents_state_remote_only);
        if (profile.type == ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO) {
            return isProfileUsableLocally(context, manager, profile)
                    ? context.getString(R.string.contents_state_installed)
                    : context.getString(R.string.contents_state_remote_only);
        }
        ContentsManager effectiveManager = resolveManager(context, manager);
        if (effectiveManager == null) return context.getString(R.string.contents_state_remote_only);

        ContentsManager.InstalledProfileState state = effectiveManager.resolveInstalledProfileState(profile);
        if (!state.present) return context.getString(R.string.contents_state_remote_only);
        if (state.usable) return context.getString(R.string.contents_state_installed);
        if (!detailed) return context.getString(R.string.contents_state_broken);
        return context.getString(R.string.contents_state_broken_detail, getBrokenReasonLabel(context, state.brokenReason));
    }

    @Nullable
    private static ContentsManager resolveManager(@NonNull Context context, @Nullable ContentsManager manager) {
        if (manager != null) return manager;
        try {
            ContentsManager fallback = new ContentsManager(context);
            fallback.syncContents();
            return fallback;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @NonNull
    private static String getBrokenReasonLabel(@NonNull Context context, @Nullable String brokenReason) {
        String normalized = brokenReason == null ? "" : brokenReason.trim();
        return switch (normalized) {
            case "missing_install_dir" -> context.getString(R.string.contents_state_broken_missing_install_dir);
            case "missing_profile_json" -> context.getString(R.string.contents_state_broken_missing_profile_json);
            case "missing_runtime_root" -> context.getString(R.string.contents_state_broken_missing_runtime_root);
            case "missing_runtime_payload" -> context.getString(R.string.contents_state_broken_missing_runtime_payload);
            case "missing_profile" -> context.getString(R.string.contents_state_broken_missing_profile);
            default -> context.getString(R.string.contents_state_broken_generic);
        };
    }
}
