package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
//    @Inject(
//            method = "submit",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"
//            ),
//            cancellable = true,
//            locals = LocalCapture.CAPTURE_FAILSOFT
//    )
//    private <S extends BlockEntityRenderState> void injectRender(S renderState, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraRenderState, CallbackInfo ci, BlockEntityRenderer blockEntityRenderer) {
//        // fixme
////        if (blockEntity.getEntityWorld() != null && Objects.requireNonNull(blockEntity.getEntityWorld()).isClient()) {
////            CTEvents.RENDER_BLOCK_ENTITY.invoker().render(matrices, blockEntity, Client.getMinecraft().getRenderTickCounter().getDynamicDeltaTicks(), ci);
////        }
//    }
}
