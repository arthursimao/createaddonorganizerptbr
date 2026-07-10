package com.sockywocky.createaddonorganizer.client;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SafeIcon {
    private static final Set<Object> BROKEN = ConcurrentHashMap.newKeySet();
    private static final ItemStack FALLBACK = new ItemStack(Items.BARRIER);

    private SafeIcon() {}

    public static ItemStack of(CreativeModeTab tab) {
        if (tab == null || BROKEN.contains(tab)) {
            return ItemStack.EMPTY;
        }
        try {
            return tab.getIconItem();
        } catch (Throwable t) {
            if (BROKEN.add(tab)) {
                createaddonorganizer.LOGGER.warn("[CAO] creative tab {} threw while resolving its icon; hiding it",
                        BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab), t);
            }
            return ItemStack.EMPTY;
        }
    }

    public static void render(GuiGraphics g, ItemStack icon, int x, int y) {
        if (icon.isEmpty()) {
            return;
        }
        if (BROKEN.contains(icon.getItem())) {
            g.renderItem(FALLBACK, x, y);
            return;
        }
        try {
            g.renderItem(icon, x, y);
        } catch (Throwable t) {
            if (BROKEN.add(icon.getItem())) {
                createaddonorganizer.LOGGER.warn("[CAO] item icon {} threw while rendering; using fallback",
                        BuiltInRegistries.ITEM.getKey(icon.getItem()), t);
            }
            g.renderItem(FALLBACK, x, y);
        }
    }
}
