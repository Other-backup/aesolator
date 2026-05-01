package com.winlator.cmod.xenvironment.components;

import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.sysvshm.SysVSHMConnectionHandler;
import com.winlator.cmod.sysvshm.SysVSHMRequestHandler;
import com.winlator.cmod.sysvshm.SysVSharedMemory;
import com.winlator.cmod.xconnector.UnixSocketConfig;
import com.winlator.cmod.xconnector.XConnectorEpoll;
import com.winlator.cmod.xenvironment.EnvironmentComponent;
import com.winlator.cmod.xserver.SHMSegmentManager;
import com.winlator.cmod.xserver.XServer;

public class SysVSharedMemoryComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    public final UnixSocketConfig socketConfig;
    private SysVSharedMemory sysVSharedMemory;
    private final XServer xServer;

    public SysVSharedMemoryComponent(XServer xServer, UnixSocketConfig socketConfig) {
        this.xServer = xServer;
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        if (connector != null) return;
        sysVSharedMemory = new SysVSharedMemory();
        xServer.setSHMSegmentManager(new SHMSegmentManager(sysVSharedMemory));
        try {
            connector = new XConnectorEpoll(socketConfig, new SysVSHMConnectionHandler(sysVSharedMemory), new SysVSHMRequestHandler());
            connector.start();
            logSysvshmEvent("SYSVSHM_CONNECTOR_READY", "sysvshm_connector_ready", null);
        }
        catch (RuntimeException error) {
            connector = null;
            logSysvshmEvent("SYSVSHM_CONNECTOR_UNAVAILABLE", "sysvshm_connector_unavailable", error);
        }
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.stop();
            connector = null;
        }

        if (sysVSharedMemory != null) {
            sysVSharedMemory.deleteAll();
            sysVSharedMemory = null;
        }
    }

    private void logSysvshmEvent(String eventId, String message, Throwable error) {
        if (error == null) {
            ForensicLogger.logEvent(
                    ForensicLogger.getAppContext(),
                    "info",
                    eventId,
                    null,
                    "sysvshm",
                    message,
                    ForensicLogger.fields(
                            "socket_path", socketConfig != null ? socketConfig.path : "",
                            "guest_socket_path", socketConfig != null ? socketConfig.guestPath : "",
                            "socket_namespace", socketConfig != null && socketConfig.abstractNamespace ? "abstract" : "pathname",
                            "socket_relocated", socketConfig != null && socketConfig.relocated,
                            "shm_manager_ready", xServer.getSHMSegmentManager() != null
                    )
            );
            return;
        }

        ForensicLogger.error(
                ForensicLogger.getAppContext(),
                eventId,
                null,
                "sysvshm",
                message,
                error,
                ForensicLogger.fields(
                        "socket_path", socketConfig != null ? socketConfig.path : "",
                        "guest_socket_path", socketConfig != null ? socketConfig.guestPath : "",
                        "socket_namespace", socketConfig != null && socketConfig.abstractNamespace ? "abstract" : "pathname",
                        "socket_relocated", socketConfig != null && socketConfig.relocated,
                        "shm_manager_ready", xServer.getSHMSegmentManager() != null,
                        "degraded_mode", "mit_shm_requests_return_bad_segment_without_host_crash"
                )
        );
    }
}
