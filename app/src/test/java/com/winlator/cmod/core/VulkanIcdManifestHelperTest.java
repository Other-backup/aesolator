package com.winlator.cmod.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VulkanIcdManifestHelperTest {
    @Test
    public void rewriteLibraryPathReplacesLibraryWithoutDroppingApiVersion() throws Exception {
        String manifest = "{\n" +
                "  \"file_format_version\": \"1.0.0\",\n" +
                "  \"ICD\": {\n" +
                "    \"library_path\": \"/root/usr/lib/libvulkan_wrapper.so\",\n" +
                "    \"api_version\": \"1.3.250\"\n" +
                "  }\n" +
                "}";

        String rewritten = VulkanIcdManifestHelper.rewriteLibraryPath(
                manifest,
                "/data/app/native/libvulkan_wrapper.so",
                null
        );

        assertTrue(rewritten.contains("\"library_path\": \"/data/app/native/libvulkan_wrapper.so\""));
        assertEquals(3, VulkanIcdManifestHelper.readApiMinor(rewritten));
    }

    @Test
    public void rewriteLibraryPathAppliesApiOverrideWhenRequested() throws Exception {
        String manifest = "{ \"ICD\": { \"library_path\": \"/old.so\", \"api_version\": \"1.2.198\" } }";

        String rewritten = VulkanIcdManifestHelper.rewriteLibraryPath(
                manifest,
                "/new.so",
                "1.1.282"
        );

        assertEquals(1, VulkanIcdManifestHelper.readApiMinor(rewritten));
        assertTrue(rewritten.contains("\"api_version\": \"1.1.282\""));
    }
}
