package com.winlator.cmod.contents;

import androidx.annotation.Nullable;

final class ContentProfileIdentity {
    private ContentProfileIdentity() {
    }

    static boolean isRemoteProfileIdentityMismatch(@Nullable ContentProfile profile, @Nullable ContentProfile remoteHint) {
        if (profile == null || remoteHint == null) return false;
        if (profile.type != null && remoteHint.type != null && profile.type != remoteHint.type) return true;

        String actualVersion = normalizeIdentityToken(profile.verName);
        String expectedVersion = normalizeIdentityToken(remoteHint.verName);
        return !actualVersion.isEmpty()
                && !expectedVersion.isEmpty()
                && !actualVersion.equalsIgnoreCase(expectedVersion);
    }

    private static String normalizeIdentityToken(String value) {
        return value == null ? "" : value.trim();
    }
}
