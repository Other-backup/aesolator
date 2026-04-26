package com.winlator.cmod.inputcontrols;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.input.InputManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.core.ForensicLogger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class ControllerManager {
    @SuppressLint("StaticFieldLeak")
    private static ControllerManager instance;

    public static synchronized ControllerManager getInstance() {
        if (instance == null) {
            instance = new ControllerManager();
        }
        return instance;
    }

    private ControllerManager() {
    }

    private Context context;
    private SharedPreferences preferences;
    private InputManager inputManager;

    private final List<InputDevice> detectedDevices = new ArrayList<>();
    private final SparseArray<String> slotAssignments = new SparseArray<>();
    private final boolean[] enabledSlots = new boolean[4];

    public static final String PREF_PLAYER_SLOT_PREFIX = "controller_slot_";
    public static final String PREF_ENABLED_SLOTS_PREFIX = "enabled_slot_";
    public static final String PREF_VIBRATION_GLOBAL = "vibration_enabled_global";

    public void init(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        this.inputManager = (InputManager) this.context.getSystemService(Context.INPUT_SERVICE);
        loadAssignments();
        scanForDevices();
    }

    public void scanForDevices() {
        detectedDevices.clear();
        if (inputManager == null) return;
        LinkedHashMap<String, InputDevice> uniqueDevices = new LinkedHashMap<>();
        int[] deviceIds = inputManager.getInputDeviceIds();
        int controllerLikeDevices = 0;
        int rejectedDevices = 0;
        for (int deviceId : deviceIds) {
            InputDevice device = inputManager.getInputDevice(deviceId);
            if (device == null) continue;
            InputDeviceHeuristics.Decision decision = InputDeviceHeuristics.inspect(device);
            if (decision.controllerLike) controllerLikeDevices++;
            if (decision.accepted) {
                String identifier = getDeviceIdentifier(device);
                InputDevice current = uniqueDevices.get(identifier);
                if (current == null || device.getMotionRanges().size() > current.getMotionRanges().size()) {
                    uniqueDevices.put(identifier, device);
                }
                continue;
            }
            if (decision.controllerLike) {
                rejectedDevices++;
                ForensicLogger.warn(
                        context,
                        "CONTROLLER_SUSPICIOUS_DEVICE_REJECTED",
                        null,
                        "input",
                        "controller_candidate_rejected",
                        ForensicLogger.fields(
                                "device_id", deviceId,
                                "name", device.getName(),
                                "descriptor", getDeviceIdentifier(device),
                                "vendor_id", device.getVendorId(),
                                "product_id", device.getProductId(),
                                "sources_hex", String.format(Locale.US, "0x%08x", device.getSources()),
                                "reason", decision.reason
                        )
                );
            }
        }
        detectedDevices.addAll(uniqueDevices.values());
        ForensicLogger.appCheckpoint(
                "info",
                "CONTROLLER_SCAN_SUMMARY",
                "input",
                "controller_scan_summary",
                ForensicLogger.fields(
                        "detected_devices", detectedDevices.size(),
                        "accepted_unique_devices", uniqueDevices.size(),
                        "controller_like_devices", controllerLikeDevices,
                        "rejected_devices", rejectedDevices
                )
        );
        Log.d("ControllerManager", "scanForDevices detected=" + detectedDevices.size()
                + " controllerLike=" + controllerLikeDevices
                + " rejected=" + rejectedDevices);
    }

    private void loadAssignments() {
        slotAssignments.clear();
        if (preferences == null) return;
        for (int i = 0; i < 4; i++) {
            String prefKey = PREF_PLAYER_SLOT_PREFIX + i;
            String deviceIdentifier = preferences.getString(prefKey, null);
            if (deviceIdentifier != null) {
                slotAssignments.put(i, deviceIdentifier);
            }

            String enabledKey = PREF_ENABLED_SLOTS_PREFIX + i;
            enabledSlots[i] = preferences.getBoolean(enabledKey, i == 0);
        }
    }

    public void saveAssignments() {
        if (preferences == null) return;
        SharedPreferences.Editor editor = preferences.edit();
        for (int i = 0; i < 4; i++) {
            String deviceIdentifier = slotAssignments.get(i);
            String prefKey = PREF_PLAYER_SLOT_PREFIX + i;
            if (deviceIdentifier != null) {
                editor.putString(prefKey, deviceIdentifier);
            } else {
                editor.remove(prefKey);
            }

            String enabledKey = PREF_ENABLED_SLOTS_PREFIX + i;
            editor.putBoolean(enabledKey, enabledSlots[i]);
        }
        editor.apply();
    }

    public static boolean isGameController(InputDevice device) {
        return InputDeviceHeuristics.isGameController(device);
    }

    public static String getDeviceIdentifier(InputDevice device) {
        if (device == null) return null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            String descriptor = device.getDescriptor();
            if (descriptor != null && !descriptor.trim().isEmpty()) return descriptor;
        }
        return "vendor_" + device.getVendorId()
                + "_product_" + device.getProductId()
                + "_name_" + (device.getName() == null ? "" : device.getName().trim().toLowerCase(Locale.US));
    }

    public List<InputDevice> getDetectedDevices() {
        return detectedDevices;
    }

    public int getEnabledPlayerCount() {
        int count = 0;
        for (boolean enabled : enabledSlots) {
            if (enabled) count++;
        }
        return count;
    }

    public void assignDeviceToSlot(int slotIndex, InputDevice device) {
        if (slotIndex < 0 || slotIndex >= 4) return;

        String newDeviceIdentifier = getDeviceIdentifier(device);
        if (newDeviceIdentifier == null) return;

        for (int i = 0; i < 4; i++) {
            if (newDeviceIdentifier.equals(slotAssignments.get(i))) {
                slotAssignments.remove(i);
            }
        }

        slotAssignments.put(slotIndex, newDeviceIdentifier);
        saveAssignments();
    }

    public void unassignSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 4) return;
        slotAssignments.remove(slotIndex);
        saveAssignments();
    }

    public int getSlotForDevice(int deviceId) {
        if (inputManager == null) return -1;
        InputDevice device = inputManager.getInputDevice(deviceId);
        String deviceIdentifier = getDeviceIdentifier(device);
        if (deviceIdentifier == null) return -1;

        for (int i = 0; i < slotAssignments.size(); i++) {
            int key = slotAssignments.keyAt(i);
            String value = slotAssignments.valueAt(i);
            if (deviceIdentifier.equals(value)) {
                return key;
            }
        }
        return -1;
    }

    public InputDevice getAssignedDeviceForSlot(int slotIndex) {
        String assignedIdentifier = slotAssignments.get(slotIndex);
        if (assignedIdentifier == null) return null;

        for (InputDevice device : detectedDevices) {
            if (assignedIdentifier.equals(getDeviceIdentifier(device))) {
                return device;
            }
        }
        return null;
    }

    public void setSlotEnabled(int slotIndex, boolean isEnabled) {
        if (slotIndex < 0 || slotIndex >= 4) return;
        enabledSlots[slotIndex] = isEnabled;
        saveAssignments();
    }

    public boolean isSlotEnabled(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 4) return false;
        return enabledSlots[slotIndex];
    }
}
