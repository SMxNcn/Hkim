package cn.hkim.addon.utils.skyblock.mining

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.utils.findItemByID
import cn.hkim.addon.utils.itemId

object TimiteHelper {
    private const val YOUNGITE_PER_CYCLE = 32
    private const val TIMITE_PER_CYCLE = 32
    private const val OBSOLITE_PER_CYCLE = 16

    const val YOUNGITE_ITEM_ID = "YOUNGITE"
    const val TIMITE_ITEM_ID = "TIMITE"
    const val OBSOLITE_ITEM_ID = "OBSOLITE"
    const val CHRONO_PICKAXE_ID = "CHRONO_PICKAXE"
    const val TIME_GUN_ID = "TIME_GUN"

    var cyclesCompleted: Int = 0
        private set

    var youngiteCount: Int = 0
        private set
    var timiteCount: Int = 0
        private set
    var obsoliteCount: Int = 0
        private set

    var remainingYoungite: Int = YOUNGITE_PER_CYCLE
        private set
    var remainingTimite: Int = TIMITE_PER_CYCLE
        private set
    var remainingObsolite: Int = OBSOLITE_PER_CYCLE
        private set

    var currentTarget: MineralType? = null
        private set

    private var initialized = false

    fun reset() {
        cyclesCompleted = 0
        youngiteCount = 0
        timiteCount = 0
        obsoliteCount = 0
        remainingYoungite = YOUNGITE_PER_CYCLE
        remainingTimite = TIMITE_PER_CYCLE
        remainingObsolite = OBSOLITE_PER_CYCLE
        currentTarget = null
        initialized = false
    }

    fun scanNow() {
        scanInventory()
        updateTarget()
    }

    fun hasValidTimiteItems(): Boolean =
        findItemByID(CHRONO_PICKAXE_ID, hotbar = true) != -1 && findItemByID(TIME_GUN_ID, hotbar = true) != -1


    private fun scanInventory() {
        println("Scan Inventory!")
        val player = mc.player ?: return

        var y = 0; var t = 0; var o = 0
        for (slot in 0 until 36) {
            val stack = player.inventory.getItem(slot)
            if (stack.isEmpty) continue
            val id = stack.itemId
            when {
                id.contains(YOUNGITE_ITEM_ID, ignoreCase = true) -> y += stack.count
                id.contains(TIMITE_ITEM_ID, ignoreCase = true)  -> t += stack.count
                id.contains(OBSOLITE_ITEM_ID, ignoreCase = true) -> o += stack.count
            }
        }

        youngiteCount = y
        timiteCount = t
        obsoliteCount = o

        val possibleCycles = minOf(
            y / YOUNGITE_PER_CYCLE,
            t / TIMITE_PER_CYCLE,
            o / OBSOLITE_PER_CYCLE
        )
        cyclesCompleted = possibleCycles

        val yLeft = y - possibleCycles * YOUNGITE_PER_CYCLE
        val tLeft = t - possibleCycles * TIMITE_PER_CYCLE
        val oLeft = o - possibleCycles * OBSOLITE_PER_CYCLE

        remainingYoungite = maxOf(0, YOUNGITE_PER_CYCLE - yLeft)
        remainingTimite   = maxOf(0, TIMITE_PER_CYCLE   - tLeft)
        remainingObsolite = maxOf(0, OBSOLITE_PER_CYCLE - oLeft)

        initialized = true
    }

    private fun updateTarget() {
        if (!initialized) return

        val deficits = mutableListOf<Pair<MineralType, Int>>().apply {
            if (remainingYoungite > 0) add(MineralType.YOUNGITE to remainingYoungite)
            if (remainingTimite   > 0) add(MineralType.TIMITE   to remainingTimite)
            if (remainingObsolite > 0) add(MineralType.OBSOLITE to remainingObsolite)
        }

        if (deficits.isEmpty()) {
            currentTarget = null
            return
        }

        if (currentTarget != null && deficits.any { it.first == currentTarget }) return

        deficits.sortByDescending { it.second }
        currentTarget = deficits.first().first
    }
}
