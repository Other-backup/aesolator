package com.winlator.cmod.contents;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ContentProfileParserTest {
    @Test
    public void parsesLegacyWineRuntimeProfileWithoutModernMetadata() {
        String rawJson = "{\n"
                + "  \"type\": \"Wine\",\n"
                + "  \"versionName\": \"9.20-x86_64\",\n"
                + "  \"versionCode\": 0,\n"
                + "  \"description\": \"Wine 9.20 x86_64 for newer cmod versions\",\n"
                + "  \"files\": [],\n"
                + "  \"wine\": {\n"
                + "    \"binPath\": \"bin\",\n"
                + "    \"libPath\": \"lib\",\n"
                + "    \"prefixPack\": \"prefixPack.txz\"\n"
                + "  }\n"
                + "}";

        ContentProfile profile = ContentProfileParser.parse(rawJson);

        assertNotNull(profile);
        assertEquals(ContentProfile.ContentType.CONTENT_TYPE_WINE, profile.type);
        assertEquals("bin", profile.wineBinPath);
        assertEquals("lib", profile.wineLibPath);
        assertEquals("prefixPack.txz", profile.winePrefixPack);
    }

    @Test
    public void parsesForeignFexCoreProfile() {
        String rawJson = "{\n"
                + "  \"type\": \"FEXCore\",\n"
                + "  \"versionName\": \"2505\",\n"
                + "  \"versionCode\": 1,\n"
                + "  \"description\": \"FEXCore 2505 from Ubuntu PPA releases\",\n"
                + "  \"files\": [\n"
                + "    {\n"
                + "      \"source\": \"system32/libarm64ecfex.dll\",\n"
                + "      \"target\": \"${system32}/libarm64ecfex.dll\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"source\": \"system32/libwow64fex.dll\",\n"
                + "      \"target\": \"${system32}/libwow64fex.dll\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        ContentProfile profile = ContentProfileParser.parse(rawJson);

        assertNotNull(profile);
        assertEquals(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE, profile.type);
        assertEquals(2, profile.fileList.size());
    }

    @Test
    public void parsesForeignBox64Profile() {
        String rawJson = "{\n"
                + "  \"type\": \"Box64\",\n"
                + "  \"versionName\": \"0.4.1-fix\",\n"
                + "  \"versionCode\": 0,\n"
                + "  \"description\": \"Box64-0.4.1-aca2450 | Built from Pypetto-Crypto.\",\n"
                + "  \"files\": [\n"
                + "    {\n"
                + "      \"source\": \"box64\",\n"
                + "      \"target\": \"${bindir}/box64\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        ContentProfile profile = ContentProfileParser.parse(rawJson);

        assertNotNull(profile);
        assertEquals(ContentProfile.ContentType.CONTENT_TYPE_BOX64, profile.type);
        assertEquals(1, profile.fileList.size());
        assertEquals("${bindir}/box64", profile.fileList.get(0).target);
    }

    @Test
    public void keepsRepairableRuntimeProfileWithoutExplicitPaths() {
        String rawJson = "{\n"
                + "  \"type\": \"Proton\",\n"
                + "  \"versionName\": \"11.0-arm64ec\",\n"
                + "  \"versionCode\": 1,\n"
                + "  \"description\": \"Proton donor profile with runtime repair expected\"\n"
                + "}";

        ContentProfile profile = ContentProfileParser.parse(rawJson);

        assertNotNull(profile);
        assertEquals(ContentProfile.ContentType.CONTENT_TYPE_PROTON, profile.type);
        assertEquals("11.0-arm64ec", profile.verName);
    }

    @Test
    public void parsesRuntimeModelFromForeignRuntimeClassHints() {
        String rawJson = "{\n"
                + "  \"type\": \"Wine\",\n"
                + "  \"versionName\": \"freewine-11.0-arm64ec\",\n"
                + "  \"runtimeClassTarget\": \"bionic-native\",\n"
                + "  \"wine\": {\n"
                + "    \"binPath\": \"arm64-v8a/bin\",\n"
                + "    \"libPath\": \"arm64-v8a/lib\",\n"
                + "    \"prefixPack\": \"prefixPack.txz\"\n"
                + "  }\n"
                + "}";

        ContentProfile profile = ContentProfileParser.parse(rawJson);

        assertNotNull(profile);
        assertEquals(ContentProfile.RUNTIME_MODEL_BIONIC, profile.getRuntimeModel());
    }

    @Test
    public void bionicRuntimeMarkersOverrideGenericDonorGlibcLabels() {
        String rawJson = "{\n"
                + "  \"type\": \"Proton\",\n"
                + "  \"versionName\": \"10.0.99-arm64ec-ntsync\",\n"
                + "  \"runtimeModel\": \"GameNative glibc donor bionic-native arm64-v8a/bin\",\n"
                + "  \"proton\": {\n"
                + "    \"binPath\": \"arm64-v8a/bin\",\n"
                + "    \"libPath\": \"arm64-v8a/lib\",\n"
                + "    \"prefixPack\": \"prefixPack.txz\"\n"
                + "  }\n"
                + "}";

        ContentProfile profile = ContentProfileParser.parse(rawJson);

        assertNotNull(profile);
        assertEquals(ContentProfile.RUNTIME_MODEL_BIONIC, profile.getRuntimeModel());
    }

    @Test
    public void parsesForeignRuntimeProfileAliases() {
        String rawJson = "{\n"
                + "  \"componentType\": \"Proton\",\n"
                + "  \"version\": \"proton-11-arm64ec\",\n"
                + "  \"version_code\": \"20260425\",\n"
                + "  \"summary\": \"foreign package schema\",\n"
                + "  \"runtime\": {\n"
                + "    \"runtime_model\": \"bionic-native\",\n"
                + "    \"bin\": \"arm64-v8a/bin\",\n"
                + "    \"libs\": \"arm64-v8a/lib\",\n"
                + "    \"prefix_pack\": \"arm64-v8a/share/wine/prefixPack.txz\"\n"
                + "  }\n"
                + "}";

        ContentProfile profile = ContentProfileParser.parse(rawJson);

        assertNotNull(profile);
        assertEquals(ContentProfile.ContentType.CONTENT_TYPE_PROTON, profile.type);
        assertEquals("proton-11-arm64ec", profile.verName);
        assertEquals(20260425, profile.verCode);
        assertEquals("arm64-v8a/bin", profile.wineBinPath);
        assertEquals("arm64-v8a/lib", profile.wineLibPath);
        assertEquals("arm64-v8a/share/wine/prefixPack.txz", profile.winePrefixPack);
        assertEquals(ContentProfile.RUNTIME_MODEL_BIONIC, profile.getRuntimeModel());
    }

    @Test
    public void parsesForeignComponentFileAliases() {
        String rawJson = "{\n"
                + "  \"packageType\": \"Box64\",\n"
                + "  \"version\": \"0.5.0\",\n"
                + "  \"code\": 7,\n"
                + "  \"installFiles\": [\n"
                + "    {\"from\":\"payload/bin/box64\", \"destination\":\"${localbin}/box64\"}\n"
                + "  ]\n"
                + "}";

        ContentProfile profile = ContentProfileParser.parse(rawJson);

        assertNotNull(profile);
        assertEquals(ContentProfile.ContentType.CONTENT_TYPE_BOX64, profile.type);
        assertEquals("payload/bin/box64", profile.fileList.get(0).source);
        assertEquals("${localbin}/box64", profile.fileList.get(0).target);
    }
}
