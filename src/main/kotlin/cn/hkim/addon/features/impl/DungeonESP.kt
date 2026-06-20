package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.ColorSetting
import cn.hkim.addon.events.impl.PacketReceiveEvent
import cn.hkim.addon.events.impl.RenderEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.events.impl.WorldEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.Colors
import cn.hkim.addon.utils.HudUtils.alert
import cn.hkim.addon.utils.HudUtils.multiplyAlpha
import cn.hkim.addon.utils.render.drawWireFrameBox
import cn.hkim.addon.utils.renderBoundingBox
import cn.hkim.addon.utils.skyblock.DungeonUtils
import cn.hkim.addon.utils.skyblock.LocationUtils
import cn.hkim.addon.utils.skyblock.M7Phases
import cn.hkim.addon.utils.skyblock.getF7Phase
import meteordevelopment.orbit.EventHandler
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import java.awt.Color

@ModuleInfo("dungeon_esp", Category.RENDER, false)
object DungeonESP : Module("Dungeon ESP", "ESP for dungeon entities.") {
    private val highlightStar by BooleanSetting("Highlight Starred Mobs", "Highlights starred dungeon mobs.", true)
    private val highlightInvisible by BooleanSetting("Highlight Invisible Mobs", "Highlight invisible starred dungeon mobs.", true).depends { highlightStar }
    private val highlightKeys by BooleanSetting("Highlight Keys", "Highlight wither and blood keys in dungeons.", true)
    private val highlightColor by ColorSetting("Highlight Color", "The color of the highlight.", Colors.WHITE.rgb)
    private val hideNonNames by BooleanSetting("Hide Non-starred Names", "Hides names of entities that are not starred.", true)

    private val batESP by BooleanSetting("Bat ESP", "Highlight Bat entities.", true)
    private val witherESP by BooleanSetting("Wither ESP", "Highlight Wither entities.", true)

    private val bloodColor by ColorSetting("Blood Color", "The color of the box.", Colors.BLACK.multiplyAlpha(0.8f).rgb)
    private val witherColor by ColorSetting("Wither Color", "The color of the box.", Colors.MINECRAFT_RED.multiplyAlpha(0.8f).rgb)

    private val dungeonMobSpawns = hashSetOf("Lurker", "Dreadlord", "Souleater", "Zombie", "Skeleton", "Skeletor", "Sniper", "Super Archer", "Spider", "Fels", "Withermancer", "Lost Adventurer", "Angry Archaeologist", "Frozen Adventurer", "Shadow Assassin")
    private val starredRegex = Regex("^.*✯ .*\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?[kM]?❤$")

    private val entities = mutableSetOf<Entity>()
    private var currentKey: KeyType? = null

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        if ((!highlightStar && !hideNonNames) || !DungeonUtils.inClear) return

        val entitiesToRemove = mutableListOf<Entity>()
        mc.level?.entitiesForRendering()?.forEach { entity ->
            if (!entity.isAlive || entity !is ArmorStand) return@forEach

            val entityName = entity.name.string
            if (!dungeonMobSpawns.any { it in entityName }) return@forEach

            val isStarred = starredRegex.matches(entityName)

            if (hideNonNames && entity.isInvisible && !isStarred && !highlightInvisible) {
                entitiesToRemove.add(entity)
                return@forEach
            }

            if (!isStarred) return@forEach

            mc.level?.getEntities(entity, entity.boundingBox.move(0.0, -1.0, 0.0)) { isValidEntity(it) }
                ?.firstOrNull()?.let { entities.add(it) }
        }

        entitiesToRemove.forEach { it.remove(Entity.RemovalReason.DISCARDED) }
        entities.removeIf { !it.isAlive }
    }

    @EventHandler
    private fun onRender(event: RenderEvent.Extract) {
        if (!LocationUtils.inDungeons) return

        if (DungeonUtils.inClear) {
            renderStarredMobs(event)
            renderKeys(event)
        }

        if (batESP || witherESP) renderBossMobs(event)
    }

    private fun renderStarredMobs(event: RenderEvent.Extract) {
        if (!highlightStar) return
        val color = Color(highlightColor)

        entities.forEach { entity ->
            if (entity.isAlive) event.drawWireFrameBox(entity.renderBoundingBox, color, 2f, false)
        }
    }

    private fun renderKeys(event: RenderEvent.Extract) {
        if (!highlightKeys) return
        val key = currentKey ?: return
        val keyEntity = key.entity ?: return
        if (!keyEntity.isAlive) {
            currentKey = null
            return
        }
        val pos = keyEntity.position()
        event.drawWireFrameBox(AABB.unitCubeFromLowerCorner(pos.add(-0.5, 1.0, -0.5)), Color(key.color()), 2f, false)
    }

    private fun renderBossMobs(event: RenderEvent.Extract) {
        val color = Color(highlightColor)
        val level = mc.level ?: return

        for (entity in level.entitiesForRendering()) {
            if (!entity.isAlive || entity.isInvisible) continue

            when {
                witherESP && entity is WitherBoss -> {
                    if (!DungeonUtils.inBoss || DungeonUtils.floor?.floorNumber != 7 || getF7Phase() == M7Phases.P5) continue
                    if (entity.invulnerableTicks == 800) continue
                    event.drawWireFrameBox(entity.renderBoundingBox, color, 2f, false)
                }

                batESP && entity.type == EntityType.BAT -> {
                    event.drawWireFrameBox(entity.renderBoundingBox, color, 2f, false)
                }
            }
        }
    }

    @EventHandler
    private fun onPacket(event: PacketReceiveEvent) {
        if (!LocationUtils.inDungeons || !DungeonUtils.inClear) return
        if (event.packet !is ClientboundSetEntityDataPacket) return

        val entity = mc.level?.getEntity(event.packet.id) as? ArmorStand ?: return
        if (currentKey?.entity == entity) return

        val keyType = KeyType.entries.find { it.displayName == entity.name.string } ?: return
        keyType.entity = entity
        currentKey = keyType
        alert("§${keyType.colorCode}${entity.name.string} §7spawned!")
    }

    @EventHandler
    private fun onWorldLoad(event: WorldEvent.Load) {
        entities.clear()
        currentKey = null
    }

    private fun isValidEntity(entity: Entity): Boolean = when (entity) {
        is ArmorStand -> false
        is WitherBoss -> false
        is Player -> entity.uuid.version() == 2 && entity != mc.player
        else -> highlightInvisible || !entity.isInvisible
    }

    private enum class KeyType(val displayName: String, val color: () -> Int, val colorCode: Char) {
        Wither("Wither Key", { witherColor }, '8'),
        Blood("Blood Key", { bloodColor }, 'c');

        var entity: Entity? = null
    }
}
