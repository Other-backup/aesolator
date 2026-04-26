package com.winlator.cmod.xenvironment.components;

import android.content.Context;

import androidx.annotation.Keep;

import com.winlator.cmod.contentdialog.VortekConfigDialog;
import com.winlator.cmod.contents.VortekVulkanDriverPackageManager;
import com.winlator.cmod.core.GPUHelper;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.NativeLibraryLoader;
import com.winlator.cmod.core.VortekExtensionPolicy;
import com.winlator.cmod.renderer.GPUImage;
import com.winlator.cmod.renderer.Texture;
import com.winlator.cmod.widget.XServerView;
import com.winlator.cmod.xconnector.Client;
import com.winlator.cmod.xconnector.ConnectionHandler;
import com.winlator.cmod.xconnector.RequestHandler;
import com.winlator.cmod.xconnector.UnixSocketConfig;
import com.winlator.cmod.xconnector.XConnectorEpoll;
import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xenvironment.EnvironmentComponent;
import com.winlator.cmod.xserver.Drawable;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.XServer;

import java.io.IOException;
import java.util.Objects;

public class VortekRendererComponent extends EnvironmentComponent implements ConnectionHandler, RequestHandler {
    private static final byte REQUEST_CODE_CREATE_CONTEXT = 1;
    private static final byte REQUEST_CODE_SEND_EXTRA_DATA = 2;
    public static final short IMAGE_CACHE_SIZE = 256;
    public static final int VK_MAX_VERSION = GPUHelper.vkMakeVersion(1, 4, 4095);

    private XConnectorEpoll connector;
    private final Options options;
    private final UnixSocketConfig socketConfig;
    private final XServer xServer;
    private final Context context;

    private native long createVkContext(int fd, Options options);

    private native void destroyVkContext(long contextPtr);

    private native boolean handleExtraDataRequest(long contextPtr, int requestId, int requestLength);

    private native void initVulkanWrapper(String nativeLibraryDir, String libvulkanPath);

    static {
        NativeLibraryLoader.ensureLoaded("vortekrenderer", "VortekRendererComponent");
    }

    public static class Options {
        public int vkMaxVersion = VK_MAX_VERSION;
        public short maxDeviceMemory = 0;
        public short imageCacheSize = IMAGE_CACHE_SIZE;
        public byte resourceMemoryType = 0;
        public String[] exposedDeviceExtensions = null;
        public String[] disabledDeviceExtensions = null;
        public String libvulkanPath = null;

        public static Options fromKeyValueSet(Context context, KeyValueSet config) {
            Options options = new Options();
            KeyValueSet safeConfig = config == null ? new KeyValueSet() : config;
            String defaultProfile = VortekExtensionPolicy.PROFILE_MALI_SYSTEM;
            String exposedDeviceExtensions = safeConfig.get("exposedDeviceExtensions");
            if (exposedDeviceExtensions.isEmpty()) {
                options.exposedDeviceExtensions = VortekExtensionPolicy.getSelectedExtensionsForProfile(
                        safeConfig.get("extensionProfile", defaultProfile),
                        VortekExtensionPolicy.buildCandidateExtensions(GPUHelper.vkGetDeviceExtensions())
                );
            } else if (!"all".equals(exposedDeviceExtensions)) {
                options.exposedDeviceExtensions = exposedDeviceExtensions.split("\\|");
            }
            String disabledDeviceExtensions = safeConfig.get("disabledDeviceExtensions");
            if (disabledDeviceExtensions.isEmpty()) {
                disabledDeviceExtensions = VortekExtensionPolicy.joinExtensions(
                        VortekExtensionPolicy.getDisabledExtensionsForProfile(safeConfig.get("extensionProfile", defaultProfile))
                );
            }
            if (!disabledDeviceExtensions.isEmpty()) {
                options.disabledDeviceExtensions = disabledDeviceExtensions.split("\\|");
            }

            String defaultVkMaxVersion = VortekConfigDialog.DEFAULT_VK_MAX_VERSION;
            String vkMaxVersion = safeConfig.get("vkMaxVersion", defaultVkMaxVersion);
            if (!vkMaxVersion.equals(defaultVkMaxVersion)) {
                String[] parts = vkMaxVersion.split("\\.");
                options.vkMaxVersion = GPUHelper.vkMakeVersion(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        4095
                );
            }

            options.maxDeviceMemory = (short) safeConfig.getInt("maxDeviceMemory");
            options.imageCacheSize = (short) safeConfig.getInt("imageCacheSize", IMAGE_CACHE_SIZE);
            options.resourceMemoryType = (byte) safeConfig.getInt("resourceMemoryType");
            VortekVulkanDriverPackageManager packageManager = new VortekVulkanDriverPackageManager(context);
            options.libvulkanPath = packageManager.resolveLibraryPath(
                    safeConfig.get("vulkanDriverEntry", VortekVulkanDriverPackageManager.SYSTEM_ENTRY)
            );
            return options;
        }
    }

