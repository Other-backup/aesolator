package com.winlator.cmod.xconnector;

import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Keep;

import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.WinlatorNative;

import java.io.IOException;

public class XConnectorEpoll implements Runnable {
    private static final String TAG = "XConnectorEpoll";
    private static final long STOP_JOIN_TIMEOUT_MS = 1500;
    private static final long STOP_JOIN_SLICE_MS = 50;
    private final ConnectionHandler connectionHandler;
    private final RequestHandler requestHandler;
    private final int epollFd;
    private final int serverFd;
    private final int shutdownFd;
    private final String socketPath;
    private final boolean socketAbstractNamespace;
    private long acceptedConnectionCount = 0;
    private Thread epollThread;
    private boolean running = false;
    private boolean multithreadedClients = false;
    private boolean canReceiveAncillaryMessages = false;
    private int initialInputBufferCapacity = 4096;
    private int initialOutputBufferCapacity = 4096;
    private final SparseArray<Client> connectedClients = new SparseArray<>();

    static {
        WinlatorNative.ensureLoaded("XConnectorEpoll");
    }

    public XConnectorEpoll(UnixSocketConfig socketConfig, ConnectionHandler connectionHandler, RequestHandler requestHandler) {
        this.connectionHandler = connectionHandler;
        this.requestHandler = requestHandler;
        this.socketPath = socketConfig != null ? socketConfig.path : "";
        this.socketAbstractNamespace = socketConfig != null && socketConfig.abstractNamespace;
        setRLimitToMax();

        serverFd = createAFUnixSocket(socketPath, socketAbstractNamespace);
        if (serverFd < 0) {
            throw new RuntimeException("Failed to create an AF_UNIX socket.");
        }

        epollFd = createEpollFd();
        if (epollFd < 0) {
            closeFd(serverFd);
            throw new RuntimeException("Failed to create epoll fd.");
        }

        if (!addFdToEpoll(epollFd, serverFd)) {
            closeFd(serverFd);
            closeFd(epollFd);
            throw new RuntimeException("Failed to add server fd to epoll.");
        }

        shutdownFd = createEventFd();
        if (!addFdToEpoll(epollFd, shutdownFd)) {
            closeFd(serverFd);
            closeFd(shutdownFd);
            closeFd(epollFd);
            throw new RuntimeException("Failed to add shutdown fd to epoll.");
        }

        epollThread = new Thread(this);
        epollThread.setName("XConnectorEpoll-" + (socketAbstractNamespace ? "abstract-" : "path-") + sanitizeSocketPathForThread(socketPath));
        logConnectorEvent(
                "XCONNECTOR_SOCKET_READY",
                "xconnector_socket_ready",
                "server_fd", serverFd,
                "epoll_fd", epollFd,
                "shutdown_fd", shutdownFd,
                "socket_namespace", socketAbstractNamespace ? "abstract" : "pathname",
                "thread_name", epollThread.getName()
        );
    }

    public synchronized void start() {
        if (running || epollThread == null) return;
        running = true;
        logConnectorEvent(
                "XCONNECTOR_THREAD_START",
                "xconnector_thread_start",
                "server_fd", serverFd,
                "epoll_fd", epollFd,
                "active_clients", connectedClients.size()
        );
        epollThread.start();
    }

