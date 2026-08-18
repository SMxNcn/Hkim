package cn.hkim.addon.config.settings

import cn.hkim.addon.config.Setting
import cn.hkim.addon.config.clickgui.Theme
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawRoundedRectWithBorder
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoCenteredText
import cn.hkim.addon.utils.render.skiko.SkikoDraw.drawSkikoText
import cn.hkim.addon.utils.startsWithOneOf
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import org.lwjgl.glfw.GLFW

class KeybindSetting(name: String, desc: String, defaultKey: Int = GLFW.GLFW_KEY_UNKNOWN) : Setting<Int>(name, desc, defaultKey) {
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
            value == GLFW.GLFW_KEY_UNKNOWN -> "None"
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

        if (button != 0) return false
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

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            value = GLFW.GLFW_KEY_UNKNOWN
            isBinding = false
            settingsChanged()
            return true
        }

        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return false

        value = keyCode
        isBinding = false
        settingsChanged()
        playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
        return true
    }

    fun handleMouseButton(button: Int): Boolean {
        if (!isBinding) return false

        if (button <= 1) {
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
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return "None"

        if (keyCode in 0..7) {
            return try {
                val key = InputConstants.Type.MOUSE.getOrCreate(keyCode)
                key.displayName.string
            } catch (_: Exception) {
                getMouseButtonFallbackName(keyCode)
            }
        }

        return try {
            val key = InputConstants.Type.KEYSYM.getOrCreate(keyCode)
            key.displayName.string
        } catch (_: Exception) {
            "Key $keyCode"
        }
    }

    private fun getMouseButtonFallbackName(button: Int): String {
        return when (button) {
            2 -> "Mouse Middle"
            else -> "Mouse ${button + 1}"
        }
    }

    companion object {
        private val keyCodeToName: Map<Int, String> by lazy {
            GLFW::class.java.declaredFields
                .filter { f -> f.type == Int::class.javaPrimitiveType && f.name.startsWithOneOf("GLFW_KEY_", "GLFW_MOUSE_BUTTON_") }
                .mapNotNull { f ->
                    try {
                        f.isAccessible = true
                        val code = f.getInt(null)
                        val name = f.name.removePrefix("GLFW_")
                        code to name
                    } catch (_: Exception) { null }
                }
                .distinctBy { (code, _) -> code }
                .toMap()
        }

        private val nameToKeyCode: Map<String, Int> by lazy {
            keyCodeToName.entries.associate { (code, name) -> name to code }
        }

        @JvmStatic
        fun keyCodeToGlfwName(code: Int): String {
            return keyCodeToName[code] ?: code.toString()
        }

        @JvmStatic
        fun glfwNameToKeyCode(name: String): Int {
            return nameToKeyCode[name] ?: GLFW.GLFW_KEY_UNKNOWN
        }
    }
}