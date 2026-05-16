package cn.hkim.addon.gui

import cn.hkim.addon.Hkim.mc
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

    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, f: Float) {
        val bgColor = if (isHovered) 0x43AAAAAA else 0x432A2A2A
        val borderColor = if (isHovered) 0x43AAAAAA else 0x33969696

        NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
            NVGRenderer.rect(x * 2f, y * 2f, width * 2f, height * 2f, Color(bgColor, true), 6f)
            NVGRenderer.hollowRect(x * 2f, y * 2f, width * 2f, height * 2f, 2.5f, Color(borderColor, true), 6f)
        }

        val isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height
        val textColor = if (isHovered) 0xFFFFFFFF.toInt() else 0xFFE0E0E0.toInt()
        graphics.centeredText(mc.font, component, this.x + this.width / 2, this.y + (this.height - mc.font.lineHeight) / 2 + 1, textColor)
    }
}