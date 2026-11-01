package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.events.impl.GuiEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.randomDelay
import cn.hkim.addon.utils.ravengard.ArmorDefence
import cn.hkim.addon.utils.ravengard.defence
import cn.hkim.addon.utils.ravengard.inRavengardInstance
import cn.hkim.addon.utils.ravengard.value
import cn.hkim.addon.utils.schedule
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import kotlin.math.ceil

@ModuleInfo("chest_stealer", Category.RAVENGARD)
object ChestStealer : Module("Chest Stealer", "Automatically loots valuable items from chests.") {
    private val valueForSteal by NumberSetting("Value for Steal", "Only steal items worth more than this value.", 5f, 0f, 200f, 5f)
    private val betterArmor by BooleanSetting("Better Armor", "Steal armor that is better than your currently equipped.", true)
    private val clickDelay by NumberSetting("Click Delay", "Delay between each steal action.", 100f, 100f, 500f, 5f, "ms")
    private val autoClose by BooleanSetting("Auto Close", "Close the chest once there is nothing to steal.", false)

    private var activeScreen: AbstractContainerScreen<*>? = null
    private var activeMenu: ChestMenu? = null
    private var taskScheduled = false

    private val reservedSlots = intArrayOf(9, 10, 11, 12)

    @EventHandler
    private fun onGuiOpen(event: GuiEvent.Open) {
        if (!enabled || !inRavengardInstance) return
        val screen = event.screen as? AbstractContainerScreen<*> ?: return
        val menu = screen.menu as? ChestMenu ?: return

        activeScreen = screen
        activeMenu = menu
        scheduleNext()
    }

    @EventHandler
    private fun onGuiClose(event: GuiEvent.Close) {
        if (event.screen !== activeScreen) return
        activeScreen = null
        activeMenu = null
    }

    private fun scheduleNext(baseDelayMs: Int = 50, varianceMs: Int = 0) {
        if (taskScheduled) return
        taskScheduled = true
        val delayMs = randomDelay(baseDelayMs, varianceMs)
        schedule(ceil(delayMs / 50.0).toInt().coerceAtLeast(1)) {
            taskScheduled = false
            attemptSteal()
        }
    }

    private fun attemptSteal() {
        if (!enabled || !inRavengardInstance) {
            activeScreen = null
            activeMenu = null
            return
        }

        val screen = activeScreen ?: return
        val menu = activeMenu ?: return
        if (mc.gui.screen() !== screen) {
            activeScreen = null
            activeMenu = null
            return
        }
        val player = mc.player ?: return

        if (isBackpackFull()) {
            scheduleNext(250)
            return
        }

        // Steal the most valuable item first; armor upgrades without value come last.
        val slot = menu.slots
            .asSequence()
            .filter { it.container !is Inventory && !it.item.isEmpty && accept(it.item) }
            .maxByOrNull { it.item.value }
        if (slot == null) {
            if (autoClose) player.closeContainer()
            activeScreen = null
            activeMenu = null
            return
        }

        mc.gameMode?.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.QUICK_MOVE, player)
        scheduleNext(clickDelay.toInt(), 50)
    }

    private fun isBackpackFull(): Boolean {
        val inventory = mc.player?.inventory ?: return true
        for (slot in 0 until Inventory.INVENTORY_SIZE) {
            if (slot in reservedSlots) continue
            if (inventory.getItem(slot).isEmpty) return false
        }
        return true
    }

    private fun accept(stack: ItemStack): Boolean {
        if (stack.value > valueForSteal) return true
        return betterArmor && isArmorUpgrade(stack)
    }

    private fun isArmorUpgrade(stack: ItemStack): Boolean {
        val slot = stack.get(DataComponents.EQUIPPABLE)?.slot() ?: return false
        if (!slot.isArmor) return false
        return stack.defence > ArmorDefence.defenceOf(slot)
    }
}
