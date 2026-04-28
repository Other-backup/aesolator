package com.winlator.cmod.xserver;

import androidx.collection.ArrayMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

public abstract class DesktopHelper {
    public static void attachTo(final XServer xServer) {
        setupXResources(xServer);

        xServer.pointer.addOnPointerMotionListener(new Pointer.OnPointerMotionListener() {
            @Override
            public void onPointerButtonPress(Pointer.Button button) {
                updateFocusedWindow(xServer);
            }
        });

        xServer.windowManager.addOnWindowModificationListener(new WindowManager.OnWindowModificationListener() {
            @Override
            public void onMapWindow(Window window) {
                setFocusedWindow(xServer, window);
            }
        });
    }

    private static void updateFocusedWindow(XServer xServer) {
        try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE)) {
            Window focusedWindow = xServer.windowManager.getFocusedWindow();
            Window child = xServer.windowManager.findPointWindow(xServer.pointer.getClampedX(), xServer.pointer.getClampedY());
            if (child == null && focusedWindow != xServer.windowManager.rootWindow) {
                xServer.windowManager.setFocus(xServer.windowManager.rootWindow, WindowManager.FocusRevertTo.NONE);
            }
            else if (child != null && child != focusedWindow) {
                setFocusedWindow(xServer, child);
            }
        }
    }

    private static void setFocusedWindow(XServer xServer, Window window) {
        if (window.isApplicationWindow()) {
            boolean parentIsRoot = window.getParent() == xServer.windowManager.rootWindow;
            xServer.windowManager.setFocus(window, parentIsRoot ? WindowManager.FocusRevertTo.POINTER_ROOT : WindowManager.FocusRevertTo.PARENT);
            xServer.getWinHandler().bringToFront(window.getClassName(), window.getHandle());
        }
    }

    private static void setupXResources(XServer xServer) {
        int atom = Atom.getId("RESOURCE_MANAGER");
        int type = Atom.getId("STRING");

        ArrayMap<String, String> values = new ArrayMap<>();
        values.put("size", "20");
        values.put("theme", "dmz");
        values.put("theme_core", "true");

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            sb.append("Xcursor")
              .append('.')
              .append(entry.getKey())
              .append(':')
              .append('\t')
              .append(entry.getValue())
              .append('\n');
        }

        byte[] data = sb.toString().getBytes(XServer.LATIN1_CHARSET);
        xServer.windowManager.rootWindow.modifyProperty(atom, type, Property.Format.BYTE_ARRAY, Property.Mode.APPEND, data);
        setupWindowManagerProperties(xServer);
    }

    private static void setupWindowManagerProperties(XServer xServer) {
        Window root = xServer.windowManager.rootWindow;
        int atomType = Atom.getId("ATOM");
        int cardinalType = Atom.getId("CARDINAL");
        int windowType = Atom.getId("WINDOW");
        int supportedAtom = Atom.internAtom("_NET_SUPPORTED");
        int supportingWmCheckAtom = Atom.internAtom("_NET_SUPPORTING_WM_CHECK");
        int clientListAtom = Atom.internAtom("_NET_CLIENT_LIST");
        int clientListStackingAtom = Atom.internAtom("_NET_CLIENT_LIST_STACKING");
        int activeWindowAtom = Atom.internAtom("_NET_ACTIVE_WINDOW");
        int workareaAtom = Atom.internAtom("_NET_WORKAREA");
        int desktopGeometryAtom = Atom.internAtom("_NET_DESKTOP_GEOMETRY");
        int desktopViewportAtom = Atom.internAtom("_NET_DESKTOP_VIEWPORT");
        int currentDesktopAtom = Atom.internAtom("_NET_CURRENT_DESKTOP");
        int numberOfDesktopsAtom = Atom.internAtom("_NET_NUMBER_OF_DESKTOPS");
        int showingDesktopAtom = Atom.internAtom("_NET_SHOWING_DESKTOP");
        int wmNameAtom = Atom.internAtom("_NET_WM_NAME");
        int utf8StringType = Atom.internAtom("UTF8_STRING");
        int gtkWorkareasAtom = Atom.internAtom("_GTK_WORKAREAS");

        root.modifyProperty(
                supportedAtom,
                atomType,
                Property.Format.INT_ARRAY,
                Property.Mode.REPLACE,
                intsToBytes(
                        supportingWmCheckAtom,
                        clientListAtom,
                        clientListStackingAtom,
                        activeWindowAtom,
                        workareaAtom,
                        desktopGeometryAtom,
                        desktopViewportAtom,
                        currentDesktopAtom,
                        numberOfDesktopsAtom,
                        showingDesktopAtom,
                        wmNameAtom,
                        gtkWorkareasAtom
                )
        );
        root.modifyProperty(supportingWmCheckAtom, windowType, Property.Format.INT_ARRAY, Property.Mode.REPLACE, intsToBytes(root.id));
        root.modifyProperty(clientListAtom, windowType, Property.Format.INT_ARRAY, Property.Mode.REPLACE, intsToBytes());
        root.modifyProperty(clientListStackingAtom, windowType, Property.Format.INT_ARRAY, Property.Mode.REPLACE, intsToBytes());
        root.modifyProperty(activeWindowAtom, windowType, Property.Format.INT_ARRAY, Property.Mode.REPLACE, intsToBytes(0));
        root.modifyProperty(workareaAtom, cardinalType, Property.Format.INT_ARRAY, Property.Mode.REPLACE, intsToBytes(0, 0, xServer.screenInfo.width, xServer.screenInfo.height));
        root.modifyProperty(desktopGeometryAtom, cardinalType, Property.Format.INT_ARRAY, Property.Mode.REPLACE, intsToBytes(xServer.screenInfo.width, xServer.screenInfo.height));
        root.modifyProperty(desktopViewportAtom, cardinalType, Property.Format.INT_ARRAY, Property.Mode.REPLACE, intsToBytes(0, 0));
        root.modifyProperty(currentDesktopAtom, cardinalType, Property.Format.INT_ARRAY, Property.Mode.REPLACE, intsToBytes(0));
        root.modifyProperty(numberOfDesktopsAtom, cardinalType, Property.Format.INT_ARRAY, Property.Mode.REPLACE, intsToBytes(1));
        root.modifyProperty(showingDesktopAtom, cardinalType, Property.Format.INT_ARRAY, Property.Mode.REPLACE, intsToBytes(0));
        root.modifyProperty(wmNameAtom, utf8StringType, Property.Format.BYTE_ARRAY, Property.Mode.REPLACE, "Ae.solator X11".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        root.modifyProperty(gtkWorkareasAtom, cardinalType, Property.Format.INT_ARRAY, Property.Mode.REPLACE, intsToBytes(0, 0, xServer.screenInfo.width, xServer.screenInfo.height));
    }

    private static byte[] intsToBytes(int... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int value : values) buffer.putInt(value);
        return buffer.array();
    }
}
