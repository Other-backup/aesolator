package com.winlator.cmod.contents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;

public class ManifestComponentHelperTest {
    @Test
    public void findManifestEntryForVersionMatchesBuildSuffix() {
        ManifestData manifest = manifestWith(
                ManifestContentTypes.DXVK,
                List.of(
                        new ManifestEntry("2.7.1-0", "DXVK 2.7.1", "https://example.com/dxvk-2.7.1.wcp", "bionic", ""),
                        new ManifestEntry("2.6.2-0", "DXVK 2.6.2", "https://example.com/dxvk-2.6.2.wcp", "bionic", "")
                )
        );

        ManifestEntry entry = ManifestComponentHelper.findManifestEntryForTypeVersion(
                manifest,
                ContentProfile.ContentType.CONTENT_TYPE_DXVK,
                "2.7.1",
                "bionic"
        );

        assertNotNull(entry);
        assertEquals("2.7.1-0", entry.id);
    }

    @Test
    public void findManifestEntryForVersionMatchesDisplayNameWhenIdCarriesBuildTag() {
        ManifestData manifest = manifestWith(
                ManifestContentTypes.WOWBOX64,
                List.of(new ManifestEntry("0.3.6-0", "WOWBox64 0.3.6", "https://example.com/wowbox64.wcp", "bionic", ""))
        );

        ManifestEntry entry = ManifestComponentHelper.findManifestEntryForTypeVersion(
                manifest,
                ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64,
                "0.3.6",
                "bionic"
        );

        assertNotNull(entry);
        assertEquals("0.3.6-0", entry.id);
    }

    @Test
    public void findManifestEntryForTypeVersionHonorsRequestedVariant() {
        ManifestData manifest = manifestWith(
                ManifestContentTypes.PROTON,
                List.of(
                        new ManifestEntry("proton-10.0-arm64ec-2", "Proton 10.0 ARM64ec", "https://example.com/proton-bionic.wcp", "bionic", "arm64ec"),
                        new ManifestEntry("proton-10.0-x86_64-1", "Proton 10.0 x86_64", "https://example.com/proton-glibc.wcp", "glibc", "x86_64")
                )
        );

        ManifestEntry entry = ManifestComponentHelper.findManifestEntryForTypeVersion(
                manifest,
                ContentProfile.ContentType.CONTENT_TYPE_PROTON,
                "proton-10.0-arm64ec",
                "bionic"
        );

        assertNotNull(entry);
        assertEquals("proton-10.0-arm64ec-2", entry.id);
    }

    @Test
    public void versionExistsMatchesSuffixTaggedInstalledVersions() {
        assertTrue(ManifestComponentHelper.versionExists("2.7.1", List.of("2.7.1-0")));
        assertTrue(ManifestComponentHelper.versionExists("0.3.6-0", List.of("0.3.6")));
    }

    private static ManifestData manifestWith(String key, List<ManifestEntry> entries) {
        LinkedHashMap<String, List<ManifestEntry>> items = new LinkedHashMap<>();
        items.put(key, entries);
        return new ManifestData(1, "2026-04-14", items);
    }
}
