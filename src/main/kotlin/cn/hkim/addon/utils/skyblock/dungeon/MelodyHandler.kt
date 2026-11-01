package cn.hkim.addon.utils.skyblock.dungeon

import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items

class MelodyHandler : TerminalHandler(TerminalEnums.MELODY) {
    private val magentaPane = Items.STAINED_GLASS_PANE.magenta
    private val limePane = Items.STAINED_GLASS_PANE.lime
    private val limeClay = Items.DYED_TERRACOTTA.lime

    var isEdgeAligned = false
        private set

    override fun canSolve(slots: List<Slot>): Boolean = true

    override fun solve(slots: List<Slot>): List<Int> {
        val magentaColumn = slots.firstOrNull { it.item.item == magentaPane }?.index?.mod(9) ?: return emptyList()
        isEdgeAligned = magentaColumn == 1 || magentaColumn == 5
        val limeColumn = slots.firstOrNull { it.item.item == limePane }?.index?.mod(9) ?: return emptyList()
        val buttonRow = slots.firstOrNull { it.item.item == limeClay }?.index?.div(9) ?: return emptyList()

        return if (limeColumn == magentaColumn) listOf(buttonRow * 9 + 7) else emptyList()
    }
}
