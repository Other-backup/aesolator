package com.winlator.cmod.xserver;

import android.graphics.Rect;
import android.util.SparseArray;

import com.winlator.cmod.core.CursorLocker;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.math.Mathf;
import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.winhandler.WinHandler;
import com.winlator.cmod.xserver.extensions.BigReqExtension;
import com.winlator.cmod.xserver.extensions.DRI3Extension;
import com.winlator.cmod.xserver.extensions.Extension;
import com.winlator.cmod.xserver.extensions.GenericEventExtension;
import com.winlator.cmod.xserver.extensions.GLXExtension;
import com.winlator.cmod.xserver.extensions.PresentExtension;
import com.winlator.cmod.xserver.extensions.ShapeExtension;
import com.winlator.cmod.xserver.extensions.SyncExtension;
import com.winlator.cmod.xserver.extensions.XComposite;
import com.winlator.cmod.xserver.extensions.XInput2Extension;
import com.winlator.cmod.xserver.extensions.XKeyboardExtension;
import com.winlator.cmod.xserver.extensions.XineramaExtension;

import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.concurrent.locks.ReentrantLock;

public class XServer {
    public enum Lockable {WINDOW_MANAGER, PIXMAP_MANAGER, DRAWABLE_MANAGER, GRAPHIC_CONTEXT_MANAGER, INPUT_DEVICE, CURSOR_MANAGER, SHMSEGMENT_MANAGER}
    public static final short VERSION = 11;
    public static final String VENDOR_NAME = "Elbrus Technologies, LLC";
    public static final int MAX_CLIENTS = 4096;
    public static final Charset LATIN1_CHARSET = Charset.forName("latin1");
    public final SparseArray<Extension> extensions = new SparseArray<>();
    public final ScreenInfo screenInfo;
    public final PixmapManager pixmapManager;
    public final ResourceIDs resourceIDs = new ResourceIDs(MAX_CLIENTS);
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
    private int nextExtensionEventId = 64;
    private int nextExtensionErrorId = 128;

    public XServer(ScreenInfo screenInfo) {
        this.screenInfo = screenInfo;
        cursorLocker = new CursorLocker(this);
        for (Lockable lockable : Lockable.values()) locks.put(lockable, new ReentrantLock());

        pixmapManager = new PixmapManager();
        drawableManager = new DrawableManager(this);
        cursorManager = new CursorManager(drawableManager);
        windowManager = new WindowManager(screenInfo, drawableManager);
        selectionManager = new SelectionManager(windowManager);
        inputDeviceManager = new InputDeviceManager(this);
        grabManager = new GrabManager(this);

        DesktopHelper.attachTo(this);
        setupExtensions();
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
            this.lockables = lockables;
            for (Lockable lockable : lockables) locks.get(lockable).lock();
        }

        @Override
        public void close() {
            for (int i = lockables.length - 1; i >= 0; i--) {
                locks.get(lockables[i]).unlock();
            }
        }
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
            int minX = 0;
            int minY = 0;
            int maxX = screenInfo.width - 1;
            int maxY = screenInfo.height - 1;
            short clampedX;
            short clampedY;

            Rect confinement = grabManager.getConfinementBounds();
            if (confinement != null) {
                minX = Math.max(minX, confinement.left);
                minY = Math.max(minY, confinement.top);
                maxX = Math.min(maxX, confinement.right - 1);
                maxY = Math.min(maxY, confinement.bottom - 1);
                if (maxX < minX) maxX = minX;
                if (maxY < minY) maxY = minY;
                clampedX = (short)Mathf.clamp(pointer.getX() + dx, minX, maxX);
                clampedY = (short)Mathf.clamp(pointer.getY() + dy, minY, maxY);
                pointer.setPosition(clampedX, clampedY);
            }
            else {
                short softMarginX = (short)(screenInfo.width * 0.05f);
                short softMarginY = (short)(screenInfo.height * 0.05f);
                short x = (short)Mathf.clamp(pointer.getX() + dx, -softMarginX, screenInfo.width - 1 + softMarginX);
                short y = (short)Mathf.clamp(pointer.getY() + dy, -softMarginY, screenInfo.height - 1 + softMarginY);
                pointer.setPosition(x, y);
            }

