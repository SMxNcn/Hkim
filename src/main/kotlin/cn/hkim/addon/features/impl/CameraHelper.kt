package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.CameraType

@ModuleInfo("camera", Category.RENDER)
object CameraHelper : Module("Camera Helper", "Modify your camera") {
    private val frontCamera by BooleanSetting("Front Camera", "Disables front camera.", true)
    private val cameraDist by NumberSetting("Camera Distance", "Third-person camera distance.", 4.0f, 2.0f, 10.0f, 0.1f)
    private val cameraClip by BooleanSetting("Camera Clip", "Allows the camera to clip through blocks.", false)

    private val smoothTransition by BooleanSetting("Smooth Perspective", "Animate the camera when switching between first and third person.", false)
    private val transitionDuration by NumberSetting("Transition Time (ms)", "Duration of the perspective switch animation.", 250f, 50f, 500f, 50f).depends { smoothTransition }

    @EventHandler
    private fun onTick(event: TickEvent.End) {
        if (!enabled || !frontCamera) return
        if (mc.options.cameraType == CameraType.THIRD_PERSON_FRONT) {
            mc.options.cameraType = CameraType.FIRST_PERSON
        }
    }

    @JvmStatic
    fun canCameraClip() = enabled && cameraClip

    @JvmStatic
    fun getDistance() = cameraDist

    @JvmStatic
    private var suppressTransition = false

    @JvmStatic
    fun suppressNextTransition() {
        suppressTransition = true
    }

    @JvmStatic
    fun isTransitionActive(): Boolean {
        val active = enabled && smoothTransition && !suppressTransition
        suppressTransition = false
        return active
    }

    @JvmStatic
    fun getTransitionDurationMs() = transitionDuration.toLong()
}