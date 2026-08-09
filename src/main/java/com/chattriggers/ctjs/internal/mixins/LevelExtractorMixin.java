package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.listeners.WorldListener;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {
    @Inject(method = "extract", at = @At("HEAD"))
    private void beforeRender(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci) {
        WorldListener.INSTANCE.triggerRenderStart(deltaTracker.getGameTimeDeltaTicks());
    }
}
