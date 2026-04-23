package com.winlator.cmod.contents;

import static org.junit.Assert.assertEquals;

import com.winlator.cmod.contentdialog.DgVoodooConfigDialog;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineUtils;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Arrays;

public class DgVoodooManagerTest {
    @Test
    public void detectExecutableArchReadsCommonPeMachineTypes() throws Exception {
        assertEquals("x86", DgVoodooManager.detectExecutableArch(writePeExecutable(0x014c)));
        assertEquals("x64", DgVoodooManager.detectExecutableArch(writePeExecutable(0x8664)));
        assertEquals("arm64", DgVoodooManager.detectExecutableArch(writePeExecutable(0xaa64)));
        assertEquals("arm64ec", DgVoodooManager.detectExecutableArch(writePeExecutable(0xa641)));
    }

    @Test
    public void resolvePackageLaneForRuntimeArchKeepsSplitLaneContract() {
        assertEquals("x86_64", DgVoodooManager.resolvePackageLaneForRuntimeArch("x86"));
        assertEquals("x86_64", DgVoodooManager.resolvePackageLaneForRuntimeArch("x64"));
        assertEquals("arm64ec", DgVoodooManager.resolvePackageLaneForRuntimeArch("arm64"));
        assertEquals("arm64ec", DgVoodooManager.resolvePackageLaneForRuntimeArch("arm64ec"));
    }

    @Test
    public void resolveAutoRuntimeArchPromotesX64ProcessToArm64EcOnArm64EcRuntime() throws Exception {
        File prefixRoot = Files.createTempDirectory("dgvoodoo-arm64ec-prefix").toFile();
        File executable = new File(prefixRoot, "drive_c/Games/Game.exe");
        assertEquals(executable.getAbsolutePath(), writePeExecutable(0x8664, executable).getAbsolutePath());

        WineUtils.WindowsLaunchTarget launchTarget = WineUtils.resolveWindowsLaunchTarget(prefixRoot, "C:\\Games\\Game.exe");
        WineInfo wineInfo = new WineInfo("wine", "11.1", "arm64ec");

        assertEquals("arm64ec", DgVoodooManager.resolveAutoRuntimeArch(launchTarget, wineInfo));
    }

    @Test
    public void resolveAutoRuntimeArchKeepsX86ProcessOnArm64EcRuntime() throws Exception {
        File prefixRoot = Files.createTempDirectory("dgvoodoo-x86-prefix").toFile();
        File executable = new File(prefixRoot, "drive_c/Games/Legacy.exe");
        assertEquals(executable.getAbsolutePath(), writePeExecutable(0x014c, executable).getAbsolutePath());

        WineUtils.WindowsLaunchTarget launchTarget = WineUtils.resolveWindowsLaunchTarget(prefixRoot, "C:\\Games\\Legacy.exe");
        WineInfo wineInfo = new WineInfo("wine", "11.1", "arm64ec");

        assertEquals("x86", DgVoodooManager.resolveAutoRuntimeArch(launchTarget, wineInfo));
    }

    @Test
    public void resolvePackageLaneFromContractTextPrefersExplicitPackageArch() {
        assertEquals("arm64ec", DgVoodooManager.resolvePackageLaneFromContractText("{\"packageArch\":\"arm64ec\",\"lane\":\"dgvoodoo-x86_64\"}"));
        assertEquals("x86_64", DgVoodooManager.resolvePackageLaneFromContractText("{\"packageArch\":\"x86_x64\"}"));
        assertEquals("x86_64", DgVoodooManager.resolvePackageLaneFromContractText("{\"packageArch\":\"x86-64\"}"));
        assertEquals("arm64ec", DgVoodooManager.resolvePackageLaneFromContractText("{\"lane\":\"dgvoodoo-arm64ec\"}"));
        assertEquals("", DgVoodooManager.resolvePackageLaneFromContractText("{\"lane\":\"dgvoodoo-custom\"}"));
        assertEquals("", DgVoodooManager.resolvePackageLaneFromContractText("not-json"));
    }

    @Test
    public void compareVersionNamesPrefersNewestSemanticDgVoodooBuild() {
        assertEquals(1, Integer.signum(DgVoodooManager.compareVersionNames("2.87.1-arm64ec", "2.86.5")));
        assertEquals(-1, Integer.signum(DgVoodooManager.compareVersionNames("2.86.5", "2.87.1-x86_64")));
        assertEquals(0, Integer.signum(DgVoodooManager.compareVersionNames("2.87.1-arm64ec", "2.87.1-arm64ec")));
        assertEquals(0, Integer.signum(DgVoodooManager.compareVersionNames("2.87.1-arm64ec", "2.87.1-x86_64")));
    }

