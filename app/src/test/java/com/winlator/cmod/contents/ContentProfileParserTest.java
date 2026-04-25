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
}
