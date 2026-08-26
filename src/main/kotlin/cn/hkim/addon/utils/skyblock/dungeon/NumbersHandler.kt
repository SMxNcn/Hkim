package cn.hkim.addon.utils.skyblock.dungeon

import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items

class NumbersHandler : TerminalHandler(TerminalEnums.NUMBERS) {
    override fun canSolve(slots: List<Slot>): Boolean =
        slots.count { it.item.item == Items.RED_STAINED_GLASS_PANE } >= 10

    override fun solve(slots: List<Slot>): List<Int> =
        slots.mapNotNull { slot ->
            if (slot.item.item == Items.RED_STAINED_GLASS_PANE) slot.index to slot.item.count else null
        }.sortedBy { it.second }.map { it.first }
}
