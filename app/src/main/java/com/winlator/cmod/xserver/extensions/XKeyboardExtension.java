package com.winlator.cmod.xserver.extensions;

import static com.winlator.cmod.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.Keyboard;
import com.winlator.cmod.xserver.XClient;
import com.winlator.cmod.xserver.errors.XRequestError;
import com.winlator.cmod.xserver.XKeycode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XKeyboardExtension implements Extension {
    public static final byte MAJOR_OPCODE = -109;
    private static final short SERVER_MAJOR_VERSION = 1;
    private static final short SERVER_MINOR_VERSION = 0;
    private static final int XKB_KEY_TYPES_MASK = 1 << 0;
    private static final int XKB_KEY_SYMS_MASK = 1 << 1;
    private static final int XKB_MODIFIER_MAP_MASK = 1 << 2;
    private static final int XKB_ALL_MAP_COMPONENTS_MASK = 0xff;
    private static final int XKB_ALL_CLIENT_INFO_MASK = XKB_KEY_TYPES_MASK | XKB_KEY_SYMS_MASK | XKB_MODIFIER_MAP_MASK;
    private static final int XKB_REPEAT_KEYS_MASK = 1;
    private static final int XKB_DETECTABLE_AUTO_REPEAT_MASK = 1;
    private static final boolean CLIENT_XKB_EXTENSION_SUPPORTED = false;
    private static final String CLIENT_XKB_DISABLE_REASON = "partial_xkb_map_contract_stalls_wine_before_window_creation";
    private byte firstEventId = 0;
    private byte firstErrorId = 0;

    private static abstract class ClientOpcodes {
        private static final byte USE_EXTENSION = 0;
        private static final byte SELECT_EVENTS = 1;
        private static final byte BELL = 3;
        private static final byte GET_STATE = 4;
        private static final byte LATCH_LOCK_STATE = 5;
        private static final byte GET_CONTROLS = 6;
        private static final byte SET_CONTROLS = 7;
        private static final byte GET_MAP = 8;
        private static final byte SET_MAP = 9;
        private static final byte GET_COMPAT_MAP = 10;
        private static final byte SET_COMPAT_MAP = 11;
        private static final byte GET_INDICATOR_STATE = 12;
        private static final byte GET_INDICATOR_MAP = 13;
        private static final byte SET_INDICATOR_MAP = 14;
        private static final byte GET_NAMED_INDICATOR = 15;
        private static final byte SET_NAMED_INDICATOR = 16;
        private static final byte GET_NAMES = 17;
        private static final byte SET_NAMES = 18;
        private static final byte GET_GEOMETRY = 19;
        private static final byte SET_GEOMETRY = 20;
        private static final byte PER_CLIENT_FLAGS = 21;
        private static final byte LIST_COMPONENTS = 22;
        private static final byte GET_KBD_BY_NAME = 23;
        private static final byte GET_DEVICE_INFO = 24;
        private static final byte SET_DEVICE_INFO = 25;
        private static final byte SET_DEBUGGING_FLAGS = 101;
    }

    @Override
    public String getName() {
        return "XKEYBOARD";
    }

    @Override
    public byte getMajorOpcode() {
        return MAJOR_OPCODE;
    }

    @Override
    public byte getFirstErrorId() {
        return firstErrorId;
    }

    @Override
    public byte getFirstEventId() {
        return firstEventId;
    }

    @Override
    public int getNumEvents() {
        return 1;
    }

    @Override
    public int getNumErrors() {
        return 1;
    }

    @Override
    public void setFirstEventId(byte firstEventId) {
        this.firstEventId = firstEventId;
    }

    @Override
    public void setFirstErrorId(byte firstErrorId) {
        this.firstErrorId = firstErrorId;
    }

    private void useExtension(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        short requestedMajor = inputStream.readShort();
        short requestedMinor = inputStream.readShort();
        inputStream.skip(client.getRemainingRequestLength());

        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XSERVER_XKEYBOARD_USE_EXTENSION",
                null,
                "xserver_extensions",
                "xkeyboard_use_extension",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "sequence_number", Short.toUnsignedInt(client.getSequenceNumber()),
                        "requested_major", requestedMajor,
                        "requested_minor", requestedMinor,
                        "server_major", SERVER_MAJOR_VERSION,
                        "server_minor", SERVER_MINOR_VERSION,
                        "supported", CLIENT_XKB_EXTENSION_SUPPORTED,
                        "compatibility_policy", "core_keyboard_fallback",
                        "reason", CLIENT_XKB_DISABLE_REASON
                )
        );

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)(CLIENT_XKB_EXTENSION_SUPPORTED ? 1 : 0));
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort(SERVER_MAJOR_VERSION);
            outputStream.writeShort(SERVER_MINOR_VERSION);
            outputStream.writePad(20);
        }
    }

    private void getState(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        logReply(client, "XSERVER_XKEYBOARD_GET_STATE_REPLY", "xkeyboard_get_state_reply",
                "mods", client.xServer.keyboard.getModifiersMask().getBits());
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeByte((byte)client.xServer.keyboard.getModifiersMask().getBits());
            outputStream.writeByte((byte)client.xServer.keyboard.getModifiersMask().getBits());
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)0);
            outputStream.writePad(6);
        }
    }

    private void getControls(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        logReply(client, "XSERVER_XKEYBOARD_GET_CONTROLS_REPLY", "xkeyboard_get_controls_reply",
                "enabled_controls", XKB_REPEAT_KEYS_MASK);
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(15);
            outputStream.writeByte((byte)1);
            outputStream.writeByte((byte)1);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)660);
            outputStream.writeShort((short)40);
            outputStream.writeShort((short)300);
            outputStream.writeShort((short)300);
            outputStream.writeShort((short)160);
            outputStream.writeShort((short)40);
            outputStream.writeShort((short)30);
            outputStream.writeShort((short)10);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(XKB_REPEAT_KEYS_MASK);
            outputStream.writePad(32);
        }
    }

    private void getMap(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        int deviceSpec = inputStream.readUnsignedShort();
        int full = inputStream.readUnsignedShort();
        int partial = inputStream.readUnsignedShort();
        int firstType = inputStream.readUnsignedByte();
        int nTypes = inputStream.readUnsignedByte();
        int firstKeySym = inputStream.readUnsignedByte();
        int nKeySyms = inputStream.readUnsignedByte();
        inputStream.skip(2);
        inputStream.skip(2);
        int virtualMods = inputStream.readUnsignedShort();
        inputStream.skip(2);
        int firstModMapKey = inputStream.readUnsignedByte();
        int nModMapKeys = inputStream.readUnsignedByte();
        inputStream.skip(client.getRemainingRequestLength());

        int requested = (full | partial) & XKB_ALL_MAP_COMPONENTS_MASK;
        int present = requested & XKB_ALL_CLIENT_INFO_MASK;
        boolean includeTypes = (present & XKB_KEY_TYPES_MASK) != 0;
        boolean includeSyms = (present & XKB_KEY_SYMS_MASK) != 0;
        boolean includeModMap = (present & XKB_MODIFIER_MAP_MASK) != 0;

        int typeFirst = includeTypes ? ((partial & XKB_KEY_TYPES_MASK) != 0 ? clamp(firstType, 0, 1) : 0) : 0;
        int typeCount = includeTypes ? ((partial & XKB_KEY_TYPES_MASK) != 0 ? clamp(nTypes, 0, 2 - typeFirst) : 2) : 0;
        int symFirst = includeSyms ? ((partial & XKB_KEY_SYMS_MASK) != 0 ? clamp(firstKeySym, Keyboard.MIN_KEYCODE, Keyboard.MAX_KEYCODE) : Keyboard.MIN_KEYCODE) : 0;
        int symCount = includeSyms ? ((partial & XKB_KEY_SYMS_MASK) != 0 ? clamp(nKeySyms, 0, Keyboard.MAX_KEYCODE - symFirst + 1) : Keyboard.KEYS_COUNT) : 0;
        int modFirst = includeModMap ? ((partial & XKB_MODIFIER_MAP_MASK) != 0 ? clamp(firstModMapKey, Keyboard.MIN_KEYCODE, Keyboard.MAX_KEYCODE) : Keyboard.MIN_KEYCODE) : 0;
        int modRange = includeModMap ? ((partial & XKB_MODIFIER_MAP_MASK) != 0 ? clamp(nModMapKeys, 0, Keyboard.MAX_KEYCODE - modFirst + 1) : Keyboard.KEYS_COUNT) : 0;
        List<int[]> modMappings = includeModMap ? modifierMappings(modFirst, modRange) : new ArrayList<>();

        int typesBytes = includeTypes ? keyTypesBytes(typeFirst, typeCount) : 0;
        int symsBytes = includeSyms ? symCount * (8 + Keyboard.KEYSYMS_PER_KEYCODE * 4) : 0;
        int modMapBytes = modMappings.size() * 2;
        int modMapPad = pad4(modMapBytes);
        int variableBytes = typesBytes + symsBytes + modMapBytes + modMapPad;

        logMapReply(client, deviceSpec, full, partial, present, symFirst, symCount, modMappings.size(), variableBytes);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(2 + (variableBytes / 4));
            outputStream.writeShort((short)0);
            outputStream.writeByte((byte)Keyboard.MIN_KEYCODE);
            outputStream.writeByte((byte)Keyboard.MAX_KEYCODE);
            outputStream.writeShort((short)present);
            outputStream.writeByte((byte)typeFirst);
            outputStream.writeByte((byte)typeCount);
            outputStream.writeByte((byte)(includeTypes ? 2 : 0));
            outputStream.writeByte((byte)symFirst);
            outputStream.writeShort((short)(symCount * Keyboard.KEYSYMS_PER_KEYCODE));
            outputStream.writeByte((byte)symCount);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)modFirst);
            outputStream.writeByte((byte)modRange);
            outputStream.writeByte((byte)modMappings.size());
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)0);

            if (includeTypes) writeKeyTypes(outputStream, typeFirst, typeCount);
            if (includeSyms) writeKeySyms(client, outputStream, symFirst, symCount);
            if (includeModMap) {
                for (int[] mapping : modMappings) {
                    outputStream.writeByte((byte)mapping[0]);
                    outputStream.writeByte((byte)mapping[1]);
                }
                outputStream.writePad(modMapPad);
            }
        }
    }

    private void getNames(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        logReply(client, "XSERVER_XKEYBOARD_GET_NAMES_REPLY", "xkeyboard_get_names_reply");
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeByte((byte)Keyboard.MIN_KEYCODE);
            outputStream.writeByte((byte)Keyboard.MAX_KEYCODE);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeInt(0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)0);
            outputStream.writePad(4);
        }
    }

    private void perClientFlags(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(2);
        inputStream.skip(2);
        int change = inputStream.readInt();
        int value = inputStream.readInt();
        inputStream.skip(client.getRemainingRequestLength());
        int effective = value & change & XKB_DETECTABLE_AUTO_REPEAT_MASK;
        logReply(client, "XSERVER_XKEYBOARD_PER_CLIENT_FLAGS_REPLY", "xkeyboard_per_client_flags_reply",
                "change", change,
                "value", value,
                "effective", effective);
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(XKB_DETECTABLE_AUTO_REPEAT_MASK);
            outputStream.writeInt(effective);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
        }
    }

    private void getCompatMap(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        logReply(client, "XSERVER_XKEYBOARD_GET_COMPAT_MAP_REPLY", "xkeyboard_get_compat_map_reply");
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writePad(16);
        }
    }

    private void getIndicatorState(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        logReply(client, "XSERVER_XKEYBOARD_GET_INDICATOR_STATE_REPLY", "xkeyboard_get_indicator_state_reply",
                "state", 0);
        writeFixedIntReply(client, outputStream, 0);
    }

    private void getIndicatorMap(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        logReply(client, "XSERVER_XKEYBOARD_GET_INDICATOR_MAP_REPLY", "xkeyboard_get_indicator_map_reply");
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeByte((byte)0);
            outputStream.writePad(15);
        }
    }

    private void getNamedIndicator(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        logReply(client, "XSERVER_XKEYBOARD_GET_NAMED_INDICATOR_REPLY", "xkeyboard_get_named_indicator_reply");
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)0);
            outputStream.writeInt(0);
            outputStream.writeByte((byte)0);
            outputStream.writePad(3);
        }
    }

    private void getGeometry(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        logReply(client, "XSERVER_XKEYBOARD_GET_GEOMETRY_REPLY", "xkeyboard_get_geometry_reply");
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
        }
    }

    private void listComponents(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        logReply(client, "XSERVER_XKEYBOARD_LIST_COMPONENTS_REPLY", "xkeyboard_list_components_reply");
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writePad(8);
        }
    }

    private void getKbdByName(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        logReply(client, "XSERVER_XKEYBOARD_GET_KBD_BY_NAME_REPLY", "xkeyboard_get_kbd_by_name_reply",
                "min_keycode", Keyboard.MIN_KEYCODE,
                "max_keycode", Keyboard.MAX_KEYCODE);
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeByte((byte)Keyboard.MIN_KEYCODE);
            outputStream.writeByte((byte)Keyboard.MAX_KEYCODE);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writePad(16);
        }
    }

    private void getDeviceInfo(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        logReply(client, "XSERVER_XKEYBOARD_GET_DEVICE_INFO_REPLY", "xkeyboard_get_device_info_reply");
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeInt(0);
        }
    }

    private void setDebuggingFlags(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        logReply(client, "XSERVER_XKEYBOARD_SET_DEBUGGING_FLAGS_REPLY", "xkeyboard_set_debugging_flags_reply");
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeInt(0);
        }
    }

    private void writeFixedIntReply(XClient client, XOutputStream outputStream, int value) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(value);
            outputStream.writePad(20);
        }
    }

    private void writeKeyTypes(XOutputStream outputStream, int firstType, int count) {
        for (int type = firstType; type < firstType + count; type++) {
            if (type == 0) {
                outputStream.writeByte((byte)0);
                outputStream.writeByte((byte)0);
                outputStream.writeShort((short)0);
                outputStream.writeByte((byte)1);
                outputStream.writeByte((byte)0);
                outputStream.writeByte((byte)0);
                outputStream.writeByte((byte)0);
            }
            else {
                outputStream.writeByte((byte)1);
                outputStream.writeByte((byte)1);
                outputStream.writeShort((short)0);
                outputStream.writeByte((byte)2);
                outputStream.writeByte((byte)1);
                outputStream.writeByte((byte)0);
                outputStream.writeByte((byte)0);
                outputStream.writeByte((byte)1);
                outputStream.writeByte((byte)1);
                outputStream.writeByte((byte)1);
                outputStream.writeByte((byte)1);
                outputStream.writeShort((short)0);
                outputStream.writeShort((short)0);
            }
        }
    }

    private void writeKeySyms(XClient client, XOutputStream outputStream, int firstKeycode, int count) {
        for (int keycode = firstKeycode; keycode < firstKeycode + count; keycode++) {
            int base = client.xServer.keyboard.getKeysym(keycode, 0);
            int shifted = client.xServer.keyboard.getKeysym(keycode, 1);
            outputStream.writeByte((byte)1);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)1);
            outputStream.writeByte((byte)Keyboard.KEYSYMS_PER_KEYCODE);
            outputStream.writeShort((short)Keyboard.KEYSYMS_PER_KEYCODE);
            outputStream.writeInt(base);
            outputStream.writeInt(shifted);
        }
    }

    private int keyTypesBytes(int firstType, int count) {
        int bytes = 0;
        for (int type = firstType; type < firstType + count; type++) bytes += type == 0 ? 8 : 16;
        return bytes;
    }

    private List<int[]> modifierMappings(int firstKeycode, int count) {
        List<int[]> result = new ArrayList<>();
        int end = firstKeycode + count;
        for (int keycode = firstKeycode; keycode < end; keycode++) {
            int mask = modifierMaskForKeycode(keycode);
            if (mask != 0) result.add(new int[]{keycode, mask});
        }
        return result;
    }

    private int modifierMaskForKeycode(int keycode) {
        if (keycode == Byte.toUnsignedInt(XKeycode.KEY_SHIFT_L.id) || keycode == Byte.toUnsignedInt(XKeycode.KEY_SHIFT_R.id)) return 1;
        if (keycode == Byte.toUnsignedInt(XKeycode.KEY_CAPS_LOCK.id)) return 2;
        if (keycode == Byte.toUnsignedInt(XKeycode.KEY_CTRL_L.id) || keycode == Byte.toUnsignedInt(XKeycode.KEY_CTRL_R.id)) return 4;
        if (keycode == Byte.toUnsignedInt(XKeycode.KEY_ALT_L.id) || keycode == Byte.toUnsignedInt(XKeycode.KEY_ALT_R.id)) return 8;
        if (keycode == Byte.toUnsignedInt(XKeycode.KEY_NUM_LOCK.id)) return 16;
        return 0;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int pad4(int bytes) {
        return (-bytes) & 3;
    }

    private void logMapReply(XClient client, int deviceSpec, int full, int partial, int present, int firstKeySym, int nKeySyms, int modifierMapEntries, int variableBytes) {
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XSERVER_XKEYBOARD_GET_MAP_REPLY",
                null,
                "xserver_extensions",
                "xkeyboard_get_map_reply",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "resource_id_base", client.resourceIDBase,
                        "sequence_number", Short.toUnsignedInt(client.getSequenceNumber()),
                        "device_spec", deviceSpec,
                        "requested_full", full,
                        "requested_partial", partial,
                        "present", present,
                        "first_key_sym", firstKeySym,
                        "n_key_syms", nKeySyms,
                        "modifier_map_entries", modifierMapEntries,
                        "variable_bytes", variableBytes
                )
        );
    }

    private static void logRequest(XClient client, int minorOpcode) {
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XSERVER_XKEYBOARD_REQUEST",
                null,
                "xserver_extensions",
                "xkeyboard_request",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "resource_id_base", client.resourceIDBase,
                        "sequence_number", Short.toUnsignedInt(client.getSequenceNumber()),
                        "minor_opcode", minorOpcode,
                        "minor_opcode_name", describeMinorOpcode(minorOpcode),
                        "request_length", client.getRequestLength(),
                        "remaining_request_length", client.getRemainingRequestLength()
                )
        );
    }

    private static void logReply(XClient client, String eventId, String message, Object... extraFields) {
        Object[] baseFields = new Object[12 + extraFields.length];
        baseFields[0] = "client_fd";
        baseFields[1] = client.fd;
        baseFields[2] = "resource_id_base";
        baseFields[3] = client.resourceIDBase;
        baseFields[4] = "sequence_number";
        baseFields[5] = Short.toUnsignedInt(client.getSequenceNumber());
        baseFields[6] = "minor_opcode";
        baseFields[7] = Byte.toUnsignedInt(client.getRequestData());
        baseFields[8] = "minor_opcode_name";
        baseFields[9] = describeMinorOpcode(Byte.toUnsignedInt(client.getRequestData()));
        baseFields[10] = "request_length";
        baseFields[11] = client.getRequestLength();
        System.arraycopy(extraFields, 0, baseFields, 12, extraFields.length);
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                eventId,
                null,
                "xserver_extensions",
                message,
                ForensicLogger.fields(baseFields)
        );
    }

    private static void logSkipped(XClient client, String reason) {
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XSERVER_XKEYBOARD_REQUEST_SKIPPED",
                null,
                "xserver_extensions",
                "xkeyboard_request_skipped",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "resource_id_base", client.resourceIDBase,
                        "sequence_number", Short.toUnsignedInt(client.getSequenceNumber()),
                        "minor_opcode", Byte.toUnsignedInt(client.getRequestData()),
                        "minor_opcode_name", describeMinorOpcode(Byte.toUnsignedInt(client.getRequestData())),
                        "request_length", client.getRequestLength(),
                        "remaining_request_length", client.getRemainingRequestLength(),
                        "reason", reason
                )
        );
    }

    static String describeMinorOpcode(int opcode) {
        switch (opcode) {
            case ClientOpcodes.USE_EXTENSION:
                return "UseExtension";
            case ClientOpcodes.SELECT_EVENTS:
                return "SelectEvents";
            case ClientOpcodes.BELL:
                return "Bell";
            case ClientOpcodes.GET_STATE:
                return "GetState";
            case ClientOpcodes.LATCH_LOCK_STATE:
                return "LatchLockState";
            case ClientOpcodes.GET_CONTROLS:
                return "GetControls";
            case ClientOpcodes.SET_CONTROLS:
                return "SetControls";
            case ClientOpcodes.GET_MAP:
                return "GetMap";
            case ClientOpcodes.SET_MAP:
                return "SetMap";
            case ClientOpcodes.GET_COMPAT_MAP:
                return "GetCompatMap";
            case ClientOpcodes.SET_COMPAT_MAP:
                return "SetCompatMap";
            case ClientOpcodes.GET_INDICATOR_STATE:
                return "GetIndicatorState";
            case ClientOpcodes.GET_INDICATOR_MAP:
                return "GetIndicatorMap";
            case ClientOpcodes.SET_INDICATOR_MAP:
                return "SetIndicatorMap";
            case ClientOpcodes.GET_NAMED_INDICATOR:
                return "GetNamedIndicator";
            case ClientOpcodes.SET_NAMED_INDICATOR:
                return "SetNamedIndicator";
            case ClientOpcodes.GET_NAMES:
                return "GetNames";
            case ClientOpcodes.SET_NAMES:
                return "SetNames";
            case ClientOpcodes.GET_GEOMETRY:
                return "GetGeometry";
            case ClientOpcodes.SET_GEOMETRY:
                return "SetGeometry";
            case ClientOpcodes.PER_CLIENT_FLAGS:
                return "PerClientFlags";
            case ClientOpcodes.LIST_COMPONENTS:
                return "ListComponents";
            case ClientOpcodes.GET_KBD_BY_NAME:
                return "GetKbdByName";
            case ClientOpcodes.GET_DEVICE_INFO:
                return "GetDeviceInfo";
            case ClientOpcodes.SET_DEVICE_INFO:
                return "SetDeviceInfo";
            case ClientOpcodes.SET_DEBUGGING_FLAGS:
                return "SetDebuggingFlags";
            default:
                return "Unknown";
        }
    }

    static boolean isClientXkbExtensionSupportedForTests() {
        return CLIENT_XKB_EXTENSION_SUPPORTED;
    }

    static String clientXkbDisableReasonForTests() {
        return CLIENT_XKB_DISABLE_REASON;
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int minorOpcode = Byte.toUnsignedInt(client.getRequestData());
        logRequest(client, minorOpcode);
        switch (client.getRequestData()) {
            case ClientOpcodes.USE_EXTENSION:
                useExtension(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_STATE:
                getState(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_CONTROLS:
                getControls(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_MAP:
                getMap(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_COMPAT_MAP:
                getCompatMap(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_INDICATOR_STATE:
                getIndicatorState(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_INDICATOR_MAP:
                getIndicatorMap(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_NAMED_INDICATOR:
                getNamedIndicator(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_NAMES:
                getNames(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_GEOMETRY:
                getGeometry(client, inputStream, outputStream);
                break;
            case ClientOpcodes.PER_CLIENT_FLAGS:
                perClientFlags(client, inputStream, outputStream);
                break;
            case ClientOpcodes.LIST_COMPONENTS:
                listComponents(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_KBD_BY_NAME:
                getKbdByName(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_DEVICE_INFO:
                getDeviceInfo(client, inputStream, outputStream);
                break;
            case ClientOpcodes.SET_DEBUGGING_FLAGS:
                setDebuggingFlags(client, inputStream, outputStream);
                break;
            case ClientOpcodes.SELECT_EVENTS:
            case ClientOpcodes.BELL:
            case ClientOpcodes.LATCH_LOCK_STATE:
            case ClientOpcodes.SET_CONTROLS:
            case ClientOpcodes.SET_MAP:
            case ClientOpcodes.SET_COMPAT_MAP:
            case ClientOpcodes.SET_INDICATOR_MAP:
            case ClientOpcodes.SET_NAMED_INDICATOR:
            case ClientOpcodes.SET_NAMES:
            case ClientOpcodes.SET_GEOMETRY:
            case ClientOpcodes.SET_DEVICE_INFO:
                logSkipped(client, "void_or_mutating_xkb_request_accepted_without_state_change");
                client.skipRequest();
                break;
            default:
                logSkipped(client, "unsupported_xkb_minor_opcode");
                client.skipRequest();
                break;
        }
    }
}
