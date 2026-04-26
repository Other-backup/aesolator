package com.winlator.cmod.inputcontrols;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class FakeInputWriter {
    public static final short ABS_BRAKE = 10;
    public static final short ABS_GAS = 9;
    public static final short ABS_HAT0X = 16;
    public static final short ABS_HAT0Y = 17;
    public static final short ABS_RX = 3;
    public static final short ABS_RY = 4;
    public static final short ABS_X = 0;
    public static final short ABS_Y = 1;
    public static final short EV_ABS = 3;
    public static final short EV_KEY = 1;
    public static final short EV_MSC = 4;
    public static final short EV_SYN = 0;
    public static final short MSC_SCAN = 4;
    public static final short SYN_REPORT = 0;
    public static final short BTN_A = 304;
    public static final short BTN_B = 305;
    public static final short BTN_X = 307;
    public static final short BTN_Y = 308;
    public static final short BTN_TL = 310;
    public static final short BTN_TR = 311;
    public static final short BTN_SELECT = 314;
    public static final short BTN_START = 315;
    public static final short BTN_THUMBL = 317;
    public static final short BTN_THUMBR = 318;

    private static final String TAG = "FakeInputWriter";
    private static final int BUFFER_SIZE = 768;
    private static final short[] BUTTON_MAP = {
            BTN_A, BTN_B, BTN_X, BTN_Y, BTN_TL,
            BTN_TR, BTN_SELECT, BTN_START, BTN_THUMBL, BTN_THUMBR
    };

    private final File eventFile;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private final boolean[] prevButtonStates = new boolean[12];

    private RandomAccessFile raf;
    private FileChannel channel;
    private boolean isOpen = false;
    private volatile boolean destroyed = false;
    private boolean hasChanges = false;
    private int prevHatX;
    private int prevHatY;
    private int prevThumbLX;
    private int prevThumbLY;
    private int prevThumbRX;
    private int prevThumbRY;
    private int prevTriggerL;
    private int prevTriggerR;

    public FakeInputWriter(String fakeInputPath, int slot) {
        this.eventFile = new File(fakeInputPath, "event" + slot);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public synchronized boolean open() {
        if (destroyed) return false;
        if (isOpen) return true;

        try {
            File parent = eventFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!eventFile.exists()) {
                eventFile.createNewFile();
            }
            raf = new RandomAccessFile(eventFile, "rw");
            raf.seek(raf.length());
            channel = raf.getChannel();
            isOpen = true;
            Log.i(TAG, "Opened fake input: " + eventFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to open fake input: " + e.getMessage());
            return false;
        }
    }

    public synchronized void close() {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
            channel = null;
        }
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException ignored) {
            }
            raf = null;
        }
        isOpen = false;
    }

    public synchronized void reset() {
        if (!isOpen && !open()) return;

        buffer.clear();
        hasChanges = false;
        for (int i = 0; i < BUTTON_MAP.length; i++) {
            if (prevButtonStates[i]) {
                prevButtonStates[i] = false;
                writeEvent(EV_MSC, MSC_SCAN, BUTTON_MAP[i]);
                writeEvent(EV_KEY, BUTTON_MAP[i], 0);
            }
        }
        if (prevThumbLX != 0) {
            prevThumbLX = 0;
            writeEvent(EV_ABS, ABS_X, 0);
        }
        if (prevThumbLY != 0) {
            prevThumbLY = 0;
            writeEvent(EV_ABS, ABS_Y, 0);
        }
        if (prevThumbRX != 0) {
            prevThumbRX = 0;
            writeEvent(EV_ABS, ABS_RX, 0);
        }
        if (prevThumbRY != 0) {
            prevThumbRY = 0;
            writeEvent(EV_ABS, ABS_RY, 0);
        }
        if (prevTriggerL != 0) {
            prevTriggerL = 0;
            writeEvent(EV_ABS, ABS_BRAKE, 0);
        }
        if (prevTriggerR != 0) {
            prevTriggerR = 0;
            writeEvent(EV_ABS, ABS_GAS, 0);
        }
        if (prevHatX != 0) {
            prevHatX = 0;
            writeEvent(EV_ABS, ABS_HAT0X, 0);
        }
        if (prevHatY != 0) {
            prevHatY = 0;
            writeEvent(EV_ABS, ABS_HAT0Y, 0);
        }

        if (!hasChanges) {
            Log.i(TAG, "Reset fake input to neutral state: " + eventFile.getAbsolutePath());
            return;
        }

        writeEvent(EV_SYN, SYN_REPORT, 0);
        buffer.flip();
        try {
            channel.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Reset write error: " + e.getMessage());
        }
        Log.i(TAG, "Reset fake input to neutral state: " + eventFile.getAbsolutePath());
    }

    public synchronized void destroy() {
        destroyed = true;
        reset();
        close();
        if (eventFile.exists()) {
            boolean deleted = eventFile.delete();
            Log.i(TAG, "Deleted fake input: " + eventFile.getAbsolutePath() + " (" + deleted + ")");
        }
    }

    public synchronized void softRelease() {
        reset();
        close();
        Log.i(TAG, "Soft released fake input: " + eventFile.getAbsolutePath());
    }

    public synchronized void writeGamepadState(GamepadState state) throws IOException {
        if (state == null) return;
        if (!isOpen && !open()) return;

        buffer.clear();
        hasChanges = false;

        for (int i = 0; i < 10; i++) {
            writeButton(i, state.isPressed((byte) i));
        }

        int lx = (int) (state.thumbLX * 32767.0f);
        int ly = (int) (state.thumbLY * 32767.0f);
        int rx = (int) (state.thumbRX * 32767.0f);
        int ry = (int) (state.thumbRY * 32767.0f);
        int tl = (int) (state.triggerL * 255.0f);
        int tr = (int) (state.triggerR * 255.0f);

        if (lx != prevThumbLX) {
            prevThumbLX = lx;
            writeEvent(EV_ABS, ABS_X, lx);
        }
        if (ly != prevThumbLY) {
            prevThumbLY = ly;
            writeEvent(EV_ABS, ABS_Y, ly);
        }
        if (rx != prevThumbRX) {
            prevThumbRX = rx;
            writeEvent(EV_ABS, ABS_RX, rx);
        }
        if (ry != prevThumbRY) {
            prevThumbRY = ry;
            writeEvent(EV_ABS, ABS_RY, ry);
        }
        if (tl != prevTriggerL) {
            prevTriggerL = tl;
            writeEvent(EV_ABS, ABS_BRAKE, tl);
        }
        if (tr != prevTriggerR) {
            prevTriggerR = tr;
            writeEvent(EV_ABS, ABS_GAS, tr);
        }

        int hatX = state.dpad[3] ? -1 : (state.dpad[1] ? 1 : 0);
        int hatY = state.dpad[0] ? -1 : (state.dpad[2] ? 1 : 0);
        if (hatX != prevHatX) {
            prevHatX = hatX;
            writeEvent(EV_ABS, ABS_HAT0X, hatX);
        }
        if (hatY != prevHatY) {
            prevHatY = hatY;
            writeEvent(EV_ABS, ABS_HAT0Y, hatY);
        }

        if (!hasChanges) return;

        writeEvent(EV_SYN, SYN_REPORT, 0);
        buffer.flip();
        channel.write(buffer);
    }

    private void writeEvent(short type, short code, int value) {
        long timeMs = System.currentTimeMillis();
        buffer.putLong(timeMs / 1000);
        buffer.putLong((timeMs % 1000) * 1000);
        buffer.putShort(type);
        buffer.putShort(code);
        buffer.putInt(value);
        hasChanges = true;
    }

    private void writeButton(int index, boolean pressed) {
        if (index < 0 || index >= BUTTON_MAP.length || prevButtonStates[index] == pressed) return;
        prevButtonStates[index] = pressed;
        writeEvent(EV_MSC, MSC_SCAN, BUTTON_MAP[index]);
        writeEvent(EV_KEY, BUTTON_MAP[index], pressed ? 1 : 0);
    }
}
