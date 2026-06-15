package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.playSoundAtPlayer
import cn.hkim.addon.utils.render.nvg.NVGPIPRenderer
import cn.hkim.addon.utils.render.nvg.NVGRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import java.awt.Color

class ActionSetting(name: String, desc: String, val action: () -> Unit) : Setting<Unit>(name, desc) {
    override val default: Unit = Unit
    override var value: Unit = Unit
    fun execute() = action()
    init { noSave() }

    override fun render(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int,
        delta: Float
    ): Float {
        val height = 20f
        val isHovered = HudUtils.isPointInRect(mouseX, mouseY, x, y, width, height)

        if (isHovered) {
            NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
                NVGRenderer.rect(x * 2, y * 2, width * 2, height * 2, Color(0x15FFFFFF, true), 6f)
            }
        }

        graphics.text(mc.font, name, x.toInt() + 10, y.toInt() + 6, 0xFFCCCCCC.toInt(), false)

        val btnX = x + width - 60f
        val btnY = y + 2f
        val btnW = 50f
        val btnH = 16f

        val isBtnHovered = HudUtils.isPointInRect(mouseX, mouseY, btnX, btnY, btnW, btnH)
        val btnColor = if (isBtnHovered) themeColor else 0xFF555555.toInt()

        NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
            NVGRenderer.rect(btnX * 2, btnY * 2, btnW * 2, btnH * 2, Color(0x3A3A3A), 6f)
            NVGRenderer.hollowRect(btnX * 2, btnY * 2, btnW * 2, btnH * 2, 2f, Color(btnColor), 6f)
        }

        graphics.text(mc.font, "Execute", (btnX + btnW / 2 - mc.font.width("Execute") / 2).toInt(), btnY.toInt() + 4, 0xFFFFFFFF.toInt(), false)

        renderDescriptionTooltip(graphics, isHovered, mouseX, mouseY)
        return height
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, x: Float, y: Float, width: Float): Boolean {
        if (button != 0) return false

        val btnX = x + width - 60f
        val btnY = y + 2f
        val btnW = 50f
        val btnH = 16f

        if (HudUtils.isPointInRect(mouseX, mouseY, btnX, btnY, btnW, btnH)) {
            execute()
            playSoundAtPlayer(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f)
            return true
        }
        return false
    }
}