package com.winlator.cmod.xenvironment.components;

import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.xenvironment.EnvironmentComponent;
import com.winlator.cmod.xconnector.XConnectorEpoll;
import com.winlator.cmod.xconnector.UnixSocketConfig;
import com.winlator.cmod.xserver.XClientConnectionHandler;
import com.winlator.cmod.xserver.XClientRequestHandler;
import com.winlator.cmod.xserver.XServer;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class XServerComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    private final ArrayList<XConnectorEpoll> aliasConnectors = new ArrayList<>();
    private final XServer xServer;
    private final UnixSocketConfig socketConfig;

    public XServerComponent(XServer xServer, UnixSocketConfig socketConfig) {
        this.xServer = xServer;
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        if (connector != null) return;
        connector = createConnector(socketConfig);
        connector.start();
        if (shouldExposeAbstractX11Socket(socketConfig)) {
            startX11AliasConnectors();
        }
    }

    @Override
    public void stop() {
        for (int i = aliasConnectors.size() - 1; i >= 0; i--) {
            aliasConnectors.get(i).stop();
        }
        aliasConnectors.clear();
        if (connector != null) {
            connector.stop();
            connector = null;
        }
    }

    public XServer getXServer() {
        return xServer;
    }

    private XConnectorEpoll createConnector(UnixSocketConfig config) {
        XConnectorEpoll newConnector = new XConnectorEpoll(config, new XClientConnectionHandler(xServer), new XClientRequestHandler());
        newConnector.setInitialInputBufferCapacity(262144);
        newConnector.setCanReceiveAncillaryMessages(true);
        newConnector.setMultithreadedClients(true);
        return newConnector;
    }

    private void startX11AliasConnectors() {
        for (UnixSocketConfig aliasConfig : buildX11AliasSocketConfigs()) {
            try {
                XConnectorEpoll aliasConnector = createConnector(aliasConfig);
                aliasConnector.start();
                aliasConnectors.add(aliasConnector);
                logXServerTransport(
                        aliasConfig.abstractNamespace
                                ? "XSERVER_ABSTRACT_TRANSPORT_READY"
                                : "XSERVER_PATH_ALIAS_TRANSPORT_READY",
                        aliasConfig.abstractNamespace
                                ? "x11_abstract_transport_ready"
                                : "x11_path_alias_transport_ready",
                        aliasConfig,
                        null
                );
            }
            catch (RuntimeException e) {
                logXServerTransport(
                        aliasConfig.abstractNamespace
                                ? "XSERVER_ABSTRACT_TRANSPORT_UNAVAILABLE"
                                : "XSERVER_PATH_ALIAS_TRANSPORT_UNAVAILABLE",
                        aliasConfig.abstractNamespace
                                ? "x11_abstract_transport_unavailable"
                                : "x11_path_alias_transport_unavailable",
                        aliasConfig,
                        e
                );
            }
        }
    }

    private ArrayList<UnixSocketConfig> buildX11AliasSocketConfigs() {
        ArrayList<UnixSocketConfig> result = new ArrayList<>();
        if (socketConfig == null || socketConfig.path == null) return result;

        String primaryGuestPath = socketConfig.guestPath != null && !socketConfig.guestPath.isEmpty()
                ? socketConfig.guestPath
                : socketConfig.path;
        String rootPath = resolveRootPathFromX11Socket(primaryGuestPath);
        LinkedHashSet<String> abstractPaths = new LinkedHashSet<>();
        abstractPaths.add(UnixSocketConfig.XSERVER_PATH);
        abstractPaths.add(primaryGuestPath);
        if (rootPath != null && !rootPath.isEmpty()) {
            String rootedUsrTmp = rootPath + "/usr/tmp/.X11-unix/X0";
            if (!primaryGuestPath.equals(rootedUsrTmp)) {
                result.add(UnixSocketConfig.createSocket(rootPath, "/usr/tmp/.X11-unix/X0"));
            }
            abstractPaths.add(rootedUsrTmp);
        }

        abstractPaths.add("/data/data/com.winlator/files/imagefs/usr/tmp/.X11-unix/X0");
        abstractPaths.add("/data/user/0/com.winlator/files/imagefs/usr/tmp/.X11-unix/X0");
        abstractPaths.add("/data/data/com.winlator/files/rootfs/tmp/.X11-unix/X0");
        abstractPaths.add("/data/user/0/com.winlator/files/rootfs/tmp/.X11-unix/X0");
        abstractPaths.add("/data/data/app.gamenative/files/imagefs/usr/tmp/.X11-unix/X0");
        abstractPaths.add("/data/user/0/app.gamenative/files/imagefs/usr/tmp/.X11-unix/X0");
        abstractPaths.add("/data/data/com.termux/files/usr/tmp/.X11-unix/X0");
        abstractPaths.add("/data/user/0/com.termux/files/usr/tmp/.X11-unix/X0");

        for (String abstractPath : abstractPaths) {
            if (abstractPath == null || abstractPath.trim().isEmpty()) continue;
            result.add(UnixSocketConfig.createAbstractSocket(abstractPath));
        }
        return result;
    }

    private static String resolveRootPathFromX11Socket(String path) {
        if (path == null) return "";
        String normalized = path.trim();
        String suffix = UnixSocketConfig.XSERVER_PATH;
        if (!normalized.endsWith(suffix)) return "";
        return normalized.substring(0, normalized.length() - suffix.length());
    }

    private static boolean shouldExposeAbstractX11Socket(UnixSocketConfig config) {
        return config != null
                && !config.abstractNamespace
                && config.path != null
                && config.path.endsWith("/.X11-unix/X0");
    }

    private static void logXServerTransport(String eventId, String message, Throwable error) {
        logXServerTransport(eventId, message, UnixSocketConfig.createAbstractSocket(UnixSocketConfig.XSERVER_PATH), error);
    }

    private static void logXServerTransport(String eventId, String message, UnixSocketConfig config, Throwable error) {
        String socketPath = config != null ? config.path : "";
        String guestSocketPath = config != null ? config.guestPath : "";
        String socketNamespace = config != null && config.abstractNamespace ? "abstract" : "pathname";
        boolean socketExists = !socketPath.isEmpty() && new File(socketPath).exists();
        if (error == null) {
            ForensicLogger.logEvent(
                    ForensicLogger.getAppContext(),
                    "info",
                    eventId,
                    null,
                    "xserver_transport",
                    message,
                    ForensicLogger.fields(
                            "pathname_socket", UnixSocketConfig.XSERVER_PATH,
                            "abstract_socket", UnixSocketConfig.XSERVER_PATH,
                            "socket_path", socketPath,
                            "guest_socket_path", guestSocketPath,
                            "socket_namespace", socketNamespace,
                            "socket_relocated", config != null && config.relocated,
                            "socket_exists", socketExists,
                            "contract", "x11_accepts_root_tmp_usr_tmp_and_known_donor_xcb_abstract_aliases"
                    )
            );
        }
        else {
            ForensicLogger.error(
                    ForensicLogger.getAppContext(),
                    eventId,
                    null,
                    "xserver_transport",
                    message,
                    error,
                    ForensicLogger.fields(
                            "pathname_socket", UnixSocketConfig.XSERVER_PATH,
                            "abstract_socket", UnixSocketConfig.XSERVER_PATH,
                            "socket_path", socketPath,
                            "guest_socket_path", guestSocketPath,
                            "socket_namespace", socketNamespace,
                            "socket_relocated", config != null && config.relocated,
                            "socket_exists", socketExists,
                            "contract", "x11_accepts_root_tmp_usr_tmp_and_known_donor_xcb_abstract_aliases"
                    )
            );
        }
    }
}
