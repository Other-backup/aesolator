package com.winlator.cmod.xenvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

public class ImageFsTest {
    @Test
    public void mainWineDirAndWinePathNormalizeToCanonicalRuntimeRoot() throws Exception {
        File rootDir = Files.createTempDirectory("imagefs-runtime-root").toFile();
        File mainWineRoot = new File(rootDir, "opt/wine");
        File abiBinDir = new File(mainWineRoot, "arm64-v8a/bin");
        File abiWineLibDir = new File(mainWineRoot, "arm64-v8a/lib/wine");
        File rootPrefixPack = new File(mainWineRoot, "prefixPack.txz");

        assertTrue("abi bin dir created", abiBinDir.mkdirs());
        assertTrue("abi wine lib dir created", abiWineLibDir.mkdirs());
        assertTrue("root prefixpack created", rootPrefixPack.createNewFile());

        ImageFs imageFs = ImageFs.find(rootDir);
        assertEquals(mainWineRoot.getAbsolutePath(), imageFs.getMainWineDir().getAbsolutePath());
        assertEquals(mainWineRoot.getAbsolutePath(), imageFs.getWinePath());

        imageFs.setWinePath(abiBinDir.getAbsolutePath());
        assertEquals(mainWineRoot.getAbsolutePath(), imageFs.getWinePath());
    }

    @Test
    public void missingRootfsMarkersInferCustomProviderAndImagefsLayout() throws Exception {
        File rootDir = Files.createTempDirectory("imagefs-rootfs-infer").toFile();
        File usrBinDir = new File(rootDir, "usr/bin");
        File usrEtcDir = new File(rootDir, "usr/etc");
        File usrLibDir = new File(rootDir, "usr/lib");
        File usrTmpDir = new File(rootDir, "usr/tmp");

        assertTrue("usr/bin dir created", usrBinDir.mkdirs());
        assertTrue("usr/etc dir created", usrEtcDir.mkdirs());
        assertTrue("usr/lib dir created", usrLibDir.mkdirs());
        assertTrue("usr/tmp dir created", usrTmpDir.mkdirs());
        Files.createSymbolicLink(new File(rootDir, "bin").toPath(), usrBinDir.toPath());

        ImageFs imageFs = ImageFs.find(rootDir);
        assertEquals(ImageFs.ROOTFS_PROVIDER_CUSTOM, imageFs.getRootfsProvider());
        assertEquals(ImageFs.ROOTFS_LAYOUT_IMAGEFS, imageFs.getRootfsLayout());

        imageFs.createRootfsProviderFile("");
        imageFs.createRootfsLayoutFile("");
        assertEquals(ImageFs.ROOTFS_PROVIDER_CUSTOM, imageFs.getRootfsProvider());
        assertEquals(ImageFs.ROOTFS_LAYOUT_IMAGEFS, imageFs.getRootfsLayout());
    }

    @Test
    public void missingProviderMarkerUsesGamenativeVariantAsFallbackOnlyWhenVariantSaysSo() throws Exception {
        File rootDir = Files.createTempDirectory("imagefs-rootfs-provider").toFile();
        ImageFs imageFs = ImageFs.find(rootDir);
        imageFs.createVariantFile("gamenative");

        assertEquals(ImageFs.ROOTFS_PROVIDER_GAMENATIVE, imageFs.getRootfsProvider());
    }
}
