package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.config.settings.SelectorSetting
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.clickKey
import cn.hkim.addon.utils.isBuildingBlock
import cn.hkim.addon.utils.itemId
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.KeyMapping

@ModuleInfo("auto_clicker", Category.SKYBLOCK)
object AutoClicker : Module("Auto Clicker", "Auto click your mouse.") {
    private val mode by SelectorSetting("Mode", "Click mode.", listOf("Normal", "Terminator"), "Normal")
    private val cps by NumberSetting("CPS", "Clicks per second.", 5.0f, 3.0f, 15.0f, 0.5f).depends { mode == 1 }
    private val rightClickBlock by BooleanSetting("Right Click Block", "Auto right-click when holding a block in Terminator mode.", true).depends { mode == 1 }
    private val blockCps by NumberSetting("Block CPS", "Right clicks per second for block spam.", 10.0f, 5.0f, 20.0f, 0.5f).depends { mode == 1 && rightClickBlock }
    private val enableLeftClick by BooleanSetting("Enable Left Click", "Enable auto-clicking for left-click.", true).depends { mode == 0 }
    private val enableRightClick by BooleanSetting("Enable Right Click", "Enable auto-clicking for right-click.", true).depends { mode == 0 }
    private val leftCps by NumberSetting("Left CPS", "Left clicks per second.", 5.0f, 1.0f, 15.0f, 0.5f).depends { mode == 0 }
    private val rightCps by NumberSetting("Right CPS", "Right clicks per second.", 5.0f, 1.0f, 15.0f, 0.5f).depends { mode == 0 }

    private var nextLeftClick = 0.0
    private var nextRightClick = 0.0
    private var nextBlockClick = 0.0

    @EventHandler
    private fun onTick(event: TickEvent.Start) {
        if (!enabled || mc.gui.screen() != null || mc.player == null || mc.player!!.isUsingItem) {
            nextLeftClick = 0.0
            nextRightClick = 0.0
            nextBlockClick = 0.0
            return
        }

        val nowMillis = System.currentTimeMillis().toDouble()
        when (mode) {
            0 -> handleNormalClicks(nowMillis)
            else -> handleTerminatorClicks(nowMillis)
        }
    }

    private fun nextDelay(cps: Float): Double {
        val base = 1000.0 / cps
        return base + (Math.random() - 0.5) * base * 0.1
    }

    private fun clickOnSchedule(key: KeyMapping, nextClick: Double, cps: Float, nowMillis: Double, beforeClick: () -> Unit = {}): Double {
        var next = if (nextClick == 0.0) nowMillis else nextClick
        if (nowMillis < next) return next

        beforeClick()
        clickKey(key)
        next += nextDelay(cps)

        if (next <= nowMillis) next = nowMillis + nextDelay(cps)

        return next
    }

    private fun handleTerminatorClicks(nowMillis: Double) {
        val player = mc.player ?: return

        if (player.mainHandItem.itemId == "TERMINATOR" && mc.options.keyUse.isDown) {
            nextRightClick = clickOnSchedule(mc.options.keyAttack, nextRightClick, cps, nowMillis) { mc.missTime = 0 }
        } else {
            nextRightClick = 0.0
        }

        if (rightClickBlock && isBuildingBlock(player.mainHandItem) && mc.options.keyUse.isDown) {
            nextBlockClick = clickOnSchedule(mc.options.keyUse, nextBlockClick, blockCps, nowMillis)
        } else {
            nextBlockClick = 0.0
        }
    }

    private fun handleNormalClicks(nowMillis: Double) {
        if (enableLeftClick && mc.options.keyAttack.isDown) {
            nextLeftClick = clickOnSchedule(mc.options.keyAttack, nextLeftClick, leftCps, nowMillis) { mc.missTime = 0 }
        } else {
            nextLeftClick = 0.0
        }

        if (enableRightClick && mc.options.keyUse.isDown) {
            nextRightClick = clickOnSchedule(mc.options.keyUse, nextRightClick, rightCps, nowMillis)
        } else {
            nextRightClick = 0.0
        }
    }
}
