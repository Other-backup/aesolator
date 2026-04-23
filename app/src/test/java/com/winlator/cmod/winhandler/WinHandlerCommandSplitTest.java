package com.winlator.cmod.winhandler;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class WinHandlerCommandSplitTest {
    @Test
    public void splitCommandKeepsCmdExeAsExecutableWhenParametersContainQuotes() {
        assertArrayEquals(
                new String[] {
                        "cmd.exe",
                        "/c start \"\" \"C:\\AePrefixPack\\cache\\glview6499-setup.exe\""
                },
                WinHandler.splitCommand("cmd.exe /c start \"\" \"C:\\AePrefixPack\\cache\\glview6499-setup.exe\"")
        );
    }

    @Test
    public void splitCommandSupportsQuotedExecutablePath() {
        assertArrayEquals(
                new String[] {
                        "C:\\Program Files\\Tool\\tool.exe",
                        "--flag value"
                },
                WinHandler.splitCommand("\"C:\\Program Files\\Tool\\tool.exe\" --flag value")
        );
    }

    @Test
    public void splitCommandKeepsCmdCallPayloadAsSingleArgument() {
        assertArrayEquals(
                new String[] {
                        "cmd.exe",
                        "/c call \"C:\\AePrefixPack\\staging\\dotnet_framework\\install-dotnet_framework.cmd\""
                },
                WinHandler.splitCommand("cmd.exe /c call \"C:\\AePrefixPack\\staging\\dotnet_framework\\install-dotnet_framework.cmd\"")
        );
    }
}
