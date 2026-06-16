package cn.hkim.addon.utils.render.pip

import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import org.joml.Matrix3x2fc
import org.joml.Matrix4f

class PIPState(
    private val x0Val: Int,
    private val y0Val: Int,
    private val x1Val: Int,
    private val y1Val: Int,
    private val scaleVal: Float,
    private val scissorAreaVal: ScreenRectangle?,
    private val poseVal: Matrix3x2fc = PictureInPictureRenderState.IDENTITY_POSE,
    val commands: List<ShapeCommand>,
    val modelViewMatrix: Matrix4f = Matrix4f()
) : PictureInPictureRenderState {

    private val boundsVal: ScreenRectangle = PictureInPictureRenderState.getBounds(x0Val, y0Val, x1Val, y1Val, scissorAreaVal)!!

    override fun x0(): Int = x0Val
    override fun y0(): Int = y0Val
    override fun x1(): Int = x1Val
    override fun y1(): Int = y1Val
    override fun scale(): Float = scaleVal
    override fun scissorArea(): ScreenRectangle = scissorAreaVal ?: PictureInPictureRenderState.getBounds(x0Val, y0Val, x1Val, y1Val, null)!!
    override fun pose(): Matrix3x2fc = poseVal
    override fun bounds(): ScreenRectangle = boundsVal
}

sealed interface ShapeCommand {
    data class RoundedRect(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val radius: Float,
        val fillColor: Int,
        val borderColor: Int?,
        val borderWidth: Float
    ) : ShapeCommand

    data class Circle(
        val cx: Float,
        val cy: Float,
        val radius: Float,
        val fillColor: Int,
        val borderColor: Int?,
        val borderWidth: Float
    ) : ShapeCommand

}
