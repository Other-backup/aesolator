package com.winlator.cmod.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.winlator.cmod.contents.ContentProfile;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class WineInfoTest {
    @Test
    public void parseProfileIdentifierAcceptsArchlessProtonProfileWithArm64Tag() throws Exception {
        ContentProfile profile = new ContentProfile();
        profile.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        profile.verName = "Proton-10.0";
        profile.wineLibPath = "lib/wine/aarch64-windows";

        Object parsed = invokeParseProfileIdentifier(profile);

        assertNotNull(parsed);
        assertEquals("proton", getParsedField(parsed, "type"));
        assertEquals("10.0", getParsedField(parsed, "version"));
        assertEquals("arm64", getParsedField(parsed, "arch"));
    }

    @Test
    public void parseProfileIdentifierAcceptsArchlessWineProfileWithArchitectureTag() throws Exception {
        ContentProfile profile = new ContentProfile();
        profile.type = ContentProfile.ContentType.CONTENT_TYPE_WINE;
        profile.verName = "9.22-X86_64";

        Object parsed = invokeParseProfileIdentifier(profile);

        assertNotNull(parsed);
        assertEquals("wine", getParsedField(parsed, "type"));
        assertEquals("9.22", getParsedField(parsed, "version"));
        assertEquals("x86_64", getParsedField(parsed, "arch"));
    }

    @Test
    public void parseProfileIdentifierPreservesSuffixAfterArchForProtonProfiles() throws Exception {
        ContentProfile profile = new ContentProfile();
        profile.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        profile.verName = "10.0.99-arm64ec-ntsync";

        Object parsed = invokeParseProfileIdentifier(profile);

        assertNotNull(parsed);
        assertEquals("proton", getParsedField(parsed, "type"));
        assertEquals("10.0.99-ntsync", getParsedField(parsed, "version"));
        assertEquals("arm64ec", getParsedField(parsed, "arch"));
    }

    @Test
    public void parseIdentifierAcceptsSuffixAfterArch() throws Exception {
        Object parsed = invokeParseIdentifier("proton-10.0.99-arm64ec-ntsync");

        assertNotNull(parsed);
        assertEquals("proton", getParsedField(parsed, "type"));
        assertEquals("10.0.99-ntsync", getParsedField(parsed, "version"));
        assertEquals("arm64ec", getParsedField(parsed, "arch"));
    }

    @Test
    public void parseIdentifierNormalizesAmd64Alias() throws Exception {
        Object parsed = invokeParseIdentifier("wine-10.15-amd64");

        assertNotNull(parsed);
        assertEquals("wine", getParsedField(parsed, "type"));
        assertEquals("10.15", getParsedField(parsed, "version"));
        assertEquals("x86_64", getParsedField(parsed, "arch"));
    }

    @Test
    public void parseIdentifierNormalizesAmd64AliasWithBuildStamp() throws Exception {
        Object parsed = invokeParseIdentifier("wine-10.15-amd64-2026041211");

        assertNotNull(parsed);
        assertEquals("wine", getParsedField(parsed, "type"));
        assertEquals("10.15-2026041211", getParsedField(parsed, "version"));
        assertEquals("x86_64", getParsedField(parsed, "arch"));
    }

    @Test
    public void parseProfileIdentifierNormalizesGlibcAmd64WineProfile() throws Exception {
        ContentProfile profile = new ContentProfile();
        profile.type = ContentProfile.ContentType.CONTENT_TYPE_WINE;
        profile.verName = "wine-10.15-amd64";
        profile.runtimeModel = ContentProfile.RUNTIME_MODEL_GLIBC;
        profile.artifactName = "imagefs-runtime-glibc-wine-glibc-wine-10.15-amd64-2026041211";
        profile.wineBinPath = "bin";
        profile.wineLibPath = "lib";
        profile.winePrefixPack = "prefixPack.txz";

        Object parsed = invokeParseProfileIdentifier(profile);

        assertNotNull(parsed);
        assertEquals("wine", getParsedField(parsed, "type"));
        assertEquals("10.15", getParsedField(parsed, "version"));
        assertEquals("x86_64", getParsedField(parsed, "arch"));
    }

    private static Object invokeParseProfileIdentifier(ContentProfile profile) throws Exception {
        Method method = WineInfo.class.getDeclaredMethod("parseProfileIdentifier", ContentProfile.class);
        method.setAccessible(true);
        return method.invoke(null, profile);
    }

    private static Object invokeParseIdentifier(String identifier) throws Exception {
        Method method = WineInfo.class.getDeclaredMethod("parseIdentifier", String.class);
        method.setAccessible(true);
        return method.invoke(null, identifier);
    }

    private static String getParsedField(Object parsed, String fieldName) throws Exception {
        Field field = parsed.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return String.valueOf(field.get(parsed));
    }
}
