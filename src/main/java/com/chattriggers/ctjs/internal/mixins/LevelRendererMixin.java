package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.listeners.WorldListener;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4fc;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(
        method = "submitBlockOutline",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/state/level/LevelRenderState;cameraRenderState:Lnet/minecraft/client/renderer/state/level/CameraRenderState;",
            opcode = Opcodes.GETFIELD
        ),
        cancellable = true
    )
    private void onDrawBlockOutline(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState, CallbackInfo ci, @Local(name = "state") BlockOutlineRenderState state) {
        if (WorldListener.INSTANCE.triggerBlockOutline(state.pos()))
            ci.cancel();
    }

    @ModifyExpressionValue(
        method = "submitFeatures",
        at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;")
    )
    private PoseStack onMatrixStack(PoseStack original) {
        WorldListener.INSTANCE.setMatrixStack(original);
        return original;
    }

    @Inject(method = "submitFeatures", at = @At("RETURN"))
    private void afterRender(LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, boolean renderOutline, CallbackInfo ci) {
        WorldListener.INSTANCE.triggerRenderLast();
    }
}
