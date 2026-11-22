package cn.hkim.addon.utils.render.pip

import cn.hkim.addon.Hkim
import cn.hkim.addon.runtime.BackdropBlur
//? if 26.2 {
import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuSampler
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
//? } else {
/*import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import com.mojang.renderpearl.api.commands.CommandEncoder
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuSampler
import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.renderpearl.api.textures.GpuTextureView
*///? }
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.state.gui.BlitRenderState
import net.minecraft.client.renderer.state.gui.GuiRenderState
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import org.joml.Matrix3x2fc
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Optional
import kotlin.math.ceil

data class BackdropDraw(
    val blur: BackdropBlur,
    val x: Float, val y: Float,
    val width: Float, val height: Float,
)

data class BackdropBlurState(
    val captureBounds: ScreenRectangle,
    val panelBounds: ScreenRectangle,
    val displayBounds: ScreenRectangle,
    val blur: BackdropBlur,
    val targetKey: Int,
) : PictureInPictureRenderState {
    override fun x0() = captureBounds.left()
    override fun y0() = captureBounds.top()
    override fun x1() = captureBounds.right()
    override fun y1() = captureBounds.bottom()
    override fun scissorArea() = displayBounds
    override fun bounds() = captureBounds
    override fun scale() = 1f
    override fun pose(): Matrix3x2fc = PictureInPictureRenderState.IDENTITY_POSE
}

class BackdropBlurPIP : PictureInPictureRenderer<BackdropBlurState>() {
    private val targets = mutableMapOf<Int, BlurTarget>()
    private var failed = false

    override fun getRenderStateClass(): Class<BackdropBlurState> = BackdropBlurState::class.java

    override fun getTextureLabel(): String = "hkim_backdrop_blur"

    override fun prepare(
        state: BackdropBlurState,
        guiRenderState: GuiRenderState,
        featureRenderDispatcher: FeatureRenderDispatcher,
        guiScale: Int
    ) {
        if (failed) return
        val minecraft = Minecraft.getInstance()
        if (minecraft.window.isIconified) return

        val main = minecraft.gameRenderer.mainRenderTarget()
        val mainW = main.width
        val mainH = main.height
        if (mainW <= 0 || mainH <= 0) return

        val scale = guiScale.coerceAtLeast(1)
        val capture = state.captureBounds
        val capX0 = (capture.left() * scale).coerceIn(0, mainW)
        val capY0 = (capture.top() * scale).coerceIn(0, mainH)
        val capX1 = (capture.right() * scale).coerceIn(0, mainW)
        val capY1 = (capture.bottom() * scale).coerceIn(0, mainH)
        val capW = capX1 - capX0
        val capH = capY1 - capY0
        if (capW <= 0 || capH <= 0) return

        val requestedDownsample = state.blur.downsample.coerceIn(1, 16)
        val panelMin = minOf(state.panelBounds.width(), state.panelBounds.height()).coerceAtLeast(1)
        val maxBySize = (panelMin / MIN_BLUR_DIM).coerceAtLeast(1)
        val downsample = minOf(requestedDownsample, maxBySize)
        val blurW = ceil(capW.toFloat() / downsample).toInt().coerceAtLeast(1)
        val blurH = ceil(capH.toFloat() / downsample).toInt().coerceAtLeast(1)

        val outScale = ceil(maxOf(capW, capH).toFloat() / MAX_OUT_DIM).toInt().coerceAtLeast(1)
        val outW = ceil(capW.toFloat() / outScale).toInt().coerceAtLeast(1)
        val outH = ceil(capH.toFloat() / outScale).toInt().coerceAtLeast(1)

        try {
            val now = System.nanoTime()
            pruneTargets(now, state.targetKey)
            val target = targetFor(state.targetKey, blurW, blurH, outW, outH)
            target.lastUsedNanos = now

            val encoder = RenderSystem.getDevice().createCommandEncoder()
            writeUniforms(encoder, target, state, scale, mainW, mainH, capX0, capY0, capX1, capY1, blurW, blurH, outW, outH, outScale, downsample)

            val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
            val buffer = target.uniformBuffer ?: return
            val passSize = UNIFORM_PASS_SIZE

            drawPass(encoder, BlurPipelines.DOWNSAMPLE, target.aView ?: return, main.colorTextureView ?: return, sampler,
                buffer.slice(0L, passSize), "hkim_blur_downsample")
            drawPass(encoder, BlurPipelines.BLUR, target.bView ?: return, target.aView ?: return, sampler,
                buffer.slice(UNIFORM_PASS_STRIDE, passSize), "hkim_blur_h")
            drawPass(encoder, BlurPipelines.BLUR, target.aView ?: return, target.bView ?: return, sampler,
                buffer.slice(UNIFORM_PASS_STRIDE * 2L, passSize), "hkim_blur_v")
            drawPass(encoder, BlurPipelines.COMPOSITE, target.outView ?: return, target.aView ?: return, sampler,
                buffer.slice(UNIFORM_PASS_STRIDE * 3L, passSize), "hkim_blur_composite")

            val outView = target.outView ?: return
            guiRenderState.addBlitToCurrentLayer(
                BlitRenderState(
                    RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                    TextureSetup.singleTexture(outView, sampler),
                    PictureInPictureRenderState.IDENTITY_POSE,
                    capture.left(), capture.top(), capture.right(), capture.bottom(),
                    0f, 1f, 1f, 0f,
                    -1,
                    state.scissorArea(),
                    state.bounds()
                )
            )
        } catch (t: Throwable) {
            failed = true
            Hkim.logger.error("Backdrop blur disabled after failure", t)
        }
    }

