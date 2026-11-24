package cn.hkim.addon.utils.render.pip

import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
//? if 26.2 {
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.BindGroupLayout
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.shaders.UniformType
//? } else {
//import com.mojang.renderpearl.api.pipeline.*
//? }

object BlurPipelines {
    private val BIND_GROUP_LAYOUT: BindGroupLayout = createBindGroupLayout()

    val DOWNSAMPLE: RenderPipeline = create("backdrop_downsample")
    val BLUR: RenderPipeline = create("backdrop_blur")
    val COMPOSITE: RenderPipeline = create("backdrop_composite")

    @JvmStatic
    fun ensureInitialized() = Unit

    private fun createBindGroupLayout(): BindGroupLayout {
        //? if 26.2 {
        return BindGroupLayout.builder()
            .withSampler("Sampler0")
            .withUniform("BlurConfig", UniformType.UNIFORM_BUFFER)
            .build()
        //? } else {
        /*return BindGroupLayout.builder()
            .withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("BlurConfig", UniformType.UNIFORM_BUFFER)
            .build()
        *///? }
    }

    private fun create(name: String): RenderPipeline {
        val location = Identifier.fromNamespaceAndPath("hkim", "pipeline/$name")
        return RenderPipelines.register(
            RenderPipeline.builder()
                .withLocation(location)
                .withVertexShader(Identifier.fromNamespaceAndPath("minecraft", "core/screenquad"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("hkim", "core/$name"))
                .withBindGroupLayout(BIND_GROUP_LAYOUT)
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withCull(false)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .build()
        )
    }
}
