package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.hasGlint
import cn.hkim.addon.utils.randomDelay
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Items
import java.util.concurrent.ConcurrentHashMap

@ModuleInfo("auto_experiments", category = Category.SKYBLOCK)
object AutoExperiments : Module("Auto Experiments", "Automatically click on the Chronomatron and Ultrasequencer experiments.") {
    private val delay by NumberSetting("Click Delay (ms)", "Time in ms between automatic test clicks.", 200f, 0f, 1000f, 10f)
    private val autoClose by BooleanSetting("Auto Close", "Automatically close the GUI after completing the experiment.", true)
    private val serumCount by NumberSetting("Serum Count", "Consumed Metaphysical Serum count.", 0f, 0f, 3f, 1f)
    private val getMaxXp by BooleanSetting("Get Max XP", "Solve Chronomatron to 15 and Ultrasequencer to 20 for max XP.", false)

    private var handler: ExperimentHandler? = null
    private var lastClickTime = 0L
    private val clickDelay: Long
        get() = randomDelay(delay.toInt(), 50)

    private abstract class ExperimentHandler {
        protected var clicks = 0
        protected var hasData = false

        abstract fun onSlotUpdate(event: GuiEvent.SlotUpdate)
        abstract fun nextClick(): Int?
        abstract fun shouldClose(autoClose: Boolean): Boolean
    }
    
    @EventHandler
    fun onGuiOpen(event: GuiEvent.Open) {
        val title = event.screen.title.string

        handler = when {
            title.startsWith("Chronomatron (") -> ChronomatronHandler()
            title.startsWith("Ultrasequencer (") -> UltrasequencerHandler()
            else -> null
        }
    }

    @EventHandler
    fun onSlotUpdate(event: GuiEvent.SlotUpdate) {
        handler?.onSlotUpdate(event)
    }
    
    @EventHandler
    fun onTick(event: TickEvent.End) {
        val handler = handler ?: return
        val player = mc.player ?: return
        val screen = mc.screen as? AbstractContainerScreen<*> ?: return

        val now = System.currentTimeMillis()
        if (now - lastClickTime < clickDelay) return

        handler.nextClick()?.let { slotId ->
            mc.gameMode?.handleContainerInput(screen.menu.containerId, slotId, 0, ContainerInput.CLONE, player)
            lastClickTime = now
        }

        if (!handler.shouldClose(autoClose)) return

        mc.player?.closeContainer()
        AutoExperiments.handler = null
    } 

    private class ChronomatronHandler : ExperimentHandler() {
        private val order = mutableListOf<Int>()
        private var lastAddedSlot = -1
        private var close = false

        override fun onSlotUpdate(event: GuiEvent.SlotUpdate) {
            val slots = event.menu.slots
            val center = slots[49].item

            if (
                lastAddedSlot != -1 &&
                center.item == Items.GLOWSTONE &&
                !slots[lastAddedSlot].item.hasGlint
            ) {
                close = order.size > if (getMaxXp) 15 else 11 - serumCount.toInt()
                hasData = false
                return
            }

            if (hasData || center.item != Items.CLOCK) return

            val slot = slots.firstOrNull { it.index in 10..43 && it.item.hasGlint } ?: return

            order.add(slot.index)
            lastAddedSlot = slot.index
            hasData = true
            clicks = 0
        }

        override fun nextClick(): Int? = if (hasData && clicks < order.size) order[clicks++] else null

        override fun shouldClose(autoClose: Boolean): Boolean {
            if (!autoClose || !close) return false
            if (clicks < order.size) return false

            close = false
            return true
        }
    }

    private class UltrasequencerHandler : ExperimentHandler() {
        private val order = ConcurrentHashMap<Int, Int>()

        override fun onSlotUpdate(event: GuiEvent.SlotUpdate) {
            val slots = event.menu.slots
            val center = slots[49].item

            if (center.item == Items.CLOCK) {
                hasData = false
                return
            }

            if (hasData || center.item != Items.GLOWSTONE) return

            order.clear()

            for (slot in slots) {
                if (slot.index in 9..44 && slot.item.hoverName.string.clean.matches(Regex("\\d+"))) order[slot.item.count - 1] = slot.index
            }

            hasData = true
            clicks = 0
        }

        override fun nextClick(): Int? = if (!hasData) order[clicks++] else null

        override fun shouldClose(autoClose: Boolean): Boolean = autoClose && order.size > if (getMaxXp) 20 else 9 - serumCount.toInt()
    }
}