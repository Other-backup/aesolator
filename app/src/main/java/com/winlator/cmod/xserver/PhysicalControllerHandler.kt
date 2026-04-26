package com.winlator.cmod.xserver

import android.graphics.PointF
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.winlator.cmod.inputcontrols.Binding
import com.winlator.cmod.inputcontrols.ControlElement
import com.winlator.cmod.inputcontrols.ControlsProfile
import com.winlator.cmod.inputcontrols.ExternalController
import com.winlator.cmod.inputcontrols.ExternalControllerBinding
import com.winlator.cmod.math.Mathf
import java.util.Timer
import java.util.TimerTask

/**
 * Donor standalone physical-controller lane that keeps controller routing
 * independent from view visibility.
 */
class PhysicalControllerHandler(
    private var profile: ControlsProfile?,
    private val xServer: XServer?,
    private val onOpenNavigationMenu: (() -> Unit)? = null,
) {
    private val tag = "gncontrol"
    private val mouseMoveOffset = PointF(0f, 0f)
    private var mouseMoveTimer: Timer? = null

    fun setProfile(profile: ControlsProfile?) {
        this.profile = profile
        Log.d(tag, "PhysicalControllerHandler: Profile set to ${profile?.name}")
        if (profile == null) {
            cleanup()
        }
    }

    fun cleanup() {
        mouseMoveTimer?.cancel()
        mouseMoveTimer = null
        mouseMoveOffset.set(0f, 0f)
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (profile != null && event.repeatCount == 0) {
            val controller = profile?.getController(event.deviceId)
            if (controller != null) {
                val controllerBinding = controller.getControllerBinding(event.keyCode)
                if (controllerBinding != null) {
                    if ((event.keyCode == KeyEvent.KEYCODE_BUTTON_L2 || event.keyCode == KeyEvent.KEYCODE_BUTTON_R2) &&
                        (controllerBinding.binding == Binding.GAMEPAD_BUTTON_L2 || controllerBinding.binding == Binding.GAMEPAD_BUTTON_R2) &&
                        deviceHasTriggerAxis(event.device, event.keyCode)
                    ) {
                        return true
                    }
                    val offset =
                        if (event.action == KeyEvent.ACTION_DOWN &&
                            (controllerBinding.binding == Binding.GAMEPAD_BUTTON_L2 || controllerBinding.binding == Binding.GAMEPAD_BUTTON_R2)
                        ) {
                            1f
                        } else {
                            0f
                        }
                    handleInputEvent(controllerBinding.binding, event.action == KeyEvent.ACTION_DOWN, offset)
                    return true
                }
            }
        }
        return false
    }

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (profile != null) {
            val controller = profile?.getController(event.deviceId)
            if (controller != null && controller.updateStateFromMotionEvent(event)) {
                var controllerBinding = controller.getControllerBinding(KeyEvent.KEYCODE_BUTTON_L2)
                if (controllerBinding != null) {
                    handleInputEvent(
                        controllerBinding.binding,
                        controller.state.triggerL > 0f,
                        controller.state.triggerL,
                    )
                }

                controllerBinding = controller.getControllerBinding(KeyEvent.KEYCODE_BUTTON_R2)
                if (controllerBinding != null) {
                    handleInputEvent(
                        controllerBinding.binding,
                        controller.state.triggerR > 0f,
                        controller.state.triggerR,
                    )
                }

                processJoystickInput(controller)
                return true
            }
        }
        return false
    }

    private fun deviceHasTriggerAxis(device: InputDevice?, keyCode: Int): Boolean =
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_L2 ->
                hasMotionRange(device, MotionEvent.AXIS_LTRIGGER) || hasMotionRange(device, MotionEvent.AXIS_BRAKE)
            KeyEvent.KEYCODE_BUTTON_R2 ->
                hasMotionRange(device, MotionEvent.AXIS_RTRIGGER) || hasMotionRange(device, MotionEvent.AXIS_GAS)
            else -> false
        }

    private fun hasMotionRange(device: InputDevice?, axis: Int): Boolean {
        if (device == null) return false
        return device.getMotionRange(axis, InputDevice.SOURCE_JOYSTICK) != null ||
            device.getMotionRange(axis, InputDevice.SOURCE_GAMEPAD) != null ||
            device.getMotionRange(axis) != null
    }

    private fun createMouseMoveTimer() {
        if (profile != null && mouseMoveTimer == null) {
            mouseMoveTimer = Timer()
            mouseMoveTimer?.schedule(
                object : TimerTask() {
                    override fun run() {
                        val magnitude =
                            Math.sqrt(
                                (mouseMoveOffset.x * mouseMoveOffset.x + mouseMoveOffset.y * mouseMoveOffset.y).toDouble(),
                            )
                        if (magnitude < 0.08) return
                        val cursorSpeed = profile?.cursorSpeed ?: 1f
                        val deltaX = (mouseMoveOffset.x * 10 * cursorSpeed).toInt()
                        val deltaY = (mouseMoveOffset.y * 10 * cursorSpeed).toInt()
                        xServer?.injectPointerMoveDelta(deltaX, deltaY)
                    }
                },
                0,
                1000 / 60,
            )
        }
    }

    private fun processJoystickInput(controller: ExternalController) {
        mouseMoveOffset.set(0f, 0f)

        val axes =
            intArrayOf(
                MotionEvent.AXIS_X,
                MotionEvent.AXIS_Y,
                MotionEvent.AXIS_Z,
                MotionEvent.AXIS_RZ,
                MotionEvent.AXIS_HAT_X,
                MotionEvent.AXIS_HAT_Y,
            )
        val values =
            floatArrayOf(
                controller.state.thumbLX,
                controller.state.thumbLY,
                controller.state.thumbRX,
                controller.state.thumbRY,
                controller.state.dPadX.toFloat(),
                controller.state.dPadY.toFloat(),
            )

        for (i in axes.indices) {
            var controllerBinding: ExternalControllerBinding?
            if (Math.abs(values[i]) > ControlElement.STICK_DEAD_ZONE) {
                val keyCode = ExternalControllerBinding.getKeyCodeForAxis(axes[i], Mathf.sign(values[i]))
                controllerBinding = controller.getControllerBinding(keyCode)
                if (controllerBinding != null) {
                    handleInputEvent(controllerBinding.binding, true, values[i])
                }
            } else {
                controllerBinding =
                    controller.getControllerBinding(ExternalControllerBinding.getKeyCodeForAxis(axes[i], 1.toByte()))
                if (controllerBinding != null) {
                    handleInputEvent(controllerBinding.binding, false, values[i])
                }
                controllerBinding =
                    controller.getControllerBinding(ExternalControllerBinding.getKeyCodeForAxis(axes[i], (-1).toByte()))
                if (controllerBinding != null) {
                    handleInputEvent(controllerBinding.binding, false, values[i])
                }
            }
        }
    }

    private fun handleInputEvent(binding: Binding, isActionDown: Boolean, offset: Float = 0f) {
        if (binding.isGamepad) {
            val winHandler = xServer?.winHandler
            val state = profile?.gamepadState
            if (state != null) {
                val buttonIdx = binding.ordinal - Binding.GAMEPAD_BUTTON_A.ordinal
                if (buttonIdx <= ExternalController.IDX_BUTTON_R2.toInt()) {
                    when (buttonIdx) {
                        ExternalController.IDX_BUTTON_L2.toInt() -> {
                            state.triggerL = offset
                            state.setPressed(ExternalController.IDX_BUTTON_L2.toInt(), offset > 0f)
                        }
                        ExternalController.IDX_BUTTON_R2.toInt() -> {
                            state.triggerR = offset
                            state.setPressed(ExternalController.IDX_BUTTON_R2.toInt(), offset > 0f)
                        }
                        else -> state.setPressed(buttonIdx, isActionDown)
                    }
                } else {
                    when (binding) {
                        Binding.GAMEPAD_LEFT_THUMB_UP, Binding.GAMEPAD_LEFT_THUMB_DOWN -> {
                            state.thumbLY = if (isActionDown) offset else 0f
                        }
                        Binding.GAMEPAD_LEFT_THUMB_LEFT, Binding.GAMEPAD_LEFT_THUMB_RIGHT -> {
                            state.thumbLX = if (isActionDown) offset else 0f
                        }
                        Binding.GAMEPAD_RIGHT_THUMB_UP, Binding.GAMEPAD_RIGHT_THUMB_DOWN -> {
                            state.thumbRY = if (isActionDown) offset else 0f
                        }
                        Binding.GAMEPAD_RIGHT_THUMB_LEFT, Binding.GAMEPAD_RIGHT_THUMB_RIGHT -> {
                            state.thumbRX = if (isActionDown) offset else 0f
                        }
                        Binding.GAMEPAD_DPAD_UP, Binding.GAMEPAD_DPAD_RIGHT, Binding.GAMEPAD_DPAD_DOWN, Binding.GAMEPAD_DPAD_LEFT -> {
                            state.dpad[binding.ordinal - Binding.GAMEPAD_DPAD_UP.ordinal] = isActionDown
                        }
                        else -> {}
                    }
                }

                if (winHandler != null) {
                    winHandler.sendVirtualGamepadState(state)
                }
            }
        } else {
            if (binding == Binding.OPEN_NAVIGATION_MENU) {
                if (isActionDown) {
                    Log.d(tag, "Opening navigation menu from controller binding")
                    onOpenNavigationMenu?.invoke()
                }
            } else if (binding == Binding.MOUSE_MOVE_LEFT || binding == Binding.MOUSE_MOVE_RIGHT) {
                if (isActionDown) {
                    val contribution = if (offset != 0f) offset else if (binding == Binding.MOUSE_MOVE_LEFT) -1f else 1f
                    mouseMoveOffset.x += contribution
                    createMouseMoveTimer()
                }
            } else if (binding == Binding.MOUSE_MOVE_DOWN || binding == Binding.MOUSE_MOVE_UP) {
                if (isActionDown) {
                    val contribution = if (offset != 0f) offset else if (binding == Binding.MOUSE_MOVE_UP) -1f else 1f
                    mouseMoveOffset.y += contribution
                    createMouseMoveTimer()
                }
            } else {
                val pointerButton = binding.pointerButton
                if (isActionDown) {
                    if (pointerButton != null) {
                        xServer?.injectPointerButtonPress(pointerButton)
                    } else {
                        xServer?.injectKeyPress(binding.keycode)
                    }
                } else {
                    if (pointerButton != null) {
                        xServer?.injectPointerButtonRelease(pointerButton)
                    } else {
                        xServer?.injectKeyRelease(binding.keycode)
                    }
                }
            }
        }
    }
}
