package com.winlator.cmod.inputcontrols;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class InputDeviceHeuristics {
    public static final class Decision {
        public final boolean accepted;
        public final boolean controllerLike;
        @NonNull
        public final String reason;

        private Decision(boolean accepted, boolean controllerLike, @NonNull String reason) {
            this.accepted = accepted;
            this.controllerLike = controllerLike;
            this.reason = reason;
        }
    }

    private InputDeviceHeuristics() {
    }

    public static boolean isGameController(@Nullable InputDevice device) {
        return inspect(device).accepted;
    }

    @NonNull
    public static Decision inspect(@Nullable InputDevice device) {
        if (device == null) return new Decision(false, false, "null_device");
        return classifyCandidate(
                device.getName(),
                device.getSources(),
                device.isVirtual(),
                hasGamepadKeys(device),
                hasControllerAxes(device)
        );
    }

    static Decision classifyCandidate(@Nullable String deviceName,
                                      int sources,
                                      boolean isVirtual,
                                      boolean hasGamepadKeys,
                                      boolean hasAxes) {
        boolean advertisesGamepad = (sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD;
        boolean advertisesJoystick = (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
        boolean hasMouseSource = (sources & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE;
        boolean controllerLike = advertisesGamepad || advertisesJoystick;

        if (isVirtual) return new Decision(false, controllerLike, "virtual_device");
        if (!controllerLike) return new Decision(false, false, "non_controller_source");
        if (isLikelyFingerprintUinputName(deviceName)) {
            return new Decision(false, true, "fingerprint_uinput");
        }
        if (advertisesGamepad && hasGamepadKeys) {
            return new Decision(true, true, "gamepad_keys");
        }
        if (advertisesJoystick && hasAxes) {
            return new Decision(true, true, hasMouseSource ? "hybrid_joystick_axes" : "joystick_axes");
        }
        if (!hasMouseSource) {
            return new Decision(true, true, "legacy_source_mask");
        }
        if (hasGamepadKeys || hasAxes) {
            return new Decision(true, true, "hybrid_controller_signature");
        }
        return new Decision(false, true, "mouse_hybrid_without_signature");
    }

    public static boolean isLikelyFingerprintUinputName(@Nullable String deviceName) {
        if (deviceName == null) return false;
        String normalized = deviceName.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty() || !normalized.contains("uinput")) return false;
        return normalized.contains("uinput-fpc")
                || normalized.contains("uinput-xiaomi")
                || normalized.contains("fingerprint")
                || normalized.contains("goodix")
                || normalized.contains("silead")
                || normalized.contains("xiaomi")
                || normalized.contains("fpc");
    }

    private static boolean hasGamepadKeys(@NonNull InputDevice device) {
        boolean[] keys = device.hasKeys(
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_BUTTON_B,
                KeyEvent.KEYCODE_BUTTON_X,
                KeyEvent.KEYCODE_BUTTON_Y,
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.KEYCODE_BUTTON_MODE,
                KeyEvent.KEYCODE_BUTTON_L1,
                KeyEvent.KEYCODE_BUTTON_R1
        );
        for (boolean key : keys) {
            if (key) return true;
        }
        return false;
    }

    private static boolean hasControllerAxes(@NonNull InputDevice device) {
        return hasAxis(device, MotionEvent.AXIS_X)
                || hasAxis(device, MotionEvent.AXIS_Y)
                || hasAxis(device, MotionEvent.AXIS_Z)
                || hasAxis(device, MotionEvent.AXIS_RZ)
                || hasAxis(device, MotionEvent.AXIS_HAT_X)
                || hasAxis(device, MotionEvent.AXIS_HAT_Y);
    }

    private static boolean hasAxis(@NonNull InputDevice device, int axis) {
        return device.getMotionRange(axis) != null;
    }
}
