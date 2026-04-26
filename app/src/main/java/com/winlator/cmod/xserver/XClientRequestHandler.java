package com.winlator.cmod.xserver;

import android.util.Log;

import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.xconnector.Client;
import com.winlator.cmod.xconnector.RequestHandler;
import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.errors.XRequestError;
import com.winlator.cmod.xserver.extensions.Extension;
import com.winlator.cmod.xserver.requests.AtomRequests;
import com.winlator.cmod.xserver.requests.CursorRequests;
import com.winlator.cmod.xserver.requests.DrawRequests;
import com.winlator.cmod.xserver.requests.ExtensionRequests;
import com.winlator.cmod.xserver.requests.FontRequests;
import com.winlator.cmod.xserver.requests.GrabRequests;
import com.winlator.cmod.xserver.requests.GraphicsContextRequests;
import com.winlator.cmod.xserver.requests.KeyboardRequests;
import com.winlator.cmod.xserver.requests.PixmapRequests;
import com.winlator.cmod.xserver.requests.SelectionRequests;
import com.winlator.cmod.xserver.requests.WindowRequests;

import java.io.IOException;
import java.nio.ByteOrder;

public class XClientRequestHandler implements RequestHandler {
    public static final byte RESPONSE_CODE_ERROR = 0;
    public static final byte RESPONSE_CODE_SUCCESS = 1;
    public static final int MAX_REQUEST_LENGTH = 65535;

    private static abstract class ClientOpcodes {
        private static final byte CREATE_WINDOW = 1;
        private static final byte CHANGE_WINDOW_ATTRIBUTES = 2;
        private static final byte GET_WINDOW_ATTRIBUTES = 3;
        private static final byte DESTROY_WINDOW = 4;
        private static final byte DESTROY_SUB_WINDOWS = 5;
        private static final byte REPARENT_WINDOW = 7;
        private static final byte MAP_WINDOW = 8;
        private static final byte MAP_SUB_WINDOWS = 9;
        private static final byte UNMAP_WINDOW = 10;
        private static final byte CONFIGURE_WINDOW = 12;
        private static final byte GET_GEOMETRY = 14;
        private static final byte QUERY_TREE = 15;
        private static final byte INTERN_ATOM = 16;
        private static final byte GET_ATOM_NAME = 17;
        private static final byte CHANGE_PROPERTY = 18;
        private static final byte DELETE_PROPERTY = 19;
        private static final byte GET_PROPERTY = 20;
        private static final byte SET_SELECTION_OWNER = 22;
        private static final byte GET_SELECTION_OWNER = 23;
        private static final byte SEND_EVENT = 25;
        private static final byte GRAB_POINTER = 26;
        private static final byte UNGRAB_POINTER = 27;
        private static final byte QUERY_POINTER = 38;
        private static final byte TRANSLATE_COORDINATES = 40;
        private static final byte WARP_POINTER = 41;
        private static final byte SET_INPUT_FOCUS = 42;
        private static final byte GET_INPUT_FOCUS = 43;
        private static final byte QUERY_KEYMAP = 44;
        private static final byte OPEN_FONT = 45;
        private static final byte LIST_FONTS = 49;
        private static final byte CREATE_PIXMAP = 53;
        private static final byte FREE_PIXMAP = 54;
        private static final byte CREATE_GC = 55;
        private static final byte CHANGE_GC = 56;
        private static final byte COPY_GC = 57;
        private static final byte SET_CLIP_RECTANGLES = 59;
        private static final byte FREE_GC = 60;
        private static final byte COPY_AREA = 62;
        private static final byte POLY_LINE = 65;
        private static final byte POLY_SEGMENT = 66;
        private static final byte POLY_RECTANGLE = 67;
        private static final byte POLY_FILL_RECTANGLE = 70;
        private static final byte PUT_IMAGE = 72;
        private static final byte GET_IMAGE = 73;
        private static final byte CREATE_COLORMAP = 78;
        private static final byte FREE_COLORMAP = 79;
        private static final byte CREATE_CURSOR = 93;
        private static final byte CREATE_GLYPH_CURSOR = 94;
        private static final byte FREE_CURSOR = 95;
        private static final byte QUERY_EXTENSION = 98;
        private static final byte GET_KEYBOARD_MAPPING = 101;
        private static final byte BELL = 104;
        private static final byte SET_SCREEN_SAVER = 107;
        private static final byte GET_SCREEN_SAVER = 108;
        private static final byte FORCE_SCREEN_SAVER = 115;
        private static final byte GET_POINTER_MAPPING = 117;
        private static final byte GET_MODIFIER_MAPPING = 119;
        private static final byte NO_OPERATION = 127;
    }

