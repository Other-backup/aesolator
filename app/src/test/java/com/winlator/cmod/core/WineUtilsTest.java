package com.winlator.cmod.core;

import static org.junit.Assert.assertEquals;
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
}
