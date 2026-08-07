package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.inventory.Item;
import com.chattriggers.ctjs.api.message.TextComponent;
import com.chattriggers.ctjs.api.triggers.TriggerType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin extends Screen {
    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    private AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(
        method = "renderTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V"
        ),
        cancellable = true
    )
    private void injectDrawMouseoverTooltip(GuiGraphics context, int x, int y, CallbackInfo ci) {
        ItemStack stack = hoveredSlot.getItem();
        TriggerType.ITEM_TOOLTIP.triggerAll(
            getTooltipFromItem(Objects.requireNonNull(minecraft), stack)
                .stream()
                .map(TextComponent::new)
                .toList(),
            Item.fromMC(stack),
            ci
        );
    }

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V", at = @At("HEAD"), cancellable = true)
    private void injectOnMouseClick(Slot slot, int slotId, int button, ClickType actionType, CallbackInfo ci) {
        if (
            (slotId != -999 && actionType == ClickType.THROW) || // dropping item from slot
                (slotId == -999 && actionType == ClickType.PICKUP) // dropping by clicking outside inventory
        ) {
            TriggerType.DROP_ITEM.triggerAll(Item.fromMC(menu.getCarried()), button == 0, ci);
        }
    }
}
