package com.winlator.cmod.contents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class WineRuntimeRunpathSanitizerTest {
    @Test
    public void buildSanitizedRunpathPrefersOriginPlusImageFsLib() {
        File elfFile = new File("/data/user/0/com.winlator.cmod/files/imagefs/opt/runtime-bionic-proton-10.0.99-arm64ec-0/lib/wine/aarch64-unix/winex11.so");
        File imageFsLibDir = new File("/data/user/0/com.winlator.cmod/files/imagefs/usr/lib");

        String runpath = WineRuntimeRunpathSanitizer.buildSanitizedRunpath(elfFile, imageFsLibDir, 47);

        assertEquals("$ORIGIN:$ORIGIN/../../../../../usr/lib", runpath);
    }

    @Test
    public void buildSanitizedRunpathFallsBackToOriginWhenBudgetIsTight() {
        File elfFile = new File("/data/user/0/com.winlator.cmod/files/imagefs/opt/runtime-bionic-proton-10.0.99-arm64ec-0/lib/wine/aarch64-unix/winex11.so");
        File imageFsLibDir = new File("/data/user/0/com.winlator.cmod/files/imagefs/usr/lib");

        String runpath = WineRuntimeRunpathSanitizer.buildSanitizedRunpath(elfFile, imageFsLibDir, 7);

        assertEquals(WineRuntimeRunpathSanitizer.ORIGIN_TOKEN, runpath);
    }

    @Test
    public void buildSanitizedRunpathHandlesRuntimeBins() {
        File elfFile = new File("/data/user/0/com.winlator.cmod/files/imagefs/opt/runtime-bionic-proton-10.0.99-arm64ec-0/bin/wineserver");
        File imageFsLibDir = new File("/data/user/0/com.winlator.cmod/files/imagefs/usr/lib");

        String runpath = WineRuntimeRunpathSanitizer.buildSanitizedRunpath(elfFile, imageFsLibDir, 47);

        assertEquals("$ORIGIN:$ORIGIN/../../../usr/lib", runpath);
    }

    @Test
    public void absoluteRunpathDetectionOnlyFlagsAbsoluteSegments() {
        assertTrue(WineRuntimeRunpathSanitizer.hasAbsolutePathSegment("/data/data/app.gamenative/files/imagefs/usr/lib"));
        assertTrue(WineRuntimeRunpathSanitizer.hasAbsolutePathSegment("$ORIGIN:/system/lib64"));
        assertFalse(WineRuntimeRunpathSanitizer.hasAbsolutePathSegment("$ORIGIN:$ORIGIN/../../../../../usr/lib"));
        assertFalse(WineRuntimeRunpathSanitizer.hasAbsolutePathSegment(""));
    }

    @Test
    public void shortAsciiFilesAreSkippedAsNonElf() throws Exception {
        File root = Files.createTempDirectory("runpath-sanitize").toFile();
        File imageFsLibDir = new File(root, "usr/lib");
        assertTrue(imageFsLibDir.mkdirs());

        File linkerScript = new File(imageFsLibDir, "librt.so");
        Files.write(linkerScript.toPath(), "INPUT()".getBytes(StandardCharsets.UTF_8));

        WineRuntimeRunpathSanitizer.Result result =
                WineRuntimeRunpathSanitizer.sanitizeTree(imageFsLibDir, imageFsLibDir);

        assertEquals(0, result.failedFiles);
        assertEquals(0, result.elfFilesScanned);
        assertEquals(1, result.nonElfFiles);
    }
}
