package cn.hkim.addon.features.impl

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.config.settings.BooleanSetting
import cn.hkim.addon.config.settings.KeybindSetting
import cn.hkim.addon.config.settings.NumberSetting
import cn.hkim.addon.config.settings.SelectorSetting
import cn.hkim.addon.events.impl.InputEvent
import cn.hkim.addon.events.impl.TickEvent
import cn.hkim.addon.features.Category
import cn.hkim.addon.features.Module
import cn.hkim.addon.features.ModuleInfo
import com.mojang.blaze3d.platform.InputConstants
import meteordevelopment.orbit.EventHandler
import net.minecraft.client.CameraType
import net.minecraft.client.player.ClientInput
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Input
import net.minecraft.world.phys.Vec2
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import kotlin.math.pow
import kotlin.math.sqrt

@ModuleInfo("free_cam", Category.MISC)
object FreeCam : Module("Free Camera", "Detach your camera and fly around freely.") {
    private val acceleration by NumberSetting("Acceleration", "Movement acceleration (blocks/s²)", 50f, 5f, 500f, 5f)
    private val maxSpeed by NumberSetting("Max Speed", "Maximum movement speed (blocks/s)", 50f, 5f, 500f, 5f)
    private val slowdownFactor by NumberSetting("Slowdown", "Velocity multiplier per second when no key is held", 0.01f, 0.0001f, 0.5f, 0.001f)
    private val movementMode by SelectorSetting("Movement Mode", "Default = camera moves on pitch plane; Spectator = horizontal-only", listOf("Default", "Spectator"), "Default")
    private val rememberInputState by BooleanSetting("Remember Input", "Remember movement key state when toggling on/off", true)
    private val showMyName by BooleanSetting("Show My Name", "Show your own name tag while in FreeCam", true)
    private val toggleKey by KeybindSetting("Toggle FreeCam", "Key to toggle FreeCam on/off", GLFW.GLFW_KEY_F6)

    @JvmStatic var isFreecamActive = false
        private set
    @JvmStatic var camX = 0.0
        private set
    @JvmStatic var camY = 0.0
        private set
    @JvmStatic var camZ = 0.0
        private set
    @JvmStatic var camYRot = 0f
        private set
    @JvmStatic var camXRot = 0f
        private set

    private var oldCameraType: CameraType = CameraType.FIRST_PERSON
    private var savedPlayerInput: ClientInput? = null

    private var forwardVel = 0.0
    private var leftVel = 0.0
    private var upVel = 0.0
    private var lastNanoTime = 0L

    private val rotation = Quaternionf()
    private val forward = Vector3f()
    private val up = Vector3f()
    private val left = Vector3f()

    @JvmStatic fun shouldShowPlayerName() = isFreecamActive && showMyName

    @EventHandler
    private fun onInput(event: InputEvent) {
        if (event.key.type == InputConstants.Type.KEYSYM && event.key.value == toggleKey) {
            toggle()
            event.cancel()
        }
    }

    @EventHandler
    private fun onTickStart(event: TickEvent.Start) {
        if (isFreecamActive) {
            savedPlayerInput?.tick()
        }
    }

    override fun onEnable() {
        if (mc.player == null || mc.level == null) {
            enabled = false
            return
        }
        startFreecam()
    }

    override fun onDisable() {
        stopFreecam()
    }

    private fun startFreecam() {
        val player = mc.player ?: return
        val entity = mc.cameraEntity ?: return

        isFreecamActive = true

        savedPlayerInput = player.input
        player.input = createDummyInput(savedPlayerInput!!)

        oldCameraType = mc.options.cameraType
        mc.options.cameraType = CameraType.THIRD_PERSON_BACK

        val pos = entity.position()
        camX = pos.x
        camY = pos.y + entity.eyeHeight.toDouble()
        camZ = pos.z
        camYRot = entity.yRot
        camXRot = entity.xRot

        recalcVectors()

        val dist = -2.0
        camX += forward.x().toDouble() * dist
        camY += forward.y().toDouble() * dist
        camZ += forward.z().toDouble() * dist

        forwardVel = 0.0
        leftVel = 0.0
        upVel = 0.0
        lastNanoTime = System.nanoTime()
    }

