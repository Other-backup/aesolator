package com.winlator.cmod.inputcontrols;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.InputDevice;

import org.junit.Test;

public class InputDeviceHeuristicsTest {
    @Test
    public void classifyCandidateRejectsFingerprintUinputFalsePositive() {
        InputDeviceHeuristics.Decision decision = InputDeviceHeuristics.classifyCandidate(
                "uinput-fpc fingerprint sensor",
                InputDevice.SOURCE_GAMEPAD,
                false,
                false,
                false
        );

        assertFalse(decision.accepted);
        assertTrue(decision.controllerLike);
        assertTrue("fingerprint_uinput".equals(decision.reason));
    }

    @Test
    public void classifyCandidateRejectsXiaomiUinputFalsePositive() {
        InputDeviceHeuristics.Decision decision = InputDeviceHeuristics.classifyCandidate(
                "uinput-xiaomi fingerprint",
                InputDevice.SOURCE_GAMEPAD,
                false,
                false,
                false
        );

        assertFalse(decision.accepted);
        assertTrue(decision.controllerLike);
        assertTrue("fingerprint_uinput".equals(decision.reason));
    }

    @Test
    public void classifyCandidateAcceptsJoystickWithAxes() {
        InputDeviceHeuristics.Decision decision = InputDeviceHeuristics.classifyCandidate(
                "DualSense",
                InputDevice.SOURCE_JOYSTICK,
                false,
                false,
                true
        );

        assertTrue(decision.accepted);
        assertTrue(decision.controllerLike);
        assertTrue("joystick_axes".equals(decision.reason));
    }

    @Test
    public void classifyCandidateRejectsMouseHybridWithoutGamepadSignature() {
        InputDeviceHeuristics.Decision decision = InputDeviceHeuristics.classifyCandidate(
                "mystery hybrid",
                InputDevice.SOURCE_JOYSTICK | InputDevice.SOURCE_MOUSE,
                false,
                false,
                false
        );

        assertFalse(decision.accepted);
        assertTrue(decision.controllerLike);
        assertTrue("mouse_hybrid_without_signature".equals(decision.reason));
    }
}
