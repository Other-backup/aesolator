package com.winlator.cmod.xserver;

import android.util.SparseArray;

import com.winlator.cmod.core.CursorLocker;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.winhandler.WinHandler;
import com.winlator.cmod.xserver.extensions.BigReqExtension;
import com.winlator.cmod.xserver.extensions.DRI3Extension;
import com.winlator.cmod.xserver.extensions.Extension;
import com.winlator.cmod.xserver.extensions.MITSHMExtension;
import com.winlator.cmod.xserver.extensions.PresentExtension;
import com.winlator.cmod.xserver.extensions.SyncExtension;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.concurrent.locks.ReentrantLock;

public class XServer {
    public enum Lockable {WINDOW_MANAGER, PIXMAP_MANAGER, DRAWABLE_MANAGER, GRAPHIC_CONTEXT_MANAGER, INPUT_DEVICE, CURSOR_MANAGER, SHMSEGMENT_MANAGER}
    public static final short VERSION = 11;
    public static final String VENDOR_NAME = "Elbrus Technologies, LLC";
    public static final Charset LATIN1_CHARSET = Charset.forName("latin1");
    public final SparseArray<Extension> extensions = new SparseArray<>();
    public final ScreenInfo screenInfo;
    public final PixmapManager pixmapManager;
    public final ResourceIDs resourceIDs = new ResourceIDs(128);
    public final GraphicsContextManager graphicsContextManager = new GraphicsContextManager();
    public final SelectionManager selectionManager;
    public final DrawableManager drawableManager;
    public final WindowManager windowManager;
    public final CursorManager cursorManager;
    public final Keyboard keyboard = Keyboard.createKeyboard(this);
    public final Pointer pointer = new Pointer(this);
    public final InputDeviceManager inputDeviceManager;
    public final GrabManager grabManager;
    public final CursorLocker cursorLocker;
    private SHMSegmentManager shmSegmentManager;
    private GLRenderer renderer;
    private WinHandler winHandler;
    private final EnumMap<Lockable, ReentrantLock> locks = new EnumMap<>(Lockable.class);
    private boolean relativeMouseMovement = false;
    private boolean simulateTouchScreen = false;
    private boolean isGrabbed = false;
    private XClient grabbingClient = null;

    public XServer(ScreenInfo screenInfo) {
        this.screenInfo = screenInfo;
        for (Lockable lockable : Lockable.values()) locks.put(lockable, new ReentrantLock());
        ForensicLogger.appCheckpoint("info", "XSERVER_CONSTRUCTOR_LOCKS_READY", "xserver", "xserver_constructor_locks_ready",
                ForensicLogger.fields("screen_width", screenInfo.width, "screen_height", screenInfo.height));

        pixmapManager = new PixmapManager();
        ForensicLogger.appCheckpoint("info", "XSERVER_CONSTRUCTOR_PIXMAP_MANAGER_READY", "xserver", "xserver_pixmap_manager_ready",
                ForensicLogger.fields("visual_depth", pixmapManager.visual.depth));
        drawableManager = new DrawableManager(this);
        ForensicLogger.appCheckpoint("info", "XSERVER_CONSTRUCTOR_DRAWABLE_MANAGER_READY", "xserver", "xserver_drawable_manager_ready", null);
        cursorManager = new CursorManager(drawableManager);
        ForensicLogger.appCheckpoint("info", "XSERVER_CONSTRUCTOR_CURSOR_MANAGER_READY", "xserver", "xserver_cursor_manager_ready", null);
        windowManager = new WindowManager(screenInfo, drawableManager);
        ForensicLogger.appCheckpoint("info", "XSERVER_CONSTRUCTOR_WINDOW_MANAGER_READY", "xserver", "xserver_window_manager_ready",
                ForensicLogger.fields("root_window_id", windowManager.rootWindow.id));
        selectionManager = new SelectionManager(windowManager);
        ForensicLogger.appCheckpoint("info", "XSERVER_CONSTRUCTOR_SELECTION_MANAGER_READY", "xserver", "xserver_selection_manager_ready", null);
        inputDeviceManager = new InputDeviceManager(this);
        ForensicLogger.appCheckpoint("info", "XSERVER_CONSTRUCTOR_INPUT_DEVICE_MANAGER_READY", "xserver", "xserver_input_device_manager_ready", null);
        grabManager = new GrabManager(this);
        ForensicLogger.appCheckpoint("info", "XSERVER_CONSTRUCTOR_GRAB_MANAGER_READY", "xserver", "xserver_grab_manager_ready", null);

        DesktopHelper.attachTo(this);
        ForensicLogger.appCheckpoint("info", "XSERVER_CONSTRUCTOR_DESKTOP_HELPER_READY", "xserver", "xserver_desktop_helper_attached", null);
        setupExtensions();
        ForensicLogger.appCheckpoint("info", "XSERVER_CONSTRUCTOR_EXTENSIONS_READY", "xserver", "xserver_extensions_ready",
                ForensicLogger.fields("extension_count", extensions.size()));
        cursorLocker = new CursorLocker(this);
        ForensicLogger.appCheckpoint("info", "XSERVER_CONSTRUCTOR_CURSOR_LOCKER_READY", "xserver", "xserver_cursor_locker_ready", null);
    }

