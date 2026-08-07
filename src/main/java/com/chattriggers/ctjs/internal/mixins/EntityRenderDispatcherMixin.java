package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.client.Client;
import com.chattriggers.ctjs.internal.engine.CTEvents;
import com.chattriggers.ctjs.api.render.Renderer;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void injectReload(ResourceManager manager, CallbackInfo ci, @Local EntityRendererProvider.Context context) {
        Renderer.initializePlayerRenderers$ctjs(context);
    }

    @Inject(
        method = "submit",
        at = @At("HEAD"),
        cancellable = true
    )
    private <S extends EntityRenderState> void injectRender(S renderState, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack matrixStack, SubmitNodeCollector orderedRenderCommandQueue, CallbackInfo ci) {
        // fixme: this technically works, however i'm unsure whether the targetedEntity is the right entity as code-wise it seems like it's the player/camera entity
//        CTEvents.RENDER_ENTITY.invoker().render(matrixStack, targetedEntity, Client.getMinecraft().getRenderTickCounter().getDynamicDeltaTicks(), ci);
    }
}
