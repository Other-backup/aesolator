package com.winlator.cmod.xserver.extensions;

import static com.winlator.cmod.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.XClient;
import com.winlator.cmod.xserver.XServer;
import com.winlator.cmod.xserver.errors.XRequestError;

import java.io.IOException;

public class XineramaExtension implements Extension {
    public static final byte MAJOR_OPCODE = -110;
    private static final short SERVER_MAJOR_VERSION = 1;
    private static final short SERVER_MINOR_VERSION = 1;
    private final XServer xServer;

    private static abstract class ClientOpcodes {
        private static final byte QUERY_VERSION = 0;
        private static final byte GET_STATE = 1;
        private static final byte GET_SCREEN_COUNT = 2;
        private static final byte GET_SCREEN_SIZE = 3;
        private static final byte IS_ACTIVE = 4;
        private static final byte QUERY_SCREENS = 5;
    }

    public XineramaExtension(XServer xServer) {
        this.xServer = xServer;
    }

    @Override
    public String getName() {
        return "XINERAMA";
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
        return 0;
    }

    private void queryVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort(SERVER_MAJOR_VERSION);
            outputStream.writeShort(SERVER_MINOR_VERSION);
            outputStream.writePad(20);
        }
    }

    private void getState(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)1);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writePad(20);
        }
    }

    private void getScreenCount(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)1);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(1);
            outputStream.writePad(20);
        }
    }

    private void getScreenSize(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(Short.toUnsignedInt(xServer.screenInfo.width));
            outputStream.writeInt(Short.toUnsignedInt(xServer.screenInfo.height));
            outputStream.writePad(16);
        }
    }

    private void isActive(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)1);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(1);
            outputStream.writePad(20);
        }
    }

    private void queryScreens(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(2);
            outputStream.writeInt(1);
            outputStream.writePad(20);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort(xServer.screenInfo.width);
            outputStream.writeShort(xServer.screenInfo.height);
        }
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        switch (client.getRequestData()) {
            case ClientOpcodes.QUERY_VERSION:
                queryVersion(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_STATE:
                getState(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_SCREEN_COUNT:
                getScreenCount(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_SCREEN_SIZE:
                getScreenSize(client, inputStream, outputStream);
                break;
            case ClientOpcodes.IS_ACTIVE:
                isActive(client, inputStream, outputStream);
                break;
            case ClientOpcodes.QUERY_SCREENS:
                queryScreens(client, inputStream, outputStream);
                break;
            default:
                ForensicLogger.logEvent(
                        ForensicLogger.getAppContext(),
                        "info",
                        "XSERVER_XINERAMA_REQUEST_SKIPPED",
                        null,
                        "xserver_extensions",
                        "xinerama_request_skipped",
                        ForensicLogger.fields(
                                "client_fd", client.fd,
                                "sequence_number", Short.toUnsignedInt(client.getSequenceNumber()),
                                "minor_opcode", Byte.toUnsignedInt(client.getRequestData()),
                                "remaining_request_length", client.getRemainingRequestLength()
                        )
                );
                client.skipRequest();
                break;
        }
    }
}
