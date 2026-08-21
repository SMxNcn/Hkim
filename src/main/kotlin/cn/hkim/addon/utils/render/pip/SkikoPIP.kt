package cn.hkim.addon.utils.render.pip

import cn.hkim.addon.utils.render.skiko.Skiko
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.BlitRenderState
import net.minecraft.client.renderer.state.gui.GuiRenderState
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import org.jetbrains.skia.*
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

class SkikoPIP(vertexConsumers: MultiBufferSource.BufferSource) : PictureInPictureRenderer<SkikoPIP.SkikoRenderState>(vertexConsumers) {
    private val rasterTargets = mutableMapOf<Int, RasterTarget>()

    override fun getTranslateY(height: Int, guiScale: Int) = height / 2f

    override fun getRenderStateClass(): Class<SkikoRenderState> = SkikoRenderState::class.java

    override fun getTextureLabel(): String = "skiko"

    override fun prepare(
        state: SkikoRenderState,
        guiRenderState: GuiRenderState,
        guiScale: Int
    ) {
        val window = Minecraft.getInstance().window
        if (window.isIconified) return

        val textureScale = textureScale(state, guiScale)
        val width = ceil(state.width * textureScale).toInt().coerceAtLeast(1)
        val height = ceil(state.height * textureScale).toInt().coerceAtLeast(1)
        val now = System.nanoTime()

        val target = rasterTargets.getOrPut(state.targetKey) { RasterTarget() }
        target.lastUsedNanos = now
        pruneRasterTargets(now, state.targetKey)
        val view = textureFor(target, width, height)
        val currentTexture = target.texture ?: return
        val canReuseRasterFrame = target.lastRasterWidth == width
            && target.lastRasterHeight == height
            && state.cacheReusable
            && state.cacheToken != null
            && state.cacheToken == target.lastRasterCacheToken

        if (!canReuseRasterFrame) {
            renderSkikoToImage(target, state, width, height, textureScale)
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(currentTexture, target.image ?: return)
            target.lastRasterWidth = width
            target.lastRasterHeight = height
            target.lastRasterCacheToken = state.cacheToken
            target.lastRasterUploadNanos = now
        }

        guiRenderState.addBlitToCurrentLayer(
            BlitRenderState(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                TextureSetup.singleTexture(
                    view,
                    RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR)
                ),
                state.pose(),
                state.x0(),
                state.y0(),
                state.x1(),
                state.y1(),
                0f,
                1f,
                1f,
                0f,
                -1,
                state.scissorArea(),
                state.bounds()
            )
        )
    }

    override fun renderToTexture(state: SkikoRenderState, poseStack: PoseStack) = Unit

    private fun renderSkikoToImage(target: RasterTarget, state: SkikoRenderState, width: Int, height: Int, dpr: Float) {
        val skikoSurface = surfaceFor(target, width, height)
        skikoSurface.canvas.clear(0)

        val canvas = skikoSurface.canvas
        val flipSaveCount = canvas.save()
        try {
            canvas.translate(0f, height.toFloat())
            canvas.scale(1f, -1f)

            Skiko.beginFrame(canvas, width.toFloat(), height.toFloat(), dpr)
            Skiko.push()
            Skiko.translate(-state.displayBounds.left() + state.pad, -state.displayBounds.top() + state.pad)
            Skiko.transform(state.poseMatrix)
            state.callback.run()
            Skiko.pop()
            Skiko.endFrame()
        } finally {
            canvas.restoreToCount(flipSaveCount)
        }
    }

    private fun textureFor(target: RasterTarget, width: Int, height: Int): GpuTextureView {
        val existingTexture = target.texture
        val existingView = target.textureView
        if (existingTexture != null && existingView != null && existingTexture.getWidth(0) == width && existingTexture.getHeight(0) == height) {
            return existingView
        }

        closeGpuTarget(target)

        val createdTexture = RenderSystem.getDevice().createTexture(
            getTextureLabel(),
            GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING or GpuTexture.USAGE_RENDER_ATTACHMENT,
            TextureFormat.RGBA8,
            width,
            height,
            1,
            1
        )
        val createdView = RenderSystem.getDevice().createTextureView(createdTexture)

        target.texture = createdTexture
        target.textureView = createdView
        return createdView
    }

    private fun surfaceFor(target: RasterTarget, width: Int, height: Int): Surface {
        val existingImage = target.image
        val existingSurface = target.surface
        if (existingImage != null && existingSurface != null && existingImage.width == width && existingImage.height == height) {
            return existingSurface
        }

        closeRasterTarget(target)

        val createdImage = NativeImage(width, height, false)
        val imageInfo = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL, ColorSpace.sRGB)
        val createdSurface = Surface.makeRasterDirect(imageInfo, createdImage.pointer, width * imageInfo.bytesPerPixel)

        target.image = createdImage
        target.surface = createdSurface
        return createdSurface
    }

    private fun pruneRasterTargets(now: Long, keepKey: Int) {
        val idleCutoff = now - RASTER_TARGET_IDLE_NANOS
        val iterator = rasterTargets.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key != keepKey && entry.value.lastUsedNanos < idleCutoff) {
                closeGpuTarget(entry.value)
                closeRasterTarget(entry.value)
                iterator.remove()
            }
        }

        while (rasterTargets.size > RASTER_MAX_TARGETS) {
            val oldest = rasterTargets
                .filterKeys { it != keepKey }
                .minByOrNull { it.value.lastUsedNanos }
                ?: break
            closeGpuTarget(oldest.value)
            closeRasterTarget(oldest.value)
            rasterTargets.remove(oldest.key)
        }
    }

    override fun close() {
        rasterTargets.values.forEach { target ->
            closeGpuTarget(target)
            closeRasterTarget(target)
        }
        rasterTargets.clear()
        super.close()
    }

    private fun closeGpuTarget(target: RasterTarget) {
        target.textureView?.close()
        target.textureView = null
        target.texture?.close()
        target.texture = null
        target.lastRasterWidth = 0
        target.lastRasterHeight = 0
        target.lastRasterCacheToken = null
        target.lastRasterUploadNanos = 0L
    }

    private fun closeRasterTarget(target: RasterTarget) {
        target.surface?.close()
        target.surface = null
        target.image?.close()
        target.image = null
    }

    private class RasterTarget {
        var texture: GpuTexture? = null
        var textureView: GpuTextureView? = null
        var image: NativeImage? = null
        var surface: Surface? = null
        var lastRasterUploadNanos = 0L
        var lastRasterWidth = 0
        var lastRasterHeight = 0
        var lastRasterCacheToken: Int? = null
        var lastUsedNanos = 0L
    }

    data class SkikoRenderState(
        val width: Int,
        val height: Int,
        val poseMatrix: Matrix3x2f,
        val renderBounds: ScreenRectangle,
        val displayBounds: ScreenRectangle,
        val callback: Runnable,
        val cacheToken: Int?,
        val targetKey: Int,
        val cacheReusable: Boolean,
        val pad: Int = 0
    ) : PictureInPictureRenderState {
        override fun x0() = renderBounds.left()
        override fun y0() = renderBounds.top()
        override fun x1() = renderBounds.right()
        override fun y1() = renderBounds.bottom()
        override fun scissorArea() = displayBounds
        override fun bounds() = renderBounds
        override fun scale() = 1f
        override fun pose(): Matrix3x2f = Matrix3x2f(PictureInPictureRenderState.IDENTITY_POSE)
    }

    companion object {
        private const val PAD = 2
        private const val RASTER_MAX_PIXELS = 4_000_000f
        private const val RASTER_MAX_TARGETS = 12
        private const val RASTER_TARGET_IDLE_NANOS = 1_000_000_000L
        private const val RASTER_ANIMATED_SCALE = 2.25f
        private val drawSlotCounters = WeakHashMap<GuiGraphicsExtractor, DrawSlotCounter>()

        @JvmStatic
        fun drawSkikoTo(graphics: GuiGraphicsExtractor, x: Number, y: Number, width: Number, height: Number, callback: Runnable) {
            graphics.drawSkiko(x, y, width, height, callback)
        }

        @JvmStatic
        fun GuiGraphicsExtractor.drawSkiko(callback: Runnable) {
            drawSkiko(0f, 0f, guiWidth().toFloat(), guiHeight().toFloat(), callback)
        }

        @JvmStatic
        fun GuiGraphicsExtractor.drawSkiko(x: Number, y: Number, width: Number, height: Number, callback: Runnable) {
            drawSkiko(x, y, width, height, null, callback)
        }

        @JvmStatic
        fun GuiGraphicsExtractor.drawSkiko(
            x: Number,
            y: Number,
            width: Number,
            height: Number,
            cacheToken: Int?,
            callback: Runnable
        ) {
            drawSkiko(x, y, width, height, null, cacheToken, callback)
        }

        @JvmStatic
        fun GuiGraphicsExtractor.drawSkikoCached(
            cacheKey: Any,
            x: Number,
            y: Number,
            width: Number,
            height: Number,
            cacheToken: Int?,
            callback: Runnable
        ) {
            drawSkiko(x, y, width, height, cacheKey, cacheToken, callback)
        }

        private fun GuiGraphicsExtractor.drawSkiko(
            x: Number,
            y: Number,
            width: Number,
            height: Number,
            cacheKey: Any?,
            cacheToken: Int?,
            callback: Runnable
        ) {
            val window = Minecraft.getInstance().window
            if (window.isIconified || window.guiScaledWidth <= 0 || window.guiScaledHeight <= 0) return

            val pose = Matrix3x2f(pose())
            val scissor = scissorStack.peek()

            val left = floor(x.toFloat()).toInt()
            val top = floor(y.toFloat()).toInt()
            val right = ceil(x.toFloat() + width.toFloat()).toInt()
            val bottom = ceil(y.toFloat() + height.toFloat()).toInt()
            val renderRect = ScreenRectangle(left, top, right - left, bottom - top).transformMaxBounds(pose)
            if (renderRect.width() <= 0 || renderRect.height() <= 0) return

            val bounds = if (scissor != null) {
                scissor.intersection(renderRect) ?: return
            }
            else {
                renderRect
            }
            if (bounds.width() <= 0 || bounds.height() <= 0) return

            val screenRect = ScreenRectangle(0, 0, window.guiScaledWidth, window.guiScaledHeight)
            val clipped = screenRect.intersection(bounds) ?: return
            if (clipped.width() <= 0 || clipped.height() <= 0) return

            val renderBounds = ScreenRectangle(
                clipped.left() - PAD,
                clipped.top() - PAD,
                clipped.width() + PAD * 2,
                clipped.height() + PAD * 2
            )

            val state = SkikoRenderState(
                renderBounds.width(),
                renderBounds.height(),
                pose,
                renderBounds,
                clipped,
                callback,
                cacheToken,
                targetKey(clipped, cacheKey, nextDrawSlotKey()),
                cacheKey != null,
                PAD
            )
            this.guiRenderState.addPicturesInPictureState(state)
        }

        private fun targetKey(bounds: ScreenRectangle, cacheKey: Any?, drawSlotKey: Int): Int {
            var result = cacheKey?.hashCode() ?: drawSlotKey
            result = 31 * result + bounds.top()
            result = 31 * result + bounds.left()
            result = 31 * result + bounds.width()
            result = 31 * result + bounds.height()
            return result
        }

        private fun GuiGraphicsExtractor.nextDrawSlotKey(): Int {
            val counter = drawSlotCounters.getOrPut(this) { DrawSlotCounter() }
            return counter.nextSlot++
        }

        private fun textureScale(state: SkikoRenderState, guiScale: Int): Float {
            val nativeScale = guiScale.toFloat().coerceAtLeast(1f)
            val preferredScale = if (state.cacheToken != null && state.cacheToken != 0) {
                nativeScale.coerceAtMost(RASTER_ANIMATED_SCALE)
            }
            else {
                nativeScale
            }
            val nativePixels = state.width.toFloat() * state.height.toFloat() * preferredScale * preferredScale
            if (!nativePixels.isFinite() || nativePixels <= RASTER_MAX_PIXELS) return preferredScale

            return (preferredScale * sqrt(RASTER_MAX_PIXELS / nativePixels)).coerceIn(0.5f, preferredScale)
        }

        private class DrawSlotCounter {
            var nextSlot = 0
        }
    }
}