package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccessor {
    @Accessor("scrollOffs")
    float getScrollOffs();

    @Accessor("scrollOffs")
    void setScrollOffs(float value);

    @Accessor("selectedTab")
    static CreativeModeTab getSelectedTab() {
        throw new AssertionError();
    }

    @Invoker("checkTabClicked")
    boolean invokeCheckTabClicked(CreativeModeTab tab, double relativeX, double relativeY);
}
