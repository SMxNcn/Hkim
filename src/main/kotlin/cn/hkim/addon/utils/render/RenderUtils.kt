package cn.hkim.addon.utils.render

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.RenderEvent
import cn.hkim.addon.utils.HudUtils.aGL
import cn.hkim.addon.utils.HudUtils.bGL
import cn.hkim.addon.utils.HudUtils.gGL
import cn.hkim.addon.utils.HudUtils.multiplyAlpha
import cn.hkim.addon.utils.HudUtils.rGL
import cn.hkim.addon.utils.render.RenderBatchManager.pipelineContexts
import cn.hkim.addon.utils.render.RenderBatchManager.usedPipelines
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.font.TextRenderable
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import java.awt.Color
import java.util.*

internal data class LineData(val from: Vec3, val to: Vec3, val color1: Int, val color2: Int, val thickness: Float, val depth: Boolean)
internal data class BoxData(val aabb: AABB, val r: Float, val g: Float, val b: Float, val a: Float, val thickness: Float, val depth: Boolean)
internal data class TextData(val text: String, val pos: Vec3, val scale: Float, val depth: Boolean, val cameraRotation: Quaternionf, val font: Font, val textWidth: Float)

class RenderConsumer {
    internal val lines = ObjectArrayList<LineData>()
    internal val filledBoxes = ObjectArrayList<BoxData>()
    internal val wireBoxes = ObjectArrayList<BoxData>()
    internal val texts = ObjectArrayList<TextData>()

    fun clear() {
        lines.clear()
        filledBoxes.clear()
        wireBoxes.clear()
        texts.clear()
    }
}

object RenderBatchManager {
    val renderConsumer = RenderConsumer()

    data class PipelineContext(
        val pipeline: RenderPipeline,
        val allocator: ByteBufferBuilder,
        var bufferBuilder: BufferBuilder,
        var vertexBuffer: GpuBuffer? = null,
        var indexBuffer: GpuBuffer? = null
    )

    val pipelineContexts = mutableMapOf<RenderPipeline, PipelineContext>()
    val usedPipelines = mutableSetOf<RenderPipeline>()
    private const val ALLOCATOR_SIZE = 2097152 // 2MB

    fun init() {
        listOf(
            CustomRenderPipelines.LINES_ESP,
            CustomRenderPipelines.LINES_TRANSLUCENT_ESP,
            CustomRenderPipelines.QUADS_ESP
        ).forEach { pipeline ->
            val allocator = ByteBufferBuilder(ALLOCATOR_SIZE)
            val format = pipeline.getVertexFormatBinding(0) ?: DefaultVertexFormat.POSITION_COLOR
            pipelineContexts[pipeline] = PipelineContext(pipeline, allocator, BufferBuilder(allocator, pipeline.primitiveTopology, format))
        }
    }

    fun renderBatch(context: LevelRenderContext) {
        if (renderConsumer.lines.isEmpty && renderConsumer.wireBoxes.isEmpty &&
            renderConsumer.filledBoxes.isEmpty && renderConsumer.texts.isEmpty
        ) return

        val matrix = context.poseStack()
        extractLines(renderConsumer.lines, matrix)
        extractWireBoxes(renderConsumer.wireBoxes, matrix)
        extractFilledBoxes(renderConsumer.filledBoxes, matrix)

        val encoder = RenderSystem.getDevice().createCommandEncoder()

        val camState = context.levelState().cameraRenderState
        val viewRot = Quaternionf().set(camState.orientation).conjugate()
        val viewMatrix = Matrix4f()
            .rotation(viewRot)
            .translate(-camState.pos.x.toFloat(), -camState.pos.y.toFloat(), -camState.pos.z.toFloat())
        drawAllPipelines(viewMatrix, encoder)
        usedPipelines.clear()

        if (renderConsumer.texts.isNotEmpty()) {
            drawTexts(renderConsumer.texts, camState, viewMatrix, encoder)
        }

        renderConsumer.clear()
    }
}

private fun Int.isFullyOpaque(): Boolean = ((this ushr 24) and 0xFF) == 0xFF

