package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.config.settings.SelectorSetting
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
    private val firstSwap by SelectorSetting("First Swap", "Which item to swap to first after the Spirit Mask is used.", listOf("Rod First", "Mask First"), "Rod First")

    private val useCustomDelay by BooleanSetting("Custom Swap Delay", "Customize delay before swapping items.", false)
    private val custom by DropdownSetting("Delay", "Delay settings.", false).depends { useCustomDelay }
    private val maskDelay by NumberSetting("Mask Delay", "Delay before equipping mask.", 200f, 100f, 2000f, 50f, unit = "ms").depends { custom }
    private val rodDelay by NumberSetting("Rod Delay", "Delay before use Autopet rod.", 200f, 100f, 2000f, 50f, unit = "ms").depends { custom }

    private val bonzoRegex = Regex("^Your (?:. )?Bonzo's Mask saved your life!$")
    private val spiritRegex = Regex("^Second Wind Activated! Your Spirit Mask saved your life!$")
    private val phoenixRegex = Regex("^Your Phoenix Pet saved you from certain death!$")

    private fun maskDelayTime() = if (useCustomDelay) maskDelay.toInt() else 400
    private fun rodDelayTime() = if (useCustomDelay) rodDelay.toInt() else 400

    @EventHandler
    private fun onChat(event: ChatReceiveEvent) {
        if (!enabled || !LocationUtils.inDungeons || (onlyP3 && getF7Phase() != M7Phases.P3)) return

        when {
            event.message.matches(spiritRegex) -> Hkim.scope.launch { handleSpirit() }
            event.message.matches(bonzoRegex) -> Hkim.scope.launch { handleBonzo() }
            event.message.matches(phoenixRegex) -> Hkim.scope.launch { handlePhoenix() }
        }
    }

    private suspend fun handleSpirit() {
        if (firstSwap == 1) {
            delay(randomDelay(maskDelayTime(), 50))
            swapEquipment(listOf("BONZO_MASK"))
        } else {
            delay(randomDelay(rodDelayTime(), 50))
            useRod()
        }
    }

    private suspend fun handleBonzo() {
        if (firstSwap != 1) return
        delay(randomDelay(rodDelayTime(), 50))
        useRod()
    }

    private suspend fun handlePhoenix() {
        if (firstSwap == 1) return
        delay(randomDelay(maskDelayTime(), 50))
        swapEquipment(listOf("BONZO_MASK"))
    }

    private suspend fun useRod() {
        val lastSlot = mc.player?.inventory?.selectedSlot ?: return
        val rodSlot = findRodSlot()
        if (rodSlot == -1) return

        mc.player?.inventory?.selectedSlot = rodSlot
        delay(randomDelay(150, 50))
        rightClick()
        delay(randomDelay(150, 50))
        mc.player?.inventory?.selectedSlot = lastSlot
    }
}