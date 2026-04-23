package com.winlator.cmod.gamefixes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.winlator.cmod.data.GameSource;

import org.junit.Test;

public class GameFixesRegistryTest {
    @Test
    public void donorMapContainsPortedSteamAndEpicFixes() {
        assertTrue(GameFixesRegistry.hasBuiltInFix(GameSource.STEAM, "400"));
        assertTrue(GameFixesRegistry.hasBuiltInFix(GameSource.STEAM, "413150"));
        assertTrue(GameFixesRegistry.hasBuiltInFix(GameSource.GOG, "1453375253"));
        assertTrue(GameFixesRegistry.hasBuiltInFix(GameSource.EPIC, "b1b4e0b67a044575820cb5e63028dcae"));
        assertFalse(GameFixesRegistry.hasBuiltInFix(GameSource.STEAM, "999999"));
    }

    @Test
    public void epicCatalogResolutionPrefersCatalogMetadata() {
        String catalogId = GameFixesRegistry.resolveEpicCatalogId(
                "12345",
                "",
                "b1b4e0b67a044575820cb5e63028dcae",
                "",
                ""
        );

        assertEquals("b1b4e0b67a044575820cb5e63028dcae", catalogId);
    }

    @Test
    public void epicCatalogResolutionFallsBackToNumericGameId() {
        assertEquals(
                "12345",
                GameFixesRegistry.resolveEpicCatalogId("12345", "", "", "", "")
        );
    }

    @Test
    public void shortcutInstallPathWindowsUsesExecutableDirectory() {
        assertEquals(
                "A:\\Games\\Fallout 3",
                GameFixesRegistry.resolveShortcutInstallPathWindows("\"A:\\Games\\Fallout 3\\FalloutLauncher.exe\" -steam")
        );
    }

    @Test
    public void shortcutInstallPathWindowsHandlesUnquotedExecutableWithSpaces() {
        assertEquals(
                "C:\\Program Files\\Game Native",
                GameFixesRegistry.resolveShortcutInstallPathWindows("C:\\Program Files\\Game Native\\Launcher.exe -silent /renderer=vulkan")
        );
    }

    @Test
    public void hostInstallPathResolvesFromWindowsDriveMapping() throws Exception {
        java.io.File aDrive = java.nio.file.Files.createTempDirectory("gfix-a-drive").toFile();
        java.io.File installDir = new java.io.File(aDrive, "Games/Fallout 3");
        assertTrue(installDir.mkdirs());

        assertEquals(
                installDir.getAbsolutePath(),
                GameFixesRegistry.resolveHostInstallPathFromWindowsPath(
                        "A:\\Games\\Fallout 3",
                        aDrive.getAbsolutePath(),
                        ""
                )
        );
    }
}
