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
import cn.hkim.addon.utils.rightClick
import cn.hkim.addon.utils.skyblock.EquipmentUtils.swapEquipment
import cn.hkim.addon.utils.skyblock.LocationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import meteordevelopment.orbit.EventHandler

@ModuleInfo("auto_swap", Category.SKYBLOCK, false)
object AutoSwap : Module("Auto Swap", "Auto swap spirit/bonzo.") {
    private val useCustomDelay by BooleanSetting("Custom Swap Delay", "Customize delay before swapping items.", false)
    private val custom by DropdownSetting("Delay", "Delay settings.", false).depends { useCustomDelay }
    private val spiritDelay by NumberSetting("Spirit Swap Delay", "Delay before equipping Spirit Mask.", 200, 100, 2000, 50).depends { custom }
    private val phoenixDelay by NumberSetting("Phoenix Swap Delay", "Delay before use Auto Pet rod.", 200, 100, 2000, 50).depends { custom }

    private val bonzoRegex = Regex("^Your (?:. )?Bonzo's Mask saved your life!$")
    private val spiritRegex = Regex("^Second Wind Activated! Your Spirit Mask saved your life!$")

    @EventHandler
    fun onChat(event: ChatReceiveEvent) {
        if (!LocationUtils.inDungeons) return

        when {
            event.message.matches(bonzoRegex) -> Hkim.scope.launch { handleBonzo() }
            event.message.matches(spiritRegex) -> Hkim.scope.launch { handleSpirit() }
        }
    }

    private suspend fun handleBonzo() {
        val delayTime = if (useCustomDelay) spiritDelay.toInt() else 250
        delay(delayTime + (0..99).random().toLong())

//        if (Auto4.isDeviceIncomplete()) Auto4.pauseShooting()
        delay(100)

        if (swapEquipment(listOf("SPIRIT_MASK"))) {
            delay(50)
//            if (Auto4.isDeviceIncomplete()) Auto4.resumeShooting()
        }
    }

    private suspend fun handleSpirit() {
        val lastSlot = mc.player?.inventory?.selectedSlot ?: return
        val rodSlot = findRodSlot()
        if (rodSlot == -1) return

        val delayTime = if (useCustomDelay) phoenixDelay.toInt() else 250
        delay(delayTime + (0..99).random().toLong())

//        if (Auto4.isDeviceIncomplete()) Auto4.pauseShooting()
        delay(100)

        mc.player?.inventory?.selectedSlot = rodSlot
        delay(160 + (0..40).random().toLong())
        rightClick()
        delay(160 + (0..40).random().toLong())
        mc.player?.inventory?.selectedSlot = lastSlot
        delay(50)

//        if (Auto4.isDeviceIncomplete()) Auto4.resumeShooting()
    }
}