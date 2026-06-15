package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.nvg.NVGPIPRenderer
import cn.hkim.addon.utils.render.nvg.NVGRenderer
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import org.lwjgl.glfw.GLFW
import java.awt.Color

class KeybindSetting(name: String, desc: String, defaultKey: Int = GLFW.GLFW_KEY_UNKNOWN) : Setting<Int>(name, desc) {
    override val default: Int = defaultKey
    override var value: Int = defaultKey

    internal var isBinding = false

    override fun render(
        graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float, themeColor: Int,
        delta: Float
    ): Float {
        val height = 20f
        val isHovered = HudUtils.isPointInRect(mouseX, mouseY, x, y, width, height)

        if (isHovered || isBinding) {
            NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
                NVGRenderer.rect(x * 2, y * 2, width * 2, height * 2, Color(0x15FFFFFF, true), 6f)
            }
        }

        graphics.text(mc.font, name, x.toInt() + 10, y.toInt() + 6, 0xFFCCCCCC.toInt(), false)

        val btnX = x + width - 80f
        val btnY = y + 2f
        val btnW = 70f
        val btnH = 16f

        val isBtnHovered = HudUtils.isPointInRect(mouseX, mouseY, btnX, btnY, btnW, btnH)
        val displayText = when {
            isBinding -> "Press..."
            value == GLFW.GLFW_KEY_UNKNOWN -> "None"
            else -> getKeyDisplayName(value)
        }

        val btnColor = if (isBinding || isBtnHovered) themeColor else 0xFF555555.toInt()

        NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
            NVGRenderer.rect(btnX * 2, btnY * 2, btnW * 2, btnH * 2, Color(0x3A3A3A), 6f)
            NVGRenderer.hollowRect(btnX * 2, btnY * 2, btnW * 2, btnH * 2, 2f, Color(btnColor), 6f)
        }

        graphics.text(mc.font, displayText, (btnX + btnW / 2 - mc.font.width(displayText) / 2).toInt(), btnY.toInt() + 4, 0xFFFFFFFF.toInt(), false)

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (button != 0) return false
        val btnX = x + width - 80f
        val btnY = y + 2f
        val btnW = 70f
        val btnH = 16f

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
            value = default
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

    private fun getKeyDisplayName(keyCode: Int): String {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return "None"
        return try {
            val key = InputConstants.Type.KEYSYM.getOrCreate(keyCode)
            key.displayName.string
        } catch (_: Exception) {
            "Key $keyCode"
        }
    }

    companion object {
        private val keyCodeToName: Map<Int, String> by lazy {
            GLFW::class.java.declaredFields
                .filter { f -> f.type == Int::class.javaPrimitiveType && f.name.startsWith("GLFW_KEY_") }
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