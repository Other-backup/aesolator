package com.winlator.cmod.xserver.extensions;

import static com.winlator.cmod.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.XClient;
import com.winlator.cmod.xserver.XServer;
import com.winlator.cmod.xserver.errors.BadImplementation;
import com.winlator.cmod.xserver.errors.BadWindow;
import com.winlator.cmod.xserver.errors.XRequestError;

import java.io.IOException;

public class ShapeExtension implements Extension {
    public static final byte MAJOR_OPCODE = -108;
    private static final byte MAJOR_VERSION = 1;
    private static final byte MINOR_VERSION = 1;
    private final XServer xServer;

    private static abstract class ClientOpcodes {
        private static final byte QUERY_VERSION = 0;
        private static final byte RECTANGLES = 1;
        private static final byte MASK = 2;
        private static final byte COMBINE = 3;
        private static final byte OFFSET = 4;
        private static final byte QUERY_EXTENTS = 5;
        private static final byte SELECT_INPUT = 6;
        private static final byte INPUT_SELECTED = 7;
        private static final byte GET_RECTANGLES = 8;
    }

    public ShapeExtension(XServer xServer) {
        this.xServer = xServer;
    }

    @Override
    public String getName() {
        return "SHAPE";
    }

    @Override
    public byte getMajorOpcode() {
        return MAJOR_OPCODE;
    }

    @Override
    public byte getFirstErrorId() {
        return 0;
    }

    @Override
    public byte getFirstEventId() {
        return 64;
    }

    @Override
    public int getNumEvents() {
        return 1;
    }

    private static void queryVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort(MAJOR_VERSION);
            outputStream.writeShort(MINOR_VERSION);
            outputStream.writePad(20);
        }
    }

    private void queryExtents(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        Window window = xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writePad(2);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort(window.getWidth());
            outputStream.writeShort(window.getHeight());
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort(window.getWidth());
            outputStream.writeShort(window.getHeight());
            outputStream.writePad(4);
        }
    }

    private void inputSelected(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        Window window = xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeByte((byte)0);
            outputStream.writePad(23);
        }
    }

    private void getRectangles(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        inputStream.skip(4);
        Window window = xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeByte((byte)0);
            outputStream.writePad(3);
            outputStream.writeInt(0);
            outputStream.writePad(16);
        }
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        switch (client.getRequestData()) {
            case ClientOpcodes.QUERY_VERSION:
                queryVersion(client, inputStream, outputStream);
                break;
            case ClientOpcodes.RECTANGLES:
            case ClientOpcodes.MASK:
            case ClientOpcodes.COMBINE:
            case ClientOpcodes.OFFSET:
            case ClientOpcodes.SELECT_INPUT:
                client.skipRequest();
                break;
            case ClientOpcodes.QUERY_EXTENTS:
                queryExtents(client, inputStream, outputStream);
                break;
            case ClientOpcodes.INPUT_SELECTED:
                inputSelected(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_RECTANGLES:
                getRectangles(client, inputStream, outputStream);
                break;
            default:
                throw new BadImplementation();
        }
    }
}
