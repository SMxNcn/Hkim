package cn.hkim.addon.config.debug

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.features.impl.FreeCam
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer
import net.minecraft.client.gui.components.debug.DebugScreenEntry
import net.minecraft.resources.Identifier
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

object FreecamDebugEntry : DebugScreenEntry {
    private val GROUP = Identifier.fromNamespaceAndPath("hkim", "freecam_target_block")

    override fun display(displayer: DebugScreenDisplayer, level: Level?, clientChunk: LevelChunk?, serverChunk: LevelChunk?) {
        if (!FreeCam.isFreecamActive) return
        val world = mc.level ?: return
        val player = mc.player ?: return

        val from = Vec3(FreeCam.camX, FreeCam.camY, FreeCam.camZ)
        val direction = player.calculateViewVector(FreeCam.camXRot, FreeCam.camYRot)
        val to = from.add(direction.scale(10.0))

        val hit = world.clip(ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
        if (hit.type == HitResult.Type.BLOCK) {
            val pos = hit.blockPos
            displayer.addToGroup(GROUP, "FreeCam Target Block: ${pos.x} ${pos.y} ${pos.z}")
        }
    }
}