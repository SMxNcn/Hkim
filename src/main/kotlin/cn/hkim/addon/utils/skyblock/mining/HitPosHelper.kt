package cn.hkim.addon.utils.skyblock.mining

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.CrossCollisionBlock
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.StairBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.SlabType
import net.minecraft.world.phys.Vec3
import kotlin.math.min
import kotlin.random.Random

object HitPosHelper {
    private const val FACE_OFFSET = 0.15
    private val rng = Random

    fun pickHitPos(level: Level, pos: BlockPos, state: BlockState, eyePos: Vec3): Vec3? {
        val block = state.block

        if (block is CrossCollisionBlock) {
            val y = (pos.y + 0.5) + (rng.nextDouble() * 0.8 - 0.4)
            return Vec3(pos.x + 0.5, y, pos.z + 0.5)
        }

        if (block is StairBlock) {
            return Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        }

        val isSlab = block is SlabBlock
        val slabMinY: Double
        val slabMaxY: Double
        if (isSlab) {
            when (state.getValue(SlabBlock.TYPE)) {
                SlabType.BOTTOM -> { slabMinY = 0.0; slabMaxY = 0.5 }
                SlabType.TOP -> { slabMinY = 0.5; slabMaxY = 1.0 }
                else -> { slabMinY = 0.0; slabMaxY = 1.0 }
            }
        } else {
            slabMinY = 0.0
            slabMaxY = 1.0
        }

        var bestFace: Direction? = null
        var bestFaceDistSq = Double.MAX_VALUE

        for (dir in Direction.entries) {
            val neighborPos = pos.relative(dir)
            if (!level.getBlockState(neighborPos).isAir) continue

            val faceCenterY = when {
                isSlab && dir.stepY > 0 -> pos.y + slabMaxY
                isSlab && dir.stepY < 0 -> pos.y + slabMinY
                isSlab -> pos.y + (slabMinY + slabMaxY) / 2
                else -> pos.y + 0.5 + dir.stepY * 0.5
            }

            val faceCenter = Vec3(
                pos.x + 0.5 + dir.stepX * 0.5,
                faceCenterY,
                pos.z + 0.5 + dir.stepZ * 0.5
            )
            val distSq = faceCenter.distanceToSqr(eyePos)
            if (distSq < bestFaceDistSq) {
                bestFaceDistSq = distSq
                bestFace = dir
            }
        }

        val face = bestFace ?: return null

        val margin = 0.2
        fun boundedOffset(min: Double, max: Double): Double {
            val center = (min + max) / 2
            val halfRange = (max - min) / 2 - margin
            if (halfRange <= 0.0) return center
            val offset = (rng.nextDouble() - 0.5) * 2 * minOf(FACE_OFFSET, halfRange)
            return (center + offset).coerceIn(min + margin, max - margin)
        }

        val offsetX = if (face.stepX != 0) 0.0 else boundedOffset(pos.x.toDouble(), pos.x + 1.0) - (pos.x + 0.5)
        val offsetZ = if (face.stepZ != 0) 0.0 else boundedOffset(pos.z.toDouble(), pos.z + 1.0) - (pos.z + 0.5)

        val hitY: Double = if (face.stepY != 0) {
            if (face.stepY > 0) pos.y + slabMaxY else pos.y + slabMinY
        } else {
            val yMin = pos.y + slabMinY
            val yMax = pos.y + slabMaxY
            val slabMargin = min(margin, (slabMaxY - slabMinY) * 0.3)
            val centerY = (yMin + yMax) / 2
            val halfRange = (yMax - yMin) / 2 - slabMargin
            if (halfRange <= 0.0) centerY
            else {
                val offsetY = (rng.nextDouble() - 0.5) * 2 * minOf(FACE_OFFSET, halfRange)
                (centerY + offsetY).coerceIn(yMin + slabMargin, yMax - slabMargin)
            }
        }

        val hitPos = Vec3(
            pos.x + 0.5 + face.stepX * 0.5 + offsetX,
            hitY,
            pos.z + 0.5 + face.stepZ * 0.5 + offsetZ
        )

        return if (hasLineOfSight(level, eyePos, hitPos, pos)) hitPos else null
    }

    fun hasLineOfSight(level: Level, from: Vec3, to: Vec3, targetPos: BlockPos): Boolean {
        val diff = to.subtract(from)
        val length = diff.length()
        if (length < 0.1) return true

        val dir = diff.normalize()
        val stepSize = 0.25
        val steps = (length / stepSize).toInt().coerceAtLeast(1)

        for (i in 0..steps) {
            val t = (i.toDouble() / steps) * length
            val point = from.add(dir.scale(t))
            val checkPos = BlockPos(point.x.toInt(), point.y.toInt(), point.z.toInt())
            if (checkPos != targetPos && !level.getBlockState(checkPos).isAir) {
                return false
            }
        }
        return true
    }

    fun matchesAnyMineral(state: BlockState): Boolean {
        return MineralType.fromBlock(state.block) != null
    }
}
