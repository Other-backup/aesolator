package com.winlator.cmod.contentdialog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.winlator.cmod.container.GraphicsDrivers;

import org.junit.Test;

public class VortekConfigDialogTest {
    @Test
    public void requiresRestartWhenRoutingOrExtensionSurfaceChanges() {
        String oldConfig =
                "vulkanDriverEntry=system," +
                "vortekPackageVersion=1.0," +
                "gladioPackageVersion=aemali-gallium:26.1," +
                "routingMode=auto," +
                "extensionProfile=mali-system," +
                "exposedDeviceExtensions=VK_KHR_surface|VK_KHR_swapchain," +
                "galliumDriver=panfrost," +
                "glVersion=3.1";
        String newConfig =
                "vulkanDriverEntry=system," +
                "vortekPackageVersion=1.0," +
                "gladioPackageVersion=aemali-gallium:26.1," +
                "routingMode=vulkan-first," +
                "extensionProfile=mali-system," +
                "exposedDeviceExtensions=VK_KHR_surface|VK_KHR_swapchain|VK_EXT_tooling_info," +
                "galliumDriver=panfrost," +
                "glVersion=3.1";

        assertTrue(GraphicsDrivers.isMediaTekConfigRestartRequired(oldConfig, newConfig));
    }

    @Test
    public void ignoresEquivalentExtraMesaMaskOrdering() {
        String oldConfig =
                "vulkanDriverEntry=system," +
                "vortekPackageVersion=1.0," +
                "gladioPackageVersion=aemali-gallium:26.1," +
                "routingMode=auto," +
                "galliumDriver=panfrost," +
                "glVersion=3.1," +
                "extraDisabledExtensions=GL_EXT_debug_label|GL_KHR_no_error";
        String newConfig =
                "vulkanDriverEntry=system," +
                "vortekPackageVersion=1.0," +
                "gladioPackageVersion=aemali-gallium:26.1," +
                "routingMode=auto," +
                "galliumDriver=panfrost," +
                "glVersion=3.1," +
                "extraDisabledExtensions=GL_KHR_no_error GL_EXT_debug_label";

        assertFalse(GraphicsDrivers.isMediaTekConfigRestartRequired(oldConfig, newConfig));
    }
}
