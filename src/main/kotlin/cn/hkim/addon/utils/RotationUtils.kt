package cn.hkim.addon.utils

import cn.hkim.addon.Hkim.mc
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object RotationUtils {
    class Rotation(var yaw: Float, var pitch: Float) {
        override fun toString(): String = "Rotation{yaw=$yaw, pitch=$pitch}"
        fun equals(other: Rotation, epsilon: Float = 0.01f): Boolean =
            abs(this.yaw - other.yaw) < epsilon && abs(this.pitch - other.pitch) < epsilon
    }

    @JvmStatic
    var clientYaw: Float = 0f
        private set

    @JvmStatic
    var clientPitch: Float = 0f
        private set

    @JvmStatic
    var serverYaw: Float = 0f
        private set

    @JvmStatic
    var serverPitch: Float = 0f
        private set

    private var initialized = false

    @JvmStatic
    var isSilentAiming: Boolean = false
        private set

    @JvmStatic
    var isStoppingAiming: Boolean = false
        private set

    fun wrapAngleTo180(angle: Float): Float {
        var result = angle % 360.0f
        if (result >= 180.0f) result -= 360.0f
        if (result < -180.0f) result += 360.0f
        return result
    }

    fun exponentialSmooth(current: Float, target: Float, factor: Float): Float {
        val diff = wrapAngleTo180(target - current)
        val factor = factor.coerceIn(0.01f, 1.0f)
        return current + diff * factor
    }

    fun vec3ToRotation(vec: Vec3): Rotation {
        val player = mc.player ?: return Rotation(0f, 0f)

        val dx = vec.x - player.x
        val dz = vec.z - player.z
        val eyeY = player.y + player.eyeHeight
        val dy = vec.y - eyeY

        val horizontalDist = sqrt(dx * dx + dz * dz)

        if (horizontalDist < 0.001) {
            return Rotation(
                player.yRot,
                if (dy > 0) -90f else 90f
            )
        }

        val yawRad = atan2(dz, dx)
        val pitchRad = atan2(dy, horizontalDist)

        val yaw = (yawRad * 180.0f / Math.PI).toFloat() - 90.0f
        val pitch = (pitchRad * 180.0f / Math.PI).toFloat() * -1.0f

        return Rotation(
            Mth.wrapDegrees(yaw),
            Mth.clamp(Mth.wrapDegrees(pitch), -90f, 90f)
        )
    }

    @JvmStatic
    fun syncClientRotation(yaw: Float, pitch: Float) {
        clientYaw = yaw
        clientPitch = pitch

        if (!initialized) {
            serverYaw = yaw
            serverPitch = pitch
            initialized = true
        }

        if (!isSilentAiming && !isStoppingAiming) {
            serverYaw = yaw
            serverPitch = pitch
        }
    }

    /**
     * @param target 目标位置
     * @param aimSpeed 正常瞄准速度
     * @param startSpeedMultiplier 启动过渡速度倍率（默认 1.5x）。首次进入瞄准或重新瞄准时使用。
     */
    @JvmStatic
    fun aimSilent(target: Vec3, aimSpeed: Float, startSpeedMultiplier: Float = 1.5f) {
        if (!initialized) return
        val targetRot = vec3ToRotation(target)

        val effectiveSpeed = if (isStoppingAiming || !isSilentAiming) {
            isStoppingAiming = false
            (aimSpeed * startSpeedMultiplier).coerceAtMost(1.0f)
        } else {
            aimSpeed
        }

        serverYaw = exponentialSmooth(serverYaw, targetRot.yaw, effectiveSpeed)
        serverPitch = exponentialSmooth(serverPitch, targetRot.pitch, effectiveSpeed)
        isSilentAiming = true

        applyModelRotation(serverYaw)
    }

    /**
     * server 角度以 [stopSpeed] 逐步回归 client 当前视角。
     *
     * @param stopSpeed 停止过渡速度（推荐 0.3 ~ 0.5，快于正常瞄准速度）
     * @return true = 仍在回归中，需继续调用；false = 已完全回归
     */
    @JvmStatic
    fun tickStopAiming(stopSpeed: Float): Boolean {
        val diffYaw = abs(wrapAngleTo180(serverYaw - clientYaw))
        val diffPitch = abs(wrapAngleTo180(serverPitch - clientPitch))

        if (diffYaw < 0.1f && diffPitch < 0.1f) {
            resetServerToClient()
            isStoppingAiming = false
            return false
        }

        serverYaw = exponentialSmooth(serverYaw, clientYaw, stopSpeed)
        serverPitch = exponentialSmooth(serverPitch, clientPitch, stopSpeed)
        applyModelRotation(serverYaw)
        isStoppingAiming = true
        return true
    }

    @JvmStatic
    fun aimVisible(target: Vec3, aimSpeed: Float) {
        val player = mc.player ?: return
        val targetRot = vec3ToRotation(target)
        player.yRot = exponentialSmooth(player.yRot, targetRot.yaw, aimSpeed)
        player.xRot = exponentialSmooth(player.xRot, targetRot.pitch, aimSpeed)
        serverYaw = player.yRot
        serverPitch = player.xRot
        applyModelRotation(player.yRot)
        isSilentAiming = false
    }
    @JvmStatic
    fun applyModelRotation(yaw: Float) {
        val player = mc.player ?: return
        player.yBodyRot = yaw
        player.yBodyRotO = yaw
        player.yHeadRot = yaw
        player.yHeadRotO = yaw
    }

    @JvmStatic
    fun resetServerToClient() {
        serverYaw = clientYaw
        serverPitch = clientPitch
        isSilentAiming = false
        isStoppingAiming = false
        applyModelRotation(clientYaw)
    }

    @JvmStatic
    fun isAligned(targetVec: Vec3, threshold: Float = 0.5f): Boolean {
        val target = vec3ToRotation(targetVec)
        return abs(wrapAngleTo180(serverYaw - target.yaw)) < threshold &&
                abs(wrapAngleTo180(serverPitch - target.pitch)) < threshold
    }

    @JvmStatic
    fun reset() {
        initialized = false
        isSilentAiming = false
        clientYaw = 0f; clientPitch = 0f
        serverYaw = 0f; serverPitch = 0f
    }
}