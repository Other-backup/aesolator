package com.winlator.cmod.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AndroidBionicHostLdPathHelperTest {
    @Test
    public void buildDirectGuestLdLibraryPathPreservesRuntimeSegmentsAndDropsGuestUsrLib() {
        String current = "/runtime/lib:/runtime/lib64:/runtime/lib/wine:/root/usr/lib:/root/usr/lib64:/system/lib64";

        String actual = AndroidBionicHostLdPathHelper.buildDirectGuestLdLibraryPath(
                current,
                "/root/usr/lib",
                "/root/usr/lib64",
                "/root/usr/lib/android-host"
        );

        assertEquals(
                "/runtime/lib:/runtime/lib64:/runtime/lib/wine:/system/lib64:/apex/com.android.runtime/lib64:/apex/com.android.art/lib64:/root/usr/lib/android-host",
                actual
        );
    }

    @Test
    public void buildDirectGuestLdLibraryPathDeduplicatesTailAndTrimsSlashes() {
        String current = "/runtime/lib/:/system/lib64:/apex/com.android.runtime/lib64:/root/usr/lib/";

        String actual = AndroidBionicHostLdPathHelper.buildDirectGuestLdLibraryPath(
                current,
                "/root/usr/lib",
                "/root/usr/lib64",
                "/root/usr/lib/android-host/"
        );

        assertEquals(
                "/runtime/lib:/system/lib64:/apex/com.android.runtime/lib64:/apex/com.android.art/lib64:/root/usr/lib/android-host",
                actual
        );
    }

    @Test
    public void directGuestPathKeepsSystemEglAheadOfAndroidHostLibs() {
        String result = AndroidBionicHostLdPathHelper.buildDirectGuestLdLibraryPath(
                "/runtime/lib:/root/usr/lib:/root/usr/lib64:/root/usr/lib/android-host:/system/lib64:/root/usr/lib/android-host/wine-x11-egl-stub",
                "/root/usr/lib",
                "/root/usr/lib64",
                "/root/usr/lib/android-host"
        );

        assertEquals(
                "/runtime/lib:/system/lib64:/apex/com.android.runtime/lib64:/apex/com.android.art/lib64:/root/usr/lib/android-host",
                result
        );
        assertFalse(result.contains("wine-x11-egl-stub"));
    }
}
