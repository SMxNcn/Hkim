package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.*
import cn.hkim.addon.events.impl.*
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.render.drawWireFrameBox
import cn.hkim.addon.utils.skyblock.mining.HitPosHelper
import cn.hkim.addon.utils.skyblock.mining.MineralFilter
import cn.hkim.addon.utils.skyblock.mining.MineralType
import cn.hkim.addon.utils.skyblock.mining.MineralType.Companion.isHighPriorityBlock
import meteordevelopment.orbit.EventHandler
import net.minecraft.core.BlockPos
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.lwjgl.glfw.GLFW
import java.awt.Color

@ModuleInfo("nuker", Category.SKYBLOCK, false)
object Nuker : Module("Nuker", "Automatically breaks mineral blocks.") {
    private val aimSpeed by NumberSetting("Aim Speed", "Aim transition speed.", 0.25f, 0.05f, 1.0f, 0.05f)
    private val aimMode by SelectorSetting("Aim Mode", "Aim mode.", listOf("Normal", "Silent"), "Normal")
    private val toolType by SelectorSetting("Tool Type", "Tool used for mining.", listOf("Pickaxe", "Drill"), "Drill")
    private val targetType by SelectorSetting("Nuker Target", "", listOf("Gold", "Mithril", "Gemstones", "Glacite"), "Mithril")
    // Gold
    private val ignoreOthers by BooleanSetting("Gold Block Only", "Only mine gold blocks.", true).depends { targetType == 0 }
    private val royalDivan by BooleanSetting("Royal & Mines of Divan", "Only mine in Royal Mines and Mines of Divan.", false).depends { targetType == 0 && ignoreOthers }
    // Mithril
    private val inDwarvenOnly by BooleanSetting("Dwarven Only", "Only mine mithril ores in Dwarven Mines.", true).depends { targetType == 1 }
    // Gemstones
    private val gemRuby by BooleanSetting("Ruby", "", false).depends { targetType == 2 }
    private val gemAmber by BooleanSetting("Amber", "", false).depends { targetType == 2 }
    private val gemSapphire by BooleanSetting("Sapphire", "", false).depends { targetType == 2 }
    private val gemJade by BooleanSetting("Jade", "", false).depends { targetType == 2 }
    private val gemAmethyst by BooleanSetting("Amethyst", "", false).depends { targetType == 2 }
    private val gemTopaz by BooleanSetting("Topaz", "", false).depends { targetType == 2 }
    private val gemJasper by BooleanSetting("Jasper", "", false).depends { targetType == 2 }
    private val gemOpal by BooleanSetting("Opal", "", false).depends { targetType == 2 }
    private val gemOnyx by BooleanSetting("Onyx", "", false).depends { targetType == 2 }
    private val gemAquamarine by BooleanSetting("Aquamarine", "", false).depends { targetType == 2 }
    private val gemCitrine by BooleanSetting("Citrine", "", false).depends { targetType == 2 }
    private val gemPeridot by BooleanSetting("Peridot", "", false).depends { targetType == 2 }
    // Glacite
    private val metalUmber by BooleanSetting("Umber", "", false).depends { targetType == 3 }
    private val metalTungsten by BooleanSetting("Tungsten", "", false).depends { targetType == 3 }

    private val highlightColor by ColorSetting("Highlight Color", "Wireframe color of the target block.", Color(0xFFBC82FC.toInt()).rgb)
    private val toggleKeybind by KeybindSetting("Toggle Keybind", "Toggle Nuker.", GLFW.GLFW_KEY_K)

    private const val SCAN_RADIUS = 5.0

    private var currentTarget: BlockPos? = null
    private var currentHitPos: Vec3? = null
    private var currentMineral: MineralType? = null
    private var hasMineralsInRange = false

    private var wasScreenOpen = false
    private var screenJustClosed = false

    @EventHandler
    private fun onKey(event: InputEvent) {
        if (event.key.value == toggleKeybind) toggle()
    }

