package com.winlator.cmod.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

public class WineUtilsTest {
    @Test
    public void resolvePreferredGraphicsDriverFallsBackToAndroidWhenOnlyAndroidSurfaceExists() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-android").toFile();
        File androidDriver = new File(runtimeRoot, "arm64-v8a/lib/wine/aarch64-unix/wineandroid.so");
        assertTrue("android driver sentinel parent created", androidDriver.getParentFile().mkdirs());
        assertTrue("android driver sentinel created", androidDriver.createNewFile());

        assertEquals("android", WineUtils.resolvePreferredGraphicsDriver(runtimeRoot));
    }

    @Test
    public void resolvePreferredGraphicsDriverFallsBackToX11Surface() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-x11").toFile();
        File x11Driver = new File(runtimeRoot, "lib/wine/aarch64-unix/winex11.so");
        assertTrue("x11 driver sentinel parent created", x11Driver.getParentFile().mkdirs());
        assertTrue("x11 driver sentinel created", x11Driver.createNewFile());

        assertEquals("x11", WineUtils.resolvePreferredGraphicsDriver(runtimeRoot));
    }

    @Test
    public void resolvePreferredGraphicsDriverPrefersX11ForProtonWhenBothSurfacesExist() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-proton-both").toFile();
        File androidDriver = new File(runtimeRoot, "arm64-v8a/lib/wine/aarch64-unix/wineandroid.so");
        File x11Driver = new File(runtimeRoot, "arm64-v8a/lib/wine/aarch64-unix/winex11.so");
        assertTrue("driver parent created", androidDriver.getParentFile().mkdirs());
        assertTrue("android driver sentinel created", androidDriver.createNewFile());
        assertTrue("x11 driver sentinel created", x11Driver.createNewFile());

        WineInfo protonInfo = new WineInfo("proton", "10.0.99", "arm64ec", runtimeRoot.getPath());
        assertEquals("x11", WineUtils.resolvePreferredGraphicsDriver(runtimeRoot, protonInfo));
    }

    @Test
    public void resolvePreferredGraphicsDriverPrefersX11ForWineWhenBothSurfacesExist() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-wine-both").toFile();
        File androidDriver = new File(runtimeRoot, "arm64-v8a/lib/wine/aarch64-unix/wineandroid.so");
        File x11Driver = new File(runtimeRoot, "arm64-v8a/lib/wine/aarch64-unix/winex11.so");
        assertTrue("driver parent created", androidDriver.getParentFile().mkdirs());
        assertTrue("android driver sentinel created", androidDriver.createNewFile());
        assertTrue("x11 driver sentinel created", x11Driver.createNewFile());

        WineInfo wineInfo = new WineInfo("wine", "11.4", "arm64ec", runtimeRoot.getPath());
        assertEquals("x11", WineUtils.resolvePreferredGraphicsDriver(runtimeRoot, wineInfo));
    }

    @Test
    public void resolvePreferredGraphicsDriverDefaultsProtonToX11WhenSurfaceIsOpaque() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-proton-empty").toFile();

        WineInfo protonInfo = new WineInfo("proton", "10.0.99", "arm64ec", runtimeRoot.getPath());
        assertEquals("x11", WineUtils.resolvePreferredGraphicsDriver(runtimeRoot, protonInfo));
    }

    @Test
    public void resolvePreferredGraphicsDriverReturnsEmptyWhenNoSurfaceExists() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-empty").toFile();

        assertEquals("", WineUtils.resolvePreferredGraphicsDriver(runtimeRoot));
    }

    @Test
    public void resolveRuntimeWineLibDirSupportsArm64V8aLayout() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-arm64v8a").toFile();
        File wineLibDir = new File(runtimeRoot, "arm64-v8a/lib/wine");
        File binDir = new File(runtimeRoot, "arm64-v8a/bin");
        File wineBinary = new File(binDir, "wine");
        assertTrue("arm64-v8a wine lib created", wineLibDir.mkdirs());
        assertTrue("arm64-v8a bin created", binDir.mkdirs());
        assertTrue("arm64-v8a wine binary created", wineBinary.createNewFile());

        assertEquals(wineLibDir.getAbsolutePath(), WineUtils.resolveRuntimeWineLibDir(runtimeRoot).getAbsolutePath());
        assertEquals(new File(runtimeRoot, "arm64-v8a/lib").getAbsolutePath(), WineUtils.resolveRuntimeLibDir(runtimeRoot).getAbsolutePath());
        assertEquals(binDir.getAbsolutePath(), WineUtils.resolveRuntimeBinDir(runtimeRoot).getAbsolutePath());
        assertEquals(wineBinary.getAbsolutePath(), WineUtils.resolveRuntimeWineBinary(runtimeRoot).getAbsolutePath());
    }

    @Test
    public void resolveRuntimePrefixPackSupportsRootAndAbiCompatLayouts() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-prefixpack").toFile();
        File rootPrefixPack = new File(runtimeRoot, "prefixPack.txz");
        assertTrue("root prefixpack created", rootPrefixPack.createNewFile());
        assertEquals(rootPrefixPack.getAbsolutePath(), WineUtils.resolveRuntimePrefixPack(runtimeRoot).getAbsolutePath());

        File abiRoot = Files.createTempDirectory("wineutils-prefixpack-abi").toFile();
        File abiPrefixPack = new File(abiRoot, "arm64-v8a/prefixPack.tzst");
        assertTrue("abi prefixpack parent created", abiPrefixPack.getParentFile().mkdirs());
        assertTrue("abi prefixpack created", abiPrefixPack.createNewFile());
        assertEquals(abiPrefixPack.getAbsolutePath(), WineUtils.resolveRuntimePrefixPack(abiRoot).getAbsolutePath());
    }

    @Test
    public void resolveRuntimeShareAndUnixDirsSupportAbiLayout() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-share-unix").toFile();
        File shareDir = new File(runtimeRoot, "arm64-v8a/share");
        File wineUnixDir = new File(runtimeRoot, "arm64-v8a/lib/wine/aarch64-unix");
        File binDir = new File(runtimeRoot, "arm64-v8a/bin");
        File prefixPack = new File(runtimeRoot, "prefixPack.txz");

        assertTrue("abi share dir created", shareDir.mkdirs());
        assertTrue("abi wine unix dir created", wineUnixDir.mkdirs());
        assertTrue("abi bin dir created", binDir.mkdirs());
        assertTrue("root prefixpack created", prefixPack.createNewFile());

        assertEquals(shareDir.getAbsolutePath(), WineUtils.resolveRuntimeShareDir(runtimeRoot).getAbsolutePath());
        assertEquals(wineUnixDir.getAbsolutePath(), WineUtils.resolveRuntimeWineUnixDir(runtimeRoot).getAbsolutePath());
    }

    @Test
    public void resolveRuntimeWineUnixDirSupportsX8664GlibcLayout() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-x8664-unix").toFile();
        File wineUnixDir = new File(runtimeRoot, "lib/wine/x86_64-unix");
        assertTrue("x86_64 wine unix dir created", wineUnixDir.mkdirs());

        WineInfo wineInfo = new WineInfo("wine", "10.15", "x86_64", runtimeRoot.getPath());

        assertEquals(wineUnixDir.getAbsolutePath(), WineUtils.resolveRuntimeWineUnixDir(runtimeRoot).getAbsolutePath());
        assertEquals(wineUnixDir.getAbsolutePath(), WineUtils.resolveRuntimeWineUnixDir(runtimeRoot, wineInfo).getAbsolutePath());
    }

    @Test
    public void resolveRuntimeWineUnixDirPrefersWineArchWhenMultipleUnixDirsExist() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-arch-unix").toFile();
        File armUnixDir = new File(runtimeRoot, "lib/wine/aarch64-unix");
        File x64UnixDir = new File(runtimeRoot, "lib/wine/x86_64-unix");
        assertTrue("aarch64 wine unix dir created", armUnixDir.mkdirs());
        assertTrue("x86_64 wine unix dir created", x64UnixDir.mkdirs());

        WineInfo wineInfo = new WineInfo("wine", "10.15", "x86_64", runtimeRoot.getPath());

        assertEquals(armUnixDir.getAbsolutePath(), WineUtils.resolveRuntimeWineUnixDir(runtimeRoot).getAbsolutePath());
        assertEquals(x64UnixDir.getAbsolutePath(), WineUtils.resolveRuntimeWineUnixDir(runtimeRoot, wineInfo).getAbsolutePath());
    }

    @Test
    public void resolveRuntimeWineWindowsDirPrefersWineArchWhenMultipleWindowsDirsExist() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-arch-windows").toFile();
        File armWindowsDir = new File(runtimeRoot, "lib/wine/aarch64-windows");
        File x64WindowsDir = new File(runtimeRoot, "lib/wine/x86_64-windows");
        assertTrue("aarch64 windows dir created", armWindowsDir.mkdirs());
        assertTrue("x86_64 windows dir created", x64WindowsDir.mkdirs());

        WineInfo wineInfo = new WineInfo("wine", "10.15", "x86_64", runtimeRoot.getPath());

        assertEquals(x64WindowsDir.getAbsolutePath(), WineUtils.resolveRuntimeWineWindowsDir(runtimeRoot, wineInfo).getAbsolutePath());
    }

    @Test
    public void validateRuntimeAbiContractFailsX8664GlibcWithoutGuestLibc() throws Exception {
        File imageRoot = Files.createTempDirectory("wineutils-image-root").toFile();
        File runtimeRoot = Files.createTempDirectory("wineutils-x8664-abi-missing").toFile();
        assertTrue("runtime bin created", new File(runtimeRoot, "bin").mkdirs());
        assertTrue("runtime wine binary created", new File(runtimeRoot, "bin/wine").createNewFile());
        assertTrue("runtime x86_64 unix created", new File(runtimeRoot, "lib/wine/x86_64-unix").mkdirs());
        assertTrue("runtime x86_64 windows created", new File(runtimeRoot, "lib/wine/x86_64-windows").mkdirs());
        assertTrue("runtime prefix pack created", new File(runtimeRoot, "prefixPack.txz").createNewFile());

        WineInfo wineInfo = new WineInfo("wine", "10.15", "x86_64", runtimeRoot.getPath());
        WineUtils.RuntimeAbiContract contract = WineUtils.validateRuntimeAbiContract(
                imageRoot,
                runtimeRoot,
                wineInfo,
                "glibc"
        );

        assertFalse("x86_64 glibc ABI should be incomplete without loader/libc", contract.complete);
        assertTrue("missing x86_64 loader recorded", contract.missing.contains("x86_64_glibc_loader"));
        assertTrue("missing x86_64 libc recorded", contract.missing.contains("x86_64_glibc_libc"));
    }

    @Test
    public void validateRuntimeAbiContractAcceptsX8664GlibcWithImageRootAbi() throws Exception {
        File imageRoot = Files.createTempDirectory("wineutils-image-root-x64").toFile();
        File runtimeRoot = Files.createTempDirectory("wineutils-x8664-abi-ok").toFile();
        assertTrue("runtime bin created", new File(runtimeRoot, "bin").mkdirs());
        assertTrue("runtime wine binary created", new File(runtimeRoot, "bin/wine").createNewFile());
        assertTrue("runtime x86_64 unix created", new File(runtimeRoot, "lib/wine/x86_64-unix").mkdirs());
        assertTrue("runtime x86_64 windows created", new File(runtimeRoot, "lib/wine/x86_64-windows").mkdirs());
        assertTrue("runtime prefix pack created", new File(runtimeRoot, "prefixPack.txz").createNewFile());
        File abiDir = new File(imageRoot, "usr/lib/x86_64-linux-gnu");
        assertTrue("image root x86_64 abi dir created", abiDir.mkdirs());
        writeElfHeader(new File(abiDir, "ld-linux-x86-64.so.2"), 2, 62);
        writeElfHeader(new File(abiDir, "libc.so.6"), 2, 62);

        WineInfo wineInfo = new WineInfo("wine", "10.15", "x86_64", runtimeRoot.getPath());
        WineUtils.RuntimeAbiContract contract = WineUtils.validateRuntimeAbiContract(
                imageRoot,
                runtimeRoot,
                wineInfo,
                "glibc"
        );

        assertTrue("x86_64 glibc ABI should be complete when image root carries loader/libc", contract.complete);
        assertEquals("", contract.missing);
    }

    @Test
    public void validateRuntimeAbiContractRejectsWrongArchGlibcCandidates() throws Exception {
        File imageRoot = Files.createTempDirectory("wineutils-image-root-wrong-arch").toFile();
        File runtimeRoot = Files.createTempDirectory("wineutils-x8664-abi-wrong-arch").toFile();
        assertTrue("runtime bin created", new File(runtimeRoot, "bin").mkdirs());
        writeElfHeader(new File(runtimeRoot, "bin/wine"), 2, 62);
        assertTrue("runtime x86_64 unix created", new File(runtimeRoot, "lib/wine/x86_64-unix").mkdirs());
        assertTrue("runtime x86_64 windows created", new File(runtimeRoot, "lib/wine/x86_64-windows").mkdirs());
        assertTrue("runtime prefix pack created", new File(runtimeRoot, "prefixPack.txz").createNewFile());
        assertTrue("image root lib64 created", new File(imageRoot, "lib64").mkdirs());
        writeElfHeader(new File(imageRoot, "lib64/ld-linux-x86-64.so.2"), 2, 183);
        writeElfHeader(new File(imageRoot, "lib64/libc.so.6"), 2, 183);

        WineInfo wineInfo = new WineInfo("wine", "10.15", "x86_64", runtimeRoot.getPath());
        WineUtils.RuntimeAbiContract contract = WineUtils.validateRuntimeAbiContract(
                imageRoot,
                runtimeRoot,
                wineInfo,
                "glibc"
        );

        assertFalse("x86_64 glibc ABI should reject aarch64 ELF files", contract.complete);
        assertTrue("wrong-arch loader remains missing", contract.missing.contains("x86_64_glibc_loader"));
        assertTrue("wrong-arch libc remains missing", contract.missing.contains("x86_64_glibc_libc"));
        assertEquals("", contract.glibcLoaderPath);
        assertEquals("", contract.glibcLibcPath);
        assertTrue("loader rejection records observed and expected machine",
                contract.glibcLoaderRejectedPath.contains("machine=183")
                        && contract.glibcLoaderRejectedPath.contains("expectedMachine=62"));
        assertTrue("libc rejection records observed and expected machine",
                contract.glibcLibcRejectedPath.contains("machine=183")
                        && contract.glibcLibcRejectedPath.contains("expectedMachine=62"));
    }

    @Test
    public void resolveCanonicalRuntimeRootPrefersPackageRootOverAbiSubdirs() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-canonical-root").toFile();
        File abiRoot = new File(runtimeRoot, "arm64-v8a");
        File abiBinDir = new File(abiRoot, "bin");
        File abiWineLibDir = new File(abiRoot, "lib/wine");
        File rootPrefixPack = new File(runtimeRoot, "prefixPack.tzst");

        assertTrue("abi bin dir created", abiBinDir.mkdirs());
        assertTrue("abi wine lib dir created", abiWineLibDir.mkdirs());
        assertTrue("root prefixpack created", rootPrefixPack.createNewFile());

        assertEquals(runtimeRoot.getAbsolutePath(), WineUtils.resolveCanonicalRuntimeRoot(abiBinDir).getAbsolutePath());
        assertEquals(runtimeRoot.getAbsolutePath(), WineUtils.resolveCanonicalRuntimeRoot(abiRoot).getAbsolutePath());
    }

    @Test
    public void resolveRuntimeLayoutPrefersCanonicalRootWhenCompatSurfaceExists() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-runtime-layout-root").toFile();
        File rootBinDir = new File(runtimeRoot, "bin");
        File rootWineLibDir = new File(runtimeRoot, "lib/wine");
        File rootPrefixPack = new File(runtimeRoot, "prefixPack.txz");
        File abiBinDir = new File(runtimeRoot, "arm64-v8a/bin");
        File abiWineLibDir = new File(runtimeRoot, "arm64-v8a/lib/wine");

        assertTrue("root bin created", rootBinDir.mkdirs());
        assertTrue("root wine lib created", rootWineLibDir.mkdirs());
        assertTrue("root prefixpack created", rootPrefixPack.createNewFile());
        assertTrue("abi bin created", abiBinDir.mkdirs());
        assertTrue("abi wine lib created", abiWineLibDir.mkdirs());

        WineUtils.RuntimeLayout layout = WineUtils.resolveRuntimeLayout(runtimeRoot);
        assertTrue("runtime layout complete", layout.isComplete());
        assertEquals(rootBinDir.getAbsolutePath(), layout.binDir.getAbsolutePath());
        assertEquals(new File(runtimeRoot, "lib").getAbsolutePath(), layout.libDir.getAbsolutePath());
        assertEquals(rootWineLibDir.getAbsolutePath(), layout.wineLibDir.getAbsolutePath());
        assertEquals(rootPrefixPack.getAbsolutePath(), layout.prefixPack.getAbsolutePath());
    }

    @Test
    public void buildExplorerDesktopShellCommandKeepsCanonicalDirectRoute() {
        assertEquals(
                "wine explorer /desktop=shell,1280x720 \"explorer.exe\"",
                WineUtils.buildExplorerDesktopShellCommand("1280x720", "\"explorer.exe\"")
        );
    }

    @Test
    public void buildExplorerDesktopShellCommandHostsWinHandlerBridgeInsideDesktop() {
        assertEquals(
                "wine explorer /desktop=shell,1280x720 winhandler.exe \"wfm.exe\"",
                WineUtils.buildExplorerDesktopShellCommand(
                        "1280x720",
                        WineUtils.buildWinHandlerDesktopShellPayload("C:\\windows\\winhandler.exe", "C:\\windows\\wfm.exe")
                )
        );
    }

    @Test
    public void canonicalDesktopShellExecutableNameStripsResolvedDosPath() {
        assertEquals(
                "explorer.exe",
                WineUtils.canonicalDesktopShellExecutableName("\"C:\\windows\\explorer.exe\"", "wfm.exe")
        );
        assertEquals(
                "wfm.exe",
                WineUtils.canonicalDesktopShellExecutableName("C:\\windows\\system32\\wfm.exe", "explorer.exe")
        );
    }

    @Test
    public void buildExplorerDesktopShellCommandRejectsMissingGeometry() {
        try {
            WineUtils.buildExplorerDesktopShellCommand(" ", "\"explorer.exe\"");
        } catch (IllegalArgumentException e) {
            assertEquals("desktop shell geometry is required", e.getMessage());
            return;
        }
        throw new AssertionError("missing desktop shell geometry should fail");
    }

    @Test
    public void hasRuntimePayloadAcceptsAbiDirsWithRootPrefixPack() throws Exception {
        File runtimeRoot = Files.createTempDirectory("wineutils-runtime-layout-mixed").toFile();
        File abiWineLibDir = new File(runtimeRoot, "arm64-v8a/lib/wine");
        File abiBinDir = new File(runtimeRoot, "arm64-v8a/bin");
        File rootPrefixPack = new File(runtimeRoot, "prefixPack.tzst");

        assertTrue("abi wine lib created", abiWineLibDir.mkdirs());
        assertTrue("abi bin created", abiBinDir.mkdirs());
        assertTrue("root prefixpack created", rootPrefixPack.createNewFile());

        assertTrue("mixed runtime payload accepted", WineUtils.hasRuntimePayload(runtimeRoot));
    }

    @Test
    public void extractWindowsCommandPathAndArgsSplitQuotedCommand() {
        String command = "\"A:\\\\Games\\\\Fallout 3\\\\FalloutLauncher.exe\" -steam -windowed";
        assertEquals("A:\\Games\\Fallout 3\\FalloutLauncher.exe", WineUtils.extractWindowsCommandPath(command));
        assertEquals("-steam -windowed", WineUtils.extractWindowsCommandArgs(command));
        assertEquals("A:\\Games\\Fallout 3", WineUtils.resolveWindowsParentDir(WineUtils.extractWindowsCommandPath(command)));
    }

    @Test
    public void resolveWindowsLaunchTargetParsesUnquotedExecutableWithSpaces() {
        WineUtils.WindowsLaunchTarget target = WineUtils.resolveWindowsLaunchTarget(
                null,
                "C:\\Program Files\\Game Native\\Launcher.exe -silent /renderer=vulkan"
        );

        assertEquals("C:\\Program Files\\Game Native\\Launcher.exe", target.commandPath);
        assertEquals("-silent /renderer=vulkan", target.commandArgs);
        assertEquals("C:\\Program Files\\Game Native", target.workingDir);
        assertEquals("Launcher.exe", target.getExecutableName());
    }

    @Test
    public void extractWineExecPayloadStripsLauncherPrefixWithoutLastIndexFolklore() {
        String execLine = "env WINEPREFIX=\"/data/user/0/app/files/prefix\" BOX64_LOG=1 /usr/bin/wine \"C:\\\\Program Files\\\\Game Native\\\\Launcher.exe\" -silent";

        assertEquals(
                "\"C:\\Program Files\\Game Native\\Launcher.exe\" -silent",
                WineUtils.extractWineExecPayload(execLine)
        );
    }

    @Test
    public void resolveHostPathFromWindowsPathSupportsCDriveLayouts() throws Exception {
        File containerRoot = Files.createTempDirectory("wineutils-cdrive").toFile();
        File cDriveTarget = new File(containerRoot, "drive_c/Games/Fallout 3");
        assertTrue("c drive target created", cDriveTarget.mkdirs());

        File resolved = WineUtils.resolveHostPathFromWindowsPath(containerRoot, "C:\\Games\\Fallout 3");
        assertEquals(cDriveTarget.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void resolveHostPathFromWindowsPathSupportsDosDevicesLinks() throws Exception {
        File containerRoot = Files.createTempDirectory("wineutils-adrive-root").toFile();
        File prefixDir = new File(containerRoot, ".wine");
        File dosdevicesDir = new File(prefixDir, "dosdevices");
        File aDriveTarget = Files.createTempDirectory("wineutils-adrive-target").toFile();
        File installDir = new File(aDriveTarget, "Games/Fallout 3");
        assertTrue("dosdevices created", dosdevicesDir.mkdirs());
        assertTrue("a drive install dir created", installDir.mkdirs());
        Files.createSymbolicLink(new File(dosdevicesDir, "a:").toPath(), aDriveTarget.toPath());

        File resolved = WineUtils.resolveHostPathFromWindowsPath(containerRoot, "A:\\Games\\Fallout 3");
        assertEquals(installDir.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void resolveWindowsLaunchTargetResolvesHostFileAndDir() throws Exception {
        File containerRoot = Files.createTempDirectory("wineutils-launchtarget-root").toFile();
        File executable = new File(containerRoot, "drive_c/Games/Fallout 3/FalloutLauncher.exe");
        assertTrue("c drive executable parent created", executable.getParentFile().mkdirs());
        assertTrue("c drive executable created", executable.createNewFile());

        WineUtils.WindowsLaunchTarget target = WineUtils.resolveWindowsLaunchTarget(
                containerRoot,
                "\"C:\\Games\\Fallout 3\\FalloutLauncher.exe\" -steam"
        );

        assertEquals(executable.getAbsolutePath(), target.hostTargetFile.getAbsolutePath());
        assertEquals(executable.getParentFile().getAbsolutePath(), target.hostTargetDir.getAbsolutePath());
    }

    private static void writeElfHeader(File file, int elfClass, int machine) throws Exception {
        assertTrue("ELF parent created", file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
        byte[] header = new byte[20];
        header[0] = 0x7f;
        header[1] = 'E';
        header[2] = 'L';
        header[3] = 'F';
        header[4] = (byte) elfClass;
        header[5] = 1;
        header[6] = 1;
        header[16] = 3;
        header[18] = (byte) (machine & 0xff);
        header[19] = (byte) ((machine >> 8) & 0xff);
        Files.write(file.toPath(), header);
    }
}