    @Test
    public void dgVoodooArchitectureTagKeepsPackageLanesSeparate() {
        ContentProfile arm64ec = new ContentProfile();
        arm64ec.type = ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO;
        arm64ec.verName = "2.87.1-arm64ec";
        arm64ec.desc = "Ae dgVoodoo upstream WCP (arm64ec+x64+x86+arm64 lane, 2.87.1)";
        arm64ec.artifactName = "dgvoodoo-arm64ec.wcp";

        ContentProfile x86_64 = new ContentProfile();
        x86_64.type = ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO;
        x86_64.verName = "2.87.1-x86_64";
        x86_64.desc = "Ae dgVoodoo upstream WCP (x86+x64 lane, 2.87.1)";
        x86_64.artifactName = "dgvoodoo-x86_64.wcp";

        assertEquals("arm64ec", arm64ec.getArchitectureTag());
        assertEquals("x86_64", x86_64.getArchitectureTag());
        assertEquals("arm64ec", DgVoodooManager.resolvePackageLaneForProfile(arm64ec));
        assertEquals("x86_64", DgVoodooManager.resolvePackageLaneForProfile(x86_64));
    }

    @Test
    public void remoteDgVoodooInstallRejectsStalePackageProfile() {
        ContentProfile stalePayload = new ContentProfile();
        stalePayload.type = ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO;
        stalePayload.verName = "2.86.5";

        ContentProfile remoteCard = new ContentProfile();
        remoteCard.type = ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO;
        remoteCard.verName = "2.87.1-arm64ec";

        assertEquals(true, ContentProfileIdentity.isRemoteProfileIdentityMismatch(stalePayload, remoteCard));
    }

    @Test
    public void remoteDgVoodooInstallAcceptsMatchingPackageProfile() {
        ContentProfile payload = new ContentProfile();
        payload.type = ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO;
        payload.verName = "2.87.1-arm64ec";

        ContentProfile remoteCard = new ContentProfile();
        remoteCard.type = ContentProfile.ContentType.CONTENT_TYPE_DGVOODOO;
        remoteCard.verName = "2.87.1-arm64ec";

        assertEquals(false, ContentProfileIdentity.isRemoteProfileIdentityMismatch(payload, remoteCard));
    }

    @Test
    public void companionDxvkPrefersSarekDyasync112ForArm64EcDgVoodoo() {
        KeyValueSet config = new KeyValueSet();
        config.put("version", "2.7.1-1-gplasync-arm64ec");

        String arm64ec = DgVoodooConfigDialog.resolveCompanionDxvkVersion(
                config,
                "arm64ec",
                true,
                Arrays.asList("1.12.0", "1.12.0-dyasync-arm64ec", "1.10.3-arm64ec-async")
        );
        String x64 = DgVoodooConfigDialog.resolveCompanionDxvkVersion(
                config,
                "x64",
                true,
                Arrays.asList("1.12.0", "1.12.0-dyasync-arm64ec", "1.10.3")
        );

        assertEquals("1.12.0-dyasync-arm64ec", arm64ec);
        assertEquals("1.12.0", x64);
    }

    private static File writePeExecutable(int machine) throws Exception {
        File executable = File.createTempFile("dgvoodoo-pe", ".exe");
        executable.deleteOnExit();
        return writePeExecutable(machine, executable);
    }

    private static File writePeExecutable(int machine, File executable) throws Exception {
        File parent = executable.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        byte[] header = new byte[0x100];
        header[0] = 'M';
        header[1] = 'Z';
        int peOffset = 0x80;
        writeIntLE(header, 0x3c, peOffset);
        header[peOffset] = 'P';
        header[peOffset + 1] = 'E';
        header[peOffset + 2] = 0;
        header[peOffset + 3] = 0;
        writeShortLE(header, peOffset + 4, machine);

        try (FileOutputStream outputStream = new FileOutputStream(executable)) {
            outputStream.write(header);
        }
        executable.deleteOnExit();
        return executable;
    }

    private static void writeShortLE(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xff);
        buffer[offset + 1] = (byte) ((value >>> 8) & 0xff);
    }

    private static void writeIntLE(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xff);
        buffer[offset + 1] = (byte) ((value >>> 8) & 0xff);
        buffer[offset + 2] = (byte) ((value >>> 16) & 0xff);
        buffer[offset + 3] = (byte) ((value >>> 24) & 0xff);
    }
}
