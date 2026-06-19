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
import cn.hkim.addon.utils.itemId
import meteordevelopment.orbit.EventHandler
import net.minecraft.world.item.BlockItem

@ModuleInfo("auto_clicker", Category.SKYBLOCK)
object AutoClicker : Module("Auto Clicker", "Auto clicker with options for left-click, right-click, or both.") {
    private val mode by SelectorSetting("Mode", "Click mode.", listOf("Normal", "Terminator"), "Normal")
    private val cps by NumberSetting("CPS", "Clicks per second.", 5.0f, 3.0f, 15.0f, 0.5f).depends { mode == 1 }
    private val rightClickBlock by BooleanSetting("Right Click Block", "Auto right-click when holding a block in Terminator mode.", true).depends { mode == 1 }
    private val blockCps by NumberSetting("Block CPS", "Right clicks per second for block spam.", 10.0f, 3.0f, 20.0f, 0.5f).depends { mode == 1 && rightClickBlock }
    private val enableLeftClick by BooleanSetting("Enable Left Click", "Enable auto-clicking for left-click.", true).depends { mode == 0 }
    private val enableRightClick by BooleanSetting("Enable Right Click", "Enable auto-clicking for right-click.", true).depends { mode == 0 }
    private val leftCps by NumberSetting("Left CPS", "Left clicks per second.", 5.0f, 3.0f, 15.0f, 0.5f).depends { mode == 0 }
    private val rightCps by NumberSetting("Right CPS", "Right clicks per second.", 5.0f, 3.0f, 15.0f, 0.5f).depends { mode == 0 }

    private var nextLeftClick = 0.0
    private var nextRightClick = 0.0
    private var nextBlockClick = 0.0

    @EventHandler
    fun onTick(event: TickEvent.Start) {
        if (mc.screen != null || mc.player == null) return
        if (mc.player!!.isUsingItem) return

        val nowMillis = System.currentTimeMillis().toDouble()
        when (mode) {
            0 -> handleNormalClicks(nowMillis)
            else -> handleTerminatorClicks(nowMillis)
        }
    }

    private fun nextDelay(cps: Float): Double {
        val base = 1000.0 / cps
        return base + (Math.random() - 0.5) * base * 0.1  // ±10%
    }

    private fun handleTerminatorClicks(nowMillis: Double) {
        if (mc.player?.mainHandItem?.itemId == "TERMINATOR" && mc.options.keyUse.isDown) {
            if (nowMillis >= nextRightClick) {
                nextRightClick = nowMillis + nextDelay(cps)
                mc.missTime = 0
                clickKey(mc.options.keyAttack)
            }
        }

        if (rightClickBlock && mc.player?.mainHandItem?.item is BlockItem && mc.options.keyUse.isDown) {
            if (nowMillis >= nextBlockClick) {
                nextBlockClick = nowMillis + nextDelay(blockCps)
                clickKey(mc.options.keyUse)
            }
        }
    }

    private fun handleNormalClicks(nowMillis: Double) {
        if (enableLeftClick && mc.options.keyAttack.isDown && nowMillis >= nextLeftClick) {
            nextLeftClick = nowMillis + nextDelay(leftCps)
            mc.missTime = 0
            clickKey(mc.options.keyAttack)
        }

        if (enableRightClick && mc.options.keyUse.isDown && nowMillis >= nextRightClick) {
            nextRightClick = nowMillis + nextDelay(rightCps)
            clickKey(mc.options.keyUse)
        }
    }
}
