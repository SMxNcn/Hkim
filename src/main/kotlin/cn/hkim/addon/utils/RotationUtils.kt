package cn.hkim.addon.utils

import cn.hkim.addon.Hkim.mc
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object RotationUtils {
    class Rotation(var yaw: Float, var pitch: Float) {

        override fun toString(): String {
            return "Rotation{yaw=$yaw, pitch=$pitch}"
        }

        fun equals(other: Rotation, epsilon: Float = 0.01f): Boolean {
            return abs(this.yaw - other.yaw) < epsilon && abs(this.pitch - other.pitch) < epsilon
        }
    }

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
}