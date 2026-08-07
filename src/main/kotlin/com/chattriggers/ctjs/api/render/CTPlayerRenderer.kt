package com.chattriggers.ctjs.api.render

import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.player.AvatarRenderer
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.ArmorModelSet
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.state.CameraRenderState
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.renderer.entity.layers.ArrowLayer
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer
import net.minecraft.client.renderer.entity.layers.CapeLayer
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer
import net.minecraft.client.renderer.entity.layers.WingsLayer

internal class CTPlayerRenderer(
    private val ctx: EntityRendererProvider.Context,
    private val slim: Boolean,
) : AvatarRenderer<AbstractClientPlayer>(ctx, slim) {
    private val PLAYER_SLIM: ArmorModelSet<ModelLayers>
        @Suppress("UNCHECKED_CAST")
        get() = ModelLayers::class.java.getField("PLAYER_SLIM").get(null) as ArmorModelSet<ModelLayers>

    var showArmor = true
        set(value) {
            field = value
            reset()
        }
    var showHeldItem = true
        set(value) {
            field = value
            reset()
        }
    var showArrows = true
        set(value) {
            field = value
            reset()
        }
    var showCape = true
        set(value) {
            field = value
            reset()
        }
    var showElytra = true
        set(value) {
            field = value
            reset()
        }
    var showParrot = true
        set(value) {
            field = value
            reset()
        }
    var showStingers = true
        set(value) {
            field = value
            reset()
        }
    var showNametag = true
        set(value) {
            field = value
            reset()
        }

    fun setOptions(
        showNametag: Boolean = true,
        showArmor: Boolean = true,
        showCape: Boolean = true,
        showHeldItem: Boolean = true,
        showArrows: Boolean = true,
        showElytra: Boolean = true,
        showParrot: Boolean = true,
        showStingers: Boolean = true,
    ) {
        this.showNametag = showNametag
        this.showArmor = showArmor
        this.showCape = showCape
        this.showHeldItem = showHeldItem
        this.showArrows = showArrows
        this.showElytra = showElytra
        this.showParrot = showParrot
        this.showStingers = showStingers

        reset()
    }

    override fun submitNameTag(
        avatarRenderState: AvatarRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        cameraRenderState: CameraRenderState
    ) {
        if (showNametag)
            super.submitNameTag(avatarRenderState, poseStack, submitNodeCollector, cameraRenderState)
    }

    private fun reset() {
        layers.clear()

        val entityModels = ctx.blockRenderDispatcher.blockModelShaper.modelManager.entityModels().get()

        if (showArmor) {
            val layer = if (slim) PLAYER_SLIM else ModelLayers.PLAYER_ARMOR
            addLayer(
                HumanoidArmorLayer(
                    this,
                    ArmorModelSet.bake(
                        layer as ArmorModelSet<ModelLayerLocation>,
                        ctx.modelSet
                    ) { PlayerModel(it, slim) },
                    ctx.equipmentRenderer
                )
            )
        }
        if (showHeldItem)
            addLayer(PlayerItemInHandLayer(this))
        if (showArrows)
            addLayer(ArrowLayer(this, ctx))
        addLayer(Deadmau5EarsLayer(this, entityModels))
        if (showCape)
            addLayer(CapeLayer(this, entityModels, ctx.equipmentAssets))
        if (showArmor)
            addLayer(CustomHeadLayer(this, entityModels, ctx.playerSkinRenderCache))
        if (showElytra)
            addLayer(WingsLayer(this, entityModels, ctx.equipmentRenderer))
        if (showParrot)
            addLayer(ParrotOnShoulderLayer(this, entityModels))
        addLayer(SpinAttackEffectLayer(this, entityModels))
        if (showStingers)
            addLayer(BeeStingerLayer(this, ctx))
    }
}
