package com.chattriggers.ctjs.internal.mixins.sound;

import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(SoundManager.class)
public interface SoundManagerAccessor {
    @Accessor
    SoundEngine getSoundEngine();

    @Accessor
    Map<ResourceLocation, WeighedSoundEvents> getRegistry();

    @Accessor
    Map<ResourceLocation, Resource> getSoundCache();
}
