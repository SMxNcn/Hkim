package cn.hkim.addon.config.settings

import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoCenteredText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import org.lwjgl.sdl.SDLMouse
import org.lwjgl.sdl.SDLScancode

class KeybindSetting(name: String, desc: String, defaultKey: Int = SDLScancode.SDL_SCANCODE_UNKNOWN) : Setting<Int>(name, desc, defaultKey) {
    override var value: Int = defaultKey

    internal var isBinding = false

    override fun render(
        graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float, themeColor: Int,
        delta: Float, visibleTop: Float, visibleBottom: Float
    ): Float {
        val height = Theme.SETTING_HEIGHT
        val isHovered = computeIsHovered(mouseX, mouseY, x, y, width, height, visibleTop, visibleBottom)

        if (isHovered || isBinding) {
            graphics.drawRoundedRectWithBorder(x, y, width, height, Theme.controlHover, 0, 0f, 3f)
        }

        graphics.drawSkikoText(name, x + 10f, y + 3f, Theme.CARD_FONT_SIZE, Theme.controlText)

        val btnX = x + width - 80f
        val btnY = y + 2f
        val btnW = 70f
        val btnH = 14f

        val isBtnHovered = HudUtils.isPointInRect(mouseX, mouseY, btnX, btnY, btnW, btnH)
        val displayText = when {
            isBinding -> "Press..."
            value == SDLScancode.SDL_SCANCODE_UNKNOWN -> "None"
            else -> getKeyDisplayName(value)
        }

        val btnColor = if (isBinding || isBtnHovered) themeColor else Theme.controlBorderHover
        graphics.drawRoundedRectWithBorder(btnX, btnY, btnW, btnH, Theme.controlButtonBg, btnColor, 1f, 3f)

        graphics.drawSkikoCenteredText(displayText, btnX + btnW / 2f, btnY + 1.5f, Theme.CARD_FONT_SIZE, Theme.controlTextActive)

        if (isBtnHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float, doubleClick: Boolean): Boolean {
        if (isBinding) return true

        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false
        val btnX = x + width - 80f
        val btnY = y + 2f
        val btnW = 70f
        val btnH = 14f

        if (HudUtils.isPointInRect(mouseX, mouseY, btnX, btnY, btnW, btnH)) {
            isBinding = true
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            return true
        }
        return false
    }

    fun handleKey(keyCode: Int): Boolean {
        if (!isBinding) return false

        if (keyCode == InputConstants.KEY_ESCAPE) {
            value = SDLScancode.SDL_SCANCODE_UNKNOWN
            isBinding = false
            settingsChanged()
            return true
        }

        if (keyCode == SDLScancode.SDL_SCANCODE_UNKNOWN) return false

        value = keyCode
        isBinding = false
        settingsChanged()
        playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
        return true
    }

    fun handleMouseButton(button: Int): Boolean {
        if (!isBinding) return false

        if (button == InputConstants.MOUSE_BUTTON_LEFT || button == InputConstants.MOUSE_BUTTON_RIGHT) {
            isBinding = false
            return true
        }

        value = button
        isBinding = false
        settingsChanged()
        playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
        return true
    }

    private fun getKeyDisplayName(keyCode: Int): String {
        if (keyCode == SDLScancode.SDL_SCANCODE_UNKNOWN) return "None"

        if (keyCode in 1..8) {
            return try {
                val key = InputConstants.Type.MOUSE.getOrCreate(keyCode)
                key.displayName.string
            } catch (_: Exception) {
                getMouseButtonFallbackName(keyCode)
            }
        }

        return try {
            val key = InputConstants.Type.KEYBOARD.getOrCreate(keyCode)
            key.displayName.string
        } catch (_: Exception) {
            "Key $keyCode"
        }
    }

    private fun getMouseButtonFallbackName(button: Int): String {
        return when (button) {
            SDLMouse.SDL_BUTTON_MIDDLE -> "Mouse Middle"
            else -> "Mouse $button"
        }
    }

    companion object {
        private val keyCodeToName: Map<Int, String> by lazy {
            buildMap {
                SDLScancode::class.java.declaredFields
                    .filter { f -> f.type == Int::class.javaPrimitiveType && f.name.startsWith("SDL_SCANCODE_") }
                    .forEach { f ->
                        try {
                            f.isAccessible = true
                            put(f.getInt(null), f.name.removePrefix("SDL_SCANCODE_"))
                        } catch (_: Exception) { }
                    }
                SDLMouse::class.java.declaredFields
                    .filter { f ->
                        f.type == Int::class.javaPrimitiveType &&
                            f.name.startsWith("SDL_BUTTON_") &&
                            !f.name.endsWith("MASK")
                    }
                    .forEach { f ->
                        try {
                            f.isAccessible = true
                            put(f.getInt(null), f.name.removePrefix("SDL_BUTTON_"))
                        } catch (_: Exception) { }
                    }
            }
        }

        private val nameToKeyCode: Map<String, Int> by lazy {
            keyCodeToName.entries.associate { (code, name) -> name to code }
        }

        @JvmStatic
        fun keyCodeToSdlName(code: Int): String {
            return keyCodeToName[code] ?: code.toString()
        }

        @JvmStatic
        fun sdlNameToKeyCode(name: String): Int {
            return nameToKeyCode[name]
                ?: run {
                    val legacyName = when {
                        name.startsWith("GLFW_KEY_") -> "SDL_SCANCODE_" + name.removePrefix("GLFW_KEY_")
                        name.startsWith("GLFW_MOUSE_BUTTON_") -> "SDL_BUTTON_" + name.removePrefix("GLFW_MOUSE_BUTTON_")
                        else -> null
                    }
                    legacyName?.let { nameToKeyCode[it] }
                }
                ?: SDLScancode.SDL_SCANCODE_UNKNOWN
        }
    }
}