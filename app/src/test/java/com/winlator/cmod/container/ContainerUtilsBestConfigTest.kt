package com.winlator.cmod.container

import org.junit.Assert.assertEquals
import org.junit.Test

class ContainerUtilsBestConfigTest {
    @Test
    fun `store mismatch does not override executable path`() {
        val base = mapOf(
            "executablePath" to "C:\\Games\\Base\\game.exe",
            "graphicsDriver" to "virgl"
        )
        val imported = mapOf(
            "executablePath" to "D:\\OtherStore\\other.exe",
            "graphicsDriver" to "turnip"
        )

        val merged = ContainerUtils.applyBestConfigMapToMap(base, imported, storeMatch = false)

        assertEquals("C:\\Games\\Base\\game.exe", merged["executablePath"])
        assertEquals("turnip", merged["graphicsDriver"])
    }

    @Test
    fun `store match keeps executable path override`() {
        val base = mapOf("executablePath" to "C:\\Games\\Base\\game.exe")
        val imported = mapOf("executablePath" to "D:\\SameStore\\game.exe")

        val merged = ContainerUtils.applyBestConfigMapToMap(base, imported, storeMatch = true)

        assertEquals("D:\\SameStore\\game.exe", merged["executablePath"])
    }
}
