package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.engine.CTEvents;
import com.chattriggers.ctjs.internal.listeners.MouseListener;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonInfo;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow
    private MouseButtonInfo activeButton;

    @Inject(
        method = "onButton",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;gui:Lnet/minecraft/client/gui/Gui;",
            opcode = Opcodes.GETFIELD
        )
    )
    private void injectOnMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        MouseListener.onRawMouseInput(input.button(), action);
    }

    @Inject(
        method = "onScroll",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;options:Lnet/minecraft/client/Options;",
            opcode = Opcodes.GETFIELD
        )
    )
    private void injectOnMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MouseListener.onRawMouseScroll(vertical);
    }

    @Inject(
        method = "handleAccumulatedMovement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z"
        ),
        cancellable = true
    )
    private void injectOnGuiMouseDrag(
        CallbackInfo ci,
        @Local(ordinal = 0) double d,
        @Local(ordinal = 1) double e,
        @Local Screen screen,
        @Local(ordinal = 2) double f,
        @Local(ordinal = 3) double g)
    {
        if (screen != null) {
            CTEvents.GUI_MOUSE_DRAG.invoker().process(f, g, d, e, activeButton.button(), screen, ci);
        }
    }
}
