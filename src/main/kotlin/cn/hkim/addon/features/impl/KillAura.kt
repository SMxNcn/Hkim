package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.KeybindSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.config.settings.SelectorSetting
import cn.hkim.addon.events.impl.InputEvent
import cn.hkim.addon.events.impl.RenderEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.RotationUtils
import cn.hkim.addon.utils.leftClick
import meteordevelopment.orbit.EventHandler
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import cn.hkim.addon.compat.input.InputCompat

@ModuleInfo("killaura", Category.MISC, false)
object KillAura : Module("Kill Aura", "Automatically attack nearby entities.") {
    private val aimMode by SelectorSetting("Aim Mode", "Aim mode.", listOf("Normal", "Silent"), "Silent")
    private val aimSpeed by NumberSetting("Aim Speed", "Aim transition speed.", 0.25f, 0.05f, 1.0f, 0.05f)
    private val attackRate by NumberSetting("Attack Rate", "Attacks per second.", 10.0f, 0.1f, 20.0f, 0.1f)
    private val targetPlayers by BooleanSetting("Target Players", "Target other players.", true)
    private val targetMobs by BooleanSetting("Target Mobs", "Target hostile mobs.", false)
    private val toggleKeybind by KeybindSetting("Toggle Keybind", "Toggle KillAura.", InputCompat.KEY_R)

    private const val SEARCH_RANGE = 3.5

    private val SIDE_FRACTIONS = floatArrayOf(0.05f, 0.25f, 0.5f, 0.75f, 0.95f)
    private val LAYER_FRACTIONS = floatArrayOf(0.05f, 0.2f, 0.35f, 0.5f, 0.65f, 0.8f, 0.95f)
    private const val EDGE_MIN = 0.02
    private const val EDGE_MAX = 0.98
    private const val INSET = 0.03
    private const val AIM_UPDATE_INTERVAL_MS = 50L

    private var target: LivingEntity? = null
    private var nextAttackTime = 0.0
    private var isStopping = false

    private var cachedAimEntity: LivingEntity? = null
    private var cachedAimFractions: Vec3? = null
    private var nextAimUpdate = 0L

    @EventHandler
    private fun onKey(event: InputEvent) {
        if (event.matches(toggleKeybind)) toggle()
    }

    @EventHandler
    private fun onExtract(event: RenderEvent.Extract) {
        if (!enabled) {
            if (RotationUtils.isSilentAiming || RotationUtils.isStoppingAiming) {
                if (!RotationUtils.tickStopAiming(aimSpeed / 2f, this)) {
                    target = null
                    isStopping = false
                }
            } else {
                target = null
                isStopping = false
            }
            return
        }

        if (isStopping) {
            if (RotationUtils.tickStopAiming(aimSpeed / 2f, this)) return
            isStopping = false
            target = null
            return
        }

        val aim = resolveAim()
        if (aim == null) {
            initiateStop()
            return
        }

        val (entity, targetVec) = aim
        target = entity

        when (aimMode) {
            0 -> RotationUtils.aimVisible(targetVec, aimSpeed / 10f, this)
            1 -> RotationUtils.aimSilent(targetVec, aimSpeed / 10f, owner = this)
        }
    }

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        val t = target
        if (t == null || isStopping) {
            nextAttackTime = 0.0
            return
        }

