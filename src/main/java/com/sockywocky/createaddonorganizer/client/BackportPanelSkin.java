package com.sockywocky.createaddonorganizer.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.ResourceLocation;

public final class BackportPanelSkin implements IndexPanelSkin {

    public static final BackportPanelSkin INSTANCE = new BackportPanelSkin();

    private static final ResourceLocation ATLAS = tex("backport_interface");
    private static final int ATLAS_SIZE = 256;

    private static final int PANEL_W = 30;
    private static final int PANEL_H = 120;
    private static final int ICON = 16;
    private static final int PITCH = 18;
    private static final int VISIBLE = 5;
    private static final int ARROW_W = 18;
    private static final int ARROW_H = 11;

    private BackportPanelSkin() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath("createaddonorganizer", "textures/gui/index_panel/" + name + ".png");
    }

    private static Rect panelRect(CreativeModeInventoryScreen s) {
        return new Rect(s.getGuiLeft() - 30, s.getGuiTop() + 2, PANEL_W, PANEL_H);
    }

    private static Rect iconRect(CreativeModeInventoryScreen s, int visibleRow) {
        return new Rect(s.getGuiLeft() - 23, s.getGuiTop() + 18 + PITCH * visibleRow, ICON, ICON);
    }

    private static Rect upRect(CreativeModeInventoryScreen s) {
        return new Rect(s.getGuiLeft() - 24, s.getGuiTop() + 6, ARROW_W, ARROW_H);
    }

    private static Rect downRect(CreativeModeInventoryScreen s) {
        return new Rect(s.getGuiLeft() - 24, s.getGuiTop() + 108, ARROW_W, ARROW_H);
    }

    @Override
    public Hit hitTest(CreativeModeInventoryScreen screen, View view, double mouseX, double mouseY) {
        if (!view.expanded() || !panelRect(screen).contains(mouseX, mouseY)) {
            return new Hit.None();
        }
        int count = view.sectionCount();
        if (count > VISIBLE) {
            if (view.scroll() > 0 && upRect(screen).contains(mouseX, mouseY)) {
                return new Hit.ScrollUp();
            }
            if (view.scroll() < maxScroll(screen, count) && downRect(screen).contains(mouseX, mouseY)) {
                return new Hit.ScrollDown();
            }
        }
        int base = (int) view.scroll();
        int visible = Math.min(VISIBLE, count);
        for (int v = 0; v < visible; v++) {
            if (iconRect(screen, v).contains(mouseX, mouseY)) {
                return new Hit.Entry(base + v);
            }
        }
        return new Hit.PanelBody();
    }

    @Override
    public boolean wheelOver(CreativeModeInventoryScreen screen, View view, double mouseX, double mouseY) {
        return view.expanded() && panelRect(screen).contains(mouseX, mouseY);
    }

    @Override
    public float wheelStep() {
        return 1f;
    }

    @Override
    public float maxScroll(CreativeModeInventoryScreen screen, int sectionCount) {
        return Math.max(0, sectionCount - VISIBLE);
    }

    @Override
    public float snap(float scroll) {
        return Math.round(scroll);
    }

    @Override
    public void render(CreativeModeInventoryScreen screen, GuiGraphics gg, View view, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (view.expanded()) {
            Rect panel = panelRect(screen);
            gg.blit(ATLAS, panel.x(), panel.y(), 0f, 0f, PANEL_W, PANEL_H, ATLAS_SIZE, ATLAS_SIZE);

            int count = view.sectionCount();
            int base = (int) view.scroll();
            int visible = Math.min(VISIBLE, count);
            for (int v = 0; v < visible; v++) {
                int idx = base + v;
                Rect icon = iconRect(screen, v);
                boolean selected = idx == view.selectedIndex();
                if (selected) {
                    gg.blit(ATLAS, icon.x() - 7, icon.y() - 1, 36f, 24f, 30, 19, ATLAS_SIZE, ATLAS_SIZE);
                }
                SafeIcon.render(gg, view.icons().get(idx), icon.x(), icon.y());
                if (!selected && icon.contains(mouseX, mouseY)) {
                    RenderSystem.enableBlend();
                    gg.blit(ATLAS, icon.x(), icon.y(), 32f, 44f, ICON, ICON, ATLAS_SIZE, ATLAS_SIZE);
                }
            }
            if (count > VISIBLE) {
                if (view.scroll() > 0) {
                    Rect up = upRect(screen);
                    float v = up.contains(mouseX, mouseY) ? 12f : 0f;
                    gg.blit(ATLAS, up.x(), up.y(), 32f, v, ARROW_W, ARROW_H, ATLAS_SIZE, ATLAS_SIZE);
                }
                if (view.scroll() < maxScroll(screen, count)) {
                    Rect down = downRect(screen);
                    float v = down.contains(mouseX, mouseY) ? 12f : 0f;
                    gg.blit(ATLAS, down.x(), down.y(), 52f, v, ARROW_W, ARROW_H, ATLAS_SIZE, ATLAS_SIZE);
                }
            }
        }
        RenderSystem.disableBlend();
    }
}
