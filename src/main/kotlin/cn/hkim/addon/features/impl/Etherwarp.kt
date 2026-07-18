package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.InputEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.hasEthermerge
import cn.hkim.addon.utils.rightClick
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler

@ModuleInfo("etherwarp", Category.SKYBLOCK)
object Etherwarp : Module("Etherwarp", "Sneak to instantly Etherwarp with AOTV/AOTE.") {
    private var lastEtherwarpTime: Long = 0

    @EventHandler
    private fun onInput(event: InputEvent) {
        if (!enabled || !LocationUtils.inSkyBlock || mc.player == null || mc.screen != null) return
        if (event.key != mc.options.keyShift) return

        val stack = mc.player?.mainHandItem ?: return
        if (stack.hasEthermerge) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastEtherwarpTime < 150) return

            rightClick()
            lastEtherwarpTime = currentTime
        }
    }
}