            XInput2Extension xInput2 = getExtension(XInput2Extension.MAJOR_OPCODE, XInput2Extension.class);
            if (xInput2 != null) xInput2.emitRawMotion(2, dx, dy);
        }
    }

    public void injectPointerButtonPress(Pointer.Button buttonCode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setButton(buttonCode, true);
            XInput2Extension xInput2 = getExtension(XInput2Extension.MAJOR_OPCODE, XInput2Extension.class);
            if (xInput2 != null) xInput2.emitRawButton(2, buttonCode.code(), true);
        }
    }

    public void injectPointerButtonRelease(Pointer.Button buttonCode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setButton(buttonCode, false);
            XInput2Extension xInput2 = getExtension(XInput2Extension.MAJOR_OPCODE, XInput2Extension.class);
            if (xInput2 != null) xInput2.emitRawButton(2, buttonCode.code(), false);
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
        registerExtension(new BigReqExtension());
        registerExtension(new DRI3Extension());
        registerExtension(new PresentExtension());
        registerExtension(new SyncExtension());
        registerExtension(new ShapeExtension(this));
        registerExtension(new XComposite(this));
        registerExtension(new GenericEventExtension());
        registerExtension(new XKeyboardExtension());
        registerExtension(new XineramaExtension(this));
        registerGlxExtension();
        registerExtension(new XInput2Extension());
        logExtensionSkipped("MIT-SHM", "disabled_until_abi_compatible_client_bridge");
    }

    private void registerExtension(Extension extension) {
        assignExtensionRanges(extension);
        extensions.put(extension.getMajorOpcode(), extension);
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XSERVER_EXTENSION_REGISTERED",
                null,
                "xserver_extensions",
                "x11_extension_registered",
                ForensicLogger.fields(
                        "extension_name", extension.getName(),
                        "major_opcode", Byte.toUnsignedInt(extension.getMajorOpcode()),
                        "first_event_id", Byte.toUnsignedInt(extension.getFirstEventId()),
                        "num_events", extension.getNumEvents(),
                        "first_error_id", Byte.toUnsignedInt(extension.getFirstErrorId()),
                        "num_errors", extension.getNumErrors()
                )
        );
    }

    private void assignExtensionRanges(Extension extension) {
        int numEvents = extension.getNumEvents();
        if (numEvents > 0) {
            int firstEventId = Byte.toUnsignedInt(extension.getFirstEventId());
            if (firstEventId == 0) {
                firstEventId = nextExtensionEventId;
                extension.setFirstEventId((byte)firstEventId);
            }
            nextExtensionEventId = Math.max(nextExtensionEventId, firstEventId + numEvents);
        }

        int numErrors = extension.getNumErrors();
        if (numErrors > 0) {
            int firstErrorId = Byte.toUnsignedInt(extension.getFirstErrorId());
            if (firstErrorId == 0) {
                firstErrorId = nextExtensionErrorId;
                extension.setFirstErrorId((byte)firstErrorId);
            }
            nextExtensionErrorId = Math.max(nextExtensionErrorId, firstErrorId + numErrors);
        }
    }

    private void registerGlxExtension() {
        try {
            registerExtension(new GLXExtension(this));
        }
        catch (Throwable error) {
            ForensicLogger.error(
                    ForensicLogger.getAppContext(),
                    "XSERVER_EXTENSION_REGISTER_FAILED",
                    null,
                    "xserver_extensions",
                    "x11_extension_register_failed",
                    error,
                    ForensicLogger.fields("extension_name", "GLX")
            );
        }
    }

    private void logExtensionSkipped(String name, String reason) {
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                "XSERVER_EXTENSION_REGISTER_SKIPPED",
                null,
                "xserver_extensions",
                "x11_extension_register_skipped",
                ForensicLogger.fields(
                        "extension_name", name,
                        "reason", reason
                )
        );
    }

    public Extension getExtension(int opcode) {
        return extensions.get(opcode);
    }

    public <T extends Extension> T getExtension(int opcode, Class<T> extensionType) {
        Extension extension = getExtension(opcode);
        return extensionType != null && extensionType.isInstance(extension)
                ? extensionType.cast(extension)
                : null;
    }

    public synchronized void setGrabbed(boolean grabbed, XClient client) {
        this.isGrabbed = grabbed;
        this.grabbingClient = client;
    }

    public synchronized boolean isGrabbedBy(XClient client) {
        return isGrabbed && grabbingClient == client;
    }
}
