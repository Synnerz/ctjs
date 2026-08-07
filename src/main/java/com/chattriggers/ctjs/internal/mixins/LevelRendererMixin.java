package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.listeners.WorldListener;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(
        method = "renderBlockOutline",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/state/CameraRenderState;pos:Lnet/minecraft/world/phys/Vec3;"
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void onDrawBlockOutline(MultiBufferSource.BufferSource immediate, PoseStack matrices, boolean renderBlockOutline, LevelRenderState renderStates, CallbackInfo ci, BlockOutlineRenderState outlineRenderState) {
        if (WorldListener.INSTANCE.triggerBlockOutline(outlineRenderState.pos()))
            ci.cancel();
    }

    @ModifyExpressionValue(
        method = "method_62214",
        at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;")
    )
    private PoseStack onMatrixStack(PoseStack original) {
        WorldListener.INSTANCE.setMatrixStack(original);
        return original;
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void beforeRender(GraphicsResourceAllocator allocator, DeltaTracker tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        WorldListener.INSTANCE.triggerRenderStart(tickCounter.getGameTimeDeltaTicks());
    }

    @Inject(method = "method_62214", at = @At("RETURN"))
    private void afterRender(GpuBufferSlice gpuBufferSlice, LevelRenderState levelRenderState, ProfilerFiller profilerFiller, Matrix4f matrix4f, ResourceHandle resourceHandle, ResourceHandle resourceHandle2, boolean bl, ResourceHandle resourceHandle3, ResourceHandle resourceHandle4, CallbackInfo ci) {
        WorldListener.INSTANCE.triggerRenderLast();
    }
}
