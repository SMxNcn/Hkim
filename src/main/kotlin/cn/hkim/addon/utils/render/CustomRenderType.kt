package cn.hkim.addon.utils.render

import net.minecraft.client.renderer.rendertype.LayeringTransform
import net.minecraft.client.renderer.rendertype.OutputTarget
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType

object CustomRenderType {
    val LINES_ESP: RenderType = RenderType.create(
        "lines-esp",
        RenderSetup.builder(CustomRenderPipelines.LINES_ESP)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutputTarget(OutputTarget.MAIN_TARGET)
            .createRenderSetup()
    )

    val LINES_TRANSLUCENT_ESP: RenderType = RenderType.create(
        "lines-translucent-esp",
        RenderSetup.builder(CustomRenderPipelines.LINES_TRANSLUCENT_ESP)
            .setOutputTarget(OutputTarget.MAIN_TARGET)
            .createRenderSetup()
    )

    val QUADS_ESP: RenderType = RenderType.create(
        "quads-esp",
        RenderSetup.builder(CustomRenderPipelines.QUADS_ESP)
            .sortOnUpload()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup()
    )

    val ROUNDED_RECT: RenderType = RenderType.create(
        "hkim:rounded_rect",
        RenderSetup.builder(CustomRenderPipelines.ROUNDED_RECT).createRenderSetup()
    )

    val CIRCLE: RenderType = RenderType.create(
        "hkim:circle",
        RenderSetup.builder(CustomRenderPipelines.CIRCLE).createRenderSetup()
    )
}