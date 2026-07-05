package cn.hkim.addon.features.impl

import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.events.impl.ChatReceiveEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.fillItemFromSack
import cn.hkim.addon.utils.schedule
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler

@ModuleInfo("auto_gfs", Category.SKYBLOCK)
object AutoGFS : Module("Auto GFS", "Automatically refills certain items from your sacks.") {
    private val inKuudra by BooleanSetting("In Kuudra", "Only gfs in Kuudra.", true)
    private val inDungeon by BooleanSetting("In Dungeon", "Only gfs in Dungeons.", true)
    private val refillOnInstanceStart by BooleanSetting("Refill on Instance Start", "Refill when a dungeon starts.", true)
    private val refillOnKuudraStunned by BooleanSetting("Refill on Kuudra Stunned", "Refill when kuudra is stunned.", false)
    private val item by DropdownSetting("Items", "Items to refill from sacks.")
    private val refillPearl by BooleanSetting("Refill Pearl", "Refill ender pearls.", false).depends { item }
    private val refillJerry by BooleanSetting("Refill Jerry", "Refill inflatable jerrys.", false).depends { item }
    private val refillTNT by BooleanSetting("Refill TNT", "Refill superboom tnt.", false).depends { item }
    private val refillToxicArrow by BooleanSetting("Refill Toxic Arrow", "Refill toxic arrow poison.", false).depends { item }
    private val refillOnTimer by BooleanSetting("Refill on Timer", "Refill on a 5s intervals.", true)
    private val timerIncrements by NumberSetting("Timer Increments (s)", "The interval in which to refill.", 5f, 1f, 60f, 1f)

    private val startRegex = Regex("\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.|\\[NPC] Mort: Right-click the Orb for spells, and Left-click \\(or Drop\\) to use your Ultimate!|\\[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!")
    private val stunnedRegex = Regex("\\[NPC] Elle: That looks like it hurt! Quickly, while Kuudra is distracted shoot him with the Ballista!")
    private val refillQueue = mutableListOf<RefillItem>()
    private var isProcessingQueue = false

    init {
        scheduleRefill()
    }

    override fun onEnable() {
        scheduleRefill()
    }

    @EventHandler
    private fun onChat(event: ChatReceiveEvent) {
        if (!enabled) return
        val message = event.message.clean

        if (refillOnInstanceStart && (LocationUtils.inDungeons || LocationUtils.inKuudra)) {
            if (message.matches(startRegex)) {
                refill()
                return
            }
        }

        if (refillOnKuudraStunned && LocationUtils.inKuudra) {
            if (message.matches(stunnedRegex)) {
                refillKuudraStunned()
            }
        }
    }

    private data class RefillItem(
        val amount: Int,
        val itemId: String,
        val sackName: String
    )

    private fun scheduleRefill() {
        val delayTicks = timerIncrements * 20
        schedule(delayTicks.toInt()) {
            if (enabled && refillOnTimer) refill()
            scheduleRefill()
        }
    }

    private fun isInValidLocation(): Boolean {
        if (!inKuudra && !inDungeon) return true
        return (inKuudra && LocationUtils.inKuudra) || (inDungeon && LocationUtils.inDungeons)
    }

    private fun refill() {
        if (!isInValidLocation()) return

        if (refillPearl) refillQueue.add(RefillItem(16, "ENDER_PEARL", "ender_pearl"))
        if (refillJerry) refillQueue.add(RefillItem(64, "INFLATABLE_JERRY", "inflatable_jerry"))
        if (refillTNT) refillQueue.add(RefillItem(64, "SUPERBOOM_TNT", "superboom_tnt"))

        if (refillQueue.isNotEmpty() && !isProcessingQueue) processNextItem()
    }

    private fun refillKuudraStunned() {
        if (refillToxicArrow) refillQueue.add(RefillItem(64, "TOXIC_ARROW_POISON", "toxic_arrow_poison"))
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