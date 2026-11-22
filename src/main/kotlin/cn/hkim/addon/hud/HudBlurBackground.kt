package cn.hkim.addon.hud

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.gui.screen.HudEditScreen
import net.minecraft.client.gui.GuiGraphicsExtractor

object HudBlurBackground {
    private val renderers = LinkedHashMap<Any, (GuiGraphicsExtractor) -> Unit>()


    fun register(key: Any, renderer: (GuiGraphicsExtractor) -> Unit) {
        renderers[key] = renderer
    }

    fun unregister(key: Any) {
        renderers.remove(key)
    }

    @JvmStatic
    fun render(graphics: GuiGraphicsExtractor) {
        if (mc.gui.hud.isHidden) return
        if (mc.gui.screen() is HudEditScreen) return
        renderers.values.forEach { it(graphics) }
    }

    @JvmStatic
    fun renderEditor(graphics: GuiGraphicsExtractor) {
        renderers.values.forEach { it(graphics) }
    }
}
