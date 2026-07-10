package com.sockywocky.createaddonorganizer.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.ResourceLocation;

public final class RefurbishedPanelSkin implements IndexPanelSkin {

    public static final RefurbishedPanelSkin INSTANCE = new RefurbishedPanelSkin();

    private static final ResourceLocation TAB_SELECTED = tex("refurbished_tab_selected");
    private static final ResourceLocation TAB_UNSELECTED = tex("refurbished_tab_unselected");
    private static final ResourceLocation ICONS = tex("refurbished_icons");
    private static final int ICONS_SIZE = 64;

    private static final ResourceLocation BTN_NORMAL = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BTN_DISABLED = ResourceLocation.withDefaultNamespace("widget/button_disabled");
    private static final ResourceLocation BTN_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted");

    private static final int TAB_W = 32;
    private static final int TAB_H = 26;
    private static final int PITCH = 29;
    private static final int VISIBLE = 4;
    private static final int BTN = 20;

    private RefurbishedPanelSkin() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath("createaddonorganizer", "textures/gui/index_panel/" + name + ".png");
    }

    private static Rect tabRect(CreativeModeInventoryScreen s, int visibleRow) {
        return new Rect(s.getGuiLeft() - 28, s.getGuiTop() + 11 + PITCH * visibleRow, TAB_W, TAB_H);
    }

    private static Rect upRect(CreativeModeInventoryScreen s) {
        return new Rect(s.getGuiLeft() - 22, s.getGuiTop() - 12, BTN, BTN);
    }

    private static Rect downRect(CreativeModeInventoryScreen s) {
        return new Rect(s.getGuiLeft() - 22, s.getGuiTop() + 127, BTN, BTN);
    }

    @Override
    public Hit hitTest(CreativeModeInventoryScreen screen, View view, double mouseX, double mouseY) {
        if (!view.expanded()) {
            return new Hit.None();
        }
        int count = view.sectionCount();
        if (count > VISIBLE) {
            if (upRect(screen).contains(mouseX, mouseY)) {
                return view.scroll() > 0 ? new Hit.ScrollUp() : new Hit.PanelBody();
            }
            if (downRect(screen).contains(mouseX, mouseY)) {
                return view.scroll() < maxScroll(screen, count) ? new Hit.ScrollDown() : new Hit.PanelBody();
            }
        }
        int base = (int) view.scroll();
        int visible = Math.min(VISIBLE, count);
        for (int v = 0; v < visible; v++) {
            if (tabRect(screen, v).contains(mouseX, mouseY)) {
                return new Hit.Entry(base + v);
            }
        }
        return new Hit.None();
    }

    @Override
    public boolean wheelOver(CreativeModeInventoryScreen screen, View view, double mouseX, double mouseY) {

        int gl = screen.getGuiLeft();
        int gt = screen.getGuiTop();
        return view.expanded()
                && mouseX >= gl - 28 && mouseX < gl
                && mouseY >= gt + 29 && mouseY < gt + 142;
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
            int count = view.sectionCount();
            int base = (int) view.scroll();
            int visible = Math.min(VISIBLE, count);
            for (int v = 0; v < visible; v++) {
                int idx = base + v;
                Rect tab = tabRect(screen, v);
                ResourceLocation sprite = idx == view.selectedIndex() ? TAB_SELECTED : TAB_UNSELECTED;
                gg.blit(sprite, tab.x(), tab.y(), 0f, 0f, TAB_W, TAB_H, TAB_W, TAB_H);
                SafeIcon.render(gg, view.icons().get(idx), tab.x() + 8, tab.y() + 5);
            }
            if (count > VISIBLE) {
                drawScrollButton(gg, upRect(screen), 0f, view.scroll() > 0, mouseX, mouseY);
                drawScrollButton(gg, downRect(screen), 10f,
                        view.scroll() < maxScroll(screen, count), mouseX, mouseY);
            }
        }
        RenderSystem.disableBlend();
    }

    private static void drawButton(GuiGraphics gg, Rect r, boolean active, boolean hovered) {
        ResourceLocation sprite = !active ? BTN_DISABLED : hovered ? BTN_HIGHLIGHTED : BTN_NORMAL;
        gg.blitSprite(sprite, r.x(), r.y(), r.w(), r.h());
    }

    private static void drawScrollButton(GuiGraphics gg, Rect r, float arrowU, boolean active,
            int mouseX, int mouseY) {
        drawButton(gg, r, active, r.contains(mouseX, mouseY));
        if (!active) {
            gg.setColor(1f, 1f, 1f, 0.5f);
        }
        gg.blit(ICONS, r.x() + 5, r.y() + 5, arrowU, 0f, 10, 10, ICONS_SIZE, ICONS_SIZE);
        if (!active) {
            gg.setColor(1f, 1f, 1f, 1f);
        }
    }
}
