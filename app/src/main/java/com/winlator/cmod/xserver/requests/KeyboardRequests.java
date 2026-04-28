package com.winlator.cmod.xserver.requests;

import static com.winlator.cmod.xserver.Keyboard.KEYSYMS_PER_KEYCODE;
import static com.winlator.cmod.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.Keyboard;
import com.winlator.cmod.xserver.XClient;
import com.winlator.cmod.xserver.errors.BadValue;
import com.winlator.cmod.xserver.errors.XRequestError;

import java.io.IOException;

public abstract class KeyboardRequests {
    public static void getKeyboardMapping(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int firstKeycode = inputStream.readUnsignedByte();
        int count = inputStream.readUnsignedByte();
        inputStream.skip(2);
        int lastKeycode = firstKeycode + count - 1;
        if (count < 1 || firstKeycode < Keyboard.MIN_KEYCODE || lastKeycode > Keyboard.MAX_KEYCODE) throw new BadValue(firstKeycode);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte(KEYSYMS_PER_KEYCODE);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(count * KEYSYMS_PER_KEYCODE);
            outputStream.writePad(24);

            for (int keycode = firstKeycode; keycode <= lastKeycode; keycode++) {
                for (int level = 0; level < KEYSYMS_PER_KEYCODE; level++) {
                    outputStream.writeInt(client.xServer.keyboard.getKeysym(keycode, level));
                }
            }
        }
    }

    public static void getModifierMapping(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)1);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(2);
            outputStream.writePad(24);
            outputStream.writePad(8);
        }
    }
}
