package com.winlator.cmod.widget;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LogViewTest {
    @Test
    public void normalizeFileStemFallsBackWhenInputIsMissing() {
        assertEquals("runtime", LogView.normalizeFileStem(null));
        assertEquals("runtime", LogView.normalizeFileStem(""));
        assertEquals("runtime", LogView.normalizeFileStem("   "));
    }

    @Test
    public void normalizeFileStemStripsPathAndExtension() {
        assertEquals("explorer_2026-04-21_15-24-00", LogView.normalizeFileStem(
                "/storage/emulated/0/Ae.solator/logs/explorer_2026-04-21_15-24-00.txt"
        ));
        assertEquals("war3", LogView.normalizeFileStem("F:\\Games\\Warcraft III\\war3.exe"));
        assertEquals("launcher", LogView.normalizeFileStem("launcher"));
    }
}
