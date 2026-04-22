// Origin RenderUtils: https://github.com/odtheking/Odin/blob/main/src/main/kotlin/com/odtheking/odin/utils/render/RenderUtils.kt

package cn.hkim.addon.utils.render

import cn.hkim.addon.Hkim.mc
import cn.hkim.addon.events.impl.RenderEvent
import cn.hkim.addon.utils.HudUtils.multiplyAlpha
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
import org.joml.Vector3f
import java.awt.Color

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

    fun renderBatch(context: LevelRenderContext) {
        val matrix = context.poseStack()
        val bufferSource = context.bufferSource()
        val camera = context.gameRenderer().mainCamera.position()

        matrix.pushPose()
        matrix.translate(-camera.x, -camera.y, -camera.z)

        matrix.renderBatchedLinesAndWireBoxes(renderConsumer.lines, renderConsumer.wireBoxes, bufferSource)
        matrix.renderBatchedFilledBoxes(renderConsumer.filledBoxes, bufferSource)

        matrix.popPose()

        matrix.renderBatchedTexts(renderConsumer.texts, bufferSource, camera)
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

private fun PoseStack.renderBatchedLinesAndWireBoxes(
    lines: List<LineData>,
    wireBoxes: List<BoxData>,
    bufferSource: MultiBufferSource.BufferSource
) {
    if (lines.isEmpty() && wireBoxes.isEmpty()) return
    val last = this.last()

    for (line in lines) {

        val dirX = line.to.x - line.from.x
        val dirY = line.to.y - line.from.y
        val dirZ = line.to.z - line.from.z
        val buffer = bufferSource.getBuffer(line.renderType())

        PrimitiveRenderer.renderVector(
            last, buffer,
            Vector3f(line.from.x.toFloat(), line.from.y.toFloat(), line.from.z.toFloat()),
            Vec3(dirX, dirY, dirZ),
            line.color1, line.color2,
            line.thickness
        )
    }

    for (box in wireBoxes) {
        val buffer = bufferSource.getBuffer(box.lineRenderType())
        PrimitiveRenderer.renderLineBox(
            last, buffer, box.aabb,
            box.r, box.g, box.b, box.a,
            box.thickness
        )
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

private fun PoseStack.renderBatchedTexts(consumer: List<TextData>, bufferSource: MultiBufferSource.BufferSource, camera: Vec3) {
    val cameraPos = Vec3(-camera.x, -camera.y, -camera.z)

    for (textData in consumer) {
        pushPose()
        val pose = last().pose()
        val scaleFactor = textData.scale * 0.025f

        pose
            .rotate(textData.cameraRotation)
            .translate(textData.pos.toVector3f())
            .translate(cameraPos.x.toFloat(), cameraPos.y.toFloat(), cameraPos.z.toFloat())
            .scale(scaleFactor, -scaleFactor, scaleFactor)

        textData.font.drawInBatch(
            textData.text, -textData.textWidth / 2f, 0f, -1, true, pose, bufferSource,
            if (textData.depth) Font.DisplayMode.NORMAL else Font.DisplayMode.SEE_THROUGH,
            0, LightCoordsUtil.FULL_BRIGHT
        )

        popPose()
    }
}

fun RenderEvent.Extract.drawWireFrameBox(aabb: AABB, color: Color, thickness: Float = 3f, depth: Boolean = false) {
    consumer.wireBoxes.add(
        BoxData(aabb, color.red.toFloat(), color.green.toFloat(), color.blue.toFloat(), color.alpha.toFloat(), thickness, depth)
    )
}

fun RenderEvent.Extract.drawFilledBox(aabb: AABB, color: Color, depth: Boolean = false) {
    consumer.filledBoxes.add(
        BoxData(aabb, color.red.toFloat(), color.green.toFloat(), color.blue.toFloat(), color.alpha.toFloat(), 3f, depth)
    )
}

fun RenderEvent.Extract.drawStyledBox(
    aabb: AABB,
    color: Color,
    style: Int = 0,
    depth: Boolean = true
) {
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
        val x0 = aabb.minX.toFloat()
        val y0 = aabb.minY.toFloat()
        val z0 = aabb.minZ.toFloat()
        val x1 = aabb.maxX.toFloat()
        val y1 = aabb.maxY.toFloat()
        val z1 = aabb.maxZ.toFloat()

        val corners = floatArrayOf(
            x0, y0, z0,
            x1, y0, z0,
            x1, y1, z0,
            x0, y1, z0,
            x0, y0, z1,
            x1, y0, z1,
            x1, y1, z1,
            x0, y1, z1
        )

        for (i in edges.indices step 2) {
            val i0 = edges[i] * 3
            val i1 = edges[i + 1] * 3

            val x0 = corners[i0]
            val y0 = corners[i0 + 1]
            val z0 = corners[i0 + 2]
            val x1 = corners[i1]
            val y1 = corners[i1 + 1]
            val z1 = corners[i1 + 2]

            val dx = x1 - x0
            val dy = y1 - y0
            val dz = z1 - z0

            buffer.addVertex(pose, x0, y0, z0).setColor(r, g, b, a).setNormal(pose, dx, dy, dz).setLineWidth(thickness)
            buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, dx, dy, dz).setLineWidth(thickness)
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

        vertex(minX, minY, minZ)
        vertex(minX, minY, minZ)
        vertex(minX, minY, minZ)

        vertex(minX, minY, maxZ)
        vertex(minX, maxY, minZ)
        vertex(minX, maxY, maxZ)

        vertex(minX, maxY, maxZ)

        vertex(minX, minY, maxZ)
        vertex(maxX, maxY, maxZ)
        vertex(maxX, minY, maxZ)

        vertex(maxX, minY, maxZ)

        vertex(maxX, minY, minZ)
        vertex(maxX, maxY, maxZ)
        vertex(maxX, maxY, minZ)

        vertex(maxX, maxY, minZ)

        vertex(maxX, minY, minZ)
        vertex(minX, maxY, minZ)
        vertex(minX, minY, minZ)

        vertex(minX, minY, minZ)

        vertex(maxX, minY, minZ)
        vertex(minX, minY, maxZ)
        vertex(maxX, minY, maxZ)

        vertex(maxX, minY, maxZ)

        vertex(minX, maxY, minZ)
        vertex(minX, maxY, minZ)
        vertex(minX, maxY, maxZ)
        vertex(maxX, maxY, minZ)
        vertex(maxX, maxY, maxZ)

        vertex(maxX, maxY, maxZ)
        vertex(maxX, maxY, maxZ)
    }

    fun renderVector(
        pose: PoseStack.Pose,
        buffer: VertexConsumer,
        start: Vector3f,
        direction: Vec3,
        startColor: Int,
        endColor: Int,
        thickness: Float
    ) {
        val endX = start.x() + direction.x.toFloat()
        val endY = start.y() + direction.y.toFloat()
        val endZ = start.z() + direction.z.toFloat()

        val nx = direction.x.toFloat()
        val ny = direction.y.toFloat()
        val nz = direction.z.toFloat()

        buffer.addVertex(pose, start.x(), start.y(), start.z())
            .setColor(startColor)
            .setNormal(pose, nx, ny, nz)
            .setLineWidth(thickness)

        buffer.addVertex(pose, endX, endY, endZ)
            .setColor(endColor)
            .setNormal(pose, nx, ny, nz)
            .setLineWidth(thickness)
    }
}