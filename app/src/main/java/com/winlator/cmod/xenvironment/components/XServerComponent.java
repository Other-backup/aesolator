package com.winlator.cmod.xenvironment.components;

import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.xenvironment.EnvironmentComponent;
import com.winlator.cmod.xconnector.XConnectorEpoll;
import com.winlator.cmod.xconnector.UnixSocketConfig;
import com.winlator.cmod.xserver.XClientConnectionHandler;
import com.winlator.cmod.xserver.XClientRequestHandler;
import com.winlator.cmod.xserver.XServer;

public class XServerComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    private XConnectorEpoll abstractConnector;
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
            try {
                abstractConnector = createConnector(UnixSocketConfig.createAbstractSocket(UnixSocketConfig.XSERVER_PATH));
                abstractConnector.start();
                logXServerTransport("XSERVER_ABSTRACT_TRANSPORT_READY", "x11_abstract_transport_ready", null);
            }
            catch (RuntimeException e) {
                abstractConnector = null;
                logXServerTransport("XSERVER_ABSTRACT_TRANSPORT_UNAVAILABLE", "x11_abstract_transport_unavailable", e);
            }
        }
    }

    @Override
    public void stop() {
        if (abstractConnector != null) {
            abstractConnector.stop();
            abstractConnector = null;
        }
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

    private static boolean shouldExposeAbstractX11Socket(UnixSocketConfig config) {
        return config != null
                && !config.abstractNamespace
                && config.path != null
                && config.path.endsWith("/.X11-unix/X0");
    }

    private static void logXServerTransport(String eventId, String message, Throwable error) {
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
                            "abstract_socket", UnixSocketConfig.XSERVER_PATH
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
                            "abstract_socket", UnixSocketConfig.XSERVER_PATH
                    )
            );
        }
    }
}
