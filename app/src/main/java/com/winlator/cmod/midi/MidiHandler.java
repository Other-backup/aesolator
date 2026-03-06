package com.winlator.cmod.midi;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.sherlock.com.sun.media.sound.SF2Soundbank;
import cn.sherlock.com.sun.media.sound.SoftSynthesizer;
import jp.kshoji.javax.sound.midi.Receiver;
import jp.kshoji.javax.sound.midi.ShortMessage;
import jp.kshoji.javax.sound.midi.SysexMessage;

public class MidiHandler {
    private static final String TAG = "MidiHandler";
    private static final int RECEIVE_BUFFER_SIZE = 4096;
    private static final int MIDI_CHANNEL_COUNT = 16;
    private static final int MIDI_NOTE_COUNT = 128;
    private static final int CONTROL_SUSTAIN_PEDAL = 64;
    private static final int CONTROL_ALL_SOUND_OFF = 120;
    private static final int CONTROL_RESET_ALL_CONTROLLERS = 121;
    private static final int CONTROL_ALL_NOTES_OFF = 123;
    private DatagramSocket socket;
    private boolean running = false;
    private static final short SERVER_PORT = 7942;
    private final ByteBuffer receiveData = ByteBuffer.allocate(RECEIVE_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    private final DatagramPacket receivePacket = new DatagramPacket(receiveData.array(), RECEIVE_BUFFER_SIZE);
    private SoftSynthesizer synth;
    private Receiver recv;
    private SF2Soundbank sf2SoundBank;
    private long lastMidiMsgTime = 0;
    private ShortMessage message = new ShortMessage();
    private ScheduledExecutorService scheduler;
    private static final long CHECK_DELAY = 200;

    public void setSoundBank(SF2Soundbank soundBank) {
        clearRecv();
        clearSynth();
        this.sf2SoundBank = soundBank;
    }

    public void start() {
        running = true;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress((InetAddress) null, SERVER_PORT));

                while (running) {
                    receivePacket.setLength(receiveData.capacity());
                    socket.receive(receivePacket);
                    receiveData.rewind();
                    handleRequest(receiveData, receivePacket.getLength());
                }
            } catch (IOException e) {
                if (running) Log.w(TAG, "MIDI socket loop stopped", e);
            }
        });
    }

    public void stop() {
        running = false;

        if (socket != null) {
            socket.close();
            socket = null;
        }

        clearRecv();
        clearSynth();
        stopMidiDataChecking();
    }

    private void handleRequest(ByteBuffer received, int packetLength) {
        if (packetLength <= 0) return;
        byte requestCode = received.get();
        switch (requestCode) {
            case RequestCodes.MIDI_SHORT:
                if (packetLength < 4) {
                    Log.w(TAG, "Ignoring truncated short MIDI packet: " + packetLength);
                    break;
                }
                if (recv != null) {
                    try {
                        lastMidiMsgTime = System.currentTimeMillis();
                        message.setMessage(received.get() & 0xff, received.get() & 0xff, received.get() & 0xff);
                        recv.send(message, -1);
                    } catch (Exception e) {
                        Log.w(TAG, "Unable to process short MIDI message", e);
                    }
                }
                break;
            case RequestCodes.MIDI_LONG:
                handleLongMessage(received, packetLength - 1);
                break;
            case RequestCodes.MIDI_PREPARE:
                // stub
                break;
            case RequestCodes.MIDI_UNPREPARE:
                // stub
                break;
            case RequestCodes.MIDI_OPEN:
                if (synth == null || recv == null) {
                    clearRecv();
                    clearSynth();
                    prepareSynthAndRecv();
                    startMidiDataChecking();
                }
                break;
            case RequestCodes.MIDI_CLOSE:
                clearRecv();
                clearSynth();
                stopMidiDataChecking();
                break;
            case RequestCodes.MIDI_RESET:
                resetMidiState();
                break;
        }
    }

    private void handleLongMessage(ByteBuffer received, int payloadLength) {
        if (recv == null || payloadLength <= 0) return;

        try {
            byte[] payload = readLongPayload(received, payloadLength);
            if (payload.length == 0) return;

            SysexMessage sysexMessage = new SysexMessage();
            sysexMessage.setMessage(payload, payload.length);
            lastMidiMsgTime = System.currentTimeMillis();
            recv.send(sysexMessage, -1);
        } catch (Exception e) {
            Log.w(TAG, "Unable to process long MIDI message", e);
        }
    }

    private byte[] readLongPayload(ByteBuffer received, int payloadLength) {
        if (payloadLength <= 0) return new byte[0];

        if (payloadLength >= Integer.BYTES) {
            received.mark();
            int declaredLength = received.getInt();
            int remaining = payloadLength - Integer.BYTES;
            if (declaredLength > 0 && declaredLength <= remaining) {
                byte[] payload = new byte[declaredLength];
                received.get(payload, 0, declaredLength);
                return payload;
            }
            received.reset();
        }

        byte[] payload = new byte[payloadLength];
        received.get(payload, 0, payloadLength);
        return payload;
    }

    private void clearRecv() {
        if (recv != null) {
            recv.close();
            recv = null;
        }
    }

    private void clearSynth() {
        if (synth != null) {
            synth.close();
            synth = null;
        }
    }

    private void prepareSynthAndRecv() {
        try {
            synth = new SoftSynthesizer();
            synth.open();
            synth.loadAllInstruments(sf2SoundBank);
            recv = synth.getReceiver();
        } catch (Exception e) {
            Log.w(TAG, "Unable to initialize MIDI synthesizer", e);
            clearRecv();
            clearSynth();
        }
    }

    private void sendAllOff() {
        if (recv == null) return;
        try {
            for (int channel = 0; channel < MIDI_CHANNEL_COUNT; channel++) {
                sendControlChange(channel, CONTROL_SUSTAIN_PEDAL, 0);
                sendControlChange(channel, CONTROL_ALL_SOUND_OFF, 0);
                sendControlChange(channel, CONTROL_RESET_ALL_CONTROLLERS, 0);
                sendControlChange(channel, CONTROL_ALL_NOTES_OFF, 0);
            }

            ShortMessage noteOff = new ShortMessage();
            for (int note = 0; note < MIDI_NOTE_COUNT; note++) {
                for (int channel = 0; channel < MIDI_CHANNEL_COUNT; channel++) {
                    noteOff.setMessage(ShortMessage.NOTE_OFF, channel, note, 0);
                    recv.send(noteOff, -1);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to flush pending MIDI notes", e);
        }
    }

    private void sendControlChange(int channel, int controller, int value) throws Exception {
        ShortMessage controlChange = new ShortMessage();
        controlChange.setMessage(ShortMessage.CONTROL_CHANGE, channel, controller, value);
        recv.send(controlChange, -1);
    }

    private void resetMidiState() {
        sendAllOff();
        lastMidiMsgTime = 0;
    }

    private void stopMidiDataChecking() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public void startMidiDataChecking() {
        stopMidiDataChecking();
        scheduler = Executors.newScheduledThreadPool(1);
        Runnable checkTask = () -> {
            long currentTime = System.currentTimeMillis();
            if (lastMidiMsgTime != 0 && currentTime - lastMidiMsgTime > (CHECK_DELAY /2)) {
                sendAllOff();
                lastMidiMsgTime = 0;
            }
        };
        scheduler.scheduleWithFixedDelay(checkTask, 0, CHECK_DELAY, TimeUnit.MILLISECONDS);
    }
}