    override fun renderToTexture(state: BackdropBlurState, poseStack: PoseStack, submitNodeCollector: SubmitNodeCollector) = Unit

    private fun drawPass(
        encoder: CommandEncoder,
        pipeline: RenderPipeline,
        output: GpuTextureView,
        input: GpuTextureView,
        sampler: GpuSampler,
        ubo: GpuBufferSlice,
        label: String
    ) {
        encoder.createRenderPass({ label }, output, Optional.empty()).use { pass ->
            //? if 26.2 {
            pass.setPipeline(pipeline)
            pass.bindTexture("Sampler0", input, sampler)
            //? } else {
            /*pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline))
            pass.setUniform("Sampler0", input, sampler)
            *///? }
            pass.setUniform("BlurConfig", ubo)
            pass.disableScissor()
            pass.draw(3, 1, 0, 0)
        }
    }

    private fun writeUniforms(
        encoder: CommandEncoder,
        target: BlurTarget,
        state: BackdropBlurState,
        scale: Int,
        mainW: Int,
        mainH: Int,
        capX0: Int, capY0: Int, capX1: Int, capY1: Int,
        blurW: Int, blurH: Int,
        outW: Int, outH: Int, outScale: Int,
        downsample: Int
    ) {
        val buffer = target.uniformBuffer ?: return
        val bytes = target.uniformBytes
        bytes.clear()

        val u0 = capX0.toFloat() / mainW
        val u1 = capX1.toFloat() / mainW
        val v0 = 1f - capY1.toFloat() / mainH
        val v1 = 1f - capY0.toFloat() / mainH

        val tap = if (downsample <= 2) 0f else downsample * 0.25f
        putPass(
            bytes,
            mainW.toFloat(), mainH.toFloat(), 1f / mainW, 1f / mainH,
            u0, v0, u1, v1,
            tap / mainW, tap / mainH, 0f, 0f,
            0f, 0f, 0f, 0f
        )
        skipUniformPadding(bytes)

        val radiusPx = (state.blur.radius * scale / downsample).coerceIn(0.5f, 24f)
        putBlurPass(bytes, blurW, blurH, radiusPx, 1f, 0f)
        skipUniformPadding(bytes)
        putBlurPass(bytes, blurW, blurH, radiusPx, 0f, 1f)
        skipUniformPadding(bytes)

        val panel = state.panelBounds
        val panelX0 = panel.left() * scale
        val panelY0 = panel.top() * scale
        val panelX1 = panel.right() * scale
        val panelY1 = panel.bottom() * scale
        val outScaleF = outScale.toFloat()
        val ix0 = (panelX0 - capX0).toFloat() / outScaleF
        val ix1 = (panelX1 - capX0).toFloat() / outScaleF
        val iy0 = (capY1 - panelY1).toFloat() / outScaleF
        val iy1 = (capY1 - panelY0).toFloat() / outScaleF
        val corner = (state.blur.cornerRadius ?: 0f).coerceAtLeast(0f) * scale / outScaleF

        val tint = state.blur.tint
        val tintA = (tint ushr 24 and 0xFF) / 255f
        val tintR = (tint ushr 16 and 0xFF) / 255f * tintA
        val tintG = (tint ushr 8 and 0xFF) / 255f * tintA
        val tintB = (tint and 0xFF) / 255f * tintA

        putPass(
            bytes,
            outW.toFloat(), outH.toFloat(), 1f / outW, 1f / outH,
            ix0, iy0, ix1, iy1,
            corner, 0f, 0f, 0f,
            tintR, tintG, tintB, tintA
        )

        bytes.flip()
        encoder.writeToBuffer(buffer.slice(0, bytes.remaining().toLong()), bytes)
    }

