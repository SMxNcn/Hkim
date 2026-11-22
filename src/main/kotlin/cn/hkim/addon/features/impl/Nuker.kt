package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.compat.input.InputCompat
import cn.hkim.addon.config.settings.*
import cn.hkim.addon.events.impl.*
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.mixins.accessors.MultiPlayerGameModeAccessor
import cn.hkim.addon.utils.*
import cn.hkim.addon.utils.notification.NotificationManager
import cn.hkim.addon.utils.render.drawWireFrameBox
import cn.hkim.addon.utils.skyblock.mining.*
import cn.hkim.addon.utils.skyblock.mining.MineralType.Companion.isHighPriorityBlock
import meteordevelopment.orbit.EventHandler
import net.minecraft.core.BlockPos
import net.minecraft.tags.ItemTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.awt.Color

@ModuleInfo("nuker", Category.SKYBLOCK, false)
object Nuker : Module("Nuker", "Automatically breaks mineral blocks.") {
    private val aimSpeed by NumberSetting("Aim Speed", "Aim transition speed.", 0.25f, 0.05f, 1.0f, 0.05f)
    private val aimMode by SelectorSetting("Aim Mode", "Aim mode.", listOf("Normal", "Silent"), "Normal")
    private val preAiming by BooleanSetting("Pre Aiming", "Pingless aiming: estimate the server break from Mining Speed + ping.", false)
    private val preAimThreshold by NumberSetting("Pre Aim Threshold", "Mining progress required to pre-aim the next target.", 0.9f, 0.1f, 0.99f, 0.05f).depends { preAiming }
    private val pingGlide by NumberSetting("Ping Glide", "Fraction of estimated server break time.", 0f, 0f, 1f, 0.1f).depends { preAiming }
    private val targetType by SelectorSetting("Nuker Target", "Choose which mineral type to mine.", listOf("Gold", "Mithril", "Gemstones", "Glacite", "Timite"), "Mithril")

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
    private val toggleKeybind by KeybindSetting("Toggle Keybind", "Toggle Nuker.", InputCompat.KEY_K)

    private const val SCAN_RADIUS = 5.0
    private const val CLUSTER_RADIUS = 2.0
    private const val CLUSTER_RADIUS_SQ = CLUSTER_RADIUS * CLUSTER_RADIUS

    private var currentTarget: BlockPos? = null
    private var currentHitPos: Vec3? = null
    private var currentMineral: MineralType? = null
    private var hasMineralsInRange = false

    private var lastTargetPos: BlockPos? = null

    private var wasScreenOpen = false
    private var screenJustClosed = false
    private var cachedAllowedTypes: Set<MineralType>? = null

    private var targetAcquiredTime = 0L

    @JvmField
    var isTimiteSlotSwitch = false

    private var pendingSlot = -1

    @EventHandler
    private fun onKey(event: InputEvent) {
        if (event.matches(toggleKeybind)) toggle()
    }

