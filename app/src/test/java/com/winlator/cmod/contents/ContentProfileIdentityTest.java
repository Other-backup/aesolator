package com.winlator.cmod.contents;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ContentProfileIdentityTest {
    @Test
    public void acceptsRemoteRuntimeBuildSuffixForEquivalentProtonProfile() {
        ContentProfile actual = new ContentProfile();
        actual.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        actual.verName = "11.0-arm64ec";

        ContentProfile remote = new ContentProfile();
        remote.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        remote.verName = "proton-11.0-arm64ec-20260422.1559";

        assertFalse(ContentProfileIdentity.isRemoteProfileIdentityMismatch(actual, remote));
    }

    @Test
    public void acceptsStableNightliesAliasForEquivalentInstalledProtonProfile() {
        ContentProfile actual = new ContentProfile();
        actual.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        actual.verName = "11.0-1-arm64ec";

        ContentProfile remote = new ContentProfile();
        remote.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        remote.verName = "proton-wine-11.0-arm64ec-20260422.1559";
        remote.artifactName = "proton-wine-11.0-arm64ec-20260422.1559.wcp.xz";

        assertFalse(ContentProfileIdentity.isRemoteProfileIdentityMismatch(actual, remote));
        assertTrue(ContentProfileIdentity.areEquivalentProfiles(actual, remote));
    }

    @Test
    public void acceptsBleedingEdgeAliasForRollingProtonRuntime() {
        ContentProfile actual = new ContentProfile();
        actual.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        actual.verName = "10.0.99-arm64ec";

        ContentProfile remote = new ContentProfile();
        remote.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        remote.verName = "proton-wine-proton-bleeding-edge-20260414-5edc831-arm64ec";
        remote.artifactName = "proton-wine-proton-bleeding-edge-20260414-5edc831-arm64ec.wcp.xz";
        remote.releaseTag = "proton-bleeding-edge-20260414-5edc831-run180";

        assertFalse(ContentProfileIdentity.isRemoteProfileIdentityMismatch(actual, remote));
        assertTrue(ContentProfileIdentity.areEquivalentProfiles(actual, remote));
        assertTrue(ContentProfileIdentity.isRuntimeAliasEquivalent(actual, remote));
    }

    @Test
    public void acceptsBleedingEdgeAliasForRollingProtonRuntimeWithCapabilitySuffix() {
        ContentProfile actual = new ContentProfile();
        actual.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        actual.verName = "10.0.99-arm64ec-ntsync";

        ContentProfile remote = new ContentProfile();
        remote.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        remote.verName = "proton-wine-proton-bleeding-edge-20260414-5edc831-arm64ec-ntsync";
        remote.artifactName = "proton-wine-proton-bleeding-edge-20260414-5edc831-arm64ec-ntsync.wcp.xz";
        remote.releaseTag = "proton-bleeding-edge-20260414-5edc831-run180";

        assertFalse(ContentProfileIdentity.isRemoteProfileIdentityMismatch(actual, remote));
        assertTrue(ContentProfileIdentity.areEquivalentProfiles(actual, remote));
        assertTrue(ContentProfileIdentity.isRuntimeAliasEquivalent(actual, remote));
    }

    @Test
    public void rejectsDifferentRuntimeMajorVersion() {
        ContentProfile actual = new ContentProfile();
        actual.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        actual.verName = "10-arm64ec";

        ContentProfile remote = new ContentProfile();
        remote.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        remote.verName = "proton-11.0-arm64ec-20260422.1559";

        assertTrue(ContentProfileIdentity.isRemoteProfileIdentityMismatch(actual, remote));
    }

    @Test
    public void keepsBleedingEdgeNightliesDistinctFromStableInstalledRuntime() {
        ContentProfile actual = new ContentProfile();
        actual.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        actual.verName = "11.0-arm64ec";

        ContentProfile remote = new ContentProfile();
        remote.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        remote.verName = "proton-wine-proton-bleeding-edge-20260414-5edc831-arm64ec";
        remote.artifactName = "proton-wine-proton-bleeding-edge-20260414-5edc831-arm64ec.wcp.xz";

        assertTrue(ContentProfileIdentity.isRemoteProfileIdentityMismatch(actual, remote));
        assertTrue(!ContentProfileIdentity.areEquivalentProfiles(actual, remote));
    }

    @Test
    public void keepsExactIdentityForNonRuntimePackages() {
        ContentProfile actual = new ContentProfile();
        actual.type = ContentProfile.ContentType.CONTENT_TYPE_DXVK;
        actual.verName = "2.7.1";

        ContentProfile remote = new ContentProfile();
        remote.type = ContentProfile.ContentType.CONTENT_TYPE_DXVK;
        remote.verName = "2.7.1-hotfix";

        assertTrue(ContentProfileIdentity.isRemoteProfileIdentityMismatch(actual, remote));
    }

    @Test
    public void doesNotTreatExactRuntimeIdentityAsAliasEquivalence() {
        ContentProfile actual = new ContentProfile();
        actual.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        actual.verName = "11.0-arm64ec";

        ContentProfile remote = new ContentProfile();
        remote.type = ContentProfile.ContentType.CONTENT_TYPE_PROTON;
        remote.verName = "11.0-arm64ec";

        assertTrue(ContentProfileIdentity.areEquivalentProfiles(actual, remote));
        assertFalse(ContentProfileIdentity.isRuntimeAliasEquivalent(actual, remote));
    }
}
