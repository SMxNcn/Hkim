package cn.hkim.addon.utils.render.pip

import cn.hkim.addon.Hkim
import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.utils.render.CustomRenderPipelines
import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.IndexType
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.gui.BlitRenderState
import net.minecraft.client.renderer.state.gui.GuiRenderState
import org.joml.Matrix4f
import org.joml.Vector4f
import java.nio.ByteOrder
import java.util.*

class PIPRenderer : PictureInPictureRenderer<PIPState>() {

    companion object {
        private const val FRAME_THRESHOLD_NANOS = 5_000_000L
    }

    override fun getRenderStateClass(): Class<PIPState> = PIPState::class.java

    override fun getTextureLabel(): String = "Gui2dPip"

    override fun textureIsReadyToBlit(state: PIPState): Boolean = false

    private data class PerStateTex(
        val color: GpuTexture,
        val view: GpuTextureView,
        val depth: GpuTexture,
        val depthView: GpuTextureView,
        val w: Int,
        val h: Int
    ) {
        fun close() {
            try { view.close() } catch (_: Exception) {}
            try { color.close() } catch (_: Exception) {}
            try { depthView.close() } catch (_: Exception) {}
            try { depth.close() } catch (_: Exception) {}
        }
    }

    private var currentStateTex: PerStateTex? = null

    private val texPool = mutableMapOf<Long, MutableList<PerStateTex>>()
    private val usedTexThisFrame = mutableListOf<PerStateTex>()

    private fun poolKey(w: Int, h: Int): Long = (w.toLong() shl 32) or h.toLong()

    private fun obtainStateTex(device: GpuDevice, w: Int, h: Int): PerStateTex {
        val key = poolKey(w, h)
        val list = texPool.getOrPut(key) { mutableListOf() }
        val existing = list.firstOrNull { it !in usedTexThisFrame }
        if (existing != null) {
            usedTexThisFrame.add(existing)
            return existing
        }
        val color = device.createTexture({ "hkim_pip_state" }, 13, GpuFormat.RGBA8_UNORM, w, h, 1, 1)
        val view = device.createTextureView(color)
        val depth = device.createTexture({ "hkim_pip_depth" }, 9, GpuFormat.D32_FLOAT, w, h, 1, 1)
        val depthView = device.createTextureView(depth)
        val tex = PerStateTex(color, view, depth, depthView, w, h)
        list.add(tex)
        usedTexThisFrame.add(tex)
        return tex
    }

    private var lastCallNanoTime = 0L
    private var frameResetDone = false

    private fun handleFrameBoundary() {
        val now = System.nanoTime()
        val gap = now - lastCallNanoTime
        lastCallNanoTime = now

        if (gap > FRAME_THRESHOLD_NANOS) frameResetDone = false

        if (!frameResetDone) {
            frameResetDone = true
            dynPoolIndex = 0
            projPoolIndex = 0
            vtxPoolIndex = 0
            usedTexThisFrame.clear()
        }
    }