private fun extractLines(lines: List<LineData>, matrices: PoseStack) {
    val pose = matrices.last()
    for (line in lines) {
        val pipeline = if (line.depth && line.color1.isFullyOpaque() && line.color2.isFullyOpaque()) {
            CustomRenderPipelines.LINES_ESP
        } else {
            CustomRenderPipelines.LINES_TRANSLUCENT_ESP
        }
        val ctx = pipelineContexts[pipeline] ?: continue
        usedPipelines.add(pipeline)
        val buffer = ctx.bufferBuilder
        val dirX = (line.to.x - line.from.x).toFloat()
        val dirY = (line.to.y - line.from.y).toFloat()
        val dirZ = (line.to.z - line.from.z).toFloat()

        buffer.addVertex(line.from.x.toFloat(), line.from.y.toFloat(), line.from.z.toFloat())
            .setColor(line.color1)
            .setNormal(pose, dirX, dirY, dirZ)
            .setLineWidth(line.thickness)

        buffer.addVertex(line.to.x.toFloat(), line.to.y.toFloat(), line.to.z.toFloat())
            .setColor(line.color2)
            .setNormal(pose, dirX, dirY, dirZ)
            .setLineWidth(line.thickness)
    }
}

private fun extractWireBoxes(boxes: List<BoxData>, matrices: PoseStack) {
    val pose = matrices.last()
    if (boxes.isEmpty()) return
    usedPipelines.add(CustomRenderPipelines.LINES_ESP)
    val ctx = pipelineContexts[CustomRenderPipelines.LINES_ESP] ?: return
    val buffer = ctx.bufferBuilder

    for (box in boxes) {
        val x0 = box.aabb.minX.toFloat(); val y0 = box.aabb.minY.toFloat(); val z0 = box.aabb.minZ.toFloat()
        val x1 = box.aabb.maxX.toFloat(); val y1 = box.aabb.maxY.toFloat(); val z1 = box.aabb.maxZ.toFloat()
        val corners = floatArrayOf(x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1)

        val edges = intArrayOf(0,1, 1,5, 5,4, 4,0, 3,2, 2,6, 6,7, 7,3, 0,3, 1,2, 5,6, 4,7)

        for (i in edges.indices step 2) {
            val i0 = edges[i] * 3; val i1 = edges[i + 1] * 3
            val cx0 = corners[i0]; val cy0 = corners[i0+1]; val cz0 = corners[i0+2]
            val cx1 = corners[i1]; val cy1 = corners[i1+1]; val cz1 = corners[i1+2]
            val nx = cx1 - cx0; val ny = cy1 - cy0; val nz = cz1 - cz0

            buffer.addVertex(cx0, cy0, cz0)
                .setColor(box.r, box.g, box.b, box.a)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(box.thickness)
            buffer.addVertex(cx1, cy1, cz1)
                .setColor(box.r, box.g, box.b, box.a)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(box.thickness)
        }
    }
}

@Suppress("UNUSED_PARAMETER")
private fun extractFilledBoxes(boxes: List<BoxData>, matrices: PoseStack) {
    if (boxes.isEmpty()) return
    usedPipelines.add(CustomRenderPipelines.QUADS_ESP)
    val ctx = pipelineContexts[CustomRenderPipelines.QUADS_ESP] ?: return
    val buffer = ctx.bufferBuilder

    for (box in boxes) {
        val x0 = box.aabb.minX.toFloat(); val y0 = box.aabb.minY.toFloat(); val z0 = box.aabb.minZ.toFloat()
        val x1 = box.aabb.maxX.toFloat(); val y1 = box.aabb.maxY.toFloat(); val z1 = box.aabb.maxZ.toFloat()

        addTriangle(buffer, x0, y0, z1, x1, y0, z1, x1, y1, z1, box)
        addTriangle(buffer, x0, y0, z1, x1, y1, z1, x0, y1, z1, box)

        addTriangle(buffer, x1, y0, z0, x0, y0, z0, x0, y1, z0, box)
        addTriangle(buffer, x1, y0, z0, x0, y1, z0, x1, y1, z0, box)

        addTriangle(buffer, x0, y0, z0, x0, y0, z1, x0, y1, z1, box)
        addTriangle(buffer, x0, y0, z0, x0, y1, z1, x0, y1, z0, box)

        addTriangle(buffer, x1, y0, z1, x1, y0, z0, x1, y1, z0, box)
        addTriangle(buffer, x1, y0, z1, x1, y1, z0, x1, y1, z1, box)

        addTriangle(buffer, x0, y1, z1, x1, y1, z1, x1, y1, z0, box)
        addTriangle(buffer, x0, y1, z1, x1, y1, z0, x0, y1, z0, box)

        addTriangle(buffer, x0, y0, z0, x1, y0, z0, x1, y0, z1, box)
        addTriangle(buffer, x0, y0, z0, x1, y0, z1, x0, y0, z1, box)
    }
}

