package com.winlator.cmod.externaldisplay

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.winlator.cmod.xserver.XKeycode
import com.winlator.cmod.xserver.XServer

class IMEInputReceiver(
    context: Context,
    private val xServer: XServer,
    private val displayContext: Context = context,
) : FrameLayout(context) {
    companion object {
        private const val TAG = "IMEInputReceiver"

        private val SHIFTED_SYMBOLS =
            setOf(
                ')', '!', '@', '#', '$', '%', '^', '&', '*', '(',
                '_', '+', '{', '}', '|', ':', '"', '<', '>', '?', '~',
            )
    }

    private var imeSessionActive = false

    init {
        isFocusable = false
        isFocusableInTouchMode = false
    }

    override fun onCheckIsTextEditor(): Boolean {
        val isEditor = imeSessionActive && hasFocus()
        Log.d(TAG, "onCheckIsTextEditor=$isEditor")
        return isEditor
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        if (!imeSessionActive || !hasFocus()) {
            Log.d(TAG, "Ignoring onCreateInputConnection active=$imeSessionActive hasFocus=${hasFocus()}")
            return BaseInputConnection(this, false)
        }

        outAttrs.inputType =
            EditorInfo.TYPE_CLASS_TEXT or
                EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        outAttrs.imeOptions =
            EditorInfo.IME_FLAG_NO_FULLSCREEN or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_ACTION_NONE

        return object : BaseInputConnection(this, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                text?.forEach { char -> sendCharacterToGame(char) }
                return true
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (beforeLength > 0) {
                    repeat(beforeLength) {
                        xServer.injectKeyPress(XKeycode.KEY_BKSP, 0)
                        xServer.injectKeyRelease(XKeycode.KEY_BKSP)
                    }
                }
                return true
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                    xServer.injectKeyPress(XKeycode.KEY_BKSP, 0)
                    xServer.injectKeyRelease(XKeycode.KEY_BKSP)
                    return true
                }
                return super.sendKeyEvent(event)
            }
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && imeSessionActive && !hasFocus()) {
            post { requestFocus() }
        }
    }

    private data class KeyDispatch(
        val keyCode: Int,
        val requiresShift: Boolean,
    )

    private fun sendCharacterToGame(char: Char) {
        val keyDispatch = charToKeyDispatch(char) ?: run {
            Log.w(TAG, "Could not map character '$char'")
            return
        }

        val metaState = if (keyDispatch.requiresShift) KeyEvent.META_SHIFT_ON else 0
        xServer.keyboard.onKeyEvent(
            KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyDispatch.keyCode, 0, metaState),
        )
        xServer.keyboard.onKeyEvent(
            KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyDispatch.keyCode, 0, metaState),
        )
    }

    private fun charToKeyDispatch(char: Char): KeyDispatch? {
        val isUppercaseLetter = char in 'A'..'Z'
        val normalizedChar = if (isUppercaseLetter) char.lowercaseChar() else char

        val keyCode =
            when (normalizedChar) {
                'a' -> KeyEvent.KEYCODE_A
                'b' -> KeyEvent.KEYCODE_B
                'c' -> KeyEvent.KEYCODE_C
                'd' -> KeyEvent.KEYCODE_D
                'e' -> KeyEvent.KEYCODE_E
                'f' -> KeyEvent.KEYCODE_F
                'g' -> KeyEvent.KEYCODE_G
                'h' -> KeyEvent.KEYCODE_H
                'i' -> KeyEvent.KEYCODE_I
                'j' -> KeyEvent.KEYCODE_J
                'k' -> KeyEvent.KEYCODE_K
                'l' -> KeyEvent.KEYCODE_L
                'm' -> KeyEvent.KEYCODE_M
                'n' -> KeyEvent.KEYCODE_N
                'o' -> KeyEvent.KEYCODE_O
                'p' -> KeyEvent.KEYCODE_P
                'q' -> KeyEvent.KEYCODE_Q
                'r' -> KeyEvent.KEYCODE_R
                's' -> KeyEvent.KEYCODE_S
                't' -> KeyEvent.KEYCODE_T
                'u' -> KeyEvent.KEYCODE_U
                'v' -> KeyEvent.KEYCODE_V
                'w' -> KeyEvent.KEYCODE_W
                'x' -> KeyEvent.KEYCODE_X
                'y' -> KeyEvent.KEYCODE_Y
                'z' -> KeyEvent.KEYCODE_Z
                '0', ')' -> KeyEvent.KEYCODE_0
                '1', '!' -> KeyEvent.KEYCODE_1
                '2', '@' -> KeyEvent.KEYCODE_2
                '3', '#' -> KeyEvent.KEYCODE_3
                '4', '$' -> KeyEvent.KEYCODE_4
                '5', '%' -> KeyEvent.KEYCODE_5
                '6', '^' -> KeyEvent.KEYCODE_6
                '7', '&' -> KeyEvent.KEYCODE_7
                '8', '*' -> KeyEvent.KEYCODE_8
                '9', '(' -> KeyEvent.KEYCODE_9
                ' ' -> KeyEvent.KEYCODE_SPACE
                '\n' -> KeyEvent.KEYCODE_ENTER
                '-', '_' -> KeyEvent.KEYCODE_MINUS
                '=', '+' -> KeyEvent.KEYCODE_EQUALS
                '[', '{' -> KeyEvent.KEYCODE_LEFT_BRACKET
                ']', '}' -> KeyEvent.KEYCODE_RIGHT_BRACKET
                '\\', '|' -> KeyEvent.KEYCODE_BACKSLASH
                ';', ':' -> KeyEvent.KEYCODE_SEMICOLON
                '\'', '"' -> KeyEvent.KEYCODE_APOSTROPHE
                ',', '<' -> KeyEvent.KEYCODE_COMMA
                '.', '>' -> KeyEvent.KEYCODE_PERIOD
                '/', '?' -> KeyEvent.KEYCODE_SLASH
                '`', '~' -> KeyEvent.KEYCODE_GRAVE
                else -> null
            } ?: return null

        return KeyDispatch(keyCode = keyCode, requiresShift = isUppercaseLetter || char in SHIFTED_SYMBOLS)
    }

    fun showKeyboard() {
        post {
            imeSessionActive = true
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            val imm = displayContext.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun hideKeyboard() {
        post {
            val imm = displayContext.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(windowToken, 0)
            clearFocus()
            imeSessionActive = false
            isFocusable = false
            isFocusableInTouchMode = false
        }
    }
}
