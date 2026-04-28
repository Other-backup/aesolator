package com.winlator.cmod.xserver.extensions;

import static com.winlator.cmod.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.XClient;
import com.winlator.cmod.xserver.errors.XRequestError;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class GenericEventExtension implements Extension {
    public static final byte MAJOR_OPCODE = -111;
    private static final short SERVER_MAJOR_VERSION = 1;
    private static final short SERVER_MINOR_VERSION = 0;

    private final Set<XClient> versionAwareClients = Collections.newSetFromMap(new WeakHashMap<>());

    private static abstract class ClientOpcodes {
        private static final byte QUERY_VERSION = 0;
    }

    static short[] negotiateVersion(int requestedMajor, int requestedMinor) {
        if (requestedMajor > SERVER_MAJOR_VERSION) {
            return new short[]{SERVER_MAJOR_VERSION, SERVER_MINOR_VERSION};
        }
        if (requestedMajor == SERVER_MAJOR_VERSION && requestedMinor > SERVER_MINOR_VERSION) {
            return new short[]{SERVER_MAJOR_VERSION, SERVER_MINOR_VERSION};
        }
        return new short[]{(short)requestedMajor, (short)requestedMinor};
    }

    @Override
    public String getName() {
        return "Generic Event Extension";
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

    public boolean isClientVersionAware(XClient client) {
        synchronized (versionAwareClients) {
            return versionAwareClients.contains(client);
        }
    }

    public void onClientDisconnected(XClient client) {
        synchronized (versionAwareClients) {
            versionAwareClients.remove(client);
        }
    }

    private void queryVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        int requestedMajor = inputStream.readUnsignedShort();
        int requestedMinor = inputStream.readUnsignedShort();
        inputStream.skip(client.getRemainingRequestLength());

        short[] negotiated = negotiateVersion(requestedMajor, requestedMinor);
        boolean versionAware = negotiated[0] >= SERVER_MAJOR_VERSION;
        if (versionAware) {
            synchronized (versionAwareClients) {
                versionAwareClients.add(client);
            }
        }

        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XSERVER_GENERIC_EVENT_QUERY_VERSION",
                null,
                "xserver_extensions",
                "generic_event_query_version",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "resource_id_base", client.resourceIDBase,
                        "sequence_number", Short.toUnsignedInt(client.getSequenceNumber()),
                        "requested_major", requestedMajor,
                        "requested_minor", requestedMinor,
                        "server_major", Short.toUnsignedInt(negotiated[0]),
                        "server_minor", Short.toUnsignedInt(negotiated[1]),
                        "xge_version_aware", versionAware ? "1" : "0"
                )
        );

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte(ClientOpcodes.QUERY_VERSION);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort(negotiated[0]);
            outputStream.writeShort(negotiated[1]);
            outputStream.writePad(20);
        }
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int opcode = Byte.toUnsignedInt(client.getRequestData());
        if (opcode == ClientOpcodes.QUERY_VERSION) {
            queryVersion(client, inputStream, outputStream);
            return;
        }

        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "warn",
                "XSERVER_GENERIC_EVENT_UNHANDLED_OPCODE",
                null,
                "xserver_extensions",
                "generic_event_unhandled_opcode",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "resource_id_base", client.resourceIDBase,
                        "sequence_number", Short.toUnsignedInt(client.getSequenceNumber()),
                        "minor_opcode", opcode,
                        "request_length", client.getRemainingRequestLength()
                )
        );
        inputStream.skip(client.getRemainingRequestLength());
    }
}