private fun addTriangle(buffer: BufferBuilder, x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, box: BoxData) {
    buffer.addVertex(x0, y0, z0).setColor(box.r, box.g, box.b, box.a)
    buffer.addVertex(x1, y1, z1).setColor(box.r, box.g, box.b, box.a)
    buffer.addVertex(x2, y2, z2).setColor(box.r, box.g, box.b, box.a)
}


private fun drawTexts(texts: List<TextData>, camState: CameraRenderState, viewMatrix: Matrix4f, encoder: CommandEncoder) {
    val seeThroughTexts = texts.filter { !it.depth }
    val depthTexts = texts.filter { it.depth }

    if (seeThroughTexts.isNotEmpty()) {
        drawTextsWithPipeline(seeThroughTexts, camState, encoder, RenderPipelines.TEXT_SEE_THROUGH, viewMatrix)
    }
    if (depthTexts.isNotEmpty()) {
        drawTextsWithPipeline(depthTexts, camState, encoder, RenderPipelines.TEXT_POLYGON_OFFSET, viewMatrix)
    }
}

private fun drawTextsWithPipeline(
    texts: List<TextData>,
    camState: CameraRenderState,
    encoder: CommandEncoder,
    pipeline: RenderPipeline,
    viewMatrix: Matrix4f
) {
    val allocator = ByteBufferBuilder(65536) // 64KB for text
    val format = pipeline.getVertexFormatBinding(0) ?: DefaultVertexFormat.POSITION_TEX_COLOR
    val buffer = BufferBuilder(allocator, PrimitiveTopology.QUADS, format)
    var fontTexture: GpuTextureView? = null

    for (text in texts) {
        val posX = text.pos.x.toFloat()
        val posY = text.pos.y.toFloat()
        val posZ = text.pos.z.toFloat()
        val scaleFactor = text.scale * 0.025f

        val positionMatrix = Matrix4f()
            .translate(posX, posY, posZ)
            .rotate(camState.orientation)
            .scale(scaleFactor, -scaleFactor, scaleFactor)

        val preparedText = text.font.prepareText(
            text.text,
            -text.textWidth / 2f, 0f,
            0xFFFFFFFF.toInt(),
            true,
            0
        )

        preparedText.visit(object : Font.GlyphVisitor {
            override fun acceptGlyph(glyph: TextRenderable.Styled) {
                if (fontTexture == null) {
                    fontTexture = glyph.textureView()
                }
                glyph.render(positionMatrix, buffer, LightCoordsUtil.FULL_BRIGHT, false)
            }

            override fun acceptEffect(effect: TextRenderable) {
                if (fontTexture == null) {
                    fontTexture = effect.textureView()
                }
                effect.render(positionMatrix, buffer, LightCoordsUtil.FULL_BRIGHT, false)
            }

            override fun acceptRenderable(renderable: TextRenderable) {
                if (fontTexture == null) {
                    fontTexture = renderable.textureView()
                }
                renderable.render(positionMatrix, buffer, LightCoordsUtil.FULL_BRIGHT, false)
            }
        })
    }

    if (fontTexture == null) {
        allocator.close()
        return
    }

    flushTextMesh(buffer, allocator, pipeline, fontTexture, encoder, viewMatrix)
}

