package com.winlator.cmod.core;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

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
}
