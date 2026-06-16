package cn.hkim.addon.utils.render.pip

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import org.joml.Matrix4f

object ShapeRenderer {

    private fun buildAndSubmitPipState(
        graphics: GuiGraphicsExtractor,
        x: Float, y: Float,
        w: Float, h: Float,
        commands: List<ShapeCommand>
    ) {
        if (commands.isEmpty()) return
        if (w <= 0f || h <= 0f) return

        val pad = 2f
        val bx = (x - pad).toInt().coerceAtLeast(0)
        val by = (y - pad).toInt().coerceAtLeast(0)
        val bw = (w + pad * 2).toInt().coerceAtLeast(1)
        val bh = (h + pad * 2).toInt().coerceAtLeast(1)

        val parentScissor = graphics.scissorStack.peek()
        val pipScissor: ScreenRectangle? = if (parentScissor != null) {
            val selfBounds = ScreenRectangle(bx, by, bw, bh)
            val clipped = parentScissor.intersection(selfBounds) ?: return
            clipped
        } else {
            null
        }

        val offsetMv = Matrix4f().identity().translate(-bx.toFloat(), -by.toFloat(), 0f)

        val state = PIPState(
            x0Val = bx, y0Val = by,
            x1Val = bx + bw, y1Val = by + bh,
            scaleVal = 1.0f,
            scissorAreaVal = pipScissor,
            poseVal = PictureInPictureRenderState.IDENTITY_POSE,
            commands = commands,
            modelViewMatrix = offsetMv
        )
        graphics.guiRenderState.addPicturesInPictureState(state)
    }

    fun GuiGraphicsExtractor.drawRoundedRect(
        x: Float, y: Float,
        width: Float, height: Float,
        color: Int,
        radius: Float
    ) {
        buildAndSubmitPipState(this, x, y, width, height, listOf(
            ShapeCommand.RoundedRect(x, y, width, height, radius, color, null, 0f)
        ))
    }

    fun GuiGraphicsExtractor.drawRoundedRectWithBorder(
        x: Float, y: Float,
        width: Float, height: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        radius: Float
    ) {
        buildAndSubmitPipState(this, x, y, width, height, listOf(
            ShapeCommand.RoundedRect(x, y, width, height, radius, fillColor, borderColor, borderWidth)
        ))
    }

    fun GuiGraphicsExtractor.drawCircle(
        cx: Float, cy: Float,
        color: Int,
        radius: Float
    ) {
        val x0 = cx - radius
        val y0 = cy - radius
        val d = radius * 2f
        buildAndSubmitPipState(this, x0, y0, d, d, listOf(
            ShapeCommand.Circle(cx, cy, radius, color, null, 0f)
        ))
    }

    fun GuiGraphicsExtractor.drawCircleWithBorder(
        cx: Float, cy: Float,
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        radius: Float
    ) {
        val x0 = cx - radius
        val y0 = cy - radius
        val d = radius * 2f
        buildAndSubmitPipState(this, x0, y0, d, d, listOf(
            ShapeCommand.Circle(cx, cy, radius, fillColor, borderColor, borderWidth)
        ))
    }

}
