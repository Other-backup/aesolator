package com.winlator.cmod.contents;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RuntimeLaunchPolicyTest {
    @Test
    public void promotesLegacyGlibcWineToRemoteProton11Arm64ec() {
        ContentProfile current = glibcWine("wine-glibc-10.10-arm64ec-1", "moze30/winlator-wcp Releases");
        ContentProfile candidate = glibcProton("proton-glibc-11.0-1-arm64ec", "GameNative/proton-wine Releases");
        candidate.remoteUrl = "https://github.com/GameNative/proton-wine/releases/download/build-20260502-1-sdk35/proton-wine-11.0-1-arm64ec.wcp.xz";

        assertTrue(RuntimeLaunchPolicy.shouldPromoteGlibcRuntime(
                current,
                true,
                true,
                candidate,
                false,
                false,
                "wine-glibc-10.10-arm64ec-1",
                ContentProfile.RUNTIME_MODEL_GLIBC
        ));
    }

    @Test
    public void keepsBionicRuntimeIsolatedFromGlibcPromotion() {
        ContentProfile current = bionicWine("wine-bionic-11.0-1-arm64ec");
        ContentProfile candidate = glibcProton("proton-glibc-11.0-1-arm64ec", "GameNative/proton-wine Releases");
        candidate.remoteUrl = "https://github.com/GameNative/proton-wine/releases/download/build-20260502-1-sdk35/proton-wine-11.0-1-arm64ec.wcp.xz";

        assertFalse(RuntimeLaunchPolicy.shouldPromoteGlibcRuntime(
                current,
                true,
                true,
                candidate,
                false,
                false,
                "wine-bionic-11.0-1-arm64ec",
                ContentProfile.RUNTIME_MODEL_BIONIC
        ));
    }

    @Test
    public void promotesGlibcWine10MajorToRemoteProton11() {
        ContentProfile current = glibcWine("wine-10.99", "Waim908/wine-winlator Releases");
        ContentProfile candidate = glibcProton("proton-glibc-11.0-1-x86_64", "GameNative/proton-wine Releases");
        candidate.artifactName = "proton-wine-11.0-1-x86_64.wcp.xz";
        candidate.remoteUrl = "https://github.com/GameNative/proton-wine/releases/download/build-20260502-1-sdk35/proton-wine-11.0-1-x86_64.wcp.xz";

        assertTrue(RuntimeLaunchPolicy.shouldPromoteGlibcRuntime(
                current,
                true,
                true,
                candidate,
                false,
                false,
                "wine-10.99",
                ContentProfile.RUNTIME_MODEL_GLIBC
        ));
    }

    @Test
    public void keepsEqualInstalledProtonRuntime() {
        ContentProfile current = glibcProton("proton-glibc-11.0-1-arm64ec", "GameNative/proton-wine Releases");
        ContentProfile candidate = glibcProton("proton-glibc-11.0-1-arm64ec", "GameNative/proton-wine Releases");

        assertFalse(RuntimeLaunchPolicy.shouldPromoteGlibcRuntime(
                current,
                true,
                true,
                candidate,
                true,
                true,
                "proton-glibc-11.0-1-arm64ec",
                ContentProfile.RUNTIME_MODEL_GLIBC
        ));
    }

    @Test
    public void persistsLegacyGlibcPromotionButNotNormalBionicChoice() {
        assertTrue(RuntimeLaunchPolicy.shouldPersistPromotedGlibcRuntime(
                "wine-glibc-10.10-arm64ec-1",
                "proton-glibc-11.0-1-arm64ec",
                ContentProfile.RUNTIME_MODEL_GLIBC
        ));
        assertFalse(RuntimeLaunchPolicy.shouldPersistPromotedGlibcRuntime(
                "wine-bionic-11.0-1-arm64ec",
                "proton-bionic-11.0-1-arm64ec",
                ContentProfile.RUNTIME_MODEL_BIONIC
        ));
    }

    private static ContentProfile glibcWine(String verName, String sourceRepo) {
        ContentProfile profile = new ContentProfile();
        profile.type = ContentProfile.ContentType.CONTENT_TYPE_WINE;
        profile.verName = verName;
        profile.runtimeModel = ContentProfile.RUNTIME_MODEL_GLIBC;
        profile.sourceRepo = sourceRepo;
        profile.artifactName = verName + ".wcp";
        return profile;
    }

    private static ContentProfile bionicWine(String verName) {
        ContentProfile profile = new ContentProfile();
        profile.type = ContentProfile.ContentType.CONTENT_TYPE_WINE;
        profile.verName = verName;
        profile.runtimeModel = ContentProfile.RUNTIME_MODEL_BIONIC;
        profile.sourceRepo = "GameNative/proton-wine Releases";
        profile.artifactName = verName + ".wcp";
        return profile;
    }

    private static ContentProfile glibcProton(String verName, String sourceRepo) {
        ContentProfile profile = new ContentProfile();
        profile.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        profile.verName = verName;
        profile.runtimeModel = ContentProfile.RUNTIME_MODEL_GLIBC;
        profile.sourceRepo = sourceRepo;
        profile.artifactName = "proton-wine-11.0-1-arm64ec.wcp.xz";
        profile.releaseTag = "build-20260502-1-sdk35";
        return profile;
    }
}
