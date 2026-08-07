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
import net.minecraft.world.entity.animal.bee.Bee
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.awt.Color

@ModuleInfo("hunting_helper", Category.SKYBLOCK)
object HuntingHelper : Module("Hunting Helper", "Features for creature hunting.") {
    private val esp by DropdownSetting("ESP")
    private val hideonsunEsp by BooleanSetting("Hideonsun ESP", "Highlight Hideonsun.", false).depends { esp }
    private val pangolinEsp by BooleanSetting("Pangolin ESP", "Highlight Pangolin.", false).depends { esp }
    private val beeHeemothEsp by BooleanSetting("BeeHeemoth ESP", "Highlight BeeHeemoth.", false).depends { esp }
    private val blueJayEsp by BooleanSetting("Blue Jay ESP", "Highlight Blue Jay.", false).depends { esp }
    private val hideonwallEsp by BooleanSetting("Hideonwall ESP", "Highlight Hideonwall  in Safari.", false).depends { esp }
    private val hideonfloorEsp by BooleanSetting("Hideonfloor ESP", "Highlight Hideonfloor in Safari.", false).depends { esp }
    private val bloodbatEsp by BooleanSetting("Bloodbat ESP", "Highlight Bloodbat in Safari.", false).depends { esp }
    private val floordropEsp by BooleanSetting("Floordrop ESP", "Highlight floordrop item in Safari.", false).depends { esp }
    private val tikiEsp by BooleanSetting("Tiki ESP", "Highlight Tiki in Torrhus Canyon.", false).depends { esp }

    private val triggerbot by DropdownSetting("Triggerbot")
    private val autoReel by BooleanSetting("Auto Reel", "Auto reel Lasso.", false).depends { triggerbot }
    private val autoFloordrop by BooleanSetting("Auto Floordrop", "Auto right-click at a floordrop.", false).depends { triggerbot }

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
            extra = { val c = (it as Shulker).color; c == DyeColor.YELLOW || c == DyeColor.BROWN }),
        HuntingMob("Hideonwall", EntityType.SHULKER, Colors.MINECRAFT_DARK_PURPLE, { hideonwallEsp },
            area = Island.Safari,
            extra = { (it as Shulker).color == DyeColor.PURPLE }),
        HuntingMob("Hideonfloor", EntityType.SHULKER, Colors.MINECRAFT_DARK_GREEN, { hideonfloorEsp },
            area = Island.Safari,
            extra = { (it as Shulker).color == DyeColor.GREEN }),
        HuntingMob("Bloodbat", EntityType.BAT, Colors.MINECRAFT_RED, { bloodbatEsp },
            area = Island.Safari),

        HuntingMob("Pangolin", EntityType.ARMADILLO, Color(0x9F5656), { pangolinEsp }),
        HuntingMob("BeeHeemoth", EntityType.BEE, Color(0xFECE3E), { beeHeemothEsp }, extra = { (it as Bee).scale == 9.0f }),
        HuntingMob("Blue Jay", EntityType.PARROT, Colors.MINECRAFT_BLUE, { blueJayEsp }, area = Island.TorrhusCanyon),
    )

    private val floordrops = mutableSetOf<BlockPos>()
    private val reelStands = mutableListOf<ArmorStand>()
    private val tikis = mutableListOf<BlockPos>()

    private var scanTick = 0
    private var lastTikiScan = 0L
    private var lastReelTime = 0L
    private var reelingStandId = -1
    private var lastFloordropClick = 0L

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        if (!enabled || !isHuntingArea()) return
        val now = System.currentTimeMillis()

        if (tikiEsp && LocationUtils.isCurrentArea(Island.TorrhusCanyon) && now - lastTikiScan >= 1000L) {
            scanTikis()
            lastTikiScan = now
        }

        if (++scanTick >= 10) {
            scanTick = 0
            refreshTargets()
        }

        val player = mc.player ?: return
        tryAutoReel(player, now)
        tryAutoFloordrop(player, now)
    }

    @EventHandler
    private fun onRender(event: RenderEvent.Extract) {
        if (!enabled || !isHuntingArea()) return

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
        reelStands.clear()
        floordrops.clear()

        val activeMobs = huntingMobs.filter { it.area == null || LocationUtils.isCurrentArea(it.area) }
        for (entity in level.entitiesForRendering()) {
            if (!entity.isAlive) continue
            for (mob in activeMobs) {
                if (entity.type == mob.type && mob.extra(entity)) mob.targets.add(entity)
            }
            if (entity is ArmorStand && entity.displayName.cleanString.contains("REEL", ignoreCase = true)) {
                reelStands.add(entity)
            }
            if ((floordropEsp || autoFloordrop) && entity is Display.ItemDisplay &&
                entity.itemRenderState()?.itemStack()?.item == Items.STRING) {
                floordrops.add(entity.blockPosition())
            }
        }
    }

    private fun scanTikis() {
        val level = mc.level ?: return
        tikis.clear()

        val renderDist = mc.options.renderDistance().get()
        val playerChunkX = (mc.player?.blockX ?: return) shr 4
        val playerChunkZ = mc.player!!.blockZ shr 4

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

    private fun tryAutoReel(player: Player, now: Long) {
        if (!autoReel) return

        if (reelingStandId != -1) {
            val stand = mc.level?.getEntity(reelingStandId)
            reelingStandId = when {
                stand == null || !stand.isAlive -> -1
                now - lastReelTime >= 3000L -> -1
                else -> return
            }
        }
        if (now - lastReelTime < 250L) return

        val held = player.mainHandItem
        if (!held.itemId.contains("LASSO", ignoreCase = true)) return

        val stand = reelStands.firstOrNull {
            it.isAlive && it.distanceToSqr(player) <= 225.0 && isAimedAt(player, it.getEyePosition()) // 15 blocks
        }
        if (stand != null) {
            lastReelTime = now
            reelingStandId = stand.id
            Hkim.scope.launch {
                delay(randomDelay(150, 50))
                rightClick()
            }
        }
    }

    private fun tryAutoFloordrop(player: Player, now: Long) {
        if (!autoFloordrop) return
        if (!aimedFloordrop(player)) return
        if (now - lastFloordropClick < 150L) return
        lastFloordropClick = now
        Hkim.scope.launch {
            delay(randomDelay(150, 100))
            rightClick()
        }
    }

    private fun isAimedAt(player: Player, target: Vec3): Boolean {
        val toTarget = target.subtract(player.eyePosition)
        val dist = toTarget.length()
        if (dist <= 0.0) return false
        return player.lookAngle.dot(toTarget.scale(1.0 / dist)) >= 0.7
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