    public boolean isRelativeMouseMovement() {
        return relativeMouseMovement;
    }

    public void setRelativeMouseMovement(boolean relativeMouseMovement) {
        cursorLocker.setEnabled(!relativeMouseMovement);
        this.relativeMouseMovement = relativeMouseMovement;
    }

    public boolean isSimulateTouchScreen() { return simulateTouchScreen; }

    public void setSimulateTouchScreen(boolean simulateTouchScreen) {
        this.simulateTouchScreen = simulateTouchScreen;
    }

    public GLRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(GLRenderer renderer) {
        this.renderer = renderer;
    }

    public WinHandler getWinHandler() {
        return winHandler;
    }

    public void setWinHandler(WinHandler winHandler) {
        this.winHandler = winHandler;
    }

    public SHMSegmentManager getSHMSegmentManager() {
        return shmSegmentManager;
    }

    public void setSHMSegmentManager(SHMSegmentManager shmSegmentManager) {
        this.shmSegmentManager = shmSegmentManager;
    }

    private class SingleXLock implements XLock {
        private final ReentrantLock lock;

        private SingleXLock(Lockable lockable) {
            this.lock = locks.get(lockable);
            lock.lock();
        }

        @Override
        public void close() {
            lock.unlock();
        }
    }

    private class MultiXLock implements XLock {
        private final Lockable[] lockables;

        private MultiXLock(Lockable[] lockables) {
            this.lockables = normalizeLockables(lockables);
            for (Lockable lockable : this.lockables) {
                locks.get(lockable).lock();
            }
        }

        @Override
        public void close() {
            for (int i = lockables.length - 1; i >= 0; i--) {
                locks.get(lockables[i]).unlock();
            }
        }
    }

    private Lockable[] normalizeLockables(Lockable[] input) {
        if (input == null || input.length == 0) return new Lockable[0];

        EnumSet<Lockable> unique = EnumSet.noneOf(Lockable.class);
        for (Lockable lockable : input) {
            if (lockable != null) unique.add(lockable);
        }

        Lockable[] normalized = unique.toArray(new Lockable[0]);
        Arrays.sort(normalized, Comparator.comparingInt(Enum::ordinal));
        return normalized;
    }

    public XLock lock(Lockable lockable) {
        return new SingleXLock(lockable);
    }

    public XLock lock(Lockable... lockables) {
        return new MultiXLock(lockables);
    }

    public XLock lockAll() {
        return new MultiXLock(Lockable.values());
    }

    public Extension getExtensionByName(String name) {
        for (int i = 0; i < extensions.size(); i++) {
            Extension extension = extensions.valueAt(i);
            if (extension.getName().equals(name)) return extension;
        }
        return null;
    }

    public void injectPointerMove(int x, int y) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setPosition(x, y);
        }
    }

    public void injectPointerMoveDelta(int dx, int dy) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setPosition(pointer.getX() + dx, pointer.getY() + dy);
        }
    }

    public void injectPointerButtonPress(Pointer.Button buttonCode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setButton(buttonCode, true);
        }
    }

    public void injectPointerButtonRelease(Pointer.Button buttonCode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setButton(buttonCode, false);
        }
    }

    public void injectKeyPress(XKeycode xKeycode) {
        injectKeyPress(xKeycode, 0);
    }

    public void injectKeyPress(XKeycode xKeycode, int keysym) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            keyboard.setKeyPress(xKeycode.id, keysym);
        }
    }

    public void injectKeyRelease(XKeycode xKeycode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            keyboard.setKeyRelease(xKeycode.id);
        }
    }

    private void setupExtensions() {
        extensions.put(BigReqExtension.MAJOR_OPCODE, new BigReqExtension());
        extensions.put(MITSHMExtension.MAJOR_OPCODE, new MITSHMExtension());
        extensions.put(DRI3Extension.MAJOR_OPCODE, new DRI3Extension());
        extensions.put(PresentExtension.MAJOR_OPCODE, new PresentExtension());
        extensions.put(SyncExtension.MAJOR_OPCODE, new SyncExtension());
    }

    public <T extends Extension> T getExtension(int opcode) {
        return (T)extensions.get(opcode);
    }

    public synchronized void setGrabbed(boolean grabbed, XClient client) {
        this.isGrabbed = grabbed;
        this.grabbingClient = client;
    }

    public synchronized boolean isGrabbedBy(XClient client) {
        return isGrabbed && grabbingClient == client;
    }
}
