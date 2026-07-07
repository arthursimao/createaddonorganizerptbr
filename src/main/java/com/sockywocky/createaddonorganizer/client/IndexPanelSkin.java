package com.sockywocky.createaddonorganizer.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;

public interface IndexPanelSkin {

    record Rect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    record View(int sectionCount, List<ItemStack> icons, boolean expanded,
            float scroll, int selectedIndex) {}

    sealed interface Hit {
        record Entry(int sectionIndex) implements Hit {}
        record ScrollUp() implements Hit {}
        record ScrollDown() implements Hit {}

        record PanelBody() implements Hit {}
        record None() implements Hit {}
    }

    Hit hitTest(CreativeModeInventoryScreen screen, View view, double mouseX, double mouseY);

    boolean wheelOver(CreativeModeInventoryScreen screen, View view, double mouseX, double mouseY);

    float wheelStep();

    float maxScroll(CreativeModeInventoryScreen screen, int sectionCount);

    default float snap(float scroll) {
        return scroll;
    }

    void render(CreativeModeInventoryScreen screen, GuiGraphics gg, View view, int mouseX, int mouseY);
}
