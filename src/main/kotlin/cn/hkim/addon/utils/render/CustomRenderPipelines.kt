package cn.hkim.addon.utils.render

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.vertex.DefaultVertexFormat
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

    val ROUNDED_RECT: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("hkim", "rounded_rect"))
            .withVertexShader(Identifier.fromNamespaceAndPath("hkim", "core/rounded_rect"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("hkim", "core/rounded_rect"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withCull(false)
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .build()
    )

    val CIRCLE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("hkim", "circle"))
            .withVertexShader(Identifier.fromNamespaceAndPath("hkim", "core/circle"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("hkim", "core/circle"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withCull(false)
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .build()
    )
}