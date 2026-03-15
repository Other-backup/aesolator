package com.winlator.cmod.data

import org.json.JSONObject

/**
 * Donor-compatible per-container gesture profile, stored as JSON in
 * {@code Container.gestureConfig}.
 */
data class TouchGestureConfig(
    val tapEnabled: Boolean = true,
    val dragEnabled: Boolean = true,
    val longPressEnabled: Boolean = false,
    val longPressAction: String = ACTION_RIGHT_CLICK,
    val longPressDelay: Int = DEFAULT_DELAY_MS,
    val doubleTapEnabled: Boolean = true,
    val doubleTapDelay: Int = DEFAULT_DELAY_MS,
    val twoFingerDragEnabled: Boolean = true,
    val twoFingerDragAction: String = PAN_MIDDLE_MOUSE,
    val pinchEnabled: Boolean = true,
    val pinchAction: String = ZOOM_SCROLL_WHEEL,
    val twoFingerTapEnabled: Boolean = true,
    val twoFingerTapAction: String = ACTION_RIGHT_CLICK,
) {
    fun toJson(): String =
        JSONObject().apply {
            put(KEY_TAP_ENABLED, tapEnabled)
            put(KEY_DRAG_ENABLED, dragEnabled)
            put(KEY_LONG_PRESS_ENABLED, longPressEnabled)
            put(KEY_LONG_PRESS_ACTION, longPressAction)
            put(KEY_LONG_PRESS_DELAY, longPressDelay)
            put(KEY_DOUBLE_TAP_ENABLED, doubleTapEnabled)
            put(KEY_DOUBLE_TAP_DELAY, doubleTapDelay)
            put(KEY_TWO_FINGER_DRAG_ENABLED, twoFingerDragEnabled)
            put(KEY_TWO_FINGER_DRAG_ACTION, twoFingerDragAction)
            put(KEY_PINCH_ENABLED, pinchEnabled)
            put(KEY_PINCH_ACTION, pinchAction)
            put(KEY_TWO_FINGER_TAP_ENABLED, twoFingerTapEnabled)
            put(KEY_TWO_FINGER_TAP_ACTION, twoFingerTapAction)
        }.toString()

    companion object {
        const val DEFAULT_DELAY_MS = 300
        const val DOUBLE_TAP_DISTANCE_PX = 100

        const val ACTION_LEFT_CLICK = "left_click"
        const val ACTION_RIGHT_CLICK = "right_click"
        const val ACTION_MIDDLE_CLICK = "middle_click"

        const val PAN_WASD = "wasd"
        const val PAN_ARROW_KEYS = "arrow_keys"
        const val PAN_MIDDLE_MOUSE = "middle_mouse_pan"

        const val ZOOM_SCROLL_WHEEL = "scroll_wheel"
        const val ZOOM_PLUS_MINUS = "plus_minus"
        const val ZOOM_PAGE_UP_DOWN = "page_up_down"

        private const val KEY_TAP_ENABLED = "tapEnabled"
        private const val KEY_DRAG_ENABLED = "dragEnabled"
        private const val KEY_LONG_PRESS_ENABLED = "longPressEnabled"
        private const val KEY_LONG_PRESS_ACTION = "longPressAction"
        private const val KEY_LONG_PRESS_DELAY = "longPressDelay"
        private const val KEY_DOUBLE_TAP_ENABLED = "doubleTapEnabled"
        private const val KEY_DOUBLE_TAP_DELAY = "doubleTapDelay"
        private const val KEY_TWO_FINGER_DRAG_ENABLED = "twoFingerDragEnabled"
        private const val KEY_TWO_FINGER_DRAG_ACTION = "twoFingerDragAction"
        private const val KEY_PINCH_ENABLED = "pinchEnabled"
        private const val KEY_PINCH_ACTION = "pinchAction"
        private const val KEY_TWO_FINGER_TAP_ENABLED = "twoFingerTapEnabled"
        private const val KEY_TWO_FINGER_TAP_ACTION = "twoFingerTapAction"

        @JvmStatic
        fun fromJson(json: String?): TouchGestureConfig {
            if (json.isNullOrBlank()) return TouchGestureConfig()
            return try {
                val obj = JSONObject(json)
                TouchGestureConfig(
                    tapEnabled = obj.optBoolean(KEY_TAP_ENABLED, true),
                    dragEnabled = obj.optBoolean(KEY_DRAG_ENABLED, true),
                    longPressEnabled = obj.optBoolean(KEY_LONG_PRESS_ENABLED, false),
                    longPressAction = obj.optString(KEY_LONG_PRESS_ACTION, ACTION_RIGHT_CLICK),
                    longPressDelay = obj.optInt(KEY_LONG_PRESS_DELAY, DEFAULT_DELAY_MS),
                    doubleTapEnabled = obj.optBoolean(KEY_DOUBLE_TAP_ENABLED, true),
                    doubleTapDelay = obj.optInt(KEY_DOUBLE_TAP_DELAY, DEFAULT_DELAY_MS),
                    twoFingerDragEnabled = obj.optBoolean(KEY_TWO_FINGER_DRAG_ENABLED, true),
                    twoFingerDragAction = obj.optString(KEY_TWO_FINGER_DRAG_ACTION, PAN_ARROW_KEYS),
                    pinchEnabled = obj.optBoolean(KEY_PINCH_ENABLED, true),
                    pinchAction = obj.optString(KEY_PINCH_ACTION, ZOOM_SCROLL_WHEEL),
                    twoFingerTapEnabled = obj.optBoolean(KEY_TWO_FINGER_TAP_ENABLED, true),
                    twoFingerTapAction = obj.optString(KEY_TWO_FINGER_TAP_ACTION, ACTION_RIGHT_CLICK),
                )
            }
            catch (_: Exception) {
                TouchGestureConfig()
            }
        }

        @JvmField
        val COMMON_MOUSE_ACTIONS =
            listOf(
                ACTION_LEFT_CLICK,
                ACTION_RIGHT_CLICK,
                ACTION_MIDDLE_CLICK,
            )

        @JvmField
        val PAN_ACTIONS =
            listOf(
                PAN_MIDDLE_MOUSE,
                PAN_WASD,
                PAN_ARROW_KEYS,
            )

        @JvmField
        val ZOOM_ACTIONS =
            listOf(
                ZOOM_SCROLL_WHEEL,
                ZOOM_PLUS_MINUS,
                ZOOM_PAGE_UP_DOWN,
            )
    }
}
