package com.chattriggers.ctjs.internal.mixins;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(KeyMapping.class)
public interface KeyBindingAccessor {
    @Accessor
    InputConstants.Key getKey();

    @Accessor
    int getClickCount();

    @Mixin(KeyMapping.Category.class)
    interface Category {
        @Accessor("SORT_ORDER")
        static List<KeyMapping.Category> getCategoryList() { throw new IllegalStateException(); }
    }
}
