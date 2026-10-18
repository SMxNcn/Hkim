package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.DropdownSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import cn.hkim.addon.utils.toRadians
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import org.joml.Quaternionf

@ModuleInfo("entity_model", Category.MISC)
object EntityModel : Module("Entity Model", "Bounces and spins the models of entities.") {
    private val targets by DropdownSetting("Targets", "Which entities receive the transforms.")
    private val self by BooleanSetting("Self", "Apply to your own model.", true).depends { targets }
    private val players by BooleanSetting("Players", "Apply to other players.", false).depends { targets }
    private val mobs by BooleanSetting("Mobs", "Apply to all other visible mobs.", false).depends { targets }

    private val bounce by BooleanSetting("Bounce", "Squash the model against the feet.", true)
    private val spin by BooleanSetting("Spin", "Rotate the model around the world Y axis.", false)

    private val bounceDepth by NumberSetting("Bounce Depth", "How far the height travels, anchored at the feet.", 0.4f, 0.05f, 0.9f, 0.05f).depends { bounce }
    private val bounceWidth by NumberSetting("Bounce Width", "Extra width at maximum squash; depth stays unchanged.", 0.5f, 0f, 2f, 0.05f).depends { bounce }
    private val bounceSpeed by NumberSetting("Bounce Speed", "Bounce loops per second; each loop squashes twice.", 1.1f, 0.2f, 5f, 0.1f).depends { bounce }
    private val spinSpeed by NumberSetting("Spin Speed", "", 390f, -480f, 480f, 10f, "°/s").depends { spin }

    private var scaleX = 1f
    private var scaleY = 1f
    private var scaleZ = 1f
    private var yawDegrees = 0f
    private var offsetX = 0f
    private var offsetY = 0f
    private var offsetZ = 0f

    @JvmStatic
    @JvmOverloads
    fun setScale(x: Float, y: Float = x, z: Float = x) {
        scaleX = x
        scaleY = y
        scaleZ = z
    }

    @JvmStatic
    fun setYaw(degrees: Float) {
        yawDegrees = degrees
    }

    @JvmStatic
    fun setOffset(x: Float, y: Float, z: Float) {
        offsetX = x
        offsetY = y
        offsetZ = z
    }

    fun resetTransform() {
        setScale(1f)
        setYaw(0f)
        setOffset(0f, 0f, 0f)
    }

    private fun shouldTransform(state: LivingEntityRenderState): Boolean {
        if (!enabled || state.isInvisible || state.isInvisibleToPlayer || state.deathTime > 0f) return false
        val avatar = state as? AvatarRenderState
        if (avatar != null && avatar.id == mc.player?.id) return self

        return (players && avatar != null) || (mobs && avatar == null)
    }

    @JvmStatic
    fun applyWorldTransform(state: LivingEntityRenderState, poseStack: PoseStack) {
        if (!shouldTransform(state)) return

        var yaw = yawDegrees
        val x = offsetX
        val y = offsetY
        val z = offsetZ

        if (spin) yaw += spinAngle()

        if (yaw != 0f) poseStack.rotate(Quaternionf().rotateY(toRadians(yaw)))
        if (x != 0f || y != 0f || z != 0f) poseStack.translate(x, y, z)
    }

    @JvmStatic
    fun applyModelScale(state: LivingEntityRenderState, poseStack: PoseStack) {
        if (!shouldTransform(state)) return

        val amount = if (bounce) bounceAmount() else 0f
        val y = scaleY * (1f - bounceDepth * amount)
        val spread = 1f + bounceWidth * amount

        val x = scaleX * spread
        val z = scaleZ * spread
        if (x == 1f && y == 1f && z == 1f) return
        poseStack.scale(x, y, z)
    }

    private fun spinAngle(): Float = (System.currentTimeMillis() / 1000.0 * spinSpeed % 360.0).toFloat()

    private fun phase(speed: Float): Float {
        val periodMs = (1000f / speed).coerceAtLeast(1f)
        return (System.currentTimeMillis() % periodMs.toLong()).toFloat() / periodMs
    }

    private const val BOUNCE_LENGTH = 0.91667f
    private val BOUNCE_TIMES = floatArrayOf(0f, 0.25f, 0.45833f, 0.70833f, BOUNCE_LENGTH)
    private val BOUNCE_VALUES = floatArrayOf(0f, 1f, 0f, 1f, 0f)

    private fun bounceAmount(): Float {
        val time = phase(bounceSpeed) * BOUNCE_LENGTH
        var i = 0
        while (i < BOUNCE_TIMES.size - 2 && time > BOUNCE_TIMES[i + 1]) i++
        val t = (time - BOUNCE_TIMES[i]) / (BOUNCE_TIMES[i + 1] - BOUNCE_TIMES[i])
        val p1 = BOUNCE_VALUES[i]
        val p2 = BOUNCE_VALUES[i + 1]

        return p1 + (p2 - p1) * t * t * (3f - 2f * t)
    }

    override fun onDisable() = resetTransform()
}
