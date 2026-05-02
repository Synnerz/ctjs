package com.chattriggers.ctjs.api.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.platform.DestFactor
import com.mojang.blaze3d.platform.SourceFactor
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderSetup

object LegacyPipelineBuilder {
    private val layerList = mutableMapOf<String, RenderLayer>()
    private val pipelineList = mutableMapOf<String, RenderPipeline>()
    private var cull: Boolean? = null
    private var depth: Boolean? = null
    private var blend: Boolean? = null
    private var drawMode = Renderer.DrawMode.QUADS
    private var vertexFormat = Renderer.VertexFormat.POSITION_COLOR
    private var snippet = Renderer.RenderSnippet.POSITION_COLOR_SNIPPET

    fun begin(
        drawMode: Renderer.DrawMode = Renderer.DrawMode.QUADS,
        vertexFormat: Renderer.VertexFormat = Renderer.VertexFormat.POSITION_COLOR,
        snippet: Renderer.RenderSnippet = Renderer.RenderSnippet.POSITION_COLOR_SNIPPET
    ) = apply {
        this.drawMode = drawMode
        this.vertexFormat = vertexFormat
        this.snippet = snippet
    }

    fun enabledBlend() = apply {
        blend = true
    }

    fun disableBlend() = apply {
        blend = false
    }

    fun enableCull() = apply {
        cull = true
    }

    fun disableCull() = apply {
        cull = false
    }

    fun enableDepth() = apply {
        depth = true
    }

    fun disableDepth() = apply {
        depth = false
    }

    fun build(): RenderPipeline {
        if (pipelineList.containsKey(state())) return pipelineList[state()]!!

        val basePipeline = RenderPipeline.builder(snippet.mcSnippet)
            .withLocation("ctjs/custom/pipeline${hashCode()}")
            .withVertexFormat(vertexFormat.toMC(), drawMode.toUC().mcMode)
        if (blend == true) basePipeline.withBlend(BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO))

        if (cull != null) basePipeline.withCull(cull!!)

        if (depth == true) {
            basePipeline
                .withDepthWrite(depth!!)
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        }
        else if (depth == false) {
            basePipeline
                .withDepthWrite(depth!!)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        }

        val pipeline = basePipeline.build()
        pipelineList[state()] = pipeline

        return pipeline
    }

    fun layer(): RenderLayer {
        if (layerList.containsKey(state())) return layerList[state()]!!

        // this is probably very, very wrong but i cannot be bothered to test
        val layer = RenderLayer.of(
            "ctjs/custom/layer${hashCode()}",
            RenderSetup.builder(
                RenderPipeline.builder().build()
            ).build()
        )
        layerList[state()] = layer

        return layer
    }

    fun state(): String {
        return "LegacyPipelineBuilder[" +
                "cull=$cull, " +
                "depth=$depth, " +
                "blend=$blend, " +
                "drawMode=${drawMode.name}, " +
                "vertexFormat=${vertexFormat.name}, " +
                "snippet=${snippet.name}" +
                "]"
    }
}