    public VortekRendererComponent(XServer xServer, UnixSocketConfig socketConfig, Options options, Context context) {
        this.xServer = xServer;
        this.socketConfig = socketConfig;
        this.options = options;
        this.context = context;
        initVulkanWrapper(context.getApplicationInfo().nativeLibraryDir, options.libvulkanPath);
    }

    @Override
    public void start() {
        if (connector != null) return;
        connector = new XConnectorEpoll(socketConfig, this, this);
        connector.setInitialInputBufferCapacity(8);
        connector.setInitialOutputBufferCapacity(0);
        connector.start();
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.stop();
            connector = null;
        }
    }

    @Keep
    private int getWindowWidth(int windowId) {
        Window window = xServer.windowManager.getWindow(windowId);
        return window != null ? window.getWidth() : 0;
    }

    @Keep
    private int getWindowHeight(int windowId) {
        Window window = xServer.windowManager.getWindow(windowId);
        return window != null ? window.getHeight() : 0;
    }

    @Keep
    private long getWindowHardwareBuffer(int windowId, boolean useHALPixelFormatBGRA8888) {
        Window window = xServer.windowManager.getWindow(windowId);
        if (window == null) return 0L;

        Drawable drawable = window.getContent();
        Texture texture = drawable.getTexture();
        if (!(texture instanceof GPUImage)) {
            XServerView xServerView = xServer.getRenderer().xServerView;
            Objects.requireNonNull(texture);
            xServerView.queueEvent(() -> destroyTexture(texture));
            drawable.setTexture(new GPUImage(drawable.width, drawable.height, false, useHALPixelFormatBGRA8888));
        }
        return ((GPUImage) drawable.getTexture()).getHardwareBufferPtr();
    }

    @Keep
    private void updateWindowContent(int windowId) {
        Window window = xServer.windowManager.getWindow(windowId);
        if (window == null) return;

        Drawable drawable = window.getContent();
        synchronized (drawable.renderLock) {
            drawable.forceUpdate();
        }
    }

    @Override
    public void handleConnectionShutdown(Client client) {
        if (client.getTag() != null) {
            destroyVkContext((Long) client.getTag());
        }
    }

    @Override
    public void handleNewConnection(Client client) {
        client.createIOStreams();
    }

    @Override
    public boolean handleRequest(Client client) throws IOException {
        XInputStream inputStream = client.getInputStream();
        if (inputStream.available() < 8) return false;

        int requestCode = inputStream.readInt();
        int requestLength = inputStream.readInt();
        if (requestCode == REQUEST_CODE_CREATE_CONTEXT) {
            long contextPtr = createVkContext(client.clientSocket.fd, options);
            if (contextPtr > 0) {
                client.setTag(contextPtr);
            } else if (connector != null) {
                connector.killConnection(client);
            }
        } else if (requestCode > 32767 && (requestCode >> 16) == REQUEST_CODE_SEND_EXTRA_DATA) {
            int requestId = requestCode & 65535;
            boolean success = handleExtraDataRequest((Long) client.getTag(), requestId, requestLength);
            if (!success) {
                throw new IOException("Failed to handle extra data request.");
            }
        }
        return true;
    }

    public static void destroyTexture(Texture texture) {
        if (texture != null) {
            texture.destroy();
        }
    }
}
