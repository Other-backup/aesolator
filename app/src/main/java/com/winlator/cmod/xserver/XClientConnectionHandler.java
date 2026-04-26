package com.winlator.cmod.xserver;

import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.xconnector.Client;
import com.winlator.cmod.xconnector.ConnectionHandler;

public class XClientConnectionHandler implements ConnectionHandler {
    private final XServer xServer;

    public XClientConnectionHandler(XServer xServer) {
        this.xServer = xServer;
    }

    @Override
    public void handleNewConnection(Client client) {
        client.createIOStreams();
        XClient xClient = new XClient(xServer, client.getInputStream(), client.getOutputStream());
        client.setTag(xClient);
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                xClient.hasValidResourceIdBase() ? "info" : "error",
                xClient.hasValidResourceIdBase() ? "XSERVER_CLIENT_CONTEXT_CREATED" : "XSERVER_CLIENT_RESOURCE_EXHAUSTED",
                null,
                "xserver_protocol",
                xClient.hasValidResourceIdBase() ? "xserver_client_context_created" : "xserver_client_resource_exhausted",
                ForensicLogger.fields(
                        "client_fd", client.clientSocket != null ? client.clientSocket.fd : -1,
                        "resource_id_base", xClient.resourceIDBase,
                        "resource_max_clients", xServer.resourceIDs.maxClients,
                        "resource_allocated_count", xServer.resourceIDs.allocatedCount(),
                        "resource_available_count", xServer.resourceIDs.availableCount()
                )
        );
    }

    @Override
    public void handleConnectionShutdown(Client client) {
        Object tag = client != null ? client.getTag() : null;
        if (tag instanceof XClient) {
            ((XClient)tag).freeResources();
            ForensicLogger.logEvent(
                    ForensicLogger.getAppContext(),
                    "info",
                    "XSERVER_CLIENT_CONTEXT_RELEASED",
                    null,
                    "xserver_protocol",
                    "xserver_client_context_released",
                    ForensicLogger.fields(
                            "client_fd", client.clientSocket != null ? client.clientSocket.fd : -1,
                            "resource_id_base", ((XClient)tag).resourceIDBase,
                            "resource_allocated_count", xServer.resourceIDs.allocatedCount(),
                            "resource_available_count", xServer.resourceIDs.availableCount()
                    )
            );
        }
        else {
            ForensicLogger.logEvent(
                    ForensicLogger.getAppContext(),
                    "warn",
                    "XSERVER_CLIENT_CONTEXT_MISSING_ON_SHUTDOWN",
                    null,
                    "xserver_protocol",
                    "xserver_client_context_missing_on_shutdown",
                    ForensicLogger.fields("client_fd", client != null && client.clientSocket != null ? client.clientSocket.fd : -1)
            );
        }
    }
}
