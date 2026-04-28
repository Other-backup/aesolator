package com.winlator.cmod.xserver.requests;

import static com.winlator.cmod.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.Atom;
import com.winlator.cmod.xserver.Drawable;
import com.winlator.cmod.xserver.Bitmask;
import com.winlator.cmod.xserver.WindowAttributes;
import com.winlator.cmod.xserver.errors.BadDrawable;
import com.winlator.cmod.xserver.errors.BadIdChoice;
import com.winlator.cmod.xserver.errors.BadValue;
import com.winlator.cmod.xserver.events.CreateNotify;
import com.winlator.cmod.xserver.events.Event;
import com.winlator.cmod.xserver.Property;
import com.winlator.cmod.xserver.Visual;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.WindowManager;
import com.winlator.cmod.xserver.XClient;
import com.winlator.cmod.xserver.errors.BadAccess;
import com.winlator.cmod.xserver.errors.BadMatch;
import com.winlator.cmod.xserver.errors.BadWindow;
import com.winlator.cmod.xserver.errors.XRequestError;
import com.winlator.cmod.xserver.events.RawEvent;

import java.io.IOException;
import java.util.List;

public abstract class WindowRequests {
    public static void createWindow(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        byte depth = client.getRequestData();
        int windowId = inputStream.readInt();
        int parentId = inputStream.readInt();

        if (!client.isValidResourceId(windowId)) throw new BadIdChoice(windowId);

        Window parent = client.xServer.windowManager.getWindow(parentId);
        if (parent == null) throw new BadWindow(parentId);

        short x = inputStream.readShort();
        short y = inputStream.readShort();
        short width = inputStream.readShort();
        short height = inputStream.readShort();
        short borderWidth = inputStream.readShort();
        WindowAttributes.WindowClass windowClass = WindowAttributes.WindowClass.values()[(byte)inputStream.readShort()];
        Visual visual = client.xServer.pixmapManager.getVisual(inputStream.readInt());
        Bitmask valueMask = new Bitmask(inputStream.readInt());

        Window window = client.xServer.windowManager.createWindow(windowId, parent, x, y, width, height, windowClass, visual, depth, client);
        window.setBorderWidth(borderWidth);
        if (!valueMask.isEmpty()) window.attributes.update(valueMask, inputStream, client);
        client.setEventListenerForWindow(window, window.attributes.getEventMask());
        client.registerAsOwnerOfResource(window);
        parent.sendEvent(Event.SUBSTRUCTURE_NOTIFY, new CreateNotify(parent, window));
        logWindowAttributeContract(client, window, valueMask, "create_window");
    }

