package com.winlator.cmod.contents;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PrefixPackCatalogTest {
    @Test
    public void parseSkipsCommentsAndBlankLines() {
        String raw = ""
                + "# comment\n"
                + "\n"
                + "mono\twine-mono-11.0.0-x86.msi\tdownload\thttps://example/mono.msi\twebstack\twinehq\thttps://example/mono/\tinstall-web.cmd\tMono\n"
                + "directx\tdirectx_Jun2010_redist.exe\tdownload\thttps://example/directx.exe\tdirectx\tmicrosoft\thttps://microsoft.invalid\tdirectx.cmd\tDirectX\n";

        List<PrefixPackCatalog.Entry> entries = PrefixPackCatalog.parse(raw);

        assertEquals(2, entries.size());
        assertEquals("mono", entries.get(0).id);
        assertEquals("directx", entries.get(1).id);
        assertEquals(2, PrefixPackCatalog.countByMode(entries, PrefixPackCatalog.MODE_DOWNLOAD));
        assertEquals("https://example/mono/", entries.get(0).sourcePageUrl);
        assertEquals("install-web.cmd", entries.get(0).installCommand);
    }

    @Test
    public void findByIdIsCaseInsensitive() {
        String raw = "vcpp_aio\tVisualCppRedist_AIO_x86_x64.exe\tdownload\thttps://example/vcpp.exe\tvcpp\tabbodi1406/vcredist\thttps://example/vcpp\tinstall-vcpp.cmd\tVC AIO\t0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\n";
        List<PrefixPackCatalog.Entry> entries = PrefixPackCatalog.parse(raw);

        PrefixPackCatalog.Entry match = PrefixPackCatalog.findById(entries, "VCPP_AIO");

        assertNotNull(match);
        assertTrue(match.isDownloadable());
        assertTrue(match.hasSha256());
        assertTrue(match.matchesSha256("0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"));
        assertEquals("VisualCppRedist_AIO_x86_x64.exe", match.fileName);
        assertEquals("install-vcpp.cmd", match.installCommand);
        assertNull(PrefixPackCatalog.findById(entries, "missing"));
    }
}
