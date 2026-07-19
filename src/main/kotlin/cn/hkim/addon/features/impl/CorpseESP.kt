package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.PacketSendEvent
import cn.hkim.addon.events.impl.RenderEvent
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.itemId
import cn.hkim.addon.utils.render.drawWireFrameBox
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import java.awt.Color

@ModuleInfo("corpse_esp", Category.RENDER, false)
object CorpseESP : Module("Corpse ESP", "Highlights corpses in the Mineshaft.") {
    private data class CorpseInfo(val entity: Entity, val color: Int)

    private val helmetColorMap = mapOf(
        "LAPIS_ARMOR_HELMET" to 0x5555FF,
        "MINERAL_HELMET" to 0xAAAAAA,
        "ARMOR_OF_YOG_HELMET" to 0xFFAA00,
        "VANGUARD_HELMET" to 0x00AAAA
    )

    private val corpses = mutableListOf<CorpseInfo>()
    private val dismissedIds by lazy { mutableSetOf<Int>() }

    private fun refreshCorpses() {
        if (!LocationUtils.isCurrentArea(Island.Mineshaft)) {
            if (corpses.isNotEmpty()) corpses.clear()
            return
        }

        val level = mc.level ?: return

        corpses.removeAll { corpse ->
            if (!corpse.entity.isAlive) {
                dismissedIds.remove(corpse.entity.id)
                true
            } else false
        }

        val trackedIds = corpses.map { it.entity.id }.toMutableSet()

        for (entity in level.entitiesForRendering()) {
            if (!entity.isAlive || entity !is ArmorStand) continue
            if (entity.id in trackedIds || entity.id in dismissedIds) continue

            val headItem = entity.getItemBySlot(EquipmentSlot.HEAD)
            if (headItem.isEmpty) continue

            val color = helmetColorMap[headItem.itemId] ?: continue

            corpses.add(CorpseInfo(entity, color))
            trackedIds.add(entity.id)
        }
    }

    @EventHandler
    private fun onPacketSend(event: PacketSendEvent) {
        val packet = event.packet as? ServerboundInteractPacket ?: return

        val entityId = packet.entityId()
        if (corpses.removeAll { it.entity.id == entityId }) {
            dismissedIds.add(entityId)
        }
    }

    @EventHandler
    private fun onWorldUnload(event: WorldEvent.Unload) {
        corpses.clear()
        dismissedIds.clear()
    }

    @EventHandler
    private fun onExtract(event: RenderEvent.Extract) {
        if (!enabled) return
        refreshCorpses()
        for (corpse in corpses) {
            event.drawWireFrameBox(corpse.entity.boundingBox, Color(corpse.color), 2f)
        }
    }
}
