package com.winlator.cmod.xserver.extensions;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class GenericEventExtensionTest {
    @Test
    public void exposesXorgGenericEventContract() {
        GenericEventExtension extension = new GenericEventExtension();

        assertEquals("Generic Event Extension", extension.getName());
        assertEquals(145, Byte.toUnsignedInt(extension.getMajorOpcode()));
        assertEquals(0, extension.getNumEvents());
        assertEquals(0, extension.getNumErrors());
    }

    @Test
    public void negotiateVersionDoesNotAdvertiseAboveServerSupport() {
        assertArrayEquals(new short[]{1, 0}, GenericEventExtension.negotiateVersion(1, 0));
        assertArrayEquals(new short[]{1, 0}, GenericEventExtension.negotiateVersion(1, 5));
        assertArrayEquals(new short[]{1, 0}, GenericEventExtension.negotiateVersion(2, 0));
        assertArrayEquals(new short[]{0, 9}, GenericEventExtension.negotiateVersion(0, 9));
    }

    @Test
    public void describesXKeyboardMinorOpcodesForForensicRows() {
        assertEquals("UseExtension", XKeyboardExtension.describeMinorOpcode(0));
        assertEquals("GetMap", XKeyboardExtension.describeMinorOpcode(8));
        assertEquals("PerClientFlags", XKeyboardExtension.describeMinorOpcode(21));
        assertEquals("SetDebuggingFlags", XKeyboardExtension.describeMinorOpcode(101));
        assertEquals("Unknown", XKeyboardExtension.describeMinorOpcode(255));
    }

    @Test
    public void xKeyboardClientMapPathUsesCoreFallbackUntilFullContractExists() {
        assertEquals("partial_xkb_map_contract_stalls_wine_before_window_creation",
                XKeyboardExtension.clientXkbDisableReasonForTests());
        assertFalse(XKeyboardExtension.isClientXkbExtensionSupportedForTests());
    }
}
