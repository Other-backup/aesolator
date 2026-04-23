package com.winlator.cmod.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.winlator.cmod.contentdialog.GraphicsDriverConfigDialog;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

public class GraphicsDriversTest {
    @Test
    public void selectsLatestBundledVortekArchive() {
        GraphicsDrivers.BundledDriverAsset asset = GraphicsDrivers.resolveBundledDriverAssetFromFileNames(
                GraphicsDrivers.VORTEK,
                Arrays.asList("vortek-2.0.tzst", "vortek-2.1.tzst", "vortek-2.1.3.tzst", "gladio-1.0.tzst")
        );

        assertNotNull(asset);
        assertEquals("2.1.3", asset.version);
        assertEquals("graphics_driver/vortek-2.1.3.tzst", asset.assetPath);
        assertEquals("usr/share/aesolator/graphics_driver/vortek-2.1.3.tzst.installed", asset.extractProbePath);
    }

    @Test
    public void buildsSelectableEntriesFromBundledAssetsOnly() {
        assertEquals(
                Arrays.asList("Wrapper", "Vortek", "VirGL"),
                GraphicsDrivers.buildSelectableEntriesFromFileNames(
                        Arrays.asList(
                                "wrapper.tzst",
                                "vortek-2.1.3.tzst",
                                "virgl-26.1.0.tzst",
                                "gladio-1.0.tzst",
                                "zink-26.1.0.tzst",
                                "turnip-25.3.0.tzst"
                        )
                )
        );
    }

    @Test
    public void exposesAeMaliGalliumWhenBundledAssetExists() {
        assertEquals(
                Arrays.asList("Wrapper", "Vortek", "VirGL"),
                GraphicsDrivers.buildSelectableEntriesFromFileNames(
                        Arrays.asList(
                                "wrapper.tzst",
                                "vortek-2.1.3.tzst",
                                "gladio-1.0.tzst",
                                "aemali-gallium-26.1-staging-abcdef.tzst",
                                "zink-26.1.0.tzst",
                                "virgl-26.1.0.tzst"
                        )
                )
        );
    }

    @Test
    public void usesAssetOwnedProbePathForBundledAeMaliGallium() {
        GraphicsDrivers.BundledDriverAsset asset = GraphicsDrivers.resolveBundledDriverAssetFromFileNames(
                GraphicsDrivers.AEMALI_GALLIUM,
                Arrays.asList("aemali-gallium-26.1-staging-abcdef.tzst")
        );

        assertNotNull(asset);
        assertEquals("usr/lib/libEGL.so", asset.extractProbePath);
    }

    @Test
    public void resolvesRequestedBundledVersionWhenAvailable() {
        GraphicsDrivers.BundledDriverAsset asset = GraphicsDrivers.resolveBundledDriverAssetFromFileNames(
                GraphicsDrivers.VORTEK,
                "2.0",
                Arrays.asList("vortek-2.0.tzst", "vortek-2.1.tzst")
        );

        assertNotNull(asset);
        assertEquals("2.0", asset.version);
    }

    @Test
    public void normalizesWrapperGalliumDriverToMesaContract() {
        assertEquals("zink", GraphicsDrivers.getWrapperGalliumDriver(""));
        assertEquals("zink", GraphicsDrivers.getWrapperGalliumDriver("Zink"));
        assertEquals("freedreno", GraphicsDrivers.getWrapperGalliumDriver("freedreno"));
        assertEquals("zink", GraphicsDrivers.getWrapperGalliumDriver("virpipe"));
    }

    @Test
    public void skipsUnknownArchivesWhenBuildingBuiltInEntries() {
        assertEquals(
                Arrays.asList("Wrapper"),
                GraphicsDrivers.buildSelectableEntriesFromFileNames(
                        Arrays.asList(
                                "wrapper.tzst",
                                "wrapper-v2.tzst",
                                "turnip-25.3.0.tzst",
                                "adrenotools-v819.tzst"
                        )
                )
        );
    }

    @Test
    public void comparesNumericAndSuffixVersionsCorrectly() {
        assertTrue(GraphicsDrivers.compareBundledVersions("2.1", "2.0") > 0);
        assertTrue(GraphicsDrivers.compareBundledVersions("25.3.0_R11", "25.3.0_R8") > 0);
    }

    @Test
    public void aliasesLegacyMediaTekTopLevelSelectionsToVortek() {
        assertEquals("vortek", GraphicsDrivers.getTopLevelSelectableDriver("vortek"));
        assertEquals("vortek", GraphicsDrivers.getTopLevelSelectableDriver("gladio"));
        assertEquals("vortek", GraphicsDrivers.getTopLevelSelectableDriver("aemali-gallium"));
        assertEquals("wrapper", GraphicsDrivers.getTopLevelSelectableDriver("zink"));
    }

    @Test
    public void migratesAeMaliGalliumConfigIntoUnifiedVortekSpace() {
        String migrated = GraphicsDrivers.migrateToUnifiedTopLevelConfig(
                GraphicsDrivers.AEMALI_GALLIUM,
                "packageVersion=custom:ae-driver-26,galliumDriver=lima,glVersion=3.2,disableGLKHRDebug=0,disableVertexArrayBGRA=1"
        );

        HashMap<String, String> config = GraphicsDrivers.parseConfig(GraphicsDrivers.VORTEK, migrated);
        assertEquals("aemali-gallium-custom:ae-driver-26", config.get("gladioPackageVersion"));
        assertEquals("opengl-first", config.get("routingMode"));
        assertEquals("lima", config.get("galliumDriver"));
        assertEquals("3.2", config.get("glVersion"));
        assertEquals("0", config.get("disableGLKHRDebug"));
        assertEquals("1", config.get("disableVertexArrayBGRA"));
    }

    @Test
    public void migratesLegacyZinkConfigIntoWrapperSpace() {
        String migrated = GraphicsDrivers.migrateToUnifiedTopLevelConfig(
                GraphicsDrivers.ZINK,
                "glVersion=3.3,galliumDriver=zink,disableGLKHRDebug=0,disableVertexArrayBGRA=1"
        );

        HashMap<String, String> config = GraphicsDriverConfigDialog.parseGraphicsDriverConfig(migrated);
        assertEquals("zink", config.get("galliumDriver"));
        assertEquals("3.3", config.get("glVersion"));
        assertEquals("0", config.get("disableGLKHRDebug"));
        assertEquals("1", config.get("disableVertexArrayBGRA"));
    }

    @Test
    public void normalizesGraphicsExtensionListsToDistinctGlTokens() {
        assertEquals(
                "GL_EXT_debug_label|GL_KHR_no_error|GL_EXT_texture_filter_anisotropic",
                GraphicsDrivers.normalizeGraphicsExtensionList(
                        "GL_EXT_debug_label, bad_token GL_KHR_no_error\nGL_EXT_debug_label GL_EXT_texture_filter_anisotropic"
                )
        );
    }

    @Test
    public void buildsMesaExtensionOverrideFromBuiltinsAndExtraMasks() {
        assertEquals(
                "-GL_KHR_debug -GL_EXT_vertex_array_bgra -GL_EXT_debug_label -GL_KHR_no_error",
                GraphicsDrivers.buildMesaExtensionOverride(
                        true,
                        true,
                        "GL_EXT_debug_label GL_KHR_no_error GL_EXT_debug_label"
                )
        );
    }
}