    private fun skipUniformPadding(bytes: ByteBuffer) {
        bytes.position(bytes.position() + (UNIFORM_PASS_STRIDE - UNIFORM_PASS_SIZE).toInt())
    }

    private fun putBlurPass(bytes: ByteBuffer, w: Int, h: Int, radius: Float, dx: Float, dy: Float) {
        putPass(
            bytes,
            w.toFloat(), h.toFloat(), 1f / w, 1f / h,
            0f, 0f, 0f, 0f,
            radius, dx, dy, 0f,
            0f, 0f, 0f, 0f
        )
    }

    private fun putPass(
        bytes: ByteBuffer,
        info0: Float, info1: Float, info2: Float, info3: Float,
        rect0: Float, rect1: Float, rect2: Float, rect3: Float,
        params0: Float, params1: Float, params2: Float, params3: Float,
        tint0: Float, tint1: Float, tint2: Float, tint3: Float
    ) {
        bytes.putFloat(info0).putFloat(info1).putFloat(info2).putFloat(info3)
        bytes.putFloat(rect0).putFloat(rect1).putFloat(rect2).putFloat(rect3)
        bytes.putFloat(params0).putFloat(params1).putFloat(params2).putFloat(params3)
        bytes.putFloat(tint0).putFloat(tint1).putFloat(tint2).putFloat(tint3)
    }

    private fun targetFor(key: Int, width: Int, height: Int, outWidth: Int, outHeight: Int): BlurTarget {
        val existing = targets.getOrPut(key) { BlurTarget() }
        if (existing.width == width && existing.height == height &&
            existing.outWidth == outWidth && existing.outHeight == outHeight &&
            existing.outView != null
        ) return existing

        closeTarget(existing)
        existing.width = width
        existing.height = height
        existing.outWidth = outWidth
        existing.outHeight = outHeight
        val device = RenderSystem.getDevice()
        existing.a = device.createTexture(getTextureLabel() + "_a", TEXTURE_USAGE, GpuFormat.RGBA8_UNORM, width, height, 1, 1)
        existing.aView = device.createTextureView(existing.a!!)
        existing.b = device.createTexture(getTextureLabel() + "_b", TEXTURE_USAGE, GpuFormat.RGBA8_UNORM, width, height, 1, 1)
        existing.bView = device.createTextureView(existing.b!!)
        existing.out = device.createTexture(getTextureLabel() + "_out", TEXTURE_USAGE, GpuFormat.RGBA8_UNORM, outWidth, outHeight, 1, 1)
        existing.outView = device.createTextureView(existing.out!!)
        existing.uniformBuffer = device.createBuffer({ "hkim_blur_ubo" }, GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, UNIFORM_SIZE)
        return existing
    }

    private fun pruneTargets(now: Long, keepKey: Int) {
        val cutoff = now - TARGET_IDLE_NANOS
        val iterator = targets.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key != keepKey && entry.value.lastUsedNanos < cutoff) {
                closeTarget(entry.value)
                iterator.remove()
            }
        }
    }

    override fun close() {
        targets.values.forEach(::closeTarget)
        targets.clear()
        super.close()
    }

    private fun closeTarget(target: BlurTarget) {
        target.aView?.close(); target.aView = null
        target.a?.close(); target.a = null
        target.bView?.close(); target.bView = null
        target.b?.close(); target.b = null
        target.outView?.close(); target.outView = null
        target.out?.close(); target.out = null
        target.uniformBuffer?.close(); target.uniformBuffer = null
        target.width = 0
        target.height = 0
        target.outWidth = 0
        target.outHeight = 0
    }

    private class BlurTarget {
        var width = 0
        var height = 0
        var outWidth = 0
        var outHeight = 0
        var a: GpuTexture? = null
        var aView: GpuTextureView? = null
        var b: GpuTexture? = null
        var bView: GpuTextureView? = null
        var out: GpuTexture? = null
        var outView: GpuTextureView? = null
        var uniformBuffer: GpuBuffer? = null
        val uniformBytes: ByteBuffer = ByteBuffer.allocateDirect(UNIFORM_SIZE.toInt()).order(ByteOrder.nativeOrder())
        var lastUsedNanos = 0L
    }

    companion object {
        private val TEXTURE_USAGE = GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_TEXTURE_BINDING
        private const val MIN_BLUR_DIM = 32
        private const val MAX_OUT_DIM = 2048f
        private const val UNIFORM_PASS_SIZE = 64L
        private const val UNIFORM_PASS_STRIDE = 256L
        private const val UNIFORM_SIZE = UNIFORM_PASS_STRIDE * 4L
        private const val TARGET_IDLE_NANOS = 1_000_000_000L
    }
}
