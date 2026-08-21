package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.events.impl.RenderEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.render.drawWireFrameBox
import cn.hkim.addon.utils.skyblock.Island
import cn.hkim.addon.utils.skyblock.LocationUtils
import cn.hkim.addon.utils.skyblock.hunting.SafariArea
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import meteordevelopment.orbit.EventHandler
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Interaction
import net.minecraft.world.entity.animal.bee.Bee
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import java.awt.Color

@ModuleInfo("hunting_helper", Category.SKYBLOCK)
object HuntingHelper : Module("Hunting Helper", "Features for creature hunting.") {
    private val torrhus by DropdownSetting("Torrhus Canyon")
    private val hideonsunEsp by BooleanSetting("Hideonsun ESP", "Highlight Hideonsun  in Torrhus Canyon.", false).depends { torrhus }
    private val pangolinEsp by BooleanSetting("Pangolin ESP", "Highlight Pangolin in Torrhus Canyon.", false).depends { torrhus }
    private val beeHeemothEsp by BooleanSetting("BeeHeemoth ESP", "Highlight BeeHeemoth in Torrhus Canyon.", false).depends { torrhus }
    private val tikiEsp by BooleanSetting("Tiki ESP", "Highlight Tiki in Torrhus Canyon.", false).depends { torrhus }

    private val safari by DropdownSetting("Critter Safari")
    private val hideonwallEsp by BooleanSetting("Hideonwall ESP", "Highlight Hideonwall in Safari.", true).depends { safari }
    private val hideonfloorEsp by BooleanSetting("Hideonfloor ESP", "Highlight Hideonfloor in Safari.", true).depends { safari }
    private val bloodbatEsp by BooleanSetting("Bloodbat ESP", "Highlight Bloodbat in Safari.", true).depends { safari }
    private val floordropEsp by BooleanSetting("Floordrop ESP", "Highlight floordrops in Safari.", false).depends { safari }
    private val duplicoEsp by BooleanSetting("Duplico ESP", "Highlight hiding Duplico in safari.", true).depends { safari }
    private val hideyhoEsp by BooleanSetting("Hidehyo ESP", "Highlight Hideyho NPC in Safari.", true).depends { safari }
    private val autoFloordrop by BooleanSetting("Auto Floordrop", "Auto pick up floordrops in Safari.", false).depends { safari }

    data class HuntingMob(
        val name: String,
        val type: EntityType<*>,
        val color: Color,
        val enabled: () -> Boolean,
        val area: Island? = null,
        val extra: (Entity) -> Boolean = { true },
    ) {
        val targets = mutableListOf<Entity>()
    }

    private val huntingMobs = listOf(
        HuntingMob("Hideonsun", EntityType.SHULKER, Colors.MINECRAFT_GOLD, { hideonsunEsp },
            area = Island.TorrhusCanyon,
            extra = { val c = (it as Shulker).color; c == DyeColor.YELLOW || c == DyeColor.ORANGE || c == DyeColor.BROWN }),
        HuntingMob("Hideonwall", EntityType.SHULKER, Colors.MINECRAFT_DARK_PURPLE, { hideonwallEsp },
            area = Island.Safari,
            extra = { (it as Shulker).color == DyeColor.PURPLE }),
        HuntingMob("Hideonfloor", EntityType.SHULKER, Colors.MINECRAFT_DARK_GREEN, { hideonfloorEsp },
            area = Island.Safari,
            extra = { (it as Shulker).color == DyeColor.GREEN }),
        HuntingMob("Bloodbat", EntityType.BAT, Colors.MINECRAFT_RED, { bloodbatEsp },
            area = Island.Safari,
            extra = { SafariArea.getCurrentArea() == SafariArea.Haunted }),

        HuntingMob("Pangolin", EntityType.ARMADILLO, Color(0x9F5656), { pangolinEsp }),
        HuntingMob("BeeHeemoth", EntityType.BEE, Color(0xFECE3E), { beeHeemothEsp }, extra = { (it as Bee).scale == 9.0f })
    )

    private val floordrops = mutableSetOf<BlockPos>()
    private val tikis = mutableListOf<BlockPos>()

    private var scanTick = 0
    private var lastTikiScan = 0L
    private var lastFloordropClick = 0L

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        if (!enabled || !isHuntingArea()) return
        val player = mc.player ?: return
        val now = System.currentTimeMillis()

        if (tikiEsp && LocationUtils.isCurrentArea(Island.TorrhusCanyon) && now - lastTikiScan >= 1000L) {
            scanTikis()
            lastTikiScan = now
        }

        if (++scanTick >= 10) {
            scanTick = 0
            refreshTargets()
        }

