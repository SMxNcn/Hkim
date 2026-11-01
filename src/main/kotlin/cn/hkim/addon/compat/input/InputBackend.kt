package cn.hkim.addon.compat.input

import cn.hkim.addon.Hkim
import com.mojang.blaze3d.platform.InputConstants
//? if 26.2 {
import org.lwjgl.glfw.GLFW
//? } else {
/*import org.lwjgl.sdl.SDLClipboard
import org.lwjgl.sdl.SDLMouse
import org.lwjgl.sdl.SDLScancode
*///? }

/**
 * Version specific input backend. Stonecutter keeps one branch:
 *  - 26.2: GLFW
 *  - 26.3+: SDL3
 */
internal object InputBackend {
    //? if 26.2 {
    val KEY_NONE = GLFW.GLFW_KEY_UNKNOWN
    //? } else {
    //val KEY_NONE = SDLScancode.SDL_SCANCODE_UNKNOWN
    //? }

    val KEY_ESCAPE = InputConstants.KEY_ESCAPE
    val KEY_TAB = InputConstants.KEY_TAB
    val KEY_HOME = InputConstants.KEY_HOME
    val KEY_END = InputConstants.KEY_END
    val KEY_BACKSPACE = InputConstants.KEY_BACKSPACE
    val KEY_DELETE = InputConstants.KEY_DELETE
    val KEY_LEFT = InputConstants.KEY_LEFT
    val KEY_RIGHT = InputConstants.KEY_RIGHT
    val KEY_UP = InputConstants.KEY_UP
    val KEY_DOWN = InputConstants.KEY_DOWN
    val KEY_LSHIFT = InputConstants.KEY_LSHIFT
    val KEY_RSHIFT = InputConstants.KEY_RSHIFT
    val KEY_LCONTROL = InputConstants.KEY_LCONTROL
    val KEY_RCONTROL = InputConstants.KEY_RCONTROL
    val KEY_RALT = InputConstants.KEY_RALT
    val KEY_A = InputConstants.KEY_A
    val KEY_D = InputConstants.KEY_D
    val KEY_F5 = InputConstants.KEY_F5
    val KEY_F6 = InputConstants.KEY_F6
    val KEY_K = InputConstants.KEY_K
    val KEY_R = InputConstants.KEY_R
    val KEY_S = InputConstants.KEY_S
    val KEY_V = InputConstants.KEY_V
    val KEY_W = InputConstants.KEY_W
    val KEY_X = InputConstants.KEY_X

    val MOUSE_LEFT = InputConstants.MOUSE_BUTTON_LEFT
    val MOUSE_RIGHT = InputConstants.MOUSE_BUTTON_RIGHT
    val MOUSE_MIDDLE = InputConstants.MOUSE_BUTTON_MIDDLE
    val MOUSE_X1 = InputConstants.MOUSE_BUTTON_4
    val MOUSE_X2 = InputConstants.MOUSE_BUTTON_5

    fun isKeyDown(keyCode: Int): Boolean {
        if (keyCode == KEY_NONE) return false
        //? if 26.2 {
        return GLFW.glfwGetKey(Hkim.mc.window.handle(), keyCode) == GLFW.GLFW_PRESS
        //? } else {
        //return InputConstants.isKeyDown(keyCode)
        //? }
    }

    fun isMouseButtonDown(button: Int): Boolean {
        //? if 26.2 {
        if (button !in 0..7) return false
        return GLFW.glfwGetMouseButton(Hkim.mc.window.handle(), button) == GLFW.GLFW_PRESS
        //? } else {
        /*val mask = when (button) {
            SDLMouse.SDL_BUTTON_LEFT -> SDLMouse.SDL_BUTTON_LMASK
            SDLMouse.SDL_BUTTON_MIDDLE -> SDLMouse.SDL_BUTTON_MMASK
            SDLMouse.SDL_BUTTON_RIGHT -> SDLMouse.SDL_BUTTON_RMASK
            SDLMouse.SDL_BUTTON_X1 -> SDLMouse.SDL_BUTTON_X1MASK
            SDLMouse.SDL_BUTTON_X2 -> SDLMouse.SDL_BUTTON_X2MASK
            else -> return false
        }
        return SDLMouse.nSDL_GetMouseState(0L, 0L) and mask != 0
        *///? }
    }