    @EventHandler
    private fun onTick(event: TickEvent.Start) {
        if (!enabled) return

        cachedAllowedTypes = null
        val player = mc.player ?: return
        val level = mc.level ?: return
        val screenOpen = mc.gui.screen() != null

        if (screenOpen) {
            holdKey(mc.options.keyAttack, false)
            holdKey(mc.options.keyUse, false)
            Pingless.reset()
            wasScreenOpen = true
            return
        }

        if (wasScreenOpen) {
            holdKey(mc.options.keyUse, false)
            clearTarget()
            Pingless.reset()
            hasMineralsInRange = false
            screenJustClosed = true
            wasScreenOpen = false
            return
        }

        screenJustClosed = false

        val target = currentTarget
        if (target != null) {
            val state = level.getBlockState(target)
            val mineralType = MineralType.fromBlock(state.block)
            val eyePos = player.eyePosition
            val dx = target.x + 0.5 - eyePos.x
            val dy = target.y + 0.5 - eyePos.y
            val dz = target.z + 0.5 - eyePos.z
            val outOfRange = dx * dx + dy * dy + dz * dz > SCAN_RADIUS * SCAN_RADIUS

            val hitPos = currentHitPos
            val reachSq = player.blockInteractionRange().let { it * it }
            val outOfReach = hitPos == null || hitPos.distanceToSqr(eyePos) > reachSq

            when {
                outOfRange || outOfReach -> {
                    Pingless.reset()
                    clearTarget()
                }
                mineralType == null -> {
                    Pingless.reset()
                    lastTargetPos = target
                    clearTarget()
                }
                !MineralFilter.isMineralAllowed(mineralType, targetType, ignoreOthers, royalDivan, inDwarvenOnly, allowedTypes = resolveAllowedMineralTypes()) -> {
                    Pingless.reset()
                    clearTarget()
                }
                targetType == 4 && System.currentTimeMillis() - targetAcquiredTime >= 15_000L -> {
                    Pingless.reset()
                    clearTarget()
                }
                else -> currentMineral = mineralType
            }
        }

        val etaReady = preAiming && targetType != 4 && currentTarget != null && Pingless.update(pingGlide)

        if (preAiming) {
            val aimTarget = currentTarget
            if (aimTarget != null) {
                if (etaReady) {
                    if (Pingless.isReady(player)) {
                        lastTargetPos = aimTarget
                        Pingless.reset()
                        clearTarget()
                    }
                } else if (Pingless.hasStarted) {
                    val destroyProgress = (mc.gameMode as MultiPlayerGameModeAccessor?)?.destroyProgress ?: 0f
                    if (destroyProgress >= preAimThreshold) {
                        val step = level.getBlockState(aimTarget).getDestroyProgress(player, level, aimTarget)
                        if (destroyProgress + step >= 1f) {
                            lastTargetPos = aimTarget
                            Pingless.reset()
                            clearTarget()
                        }
                    }
                }
            }
        }

        if (currentTarget == null) {
            scanBestTarget(player, level)?.let { t ->
                Pingless.setTarget(t.pos)
                currentTarget = t.pos
                currentHitPos = t.hitPos
                currentMineral = t.mineralType
                targetAcquiredTime = System.currentTimeMillis()
            }
        }

        hasMineralsInRange = currentTarget != null

        if (currentTarget != null) resetStaleDestroyPos()

        if (targetType == 4) {
            executeTimiteAction(player)
        } else {
            val stillMining = mc.gameMode?.isDestroying == true
            holdKey(mc.options.keyAttack, currentTarget != null || stillMining)
        }
    }

    private fun resetStaleDestroyPos() {
        val gameMode = mc.gameMode ?: return
        if (gameMode.isDestroying) return
        (gameMode as? MultiPlayerGameModeAccessor)?.setDestroyBlockPos(null)
    }

    private fun abortCurrentMining() {
        val gameMode = mc.gameMode ?: return
        if (gameMode.isDestroying) gameMode.stopDestroyBlock()
        (gameMode as? MultiPlayerGameModeAccessor)?.setDestroyBlockPos(null)
    }

    @EventHandler
    private fun onBlockUpdate(event: WorldEvent.BlockUpdate) {
        val target = currentTarget
        if (target != null && event.pos == target) {
            if (!HitPosHelper.matchesAnyMineral(event.newState)) {
                Pingless.reset()
                lastTargetPos = target
                clearTarget()
            } else {
                abortCurrentMining()
                Pingless.reset()
                lastTargetPos = target
                clearTarget()
            }
        }
        if (targetType == 4 && HitPosHelper.matchesAnyMineral(event.oldState)
            && !HitPosHelper.matchesAnyMineral(event.newState)) {
            TimiteHelper.scanNow()
        }
    }

