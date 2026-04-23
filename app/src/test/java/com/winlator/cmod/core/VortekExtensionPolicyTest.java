package com.winlator.cmod.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class VortekExtensionPolicyTest {
    @Test
    public void candidateSurfaceMergesVortekAndroidAndLiveIcdExtensions() {
        String[] candidates = VortekExtensionPolicy.buildCandidateExtensions(new String[] {
                "VK_VENDOR_live_mali_extension",
                "VK_KHR_swapchain"
        });

        assertTrue(Arrays.asList(candidates).contains("VK_EXT_extended_dynamic_state3"));
        assertTrue(Arrays.asList(candidates).contains("VK_ANDROID_external_memory_android_hardware_buffer"));
        assertTrue(Arrays.asList(candidates).contains("VK_VENDOR_live_mali_extension"));
    }

    @Test
    public void maliCompatProfileKeepsMaliRouteButFiltersFragileExtensions() {
        String[] candidates = {
                "VK_KHR_swapchain",
                "VK_EXT_extended_dynamic_state",
                "VK_KHR_push_descriptor",
                "VK_VENDOR_live_mali_extension"
        };

        String[] selected = VortekExtensionPolicy.getSelectedExtensionsForProfile(
                VortekExtensionPolicy.PROFILE_MALI_COMPAT,
                candidates
        );

        assertArrayEquals(new String[] {"VK_KHR_swapchain", "VK_VENDOR_live_mali_extension"}, selected);
    }

    @Test
    public void maximumProfilePreservesFullExtensionSurface() {
        String[] candidates = {
                "VK_KHR_swapchain",
                "VK_KHR_push_descriptor"
        };

        assertArrayEquals(
                candidates,
                VortekExtensionPolicy.getSelectedExtensionsForProfile(VortekExtensionPolicy.PROFILE_MAXIMUM, candidates)
        );
    }

    @Test
    public void normalizesUserFacingProfileNames() {
        assertEquals(VortekExtensionPolicy.PROFILE_MALI_SYSTEM, VortekExtensionPolicy.normalizeProfile("Mali system Vulkan"));
        assertEquals(VortekExtensionPolicy.PROFILE_MALI_COMPAT, VortekExtensionPolicy.normalizeProfile("Mali compatibility"));
        assertEquals(VortekExtensionPolicy.PROFILE_MAXIMUM, VortekExtensionPolicy.normalizeProfile("all"));
    }
}