    @Override
    public boolean handleRequest(Client client) throws IOException {
        XClient xClient = (XClient)client.getTag();
        XInputStream inputStream = client.getInputStream();
        XOutputStream outputStream = client.getOutputStream();

        if (xClient.isAuthenticated()) {
            return handleNormalRequest(xClient, inputStream, outputStream);
        }
        else return handleAuthRequest(xClient, inputStream, outputStream);
    }

    private void sendServerInformation(XClient client, XOutputStream outputStream) throws IOException {
        short vendorNameLength = (short)XServer.VENDOR_NAME.length();
        byte pixmapFormatCount = (byte)client.xServer.pixmapManager.supportedPixmapFormats.length;
        short additionalDataLength = (short)(8 + (2 * pixmapFormatCount) + ((vendorNameLength + 3) / 4) + ((40 + 8 * client.xServer.pixmapManager.supportedVisuals.length + 24) + 3) / 4);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(XServer.VERSION);
            outputStream.writeShort((short)0);
            outputStream.writeShort(additionalDataLength);
            outputStream.writeInt(1);
            outputStream.writeInt(client.resourceIDBase);
            outputStream.writeInt(client.xServer.resourceIDs.idMask);
            outputStream.writeInt(256);
            outputStream.writeShort(vendorNameLength);
            outputStream.writeShort((short)MAX_REQUEST_LENGTH);
            outputStream.writeByte((byte)1);
            outputStream.writeByte(pixmapFormatCount);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)32);
            outputStream.writeByte((byte)32);
            outputStream.writeByte((byte)Keyboard.MIN_KEYCODE);
            outputStream.writeByte((byte)Keyboard.MAX_KEYCODE);
            outputStream.writeInt(0);
            outputStream.writeString8(XServer.VENDOR_NAME);

            for (PixmapFormat pixmapFormat : client.xServer.pixmapManager.supportedPixmapFormats) {
                outputStream.writeByte(pixmapFormat.depth);
                outputStream.writeByte(pixmapFormat.bitsPerPixel);
                outputStream.writeByte(pixmapFormat.scanlinePad);
                outputStream.writePad(5);
            }

            Visual rootVisual = client.xServer.windowManager.rootWindow.getContent().visual;

            outputStream.writeInt(client.xServer.windowManager.rootWindow.id);
            outputStream.writeInt(0);
            outputStream.writeInt(0xffffff);
            outputStream.writeInt(0x000000);
            outputStream.writeInt(client.xServer.windowManager.rootWindow.getAllEventMasks().getBits());
            outputStream.writeShort(client.xServer.screenInfo.width);
            outputStream.writeShort(client.xServer.screenInfo.height);
            outputStream.writeShort(client.xServer.screenInfo.getWidthInMillimeters());
            outputStream.writeShort(client.xServer.screenInfo.getHeightInMillimeters());
            outputStream.writeShort((short)1);
            outputStream.writeShort((short)1);
            outputStream.writeInt(rootVisual.id);
            outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeByte(rootVisual.depth);
            outputStream.writeByte((byte)client.xServer.pixmapManager.supportedVisuals.length);

            for (Visual visual : client.xServer.pixmapManager.supportedVisuals) {
                outputStream.writeByte(visual.depth);
                outputStream.writeByte((byte)0);
                outputStream.writeShort((short)(visual.displayable ? 1 : 0));
                outputStream.writeInt(0);

                if (visual.displayable) {
                    outputStream.writeInt(visual.id);
                    outputStream.writeByte(visual.visualClass);
                    outputStream.writeByte(visual.bitsPerRGBValue);
                    outputStream.writeShort(visual.colormapEntries);
                    outputStream.writeInt(visual.redMask);
                    outputStream.writeInt(visual.greenMask);
                    outputStream.writeInt(visual.blueMask);
                    outputStream.writeInt(0);
                }
            }
        }
    }

    private boolean handleAuthRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        if (inputStream.available() < 12) return false;

        byte byteOrder = inputStream.readByte();
        if (byteOrder == 66) {
            inputStream.setByteOrder(ByteOrder.BIG_ENDIAN);
            outputStream.setByteOrder(ByteOrder.BIG_ENDIAN);
        }
        else if (byteOrder == 108) {
            inputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            outputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        }

        inputStream.skip(1);

        short majorVersion = inputStream.readShort();
        if (majorVersion != 11) {
            logAuthFailure(client, byteOrder, majorVersion);
            throw new UnsupportedOperationException("Unsupported major X protocol version "+majorVersion+".");
        }

        inputStream.skip(2);
        int nameLength = inputStream.readShort();
        int dataLength = inputStream.readShort();
        inputStream.skip(2);

        if (nameLength > 0) inputStream.readString8(nameLength);
        if (dataLength > 0) inputStream.readString8(dataLength);

        if (!client.hasValidResourceIdBase()) {
            ForensicLogger.logEvent(
                    ForensicLogger.getAppContext(),
                    "error",
                    "XSERVER_CLIENT_AUTH_REJECTED_RESOURCE_EXHAUSTED",
                    null,
                    "xserver_protocol",
                    "x11_client_auth_rejected_resource_exhausted",
                    ForensicLogger.fields(
                            "client_fd", client.fd,
                            "resource_id_base", client.resourceIDBase,
                            "resource_max_clients", client.xServer.resourceIDs.maxClients,
                            "resource_allocated_count", client.xServer.resourceIDs.allocatedCount(),
                            "resource_available_count", client.xServer.resourceIDs.availableCount(),
                            "byte_order", byteOrder == 66 ? "big_endian" : byteOrder == 108 ? "little_endian" : "unknown",
                            "major_version", majorVersion
                    )
            );
            throw new IOException("X11 client resource id space exhausted");
        }

        try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
            sendServerInformation(client, outputStream);
        }

        client.setAuthenticated(true);
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XSERVER_CLIENT_AUTHENTICATED",
                null,
                "xserver_protocol",
                "x11_client_authenticated",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "resource_id_base", client.resourceIDBase,
                        "byte_order", byteOrder == 66 ? "big_endian" : byteOrder == 108 ? "little_endian" : "unknown",
                        "major_version", majorVersion,
                        "auth_name_length", nameLength,
                        "auth_data_length", dataLength,
                        "vendor", XServer.VENDOR_NAME,
                        "screen_width", client.xServer.screenInfo.width,
                        "screen_height", client.xServer.screenInfo.height,
                        "root_window_id", client.xServer.windowManager.rootWindow.id,
                        "pixmap_format_count", client.xServer.pixmapManager.supportedPixmapFormats.length,
                        "visual_count", client.xServer.pixmapManager.supportedVisuals.length
                )
        );
        return true;
    }

    private boolean handleNormalRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        if (inputStream.available() < 4) return false;
        byte opcode = inputStream.readByte();
        byte requestData = inputStream.readByte();

        int requestLength = inputStream.readUnsignedShort();
        if (requestLength != 0) {
            requestLength = requestLength * 4 - 4;
        }
        else if (inputStream.available() < 4) {
            return false;
        }
        else requestLength = inputStream.readInt() * 4 - 8;
        if (inputStream.available() < requestLength) return false;

        client.generateSequenceNumber();
        client.setRequestData(requestData);
        client.setRequestLength(requestLength);
        logProtocolMilestone(client, opcode, requestLength, inputStream.available());

        try {
            switch (opcode) {
                case ClientOpcodes.CREATE_WINDOW:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.INPUT_DEVICE, XServer.Lockable.CURSOR_MANAGER)) {
                        WindowRequests.createWindow(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.CHANGE_WINDOW_ATTRIBUTES:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.CURSOR_MANAGER)) {
                        WindowRequests.changeWindowAttributes(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.GET_WINDOW_ATTRIBUTES:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                        WindowRequests.getWindowAttributes(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.DESTROY_WINDOW:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.INPUT_DEVICE)) {
                        WindowRequests.destroyWindow(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.DESTROY_SUB_WINDOWS:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.INPUT_DEVICE)) {
                        WindowRequests.destroySubWindows(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.REPARENT_WINDOW:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                        WindowRequests.reparentWindow(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.MAP_WINDOW:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE)) {
                        WindowRequests.mapWindow(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.MAP_SUB_WINDOWS:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE)) {
                        WindowRequests.mapSubWindows(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.UNMAP_WINDOW:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE)) {
                        WindowRequests.unmapWindow(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.CONFIGURE_WINDOW:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE)) {
                        WindowRequests.configureWindow(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.GET_GEOMETRY:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER)) {
                        WindowRequests.getGeometry(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.QUERY_TREE:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                        WindowRequests.queryTree(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.INTERN_ATOM:
                    AtomRequests.internAtom(client, inputStream, outputStream);
                    break;
                /* This seems to also link to UnmapWindow */
                case ClientOpcodes.GET_ATOM_NAME:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE)) {
                        AtomRequests.getAtomName(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.CHANGE_PROPERTY:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                        WindowRequests.changeProperty(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.DELETE_PROPERTY:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                        WindowRequests.deleteProperty(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.GET_PROPERTY:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                        WindowRequests.getProperty(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.SET_SELECTION_OWNER:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                        SelectionRequests.setSelectionOwner(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.GET_SELECTION_OWNER:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                        SelectionRequests.getSelectionOwner(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.SEND_EVENT:
                    try (XLock lock = client.xServer.lockAll()) {
                        WindowRequests.sendEvent(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.GRAB_POINTER:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE, XServer.Lockable.CURSOR_MANAGER)) {
                        GrabRequests.grabPointer(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.UNGRAB_POINTER:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE)) {
                        GrabRequests.ungrabPointer(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.QUERY_POINTER:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE)) {
                        WindowRequests.queryPointer(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.TRANSLATE_COORDINATES:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                        WindowRequests.translateCoordinates(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.WARP_POINTER:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE)) {
                        WindowRequests.warpPointer(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.SET_INPUT_FOCUS:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                        WindowRequests.setInputFocus(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.GET_INPUT_FOCUS:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                        WindowRequests.getInputFocus(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.QUERY_KEYMAP:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                        outputStream.writeByte(RESPONSE_CODE_SUCCESS);
                        outputStream.writeByte((byte) 0);
                        outputStream.writeShort(client.getSequenceNumber());
                        outputStream.writeInt(2);
                        outputStream.writePad(32);
                    }
                    break;
                case ClientOpcodes.OPEN_FONT:
                    FontRequests.openFont(client, inputStream, outputStream);
                    break;
                case ClientOpcodes.LIST_FONTS:
                    FontRequests.listFonts(client, inputStream, outputStream);
                    break;
                case ClientOpcodes.CREATE_PIXMAP:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER)) {
                        PixmapRequests.createPixmap(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.FREE_PIXMAP:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER)) {
                        PixmapRequests.freePixmap(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.CREATE_GC:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER)) {
                        GraphicsContextRequests.createGC(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.CHANGE_GC:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER)) {
                        GraphicsContextRequests.changeGC(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.COPY_GC:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER)) {
                        GraphicsContextRequests.copyGC(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.SET_CLIP_RECTANGLES:
                    client.skipRequest();
                    break;
                case ClientOpcodes.FREE_GC:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.GRAPHIC_CONTEXT_MANAGER)) {
                        GraphicsContextRequests.freeGC(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.COPY_AREA:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER)) {
                        DrawRequests.copyArea(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.POLY_LINE:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER)) {
                        DrawRequests.polyLine(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.POLY_SEGMENT:
                    client.skipRequest();
                    break;
                case ClientOpcodes.POLY_RECTANGLE:
                    client.skipRequest();
                    break;
                case ClientOpcodes.POLY_FILL_RECTANGLE:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER)) {
                        DrawRequests.polyFillRectangle(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.PUT_IMAGE:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER)) {
                        DrawRequests.putImage(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.GET_IMAGE:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER)) {
                        DrawRequests.getImage(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.CREATE_COLORMAP:
                    client.skipRequest();
                    break;
                case ClientOpcodes.FREE_COLORMAP:
                    client.skipRequest();
                    break;
                case ClientOpcodes.CREATE_CURSOR:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.CURSOR_MANAGER)) {
                        CursorRequests.createCursor(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.CREATE_GLYPH_CURSOR:
                    client.skipRequest();
                    break;
                case ClientOpcodes.FREE_CURSOR:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.CURSOR_MANAGER)) {
                        CursorRequests.freeCursor(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.QUERY_EXTENSION:
                    ExtensionRequests.queryExtension(client, inputStream, outputStream);
                    break;
                case ClientOpcodes.GET_KEYBOARD_MAPPING:
                    try (XLock lock = client.xServer.lock(XServer.Lockable.INPUT_DEVICE)) {
                        KeyboardRequests.getKeyboardMapping(client, inputStream, outputStream);
                    }
                    break;
                case ClientOpcodes.BELL:
                    client.skipRequest();
                    break;
                case ClientOpcodes.SET_SCREEN_SAVER:
                    client.skipRequest();
                    break;
                case ClientOpcodes.GET_SCREEN_SAVER:
                    WindowRequests.getScreenSaver(client, inputStream, outputStream);
                    break;
                case ClientOpcodes.FORCE_SCREEN_SAVER:
                    client.skipRequest();
                    break;
                case ClientOpcodes.GET_MODIFIER_MAPPING:
                    KeyboardRequests.getModifierMapping(client, inputStream, outputStream);
                    break;
                case ClientOpcodes.NO_OPERATION:
                    client.skipRequest();
                    break;
                case ClientOpcodes.GET_POINTER_MAPPING:
                    CursorRequests.getPointerMapping(client, inputStream, outputStream);
                    break;
                case 36: // X_GrabServer
                    try (XLock lock = client.xServer.lockAll()) {
                        client.xServer.setGrabbed(true, client);
                        outputStream.writeSuccessReply(client.getSequenceNumber(), 0);
                        Log.d("XClientRequestHandler", "X_GrabServer request handled successfully:" + outputStream.buffer.position());
                    }
                    break;

                case 37: // X_UngrabServer
                    try (XLock lock = client.xServer.lockAll()) {
                        if (client.xServer.isGrabbedBy(client)) {
                            client.xServer.setGrabbed(false, null);
                        }
                        outputStream.writeSuccessReply(client.getSequenceNumber(), 0);
                        Log.d("XClientRequestHandler", "X_UngrabServer request handled successfully:" + outputStream.buffer.position());
                    }
                    break;
                default:
                    if (opcode < 0) {
                        Extension extension = client.xServer.extensions.get(opcode);
                        if (extension != null) extension.handleRequest(client, inputStream, outputStream);
                    }
                    else {
                        Log.d("XClientRequestHandler", "Unsupported opcode " + opcode);
                        logUnsupportedOpcode(client, opcode, requestLength);
                    }
                    break;
            }
        }
        catch (XRequestError e) {
            logProtocolError(client, opcode, requestLength, e);
            client.skipRequest();
            e.sendError(client, opcode);
        }

        return true;
    }

    private static void logProtocolMilestone(XClient client, byte opcode, int requestLength, int availableAfterHeader) {
        String opcodeName = describeMilestoneOpcode(opcode);
        if (opcodeName == null) return;
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XSERVER_PROTOCOL_MILESTONE",
                null,
                "xserver_protocol",
                "x11_protocol_milestone",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "resource_id_base", client.resourceIDBase,
                        "sequence_number", Short.toUnsignedInt(client.getSequenceNumber()),
                        "opcode", Byte.toUnsignedInt(opcode),
                        "opcode_name", opcodeName,
                        "request_length", requestLength,
                        "available_after_header", availableAfterHeader
                )
        );
    }

    private static String describeMilestoneOpcode(byte opcode) {
        switch (opcode) {
            case ClientOpcodes.CREATE_WINDOW:
                return "CreateWindow";
            case ClientOpcodes.CHANGE_WINDOW_ATTRIBUTES:
                return "ChangeWindowAttributes";
            case ClientOpcodes.MAP_WINDOW:
                return "MapWindow";
            case ClientOpcodes.UNMAP_WINDOW:
                return "UnmapWindow";
            case ClientOpcodes.CONFIGURE_WINDOW:
                return "ConfigureWindow";
            case ClientOpcodes.INTERN_ATOM:
                return "InternAtom";
            case ClientOpcodes.CHANGE_PROPERTY:
                return "ChangeProperty";
            case ClientOpcodes.GET_PROPERTY:
                return "GetProperty";
            case ClientOpcodes.SEND_EVENT:
                return "SendEvent";
            case ClientOpcodes.QUERY_EXTENSION:
                return "QueryExtension";
            case ClientOpcodes.NO_OPERATION:
                return "NoOperation";
            default:
                return null;
        }
    }

    private static void logAuthFailure(XClient client, byte byteOrder, short majorVersion) {
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "error",
                "XSERVER_CLIENT_AUTH_REJECTED",
                null,
                "xserver_protocol",
                "x11_client_auth_rejected",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "resource_id_base", client.resourceIDBase,
                        "byte_order", byteOrder == 66 ? "big_endian" : byteOrder == 108 ? "little_endian" : "unknown",
                        "major_version", majorVersion
                )
        );
    }

    private static void logUnsupportedOpcode(XClient client, byte opcode, int requestLength) {
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "warn",
                "XSERVER_UNSUPPORTED_OPCODE",
                null,
                "xserver_protocol",
                "x11_unsupported_opcode",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "resource_id_base", client.resourceIDBase,
                        "sequence_number", Short.toUnsignedInt(client.getSequenceNumber()),
                        "opcode", Byte.toUnsignedInt(opcode),
                        "request_length", requestLength
                )
        );
    }

    private static void logProtocolError(XClient client, byte opcode, int requestLength, XRequestError error) {
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "warn",
                "XSERVER_PROTOCOL_ERROR",
                null,
                "xserver_protocol",
                "x11_protocol_error",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "resource_id_base", client.resourceIDBase,
                        "sequence_number", Short.toUnsignedInt(client.getSequenceNumber()),
                        "opcode", Byte.toUnsignedInt(opcode),
                        "request_length", requestLength,
                        "error_class", error.getClass().getName(),
                        "error_code", Byte.toUnsignedInt(error.getCode()),
                        "error_data", error.getData()
                )
        );
    }
}
