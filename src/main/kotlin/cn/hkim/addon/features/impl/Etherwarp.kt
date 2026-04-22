package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.PlayerEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.isEtherwarpItem
import cn.hkim.addon.utils.rightClick
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler

@ModuleInfo("etherwarp", Category.SKYBLOCK, false)
object Etherwarp : Module("Etherwarp", "Sneak to instantly Etherwarp with AOTV/AOTE.") {
    var lastEtherwarpTime: Long = 0

    @EventHandler
    fun onSneak(event: PlayerEvent.Sneak) {
        if (!LocationUtils.inSkyBlock || mc.player == null || mc.screen != null) return
        val stack = mc.player?.mainHandItem ?: return
        if (stack.isEtherwarpItem()?.contains("ethermerge") == true) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastEtherwarpTime < 150) return

            rightClick()
            lastEtherwarpTime = currentTime
        }
    }
}