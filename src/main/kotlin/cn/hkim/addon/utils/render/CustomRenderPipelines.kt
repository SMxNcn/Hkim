package cn.hkim.addon.utils.render

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.renderpearl.api.pipeline.*
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

object CustomRenderPipelines {

    val LINES_ESP: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation("lines_esp")
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withPrimitiveTopology(PrimitiveTopology.LINES)
            .build()
    )

    val LINES_TRANSLUCENT_ESP: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation("lines_translucent_esp")
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withPrimitiveTopology(PrimitiveTopology.LINES)
            .build()
    )

    val QUADS_ESP: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation("quads_esp")
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withCull(false)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build()
    )


    val BACKGROUND: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("hkim", "background"))
            .withVertexShader(Identifier.fromNamespaceAndPath("hkim", "core/bg"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("hkim", "core/bg"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withBindGroupLayout(
                BindGroupLayout.builder()
                    .withUniform("HkimBackground", UniformType.UNIFORM_BUFFER)
                    .build()
            )
            .withCull(false)
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build()
    )
}