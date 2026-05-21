package cn.hkim.addon.gui

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.utils.HudUtils
import cn.hkim.addon.utils.render.Easing
import cn.hkim.addon.utils.render.GuiAnimation
import cn.hkim.addon.utils.render.nvg.NVGPIPRenderer
import cn.hkim.addon.utils.render.nvg.NVGRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import java.awt.Color

class ClientButton(
    x: Int, y: Int,
    width: Int, height: Int,
    private val component: Component,
    onPress: OnPress,
): Button(x, y, width, height, component, onPress, DEFAULT_NARRATION) {

    private val hoverAnim = GuiAnimation.create()
        .duration(150L)
        .easing(Easing.CUBIC_OUT)

    private var wasHovered = false

    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, f: Float) {
        val hovered = isHovered
        if (hovered != wasHovered) {
            wasHovered = hovered
            hoverAnim.animateTo(if (hovered) 1f else 0f)
        }

        val progress = hoverAnim.getValue()
        val bgColor = HudUtils.lerpColor(0x432A2A2A, 0x43AAAAAA, progress)
        val borderColor = HudUtils.lerpColor(0x33969696, 0x43AAAAAA, progress)
        val textColor = HudUtils.lerpColor(0xFFE0E0E0.toInt(), 0xFFFFFFFF.toInt(), progress)

        NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
            NVGRenderer.rect(x * 2f, y * 2f, width * 2f, height * 2f, Color(bgColor, true), 6f)
            NVGRenderer.hollowRect(x * 2f, y * 2f, width * 2f, height * 2f, 2.5f, Color(borderColor, true), 6f)
        }

        graphics.centeredText(mc.font, component, this.x + this.width / 2, this.y + (this.height - mc.font.lineHeight) / 2 + 1, textColor)
    }
}