    @EventHandler
    private fun onRender(event: RenderEvent.Extract) {
        if (!enabled) {
            if (RotationUtils.isSilentAiming || RotationUtils.isStoppingAiming) {
                RotationUtils.tickStopAiming(aimSpeed / 2f, this)
            }
            return
        }

        if (mc.gui.screen() != null || wasScreenOpen || screenJustClosed) return

        val hitPos = currentHitPos
        if (hitPos != null) {
            aimAt(hitPos)
        } else if (!hasMineralsInRange && (RotationUtils.isSilentAiming || RotationUtils.isStoppingAiming)) {
            RotationUtils.tickStopAiming(aimSpeed / 2f, this)
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

    private fun executeTimiteAction(player: Player) {
        val blockMineral = currentMineral ?: run {
            holdKey(mc.options.keyUse, false)
            holdKey(mc.options.keyAttack, false)
            return
        }
        val wantTarget = TimiteHelper.currentTarget
        val blockStage = RIFT_STAGE[blockMineral] ?: return
        val targetStage = wantTarget?.let { RIFT_STAGE[it] } ?: 2

        if (blockStage < targetStage) {
            holdKey(mc.options.keyAttack, false)
            val gunSlot = findItemByID(TimiteHelper.TIME_GUN_ID, hotbar = true)
            if (gunSlot >= 0 && switchTo(player, gunSlot)) {
                holdKey(mc.options.keyUse, true)
            } else {
                holdKey(mc.options.keyUse, false)
            }
        } else {
            holdKey(mc.options.keyUse, false)
            val pickSlot = findItemByID(TimiteHelper.CHRONO_PICKAXE_ID, hotbar = true)
            if (pickSlot >= 0 && switchTo(player, pickSlot)) {
                holdKey(mc.options.keyAttack, true)
            } else {
                holdKey(mc.options.keyAttack, false)
            }
        }
    }

    private fun switchTo(player: Player, targetSlot: Int): Boolean {
        if (player.inventory.selectedSlot == targetSlot) {
            pendingSlot = -1
            return true
        }
        if (pendingSlot == targetSlot) return false
        pendingSlot = targetSlot
        schedule(randomDelay(1, 3).toInt()) {
            if (player.inventory.selectedSlot != targetSlot) {
                isTimiteSlotSwitch = true
                player.inventory.selectedSlot = targetSlot
            }
            pendingSlot = -1
        }
        return false
    }

    private val RIFT_STAGE = mapOf(
        MineralType.YOUNGITE to 0,
        MineralType.TIMITE to 1,
        MineralType.OBSOLITE to 2,
    )

    override fun onEnable() {
        lastTargetPos = null
        Pingless.reset()
        val player = mc.player ?: return
        val held = player.mainHandItem

        if (targetType == 4) {
            if (!TimiteHelper.hasValidTimiteItems()) {
                modMessage("§cRequires Chrono Pickaxe and Time Gun in hotbar!")
                enabled = false
                return
            }
            TimiteHelper.reset()
            TimiteHelper.scanNow()
        } else {
            if (held.isEmpty || (!held.`is`(ItemTags.PICKAXES) && !held.itemId.containsOneOf("DRILL" , "GEMSTONE_GAUNTLET"))) {
                modMessage("${held.hoverName.legacy} §cis not a valid mining tool!")
                enabled = false
                return
            }
        }
        NotificationManager.toggle(name, true)
    }

    override fun onDisable() {
        holdKey(mc.options.keyAttack, false)
        holdKey(mc.options.keyUse, false)
        clearTarget()
        lastTargetPos = null
        Pingless.reset()
        hasMineralsInRange = false
        cachedAllowedTypes = null
        targetAcquiredTime = 0L
        TimiteHelper.reset()
        NotificationManager.toggle(name, false)
    }

    private fun clearTarget() {
        currentTarget = null
        currentHitPos = null
        currentMineral = null
        pendingSlot = -1
    }

    private data class Target(val pos: BlockPos, val hitPos: Vec3, val mineralType: MineralType)

    private class NearestTarget {
        var target: Target? = null
            private set
        private var distSq = Double.MAX_VALUE

        fun offer(distSq: Double, pos: BlockPos, hitPos: Vec3, mineralType: MineralType) {
            if (distSq >= this.distSq) return
            this.distSq = distSq
            target = Target(pos.immutable(), hitPos, mineralType)
        }
    }

    private inline fun forEachSphereBlock(eyePos: Vec3, action: (BlockPos) -> Unit) {
        val r = SCAN_RADIUS
        val radiusSq = r * r
        val ex = eyePos.x; val ey = eyePos.y; val ez = eyePos.z
        val minPos = BlockPos(Mth.floor(ex - r), Mth.floor(ey - r), Mth.floor(ez - r))
        val maxPos = BlockPos(Mth.floor(ex + r), Mth.floor(ey + r), Mth.floor(ez + r))

        val iter = BlockPos.betweenClosedStream(minPos, maxPos).iterator()
        while (iter.hasNext()) {
            val pos = iter.next()
            val dx = pos.x + 0.5 - ex
            val dy = pos.y + 0.5 - ey
            val dz = pos.z + 0.5 - ez
            if (dx * dx + dy * dy + dz * dz <= radiusSq) action(pos)
        }
    }

    private fun scanBestTarget(player: Player, level: Level): Target? {
        val eyePos = player.eyePosition
        val allowedTypes = resolveAllowedMineralTypes()
        val anchor = lastTargetPos
        val reachSq = player.blockInteractionRange().let { it * it }

        val clusterHighPri = NearestTarget()
        val clusterNormal = NearestTarget()
        val bestHighPri = NearestTarget()
        val bestNormal = NearestTarget()

        forEachSphereBlock(eyePos) { pos ->
            val state = level.getBlockState(pos)
            val mineralType = MineralType.fromBlock(state.block) ?: return@forEachSphereBlock
            if (!MineralFilter.isMineralAllowed(mineralType, targetType, ignoreOthers, royalDivan, inDwarvenOnly, allowedTypes = allowedTypes)) return@forEachSphereBlock

            val hitPos = HitPosHelper.pickHitPos(level, pos, state, eyePos) ?: return@forEachSphereBlock
            val distSq = hitPos.distanceToSqr(eyePos)
            if (distSq > reachSq) return@forEachSphereBlock
            val isHighPri = mineralType.isHighPriorityBlock(state.block)
            val inCluster = anchor != null && pos.distSqr(anchor) <= CLUSTER_RADIUS_SQ

            val clusterTarget = if (isHighPri) clusterHighPri else clusterNormal
            val overallTarget = if (isHighPri) bestHighPri else bestNormal
            if (inCluster) clusterTarget.offer(distSq, pos, hitPos, mineralType)
            overallTarget.offer(distSq, pos, hitPos, mineralType)
        }

        return clusterHighPri.target ?: clusterNormal.target ?: bestHighPri.target ?: bestNormal.target
    }

    private fun resolveAllowedMineralTypes(): Set<MineralType> {
        return cachedAllowedTypes ?: computeAllowedMineralTypes().also { cachedAllowedTypes = it }
    }

    private fun computeAllowedMineralTypes(): Set<MineralType> = MineralFilter.buildAllowedTypes(
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
            4 -> mapOf(
                MineralType.YOUNGITE to true,
                MineralType.TIMITE to true,
                MineralType.OBSOLITE to true,
            )
            else -> emptyMap()
        }
    )

    private fun aimAt(hitPos: Vec3) {
        when (aimMode) {
            0 -> RotationUtils.aimVisible(hitPos, aimSpeed / 10f, this)
            1 -> RotationUtils.aimSilent(hitPos, aimSpeed / 10f, 1.2f, this)
        }
    }
}
