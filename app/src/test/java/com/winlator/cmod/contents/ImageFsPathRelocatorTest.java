package com.winlator.cmod.contents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.winlator.cmod.xenvironment.ImageFs;

import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ImageFsPathRelocatorTest {
    @Test
    public void relocatesBinaryNullTerminatedDonorPathToCwdAlias() throws Exception {
        File root = Files.createTempDirectory("imagefs-path-relocator-bin").toFile();
        File binDir = new File(root, "opt/runtime/bin");
        assertTrue(binDir.mkdirs());
        File binary = new File(binDir, "wineserver");
        byte[] stale = "\0/data/data/com.winlator/files/rootfs/tmp/.wine-%u\0tail".getBytes(StandardCharsets.ISO_8859_1);
        Files.write(binary.toPath(), stale);

        ImageFsPathRelocator.Result result =
                ImageFsPathRelocator.relocateTree(binDir, ImageFs.find(root), "runtime");

        assertEquals(1, result.patchedFiles);
        assertEquals(1, result.replacements);
        String patched = new String(Files.readAllBytes(binary.toPath()), StandardCharsets.ISO_8859_1);
        assertTrue(patched.contains("/proc/self/cwd/tmp/.wine-%u"));
        assertFalse(patched.contains("/data/data/com.winlator/files/rootfs"));
    }

    @Test
    public void relocatesTextDonorPathToCurrentImageFsRoot() throws Exception {
        File root = Files.createTempDirectory("imagefs-path-relocator-text").toFile();
        File scriptDir = new File(root, "usr/bin");
        assertTrue(scriptDir.mkdirs());
        File script = new File(scriptDir, "ldd");
        Files.write(
                script.toPath(),
                "prefix=/data/data/app.gamenative/files/imagefs/usr/lib\n".getBytes(StandardCharsets.ISO_8859_1)
        );

        ImageFsPathRelocator.Result result =
                ImageFsPathRelocator.relocateTree(scriptDir, ImageFs.find(root), "rootfs");

        assertEquals(1, result.patchedFiles);
        String patched = new String(Files.readAllBytes(script.toPath()), StandardCharsets.ISO_8859_1);
        assertTrue(patched.contains(root.getAbsolutePath() + "/usr/lib"));
        assertFalse(patched.contains("/data/data/app.gamenative/files/imagefs"));
    }

    @Test
    public void relocatesLargeBinaryWithoutLoadingWholeFile() throws Exception {
        File root = Files.createTempDirectory("imagefs-path-relocator-large").toFile();
        File libDir = new File(root, "usr/lib");
        assertTrue(libDir.mkdirs());
        File binary = new File(libDir, "libGL.so.1.5.0");
        long segmentStart = 8L * 1024L * 1024L + 256L;
        byte[] stale = "/data/data/com.winlator/files/rootfs/usr/share/wine/nls".getBytes(StandardCharsets.ISO_8859_1);
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(binary, "rw")) {
            randomAccessFile.setLength(segmentStart + stale.length + 4096L);
            randomAccessFile.seek(segmentStart);
            randomAccessFile.write(stale);
            randomAccessFile.write(0);
        }

        ImageFsPathRelocator.Result result =
                ImageFsPathRelocator.relocateTree(libDir, ImageFs.find(root), "rootfs");

        assertEquals(1, result.patchedFiles);
        assertEquals(1, result.replacements);
        byte[] patchedBytes = new byte[96];
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(binary, "r")) {
            randomAccessFile.seek(segmentStart);
            randomAccessFile.readFully(patchedBytes);
        }
        String patched = new String(patchedBytes, StandardCharsets.ISO_8859_1);
        assertTrue(patched.contains("/proc/self/cwd/usr/share/wine/nls"));
        assertFalse(patched.contains("/data/data/com.winlator/files/rootfs"));
    }
}
