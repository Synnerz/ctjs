package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.world.Scoreboard;
import com.chattriggers.ctjs.internal.engine.CTEvents;
import gg.essential.universal.UMatrixStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V", at = @At("HEAD"), cancellable = true)
    private void injectRenderScoreboard(GuiGraphics matrices, Objective objective, CallbackInfo ci) {
        if (!Scoreboard.getShouldRender())
            ci.cancel();
    }

    @Inject(
        method = "renderBossOverlay",
        at = @At("TAIL")
    )
    private void injectRenderOverlay(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        CTEvents.RENDER_OVERLAY.invoker().render(context, new UMatrixStack(context.pose()).toMC(), tickCounter.getGameTimeDeltaTicks());
    }
}
