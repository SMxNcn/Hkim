package cn.hkim.addon.utils.render

import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

object CustomRenderPipelines {
    val LINES_ESP: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation("lines_esp")
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .build()
    )

    val LINES_TRANSLUCENT_ESP: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withLocation("lines_translucent_esp")
            .build()
    )

    val QUADS_ESP: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withCull(false)
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withLocation("quads_esp")
            .build()
    )

    val BACKGROUND: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("hkim", "background"))
            .withVertexShader(Identifier.fromNamespaceAndPath("hkim", "core/bg"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("hkim", "core/bg"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withUniform("HkimBackground", UniformType.UNIFORM_BUFFER)
            .withCull(false)
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build()
    )
}
