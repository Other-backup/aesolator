package com.winlator.cmod.xserver.requests;

import static com.winlator.cmod.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.XClient;
import com.winlator.cmod.xserver.errors.XRequestError;
import com.winlator.cmod.xserver.extensions.Extension;

import java.io.IOException;

public abstract class ExtensionRequests {
    public static void queryExtension(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        short length = inputStream.readShort();
        inputStream.skip(2);
        String name = inputStream.readString8(length);
        Extension extension = client.xServer.getExtensionByName(name);
        boolean missingOptionalExtension = extension == null && isExpectedOptionalExtension(name);
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                extension != null || missingOptionalExtension ? "info" : "warn",
                "XSERVER_EXTENSION_QUERY",
                null,
                "xserver_extensions",
                "x11_extension_query",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "resource_id_base", client.resourceIDBase,
                        "sequence_number", Short.toUnsignedInt(client.getSequenceNumber()),
                        "extension_name", name,
                        "present", extension != null,
                        "major_opcode", extension != null ? Byte.toUnsignedInt(extension.getMajorOpcode()) : -1,
                        "first_event_id", extension != null ? Byte.toUnsignedInt(extension.getFirstEventId()) : -1,
                        "first_error_id", extension != null ? Byte.toUnsignedInt(extension.getFirstErrorId()) : -1
                )
        );
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);

            if (extension != null) {
                outputStream.writeByte((byte)1);
                outputStream.writeByte(extension.getMajorOpcode());
                outputStream.writeByte(extension.getFirstEventId());
                outputStream.writeByte(extension.getFirstErrorId());
                outputStream.writePad(20);
            }
            else {
                outputStream.writeByte((byte)0);
                outputStream.writePad(23);
            }
        }
    }

    private static boolean isExpectedOptionalExtension(String name) {
        if (name == null) return false;
        String normalized = name.trim();
        return "Generic Event Extension".equals(normalized)
                || "XFree86-VidModeExtension".equals(normalized)
                || "RANDR".equals(normalized)
                || "XKEYBOARD".equals(normalized)
                || "XINERAMA".equals(normalized)
                || "MIT-SHM".equals(normalized);
    }
}
