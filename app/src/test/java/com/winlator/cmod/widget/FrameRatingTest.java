package com.winlator.cmod.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.HashMap;

public class FrameRatingTest {
    @Test
    public void rendererDriverNameUsesTrimmedWrapperVersion() {
        HashMap<String, String> config = new HashMap<>();
        config.put("version", "  wrapper-vulkan-1  ");

        assertEquals("wrapper-vulkan-1", FrameRating.getRendererDriverName(config));
    }

    @Test
    public void rendererDriverNameFallsBackToSystemRendererForKeyValueRoutes() {
        HashMap<String, String> config = new HashMap<>();
        config.put("packageVersion", "virgl-bundled");
        config.put("glVersion", "3.1");

        assertNull(FrameRating.getRendererDriverName(config));
    }

    @Test
    public void rendererDriverNameFallsBackToSystemRendererForMissingConfig() {
        assertNull(FrameRating.getRendererDriverName(null));
        assertNull(FrameRating.getRendererDriverName(new HashMap<>()));
    }
}
