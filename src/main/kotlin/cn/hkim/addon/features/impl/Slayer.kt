package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.events.impl.PacketReceiveEvent
import cn.hkim.addon.events.impl.RenderEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.clean
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.render.drawText
import cn.hkim.addon.utils.renderPos
import cn.hkim.addon.utils.schedule
import cn.hkim.addon.utils.skyblock.LocationUtils
import cn.hkim.addon.utils.skyblock.slayer.HellionType
import cn.hkim.addon.utils.skyblock.slayer.SlayerUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Blaze
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

@ModuleInfo("slayer", Category.SKYBLOCK)
object Slayer : Module("Slayer", "Slayer features.") {
    private val sendKillTime by BooleanSetting("Send Kill Time", "Show your slayer boss kill time.", true)
    private val blaze by DropdownSetting("Blaze")
    private val hellionShield by BooleanSetting("Show Hellion Shield", "Show the current Hellion Shield type above the Blaze Slayer boss.", true).depends { blaze }
    private val hideParticles by BooleanSetting("Hide Particles", "Hide particles around the Blaze Slayer boss.", true).depends { blaze }
    private val particleRadius by NumberSetting("Hide Radius", "Radius which particles get hidden.", 10f, 2f, 10f, 1f).depends { blaze && hideParticles }

    private data class ShieldBoss(val entity: Entity, val type: HellionType, val hits: Int)

    private val shieldPattern = Regex("""(AURIC|ASHEN|SPIRIT|CRYSTAL)\s+\D?\s*(\d)(?:\s|$)""")
    private val shieldBosses = mutableListOf<ShieldBoss>()

    @Volatile
    private var particleCenters: List<Vec3> = emptyList()

    @EventHandler
    private fun onServerTick(event: TickEvent.Server) {
        if (!enabled || !LocationUtils.inSkyBlock) return
        SlayerUtils.onServerTick()
    }

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        if (!enabled || !LocationUtils.inSkyBlock) {
            if (shieldBosses.isNotEmpty()) shieldBosses.clear()
            if (particleCenters.isNotEmpty()) particleCenters = emptyList()
            SlayerUtils.resetState()
            return
        }

        SlayerUtils.updateState()?.let { killTime ->
            if (sendKillTime) {
                schedule(1) {
                    modMessage("Slayer took §6${"%.2f".format(killTime)}s§7 to kill!")
                }
            }
        }

        if (!hellionShield && !hideParticles) {
            if (shieldBosses.isNotEmpty()) shieldBosses.clear()
            if (particleCenters.isNotEmpty()) particleCenters = emptyList()
            return
        }

        val level = mc.level ?: return
        shieldBosses.clear()

        for (stand in level.entitiesForRendering()) {
            if (!stand.isAlive || stand !is ArmorStand) continue

            val match = shieldPattern.find(stand.name.string.clean) ?: continue
            val type = HellionType.entries.firstOrNull { it.name == match.groupValues[1] } ?: continue
            val hits = match.groupValues[2].toIntOrNull()?.takeIf { it in 1..8 } ?: continue

            val box = AABB(
                stand.x - 1.5, stand.y - 8.0, stand.z - 1.5,
                stand.x + 1.5, stand.y + 0.5, stand.z + 1.5
            )
            val boss = level.getEntities(null, box) { isBlazeBoss(it) }
                .minByOrNull { it.distanceToSqr(stand) } ?: continue

            shieldBosses.add(ShieldBoss(boss, type, hits))
        }

        particleCenters = if (hideParticles) shieldBosses.map { it.entity.position() } else emptyList()
    }

    @EventHandler
    private fun onRender(event: RenderEvent.Extract) {
        if (!enabled || !hellionShield || !LocationUtils.inSkyBlock || shieldBosses.isEmpty()) return

        for ((entity, type, hits) in shieldBosses) {
            if (!entity.isAlive) continue

            val text = Component.literal("${type.name} $hits").withColor(type.color.rgb).withStyle(ChatFormatting.BOLD)
            val pos = entity.renderPos.add(0.0, entity.bbHeight / 2.0 + 0.1, 0.0)
            event.drawText(text, pos, 1.5f, true, 0xFF000000.toInt() or type.color.rgb)
        }
    }

    @EventHandler
    private fun onPacketReceive(event: PacketReceiveEvent) {
        if (!enabled || !hideParticles) return

        val packet = event.packet as? ClientboundLevelParticlesPacket ?: return
        val centers = particleCenters
        if (centers.isEmpty()) return

        val radiusSqr = particleRadius * particleRadius
        if (centers.any { it.distanceToSqr(packet.x, packet.y, packet.z) <= radiusSqr }) {
            event.cancel()
        }
    }

    @EventHandler
    private fun onWorldLoad(event: WorldEvent.Load) {
        shieldBosses.clear()
        particleCenters = emptyList()
        SlayerUtils.resetState()
    }

    override fun onDisable() {
        shieldBosses.clear()
        particleCenters = emptyList()
        SlayerUtils.resetState()
    }

    private fun isBlazeBoss(entity: Entity): Boolean =
        entity.isAlive && (entity is Blaze || entity is ZombifiedPiglin || entity is AbstractSkeleton)
}