    override fun renderToTexture(
        state: PIPState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector
    ) {
        if (state.commands.isEmpty()) return

        handleFrameBoundary()

        val device = RenderSystem.getDevice()
        val guiScale = mc.window.guiScale

        val bw = (state.x1() - state.x0()).coerceAtLeast(1)
        val bh = (state.y1() - state.y0()).coerceAtLeast(1)
        val texW = (bw * guiScale).coerceAtLeast(1)
        val texH = (bh * guiScale).coerceAtLeast(1)
        if (texW <= 0 || texH <= 0) return

        ensureStaticBuffers(device)

        val stateTex = obtainStateTex(device, texW, texH)
        currentStateTex = stateTex

        val encoder = device.createCommandEncoder()
        encoder.clearColorAndDepthTextures(stateTex.color, Vector4f(0f, 0f, 0f, 0f), stateTex.depth, 1.0)

        val projBuf = obtainProj(device).also { writeProjForBounds(it, bw, bh) }
        val mv = state.modelViewMatrix

        val draws = mutableListOf<DrawCmd>()

        for (cmd in state.commands) {
            try {
                when (cmd) {
                    is ShapeCommand.RoundedRect -> {
                        val d = buildDraw(cmd, mv, device) ?: continue
                        draws.add(d)
                    }
                    is ShapeCommand.Circle -> {
                        val d = buildDraw(cmd, mv, device) ?: continue
                        draws.add(d)
                    }
                }
            } catch (e: Exception) {
                Hkim.logger.error("PIPRenderer: prepare failed", e)
            }
        }

        if (draws.isEmpty()) {
            encoder.submit()
            return
        }

        try {
            encoder.createRenderPass(
                { "hkim_gui2d_render" },
                stateTex.view,
                Optional.empty(),
                stateTex.depthView,
                OptionalDouble.empty()
            ).use { rp ->
                for (d in draws) {
                    try {
                        rp.setPipeline(d.pipeline)
                        rp.setUniform("Projection", projBuf)
                        rp.setUniform("DynamicTransforms", d.dynTrans)
                        rp.setVertexBuffer(0, d.vtxBuffer.slice())
                        rp.setIndexBuffer(d.indexBuffer, d.indexType)
                        rp.drawIndexed(d.indexCount, 1, 0, 0, 0)
                    } catch (e: Exception) {
                        Hkim.logger.error("PIPRenderer: draw error", e)
                    }
                }
            }
            encoder.submit()
        } catch (e: Exception) {
            Hkim.logger.error("PIPRenderer: render pass failed", e)
        }
    }

