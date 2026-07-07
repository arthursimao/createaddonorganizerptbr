package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Inject(
            method = "renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;IIF)V",
            at = @At("HEAD"),
            cancellable = true)
    private void createaddonorganizer$noDividerHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        if ((Object) this instanceof CreativeModeInventoryScreen
                && slot.getClass().getName().endsWith("CustomCreativeSlot")
                && !slot.hasItem()) {
            ci.cancel();
        }
    }
}
