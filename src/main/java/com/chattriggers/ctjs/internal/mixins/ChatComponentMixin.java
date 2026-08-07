package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.message.ChatLib;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @Final
    @Shadow
    private List<GuiMessage> allMessages;

    @Inject(method = "clearMessages", at = @At("TAIL"))
    private void injectClear(boolean clearHistory, CallbackInfo ci) {
        ChatLib.INSTANCE.onChatHudClearChat$ctjs();
    }

    // TODO: is it this or addVisibleMessage
    @Inject(
        method = "addMessageToQueue(Lnet/minecraft/client/GuiMessage;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;remove(I)Ljava/lang/Object;",
            shift = At.Shift.BEFORE
        )
    )
    private void injectMessageRemovedForChatLimit(GuiMessage message, CallbackInfo ci) {
        ChatLib.INSTANCE.onChatHudLineRemoved$ctjs(allMessages.getLast());
    }

    // Note: ChatHudLine objects are also removed in queueForRemoval, however those are signature based.
    //       The Message objects that CT sends will always create ChatHudLine objects with null signatures,
    //       so objects removed in that method will never be in the ChatLib.chatLineIds map
}
