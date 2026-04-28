package com.winlator.cmod.xserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.winlator.cmod.xserver.extensions.XKeyboardExtension;

import org.junit.Test;

public class XClientRequestHandlerTest {
    @Test
    public void describesImplementedCoreRequestsForForensicRows() {
        assertEquals("CreateWindow", XClientRequestHandler.describeCoreOpcode((byte)1));
        assertEquals("QueryTree", XClientRequestHandler.describeCoreOpcode((byte)15));
        assertEquals("GetInputFocus", XClientRequestHandler.describeCoreOpcode((byte)43));
        assertEquals("GetKeyboardMapping", XClientRequestHandler.describeCoreOpcode((byte)101));
        assertEquals("NoOperation", XClientRequestHandler.describeCoreOpcode((byte)127));
    }

    @Test
    public void keepsExtensionOpcodesOutOfCoreNames() {
        assertNull(XClientRequestHandler.describeCoreOpcode(XKeyboardExtension.MAJOR_OPCODE));
    }
}
