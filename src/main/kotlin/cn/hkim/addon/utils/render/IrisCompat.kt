package cn.hkim.addon.utils.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import net.fabricmc.loader.api.FabricLoader
import net.irisshaders.iris.api.v0.IrisApi
import net.irisshaders.iris.api.v0.IrisProgram
import net.minecraft.client.renderer.rendertype.RenderType

enum class IrisShaderType {
    LINES,
    BASIC,
}

interface IrisCompatibility {
    fun registerPipeline(pipeline: RenderPipeline, shaderType: IrisShaderType) {}
    fun registerRenderType(pipeline: RenderType, shaderType: IrisShaderType) {
        registerPipeline(pipeline.pipeline(), shaderType)
    }

    companion object : IrisCompatibility by resolve() {
        init {
            registerRenderType(CustomRenderType.LINES_ESP, IrisShaderType.LINES)
            registerRenderType(CustomRenderType.LINES_TRANSLUCENT_ESP, IrisShaderType.LINES)
            registerRenderType(CustomRenderType.QUADS_ESP, IrisShaderType.BASIC)
        }
    }
}

internal object IrisCompatImpl : IrisCompatibility {
    private val instance by lazy { IrisApi.getInstance() }

    override fun registerPipeline(pipeline: RenderPipeline, shaderType: IrisShaderType) {
        val type = when (shaderType) {
            IrisShaderType.BASIC -> IrisProgram.BASIC
            IrisShaderType.LINES -> IrisProgram.LINES
        }
        instance.assignPipeline(pipeline, type)
    }
}

internal object IrisCompatNoOp : IrisCompatibility

internal fun resolve(): IrisCompatibility = if (FabricLoader.getInstance().isModLoaded("iris")) IrisCompatImpl else IrisCompatNoOp