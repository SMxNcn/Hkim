package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.fillItemFromSack
import cn.hkim.addon.utils.itemId
import cn.hkim.addon.utils.schedule
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler

@ModuleInfo("auto_gfs", Category.SKYBLOCK)
object AutoGFS : Module("Auto GFS", "Automatically refills certain items from your sacks.") {
    private val inKuudra by BooleanSetting("In Kuudra", "Only gfs in Kuudra.", true)
    private val inDungeon by BooleanSetting("In Dungeon", "Only gfs in Dungeons.", true)
    private val refillOnDungeonStart by BooleanSetting("Refill on Dungeon Start", "Refill when a dungeon starts.", true)
    private val refillPearl by BooleanSetting("Refill Pearl", "Refill ender pearls.", true)
    private val refillJerry by BooleanSetting("Refill Jerry", "Refill inflatable jerrys.", true)
    private val refillTNT by BooleanSetting("Refill TNT", "Refill superboom tnt.", true)
    private val refillOnTimer by BooleanSetting("Refill on Timer", "Refill on a 5s intervals.", true)
    private val timerIncrements by NumberSetting("Timer Increments (s)", "The interval in which to refill.", 5f, 1f, 60f, 1f)

    private val startRegex = Regex("\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.|\\[NPC] Mort: Right-click the Orb for spells, and Left-click \\(or Drop\\) to use your Ultimate!")
    private val refillQueue = mutableListOf<RefillItem>()
    private var isProcessingQueue = false

    init {
        scheduleRefill()
    }

    @EventHandler
    private fun onChat(event: ChatReceiveEvent) {
        if (!enabled || !refillOnDungeonStart || !LocationUtils.inDungeons) return
        when { event.message.clean.matches(startRegex) -> refill() }
    }

    private data class RefillItem(
        val amount: Int,
        val itemId: String,
        val sackName: String
    )

    private fun scheduleRefill() {
        if (!((inKuudra && LocationUtils.inKuudra) || (inDungeon && LocationUtils.inDungeons))) return
        val delayTicks = timerIncrements * 20
        schedule(delayTicks.toInt()) {
            if (enabled && refillOnTimer) refill()
            scheduleRefill()
        }
    }

    private fun refill() {
        val inventory = mc.player?.inventory ?: return

        inventory.find { it.itemId == "ENDER_PEARL" }?.takeIf { refillPearl }?.also { refillQueue.add(RefillItem(16, "ENDER_PEARL", "ender_pearl")) }

        inventory.find { it.itemId == "INFLATABLE_JERRY" }?.takeIf { refillJerry }?.also { refillQueue.add(RefillItem(64, "INFLATABLE_JERRY", "inflatable_jerry")) }

        inventory.find { it.itemId == "SUPERBOOM_TNT" }.takeIf { refillTNT }?.also { refillQueue.add(RefillItem(64, "SUPERBOOM_TNT", "superboom_tnt")) }

        if (refillQueue.isNotEmpty() && !isProcessingQueue) processNextItem()
    }

    private fun processNextItem() {
        if (refillQueue.isEmpty()) {
            isProcessingQueue = false
            return
        }

        isProcessingQueue = true
        val item = refillQueue.removeFirst()

        fillItemFromSack(item.amount, item.itemId, item.sackName)

        schedule(40) {
            processNextItem()
        }
    }
}