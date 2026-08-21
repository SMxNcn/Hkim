package cn.hkim.addon.utils.skyblock.hunting

import cn.hkim.addon.Hkim
import cn.hkim.addon.utils.isPositionInArea
import net.minecraft.core.BlockPos

enum class SafariArea(val corner1: BlockPos, val corner2: BlockPos) {
    Spawn(BlockPos(-74, 32, -24), BlockPos(-26, 128, 24)),
    Icy(BlockPos(-50, 32, -1), BlockPos(-181, 128, -120)),
    Haunted(BlockPos(-49, 32, 0), BlockPos(62, 128, -120)),
    Cavern(BlockPos(-51, 32, 0), BlockPos(-181, 128, 120)),
    Forest(BlockPos(-50, 32, 1), BlockPos(62, 128, 120));

    companion object {
        fun getCurrentArea(): SafariArea? {
            val player = Hkim.mc.player ?: return null
            val playerPos = BlockPos(player.x.toInt(), player.y.toInt(), player.z.toInt())

            if (isPositionInArea(Spawn.corner1, Spawn.corner2, playerPos)) return Spawn

            return entries.firstOrNull { area ->
                area != Spawn && isPositionInArea(area.corner1, area.corner2, playerPos)
            }
        }
    }
}