    @EventHandler
    private fun onTick(event: TickEvent.Start) {
        if (!enabled) return

        val player = mc.player ?: return
        val level = mc.level ?: return
        val screenOpen = mc.gui.screen() != null

        if (screenOpen) {
            holdKey(mc.options.keyAttack, false)
            wasScreenOpen = true
            return
        }

        if (wasScreenOpen) {
            currentTarget = null
            currentHitPos = null
            currentMineral = null
            hasMineralsInRange = false
            screenJustClosed = true
            wasScreenOpen = false
            return
        }

        screenJustClosed = false

        if (currentTarget != null) {
            val state = level.getBlockState(currentTarget!!)
            val mineralType = MineralType.fromBlock(state.block)
            val eyePos = player.eyePosition
            val blockCenter = Vec3(currentTarget!!.x + 0.5, currentTarget!!.y + 0.5, currentTarget!!.z + 0.5)
            val outOfRange = blockCenter.distanceToSqr(eyePos) > SCAN_RADIUS * SCAN_RADIUS
            if (outOfRange || mineralType == null || !MineralFilter.isMineralAllowed(mineralType, targetType, ignoreOthers, royalDivan, inDwarvenOnly, allowedTypes = getAllowedMineralTypes())) {
                currentTarget = null
                currentHitPos = null
                currentMineral = null
            }
        }

        if (currentTarget == null) {
            val target = scanBestTarget(player, level)
            if (target != null) {
                currentTarget = target.pos
                currentHitPos = target.hitPos
                currentMineral = target.mineralType
                hasMineralsInRange = true
            } else {
                hasMineralsInRange = false
            }
        } else {
            hasMineralsInRange = hasAnyMineralInRange(player, level)
        }

        holdKey(mc.options.keyAttack, currentTarget != null)
    }

    @EventHandler
    private fun onBlockUpdate(event: WorldEvent.BlockUpdate) {
        if (event.pos == currentTarget) {
            if (!HitPosHelper.matchesAnyMineral(event.newState)) {
                currentTarget = null
                currentHitPos = null
                currentMineral = null
            }
        }
    }

    @EventHandler
    private fun onRender(event: RenderEvent.Extract) {
        if (!enabled) {
            if (RotationUtils.isSilentAiming || RotationUtils.isStoppingAiming) {
                RotationUtils.tickStopAiming(aimSpeed / 5f)
            }
            return
        }

        if (mc.gui.screen() != null || wasScreenOpen || screenJustClosed) return

        val hitPos = currentHitPos
        if (hitPos != null) {
            aimAt(hitPos)
        } else if (!hasMineralsInRange && (RotationUtils.isSilentAiming || RotationUtils.isStoppingAiming)) {
            RotationUtils.tickStopAiming(aimSpeed / 5f)
            return
        }

        val pos = currentTarget ?: return
        event.drawWireFrameBox(
            AABB(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                 pos.x + 1.0, pos.y + 1.0, pos.z + 1.0),
            Color(highlightColor),
            2f, false
        )
    }

    override fun onEnable() {
        val player = mc.player ?: return
        val held = player.mainHandItem
        if (held.isEmpty || (!held.`is`(ItemTags.PICKAXES) && held.item != Items.PRISMARINE_SHARD)) {
            modMessage("${held.displayName.legacy} §cis not a valid mining tool!")
            enabled = false
            return
        }
        modMessage("§6Nuker§a enabled.")
    }

    override fun onDisable() {
        holdKey(mc.options.keyAttack, false)
        currentTarget = null
        currentHitPos = null
        currentMineral = null
        hasMineralsInRange = false
        modMessage("§6Nuker§c disabled.")
    }

    private data class Target(val pos: BlockPos, val hitPos: Vec3, val mineralType: MineralType)

