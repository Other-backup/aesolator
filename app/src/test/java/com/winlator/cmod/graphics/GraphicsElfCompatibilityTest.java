package com.winlator.cmod.graphics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GraphicsElfCompatibilityTest {
    @Test
    public void rejectsGlibcLinkedElfPayloads() throws Exception {
        File file = writeElf("libc.so.6");

        assertTrue(GraphicsElfCompatibility.hasForbiddenBionicToken(file));
        assertFalse(GraphicsElfCompatibility.isBionicCompatibleLibrary(file));
    }

    @Test
    public void rejectsGlibcLoaderElfPayloads() throws Exception {
        File file = writeElf("ld-linux-aarch64.so.1");

        assertTrue(GraphicsElfCompatibility.hasForbiddenBionicToken(file));
        assertFalse(GraphicsElfCompatibility.isBionicCompatibleLibrary(file));
    }

    @Test
    public void acceptsBionicElfPayloads() throws Exception {
        File file = writeElf("libdl.so libc.so");

        assertFalse(GraphicsElfCompatibility.hasForbiddenBionicToken(file));
        assertTrue(GraphicsElfCompatibility.isBionicCompatibleLibrary(file));
    }

    private static File writeElf(String payload) throws IOException {
        File file = File.createTempFile("graphics-elf-compat", ".so");
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(new byte[]{0x7f, 'E', 'L', 'F'});
            outputStream.write(new byte[32]);
            outputStream.write(payload.getBytes(StandardCharsets.US_ASCII));
        }
        file.deleteOnExit();
        return file;
    }
}