    override fun blitTexture(state: PIPState, guiRenderState: GuiRenderState) {
        val tex = currentStateTex ?: return

        val scissor = state.scissorArea()
        if (scissor.width() <= 0 || scissor.height() <= 0) {
            if (Hkim.logger.isTraceEnabled) {
                Hkim.logger.trace("PIPRenderer: skip blit with zero-area scissor {}x{}", scissor.width(), scissor.height())
            }
            return
        }

        val guiScale = mc.window.guiScale
        val winW = mc.window.width
        val winH = mc.window.height
        val left = scissor.left() * guiScale
        val top = scissor.top() * guiScale
        val right = (scissor.right() * guiScale).coerceAtMost(winW)
        val bottom = (scissor.bottom() * guiScale).coerceAtMost(winH)
        if (right <= left || bottom <= top) {
            if (Hkim.logger.isTraceEnabled) {
                Hkim.logger.trace("PIPRenderer: skip off-screen blit at ({},{}..{},{}) scale={} scr={}x{}",
                    scissor.left(), scissor.top(), scissor.right(), scissor.bottom(),
                    guiScale, winW, winH)
            }
            return
        }

        try {
            guiRenderState.addBlitToCurrentLayer(
                BlitRenderState(
                    RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                    TextureSetup.singleTexture(tex.view, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                    state.pose(),
                    state.x0(),
                    state.y0(),
                    state.x1(),
                    state.y1(),
                    0.0F, 1.0F, 1.0F, 0.0F,
                    -1,
                    scissor,
                    null
                )
            )
        } catch (e: Exception) {
            Hkim.logger.error("PIPRenderer: blit failed for state at ({},{}), scissor {}x{}",
                state.x0(), state.y0(), scissor.width(), scissor.height(), e)
        }
    }

    private lateinit var quadIndexBuffer: GpuBuffer

    private val projPool = mutableListOf<GpuBuffer>()
    private var projPoolIndex = 0

    private val dynTransPool = mutableListOf<GpuBuffer>()
    private var dynPoolIndex = 0

    private fun ensureStaticBuffers(device: GpuDevice) {
        if (this::quadIndexBuffer.isInitialized) return

        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)
        val idxBb = java.nio.ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder())
        idxBb.asShortBuffer().put(indices); idxBb.rewind()
        val idxBuf = device.createBuffer({ "hkim_idx_static" },
            GpuBuffer.USAGE_INDEX or GpuBuffer.USAGE_MAP_WRITE, indices.size.toLong() * 2L)
        idxBuf.map(0, indices.size.toLong() * 2L, false, true).use { it.data().put(idxBb) }
        quadIndexBuffer = idxBuf
    }

    private fun obtainProj(device: GpuDevice): GpuBuffer {
        if (projPoolIndex >= projPool.size) {
            val buf = device.createBuffer({ "hkim_proj_pool" },
                GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_MAP_WRITE, 64)
            projPool.add(buf)
        }
        return projPool[projPoolIndex++]
    }

    private fun writeProjForBounds(buf: GpuBuffer, boundsW: Int, boundsH: Int) {
        val proj = Matrix4f().setOrtho2D(0f, boundsW.toFloat(), boundsH.toFloat(), 0f)
        writeMat4ToBuffer(buf, proj)
    }

    private fun writeMat4ToBuffer(buf: GpuBuffer, m: Matrix4f) {
        val bb = java.nio.ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(floatArrayOf(
            m.m00(), m.m01(), m.m02(), m.m03(),
            m.m10(), m.m11(), m.m12(), m.m13(),
            m.m20(), m.m21(), m.m22(), m.m23(),
            m.m30(), m.m31(), m.m32(), m.m33()
        ))
        bb.position(64).flip()
        buf.map(0, 64, false, true).use { it.data().put(bb) }
    }

    private fun obtainDynTrans(device: GpuDevice): GpuBuffer {
        if (dynPoolIndex >= dynTransPool.size) {
            val buf = device.createBuffer({ "hkim_dyn_pool" },
                GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_MAP_WRITE, 160)
            dynTransPool.add(buf)
        }
        return dynTransPool[dynPoolIndex++]
    }

    private fun writeDynTrans(buf: GpuBuffer, modelView: Matrix4f, texMat: Matrix4f) {
        val bb = java.nio.ByteBuffer.allocateDirect(160).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fun putM(m: Matrix4f) = fb.put(floatArrayOf(
            m.m00(), m.m01(), m.m02(), m.m03(),
            m.m10(), m.m11(), m.m12(), m.m13(),
            m.m20(), m.m21(), m.m22(), m.m23(),
            m.m30(), m.m31(), m.m32(), m.m33()
        ))
        putM(modelView)
        fb.put(floatArrayOf(1f, 1f, 1f, 1f))
        fb.put(floatArrayOf(0f, 0f, 0f, 0f))
        putM(texMat)
        bb.position(160).flip()
        buf.map(0, 160, false, true).use { it.data().put(bb) }
    }

    private fun buildDraw(
        cmd: ShapeCommand.RoundedRect,
        modelView: Matrix4f,
        device: GpuDevice
    ): DrawCmd? {
        val pipeline = CustomRenderPipelines.ROUNDED_RECT
        val c = unpackColor(cmd.fillColor)
        val bc = cmd.borderColor ?: 0

        val texMat = Matrix4f()
        texMat.m00(cmd.x); texMat.m01(cmd.y)
        texMat.m02(cmd.width); texMat.m03(cmd.height)
        texMat.m10(cmd.radius)
        texMat.m11(if (cmd.borderColor != null) cmd.borderWidth else 0f)
        texMat.m12(((bc shr 16) and 0xFF) / 255f)
        texMat.m13(((bc shr 8) and 0xFF) / 255f)
        texMat.m20(((bc and 0xFF) / 255f))
        texMat.m21(((bc shr 24) and 0xFF) / 255f)

        val dynTrans = obtainDynTrans(device).also { writeDynTrans(it, modelView, texMat) }

        val alloc = ByteBufferBuilder(128)
        val buf = BufferBuilder(alloc, PrimitiveTopology.QUADS,
            pipeline.getVertexFormatBinding(0) ?: DefaultVertexFormat.POSITION_COLOR)
        val pad = 2f
        buf.addVertex(cmd.x - pad, cmd.y - pad, 0f).setColor(c[0], c[1], c[2], c[3])
        buf.addVertex(cmd.x + cmd.width + pad, cmd.y - pad, 0f).setColor(c[0], c[1], c[2], c[3])
        buf.addVertex(cmd.x + cmd.width + pad, cmd.y + cmd.height + pad, 0f).setColor(c[0], c[1], c[2], c[3])
        buf.addVertex(cmd.x - pad, cmd.y + cmd.height + pad, 0f).setColor(c[0], c[1], c[2], c[3])
        val mesh = buf.buildOrThrow()
        val vData = mesh.vertexBuffer()
        if (vData.remaining() == 0) { mesh.close(); alloc.close(); return null }
        val vSize = vData.remaining().toLong()

        val vtx = obtainVtx(device, vSize)
        vtx.map(0, vSize, false, true).use { it.data().put(vData) }

        mesh.close(); alloc.close()
        return DrawCmd(pipeline, vtx, dynTrans, quadIndexBuffer, IndexType.SHORT, 6)
    }

    private fun buildDraw(
        cmd: ShapeCommand.Circle,
        modelView: Matrix4f,
        device: GpuDevice
    ): DrawCmd? {
        val pipeline = CustomRenderPipelines.CIRCLE
        val c = unpackColor(cmd.fillColor)
        val bc = cmd.borderColor ?: 0
        val d = cmd.radius * 2f
        val x0 = cmd.cx - cmd.radius; val y0 = cmd.cy - cmd.radius

        val texMat = Matrix4f()
        texMat.m00(cmd.cx); texMat.m01(cmd.cy)
        texMat.m10(cmd.radius)
        texMat.m11(if (cmd.borderColor != null) cmd.borderWidth else 0f)
        texMat.m12(((bc shr 16) and 0xFF) / 255f)
        texMat.m13(((bc shr 8) and 0xFF) / 255f)
        texMat.m20(((bc and 0xFF) / 255f))
        texMat.m21(((bc shr 24) and 0xFF) / 255f)

        val dynTrans = obtainDynTrans(device).also { writeDynTrans(it, modelView, texMat) }

        val alloc = ByteBufferBuilder(128)
        val buf = BufferBuilder(alloc, PrimitiveTopology.QUADS,
            pipeline.getVertexFormatBinding(0) ?: DefaultVertexFormat.POSITION_COLOR)
        val pad = 2f
        buf.addVertex(x0 - pad, y0 - pad, 0f).setColor(c[0], c[1], c[2], c[3])
        buf.addVertex(x0 + d + pad, y0 - pad, 0f).setColor(c[0], c[1], c[2], c[3])
        buf.addVertex(x0 + d + pad, y0 + d + pad, 0f).setColor(c[0], c[1], c[2], c[3])
        buf.addVertex(x0 - pad, y0 + d + pad, 0f).setColor(c[0], c[1], c[2], c[3])
        val mesh = buf.buildOrThrow()
        val vData = mesh.vertexBuffer()
        if (vData.remaining() == 0) { mesh.close(); alloc.close(); return null }
        val vSize = vData.remaining().toLong()

        val vtx = obtainVtx(device, vSize)
        vtx.map(0, vSize, false, true).use { it.data().put(vData) }

        mesh.close();
        alloc.close()
        return DrawCmd(pipeline, vtx, dynTrans, quadIndexBuffer, IndexType.SHORT, 6)
    }

    private data class DrawCmd(
        val pipeline: RenderPipeline,
        val vtxBuffer: GpuBuffer,
        val dynTrans: GpuBuffer,
        val indexBuffer: GpuBuffer,
        val indexType: IndexType,
        val indexCount: Int
    )

    private val vtxPool = mutableListOf<GpuBuffer>()
    private var vtxPoolIndex = 0

    private fun obtainVtx(device: GpuDevice, size: Long): GpuBuffer {
        if (vtxPoolIndex >= vtxPool.size) {
            val buf = device.createBuffer({ "hkim_vtx_pool" },
                GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_MAP_WRITE, size)
            vtxPool.add(buf)
        }
        return vtxPool[vtxPoolIndex++]
    }

    override fun close() {
        for (list in texPool.values) {
            list.forEach { try { it.close() } catch (_: Exception) {} }
        }
        texPool.clear()
        usedTexThisFrame.clear()

        vtxPool.forEach { try { it.close() } catch (_: Exception) {} }
        vtxPool.clear()

        projPool.forEach { try { it.close() } catch (_: Exception) {} }
        projPool.clear()
        dynTransPool.forEach { try { it.close() } catch (_: Exception) {} }
        dynTransPool.clear()

        if (this::quadIndexBuffer.isInitialized) {
            try { quadIndexBuffer.close() } catch (_: Exception) {}
        }

        super.close()
    }
}

private fun unpackColor(c: Int): FloatArray = floatArrayOf(
    ((c shr 16) and 0xFF) / 255f,
    ((c shr 8) and 0xFF) / 255f,
    (c and 0xFF) / 255f,
    ((c shr 24) and 0xFF) / 255f
)