        tryAutoFloordrop(player, now)
    }

    @EventHandler
    private fun onRender(event: RenderEvent.Extract) {
        if (!enabled || !isHuntingArea()) return
        val player = mc.player ?: return
        val level = mc.level ?: return

        if (SafariArea.getCurrentArea() == SafariArea.Haunted) {
            val area = SafariArea.Haunted
            if (duplicoEsp) {
                for (entity in level.entitiesForRendering()) {
                    if (entity is Interaction && isPositionInArea(area.corner1, area.corner2, entity.blockPosition())) {
                        event.drawWireFrameBox(entity.renderBoundingBox, Colors.MINECRAFT_GREEN, 2f)
                    }
                }
            }
            if (hideyhoEsp) {
                for (entity in level.players()) {
                    if (entity == player || entity.isRemoved || !entity.isAlive || !entity.name.string.contains("Hideyho")) continue
                    event.drawWireFrameBox(entity.renderBoundingBox, Color(0xA335EE), 2f)
                }
            }
        }

        renderMobEsp(event)
        renderFloordropEsp(event)
        renderTikiEsp(event)
    }

    private fun renderMobEsp(event: RenderEvent.Extract) {
        for (mob in huntingMobs) {
            if (!mob.enabled()) continue
            for (entity in mob.targets) {
                if (entity.isAlive) event.drawWireFrameBox(entity.renderBoundingBox, mob.color, 2f)
            }
        }
    }

    private fun renderFloordropEsp(event: RenderEvent.Extract) {
        if (!floordropEsp) return
        val area = SafariArea.getCurrentArea() ?: return
        if (area == SafariArea.Spawn) return
        for (pos in floordrops) {
            if (!isPositionInArea(area.corner1, area.corner2, pos)) continue
            event.drawWireFrameBox(floordropBox(pos), Colors.WHITE, 2f)
        }
    }

    private fun renderTikiEsp(event: RenderEvent.Extract) {
        if (!tikiEsp || !LocationUtils.isCurrentArea(Island.TorrhusCanyon)) return
        for (pos in tikis) {
            event.drawWireFrameBox(AABB(
                pos.x + 0.1, pos.y.toDouble(), pos.z + 0.1,
                pos.x + 0.9, pos.y + 2.5, pos.z + 0.9), Color(0xC5BF8E), 2f)
        }
    }

    private fun refreshTargets() {
        val level = mc.level ?: return
        huntingMobs.forEach { it.targets.clear() }
        floordrops.clear()

        val activeMobs = huntingMobs.filter { it.area == null || LocationUtils.isCurrentArea(it.area) }
        for (entity in level.entitiesForRendering()) {
            if (!entity.isAlive) continue
            for (mob in activeMobs) {
                if (entity.type == mob.type && mob.extra(entity)) mob.targets.add(entity)
            }
            if ((floordropEsp || autoFloordrop) && entity is Display.ItemDisplay && entity.itemStack.item == Items.STRING) {
                if (LocationUtils.isCurrentArea(Island.Safari)) floordrops.add(entity.blockPosition())
            }
        }
    }

    private fun scanTikis() {
        val level = mc.level ?: return
        tikis.clear()

        val renderDist = mc.options.renderDistance().get()
        val playerChunkX = (mc.player?.blockX ?: return) shr 4
        val playerChunkZ = (mc.player?.blockZ ?: return) shr 4

        for (cx in playerChunkX - renderDist..playerChunkX + renderDist) {
            for (cz in playerChunkZ - renderDist..playerChunkZ + renderDist) {
                if (!level.isLoaded(BlockPos(cx * 16, 0, cz * 16))) continue
                val chunk = level.getChunk(cx, cz)

                for (pos in chunk.getBlockEntities().keys) {
                    if (!level.getBlockState(pos).`is`(Blocks.PLAYER_HEAD)) continue

                    if (!level.getBlockState(pos.below()).`is`(Blocks.PLAYER_HEAD) &&
                        level.getBlockState(pos.above()).`is`(Blocks.PLAYER_HEAD) &&
                        level.getBlockState(pos.above(2)).`is`(Blocks.PLAYER_HEAD)) {
                        tikis.add(pos)
                    }
                }
            }
        }
    }

    private fun tryAutoFloordrop(player: Player, now: Long) {
        if (!autoFloordrop || !aimedFloordrop(player) || !LocationUtils.isCurrentArea(Island.Safari)) return
        if (now - lastFloordropClick < 1000L) return
        lastFloordropClick = now
        Hkim.scope.launch {
            delay(randomDelay(250, 100))
            leftClick()
        }
    }

    private fun aimedFloordrop(player: Player): Boolean {
        val eye = player.eyePosition
        val end = eye.add(player.lookAngle.scale(4.0))
        return floordrops.any { pos -> floordropBox(pos).intersects(eye, end) }
    }

    private fun floordropBox(pos: BlockPos): AABB = AABB(
        pos.x.toDouble(), pos.y + 1.0, pos.z.toDouble(),
        pos.x + 1.0, pos.y + 1.125, pos.z + 1.0)

    private fun isHuntingArea(): Boolean =
        LocationUtils.isCurrentArea(Island.MoongladeMarsh, Island.TorrhusCanyon, Island.Safari)
}