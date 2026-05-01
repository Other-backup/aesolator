package com.winlator.cmod.winhandler;

import static com.winlator.cmod.inputcontrols.ExternalController.TRIGGER_IS_AXIS;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.input.InputManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.XServerDisplayActivity;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.inputcontrols.ControllerManager;
import com.winlator.cmod.inputcontrols.ControlsProfile;
import com.winlator.cmod.inputcontrols.ExternalController;
import com.winlator.cmod.inputcontrols.FakeInputWriter;
import com.winlator.cmod.inputcontrols.GamepadState;
import com.winlator.cmod.math.Mathf;
import com.winlator.cmod.xserver.XServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WinHandler {
    private static final String TAG = "WinHandler";
    private static final short SERVER_PORT = 7947;
    private static final short CLIENT_PORT = 7946;
    private static final int SEND_PACKET_SIZE = 64;
    private static final int RECEIVE_PACKET_SIZE = 2048;
    private static final int BRING_TO_FRONT_NAME_BYTES = SEND_PACKET_SIZE - 1 - Integer.BYTES - Long.BYTES;
    private static final int MAX_CONTROLLERS = 4;
    private static final int OSC_DEVICE_ID = -1;
    private static final Object VIBRATION_SOCKET_LOCK = new Object();
    private static WinHandler activeVibrationOwner;
    private static LocalServerSocket activeVibrationServer;

    public static final byte FLAG_DINPUT_MAPPER_STANDARD = 0x01;
    public static final byte FLAG_DINPUT_MAPPER_XINPUT = 0x02;
    public static final byte FLAG_INPUT_TYPE_XINPUT = 0x04;
    public static final byte FLAG_INPUT_TYPE_DINPUT = 0x08;
    public static final byte DEFAULT_INPUT_TYPE = FLAG_INPUT_TYPE_XINPUT;
    public static final byte INPUT_TYPE_MIXED = 2;
    public static final byte DINPUT_MAPPER_TYPE_STANDARD = 0;
    public static final byte DINPUT_MAPPER_TYPE_XINPUT = 1;

    private final XServerDisplayActivity activity;
    private final ByteBuffer sendData = ByteBuffer.allocate(SEND_PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    private final ByteBuffer receiveData = ByteBuffer.allocate(RECEIVE_PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    private final DatagramPacket sendPacket = new DatagramPacket(sendData.array(), SEND_PACKET_SIZE);
    private final DatagramPacket receivePacket = new DatagramPacket(receiveData.array(), RECEIVE_PACKET_SIZE);
    private final ArrayDeque<Runnable> actions = new ArrayDeque<>();
    private final List<Integer> gamepadClients = new CopyOnWriteArrayList<>();
    private final InputManager inputManager;
    private final Map<Integer, ExternalController> controllers = new HashMap<>();
    private final FakeInputWriter[] fakeInputWriters = new FakeInputWriter[MAX_CONTROLLERS];
    private final Map<Integer, Integer> deviceToSlot = new HashMap<>();
    private final Map<String, Integer> descriptorToSlot = new HashMap<>();
    private final Map<Integer, String> deviceToDescriptor = new HashMap<>();
    private final Set<Integer> usedSlots = new HashSet<>();
    private final boolean[] vibrationEnabledSlots = new boolean[MAX_CONTROLLERS];
    private final GamepadState outputGamepadState = new GamepadState();
    private final InputManager.InputDeviceListener inputDeviceListener = new InputManager.InputDeviceListener() {
        @Override
        public void onInputDeviceAdded(int deviceId) {
            assignConnectedDeviceIfPossible(deviceId, "hotplug");
        }

        @Override
        public void onInputDeviceRemoved(int deviceId) {
            releaseSlot(deviceId);
            controllers.remove(deviceId);
            if (currentController != null && currentController.getDeviceId() == deviceId) {
                currentController = null;
            }
        }

        @Override
        public void onInputDeviceChanged(int deviceId) {
            assignConnectedDeviceIfPossible(deviceId, "change");
        }
    };

    private DatagramSocket socket;
    private boolean initReceived = false;
    private boolean running = false;
    private OnGetProcessInfoListener onGetProcessInfoListener;
    private ExternalController currentController;
    private InetAddress localhost;
    private byte inputType = DEFAULT_INPUT_TYPE;
    private SharedPreferences preferences;
    private byte triggerType;
    private boolean xinputDisabled;
    private boolean xinputDisabledInitialized = false;
    private boolean useLegacyInputMethod = false;
    private byte dinputMapperType = DINPUT_MAPPER_TYPE_XINPUT;

    private float gyroX = 0;
    private float gyroY = 0;
    private float gyroSensitivityX = 0.35f;
    private float gyroSensitivityY = 0.25f;
    private float smoothingFactor = 0.45f;
    private boolean invertGyroX = true;
    private boolean invertGyroY = false;
    private float gyroDeadzone = 0.01f;
    private float smoothGyroX = 0;
    private float smoothGyroY = 0;
    private boolean processGyroWithLeftTrigger = false;
    private int gyroTriggerButton;
    private boolean isGyroActive = false;
    private boolean isToggleMode;

    private String fakeInputBasePath;
    private LocalServerSocket vibrationServer;
    private volatile boolean vibrationRunning = false;
    private volatile boolean vibrationSuperseded = false;
    private boolean globalVibrationEnabled = true;
    private int fallbackSlot = -1;

    public WinHandler(XServerDisplayActivity activity) {
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
        this.inputManager = (InputManager) activity.getSystemService(Context.INPUT_SERVICE);
        if (inputManager != null) {
            inputManager.registerInputDeviceListener(inputDeviceListener, null);
        }
        this.triggerType = (byte) preferences.getInt("trigger_type", TRIGGER_IS_AXIS);
        for (int slot = 0; slot < MAX_CONTROLLERS; slot++) {
            String key = "vibration_slot_" + slot;
            String legacyKey = "vibrate_slot_" + slot;
            if (preferences.contains(key)) {
                vibrationEnabledSlots[slot] = preferences.getBoolean(key, true);
            } else {
                vibrationEnabledSlots[slot] = preferences.getBoolean(legacyKey, true);
            }
        }
        globalVibrationEnabled = preferences.getBoolean(ControllerManager.PREF_VIBRATION_GLOBAL, true);
    }

    public void setGyroSensitivityX(float sensitivity) {
        this.gyroSensitivityX = sensitivity;
    }

    public void setGyroSensitivityY(float sensitivity) {
        this.gyroSensitivityY = sensitivity;
    }

    public void setSmoothingFactor(float factor) {
        this.smoothingFactor = factor;
    }

    public void setInvertGyroX(boolean invert) {
        this.invertGyroX = invert;
    }

    public void setInvertGyroY(boolean invert) {
        this.invertGyroY = invert;
    }

    public void setGyroDeadzone(float deadzone) {
        this.gyroDeadzone = deadzone;
    }

    private boolean isLeftTriggerPressed() {
        return currentController != null && currentController.state.triggerL > 0.5f;
    }

    public void updateGyroData(float rawGyroX, float rawGyroY) {
        if (!preferences.getBoolean("gyro_enabled", false)) {
            return;
        }

        if (processGyroWithLeftTrigger && !isLeftTriggerPressed()) {
            return;
        }

        if (!isGyroActive) {
            return;
        }

        if (Math.abs(rawGyroX) < gyroDeadzone) rawGyroX = 0;
        if (Math.abs(rawGyroY) < gyroDeadzone) rawGyroY = 0;
        if (invertGyroX) rawGyroX = -rawGyroX;
        if (invertGyroY) rawGyroY = -rawGyroY;

        float sensitivityMultiplier = 0.25f;
        rawGyroX *= gyroSensitivityX * sensitivityMultiplier;
        rawGyroY *= gyroSensitivityY * sensitivityMultiplier;

        smoothGyroX = smoothGyroX * smoothingFactor + rawGyroX * (1 - smoothingFactor);
        smoothGyroY = smoothGyroY * smoothingFactor + rawGyroY * (1 - smoothingFactor);

        smoothGyroX = Mathf.clamp(smoothGyroX, -0.25f, 0.25f);
        smoothGyroY = Mathf.clamp(smoothGyroY, -0.25f, 0.25f);

        this.gyroX = smoothGyroX;
        this.gyroY = smoothGyroY;
        sendGamepadState();
    }

    private boolean sendPacket(int port) {
        return sendPacket(port, sendData);
    }

    private boolean sendPacket(int port, ByteBuffer buffer) {
        try {
            int size = buffer.position();
            if (size == 0 || socket == null || localhost == null) return false;
            if (buffer == sendData) {
                sendPacket.setData(sendData.array(), 0, size);
                sendPacket.setAddress(localhost);
                sendPacket.setPort(port);
                socket.send(sendPacket);
            } else {
                DatagramPacket packet = new DatagramPacket(buffer.array(), size, localhost, port);
                socket.send(packet);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void exec(String command) {
        command = command.trim();
        if (command.isEmpty()) return;

        String[] parsed = splitCommand(command);
        String filename = parsed[0];
        String parameters = parsed[1];

        addAction(() -> {
            byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
            byte[] parametersBytes = parameters.getBytes(StandardCharsets.UTF_8);
            int packetSize = 1 + (Integer.BYTES * 3) + filenameBytes.length + parametersBytes.length;
            ByteBuffer packetBuffer = packetSize <= SEND_PACKET_SIZE
                    ? sendData
                    : ByteBuffer.allocate(packetSize).order(ByteOrder.LITTLE_ENDIAN);

            packetBuffer.clear();
            packetBuffer.put(RequestCodes.EXEC);
            packetBuffer.putInt(filenameBytes.length + parametersBytes.length + 8);
            packetBuffer.putInt(filenameBytes.length);
            packetBuffer.putInt(parametersBytes.length);
            packetBuffer.put(filenameBytes);
            packetBuffer.put(parametersBytes);
            sendPacket(CLIENT_PORT, packetBuffer);
        });
    }

    static String[] splitCommand(String command) {
        String trimmed = command != null ? command.trim() : "";
        if (trimmed.isEmpty()) return new String[] {"", ""};

        if (trimmed.charAt(0) == '"') {
            int closingQuote = findClosingQuote(trimmed, 1);
            if (closingQuote > 1) {
                String filename = trimmed.substring(1, closingQuote);
                String parameters = closingQuote + 1 < trimmed.length()
                        ? trimmed.substring(closingQuote + 1).trim()
                        : "";
                return new String[] {filename, parameters};
            }
        }

        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace == -1) return new String[] {trimmed, ""};
        return new String[] {
                trimmed.substring(0, firstSpace),
                trimmed.substring(firstSpace + 1).trim()
        };
    }

    private static int findClosingQuote(String value, int start) {
        for (int index = start; index < value.length(); index++) {
            if (value.charAt(index) != '"') continue;
            int backslashCount = 0;
            for (int cursor = index - 1; cursor >= 0 && value.charAt(cursor) == '\\'; cursor--) {
                backslashCount++;
            }
            if ((backslashCount & 1) == 0) return index;
        }
        return -1;
    }

    public void killProcess(final String processName) {
        addAction(() -> {
            sendData.rewind();
            sendData.put(RequestCodes.KILL_PROCESS);
            byte[] bytes = processName.getBytes();
            sendData.putInt(bytes.length);
            sendData.put(bytes);
            sendPacket(CLIENT_PORT);
        });
    }

    public void listProcesses() {
        if (!running) {
            OnGetProcessInfoListener listener = onGetProcessInfoListener;
            if (listener != null) listener.onGetProcessInfo(0, 0, null);
            return;
        }
        addAction(() -> {
            sendData.rewind();
            sendData.put(RequestCodes.LIST_PROCESSES);
            sendData.putInt(0);

            if (!sendPacket(CLIENT_PORT) && onGetProcessInfoListener != null) {
                onGetProcessInfoListener.onGetProcessInfo(0, 0, null);
            }
        });
    }

    public void setProcessAffinity(final String processName, final int affinityMask) {
        addAction(() -> {
            byte[] bytes = processName.getBytes();
            sendData.rewind();
            sendData.put(RequestCodes.SET_PROCESS_AFFINITY);
            sendData.putInt(9 + bytes.length);
            sendData.putInt(0);
            sendData.putInt(affinityMask);
            sendData.put((byte) bytes.length);
            sendData.put(bytes);
            sendPacket(CLIENT_PORT);
        });
    }

    public void setProcessAffinity(final int pid, final int affinityMask) {
        addAction(() -> {
            sendData.rewind();
            sendData.put(RequestCodes.SET_PROCESS_AFFINITY);
            sendData.putInt(9);
            sendData.putInt(pid);
            sendData.putInt(affinityMask);
            sendData.put((byte) 0);
            sendPacket(CLIENT_PORT);
        });
    }

    public void mouseEvent(int flags, int dx, int dy, int wheelDelta) {
        if (!initReceived) return;
        addAction(() -> {
            sendData.rewind();
            sendData.put(RequestCodes.MOUSE_EVENT);
            sendData.putInt(10);
            sendData.putInt(flags);
            sendData.putShort((short) dx);
            sendData.putShort((short) dy);
            sendData.putShort((short) wheelDelta);
            sendData.put((byte) ((flags & MouseEventFlags.MOVE) != 0 ? 1 : 0));
            sendPacket(CLIENT_PORT);
        });
    }

    public void keyboardEvent(byte vkey, int flags) {
        if (!initReceived) return;
        addAction(() -> {
            sendData.rewind();
            sendData.put(RequestCodes.KEYBOARD_EVENT);
            sendData.put(vkey);
            sendData.putInt(flags);
            sendPacket(CLIENT_PORT);
        });
    }

    public void bringToFront(final String processName) {
        bringToFront(processName, 0);
    }

    public void bringToFront(final String processName, final long handle) {
        addAction(() -> {
            sendData.rewind();
            sendData.put(RequestCodes.BRING_TO_FRONT);
            byte[] bytes = encodeProcessName(processName, BRING_TO_FRONT_NAME_BYTES);
            sendData.putInt(bytes.length);
            sendData.put(bytes);
            sendData.putLong(handle);
            sendPacket(CLIENT_PORT);
        });
    }

    private byte[] encodeProcessName(String processName, int maxBytes) {
        if (processName == null || processName.isEmpty() || maxBytes <= 0) return new byte[0];

        byte[] encoded = processName.getBytes(StandardCharsets.UTF_8);
        if (encoded.length <= maxBytes) return encoded;

        for (int end = processName.length() - 1; end > 0; end--) {
            encoded = processName.substring(0, end).getBytes(StandardCharsets.UTF_8);
            if (encoded.length <= maxBytes) return encoded;
        }
        return new byte[0];
    }

    private void addAction(Runnable action) {
        synchronized (actions) {
            if (!running) return;
            actions.add(action);
            actions.notifyAll();
        }
    }

    public boolean isReady() {
        return initReceived;
    }

    public OnGetProcessInfoListener getOnGetProcessInfoListener() {
        return onGetProcessInfoListener;
    }

    public void setOnGetProcessInfoListener(OnGetProcessInfoListener onGetProcessInfoListener) {
        synchronized (actions) {
            this.onGetProcessInfoListener = onGetProcessInfoListener;
        }
    }

    private void startSendThread() {
        Executors.newSingleThreadExecutor().execute(() -> {
            while (running) {
                synchronized (actions) {
                    while (running && initReceived && !actions.isEmpty()) actions.poll().run();
                    try {
                        while (running && (!initReceived || actions.isEmpty())) {
                            actions.wait();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Log.w(TAG, "WinHandler send thread interrupted", e);
                        return;
                    }
                }
            }
        });
    }

    public void stop() {
        boolean wasRunning = running;
        running = false;
        initReceived = false;

        if (socket != null) {
            socket.close();
            socket = null;
        }
        closeVibrationListener("stop");
        vibrationRunning = false;
        closeFakeInputWriter();
        gamepadClients.clear();

        synchronized (actions) {
            actions.clear();
            actions.notifyAll();
        }

        if (wasRunning) {
            ForensicLogger.logEvent(
                    activity,
                    "info",
                    "WINHANDLER_SOCKET_STOPPED",
                    null,
                    "winhandler",
                    "winhandler_socket_stopped",
                    ForensicLogger.fields(
                            "server_port", (int) SERVER_PORT,
                            "client_port", (int) CLIENT_PORT
                    )
            );
        }
    }

    private void handleRequest(byte requestCode, final int port) {
        switch (requestCode) {
            case RequestCodes.INIT: {
                initReceived = true;
                ForensicLogger.logEvent(
                        activity,
                        "info",
                        "WINHANDLER_INIT_RECEIVED",
                        null,
                        "winhandler",
                        "winhandler_init_received",
                        ForensicLogger.fields(
                                "server_port", (int) SERVER_PORT,
                                "client_port", (int) CLIENT_PORT,
                                "remote_port", port
                        )
                );
                synchronized (actions) {
                    actions.notifyAll();
                }

                preferences = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
                gyroTriggerButton = preferences.getInt("gyro_trigger_button", KeyEvent.KEYCODE_BUTTON_L1);
                isToggleMode = preferences.getInt("gyro_mode", 0) == 1;
                triggerType = (byte) preferences.getInt("trigger_type", TRIGGER_IS_AXIS);

                refreshControllerMappings();

                if (!xinputDisabledInitialized) {
                    xinputDisabled = preferences.getBoolean("xinput_toggle", false);
                }

                useLegacyInputMethod = preferences.getBoolean("useLegacyInputMethod", false);
                setGyroSensitivityX(preferences.getFloat("gyro_x_sensitivity", 1.0f));
                setGyroSensitivityY(preferences.getFloat("gyro_y_sensitivity", 1.0f));
                setSmoothingFactor(preferences.getFloat("gyro_smoothing", 0.9f));
                setInvertGyroX(preferences.getBoolean("invert_gyro_x", false));
                setInvertGyroY(preferences.getBoolean("invert_gyro_y", false));
                setGyroDeadzone(preferences.getFloat("gyro_deadzone", 0.05f));
                processGyroWithLeftTrigger = preferences.getBoolean("process_gyro_with_left_trigger", false);
                globalVibrationEnabled = preferences.getBoolean(ControllerManager.PREF_VIBRATION_GLOBAL, true);

                synchronized (actions) {
                    actions.notify();
                }
                break;
            }

            case RequestCodes.GET_PROCESS: {
                if (onGetProcessInfoListener == null) return;
                if (receiveData.remaining() < 57) return;
                receiveData.position(receiveData.position() + 4);
                int numProcesses = receiveData.getShort();
                int index = receiveData.getShort();
                int pid = receiveData.getInt();
                long memoryUsage = receiveData.getLong();
                int affinityMask = receiveData.getInt();
                boolean wow64Process = receiveData.get() == 1;

                byte[] bytes = new byte[32];
                receiveData.get(bytes);
                String name = StringUtils.fromANSIString(bytes);
                String path = "";
                if (receiveData.hasRemaining()) {
                    byte[] pathBytes = new byte[receiveData.remaining()];
                    receiveData.get(pathBytes);
                    path = StringUtils.fromANSIString(pathBytes);
                }

                onGetProcessInfoListener.onGetProcessInfo(index, numProcesses, new ProcessInfo(pid, name, path, memoryUsage, affinityMask, wow64Process));
                break;
            }

            case RequestCodes.GET_GAMEPAD: {
                if (xinputDisabled) return;
                boolean isXInput = receiveData.get() == 1;
                boolean notify = receiveData.get() == 1;
                final ControlsProfile profile = activity.getInputControlsView().getProfile();
                boolean useVirtualGamepad = profile != null && profile.isVirtualGamepad() && activity.getInputControlsView().isShowTouchscreenControls();

                if (!useVirtualGamepad && (currentController == null || !currentController.isConnected())) {
                    currentController = resolveController(0);
                }

                final boolean enabled = currentController != null || useVirtualGamepad;

                if (enabled && notify) {
                    if (!gamepadClients.contains(port)) gamepadClients.add(port);
                } else {
                    gamepadClients.remove(Integer.valueOf(port));
                }

                addAction(() -> {
                    sendData.rewind();
                    sendData.put(RequestCodes.GET_GAMEPAD);

                    if (enabled) {
                        sendData.putInt(!useVirtualGamepad ? currentController.getDeviceId() : profile.id);
                        sendData.put(useLegacyInputMethod ? dinputMapperType : inputType);
                        byte[] bytes = (useVirtualGamepad ? profile.getName() : currentController.getName()).getBytes();
                        sendData.putInt(bytes.length);
                        sendData.put(bytes);
                    } else {
                        sendData.putInt(0);
                    }

                    sendPacket(port);
                });
                break;
            }

            case RequestCodes.GET_GAMEPAD_STATE: {
                if (xinputDisabled) return;
                int gamepadId = receiveData.getInt();
                final ControlsProfile profile = activity.getInputControlsView().getProfile();
                boolean useVirtualGamepad = profile != null && profile.isVirtualGamepad() && activity.getInputControlsView().isShowTouchscreenControls();
                final boolean enabled = currentController != null || useVirtualGamepad;

                if (!useVirtualGamepad) {
                    if (currentController == null || currentController.getDeviceId() != gamepadId) {
                        currentController = resolveController(gamepadId);
                    }
                }

                addAction(() -> {
                    sendData.rewind();
                    sendData.put(RequestCodes.GET_GAMEPAD_STATE);
                    sendData.put((byte) (enabled ? 1 : 0));

                    if (enabled) {
                        sendData.putInt(gamepadId);
                        GamepadState state = useVirtualGamepad ? profile.getGamepadState() : currentController.state;
                        getOutputGamepadState(state).writeTo(sendData);
                    }

                    sendPacket(port);
                });
                break;
            }

            case RequestCodes.RELEASE_GAMEPAD: {
                currentController = null;
                gamepadClients.clear();
                break;
            }

            case RequestCodes.CURSOR_POS_FEEDBACK: {
                short x = receiveData.getShort();
                short y = receiveData.getShort();
                XServer xServer = activity.getXServer();
                xServer.pointer.setX(x);
                xServer.pointer.setY(y);
                activity.getXServerView().requestRender();
                break;
            }

            default: {
                break;
            }
        }
    }

    public void start() {
        if (running) {
            stop();
        } else if (socket != null) {
            socket.close();
            socket = null;
        }

        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            Log.w(TAG, "Falling back to loopback address for WinHandler transport", e);
            localhost = InetAddress.getLoopbackAddress();
        }

        if (fakeInputBasePath != null && !fakeInputBasePath.isEmpty() && !vibrationRunning) {
            startVibrationListener();
        }

        initReceived = false;
        running = true;
        ForensicLogger.logEvent(
                activity,
                "info",
                "WINHANDLER_SOCKET_STARTING",
                null,
                "winhandler",
                "winhandler_socket_starting",
                ForensicLogger.fields(
                        "server_port", (int) SERVER_PORT,
                        "client_port", (int) CLIENT_PORT,
                        "localhost", localhost == null ? "" : localhost.getHostAddress()
                )
        );
        startSendThread();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress((InetAddress) null, SERVER_PORT));
                ForensicLogger.logEvent(
                        activity,
                        "info",
                        "WINHANDLER_SOCKET_BOUND",
                        null,
                        "winhandler",
                        "winhandler_socket_bound",
                        ForensicLogger.fields(
                                "server_port", (int) SERVER_PORT,
                                "client_port", (int) CLIENT_PORT,
                                "localhost", localhost == null ? "" : localhost.getHostAddress()
                        )
                );

                while (running) {
                    receivePacket.setLength(receiveData.capacity());
                    socket.receive(receivePacket);

                    synchronized (actions) {
                        receiveData.clear();
                        receiveData.limit(receivePacket.getLength());
                        byte requestCode = receiveData.get();
                        handleRequest(requestCode, receivePacket.getPort());
                    }
                }
            } catch (IOException e) {
                if (running) {
                    Log.e(TAG, "WinHandler socket loop failed", e);
                    ForensicLogger.error(
                            activity,
                            "WINHANDLER_SOCKET_FAILED",
                            null,
                            "winhandler",
                            "winhandler_socket_failed",
                            e,
                            ForensicLogger.fields(
                                    "server_port", (int) SERVER_PORT,
                                    "client_port", (int) CLIENT_PORT,
                                    "localhost", localhost == null ? "" : localhost.getHostAddress()
                            )
                    );
                }
            } finally {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                running = false;
                initReceived = false;
                synchronized (actions) {
                    actions.notifyAll();
                }
            }
        });
    }

    public void sendGamepadState() {
        if (xinputDisabled) return;

        final ControlsProfile profile = activity.getInputControlsView().getProfile();
        final boolean useVirtualGamepad = profile != null && profile.isVirtualGamepad() && activity.getInputControlsView().isShowTouchscreenControls();

        if (useVirtualGamepad) {
            GamepadState state = profile.getGamepadState();
            writeVirtualStateToFakeInput(state);
            sendVirtualStateToClients(profile, state);
            return;
        }

        releaseSlot(OSC_DEVICE_ID);

        ExternalController controller = currentController;
        if (controller == null || !controller.isConnected()) {
            controller = resolveController(0);
            if (controller != null) currentController = controller;
        }
        if (controller == null) return;

        writeControllerStateToFakeInput(controller);
        sendControllerStateToClients(controller);
    }

    public void sendVirtualGamepadState(GamepadState state) {
        if (xinputDisabled || state == null) return;

        ControlsProfile profile = activity.getInputControlsView().getProfile();
        if (profile == null || !profile.isVirtualGamepad()) {
            releaseSlot(OSC_DEVICE_ID);
            return;
        }

        writeVirtualStateToFakeInput(state);
        sendVirtualStateToClients(profile, state);
    }

    private void sendControllerState(ExternalController controller) {
        if (controller == null || xinputDisabled) return;
        currentController = controller;
        writeControllerStateToFakeInput(controller);
        sendControllerStateToClients(controller);
    }

    private void sendControllerStateToClients(ExternalController controller) {
        if (!initReceived || gamepadClients.isEmpty() || controller == null) return;

        final GamepadState state = getOutputGamepadState(controller.state);
        for (final int port : gamepadClients) {
            addAction(() -> {
                sendData.rewind();
                sendData.put(RequestCodes.GET_GAMEPAD_STATE);
                sendData.put((byte) 1);
                sendData.putInt(controller.getDeviceId());
                state.writeTo(sendData);
                sendPacket(port);
            });
        }
    }

    private void sendVirtualStateToClients(ControlsProfile profile, GamepadState state) {
        if (!initReceived || gamepadClients.isEmpty() || profile == null || state == null) return;

        final GamepadState outputState = getOutputGamepadState(state);
        for (final int port : gamepadClients) {
            addAction(() -> {
                sendData.rewind();
                sendData.put(RequestCodes.GET_GAMEPAD_STATE);
                sendData.put((byte) 1);
                sendData.putInt(profile.id);
                outputState.writeTo(sendData);
                sendPacket(port);
            });
        }
    }

    public void setXInputDisabled(boolean disabled) {
        this.xinputDisabled = disabled;
        this.xinputDisabledInitialized = true;
        Log.d(TAG, "XInput Disabled set to: " + xinputDisabled);
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        if (!running) return false;
        ExternalController controller = resolveController(event.getDeviceId());
        boolean handled = false;
        if (controller != null) {
            handled = controller.updateStateFromMotionEvent(event);
            if (handled) {
                sendControllerState(controller);
            }
        }

        if (gyroTriggerButton == KeyEvent.KEYCODE_BUTTON_L2 || gyroTriggerButton == KeyEvent.KEYCODE_BUTTON_R2) {
            float triggerValue = gyroTriggerButton == KeyEvent.KEYCODE_BUTTON_L2
                    ? event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
                    : event.getAxisValue(MotionEvent.AXIS_RTRIGGER);

            boolean isPressed = triggerValue > 0.5f;
            if (isPressed) {
                if (!isGyroActive) {
                    isGyroActive = isToggleMode ? !isGyroActive : true;
                }
            } else if (isGyroActive && !isToggleMode) {
                isGyroActive = false;
            }
        }
        return handled;
    }

    public boolean onKeyEvent(KeyEvent event) {
        if (!running) return false;
        if (event.getKeyCode() == gyroTriggerButton) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (isToggleMode) {
                    isGyroActive = !isGyroActive;
                } else {
                    isGyroActive = true;
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP && !isToggleMode) {
                isGyroActive = false;
                if (currentController != null) {
                    currentController.state.thumbRX = 0.0f;
                    currentController.state.thumbRY = 0.0f;
                }
                sendGamepadState();
            }
        }

        ExternalController controller = resolveController(event.getDeviceId());
        boolean handled = false;
        if (controller != null && event.getRepeatCount() == 0) {
            int action = event.getAction();
            if (action == KeyEvent.ACTION_DOWN || action == KeyEvent.ACTION_UP) {
                handled = controller.updateStateFromKeyEvent(event);
            }
            if (handled) {
                sendControllerState(controller);
            }
        }
        return handled;
    }

    public byte getInputType() {
        return inputType;
    }

    public void setInputType(byte inputType) {
        this.inputType = inputType;
    }

    public ExternalController getCurrentController() {
        return currentController;
    }

    public void execWithDelay(String command, int delaySeconds) {
        if (command == null || command.trim().isEmpty() || delaySeconds < 0) return;
        Executors.newSingleThreadScheduledExecutor().schedule(() -> exec(command), delaySeconds, TimeUnit.SECONDS);
    }

    public void initializeController() {
        currentController = resolveController(0);
        if (currentController != null) {
            currentController.setTriggerType(triggerType);
            Log.d(TAG, "Controller initialized with trigger type " + triggerType);
        }
    }

    public void refreshControllerMappings() {
        triggerType = (byte) preferences.getInt("trigger_type", TRIGGER_IS_AXIS);
        for (ExternalController controller : controllers.values()) {
            if (controller != null) {
                controller.setTriggerType(triggerType);
            }
        }
        if (currentController != null) {
            currentController.setTriggerType(triggerType);
            sendControllerState(currentController);
        }
    }

    public void setFakeInputPath(String fakeInputPath) {
        if (fakeInputPath == null || fakeInputPath.isEmpty()) return;
        this.fakeInputBasePath = fakeInputPath;
        ForensicLogger.logEvent(
                activity,
                "info",
                "WINHANDLER_FAKEINPUT_PATH_READY",
                null,
                "winhandler",
                "winhandler_fakeinput_path_ready",
                ForensicLogger.fields(
                        "path", fakeInputPath,
                        "max_controllers", MAX_CONTROLLERS
                )
        );
        startVibrationListener();
    }

    public int preAssignConnectedControllers() {
        if (fakeInputBasePath == null || fakeInputBasePath.isEmpty()) return 0;

        int assignedCount = 0;
        for (int deviceId : getConnectedGamepadDeviceIds()) {
            if (usedSlots.size() >= MAX_CONTROLLERS) break;
            if (assignConnectedDeviceIfPossible(deviceId, "startup-scan")) {
                assignedCount++;
            }
        }
        ForensicLogger.logEvent(
                activity,
                "info",
                "CONTROLLER_PREASSIGN_SUMMARY",
                null,
                "input",
                "controller_preassign_summary",
                ForensicLogger.fields(
                        "assigned_count", assignedCount,
                        "tracked_slots", usedSlots.size(),
                        "fake_input_path", fakeInputBasePath
                )
        );
        return assignedCount;
    }

    private int[] getConnectedGamepadDeviceIds() {
        int[] deviceIds = InputDevice.getDeviceIds();
        Integer[] sortedIds = new Integer[deviceIds.length];
        for (int i = 0; i < deviceIds.length; i++) {
            sortedIds[i] = deviceIds[i];
        }
        Arrays.sort(sortedIds, Comparator
                .comparing((Integer id) -> {
                    InputDevice device = InputDevice.getDevice(id);
                    return ExternalController.getPhysicalDeviceIdentifier(device);
                })
                .thenComparingInt(Integer::intValue));

        int[] result = new int[sortedIds.length];
        for (int i = 0; i < sortedIds.length; i++) {
            result[i] = sortedIds[i];
        }
        return result;
    }

    private boolean assignConnectedDeviceIfPossible(int deviceId, String source) {
        if (deviceToSlot.containsKey(deviceId)) return false;
        if (usedSlots.size() >= MAX_CONTROLLERS) return false;

        InputDevice device = InputDevice.getDevice(deviceId);
        if (!ExternalController.isGameController(device)) return false;

        ExternalController controller = resolveController(deviceId);
        if (controller == null) return false;

        int slot = assignSlot(deviceId);
        if (slot < 0) return false;

        controller.setTriggerType(triggerType);
        ForensicLogger.logEvent(
                activity,
                "info",
                "CONTROLLER_SLOT_ASSIGNED",
                null,
                "input",
                "controller_slot_assigned",
                ForensicLogger.fields(
                        "device_id", deviceId,
                        "slot", slot,
                        "source", source,
                        "physical_id", ExternalController.getPhysicalDeviceIdentifier(device),
                        "device_name", device == null ? "" : device.getName()
                )
        );
        return true;
    }

    private ExternalController resolveController(int deviceId) {
        if (controllers.containsKey(deviceId)) {
            ExternalController existing = controllers.get(deviceId);
            if (existing != null) existing.setTriggerType(triggerType);
            return existing;
        }

        InputDevice device = InputDevice.getDevice(deviceId);
        String physicalId = ExternalController.getPhysicalDeviceIdentifier(device);
        if (!physicalId.isEmpty()) {
            for (Map.Entry<Integer, ExternalController> entry : controllers.entrySet()) {
                InputDevice sibling = InputDevice.getDevice(entry.getKey());
                if (physicalId.equals(ExternalController.getPhysicalDeviceIdentifier(sibling))) {
                    controllers.put(deviceId, entry.getValue());
                    entry.getValue().setTriggerType(triggerType);
                    return entry.getValue();
                }
            }
        }

        ExternalController controller = ExternalController.getController(deviceId);
        if (controller != null) {
            controller.setContext(activity);
            controller.setTriggerType(triggerType);
            controllers.put(deviceId, controller);
        }
        return controller;
    }

    private void ensureWriterForSlot(int slot) {
        if (slot < 0 || slot >= MAX_CONTROLLERS || fakeInputBasePath == null) return;
        if (fakeInputWriters[slot] == null) {
            fakeInputWriters[slot] = new FakeInputWriter(fakeInputBasePath, slot);
            fakeInputWriters[slot].open();
        }
    }

    private boolean isPhysicalSlotOccupied(int slot) {
        for (Map.Entry<Integer, Integer> entry : deviceToSlot.entrySet()) {
            if (entry.getKey() != OSC_DEVICE_ID && entry.getValue() == slot) {
                return true;
            }
        }
        return false;
    }

    private int getHighestPhysicalSlot() {
        int highest = -1;
        for (Map.Entry<Integer, Integer> entry : deviceToSlot.entrySet()) {
            if (entry.getKey() != OSC_DEVICE_ID) {
                highest = Math.max(highest, entry.getValue());
            }
        }
        return highest;
    }

    private int findLowestAvailablePhysicalSlot() {
        for (int slot = 0; slot < MAX_CONTROLLERS; slot++) {
            if (!isPhysicalSlotOccupied(slot)) return slot;
        }
        return -1;
    }

    private int findPreferredVirtualSlot(Integer currentSlot) {
        int minSlot = getHighestPhysicalSlot() + 1;
        for (int slot = minSlot; slot < MAX_CONTROLLERS; slot++) {
            if ((currentSlot != null && currentSlot == slot) || !usedSlots.contains(slot)) {
                return slot;
            }
        }
        return -1;
    }

    private void bindDeviceToSlot(int deviceId, String descriptor, int slot) {
        usedSlots.add(slot);
        deviceToSlot.put(deviceId, slot);
        if (descriptor != null && !descriptor.isEmpty()) {
            descriptorToSlot.put(descriptor, slot);
            deviceToDescriptor.put(deviceId, descriptor);
        }
        ensureWriterForSlot(slot);
    }

    private boolean moveVirtualGamepadToSlot(int targetSlot) {
        Integer currentSlot = deviceToSlot.get(OSC_DEVICE_ID);
        if (currentSlot == null) return false;
        if (targetSlot == currentSlot) return true;
        if (targetSlot < 0 || targetSlot >= MAX_CONTROLLERS) {
            releaseSlot(OSC_DEVICE_ID);
            return false;
        }

        ensureWriterForSlot(targetSlot);
        if (fakeInputWriters[currentSlot] != null) {
            fakeInputWriters[currentSlot].reset();
        }

        deviceToSlot.put(OSC_DEVICE_ID, targetSlot);
        usedSlots.remove(currentSlot);
        usedSlots.add(targetSlot);
        if (fallbackSlot == currentSlot) {
            fallbackSlot = -1;
        }
        return true;
    }

    private void rebalanceVirtualGamepadSlot() {
        Integer virtualSlot = deviceToSlot.get(OSC_DEVICE_ID);
        if (virtualSlot == null) return;

        int preferredSlot = findPreferredVirtualSlot(virtualSlot);
        if (preferredSlot == -1) {
            releaseSlot(OSC_DEVICE_ID);
        } else if (preferredSlot != virtualSlot) {
            moveVirtualGamepadToSlot(preferredSlot);
        }
    }

    private int assignSlot(int deviceId) {
        Integer existing = deviceToSlot.get(deviceId);
        if (existing != null) {
            if (deviceId == OSC_DEVICE_ID) {
                int preferredVirtualSlot = findPreferredVirtualSlot(existing);
                if (preferredVirtualSlot != -1 && preferredVirtualSlot != existing) {
                    moveVirtualGamepadToSlot(preferredVirtualSlot);
                    Integer updatedSlot = deviceToSlot.get(deviceId);
                    return updatedSlot != null ? updatedSlot : -1;
                }
            }
            return existing;
        }

        InputDevice device = InputDevice.getDevice(deviceId);
        String physicalId = ExternalController.getPhysicalDeviceIdentifier(device);
        if (!physicalId.isEmpty()) {
            Integer siblingSlot = descriptorToSlot.get(physicalId);
            if (siblingSlot != null) {
                deviceToSlot.put(deviceId, siblingSlot);
                deviceToDescriptor.put(deviceId, physicalId);
                return siblingSlot;
            }
        }

        if (deviceId == OSC_DEVICE_ID) {
            int virtualSlot = findPreferredVirtualSlot(null);
            if (virtualSlot >= 0) {
                bindDeviceToSlot(deviceId, null, virtualSlot);
                return virtualSlot;
            }
            return -1;
        }

        int preferredPhysicalSlot = findLowestAvailablePhysicalSlot();
        if (preferredPhysicalSlot < 0) return -1;

        Integer virtualSlot = deviceToSlot.get(OSC_DEVICE_ID);
        if (virtualSlot != null && virtualSlot == preferredPhysicalSlot) {
            int relocatedVirtualSlot = findPreferredVirtualSlot(null);
            moveVirtualGamepadToSlot(relocatedVirtualSlot);
        }

        bindDeviceToSlot(deviceId, physicalId, preferredPhysicalSlot);
        return preferredPhysicalSlot;
    }

    private void releaseSlot(int deviceId) {
        Integer slot = deviceToSlot.remove(deviceId);
        if (slot == null) return;

        String descriptor = deviceToDescriptor.remove(deviceId);
        boolean slotStillInUse = false;
        if (descriptor != null) {
            for (Map.Entry<Integer, String> entry : deviceToDescriptor.entrySet()) {
                if (descriptor.equals(entry.getValue()) && deviceToSlot.containsKey(entry.getKey())) {
                    slotStillInUse = true;
                    break;
                }
            }
            if (!slotStillInUse) {
                descriptorToSlot.remove(descriptor);
            }
        }

        if (!slotStillInUse) {
            if (fallbackSlot == slot) fallbackSlot = -1;
            if (fakeInputWriters[slot] != null) {
                fakeInputWriters[slot].softRelease();
                fakeInputWriters[slot] = null;
            }
            usedSlots.remove(slot);
            if (deviceId != OSC_DEVICE_ID) rebalanceVirtualGamepadSlot();
        }

        ForensicLogger.logEvent(
                activity,
                "info",
                "CONTROLLER_SLOT_RELEASED",
                null,
                "input",
                "controller_slot_released",
                ForensicLogger.fields(
                        "device_id", deviceId,
                        "slot", slot,
                        "slot_still_in_use", slotStillInUse,
                        "descriptor", descriptor == null ? "" : descriptor
                )
        );
    }

    private void startVibrationListener() {
        if (vibrationRunning || fakeInputBasePath == null || fakeInputBasePath.isEmpty()) return;
        vibrationRunning = true;
        vibrationSuperseded = false;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                claimVibrationSocketOwnership();
                vibrationServer = new LocalServerSocket("winlator_vibration");
                synchronized (VIBRATION_SOCKET_LOCK) {
                    activeVibrationOwner = this;
                    activeVibrationServer = vibrationServer;
                }
                while (vibrationRunning) {
                    LocalSocket client = vibrationServer.accept();
                    try (InputStream input = client.getInputStream()) {
                        byte[] buffer = new byte[8];
                        int read = input.read(buffer);
                        if (read == 8) {
                            int strong = (buffer[0] & 0xff) | ((buffer[1] & 0xff) << 8);
                            int weak = (buffer[2] & 0xff) | ((buffer[3] & 0xff) << 8);
                            int durationMs = (buffer[4] & 0xff) | ((buffer[5] & 0xff) << 8);
                            int slot = (buffer[6] & 0xff) | ((buffer[7] & 0xff) << 8);
                            triggerVibration(strong, weak, durationMs, slot);
                        }
                    } catch (IOException e) {
                        if (vibrationRunning) {
                            Log.e(TAG, "Vibration client error", e);
                        }
                    } finally {
                        try {
                            client.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            } catch (IOException e) {
                if (vibrationRunning && !vibrationSuperseded) {
                    Log.e(TAG, "Vibration listener error", e);
                    ForensicLogger.error(
                            activity,
                            "WINHANDLER_VIBRATION_LISTENER_FAILED",
                            null,
                            "input",
                            "winhandler_vibration_listener_failed",
                            e,
                            ForensicLogger.fields(
                                    "fake_input_path", fakeInputBasePath
                        )
                    );
                }
            } finally {
                releaseVibrationSocketOwnership();
            }
        });
    }

    private void claimVibrationSocketOwnership() {
        synchronized (VIBRATION_SOCKET_LOCK) {
            if (activeVibrationOwner != null && activeVibrationOwner != this) {
                activeVibrationOwner.vibrationSuperseded = true;
                activeVibrationOwner.vibrationRunning = false;
                LocalServerSocket previousServer = activeVibrationServer;
                if (previousServer != null) {
                    try {
                        previousServer.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            activeVibrationOwner = this;
            activeVibrationServer = null;
        }
    }

    private void releaseVibrationSocketOwnership() {
        synchronized (VIBRATION_SOCKET_LOCK) {
            if (activeVibrationOwner == this) {
                activeVibrationOwner = null;
                activeVibrationServer = null;
            }
        }
        vibrationServer = null;
        vibrationRunning = false;
    }

    private void closeVibrationListener(String reason) {
        vibrationRunning = false;
        synchronized (VIBRATION_SOCKET_LOCK) {
            if (activeVibrationOwner == this) {
                activeVibrationOwner = null;
                activeVibrationServer = null;
            }
        }
        if (vibrationServer != null) {
            try {
                vibrationServer.close();
            } catch (IOException ignored) {
            }
            vibrationServer = null;
        }
        ForensicLogger.logEvent(
                activity,
                "info",
                "WINHANDLER_VIBRATION_LISTENER_CLOSED",
                null,
                "input",
                "winhandler_vibration_listener_closed",
                ForensicLogger.fields(
                        "reason", reason,
                        "fake_input_path", fakeInputBasePath == null ? "" : fakeInputBasePath
                )
        );
    }

    private void triggerVibration(int strong, int weak, int durationMs, int slot) {
        if (!globalVibrationEnabled) return;
        if (slot >= 0 && slot < MAX_CONTROLLERS && !vibrationEnabledSlots[slot]) return;

        Vibrator vibrator = null;
        Integer slotOwner = null;
        for (Map.Entry<Integer, Integer> entry : deviceToSlot.entrySet()) {
            if (entry.getValue() == slot) {
                if (entry.getKey() == OSC_DEVICE_ID) {
                    slotOwner = entry.getKey();
                    break;
                }
                if (slotOwner == null) {
                    slotOwner = entry.getKey();
                }
            }
        }

        if (slotOwner != null && slotOwner == OSC_DEVICE_ID) {
            vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        } else if (slotOwner != null) {
            for (Map.Entry<Integer, Integer> entry : deviceToSlot.entrySet()) {
                if (entry.getValue() != slot || entry.getKey() == OSC_DEVICE_ID) continue;
                InputDevice device = InputDevice.getDevice(entry.getKey());
                if (device == null) continue;
                Vibrator candidate = device.getVibrator();
                if (candidate != null && candidate.hasVibrator()) {
                    vibrator = candidate;
                    break;
                }
            }

            if ((vibrator == null || !vibrator.hasVibrator())
                    && !deviceToSlot.containsKey(OSC_DEVICE_ID)
                    && (fallbackSlot == -1 || fallbackSlot == slot)) {
                vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
                fallbackSlot = slot;
            }
        }

        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (strong > 0 || weak > 0) {
            int intensity = Math.max(strong, weak);
            int amplitude = Math.min(255, Math.max(1, (int) ((intensity / 65535.0f) * 255.0f)));
            int duration = Math.max(1, durationMs);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude));
            } else {
                vibrator.vibrate(duration);
            }
        } else {
            vibrator.cancel();
        }
    }

    public void setVibrationEnabledForSlot(int slot, boolean enabled) {
        if (slot < 0 || slot >= MAX_CONTROLLERS) return;
        vibrationEnabledSlots[slot] = enabled;
        preferences.edit()
                .putBoolean("vibration_slot_" + slot, enabled)
                .putBoolean("vibrate_slot_" + slot, enabled)
                .apply();
    }

    public void setGlobalVibrationEnabled(boolean enabled) {
        globalVibrationEnabled = enabled;
        preferences.edit().putBoolean(ControllerManager.PREF_VIBRATION_GLOBAL, enabled).apply();
    }

    private void closeFakeInputWriter() {
        for (int i = 0; i < MAX_CONTROLLERS; i++) {
            if (fakeInputWriters[i] != null) {
                fakeInputWriters[i].destroy();
                fakeInputWriters[i] = null;
            }
        }
        deviceToSlot.clear();
        descriptorToSlot.clear();
        deviceToDescriptor.clear();
        usedSlots.clear();
        controllers.clear();
        currentController = null;
        fallbackSlot = -1;
    }

    private void writeControllerStateToFakeInput(ExternalController controller) {
        if (controller == null) return;
        int slot = assignSlot(controller.getDeviceId());
        if (slot < 0) return;
        writeStateToFakeInput(slot, controller.state);
    }

    private void writeVirtualStateToFakeInput(GamepadState state) {
        int slot = assignSlot(OSC_DEVICE_ID);
        if (slot < 0) return;
        writeStateToFakeInput(slot, state);
    }

    private void writeStateToFakeInput(int slot, GamepadState state) {
        if (state == null) return;
        ensureWriterForSlot(slot);
        FakeInputWriter writer = fakeInputWriters[slot];
        if (writer == null) return;
        try {
            writer.writeGamepadState(getOutputGamepadState(state));
        } catch (IOException e) {
            Log.e(TAG, "Failed to write fake input state", e);
            ForensicLogger.error(
                    activity,
                    "WINHANDLER_FAKEINPUT_WRITE_FAILED",
                    null,
                    "input",
                    "winhandler_fakeinput_write_failed",
                    e,
                    ForensicLogger.fields(
                            "slot", slot,
                            "fake_input_path", fakeInputBasePath
                    )
            );
        }
    }

    private GamepadState getOutputGamepadState(GamepadState baseState) {
        if (baseState == null) return null;
        outputGamepadState.copy(baseState);
        outputGamepadState.thumbRX = Mathf.clamp(baseState.thumbRX + gyroX, -1.0f, 1.0f);
        outputGamepadState.thumbRY = Mathf.clamp(baseState.thumbRY + gyroY, -1.0f, 1.0f);
        return outputGamepadState;
    }
}
