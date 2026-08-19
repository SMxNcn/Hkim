package cn.hkim.addon.utils.skyblock.mining

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.CrossCollisionBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import kotlin.random.Random

object HitPosHelper {
    private const val JITTER = 0.08
    private val rng = Random

    fun pickHitPos(level: Level, pos: BlockPos, state: BlockState, eyePos: Vec3): Vec3? {
        val shape = state.getShape(level, pos, CollisionContext.empty())
        if (shape.isEmpty) return null

        val centre = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        clipToBlock(level, eyePos, centre, pos)?.let { r ->
            val jit = jitterOnHitPoint(r.location, r.direction)
            return if (hasLineOfSight(level, eyePos, jit, pos)) jit else r.location
        }

        val candidates = listOf(
            Vec3(pos.x + 0.5, pos.y + 0.85, pos.z + 0.5),   // upper segment
            Vec3(pos.x + 0.85, pos.y + 0.5, pos.z + 0.5),   // east bias
            Vec3(pos.x + 0.15, pos.y + 0.5, pos.z + 0.5),   // west bias
            Vec3(pos.x + 0.5,  pos.y + 0.5, pos.z + 0.85),  // south bias
            Vec3(pos.x + 0.5,  pos.y + 0.5, pos.z + 0.15),  // north bias
        )
        for (target in candidates) {
            clipToBlock(level, eyePos, target, pos)?.let { r ->
                val jit = jitterOnHitPoint(r.location, r.direction)
                return if (hasLineOfSight(level, eyePos, jit, pos)) jit else r.location
            }
        }
        return null
    }

    private fun clipToBlock(level: Level, from: Vec3, to: Vec3, targetPos: BlockPos): BlockHitResult? {
        var currentFrom = from
        repeat(8) {
            val ctx = ClipContext(currentFrom, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty())
            val result = level.clip(ctx)
            if (result.type == HitResult.Type.MISS) return null
            if (result.type == HitResult.Type.BLOCK) {
                if (result.blockPos == targetPos) return result
                if (level.getBlockState(result.blockPos).block is CrossCollisionBlock) {
                    val dir = Vec3(to.x - currentFrom.x, to.y - currentFrom.y, to.z - currentFrom.z).normalize()
                    currentFrom = result.location.add(dir.scale(0.01))
                } else {
                    return null
                }
            } else {
                return null
            }
        }
        return null
    }

    private fun jitterOnHitPoint(hitPos: Vec3, face: Direction): Vec3 {
        val dx = (rng.nextDouble() - 0.5) * JITTER
        val dy = (rng.nextDouble() - 0.5) * JITTER
        val dz = (rng.nextDouble() - 0.5) * JITTER
        return when (face.axis) {
            Direction.Axis.X -> Vec3(hitPos.x, hitPos.y + dy, hitPos.z + dz)
            Direction.Axis.Y -> Vec3(hitPos.x + dx, hitPos.y, hitPos.z + dz)
            Direction.Axis.Z -> Vec3(hitPos.x + dx, hitPos.y + dy, hitPos.z)
        }
    }


    fun hasLineOfSight(level: Level, from: Vec3, to: Vec3, targetPos: BlockPos): Boolean {
        return clipToBlock(level, from, to, targetPos) != null
    }

    fun matchesAnyMineral(state: BlockState): Boolean {
        return MineralType.fromBlock(state.block) != null
    }
}