package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public interface ItemPickerMenuAccessor {
    @Invoker("scrollTo")
    void invokeScrollTo(float scrollPercentage);
}
