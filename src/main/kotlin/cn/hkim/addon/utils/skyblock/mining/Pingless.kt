package cn.hkim.addon.utils.skyblock.mining

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.mixins.accessors.MultiPlayerGameModeAccessor
import meteordevelopment.orbit.EventHandler
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import kotlin.math.ceil

object Pingless {
    private var target: BlockPos? = null
    private var startTick = -1L
    private var safeTick = -1L
    private var pingGlide = 0f

    private var lastStartPos: BlockPos? = null
    private var lastStartTick = -1L

    val hasTarget: Boolean get() = target != null
    val hasStarted: Boolean get() = startTick >= 0
    val hasEstimate: Boolean get() = safeTick >= 0

    fun setTarget(pos: BlockPos?) {
        target = pos
        startTick = -1L
        safeTick = -1L
    }

    fun reset() {
        target = null
        startTick = -1L
        safeTick = -1L
    }

    @EventHandler
    private fun onStartBreak(event: WorldEvent.BlockBreakStart) {
        lastStartPos = event.pos
        lastStartTick = event.startTick.toLong()
        if (event.pos != target) return
        startTick = event.startTick.toLong()
        update()
    }

    fun update(glide: Float = pingGlide): Boolean {
        pingGlide = glide
        val player = mc.player ?: return false
        val tracked = target ?: return false
        val level = mc.level ?: return false
        val state = level.getBlockState(tracked)
        val mineral = MineralType.fromBlock(state.block) ?: return false
        val strength = mineral.strengthOf(state.block)

        if (strength <= 0 || !MiningStatsTracker.hasSpeed) {
            safeTick = -1L
            return false
        }

        val speed = MiningStatsTracker.currentSpeed(player)
        if (speed <= 0) {
            safeTick = -1L
            return false
        }

        if (startTick < 0) {
            val gameMode = mc.gameMode ?: return false
            val destroyPos = (gameMode as? MultiPlayerGameModeAccessor)?.destroyBlockPos
            if (gameMode.isDestroying && destroyPos == tracked) {
                startTick = if (lastStartPos == tracked) lastStartTick else player.tickCount.toLong()
            } else {
                safeTick = -1L
                return false
            }
        }

        val ticks = MiningPredictor.predictedTicks(
            strength, speed, mineral.instantMultiplier, player, pingGlide
        )
        if (ticks == Float.MAX_VALUE) {
            safeTick = -1L
            return false
        }

        safeTick = startTick + ceil(ticks).toLong()
        return true
    }

    fun isReady(player: Player): Boolean =
        safeTick >= 0 && player.tickCount.toLong() >= safeTick

    fun ticksRemaining(player: Player): Long =
        if (safeTick < 0) -1L else (safeTick - player.tickCount.toLong()).coerceAtLeast(0L)
}