    fun mouseButton(button: Int): MouseButton = when (button) {
        InputConstants.MOUSE_BUTTON_LEFT -> MouseButton.LEFT
        InputConstants.MOUSE_BUTTON_RIGHT -> MouseButton.RIGHT
        InputConstants.MOUSE_BUTTON_MIDDLE -> MouseButton.MIDDLE
        InputConstants.MOUSE_BUTTON_4 -> MouseButton.X1
        InputConstants.MOUSE_BUTTON_5 -> MouseButton.X2
        else -> MouseButton.OTHER
    }

    fun isKeyboard(key: InputConstants.Key): Boolean {
        //? if 26.2 {
        return key.type == InputConstants.Type.KEYSYM || key.type == InputConstants.Type.SCANCODE
        //? } else {
        //return key.type == InputConstants.Type.KEYBOARD
        //? }
    }

    fun shiftPressed(modifiers: Int): Boolean {
        //? if 26.2 {
        return modifiers and GLFW.GLFW_MOD_SHIFT != 0
        //? } else {
        //return modifiers and InputConstants.MOD_SHIFT != 0
        //? }
    }

    private val keyboardType: InputConstants.Type
        get() {
            //? if 26.2 {
            return InputConstants.Type.KEYSYM
            //? } else {
            //return InputConstants.Type.KEYBOARD
            //? }
        }

    fun keyDisplayName(code: Int, type: KeyType): String {
        if (code == KEY_NONE) return "None"
        return try {
            val mcType = if (type == KeyType.MOUSE) InputConstants.Type.MOUSE else keyboardType
            mcType.getOrCreate(code).displayName.string
        } catch (_: Exception) {
            if (type == KeyType.MOUSE) "Mouse ${code + 1}" else "Key $code"
        }
    }

    fun keyName(code: Int, type: KeyType): String {
        if (code == KEY_NONE) return "key.keyboard.unknown"
        val mcType = if (type == KeyType.MOUSE) InputConstants.Type.MOUSE else keyboardType
        return mcType.getOrCreate(code).name
    }

    fun keyCode(name: String): InputBinding? {
        if (name.isBlank()) return null

        val mcKey = try {
            InputConstants.getKey(name)
        } catch (_: Exception) {
            null
        }
        if (mcKey != null && mcKey != InputConstants.UNKNOWN) {
            val type = if (mcKey.type == InputConstants.Type.MOUSE) KeyType.MOUSE else KeyType.KEYBOARD
            return InputBinding(mcKey.value, type)
        }

        return legacyNames[name]
    }

    private val legacyNames: Map<String, InputBinding> by lazy {
        //? if 26.2 {
        buildMap {
            GLFW::class.java.declaredFields
                .filter { field ->
                    field.type == Int::class.javaPrimitiveType &&
                        (field.name.startsWith("GLFW_KEY_") || field.name.startsWith("GLFW_MOUSE_BUTTON_"))
                }
                .forEach { field ->
                    try {
                        field.isAccessible = true
                        val code = field.getInt(null)
                        val type =
                            if (field.name.startsWith("GLFW_MOUSE_BUTTON_")) KeyType.MOUSE else KeyType.KEYBOARD
                        put(field.name.removePrefix("GLFW_"), InputBinding(code, type))
                    } catch (_: Exception) {
                    }
                }
        }
        //? } else {
        /*buildMap {
            SDLScancode::class.java.declaredFields
                .filter { field -> field.type == Int::class.javaPrimitiveType && field.name.startsWith("SDL_SCANCODE_") }
                .forEach { field ->
                    try {
                        field.isAccessible = true
                        put(field.name.removePrefix("SDL_"), InputBinding(field.getInt(null), KeyType.KEYBOARD))
                    } catch (_: Exception) {
                    }
                }
            SDLMouse::class.java.declaredFields
                .filter { field ->
                    field.type == Int::class.javaPrimitiveType &&
                        field.name.startsWith("SDL_BUTTON_") &&
                        !field.name.endsWith("MASK")
                }
                .forEach { field ->
                    try {
                        field.isAccessible = true
                        put(field.name.removePrefix("SDL_"), InputBinding(field.getInt(null), KeyType.MOUSE))
                    } catch (_: Exception) {
                    }
                }
        }
        *///? }
    }

    fun getClipboard(): String? {
        //? if 26.2 {
        return GLFW.glfwGetClipboardString(Hkim.mc.window.handle())
        //? } else {
        //return SDLClipboard.SDL_GetClipboardText()
        //? }
    }

    fun setClipboard(text: String) {
        //? if 26.2 {
        GLFW.glfwSetClipboardString(Hkim.mc.window.handle(), text)
        //? } else {
        //SDLClipboard.SDL_SetClipboardText(text)
        //? }
    }
}