    private inline fun forEachSphereBlock(eyePos: Vec3, action: (BlockPos) -> Unit) {
        val r = SCAN_RADIUS
        val radiusSq = r * r
        val minPos = BlockPos((eyePos.x - r).toInt(), (eyePos.y - r).toInt(), (eyePos.z - r).toInt())
        val maxPos = BlockPos((eyePos.x + r).toInt(), (eyePos.y + r).toInt(), (eyePos.z + r).toInt())

        val iter = BlockPos.betweenClosedStream(minPos, maxPos).iterator()
        while (iter.hasNext()) {
            val pos = iter.next()
            val center = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
            if (center.distanceToSqr(eyePos) <= radiusSq) action(pos)
        }
    }

    private fun scanBestTarget(player: Player, level: Level): Target? {
        val eyePos = player.eyePosition

        val highPri = scanClosest(eyePos, level) { mineralType, block ->
            mineralType.isHighPriorityBlock(block)
        }
        if (highPri != null) return highPri

        return scanClosest(eyePos, level) { _, _ -> true }
    }

    /** Scans the sphere for the closest block matching both standard and [extraFilter] criteria. */
    private fun scanClosest(
        eyePos: Vec3, level: Level,
        extraFilter: (MineralType, Block) -> Boolean
    ): Target? {
        val allowedTypes = getAllowedMineralTypes()
        var bestPos: BlockPos? = null
        var bestHitPos: Vec3? = null
        var bestMineralType: MineralType? = null
        var bestDistSq = Double.MAX_VALUE

        forEachSphereBlock(eyePos) { pos ->
            val state = level.getBlockState(pos)
            val mineralType = MineralType.fromBlock(state.block) ?: return@forEachSphereBlock
            if (!MineralFilter.isMineralAllowed(mineralType, targetType, ignoreOthers, royalDivan, inDwarvenOnly, allowedTypes = allowedTypes)) return@forEachSphereBlock
            if (!extraFilter(mineralType, state.block)) return@forEachSphereBlock

            val hitPos = HitPosHelper.pickHitPos(level, pos, state, eyePos) ?: return@forEachSphereBlock
            val distSq = hitPos.distanceToSqr(eyePos)
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                bestPos = pos.immutable()
                bestHitPos = hitPos
                bestMineralType = mineralType
            }
        }

        val foundPos = bestPos ?: return null
        return Target(foundPos, bestHitPos!!, bestMineralType!!)
    }

    private fun hasAnyMineralInRange(player: Player, level: Level): Boolean {
        val eyePos = player.eyePosition
        val allowedTypes = getAllowedMineralTypes()
        forEachSphereBlock(eyePos) { pos ->
            val mineralType = MineralType.fromBlock(level.getBlockState(pos).block) ?: return@forEachSphereBlock
            if (MineralFilter.isMineralAllowed(mineralType, targetType, ignoreOthers, royalDivan, inDwarvenOnly, allowedTypes = allowedTypes)) return true
        }
        return false
    }

    private fun getAllowedMineralTypes(): Set<MineralType> = MineralFilter.buildAllowedTypes(
        when (targetType) {
            2 -> mapOf(
                MineralType.RUBY to gemRuby,
                MineralType.AMBER to gemAmber,
                MineralType.SAPPHIRE to gemSapphire,
                MineralType.JADE to gemJade,
                MineralType.AMETHYST to gemAmethyst,
                MineralType.TOPAZ to gemTopaz,
                MineralType.JASPER to gemJasper,
                MineralType.OPAL to gemOpal,
                MineralType.ONYX to gemOnyx,
                MineralType.AQUAMARINE to gemAquamarine,
                MineralType.CITRINE to gemCitrine,
                MineralType.PERIDOT to gemPeridot,
            )
            3 -> mapOf(
                MineralType.UMBER to metalUmber,
                MineralType.TUNGSTEN to metalTungsten,
            )
            else -> emptyMap()
        }
    )

    private fun aimAt(hitPos: Vec3) {
        when (aimMode) {
            0 -> RotationUtils.aimVisible(hitPos, aimSpeed / 10f)
            1 -> RotationUtils.aimSilent(hitPos, aimSpeed / 10f, 1.2f)
        }
    }
}