        nextAttackTime = attackOnSchedule(t, nextAttackTime, System.currentTimeMillis().toDouble())
    }

    private fun attackOnSchedule(t: LivingEntity, nextAttack: Double, nowMillis: Double): Double {
        var next = if (nextAttack == 0.0) nowMillis else nextAttack
        if (nowMillis < next) return next

        if (mc.crosshairPickEntity != t) return next

        leftClick()
        next += 1000.0 / attackRate.toDouble()

        if (next <= nowMillis) next = nowMillis + 1000.0 / attackRate.toDouble()

        return next
    }

    private fun resolveAim(): Pair<LivingEntity, Vec3>? {
        val now = System.currentTimeMillis()
        if (now < nextAimUpdate) {
            val cached = cachedAimEntity
            if (cached != null && cached.isAlive) {
                val point = aimPointOf(cached)
                if (point != null) return cached to point
            }
            return null
        }
        nextAimUpdate = now + AIM_UPDATE_INTERVAL_MS

        val current = target
        if (current != null && current.isAlive) {
            findAimPoint(current)?.let { point ->
                cacheAim(current, point)
                return current to point
            }
        }

        val found = findTarget()
        if (found != null) {
            cacheAim(found.first, found.second)
            return found
        }

        cachedAimEntity = null
        cachedAimFractions = null
        return null
    }

    private fun cacheAim(entity: LivingEntity, point: Vec3) {
        val box = entity.boundingBox
        cachedAimEntity = entity
        cachedAimFractions = Vec3(
            ((point.x - box.minX) / box.xsize).coerceIn(0.0, 1.0),
            ((point.y - box.minY) / box.ysize).coerceIn(0.0, 1.0),
            ((point.z - box.minZ) / box.zsize).coerceIn(0.0, 1.0)
        )
    }

    private fun aimPointOf(entity: LivingEntity): Vec3? {
        val fractions = cachedAimFractions ?: return null
        val box = entity.boundingBox
        return Vec3(
            box.minX + box.xsize * fractions.x,
            box.minY + box.ysize * fractions.y,
            box.minZ + box.zsize * fractions.z
        )
    }

    private fun findTarget(): Pair<LivingEntity, Vec3>? {
        val player = mc.player ?: return null
        val level = mc.level ?: return null

        val targets = level.getEntitiesOfClass(
            LivingEntity::class.java,
            player.boundingBox.inflate(SEARCH_RANGE)
        ).filter { isEligibleTarget(it, player) }
            .sortedBy { it.distanceToSqr(player) }

        for (entity in targets) {
            val aimPoint = findAimPoint(entity) ?: continue
            return entity to aimPoint
        }
        return null
    }

    private fun isEligibleTarget(entity: LivingEntity, player: Player): Boolean {
        if (entity === player || !entity.isAlive) return false
        if (entity is ArmorStand) return false
        if (entity is Player && !targetPlayers) return false
        if (entity !is Player && !targetMobs) return false
        return true
    }

    private fun findAimPoint(entity: LivingEntity): Vec3? {
        val player = mc.player ?: return null
        val level = mc.level ?: return null
        val eye = player.eyePosition
        val box = entity.boundingBox

        val preferred = preferredAimPoint(box, eye)
        if (canHitTarget(level, player, entity, preferred)) return preferred

        val closest = Vec3(
            clampInset(eye.x, box.minX, box.maxX),
            clampInset(eye.y, box.minY, box.maxY),
            clampInset(eye.z, box.minZ, box.maxZ)
        )
        if (closest != preferred && canHitTarget(level, player, entity, closest)) return closest

        for (point in sampleAimPoints(box, preferred)) {
            if (canHitTarget(level, player, entity, point)) return point
        }
        return null
    }

    private fun preferredAimPoint(box: AABB, eye: Vec3): Vec3 {
        val targetX = clampInset(eye.x, box.minX, box.maxX)
        val targetZ = clampInset(eye.z, box.minZ, box.maxZ)

        val minY = box.minY + box.ysize * 0.05
        val maxY = box.minY + box.ysize * 0.75
        val targetY = when {
            eye.y >= maxY -> maxY
            eye.y <= minY -> minY
            else -> eye.y
        }

        return Vec3(targetX, targetY, targetZ)
    }

    private fun sampleAimPoints(box: AABB, preferred: Vec3): List<Vec3> {
        fun point(x: Double, y: Double, z: Double) = Vec3(
            box.minX + box.xsize * x,
            box.minY + box.ysize * y,
            box.minZ + box.zsize * z
        )

        val points = ArrayList<Vec3>(
            LAYER_FRACTIONS.size * SIDE_FRACTIONS.size * 4 +
                    SIDE_FRACTIONS.size * SIDE_FRACTIONS.size * 2
        )

        for (y in LAYER_FRACTIONS) {
            for (a in SIDE_FRACTIONS) {
                val ty = y.toDouble()
                val ta = a.toDouble()
                points += point(EDGE_MIN, ty, ta)
                points += point(EDGE_MAX, ty, ta)
                points += point(ta, ty, EDGE_MIN)
                points += point(ta, ty, EDGE_MAX)
            }
        }

        for (y in doubleArrayOf(EDGE_MIN, EDGE_MAX)) {
            for (x in SIDE_FRACTIONS) {
                for (z in SIDE_FRACTIONS) {
                    points += point(x.toDouble(), y, z.toDouble())
                }
            }
        }

        points.sortBy { it.distanceToSqr(preferred) }
        return points
    }

    private fun canHitTarget(level: Level, player: Player, target: LivingEntity, point: Vec3): Boolean {
        val eye = player.eyePosition
        val offset = point.subtract(eye)
        val lengthSqr = offset.lengthSqr()
        if (lengthSqr < 1.0E-6) return false
        val look = offset.scale(1.0 / kotlin.math.sqrt(lengthSqr))

        val entityRange = player.entityInteractionRange()
        val maxRange = maxOf(player.blockInteractionRange(), entityRange)
        val rayEnd = eye.add(look.scale(maxRange))

        val blockHit = level.clip(
            ClipContext(eye, rayEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)
        )
        val rayLength = if (blockHit.type == HitResult.Type.MISS) maxRange
        else blockHit.location.distanceTo(eye)
        if (rayLength <= 1.0E-4) return false

        val entityEnd = eye.add(look.scale(rayLength))
        val searchBox = player.boundingBox.expandTowards(look.scale(rayLength)).inflate(1.0)
        val entityHit = ProjectileUtil.getEntityHitResult(
            player, eye, entityEnd, searchBox, EntitySelector.CAN_BE_PICKED, rayLength * rayLength
        ) ?: return false

        return entityHit.entity === target &&
                eye.distanceToSqr(entityHit.location) < entityRange * entityRange
    }

    private fun clampInset(value: Double, min: Double, max: Double): Double {
        val lower = min + INSET
        val upper = max - INSET
        return if (lower > upper) (min + max) / 2.0 else value.coerceIn(lower, upper)
    }

    private fun initiateStop() {
        if (!isStopping && (RotationUtils.isSilentAiming || RotationUtils.isStoppingAiming)) {
            isStopping = true
        }
        if (!isStopping) {
            target = null
        }
    }

    override fun onDisable() {
        isStopping = false
        target = null
        nextAttackTime = 0.0
        cachedAimEntity = null
        cachedAimFractions = null
        nextAimUpdate = 0L
    }
}
