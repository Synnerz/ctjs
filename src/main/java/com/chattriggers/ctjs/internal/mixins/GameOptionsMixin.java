package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.BoundKeyUpdater;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class GameOptionsMixin implements BoundKeyUpdater {
    @Unique
    private Options.FieldAccess fieldAccess;
//    @Unique
//    private Options visitor;
//
//    @Inject(method = "accept", at = @At("HEAD"))
//    private void captureVisitor(GameOptions.Visitor visitor, CallbackInfo ci) {
//        this.visitor = visitor;
//    }
//
//    @Override
//    public void ctjs_updateBoundKey(KeyBinding keyBinding) {
//        String string = keyBinding.getBoundKeyTranslationKey();
//        String string2 = visitor.visitString("key_" + keyBinding.getBoundKeyTranslationKey(), string);
//        if (!string.equals(string2)) {
//            keyBinding.setBoundKey(InputUtil.fromTranslationKey(string2));
//        }
//    }

    @Inject(method = "processOptions", at = @At("HEAD"))
    private void captureField(Options.FieldAccess fieldAccess, CallbackInfo ci) {
        this.fieldAccess = fieldAccess;
    }

    @Override
    public void ctjs_updateBoundKey(KeyMapping keyBinding) {
        String string = keyBinding.getTranslatedKeyMessage().getString();
        String string2 = fieldAccess.process("key_" + keyBinding.getTranslatedKeyMessage().getString(), string);
        if (!string.equals(string2)) {
            keyBinding.setKey(InputConstants.getKey(string2));
        }
    }
}
