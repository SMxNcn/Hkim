package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.findRodSlot
import cn.hkim.addon.utils.randomDelay
import cn.hkim.addon.utils.rightClick
import cn.hkim.addon.utils.skyblock.LocationUtils
import cn.hkim.addon.utils.skyblock.M7Phases
import cn.hkim.addon.utils.skyblock.getF7Phase
import cn.hkim.addon.utils.skyblock.inventory.EquipmentUtils.swapEquipment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import meteordevelopment.orbit.EventHandler

@ModuleInfo("auto_swap", Category.SKYBLOCK)
object AutoSwap : Module("Auto Swap", "Auto swap spirit/bonzo.") {
    private val onlyP3 by BooleanSetting("Only P3", "Only swap heads/pet in P3", true)
    private val useCustomDelay by BooleanSetting("Custom Swap Delay", "Customize delay before swapping items.", false)
    private val custom by DropdownSetting("Delay", "Delay settings.", false).depends { useCustomDelay }
    private val spiritDelay by NumberSetting("Spirit Delay", "Delay before equipping Spirit Mask.", 200f, 100f, 2000f, 50f, unit = "ms").depends { custom }
    private val phoenixDelay by NumberSetting("Phoenix Delay", "Delay before use Auto Pet rod.", 200f, 100f, 2000f, 50f, unit = "ms").depends { custom }

    private val bonzoRegex = Regex("^Your (?:. )?Bonzo's Mask saved your life!$")
    private val spiritRegex = Regex("^Second Wind Activated! Your Spirit Mask saved your life!$")

    @EventHandler
    private fun onChat(event: ChatReceiveEvent) {
        if (!enabled || !LocationUtils.inDungeons || (onlyP3 && getF7Phase() != M7Phases.P3)) return

        when {
            event.message.matches(bonzoRegex) -> Hkim.scope.launch { handleBonzo() }
            event.message.matches(spiritRegex) -> Hkim.scope.launch { handleSpirit() }
        }
    }

    private suspend fun handleBonzo() {
        val delayTime = if (useCustomDelay) spiritDelay.toInt() else 400
        delay(randomDelay(delayTime, 50))
        swapEquipment(listOf("SPIRIT_MASK"))
    }

    private suspend fun handleSpirit() {
        val lastSlot = mc.player?.inventory?.selectedSlot ?: return
        val rodSlot = findRodSlot()
        if (rodSlot == -1) return

        val delayTime = if (useCustomDelay) phoenixDelay.toInt() else 400
        delay(randomDelay(delayTime, 50))

        mc.player?.inventory?.selectedSlot = rodSlot
        delay(randomDelay(150, 50))
        rightClick()
        delay(randomDelay(150, 50))
        mc.player?.inventory?.selectedSlot = lastSlot
    }
}