package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.HudUtils.drawRectWithBorder
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import java.awt.Color

@ModuleInfo("hurt_cam", Category.RENDER, true)
object HurtCamera : Module("Hurt Camera", "Renders hurt overlay effect.") {
    private val hurtLayer: Identifier = Identifier.fromNamespaceAndPath("hkim", "hurt")

    init {
        HudElementRegistry.attachElementBefore(VanillaHudElements.SLEEP, hurtLayer, this::render)
    }

    override fun render(graphics: GuiGraphicsExtractor, tickTracker: DeltaTracker) {
        val hurtTime = mc.player?.hurtTime ?: return
        if (hurtTime <= 0 || !enabled || mc.options.hideGui) return
        val alpha = hurtTime.times(25.5f).toInt()
        graphics.drawRectWithBorder(0f, 0f, mc.window.guiScaledWidth.toFloat(), mc.window.guiScaledHeight.toFloat(), 0, Color(255, 0, 0, alpha).rgb, 2)
        super.render(graphics, tickTracker)
    }
}