    private fun stopFreecam() {
        if (!isFreecamActive) return
        isFreecamActive = false

        mc.options.cameraType = oldCameraType

        val player = mc.player ?: return
        val saved = savedPlayerInput ?: return
        player.input = saved
        savedPlayerInput = null

        forwardVel = 0.0
        leftVel = 0.0
        upVel = 0.0
        lastNanoTime = 0L
    }

    @JvmStatic
    fun onRenderTick() {
        if (!isFreecamActive) return

        if (lastNanoTime == 0L) {
            lastNanoTime = System.nanoTime()
            return
        }

        val now = System.nanoTime()
        val frameSec = (now - lastNanoTime) / 1e9f
        lastNanoTime = now

        val input = savedPlayerInput ?: return
        val kp = input.keyPresses

        val fwd = if (kp.forward()) 1 else if (kp.backward()) -1 else 0
        val lft = if (kp.left()) 1 else if (kp.right()) -1 else 0
        val upI  = if (kp.jump()) 1 else if (kp.shift()) -1 else 0

        val slow = slowdownFactor.toDouble().pow(frameSec.toDouble())

        forwardVel = combine(forwardVel, fwd.toDouble(), frameSec.toDouble(), acceleration.toDouble(), slow)
        leftVel = combine(leftVel, lft.toDouble(), frameSec.toDouble(), acceleration.toDouble(), slow)
        upVel = combine(upVel, upI.toDouble(), frameSec.toDouble(), acceleration.toDouble(), slow)

        var dx = forward.x().toDouble() * forwardVel + left.x().toDouble() * leftVel
        var dy = forward.y().toDouble() * forwardVel + upVel + left.y().toDouble() * leftVel
        var dz = forward.z().toDouble() * forwardVel + left.z().toDouble() * leftVel

        dx *= frameSec
        dy *= frameSec
        dz *= frameSec

        val speed = sqrt(dx * dx + dy * dy + dz * dz) / frameSec
        if (speed > maxSpeed.toDouble()) {
            val factor = maxSpeed.toDouble() / speed
            forwardVel *= factor
            leftVel *= factor
            upVel *= factor
            dx *= factor; dy *= factor; dz *= factor
        }

        camX += dx
        camY += dy
        camZ += dz
    }

    @JvmStatic
    fun onPlayerTurn(yRot: Double, xRot: Double): Boolean {
        if (isFreecamActive) {
            camXRot = Mth.clamp(camXRot + xRot.toFloat() * 0.15f, -90f, 90f)
            camYRot += yRot.toFloat() * 0.15f
            recalcVectors()
            return false
        }
        return true
    }

    private fun recalcVectors() {
        val spectator = movementMode == 1
        rotation.rotationYXZ(
            -camYRot * (Math.PI.toFloat() / 180f),
            (if (spectator) 0f else camXRot) * (Math.PI.toFloat() / 180f),
            0f
        )
        forward.set(0f, 0f, 1f).rotate(rotation)
        up.set(0f, 1f, 0f).rotate(rotation)
        left.set(1f, 0f, 0f).rotate(rotation)
    }

    private fun combine(vel: Double, impulse: Double, dt: Double, accel: Double, slow: Double): Double {
        if (impulse != 0.0) {
            val nv = if (impulse > 0 && vel < 0 || impulse < 0 && vel > 0) 0.0 else vel
            return nv + accel * impulse * dt
        }
        return vel * slow
    }

    private fun createDummyInput(original: ClientInput): ClientInput {
        val keys = if (rememberInputState) {
            val kp = original.keyPresses
            Input(kp.forward(), kp.backward(), kp.left(), kp.right(), kp.jump(), kp.shift(), kp.sprint())
        } else {
            Input(false, false, false, false, false, false, false)
        }
        return object : ClientInput() {
            init {
                this.keyPresses = keys
                this.moveVector = Vec2(
                    calcImpulse(keys.left(), keys.right()),
                    calcImpulse(keys.forward(), keys.backward())
                ).normalized()
            }
        }
    }

    private fun calcImpulse(b1: Boolean, b2: Boolean): Float {
        if (b1 == b2) return 0f
        return if (b1) 1f else -1f
    }
}
