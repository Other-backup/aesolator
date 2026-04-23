package com.winlator.cmod.container;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ContainerGraphicsDriverCompatTest {
    @Test
    public void preservesFirstClassBuiltInGraphicsDrivers() {
        assertEquals("gladio", Container.normalizeGraphicsDriver("gladio"));
        assertEquals("gladio", Container.normalizeGraphicsDriver("gladium"));
        assertEquals("vortek", Container.normalizeGraphicsDriver("vortek"));
        assertEquals("virgl", Container.normalizeGraphicsDriver("virgl"));
    }

    @Test
    public void normalizesLegacyDonorGraphicsDriverToWrapper() {
        assertEquals("wrapper", Container.normalizeGraphicsDriver("turnip,gladio"));
        assertEquals("zink", Container.normalizeGraphicsDriver("zink"));
        assertEquals("wrapper", Container.normalizeGraphicsDriver("llvmpipe"));
    }

    @Test
    public void extractsLegacyGraphicsProviderHint() {
        assertEquals("turnip-vulkan", Container.extractLegacyGraphicsProviderHint("turnip,gladio"));
        assertEquals("", Container.extractLegacyGraphicsProviderHint("zink"));
        assertEquals("llvmpipe-software", Container.extractLegacyGraphicsProviderHint("llvmpipe"));
        assertEquals("", Container.extractLegacyGraphicsProviderHint("gladio"));
        assertEquals("", Container.extractLegacyGraphicsProviderHint("vortek"));
        assertEquals("", Container.extractLegacyGraphicsProviderHint("virgl"));
        assertEquals("", Container.extractLegacyGraphicsProviderHint("wrapper"));
    }

    @Test
    public void classifiesLegacyGraphicsPolicy() {
        assertEquals("provider-compat", Container.extractLegacyGraphicsPolicy("turnip"));
        assertEquals("software-secondary path", Container.extractLegacyGraphicsPolicy("llvmpipe"));
        assertEquals("", Container.extractLegacyGraphicsPolicy("zink"));
        assertEquals("", Container.extractLegacyGraphicsPolicy("gladio"));
        assertEquals("", Container.extractLegacyGraphicsPolicy("vortek"));
        assertEquals("", Container.extractLegacyGraphicsPolicy("virgl"));
        assertEquals("", Container.extractLegacyGraphicsPolicy("wrapper"));
    }

    @Test
    public void preservesLegacyGraphicsRouteInConfigMetadata() {
        String config = Container.reconcileLegacyGraphicsConfig("turnip,gladio", Container.DEFAULT_GRAPHICSDRIVERCONFIG);
        assertEquals("turnip,gladio", Container.resolveLegacyGraphicsRequestedDriver("wrapper", config));
        assertEquals("turnip-vulkan", Container.resolveLegacyGraphicsProviderHint("wrapper", config));
        assertEquals("provider-compat", Container.resolveLegacyGraphicsPolicy("wrapper", config));
    }

    @Test
    public void preservesExistingLegacyGraphicsMetadataOnWrapperResave() {
        String config = Container.reconcileLegacyGraphicsConfig("turnip,gladio", Container.DEFAULT_GRAPHICSDRIVERCONFIG);
        String resaved = Container.reconcileLegacyGraphicsConfig("wrapper", config);
        assertEquals("turnip,gladio", Container.resolveLegacyGraphicsRequestedDriver("wrapper", resaved));
        assertEquals("turnip-vulkan", Container.resolveLegacyGraphicsProviderHint("wrapper", resaved));
        assertEquals("provider-compat", Container.resolveLegacyGraphicsPolicy("wrapper", resaved));
    }
}
