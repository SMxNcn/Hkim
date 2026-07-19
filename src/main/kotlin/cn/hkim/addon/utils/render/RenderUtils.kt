package cn.hkim.addon.utils.render

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.RenderEvent
import cn.hkim.addon.utils.HudUtils.aGL
import cn.hkim.addon.utils.HudUtils.bGL
import cn.hkim.addon.utils.HudUtils.gGL
import cn.hkim.addon.utils.HudUtils.multiplyAlpha
import cn.hkim.addon.utils.HudUtils.rGL
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import java.awt.Color

internal data class LineData(val from: Vec3, val to: Vec3, val color1: Int, val color2: Int, val thickness: Float, val depth: Boolean)
internal data class BoxData(val aabb: AABB, val r: Float, val g: Float, val b: Float, val a: Float, val thickness: Float, val depth: Boolean)
internal data class TextData(val text: String, val pos: Vec3, val scale: Float, val depth: Boolean, val cameraRotation: Quaternionf, val font: Font, val textWidth: Float)

// Origin RenderUtils: https://github.com/odtheking/Odin/blob/main/src/main/kotlin/com/odtheking/odin/utils/render/RenderUtils.kt

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

    fun renderBatch(context: LevelRenderContext) {
        if (renderConsumer.lines.isEmpty && renderConsumer.wireBoxes.isEmpty &&
            renderConsumer.filledBoxes.isEmpty && renderConsumer.texts.isEmpty) return

        val matrix = context.poseStack()
        val bufferSource = context.bufferSource()
        val camera = context.gameRenderer().mainCamera.position()

        matrix.pushPose()
        matrix.translate(-camera.x, -camera.y, -camera.z)

        matrix.renderBatchedLinesAndWireBoxes(renderConsumer, bufferSource)
        matrix.renderBatchedFilledBoxes(renderConsumer.filledBoxes, bufferSource)
        matrix.renderBatchedTexts(renderConsumer.texts, bufferSource)

        matrix.popPose()

        bufferSource.endBatch()
        renderConsumer.clear()
    }
}

private fun Int.isFullyOpaque(): Boolean = ((this ushr 24) and 0xFF) == 0xFF

private fun resolveLineRenderType(depth: Boolean, fullyOpaque: Boolean) = when {
    depth && fullyOpaque -> RenderTypes.LINES
    depth -> RenderTypes.LINES_TRANSLUCENT
    fullyOpaque -> CustomRenderType.LINES_ESP
    else -> CustomRenderType.LINES_TRANSLUCENT_ESP
}

private fun LineData.renderType() = resolveLineRenderType(depth, color1.isFullyOpaque() && color2.isFullyOpaque())

private fun BoxData.lineRenderType() = resolveLineRenderType(depth, a >= 0.999f)

private fun BoxData.filledRenderType() = if (depth) RenderTypes.debugFilledBox() else CustomRenderType.QUADS_ESP

private fun PoseStack.renderBatchedLinesAndWireBoxes(consumer: RenderConsumer, bufferSource: MultiBufferSource.BufferSource) {
    val last = this.last()

    for (line in consumer.lines) {
        val buffer = bufferSource.getBuffer(line.renderType())
        val dir = Vec3(line.to.x - line.from.x, line.to.y - line.from.y, line.to.z - line.from.z)

        buffer.addVertex(last, line.from.x.toFloat(), line.from.y.toFloat(), line.from.z.toFloat())
            .setColor(line.color1)
            .setNormal(last, dir.x.toFloat(), dir.y.toFloat(), dir.z.toFloat())
            .setLineWidth(line.thickness)
        buffer.addVertex(last, line.to.x.toFloat(), line.to.y.toFloat(), line.to.z.toFloat())
            .setColor(line.color2)
            .setNormal(last, dir.x.toFloat(), dir.y.toFloat(), dir.z.toFloat())
            .setLineWidth(line.thickness)
    }

    for (box in consumer.wireBoxes) {
        val buffer = bufferSource.getBuffer(box.lineRenderType())
        PrimitiveRenderer.renderLineBox(last, buffer, box.aabb, box.r, box.g, box.b, box.a, box.thickness)
    }
}

private fun PoseStack.renderBatchedFilledBoxes(consumer: List<BoxData>, bufferSource: MultiBufferSource.BufferSource) {
    if (consumer.isEmpty()) return
    val last = this.last()

    for (box in consumer) {
        val buffer = bufferSource.getBuffer(box.filledRenderType())
        PrimitiveRenderer.addChainedFilledBoxVertices(
            last, buffer,
            box.aabb.minX.toFloat(), box.aabb.minY.toFloat(), box.aabb.minZ.toFloat(),
            box.aabb.maxX.toFloat(), box.aabb.maxY.toFloat(), box.aabb.maxZ.toFloat(),
            box.r, box.g, box.b, box.a
        )
    }
}

