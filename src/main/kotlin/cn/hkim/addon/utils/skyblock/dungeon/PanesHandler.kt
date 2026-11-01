package cn.hkim.addon.utils.skyblock.dungeon

import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items

class PanesHandler : TerminalHandler(TerminalEnums.PANES) {
    override fun canSolve(slots: List<Slot>): Boolean =
        slots.any { it.item.item == Items.STAINED_GLASS_PANE.red }

    override fun solve(slots: List<Slot>): List<Int> =
        slots.mapNotNull { slot ->
            if (slot.item.item == Items.STAINED_GLASS_PANE.red) slot.index else null
        }
}
