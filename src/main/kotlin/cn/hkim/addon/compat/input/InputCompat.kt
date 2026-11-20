package cn.hkim.addon.compat.input

import com.mojang.blaze3d.platform.InputConstants

/**
 * Which device a keybind belongs to.
 *
 * The numeric code alone is ambiguous: on 26.2 (GLFW) mouse button 4 is `3`,
 * while on 26.3 (SDL) it is `4` — which also happens to be the SDL scancode
 * for `A`. Keybinds therefore always carry their device type alongside the code.
 */
enum class KeyType {
    KEYBOARD,
    MOUSE,
}

enum class MouseButton {
    LEFT,
    RIGHT,
    MIDDLE,
    X1,
    X2,
    OTHER,
}

data class InputBinding(val code: Int, val type: KeyType)

/**
 * Version independent input API.
 *
 * All call sites in Hkim should use this instead of touching GLFW/SDL or the
 * numeric constants of [InputConstants] directly. There is exactly one
 * [InputBackend] compiled per Minecraft version:
 *
 *  - 26.2: `src/main/.../InputBackend.kt` (GLFW)
 *  - 26.3: `versions/26.3/src/main/.../InputBackend.kt` (SDL)
 *
 * The two backends expose the same surface, so shared code never needs to know
 * which input library the current Minecraft version uses.
 */
object InputCompat {
    val KEY_NONE get() = NONE

    private const val MOUSE_FLAG = 0x10000
    private const val NONE = Int.MIN_VALUE
    val KEY_ESCAPE get() = InputBackend.KEY_ESCAPE
    val KEY_TAB get() = InputBackend.KEY_TAB
    val KEY_HOME get() = InputBackend.KEY_HOME
    val KEY_END get() = InputBackend.KEY_END
    val KEY_BACKSPACE get() = InputBackend.KEY_BACKSPACE
    val KEY_DELETE get() = InputBackend.KEY_DELETE
    val KEY_LEFT get() = InputBackend.KEY_LEFT
    val KEY_RIGHT get() = InputBackend.KEY_RIGHT
    val KEY_UP get() = InputBackend.KEY_UP
    val KEY_DOWN get() = InputBackend.KEY_DOWN
    val KEY_LSHIFT get() = InputBackend.KEY_LSHIFT
    val KEY_RSHIFT get() = InputBackend.KEY_RSHIFT
    val KEY_LCONTROL get() = InputBackend.KEY_LCONTROL
    val KEY_RCONTROL get() = InputBackend.KEY_RCONTROL
    val KEY_LALT get() = InputBackend.KEY_LALT
    val KEY_RALT get() = InputBackend.KEY_RALT
    val KEY_A get() = InputBackend.KEY_A
    val KEY_D get() = InputBackend.KEY_D
    val KEY_F5 get() = InputBackend.KEY_F5
    val KEY_F6 get() = InputBackend.KEY_F6
    val KEY_K get() = InputBackend.KEY_K
    val KEY_R get() = InputBackend.KEY_R
    val KEY_S get() = InputBackend.KEY_S
    val KEY_V get() = InputBackend.KEY_V
    val KEY_W get() = InputBackend.KEY_W
    val KEY_X get() = InputBackend.KEY_X

    // ---------------------------------------------------------------------
    // Mouse buttons (version native codes)
    // ---------------------------------------------------------------------
    val MOUSE_LEFT get() = InputBackend.MOUSE_LEFT
    val MOUSE_RIGHT get() = InputBackend.MOUSE_RIGHT
    val MOUSE_MIDDLE get() = InputBackend.MOUSE_MIDDLE
    val MOUSE_X1 get() = InputBackend.MOUSE_X1
    val MOUSE_X2 get() = InputBackend.MOUSE_X2

    const val PRESS = 1
    const val RELEASE = 0

    fun encode(code: Int, type: KeyType): Int {
        if (code == NONE || code == InputBackend.KEY_NONE) return KEY_NONE
        return if (type == KeyType.MOUSE) code or MOUSE_FLAG else code
    }

    fun encode(key: InputConstants.Key): Int = when {
        isKeyboard(key) -> encode(key.value, KeyType.KEYBOARD)
        key.type == InputConstants.Type.MOUSE -> encode(key.value, KeyType.MOUSE)
        else -> KEY_NONE
    }

    fun decodeCode(code: Int): Int {
        if (code == KEY_NONE) return InputBackend.KEY_NONE
        return if (code and MOUSE_FLAG != 0) code and MOUSE_FLAG.inv() else code
    }

    fun decodeType(code: Int): KeyType =
        if (code and MOUSE_FLAG != 0) KeyType.MOUSE else KeyType.KEYBOARD

    // ---------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------
    fun isKeyDown(keyCode: Int): Boolean {
        if (keyCode == KEY_NONE) return false
        return InputBackend.isKeyDown(decodeCode(keyCode))
    }

    fun isShiftDown(): Boolean = isKeyDown(KEY_LSHIFT) || isKeyDown(KEY_RSHIFT)

    fun isCtrlDown(): Boolean = isKeyDown(KEY_LCONTROL) || isKeyDown(KEY_RCONTROL)

    fun isAltDown(): Boolean = isKeyDown(KEY_LALT) || isKeyDown(KEY_RALT)

    fun isMouseButtonDown(button: Int): Boolean {
        if (button == KEY_NONE) return false
        return InputBackend.isMouseButtonDown(decodeCode(button))
    }

    fun mouseButton(button: Int): MouseButton = InputBackend.mouseButton(button)

    fun isPrimaryMouseButton(button: Int): Boolean = when (mouseButton(button)) {
        MouseButton.LEFT, MouseButton.RIGHT, MouseButton.MIDDLE -> true
        else -> false
    }

    fun isKeyboard(key: InputConstants.Key): Boolean = InputBackend.isKeyboard(key)

    fun isMouse(key: InputConstants.Key): Boolean = key.type == InputConstants.Type.MOUSE

    /** True if [modifiers] (as reported by [net.minecraft.client.input.KeyEvent]) has shift held. */
    fun shiftPressed(modifiers: Int): Boolean = InputBackend.shiftPressed(modifiers)

    fun matches(key: InputConstants.Key, code: Int): Boolean =
        code != KEY_NONE && encode(key) == code

    fun matchesKeyboard(keyCode: Int, code: Int): Boolean =
        code != KEY_NONE && encode(keyCode, KeyType.KEYBOARD) == code

    fun matches(key: InputConstants.Key, code: Int, type: KeyType): Boolean =
        code != KEY_NONE && decodeType(code) == type && encode(key) == code

    // ---------------------------------------------------------------------
    // Naming / (de)serialization
    // ---------------------------------------------------------------------
    fun keyDisplayName(code: Int, type: KeyType): String {
        if (code == KEY_NONE) return "None"
        return InputBackend.keyDisplayName(decodeCode(code), type)
    }

    /** Stable, cross-version name (e.g. `key.keyboard.x`, `key.mouse.4`). */
    fun keyName(code: Int, type: KeyType): String = InputBackend.keyName(code, type)

    /** Parse both the stable names and legacy GLFW/SDL names. */
    fun keyCode(name: String): InputBinding? = InputBackend.keyCode(name)

    // ---------------------------------------------------------------------
    // Clipboard (GLFW on 26.2, SDL on 26.3)
    // ---------------------------------------------------------------------
    fun getClipboard(): String? = InputBackend.getClipboard()

    fun setClipboard(text: String) = InputBackend.setClipboard(text)
}
