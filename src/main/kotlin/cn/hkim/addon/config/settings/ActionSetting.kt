package cn.hkim.addon.config.settings

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.Setting
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.HudUtils.drawRectWithBorder
import net.minecraft.client.gui.GuiGraphicsExtractor

class ActionSetting(name: String, desc: String, val action: () -> Unit) : Setting<Unit>(name, desc) {
    override val default: Unit = Unit
    override var value: Unit = Unit
    fun execute() = action()
    init { noSave() }

    override fun render(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float, width: Float,
        mouseX: Float, mouseY: Float,
        themeColor: Int
    ): Float {
        val height = 20f
        val isHovered = HudUtils.isPointInRect(mouseX, mouseY, x, y, width, height)

        if (isHovered) {
            graphics.fill(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), 0x15FFFFFF)
        }

        graphics.text(mc.font, name, x.toInt() + 10, y.toInt() + 6, 0xFFCCCCCC.toInt(), false)

        val btnX = x + width - 60f
        val btnY = y + 2f
        val btnW = 50f
        val btnH = 16f

        val isBtnHovered = HudUtils.isPointInRect(mouseX, mouseY, btnX, btnY, btnW, btnH)
        val btnColor = if (isBtnHovered) themeColor else 0xFF555555.toInt()
        graphics.drawRectWithBorder(btnX, btnY, btnW, btnH, 0xFF3A3A3A.toInt(), btnColor)

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
            return true
        }
        return false
    }
}