private fun PoseStack.renderBatchedTexts(consumer: List<TextData>, bufferSource: MultiBufferSource.BufferSource) {
    for (textData in consumer) {
        pushPose()

        translate(textData.pos.x, textData.pos.y, textData.pos.z)

        val invRotation = Quaternionf().set(textData.cameraRotation)
        mulPose(invRotation)

        val scaleFactor = textData.scale * 0.025f
        scale(scaleFactor, -scaleFactor, scaleFactor)

        val displayMode = if (textData.depth) Font.DisplayMode.NORMAL else Font.DisplayMode.SEE_THROUGH
        val color = 0xFFFFFFFF.toInt()

        textData.font.drawInBatch(
            textData.text,
            -textData.textWidth / 2f, 0f,
            color,
            true,
            last().pose(),
            bufferSource,
            displayMode,
            0,
            LightCoordsUtil.FULL_BRIGHT
        )
        popPose()
    }
}

fun RenderEvent.Extract.drawWireFrameBox(aabb: AABB, color: Color, thickness: Float = 3f, depth: Boolean = false) {
    consumer.wireBoxes.add(
        BoxData(aabb, color.rGL, color.gGL, color.bGL, color.aGL, thickness, depth)
    )
}

fun RenderEvent.Extract.drawFilledBox(aabb: AABB, color: Color, depth: Boolean = false) {
    consumer.filledBoxes.add(
        BoxData(aabb, color.rGL, color.gGL, color.bGL, color.aGL, 3f, depth)
    )
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
    val cameraRotation = mc.gameRenderer.mainCamera.rotation()
    val font = mc.font
    val textWidth = font.width(text).toFloat()

    consumer.texts.add(TextData(text, pos, scale, depth, cameraRotation, font, textWidth))
}

fun RenderEvent.Extract.drawLine(from: Vec3, to: Vec3, color: Color, thickness: Float = 1.5f, depth: Boolean = false) {
    consumer.lines.add(LineData(from, to, color.rgb, color.rgb, thickness, depth))
}

/*fun RenderEvent.Extract.drawPathLine(points: List<Vec3>, color: Color, thickness: Float = 1.5f, depth: Boolean = false) {
    if (points.size < 2) return
    for (i in 0 until points.size - 1) {
        drawLine(points[i], points[i + 1], color, thickness, depth)
    }
}*/

object PrimitiveRenderer {
    private val edges = intArrayOf(
        0, 1,  1, 5,  5, 4,  4, 0,
        3, 2,  2, 6,  6, 7,  7, 3,
        0, 3,  1, 2,  5, 6,  4, 7
    )

    fun renderLineBox(
        pose: PoseStack.Pose,
        buffer: VertexConsumer,
        aabb: AABB,
        r: Float, g: Float, b: Float, a: Float,
        thickness: Float
    ) {
        val x0 = aabb.minX.toFloat(); val y0 = aabb.minY.toFloat(); val z0 = aabb.minZ.toFloat()
        val x1 = aabb.maxX.toFloat(); val y1 = aabb.maxY.toFloat(); val z1 = aabb.maxZ.toFloat()

        val corners = floatArrayOf(
            x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0,
            x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1
        )

        for (i in edges.indices step 2) {
            val i0 = edges[i] * 3; val i1 = edges[i + 1] * 3
            val cx0 = corners[i0]; val cy0 = corners[i0+1]; val cz0 = corners[i0+2]
            val cx1 = corners[i1]; val cy1 = corners[i1+1]; val cz1 = corners[i1+2]
            val nx = cx1 - cx0; val ny = cy1 - cy0; val nz = cz1 - cz0

            buffer.addVertex(pose, cx0, cy0, cz0)
                .setColor(r, g, b, a)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(thickness)
            buffer.addVertex(pose, cx1, cy1, cz1)
                .setColor(r, g, b, a)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(thickness)
        }
    }

    fun addChainedFilledBoxVertices(
        pose: PoseStack.Pose,
        buffer: VertexConsumer,
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        val matrix = pose.pose()

        fun vertex(x: Float, y: Float, z: Float) {
            buffer.addVertex(matrix, x, y, z).setColor(r, g, b, a)
        }

        vertex(minX, minY, minZ); vertex(maxX, minY, minZ); vertex(maxX, minY, maxZ); vertex(minX, minY, maxZ)

        vertex(minX, maxY, minZ); vertex(minX, maxY, maxZ); vertex(maxX, maxY, maxZ); vertex(maxX, maxY, minZ)

        vertex(minX, minY, minZ); vertex(maxX, minY, minZ); vertex(maxX, maxY, minZ); vertex(minX, maxY, minZ)

        vertex(minX, minY, maxZ); vertex(minX, maxY, maxZ); vertex(maxX, maxY, maxZ); vertex(maxX, minY, maxZ)

        vertex(minX, minY, minZ); vertex(minX, minY, maxZ); vertex(minX, maxY, maxZ); vertex(minX, maxY, minZ)

        vertex(maxX, minY, minZ); vertex(maxX, maxY, minZ); vertex(maxX, maxY, maxZ); vertex(maxX, minY, maxZ)
    }
}