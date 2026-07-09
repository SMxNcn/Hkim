package cn.hkim.addon.utils.skyblock.inventory

import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.features.impl.SwapOptions
import meteordevelopment.orbit.EventHandler

object SwapHandler {
    var isInSwap = false
        private set

    fun startSwap() {
        isInSwap = true
    }

    fun endSwap() {
        isInSwap = false
    }

    @EventHandler
    fun onGuiOpen(event: GuiEvent.Open) {
        if (EquipmentUtils.consumeGuiOpen(event.screen)) return
        if (LoadoutUtils.consumeGuiOpen(event.screen)) return
        WardrobeUtils.consumeGuiOpen(event.screen)
    }

    @EventHandler
    fun onGuiDraw(event: GuiEvent.Draw) {
        if (!SwapOptions.shouldHideGui() || !isInSwap) return
        event.cancel()
    }

    @EventHandler
    fun onGuiDrawBackground(event: GuiEvent.DrawBackground) {
        if (!SwapOptions.shouldHideGui() || !isInSwap) return
        event.cancel()
    }
}