    public synchronized void stop() {
        Thread thread = epollThread;
        if (!running || thread == null) return;
        running = false;
        requestShutdown();
        logConnectorEvent(
                "XCONNECTOR_THREAD_STOP_REQUESTED",
                "xconnector_thread_stop_requested",
                "active_clients", connectedClients.size(),
                "accepted_connection_count", acceptedConnectionCount
        );

        if (thread == Thread.currentThread()) {
            epollThread = null;
            return;
        }

        boolean interrupted = false;
        long deadline = System.currentTimeMillis() + STOP_JOIN_TIMEOUT_MS;
        while (thread.isAlive()) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                Log.w(TAG, "Timed out waiting for epoll thread to stop; continuing teardown asynchronously");
                break;
            }
            try {
                thread.join(Math.min(remaining, STOP_JOIN_SLICE_MS));
            }
            catch (InterruptedException e) {
                interrupted = true;
            }
        }
        epollThread = null;
        if (interrupted) Thread.currentThread().interrupt();
    }

    @Override
    public void run() {
        while (running && doEpollIndefinitely(epollFd, serverFd, !multithreadedClients));
        shutdown();
    }

    @Keep
    private void handleNewConnection(int fd) {
        acceptedConnectionCount++;
        logConnectorEvent(
                "XCONNECTOR_CLIENT_ACCEPTED",
                "xconnector_client_accepted",
                "client_fd", fd,
                "active_clients_before", connectedClients.size(),
                "accepted_connection_count", acceptedConnectionCount,
                "multithreaded_clients", multithreadedClients,
                "can_receive_ancillary_messages", canReceiveAncillaryMessages,
                "initial_input_buffer_capacity", initialInputBufferCapacity,
                "initial_output_buffer_capacity", initialOutputBufferCapacity
        );
        final Client client = new Client(this, new ClientSocket(fd));
        client.connected = true;
        if (multithreadedClients) {
            client.shutdownFd = createEventFd();
            client.pollThread = new Thread(() -> {
                connectionHandler.handleNewConnection(client);
                while (client.connected && waitForSocketRead(client.clientSocket.fd, client.shutdownFd));
            });
            client.pollThread.start();
        }
        else connectionHandler.handleNewConnection(client);
        connectedClients.put(fd, client);
        logConnectorEvent(
                "XCONNECTOR_CLIENT_REGISTERED",
                "xconnector_client_registered",
                "client_fd", fd,
                "active_clients", connectedClients.size(),
                "accepted_connection_count", acceptedConnectionCount
        );
    }

    @Keep
    private void handleExistingConnection(int fd) {
        Client client = connectedClients.get(fd);
        if (client == null) return;

        XInputStream inputStream = client.getInputStream();
        try {
            if (inputStream != null) {
                if (inputStream.readMoreData(canReceiveAncillaryMessages) > 0) {
                    int activePosition = 0;
                    while (running && requestHandler.handleRequest(client)) activePosition = inputStream.getActivePosition();
                    inputStream.setActivePosition(activePosition);
                }
                else killConnection(client);
            }
            else requestHandler.handleRequest(client);
        }
        catch (IOException e) {
            killConnection(client);
        }
    }

    public Client getClient(int fd) {
        return connectedClients.get(fd);
    }

    public void killConnection(Client client) {
        int clientFd = client != null && client.clientSocket != null ? client.clientSocket.fd : -1;
        logConnectorEvent(
                "XCONNECTOR_CLIENT_SHUTDOWN",
                "xconnector_client_shutdown",
                "client_fd", clientFd,
                "active_clients_before", connectedClients.size(),
                "accepted_connection_count", acceptedConnectionCount
        );
        client.connected = false;
        connectionHandler.handleConnectionShutdown(client);
        if (multithreadedClients) {
            if (Thread.currentThread() != client.pollThread) {
                client.requestShutdown();

                while (client.pollThread.isAlive()) {
                    try {
                        client.pollThread.join();
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Log.w(TAG, "Interrupted while waiting for client poll thread shutdown", e);
                        break;
                    }
                }

                if (!client.pollThread.isAlive()) client.pollThread = null;
            }
            closeFd(client.shutdownFd);
        }
        else removeFdFromEpoll(epollFd, client.clientSocket.fd);
        closeFd(client.clientSocket.fd);
        connectedClients.remove(client.clientSocket.fd);
    }

    private void shutdown() {
        logConnectorEvent(
                "XCONNECTOR_SHUTDOWN_BEGIN",
                "xconnector_shutdown_begin",
                "active_clients", connectedClients.size(),
                "accepted_connection_count", acceptedConnectionCount
        );
        while (connectedClients.size() > 0) {
            Client client = connectedClients.valueAt(connectedClients.size()-1);
            killConnection(client);
        }

        removeFdFromEpoll(epollFd, serverFd);
        removeFdFromEpoll(epollFd, shutdownFd);
        closeFd(serverFd);
        closeFd(shutdownFd);
        closeFd(epollFd);
        logConnectorEvent(
                "XCONNECTOR_SHUTDOWN_DONE",
                "xconnector_shutdown_done",
                "active_clients", connectedClients.size(),
                "accepted_connection_count", acceptedConnectionCount
        );
    }

    private void logConnectorEvent(String eventId, String message, Object... fields) {
        Object[] base = new Object[fields.length + 6];
        base[0] = "socket_path";
        base[1] = socketPath;
        base[2] = "socket_role";
        base[3] = classifySocketRole(socketPath);
        base[4] = "socket_namespace";
        base[5] = socketAbstractNamespace ? "abstract" : "pathname";
        System.arraycopy(fields, 0, base, 6, fields.length);
        ForensicLogger.logEvent(
                ForensicLogger.getAppContext(),
                "info",
                eventId,
                null,
                "xconnector",
                message,
                ForensicLogger.fields(base)
        );
    }

    private static String classifySocketRole(String path) {
        if (path == null) return "unknown";
        if (path.endsWith("/.X11-unix/X0")) return "x11";
        if (path.endsWith("/.sysvshm/SM0")) return "sysvshm";
        if (path.endsWith("/.sound/AS0")) return "alsa";
        if (path.endsWith("/.sound/PS0")) return "pulseaudio";
        if (path.endsWith("/.virgl/V0")) return "virgl";
        if (path.endsWith("/.vortek/V0")) return "vortek";
        if (path.endsWith("/.steam/steam_pipe")) return "steam";
        return "other";
    }

    private static String sanitizeSocketPathForThread(String path) {
        if (path == null || path.trim().isEmpty()) return "unknown";
        String normalized = path.trim();
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < normalized.length()) normalized = normalized.substring(slash + 1);
        normalized = normalized.replaceAll("[^A-Za-z0-9._-]", "_");
        return normalized.isEmpty() ? "unknown" : normalized;
    }

    public int getInitialInputBufferCapacity() {
        return initialInputBufferCapacity;
    }

    public void setInitialInputBufferCapacity(int initialInputBufferCapacity) {
        this.initialInputBufferCapacity = initialInputBufferCapacity;
    }

    public int getInitialOutputBufferCapacity() {
        return initialOutputBufferCapacity;
    }

    public void setInitialOutputBufferCapacity(int initialOutputBufferCapacity) {
        this.initialOutputBufferCapacity = initialOutputBufferCapacity;
    }

    public boolean isMultithreadedClients() {
        return multithreadedClients;
    }

    public void setMultithreadedClients(boolean multithreadedClients) {
        this.multithreadedClients = multithreadedClients;
    }

    public boolean isCanReceiveAncillaryMessages() {
        return canReceiveAncillaryMessages;
    }

    public void setCanReceiveAncillaryMessages(boolean canReceiveAncillaryMessages) {
        this.canReceiveAncillaryMessages = canReceiveAncillaryMessages;
    }

    private void requestShutdown() {
        if (!signalFd(shutdownFd)) {
            Log.w(TAG, "Failed to signal shutdown event fd");
        }
    }

    public static native void closeFd(int fd);

    private static native void setRLimitToMax();

    static native boolean signalFd(int fd);

    private native int createEpollFd();

    private native int createEventFd();

    private native boolean doEpollIndefinitely(int epollFd, int serverFd, boolean addClientToEpoll);

    private native boolean addFdToEpoll(int epollFd, int fd);

    private native void removeFdFromEpoll(int epollFd, int fd);

    private native boolean waitForSocketRead(int clientFd, int shutdownFd);

    private native int createAFUnixSocket(String path, boolean abstractNamespace);
}
