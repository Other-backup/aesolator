package com.winlator.cmod.xserver.extensions;

import static com.winlator.cmod.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.XClient;
import com.winlator.cmod.xserver.XServer;
import com.winlator.cmod.xserver.errors.BadAccess;
import com.winlator.cmod.xserver.errors.BadImplementation;
import com.winlator.cmod.xserver.errors.BadMatch;
import com.winlator.cmod.xserver.errors.BadValue;
import com.winlator.cmod.xserver.errors.BadWindow;
import com.winlator.cmod.xserver.errors.XRequestError;

import java.io.IOException;

public class XComposite implements Extension {
    public static final byte MAJOR_OPCODE = -105;
    public static final byte MAJOR_VERSION = 0;
    public static final byte MINOR_VERSION = 1;
    private static final String TAG_REDIRECT_PARENT = "compositeRedirectParent";
    private static final String TAG_REDIRECT_COUNT = "compositeRedirectCount";
    private static final String TAG_RENDER_SUBWINDOWS_BEFORE_REDIRECT = "compositeRenderSubwindowsBeforeRedirect";
    private final XServer xServer;

    private static abstract class ClientOpcodes {
        private static final byte QUERY_VERSION = 0;
        private static final byte REDIRECT_WINDOW = 1;
        private static final byte UNREDIRECT_WINDOW = 3;
    }

    private static abstract class UpdateMode {
        private static final byte REDIRECT_MANUAL = 1;
    }

    public XComposite(XServer xServer) {
        this.xServer = xServer;
    }

    @Override
    public String getName() {
        return "Composite";
    }

    @Override
    public byte getMajorOpcode() {
        return MAJOR_OPCODE;
    }

    @Override
    public byte getFirstErrorId() {
        return Byte.MIN_VALUE;
    }

    @Override
    public byte getFirstEventId() {
        return 0;
    }

    private void setWindowsToOffscreenStorage(Window window, boolean offscreenStorage) {
        if (!window.attributes.isMapped()) return;
        if (window.isInputOutput()) window.getContent().setOffscreenStorage(offscreenStorage);

        for (Window child : window.getChildren()) {
            setWindowsToOffscreenStorage(child, offscreenStorage);
        }
    }

    private int getRedirectCount(Window parent) {
        Object tag = parent.getTag(TAG_REDIRECT_COUNT, 0);
        return tag instanceof Number ? ((Number) tag).intValue() : 0;
    }

    private boolean getRenderSubwindowsBeforeRedirect(Window parent) {
        Object tag = parent.getTag(TAG_RENDER_SUBWINDOWS_BEFORE_REDIRECT, Boolean.TRUE);
        return !(tag instanceof Boolean) || (Boolean) tag;
    }

    private void queryVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(8);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte) 0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(MAJOR_VERSION);
            outputStream.writeInt(MINOR_VERSION);
            outputStream.writePad(16);
        }
    }

    private void redirectWindow(XInputStream inputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        byte updateMode = inputStream.readByte();
        inputStream.skip(3);

        Window window = xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);
        if (window == xServer.windowManager.rootWindow) throw new BadMatch();
        if (updateMode != UpdateMode.REDIRECT_MANUAL) throw new BadImplementation();
        if (window.getTag(TAG_REDIRECT_PARENT) != null) throw new BadAccess();

        Window parent = window.getParent();
        if (parent == null) throw new BadMatch();

        int redirectCount = getRedirectCount(parent);
        if (redirectCount == 0) {
            parent.setTag(TAG_RENDER_SUBWINDOWS_BEFORE_REDIRECT, parent.attributes.isRenderSubwindows());
        }
        parent.setTag(TAG_REDIRECT_COUNT, redirectCount + 1);
        window.setTag(TAG_REDIRECT_PARENT, parent);
        setWindowsToOffscreenStorage(window, true);
        parent.attributes.setRenderSubwindows(false);
        xServer.windowManager.triggerOnChangeWindowZOrder(window);
    }

    private void unredirectWindow(XInputStream inputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        byte updateMode = inputStream.readByte();
        inputStream.skip(3);

        Window window = xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);
        if (window == xServer.windowManager.rootWindow) throw new BadMatch();
        if (updateMode != UpdateMode.REDIRECT_MANUAL) throw new BadImplementation();

        Window parent = (Window) window.getTag(TAG_REDIRECT_PARENT);
        if (parent == null) throw new BadValue(windowId);

        window.removeTag(TAG_REDIRECT_PARENT);
        setWindowsToOffscreenStorage(window, false);

        int redirectCount = Math.max(0, getRedirectCount(parent) - 1);
        if (redirectCount == 0) {
            parent.attributes.setRenderSubwindows(getRenderSubwindowsBeforeRedirect(parent));
            parent.removeTag(TAG_REDIRECT_COUNT);
            parent.removeTag(TAG_RENDER_SUBWINDOWS_BEFORE_REDIRECT);
        } else {
            parent.setTag(TAG_REDIRECT_COUNT, redirectCount);
        }
        xServer.windowManager.triggerOnChangeWindowZOrder(window);
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        switch (client.getRequestData()) {
            case ClientOpcodes.QUERY_VERSION:
                queryVersion(client, inputStream, outputStream);
                break;
            case ClientOpcodes.REDIRECT_WINDOW:
                redirectWindow(inputStream);
                break;
            case ClientOpcodes.UNREDIRECT_WINDOW:
                unredirectWindow(inputStream);
                break;
            default:
                throw new BadImplementation();
        }
    }
}
