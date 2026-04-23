package com.winlator.cmod.contentdialog;

import com.winlator.cmod.contents.PrefixPackCatalog;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PrefixPackLocalImportResolverTest {
    @Test
    public void resolveXnaPayloadsFindsDonorCommonRedistFiles() throws IOException {
        File root = Files.createTempDirectory("prefix-pack-xna").toFile();
        File installRoot = new File(root, "install/Game");
        File aDriveRoot = new File(root, "adrive");
        assertTrue(new File(installRoot, "_CommonRedist/XNA_40/redist").mkdirs());
        assertTrue(new File(aDriveRoot, "Redist").mkdirs());
        assertTrue(new File(installRoot, "_CommonRedist/XNA_40/redist/xnafx40_redist.msi").createNewFile());
        assertTrue(new File(aDriveRoot, "Redist/xnafx31_redist.msi").createNewFile());

        List<PrefixPackCatalog.Entry> entries = PrefixPackCatalog.parse(
                "xnafx31_refresh\txnafx31_redist.msi\tdownload\thttps://example/xna31.msi\txna\ttest\t\tinstall-xna.cmd\tXNA 3.1\n" +
                "xnafx40_refresh\txnafx40_redist.msi\tdownload\thttps://example/xna40.msi\txna\ttest\t\tinstall-xna.cmd\tXNA 4.0\n"
        );

        Map<String, File> resolved = PrefixPackLocalImportResolver.resolveXnaPayloads(
                entries,
                installRoot.getAbsolutePath(),
                aDriveRoot.getAbsolutePath()
        );

        assertEquals(2, resolved.size());
        assertEquals(
                new File(aDriveRoot, "Redist/xnafx31_redist.msi").getAbsolutePath(),
                resolved.get(PrefixPackLocalImportResolver.normalizeFileKey("xnafx31_redist.msi")).getAbsolutePath()
        );
        assertEquals(
                new File(installRoot, "_CommonRedist/XNA_40/redist/xnafx40_redist.msi").getAbsolutePath(),
                resolved.get(PrefixPackLocalImportResolver.normalizeFileKey("xnafx40_redist.msi")).getAbsolutePath()
        );
    }

    @Test
    public void collectGameRootsDedupesSamePathAndNormalizesFileInputs() throws IOException {
        File root = Files.createTempDirectory("prefix-pack-roots").toFile();
        File installRoot = new File(root, "install");
        assertTrue(installRoot.mkdirs());
        File launcher = new File(installRoot, "game.exe");
        assertTrue(launcher.createNewFile());

        List<File> roots = PrefixPackLocalImportResolver.collectGameRoots(
                launcher.getAbsolutePath(),
                installRoot.getAbsolutePath()
        );

        assertEquals(1, roots.size());
        assertEquals(installRoot.getAbsolutePath(), roots.get(0).getAbsolutePath());
    }
}
