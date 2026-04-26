package com.winlator.cmod.xserver.extensions;

import static com.winlator.cmod.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import android.util.Log;

import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.Bitmask;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.XClient;
import com.winlator.cmod.xserver.XLock;
import com.winlator.cmod.xserver.XServer;
import com.winlator.cmod.xserver.errors.BadValue;
import com.winlator.cmod.xserver.errors.BadWindow;
import com.winlator.cmod.xserver.errors.XRequestError;
import com.winlator.cmod.xserver.events.XIRawButtonPressNotify;
import com.winlator.cmod.xserver.events.XIRawButtonReleaseNotify;
import com.winlator.cmod.xserver.events.XIRawMotionNotify;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class XInput2Extension implements Extension {
    private static final String TAG = "XInput2Extension";
    public static final byte MAJOR_OPCODE = -105;

    private static final int XI_MAJOR = 2;
    private static final int XI_MINOR = 2;
    private static final int XI_ALL_DEVICES = 0;
    private static final int XI_ALL_MASTER_DEVICES = 1;
    private static final int MASTER_POINTER_ID = 2;
    private static final int MASTER_KEYBOARD_ID = 3;
    private static final int XI_BUTTON_CLASS = 1;
    private static final int XI_VALUATOR_CLASS = 2;
    private static final long XI_RAW_BUTTON_PRESS_MASK = 1L << 15;
    private static final long XI_RAW_BUTTON_RELEASE_MASK = 1L << 16;
    private static final long XI_RAW_MOTION_MASK = 1L << 17;
    private static final int RAW_MOTION_XY_MASK = (1 << 0) | (1 << 1);
    private static final int POINTER_BUTTON_COUNT = 7;

    private byte firstEventId = 0;
    private byte firstErrorId = 0;
    private final List<Selection> selections = new CopyOnWriteArrayList<>();

    private static abstract class ClientOpcodes {
        private static final byte GET_EXTENSION_VERSION = 1;
        private static final byte GET_CLIENT_POINTER = 45;
        private static final byte SELECT_EVENTS = 46;
        private static final byte QUERY_VERSION = 47;
        private static final byte QUERY_DEVICE = 48;
    }

    private static class Selection {
        Window window;
        XClient client;
        int windowId;
        Bitmask mask;
        int deviceId;
    }

    @Override
    public String getName() {
        return "XInputExtension";
    }

    @Override
    public byte getMajorOpcode() {
        return MAJOR_OPCODE;
    }

    @Override
    public int getNumEvents() {
        return 24;
    }

    @Override
    public int getNumErrors() {
        return 5;
    }

    @Override
    public void setFirstEventId(byte firstEventId) {
        this.firstEventId = firstEventId;
    }

    @Override
    public void setFirstErrorId(byte firstErrorId) {
        this.firstErrorId = firstErrorId;
    }

    @Override
    public byte getFirstEventId() {
        return firstEventId;
    }

    @Override
    public byte getFirstErrorId() {
        return firstErrorId;
    }

    private boolean matchesSelection(Selection selection, int deviceId) {
        return selection.deviceId == XI_ALL_DEVICES
                || (selection.deviceId == XI_ALL_MASTER_DEVICES && isMasterDevice(deviceId))
                || selection.deviceId == deviceId;
    }

    private boolean isMasterDevice(int deviceId) {
        return deviceId == MASTER_POINTER_ID || deviceId == MASTER_KEYBOARD_ID;
    }

    private static void getExtensionVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort((short)XI_MAJOR);
            outputStream.writeShort((short)0);
            outputStream.writeByte((byte)1);
            outputStream.writePad(19);
        }
    }

    private static void getClientPointer(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeByte((byte)1);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)MASTER_POINTER_ID);
            outputStream.writePad(20);
        }
    }

    private static void queryVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        short clientMajor = (short)(inputStream.readShort() & 0xffff);
        short clientMinor = (short)(inputStream.readShort() & 0xffff);
        inputStream.skip(client.getRemainingRequestLength());

        short negotiatedMajor = clientMajor;
        short negotiatedMinor = clientMinor;
        if (clientMajor > XI_MAJOR || (clientMajor == XI_MAJOR && clientMinor > XI_MINOR)) {
            negotiatedMajor = XI_MAJOR;
            negotiatedMinor = XI_MINOR;
        }

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort(negotiatedMajor);
            outputStream.writeShort(negotiatedMinor);
            outputStream.writePad(20);
        }
    }

    private void writeButtonClass(XOutputStream outputStream, int sourceId, int numButtons) throws IOException {
        int stateBytes = Math.max(4, ((numButtons + 31) / 32) * 4);
        int labelsBytes = numButtons * 4;
        int totalBytes = 8 + stateBytes + labelsBytes;

        outputStream.writeShort((short)XI_BUTTON_CLASS);
        outputStream.writeShort((short)(totalBytes / 4));
        outputStream.writeShort((short)sourceId);
        outputStream.writeShort((short)numButtons);
        outputStream.writeInt(0);
        if (stateBytes > 4) outputStream.writePad(stateBytes - 4);
        for (int i = 0; i < numButtons; i++) outputStream.writeInt(0);
    }

    private void writeValuatorClass(XOutputStream outputStream, int axisNumber) throws IOException {
        outputStream.writeShort((short)XI_VALUATOR_CLASS);
        outputStream.writeShort((short)11);
        outputStream.writeShort((short)MASTER_POINTER_ID);
        outputStream.writeShort((short)axisNumber);
        outputStream.writeInt(0);
        outputStream.writeFP3232(0);
        outputStream.writeFP3232(0);
        outputStream.writeFP3232(0);
        outputStream.writeInt(0);
        outputStream.writeByte((byte)0);
        outputStream.writePad(3);
    }

    private void queryDevice(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());

        byte[] nameBytes = "Virtual Core Pointer".getBytes();
        int nameLen = nameBytes.length;
        int namePad = (nameLen + 3) & ~3;
        int buttonStateBytes = Math.max(4, ((POINTER_BUTTON_COUNT + 31) / 32) * 4);
        int buttonClassBytes = 8 + buttonStateBytes + POINTER_BUTTON_COUNT * 4;
        int numValuators = 2;
        int numClasses = 1 + numValuators;
        int deviceInfoSize = 12 + namePad + buttonClassBytes + 44 * numValuators;

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(deviceInfoSize / 4);
            outputStream.writeShort((short)1);
            outputStream.writePad(22);

            outputStream.writeShort((short)MASTER_POINTER_ID);
            outputStream.writeShort((short)1);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)numClasses);
            outputStream.writeShort((short)nameLen);
            outputStream.writeByte((byte)1);
            outputStream.writeByte((byte)0);
            outputStream.write(nameBytes);
            outputStream.writePad(namePad - nameLen);

            writeButtonClass(outputStream, MASTER_POINTER_ID, POINTER_BUTTON_COUNT);
            writeValuatorClass(outputStream, 0);
            writeValuatorClass(outputStream, 1);
        }
    }

    private void selectEvents(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        int numMasks = inputStream.readShort() & 0xffff;
        if (numMasks == 0) {
            inputStream.skip(client.getRemainingRequestLength());
            throw new BadValue(numMasks);
        }
        inputStream.readShort();

        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) {
            inputStream.skip(client.getRemainingRequestLength());
            throw new BadWindow(windowId);
        }

        for (int i = 0; i < numMasks; i++) {
            int deviceId = inputStream.readShort() & 0xffff;
            int maskLen = inputStream.readShort() & 0xffff;
            Bitmask mask = new Bitmask(0);
            for (int word = 0; word < maskLen; word++) {
                long value = inputStream.readUnsignedInt();
                mask.set(value << (word * 32));
            }

            Selection selection = new Selection();
            selection.client = client;
            selection.window = window;
            selection.windowId = windowId;
            selection.deviceId = deviceId;
            selection.mask = mask;

            selections.removeIf(old -> old.client == client
                    && old.windowId == windowId
                    && old.deviceId == deviceId);
            selections.add(selection);
        }

        inputStream.skip(client.getRemainingRequestLength());
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int opcode = client.getRequestData();
        switch (opcode) {
            case ClientOpcodes.GET_EXTENSION_VERSION:
                getExtensionVersion(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_CLIENT_POINTER:
                getClientPointer(client, inputStream, outputStream);
                break;
            case ClientOpcodes.SELECT_EVENTS:
                try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                    selectEvents(client, inputStream, outputStream);
                }
                break;
            case ClientOpcodes.QUERY_VERSION:
                queryVersion(client, inputStream, outputStream);
                break;
            case ClientOpcodes.QUERY_DEVICE:
                queryDevice(client, inputStream, outputStream);
                break;
            default:
                Log.w(TAG, "Unhandled minor opcode=" + opcode + " length=" + client.getRemainingRequestLength());
                inputStream.skip(client.getRemainingRequestLength());
                break;
        }
    }

    public void onClientDisconnected(XClient client) {
        selections.removeIf(selection -> selection.client == client);
    }

    public void emitRawMotion(int deviceId, double deltaX, double deltaY) {
        for (Selection selection : selections) {
            if (!matchesSelection(selection, deviceId) || !selection.mask.isSet(XI_RAW_MOTION_MASK)) continue;
            try {
                selection.client.sendEvent(new XIRawMotionNotify(deviceId, MAJOR_OPCODE, new double[] {deltaX, deltaY}, RAW_MOTION_XY_MASK));
            }
            catch (RuntimeException ignored) {
            }
        }
    }

    public void emitRawButton(int deviceId, int buttonNumber, boolean pressed) {
        long mask = pressed ? XI_RAW_BUTTON_PRESS_MASK : XI_RAW_BUTTON_RELEASE_MASK;
        for (Selection selection : selections) {
            if (!matchesSelection(selection, deviceId) || !selection.mask.isSet(mask)) continue;
            try {
                if (pressed) {
                    selection.client.sendEvent(new XIRawButtonPressNotify(deviceId, MAJOR_OPCODE, buttonNumber));
                }
                else {
                    selection.client.sendEvent(new XIRawButtonReleaseNotify(deviceId, MAJOR_OPCODE, buttonNumber));
                }
            }
            catch (RuntimeException ignored) {
            }
        }
    }
}
