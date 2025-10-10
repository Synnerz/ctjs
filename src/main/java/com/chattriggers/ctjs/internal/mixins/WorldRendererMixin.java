package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.listeners.WorldListener;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(
        method = "renderTargetBlockOutline",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/render/state/CameraRenderState;pos:Lnet/minecraft/util/math/Vec3d;"
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void onDrawBlockOutline(VertexConsumerProvider.Immediate immediate, MatrixStack matrices, boolean renderBlockOutline, WorldRenderState renderStates, CallbackInfo ci, OutlineRenderState outlineRenderState) {
        if (WorldListener.INSTANCE.triggerBlockOutline(outlineRenderState.pos()))
            ci.cancel();
    }

    @ModifyExpressionValue(
        method = "method_62214",
        at = @At(value = "NEW", target = "()Lnet/minecraft/client/util/math/MatrixStack;")
    )
    private MatrixStack onMatrixStack(MatrixStack original) {
        WorldListener.INSTANCE.setMatrixStack(original);
        return original;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        WorldListener.INSTANCE.triggerRenderStart(tickCounter.getDynamicDeltaTicks());
    }

    @Inject(method = "method_62214", at = @At("RETURN"))
    private void afterRender(GpuBufferSlice gpuBufferSlice, WorldRenderState worldRenderState, Profiler profiler, Matrix4f matrix4f, Handle handle, Handle handle2, boolean bl, Handle handle3, Handle handle4, CallbackInfo ci) {
        WorldListener.INSTANCE.triggerRenderLast();
    }
}
