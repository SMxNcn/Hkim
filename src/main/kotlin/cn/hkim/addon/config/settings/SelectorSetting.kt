package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.nvg.NVGPIPRenderer
import cn.hkim.addon.utils.render.nvg.NVGRenderer
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import java.awt.Color

class SelectorSetting(name: String, desc: String, val options: List<String>, default: String) : Setting<Int>(name, desc, options.indexOf(default).coerceAtLeast(0)) {
    private var _selected: String = options.firstOrNull() ?: ""
    fun select(option: String) { if (option in options) { _selected = option; value = options.indexOf(option) } }
    fun getSelected(): String = _selected

    override fun render(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int,
        delta: Float, visibleTop: Float, visibleBottom: Float
    ): Float {
        val height = 20f
        val isHovered = (visibleTop == -1f || mouseY in visibleTop..visibleBottom) && HudUtils.isPointInRect(mouseX, mouseY, x, y, width, height)

        if (isHovered) {
            NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
                NVGRenderer.rect(x * 2, y * 2, width * 2, height * 2, Color(0x15FFFFFF, true), 6f)
            }
        }

        graphics.text(mc.font, name, x.toInt() + 10, y.toInt() + 6, 0xFFCCCCCC.toInt(), false)

        val selectorX = x + width - 100f
        val selectorW = 90f
        val selectorY = y + 2f
        val selectorH = 16f

        NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
            NVGRenderer.rect(selectorX * 2, selectorY * 2, selectorW * 2, selectorH * 2, Color(0x222222), 6f)
            NVGRenderer.hollowRect(selectorX * 2, selectorY * 2, selectorW * 2, selectorH * 2, 2f, Color(0x444444), 6f)
        }

        val leftArrowHovered = HudUtils.isPointInRect(mouseX, mouseY, selectorX, selectorY, 14f, selectorH)
        val rightArrowHovered = HudUtils.isPointInRect(mouseX, mouseY, selectorX + selectorW - 16f, selectorY, 14f, selectorH)
        val leftArrowColor = if (leftArrowHovered) themeColor else 0xFF888888.toInt()
        val rightArrowColor = if (rightArrowHovered) themeColor else 0xFF888888.toInt()
        val optionsX = (selectorX + selectorW / 2).toInt()

        graphics.text(mc.font, "<", selectorX.toInt() + 6, selectorY.toInt() + 4, leftArrowColor, false)
        graphics.text(mc.font, ">", (selectorX + selectorW - 12).toInt(), selectorY.toInt() + 4, rightArrowColor, false)

        graphics.centeredText(mc.font, _selected, optionsX, selectorY.toInt() + 4, 0xFFFFFFFF.toInt())

        if (leftArrowHovered || rightArrowHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (button != 0) return false

        val selectorX = x + width - 100f
        val selectorW = 90f
        val selectorY = y + 2f
        val selectorH = 16f

        if (HudUtils.isPointInRect(mouseX, mouseY, selectorX, selectorY, 14f, selectorH)) {
            return previous()
        }

        if (HudUtils.isPointInRect(mouseX, mouseY, selectorX + selectorW - 16f, selectorY, 14f, selectorH)) {
            return next()
        }
        return false
    }

    private fun next(): Boolean {
        val nextIndex = (value + 1) % options.size
        select(options[nextIndex])
        playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
        settingsChanged()
        return true
    }

    private fun previous(): Boolean {
        val prevIndex = if (value - 1 < 0) options.size - 1 else value - 1
        select(options[prevIndex])
        playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
        settingsChanged()
        return true
    }
}