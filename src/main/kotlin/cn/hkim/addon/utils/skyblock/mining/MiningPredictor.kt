package cn.hkim.addon.utils.skyblock.mining

import cn.hkim.addon.utils.ServerUtils
import cn.hkim.addon.utils.enchantment
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import kotlin.math.max
import kotlin.math.roundToInt

object MiningPredictor {
    private const val DEFAULT_INSTANT_MULTIPLIER = 60

    fun serverTicks(
        strength: Int,
        speed: Int,
        instantMultiplier: Int = DEFAULT_INSTANT_MULTIPLIER
    ): Int {
        if (strength <= 0 || speed <= 0) return Int.MAX_VALUE
        if (speed > strength * instantMultiplier) return 1
        return max(4, (strength * 30f / speed).roundToInt())
    }

    fun miningPenalty(player: Player): Int {
        val headInWater = player.isEyeInFluid(FluidTags.WATER)
        val hasAquaAffinity = player.getItemBySlot(EquipmentSlot.HEAD).enchantment("aqua_affinity") > 0
        return if (!player.onGround() || (headInWater && !hasAquaAffinity)) 5 else 1
    }

    fun predictedTicks(
        strength: Int,
        speed: Int,
        instantMultiplier: Int,
        player: Player,
        pingGlide: Float
    ): Float {
        val server = serverTicks(strength, speed, instantMultiplier)
        if (server == Int.MAX_VALUE) return Float.MAX_VALUE

        val tps = ServerUtils.averageTps.takeIf { it > 0f } ?: 20f
        val clientTicks = server * miningPenalty(player) * (20f / tps)
        val oneWayTicks = ServerUtils.averagePing.toFloat() / 100f // RTT
        return max(1f, clientTicks - pingGlide * oneWayTicks)
    }
}
