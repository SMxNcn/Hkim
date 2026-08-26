package cn.hkim.addon.utils.skyblock.dungeon

import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items

/**
 * 旋律终端（新版 3 行，容器 5×9，底部空出一行）：
 * - 行 0：紫色标记（列 1-5 随机）指示目标列
 * - 行 1-3：当前行列 1-5 为红板 + 1 块移动的绿板，其他行为白板；列 7 为按钮（当前行绿色陶瓦）
 * - 当绿板列 == 紫标记列时，点击当前行按钮（行×9+7）
 * - 点错/时机不对不失败：绿板暂停 1s 后按原方向继续移动
 *
 * 盲连击由模块层实现：点击行 N 按钮后，延迟 Melody Skip Delay 直接点击行 N+1，
 * 不等待行+1 确认；绿板恰好对齐则连击成功，否则点错只暂停 1s（无惩罚）。
 */
class MelodyHandler : TerminalHandler(TerminalEnums.MELODY) {

    private val magentaPane = Items.MAGENTA_STAINED_GLASS_PANE
    private val limePane = Items.LIME_STAINED_GLASS_PANE
    private val limeClay = Items.LIME_TERRACOTTA

    /** 紫标记是否在边缘列（索引 1/5，即 1 起始的列 2/6），盲连击（Skip）的前提。 */
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
