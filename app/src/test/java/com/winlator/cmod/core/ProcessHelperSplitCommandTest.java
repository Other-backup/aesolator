package com.winlator.cmod.core;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ProcessHelperSplitCommandTest {
    @Test
    public void splitCommandStripsQuotesFromCmdPayloadPath() {
        assertArrayEquals(
                new String[] {
                        "/data/user/0/com.winlator.cmod/files/imagefs/opt/wine/bin/wine",
                        "C:\\windows\\system32\\cmd.exe",
                        "/c",
                        "C:\\AePrefixPack\\staging\\dotnet_framework\\install-dotnet_framework.cmd"
                },
                ProcessHelper.splitCommand(
                        "/data/user/0/com.winlator.cmod/files/imagefs/opt/wine/bin/wine "
                                + "C:\\windows\\system32\\cmd.exe /c "
                                + "\"C:\\AePrefixPack\\staging\\dotnet_framework\\install-dotnet_framework.cmd\""
                )
        );
    }

    @Test
    public void splitCommandKeepsQuotedExecutableAsSingleTokenWithoutQuoteChars() {
        assertArrayEquals(
                new String[] {
                        "C:\\Program Files\\Tool\\tool.exe",
                        "--flag",
                        "quoted arg"
                },
                ProcessHelper.splitCommand("\"C:\\Program Files\\Tool\\tool.exe\" --flag \"quoted arg\"")
        );
    }

    @Test
    public void parseProcStatHandlesProcessNamesWithSpacesAndParens() {
        ProcessHelper.ProcessInfo info = ProcessHelper.parseProcStat(
                "1234",
                "1234 (wine server(arm64)) S 1200 1 1 0 -1 4194560 0 0 0 0 0 0 0 0 20 0 1 0"
        );

        assertNotNull(info);
        assertEquals(1234, info.pid);
        assertEquals(1200, info.ppid);
        assertEquals("wine server(arm64)", info.name);
    }

    @Test
    public void exitStatusDecoderClassifiesUnixSignalStyleStatuses() {
        assertEquals("ok", ProcessHelper.classifyExitStatus(0));
        assertEquals("exit_code", ProcessHelper.classifyExitStatus(1));
        assertEquals("signal", ProcessHelper.classifyExitStatus(132));
        assertEquals(4, ProcessHelper.resolveSignalFromExitStatus(132));
        assertEquals("SIGILL", ProcessHelper.resolveSignalName(4));
        assertEquals("SIGKILL", ProcessHelper.resolveSignalName(ProcessHelper.resolveSignalFromExitStatus(137)));
        assertEquals("SIGTERM", ProcessHelper.resolveSignalName(ProcessHelper.resolveSignalFromExitStatus(143)));
        assertEquals(0, ProcessHelper.resolveSignalFromExitStatus(255));
    }
}