private fun flushTextMesh(
    buffer: BufferBuilder,
    allocator: ByteBufferBuilder,
    pipeline: RenderPipeline,
    fontTexture: GpuTextureView,
    encoder: CommandEncoder,
    viewMatrix: Matrix4f
) {
    val meshData = buffer.buildOrThrow()
    val vertexData = meshData.vertexBuffer()
    if (vertexData.remaining() == 0) {
        meshData.close()
        allocator.close()
        return
    }

    val bufferSize = vertexData.remaining().toLong()
    val vertexBuffer = RenderSystem.getDevice().createBuffer(
        { "hkim_text" },
        GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST,
        bufferSize * 2
    )
    encoder.writeToBuffer(vertexBuffer.slice(0, bufferSize), vertexData)

    val drawState = meshData.drawState()
    val seqBuffer = RenderSystem.getSequentialBuffer(pipeline.primitiveTopology)
    val indices = seqBuffer.getBuffer(drawState.indexCount())
    val indexType = seqBuffer.type()

    val dynamicTransforms = RenderSystem.getDynamicUniforms()
        .writeTransform(viewMatrix, COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX)

    val renderPass = encoder.createRenderPass(
        { "hkim_text" },
        mc.gameRenderer.mainRenderTarget().colorTextureView!!,
        Optional.empty(),
        mc.gameRenderer.mainRenderTarget().depthTextureView!!,
        OptionalDouble.empty()
    )
    renderPass.use { rp ->
        rp.setPipeline(pipeline)
        RenderSystem.bindDefaultUniforms(rp)
        rp.setUniform("DynamicTransforms", dynamicTransforms)
        rp.bindTexture("Sampler0", fontTexture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
        rp.setVertexBuffer(0, vertexBuffer.slice())
        rp.setIndexBuffer(indices, indexType)
        rp.drawIndexed(drawState.indexCount(), 1, 0, 0, 0)
    }

    meshData.close()
    vertexBuffer.close()
    allocator.close()
}

private fun drawAllPipelines(viewMatrix: Matrix4f, encoder: CommandEncoder) {
    usedPipelines.forEach { pipeline ->
        pipelineContexts[pipeline]?.let { drawPipeline(it, viewMatrix, encoder) }
    }
}

private val COLOR_MODULATOR = Vector4f(1f, 1f, 1f, 1f)
private val MODEL_OFFSET = Vector3f()
private val TEXTURE_MATRIX = Matrix4f()

private fun drawPipeline(ctx: RenderBatchManager.PipelineContext, viewMatrix: Matrix4f, encoder: CommandEncoder) {
    val meshData = ctx.bufferBuilder.buildOrThrow()
    ctx.bufferBuilder = BufferBuilder(ctx.allocator, ctx.pipeline.primitiveTopology, ctx.pipeline.getVertexFormatBinding(0)!!)

    val vertexData = meshData.vertexBuffer()
    if (vertexData.remaining() == 0) {
        meshData.close()
        ctx.allocator.clear()
        return
    }

    val bufferSize = vertexData.remaining().toLong()
    if (ctx.vertexBuffer == null || ctx.vertexBuffer!!.size() < bufferSize) {
        ctx.vertexBuffer?.close()
        ctx.vertexBuffer = RenderSystem.getDevice().createBuffer(
            { "hkim_${ctx.pipeline.location}" },
            GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST,
            bufferSize * 2
        )
    }
    encoder.writeToBuffer(ctx.vertexBuffer!!.slice(0, bufferSize), vertexData)

    val drawState = meshData.drawState()
    val seqBuffer = RenderSystem.getSequentialBuffer(ctx.pipeline.primitiveTopology)
    val indices = seqBuffer.getBuffer(drawState.indexCount())
    val indexType = seqBuffer.type()

    val dynamicTransforms = RenderSystem.getDynamicUniforms()
        .writeTransform(viewMatrix, COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX)

    val renderPass = encoder.createRenderPass(
        { "hkim_${ctx.pipeline.location}" },
        mc.gameRenderer.mainRenderTarget().colorTextureView!!,
        Optional.empty(),
        mc.gameRenderer.mainRenderTarget().depthTextureView!!,
        OptionalDouble.empty()
    )
    renderPass.use { rp ->
        rp.setPipeline(ctx.pipeline)
        RenderSystem.bindDefaultUniforms(rp)
        rp.setUniform("DynamicTransforms", dynamicTransforms)
        rp.setVertexBuffer(0, ctx.vertexBuffer!!.slice())
        rp.setIndexBuffer(indices, indexType)

        rp.drawIndexed(drawState.indexCount(), 1, 0, 0, 0)
    }

    meshData.close()
    ctx.allocator.clear()
}

fun RenderEvent.Extract.drawWireFrameBox(aabb: AABB, color: Color, thickness: Float = 3f, depth: Boolean = false) {
    consumer.wireBoxes.add(BoxData(aabb, color.rGL, color.gGL, color.bGL, color.aGL, thickness, depth))
}
fun RenderEvent.Extract.drawFilledBox(aabb: AABB, color: Color, depth: Boolean = false) {
    consumer.filledBoxes.add(BoxData(aabb, color.rGL, color.gGL, color.bGL, color.aGL, 3f, depth))
}
fun RenderEvent.Extract.drawStyledBox(aabb: AABB, color: Color, style: Int = 0, depth: Boolean = true) {
    when (style) {
        0 -> drawFilledBox(aabb, color, depth = depth)
        1 -> drawWireFrameBox(aabb, color, depth = depth)
        2 -> {
            drawFilledBox(aabb, color.multiplyAlpha(0.5f), depth = depth)
            drawWireFrameBox(aabb, color, depth = depth)
        }
    }
}
fun RenderEvent.Extract.drawText(text: String, pos: Vec3, scale: Float, depth: Boolean) {
    val cameraRotation = mc.gameRenderer.mainCamera().rotation()
    val font = mc.font
    consumer.texts.add(TextData(text, pos, scale, depth, cameraRotation, font, font.width(text).toFloat()))
}