    public static void getWindowAttributes(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)window.attributes.getBackingStore().ordinal());
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(3);
            outputStream.writeInt(window.isInputOutput() ? window.getContent().visual.id : 0);
            outputStream.writeShort((short)window.attributes.getWindowClass().ordinal());
            outputStream.writeByte((byte)window.attributes.getBitGravity().ordinal());
            outputStream.writeByte((byte)window.attributes.getWinGravity().ordinal());
            outputStream.writeInt(window.attributes.getBackingPlanes());
            outputStream.writeInt(window.attributes.getBackingPixel());
            outputStream.writeByte((byte)(window.attributes.isSaveUnder() ? 1 : 0));
            outputStream.writeByte((byte)1);
            outputStream.writeByte((byte)window.getMapState().ordinal());
            outputStream.writeByte((byte)(window.attributes.isOverrideRedirect() ? 1 : 0));
            outputStream.writeInt(0);
            outputStream.writeInt(window.getAllEventMasks().getBits());
            outputStream.writeInt(client.getEventMaskForWindow(window).getBits());
            outputStream.writeShort((short)window.attributes.getDoNotPropagateMask().getBits());
            outputStream.writeShort((short)0);
        }
    }

    public static void changeWindowAttributes(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int windowId = inputStream.readInt();
        Bitmask valueMask = new Bitmask(inputStream.readInt());
        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);
        if (!valueMask.isEmpty()) {
            window.attributes.update(valueMask, inputStream, client);

            if (valueMask.isSet(WindowAttributes.FLAG_EVENT_MASK)) {
                if (isClientCanSelectFor(Event.SUBSTRUCTURE_REDIRECT, window, client) &&
                    isClientCanSelectFor(Event.RESIZE_REDIRECT, window, client) &&
                    isClientCanSelectFor(Event.BUTTON_PRESS, window, client)) {
                    client.setEventListenerForWindow(window, window.attributes.getEventMask());
                    logWindowAttributeContract(client, window, valueMask, "change_window_attributes");
                }
                else throw new BadAccess();
            }
            else {
                logWindowAttributeContract(client, window, valueMask, "change_window_attributes");
            }
        }
    }

    private static boolean isClientCanSelectFor(int eventId, Window window, XClient client) {
        return !window.attributes.getEventMask().isSet(eventId) || !(window.hasEventListenerFor(eventId) && !client.isInterestedIn(eventId, window));
    }

    private static void logWindowAttributeContract(XClient client, Window window, Bitmask valueMask, String source) {
        if (client == null || window == null || valueMask == null) return;
        Window parent = window.getParent();
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XSERVER_WINDOW_ATTRIBUTE_CONTRACT",
                null,
                "xserver_window",
                "xserver_window_attribute_contract",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "source", source,
                        "window_id", window.id,
                        "parent_window_id", parent != null ? parent.id : 0,
                        "value_mask", valueMask.getBits(),
                        "event_mask", window.attributes.getEventMask().getBits(),
                        "do_not_propagate_mask", window.attributes.getDoNotPropagateMask().getBits(),
                        "override_redirect", window.attributes.isOverrideRedirect(),
                        "substructure_redirect", window.hasEventListenerFor(Event.SUBSTRUCTURE_REDIRECT),
                        "substructure_notify", window.hasEventListenerFor(Event.SUBSTRUCTURE_NOTIFY),
                        "structure_notify", window.hasEventListenerFor(Event.STRUCTURE_NOTIFY),
                        "exposure", window.hasEventListenerFor(Event.EXPOSURE),
                        "property_change", window.hasEventListenerFor(Event.PROPERTY_CHANGE),
                        "root_window", window == client.xServer.windowManager.rootWindow,
                        "mapped", window.attributes.isMapped(),
                        "map_state", window.getMapState().name()
                )
        );
    }

    public static void destroyWindow(XClient client, XInputStream inputStream, XOutputStream outputStream) {
        client.xServer.windowManager.destroyWindow(inputStream.readInt());
    }

    public static void destroySubWindows(XClient client, XInputStream inputStream, XOutputStream outputStream) {
        client.xServer.windowManager.destroyWindow(inputStream.readInt());
    }

    public static void reparentWindow(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int windowId = inputStream.readInt();
        int parentId = inputStream.readInt();
        inputStream.skip(4);

        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        Window parent = client.xServer.windowManager.getWindow(parentId);
        if (parent == null) throw new BadWindow(parentId);

        client.xServer.windowManager.reparentWindow(window, parent);
    }

    public static void mapWindow(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int windowId = inputStream.readInt();
        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);
        client.xServer.windowManager.mapWindow(window);
    }

    public static void mapSubWindows(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int windowId = inputStream.readInt();
        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);
        for (Window child : window.getChildren())
            mapSubWindows(client, child.id);
        client.xServer.windowManager.mapWindow(window);
    }

    private static void mapSubWindows(XClient client, int windowId) throws XRequestError {
        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);
        for (Window child : window.getChildren())
            mapSubWindows(client, child.id);
        client.xServer.windowManager.mapWindow(window);
    }

    public static void unmapWindow(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int windowId = inputStream.readInt();
        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);
        client.xServer.windowManager.unmapWindow(window);
    }

    public static void changeProperty(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        Property.Mode mode = Property.Mode.values()[client.getRequestData()];
        int windowId = inputStream.readInt();
        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        int atom = inputStream.readInt();
        int type = inputStream.readInt();
        byte format = inputStream.readByte();
        inputStream.skip(3);
        int length  = inputStream.readInt();
        int totalSize = length * (format >> 3);

        byte[] data = null;
        if (totalSize > 0) {
            data = new byte[totalSize];
            inputStream.read(data);
            inputStream.skip(-totalSize & 3);
        }

        Property property = window.modifyProperty(atom, type, Property.Format.valueOf(format), mode, data);
        if (property == null) throw new BadMatch();

        logPropertyChanged(client, window, property, mode, data);
        client.xServer.windowManager.triggerOnModifyWindowProperty(window, property);
    }

    public static void deleteProperty(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int windowId = inputStream.readInt();
        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);
        int atom = inputStream.readInt();
        window.removeProperty(atom);
        logPropertyDeleted(client, window, atom);
    }

    public static void getProperty(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        boolean delete = client.getRequestData() == 1;
        short sequenceNumber = client.getSequenceNumber();
        int windowId = inputStream.readInt();
        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        int atom = inputStream.readInt();
        int type = inputStream.readInt();
        int longOffset = inputStream.readInt();
        int longLength = inputStream.readInt();
        Property property = window.getProperty(atom);

        int bytesAfter = 0;
        try (XStreamLock lock = outputStream.lock()) {
            if (property == null) {
                outputStream.writeByte(RESPONSE_CODE_SUCCESS);
                outputStream.writeByte((byte)0);
                outputStream.writeShort(sequenceNumber);
                outputStream.writeInt(0);
                outputStream.writeInt(0);
                outputStream.writeInt(0);
                outputStream.writeInt(0);
                outputStream.writePad(12);
            }
            else if (property.type != type && type != 0) {
                outputStream.writeByte(RESPONSE_CODE_SUCCESS);
                outputStream.writeByte(property.format.value);
                outputStream.writeShort(sequenceNumber);
                outputStream.writeInt(0);
                outputStream.writeInt(property.type);
                outputStream.writeInt(property.byteLength());
                outputStream.writeInt(0);
                outputStream.writePad(12);
            }
            else {
                byte[] data = property.data.array();
                long offset = Integer.toUnsignedLong(longOffset) * 4L;
                long requestedLength = Integer.toUnsignedLong(longLength) * 4L;
                if (offset > data.length) throw new BadValue(longOffset);
                int readOffset = (int) offset;
                int length = (int)Math.min(data.length - offset, requestedLength);
                bytesAfter = data.length - (readOffset + length);

                if (longOffset < 0 || longLength < 0) {
                    logUnsignedGetPropertyCompat(client, atom, type, property, longOffset, longLength, readOffset, length, bytesAfter);
                }

                outputStream.writeByte(RESPONSE_CODE_SUCCESS);
                outputStream.writeByte(property.format.value);
                outputStream.writeShort(sequenceNumber);
                outputStream.writeInt((length + 3) / 4);
                outputStream.writeInt(property.type);
                outputStream.writeInt(bytesAfter);
                outputStream.writeInt(length / (property.format.value / 8));
                outputStream.writePad(12);
                outputStream.write(data, readOffset, length);
                if ((-length & 3) > 0) outputStream.writePad(-length & 3);
            }
        }

        if (delete && property != null && bytesAfter == 0) {
            window.removeProperty(atom);
        }
    }

    private static void logUnsignedGetPropertyCompat(XClient client,
                                                     int atom,
                                                     int requestedType,
                                                     Property property,
                                                     int rawLongOffset,
                                                     int rawLongLength,
                                                     int readOffset,
                                                     int readLength,
                                                     int bytesAfter) {
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XSERVER_GET_PROPERTY_UNSIGNED_CARD32_COMPAT",
                null,
                "xserver_protocol",
                "x11_get_property_unsigned_card32_compat",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "resource_id_base", client.resourceIDBase,
                        "sequence_number", Short.toUnsignedInt(client.getSequenceNumber()),
                        "property_atom", atom,
                        "property_name", Atom.getName(atom),
                        "requested_type", requestedType,
                        "actual_type", property != null ? property.type : 0,
                        "actual_type_name", property != null ? Atom.getName(property.type) : "",
                        "raw_long_offset", rawLongOffset,
                        "raw_long_length", rawLongLength,
                        "unsigned_long_offset", Integer.toUnsignedLong(rawLongOffset),
                        "unsigned_long_length", Integer.toUnsignedLong(rawLongLength),
                        "read_offset", readOffset,
                        "read_length", readLength,
                        "bytes_after", bytesAfter
                )
        );
    }

    public static void queryPointer(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);
        short rootX = client.xServer.pointer.getClampedX();
        short rootY = client.xServer.pointer.getClampedY();
        Window child = window.getChildByCoords(rootX, rootY);
        short[] localPoint = window.rootPointToLocal(rootX, rootY);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)((!client.xServer.isRelativeMouseMovement())  ? 1 : 0));
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(client.xServer.windowManager.rootWindow.id);
            outputStream.writeInt(child != null ? child.id : 0);
            outputStream.writeShort(rootX);
            outputStream.writeShort(rootY);
            outputStream.writeShort(localPoint[0]);
            outputStream.writeShort(localPoint[1]);
            outputStream.writeShort((short)client.xServer.inputDeviceManager.getKeyButMask().getBits());
            outputStream.writePad(6);
        }
    }

    public static void translateCoordinates(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int srcWindowId = inputStream.readInt();
        int dstWindowId = inputStream.readInt();
        short srcX = inputStream.readShort();
        short srcY = inputStream.readShort();

        Window srcWindow = client.xServer.windowManager.getWindow(srcWindowId);
        Window dstWindow = client.xServer.windowManager.getWindow(dstWindowId);

        if (srcWindow == null) throw new BadWindow(srcWindowId);
        if (dstWindow == null) throw new BadWindow(dstWindowId);

        short[] rootPoint = srcWindow.localPointToRoot(srcX, srcY);
        short[] localPoint = dstWindow.rootPointToLocal(rootPoint[0], rootPoint[1]);
        Window child = dstWindow.getChildByCoords(rootPoint[0], rootPoint[1]);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)1);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(child != null ? child.id : 0);
            outputStream.writeShort(localPoint[0]);
            outputStream.writeShort(localPoint[1]);
            outputStream.writePad(16);
        }
    }

    public static void warpPointer(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        if (client.xServer.isRelativeMouseMovement()) {
            client.skipRequest();
            return;
        }

        Window srcWindow = client.xServer.windowManager.getWindow(inputStream.readInt());
        Window dstWindow = client.xServer.windowManager.getWindow(inputStream.readInt());
        short srcX = inputStream.readShort();
        short srcY = inputStream.readShort();
        short srcWidth = inputStream.readShort();
        short srcHeight = inputStream.readShort();
        short dstX = inputStream.readShort();
        short dstY = inputStream.readShort();

        if (srcWindow != null) {
            if (srcWidth == 0) srcWidth = (short)(srcWindow.getWidth() - srcX);
            if (srcHeight == 0) srcHeight = (short)(srcWindow.getHeight() - srcY);

            short[] localPoint = srcWindow.rootPointToLocal(client.xServer.pointer.getX(), client.xServer.pointer.getY());
            short softMarginX = (short)(client.xServer.screenInfo.width * 0.05f);
            short softMarginY = (short)(client.xServer.screenInfo.height * 0.05f);
            boolean isContained = localPoint[0] >= srcX - softMarginX && localPoint[1] >= srcY - softMarginY &&
                    localPoint[0] < (srcX + srcWidth + softMarginX) && localPoint[1] < (srcY + srcHeight + softMarginY);
            if (!isContained) return;
        }

        if (dstWindow == null) {
            client.xServer.pointer.setX(client.xServer.pointer.getX() + dstX);
            client.xServer.pointer.setY(client.xServer.pointer.getY() + dstY);
        }
        else {
            short[] localPoint = dstWindow.localPointToRoot(dstX, dstY);
            client.xServer.pointer.setX(localPoint[0]);
            client.xServer.pointer.setY(localPoint[1]);
        }
    }

    public static void setInputFocus(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        WindowManager.FocusRevertTo focusRevertTo = WindowManager.FocusRevertTo.values()[client.getRequestData()];
        int windowId = inputStream.readInt();
        inputStream.skip(4);

        switch (focusRevertTo) {
            case NONE:
                client.xServer.windowManager.setFocus(null, focusRevertTo);
                break;
            case POINTER_ROOT:
                client.xServer.windowManager.setFocus(client.xServer.windowManager.rootWindow, focusRevertTo);
                break;
            case PARENT:
                Window window = client.xServer.windowManager.getWindow(windowId);
                if (window == null) throw new BadWindow(windowId);
                client.xServer.windowManager.setFocus(window, focusRevertTo);
                break;
        }
    }

    private static void logPropertyChanged(XClient client, Window window, Property property, Property.Mode mode, byte[] data) {
        String atomName = safeAtomName(property.name);
        String typeName = safeAtomName(property.type);
        int byteLength = data != null ? data.length : 0;
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XSERVER_WINDOW_PROPERTY_CHANGED",
                null,
                "xserver_window",
                "xserver_window_property_changed",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "window_id", window.id,
                        "atom", property.name,
                        "atom_name", atomName,
                        "type", property.type,
                        "type_name", typeName,
                        "format", property.format != null ? property.format.value : 0,
                        "mode", mode.name(),
                        "byte_length", byteLength,
                        "value_sha256", byteLength > 0 ? ForensicLogger.sha256Hex(data) : "",
                        "identity_property", isIdentityAtom(atomName)
                )
        );
    }

    private static void logPropertyDeleted(XClient client, Window window, int atom) {
        String atomName = safeAtomName(atom);
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XSERVER_WINDOW_PROPERTY_DELETED",
                null,
                "xserver_window",
                "xserver_window_property_deleted",
                ForensicLogger.fields(
                        "client_fd", client.fd,
                        "window_id", window.id,
                        "atom", atom,
                        "atom_name", atomName,
                        "identity_property", isIdentityAtom(atomName)
                )
        );
    }

    private static String safeAtomName(int id) {
        try {
            return Atom.isValid(id) ? Atom.getName(id) : "";
        }
        catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean isIdentityAtom(String atomName) {
        switch (atomName) {
            case "WM_NAME":
            case "WM_CLASS":
            case "WM_HINTS":
            case "WM_CLIENT_MACHINE":
            case "_NET_WM_PID":
            case "_NET_WM_HWND":
            case "_WINE_HWND":
            case "_NET_WM_WOW64":
                return true;
            default:
                return false;
        }
    }

    public static void getInputFocus(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        Window focusedWindow = client.xServer.windowManager.getFocusedWindow();

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)client.xServer.windowManager.getFocusRevertTo().ordinal());
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(focusedWindow != null ? focusedWindow.id : 0);
            outputStream.writePad(20);
        }
    }

    public static void configureWindow(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int windowId = inputStream.readInt();
        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);
        Bitmask valueMask = new Bitmask(inputStream.readShort());
        inputStream.skip(2);
        if (!valueMask.isEmpty()) client.xServer.windowManager.configureWindow(window, valueMask, inputStream);
    }

    public static void getGeometry(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int drawableId = inputStream.readInt();
        Drawable drawable =  client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) throw new BadDrawable(drawableId);
        Window window = client.xServer.windowManager.getWindow(drawableId);
        short x = window != null ? window.getX() : 0;
        short y = window != null ? window.getY() : 0;
        short borderWidth = window != null ? window.getBorderWidth() : 0;

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte(drawable.visual.depth);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(client.xServer.windowManager.rootWindow.id);
            outputStream.writeShort(x);
            outputStream.writeShort(y);
            outputStream.writeShort(drawable.width);
            outputStream.writeShort(drawable.height);
            outputStream.writeShort(borderWidth);
            outputStream.writePad(10);
        }
    }

    public static void queryTree(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);
        Window parent = window.getParent();
        List<Window> children = window.getChildren();

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(children.size());
            outputStream.writeInt(client.xServer.windowManager.rootWindow.id);
            outputStream.writeInt(parent != null ? parent.id : 0);
            outputStream.writeShort((short)children.size());
            outputStream.writePad(14);

            for (int i = children.size()-1; i >= 0; i--) outputStream.writeInt(children.get(i).id);
        }
    }

    public static void sendEvent(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();

        if (windowId == 0 || windowId == 1) {
            client.skipRequest();
            return;
        }

        Window destination = client.xServer.windowManager.getWindow(windowId);
        if (destination == null) throw new BadWindow(windowId);

        Bitmask eventMask = new Bitmask(inputStream.readInt());

        byte[] data = new byte[32];
        inputStream.read(data);
        Event event = new RawEvent(data);

        if (eventMask.isEmpty()) {
            destination.originClient.sendEvent(event);
        }
        else destination.sendEvent(eventMask, event);
    }

    public static void getScreenSaver(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort((short)600);
            outputStream.writeShort((short)600);
            outputStream.writeByte((byte)1);
            outputStream.writeByte((byte)1);
            outputStream.writePad(18);
        }
    }
}
