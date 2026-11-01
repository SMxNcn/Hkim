package cn.hkim.addon.utils.skyblock.mining

import net.minecraft.core.BlockPos

object DestroyProgressTracker {
    private data class Entry(val stage: Int, val updatedAt: Long)
    private val entries = HashMap<Long, Entry>()

    @JvmStatic
    fun update(pos: BlockPos, stage: Int) {
        if (stage !in 0..<10) {
            entries.remove(pos.asLong())
            return
        }
        entries[pos.asLong()] = Entry(stage, System.currentTimeMillis())
    }

    fun getDestroyProgress(pos: BlockPos): Int {
        val entry = entries[pos.asLong()] ?: return 0
        if (System.currentTimeMillis() - entry.updatedAt > 10000) {
            entries.remove(pos.asLong())
            return 0
        }
        return entry.stage
    }

    fun clear() = entries.clear()
}
