package cn.hkim.addon.gui

import cn.hkim.addon.Hkim.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class ClientButton(
    x: Int, y: Int,
    width: Int, height: Int,
    private val component: Component,
    onPress: OnPress,
): Button(x, y, width, height, component, onPress, DEFAULT_NARRATION) {
    private val normalTex: Identifier = Identifier.fromNamespaceAndPath("hkim", "button")
    private val hoveredTex: Identifier = Identifier.fromNamespaceAndPath("hkim", "button_highlighted")

    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, f: Float) {
        if (isHovered) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hoveredTex, this.x, this.y, this.width, this.height)
        else graphics.blitSprite(RenderPipelines.GUI_TEXTURED, normalTex, this.x, this.y, this.width, this.height)

        val isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height
        val textColor = if (isHovered) 0xFFFFFFFF.toInt() else 0xFFE0E0E0.toInt()
        graphics.centeredText(mc.font, component, this.x + this.width / 2, this.y + (this.height - mc.font.lineHeight) / 2 + 1, textColor)
    }
}