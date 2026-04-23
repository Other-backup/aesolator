package com.winlator.cmod.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

public class TarCompressorUtilsTest {
    @Test
    public void resolveExtractionTargetAllowsNestedEntriesInsideRoot() throws Exception {
        File root = Files.createTempDirectory("ae-safe-tar-root").toFile();

        File target = TarCompressorUtils.resolveExtractionTarget(root, "usr/lib/libwine.so");

        assertNotNull(target);
        assertTrue(FileUtils.isSameOrChild(root, target));
        assertEquals(new File(root, "usr/lib/libwine.so").getCanonicalFile(), target);
    }

    @Test
    public void resolveExtractionTargetRejectsParentTraversal() throws Exception {
        File root = Files.createTempDirectory("ae-safe-tar-root").toFile();

        assertNull(TarCompressorUtils.resolveExtractionTarget(root, "../escape"));
        assertNull(TarCompressorUtils.resolveExtractionTarget(root, "usr/../../escape"));
    }

    @Test
    public void resolveExtractionTargetRejectsAbsoluteEntries() throws Exception {
        File root = Files.createTempDirectory("ae-safe-tar-root").toFile();

        assertNull(TarCompressorUtils.resolveExtractionTarget(root, "/data/local/tmp/escape"));
    }

    @Test
    public void safeChildCheckDoesNotAcceptSiblingPrefix() throws Exception {
        File parent = Files.createTempDirectory("ae-safe-parent").toFile();
        File root = new File(parent, "root");
        File sibling = new File(parent, "root-sibling/file");
        assertTrue(root.mkdirs());

        assertFalse(FileUtils.isSameOrChild(root, sibling));
    }

    @Test
    public void stripTopLevelDirectoryUsesLiteralPathPrefix() {
        assertEquals("nested/file.txt", TarCompressorUtils.stripTopLevelDirectory("package-1.0/nested/file.txt", "package-1.0/"));
        assertEquals("nested/file.txt", TarCompressorUtils.stripTopLevelDirectory("./package[1]/nested/file.txt", "package[1]"));
        assertEquals("other/file.txt", TarCompressorUtils.stripTopLevelDirectory("other/file.txt", "package-1.0"));
    }
}
