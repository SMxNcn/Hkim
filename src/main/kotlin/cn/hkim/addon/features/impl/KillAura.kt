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
import com.mojang.blaze3d.platform.InputConstants
import meteordevelopment.orbit.EventHandler
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

@ModuleInfo("killaura", Category.MISC, false)
object KillAura : Module("Kill Aura", "Automatically aims at nearby entities.") {
    private val aimMode by SelectorSetting("Aim Mode", "Aim mode.", listOf("Normal", "Silent"), "Silent")
    private val aimSpeed by NumberSetting("Aim Speed", "Aim transition speed.", 0.25f, 0.05f, 1.0f, 0.05f)
    private val attackRate by NumberSetting("Attack Rate", "Attacks per second.", 10.0f, 0.1f, 20.0f, 0.1f)
    private val targetPlayers by BooleanSetting("Target Players", "Target other players.", true)
    private val targetMobs by BooleanSetting("Target Mobs", "Target hostile mobs.", false)

    private val toggleKeybind by KeybindSetting("Toggle Keybind", "Toggle KillAura.", InputConstants.KEY_R)

    private const val SEARCH_RANGE = 3.5

    private var target: LivingEntity? = null
    private var lastAttackTime = 0L
    private var isStopping = false

    @EventHandler
    private fun onKey(event: InputEvent) {
        if (event.key.value == toggleKeybind) toggle()
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

        target = findTarget() ?: run {
            initiateStop()
            return@onExtract
        }

        val targetVec = getTargetVec(target!!)

        when (aimMode) {
            0 -> RotationUtils.aimVisible(targetVec, aimSpeed / 10f, this)
            1 -> RotationUtils.aimSilent(targetVec, aimSpeed / 10f, owner = this)
        }
    }

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        val t = target ?: return
        if (isStopping) return

        val interval = (1000.0 / attackRate.toDouble()).toLong()
        if (System.currentTimeMillis() - lastAttackTime < interval) return

        if (mc.crosshairPickEntity != t) return

        leftClick()
        lastAttackTime = System.currentTimeMillis()
    }

    private fun findTarget(): LivingEntity? {
        val player = mc.player ?: return null
        val level = mc.level ?: return null

        return level.getEntitiesOfClass(
            LivingEntity::class.java,
            player.boundingBox.inflate(SEARCH_RANGE)
        ).filter { entity ->
            if (entity == player || !entity.isAlive) return@filter false
            if (entity is ArmorStand) return@filter false
            if (entity is Player && !targetPlayers) return@filter false
            if (entity !is Player && !targetMobs) return@filter false
            true
        }.minByOrNull { entity ->
            entity.distanceToSqr(player)
        }
    }

    private fun getTargetVec(entity: LivingEntity): Vec3 {
        val eyePos = mc.player!!.eyePosition
        val box = entity.boundingBox

        val targetX = Mth.clamp(eyePos.x, box.minX, box.maxX)
        val targetZ = Mth.clamp(eyePos.z, box.minZ, box.maxZ)

        val minY = box.minY + box.getYsize() * 0.05
        val maxY = box.minY + box.getYsize() * 0.75
        val targetY = when {
            eyePos.y >= maxY -> maxY      // 眼睛在目标上方 → 瞄头顶
            eyePos.y <= minY -> minY      // 眼睛在目标下方 → 瞄脚踝
            else -> eyePos.y               // 眼睛在高度区间内 → 瞄眼睛水平
        }

        return Vec3(targetX, targetY, targetZ)
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
    }
}
