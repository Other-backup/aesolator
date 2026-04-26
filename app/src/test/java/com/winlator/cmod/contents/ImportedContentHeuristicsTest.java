package com.winlator.cmod.contents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ImportedContentHeuristicsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void infersBionicProtonFromPayloadWithoutRemoteHint() throws Exception {
        File root = temporaryFolder.newFolder("imported-proton");
        assertTrue(new File(root, "arm64-v8a/bin").mkdirs());
        assertTrue(new File(root, "arm64-v8a/lib/wine/aarch64-unix").mkdirs());
        assertTrue(new File(root, "arm64-v8a/share").mkdirs());
        assertTrue(new File(root, "arm64-v8a/bin/wine").createNewFile());
        assertTrue(new File(root, "arm64-v8a/lib/wine/aarch64-unix/wineandroid.so").createNewFile());
        assertTrue(new File(root, "prefixPack.txz").createNewFile());

        ContentProfile.ContentType type = ImportedContentHeuristics.inferContentType(
                root,
                null,
                null,
                "proton-wine-11.0-arm64ec.wcp.xz"
        );
        String runtimeModel = ImportedContentHeuristics.inferRuntimeModel(
                root,
                null,
                null,
                "proton-wine-11.0-arm64ec.wcp.xz"
        );
        String versionName = ImportedContentHeuristics.deriveVersionName(
                "proton-wine-11.0-arm64ec.wcp.xz",
                type,
                ""
        );

        assertEquals(ContentProfile.ContentType.CONTENT_TYPE_PROTON, type);
        assertEquals(ContentProfile.RUNTIME_MODEL_BIONIC, runtimeModel);
        assertEquals("11.0-arm64ec", versionName);
    }

    @Test
    public void payloadEvidenceOverridesGenericGlibcLabelForBionicRuntime() throws Exception {
        File root = temporaryFolder.newFolder("mislabeled-bionic-runtime");
        assertTrue(new File(root, "arm64-v8a/bin").mkdirs());
        assertTrue(new File(root, "arm64-v8a/lib/wine/aarch64-unix").mkdirs());
        assertTrue(new File(root, "arm64-v8a/bin/wine").createNewFile());
        assertTrue(new File(root, "arm64-v8a/lib/wine/aarch64-unix/winex11.so").createNewFile());
        assertTrue(new File(root, "prefixPack.txz").createNewFile());

        ContentProfile parsed = new ContentProfile();
        parsed.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        parsed.verName = "glibc-proton-11.0";
        parsed.runtimeModel = ContentProfile.RUNTIME_MODEL_GLIBC;

        assertEquals(
                ContentProfile.RUNTIME_MODEL_BIONIC,
                ImportedContentHeuristics.inferRuntimeModel(root, parsed, null, "proton-11.0-glibc-label.wcp.xz")
        );
    }

    @Test
    public void androidElfMarkerClassifiesFlatWineRuntimeAsBionic() throws Exception {
        File root = temporaryFolder.newFolder("flat-bionic-runtime");
        assertTrue(new File(root, "bin").mkdirs());
        assertTrue(new File(root, "lib/wine/aarch64-unix").mkdirs());
        assertTrue(new File(root, "lib/wine/aarch64-windows").mkdirs());
        assertTrue(new File(root, "bin/wine").createNewFile());
        Files.write(
                new File(root, "lib/wine/aarch64-unix/ntdll.so").toPath(),
                "ELF\0.note.android.ident\0__bionic_ctype_in_range".getBytes(StandardCharsets.UTF_8)
        );
        assertTrue(new File(root, "lib/wine/aarch64-windows/winex11.drv").createNewFile());
        assertTrue(new File(root, "prefixPack.txz").createNewFile());

        ContentProfile parsed = new ContentProfile();
        parsed.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        parsed.verName = "glibc-proton-11.0";
        parsed.runtimeModel = ContentProfile.RUNTIME_MODEL_GLIBC;

        assertEquals(
                ContentProfile.RUNTIME_MODEL_BIONIC,
                ImportedContentHeuristics.inferRuntimeModel(root, parsed, null, "proton-wine-11.0-arm64ec.wcp.xz")
        );
    }

    @Test
    public void payloadEvidenceOverridesGenericBionicLabelForGlibcRuntime() throws Exception {
        File root = temporaryFolder.newFolder("mislabeled-glibc-runtime");
        assertTrue(new File(root, "bin").mkdirs());
        assertTrue(new File(root, "lib/wine/aarch64-unix").mkdirs());
        assertTrue(new File(root, "lib64").mkdirs());
        assertTrue(new File(root, "usr/lib/aarch64-linux-gnu").mkdirs());
        assertTrue(new File(root, "bin/wine").createNewFile());
        assertTrue(new File(root, "lib/wine/aarch64-unix/ntdll.so").createNewFile());
        assertTrue(new File(root, "lib64/ld-linux-aarch64.so.1").createNewFile());
        assertTrue(new File(root, "prefixPack.txz").createNewFile());

        ContentProfile parsed = new ContentProfile();
        parsed.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        parsed.verName = "bionic-proton-10.0";
        parsed.runtimeModel = ContentProfile.RUNTIME_MODEL_BIONIC;

        assertEquals(
                ContentProfile.RUNTIME_MODEL_GLIBC,
                ImportedContentHeuristics.inferRuntimeModel(root, parsed, null, "proton-10.0-bionic-label.wcp.xz")
        );
    }

    @Test
    public void glibcElfMarkerClassifiesFlatWineRuntimeAsGlibc() throws Exception {
        File root = temporaryFolder.newFolder("flat-glibc-runtime");
        assertTrue(new File(root, "bin").mkdirs());
        assertTrue(new File(root, "lib/wine/x86_64-unix").mkdirs());
        assertTrue(new File(root, "lib/wine/x86_64-windows").mkdirs());
        assertTrue(new File(root, "bin/wine").createNewFile());
        Files.write(
                new File(root, "lib/wine/x86_64-unix/ntdll.so").toPath(),
                "ELF\0/lib64/ld-linux-x86-64.so.2\0GLIBC_2.38".getBytes(StandardCharsets.UTF_8)
        );
        assertTrue(new File(root, "prefixPack.txz").createNewFile());

        ContentProfile parsed = new ContentProfile();
        parsed.type = ContentProfile.ContentType.CONTENT_TYPE_WINE;
        parsed.verName = "wine-native";
        parsed.runtimeModel = ContentProfile.RUNTIME_MODEL_BIONIC;

        assertEquals(
                ContentProfile.RUNTIME_MODEL_GLIBC,
                ImportedContentHeuristics.inferRuntimeModel(root, parsed, null, "wine-native.wcp")
        );
    }

    @Test
    public void recognizesRecoverableDxvkPayloadWithoutProfile() throws Exception {
        File root = temporaryFolder.newFolder("imported-dxvk");
        assertTrue(new File(root, "system32").mkdirs());
        assertTrue(new File(root, "syswow64").mkdirs());
        assertTrue(new File(root, "system32/d3d11.dll").createNewFile());
        assertTrue(new File(root, "system32/dxgi.dll").createNewFile());

        assertTrue(ImportedContentHeuristics.hasRecoverablePayload(
                root,
                null,
                null,
                "dxvk-2.7.1.zip"
        ));
        assertEquals(
                ContentProfile.ContentType.CONTENT_TYPE_DXVK,
                ImportedContentHeuristics.inferContentType(root, null, null, "dxvk-2.7.1.zip")
        );
    }
}
