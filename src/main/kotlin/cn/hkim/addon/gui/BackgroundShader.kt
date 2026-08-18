package cn.hkim.addon.gui

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import java.nio.ByteBuffer
import java.nio.ByteOrder

object BackgroundShader {
    private const val UBO_SIZE = 48L

    private val buffers = arrayOfNulls<GpuBuffer>(3)
    private var frameIndex = 0

    private val startNanos = System.nanoTime()
    private var timeSeconds = 0f
    private var mouseX = -1f
    private var mouseY = -1f
    private var screenWidth = 1f
    private var screenHeight = 1f
    private var dirty = false

    fun update(screenWidth: Int, screenHeight: Int, mouseX: Double, mouseY: Double) {
        timeSeconds = (System.nanoTime() - startNanos) / 1_000_000_000f
        this.mouseX = mouseX.toFloat()
        this.mouseY = mouseY.toFloat()
        this.screenWidth = screenWidth.toFloat()
        this.screenHeight = screenHeight.toFloat()
        dirty = true
    }

    @JvmStatic
    fun bindUniforms(pass: RenderPass) {
        if (!dirty) return
        dirty = false

        val slot = frameIndex++ % buffers.size
        var buffer = buffers[slot]
        if (buffer == null) {
            buffer = RenderSystem.getDevice().createBuffer(
                { "hkim_bg_uniform" },
                GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_MAP_WRITE,
                UBO_SIZE
            )
            buffers[slot] = buffer
        }

        val bb = ByteBuffer.allocateDirect(UBO_SIZE.toInt()).order(ByteOrder.nativeOrder())
        bb.putFloat(timeSeconds).putFloat(0f).putFloat(0f).putFloat(0f)
        bb.putFloat(mouseX).putFloat(mouseY).putFloat(0f).putFloat(0f)
        bb.putFloat(screenWidth).putFloat(screenHeight).putFloat(0f).putFloat(0f)
        bb.flip()

        buffer.map(0, UBO_SIZE, false, true).use { it.data().put(bb) }
        pass.setUniform("HkimBackground", buffer.slice())
    }
}
