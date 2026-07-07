package com.sockywocky.createaddonorganizer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

public final class ClassicPanelSkin implements IndexPanelSkin {

    public static final ClassicPanelSkin VANILLA = new ClassicPanelSkin(true);
    public static final ClassicPanelSkin DARK = new ClassicPanelSkin(false);

    private static final int ICON = 18;
    private static final int GAP = 2;
    private static final int PANEL_W = 24;

    private static final int DARK_BG = 0xD0101010;
    private static final int DARK_BORDER = 0xFF373737;
    private static final int DARK_HOVER = 0x50FFFFFF;

    private static final int VANILLA_PANEL_BG = 0xFFC6C6C6;
    private static final int VANILLA_OUTER_BORDER = 0xFF000000;
    private static final int VANILLA_RAISED_HI = 0xFFFFFFFF;
    private static final int VANILLA_RAISED_SHADOW = 0xFF555555;
    private static final int VANILLA_SLOT_BG = 0xFF8B8B8B;
    private static final int VANILLA_SLOT_SHADOW = 0xFF373737;
    private static final int VANILLA_SLOT_HI = 0xFFFFFFFF;
    private static final int VANILLA_HOVER = 0x60FFFFFF;

    private final boolean vanilla;

    private ClassicPanelSkin(boolean vanilla) {
        this.vanilla = vanilla;
    }

    private static Rect panelRect(CreativeModeInventoryScreen s, int sectionCount) {
        int py = s.getGuiTop() + 4;
        int maxH = s.getGuiTop() + s.getYSize() - 4 - py;
        int rows = Math.min(sectionCount, Math.max(0, (maxH - 6 + GAP) / (ICON + GAP)));
        int h = rows <= 0 ? 0 : rows * (ICON + GAP) - GAP + 6;
        return new Rect(s.getGuiLeft() - 2 - PANEL_W, py, PANEL_W, h);
    }

    private static Rect entryRect(Rect panel, int index, float scroll) {
        int ix = panel.x() + (PANEL_W - ICON) / 2;
        int iy = panel.y() + 3 + index * (ICON + GAP) - (int) scroll;
        return new Rect(ix, iy, ICON, ICON);
    }

    @Override
    public Hit hitTest(CreativeModeInventoryScreen screen, View view, double mouseX, double mouseY) {
        Rect panel = panelRect(screen, view.sectionCount());
        if (!view.expanded() || panel.h() <= ICON || !panel.contains(mouseX, mouseY)) {
            return new Hit.None();
        }
        int i = (int) ((mouseY - (panel.y() + 3) + view.scroll()) / (ICON + GAP));
        if (i >= 0 && i < view.sectionCount() && entryRect(panel, i, view.scroll()).contains(mouseX, mouseY)) {
            return new Hit.Entry(i);
        }
        return new Hit.PanelBody();
    }

    @Override
    public boolean wheelOver(CreativeModeInventoryScreen screen, View view, double mouseX, double mouseY) {
        return view.expanded() && panelRect(screen, view.sectionCount()).contains(mouseX, mouseY);
    }

    @Override
    public float wheelStep() {
        return 20f;
    }

    @Override
    public float maxScroll(CreativeModeInventoryScreen screen, int sectionCount) {
        int contentH = sectionCount * (ICON + GAP) - GAP + 6;
        return Math.max(0, contentH - panelRect(screen, sectionCount).h());
    }

    @Override
    public void render(CreativeModeInventoryScreen screen, GuiGraphics gg, View view, int mouseX, int mouseY) {
        if (!view.expanded()) {
            return;
        }
        Rect panel = panelRect(screen, view.sectionCount());
        int px = panel.x();
        int py = panel.y();
        int pBottom = py + panel.h();
        if (panel.h() <= ICON) {
            return;
        }

        if (vanilla) {
            drawRaisedBox(gg, px, py, PANEL_W, panel.h());
        } else {
            gg.fill(px, py, px + PANEL_W, pBottom, DARK_BG);
            outline(gg, px, py, PANEL_W, panel.h(), DARK_BORDER);
        }

        gg.enableScissor(px + 2, py + 2, px + PANEL_W - 2, pBottom - 2);
        for (int i = 0; i < view.sectionCount(); i++) {
            Rect entry = entryRect(panel, i, view.scroll());
            int ix = entry.x();
            int iy = entry.y();
            if (iy + ICON < py || iy > pBottom) {
                continue;
            }
            boolean slotHovered = entry.contains(mouseX, mouseY) && panel.contains(mouseX, mouseY);
            if (vanilla) {
                drawSunkenSlot(gg, ix, iy, ICON, ICON);
                if (slotHovered) {
                    gg.fill(ix + 1, iy + 1, ix + ICON - 1, iy + ICON - 1, VANILLA_HOVER);
                }
            } else if (slotHovered) {
                gg.fill(ix, iy, ix + ICON, iy + ICON, DARK_HOVER);
            }
            gg.renderItem(view.icons().get(i), ix + 1, iy + 1);
        }
        gg.disableScissor();
    }

    private static void outline(GuiGraphics gg, int x, int y, int w, int h, int color) {
        gg.fill(x, y, x + w, y + 1, color);
        gg.fill(x, y + h - 1, x + w, y + h, color);
        gg.fill(x, y, x + 1, y + h, color);
        gg.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static void drawRaisedBox(GuiGraphics gg, int x, int y, int w, int h) {
        gg.fill(x, y, x + w, y + h, VANILLA_OUTER_BORDER);
        gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, VANILLA_PANEL_BG);
        gg.fill(x + 1, y + 1, x + w - 1, y + 2, VANILLA_RAISED_HI);
        gg.fill(x + 1, y + 1, x + 2, y + h - 1, VANILLA_RAISED_HI);
        gg.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, VANILLA_RAISED_SHADOW);
        gg.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, VANILLA_RAISED_SHADOW);
    }

    private static void drawSunkenSlot(GuiGraphics gg, int x, int y, int w, int h) {
        gg.fill(x, y, x + w, y + h, VANILLA_SLOT_BG);
        gg.fill(x, y, x + w, y + 1, VANILLA_SLOT_SHADOW);
        gg.fill(x, y, x + 1, y + h, VANILLA_SLOT_SHADOW);
        gg.fill(x, y + h - 1, x + w, y + h, VANILLA_SLOT_HI);
        gg.fill(x + w - 1, y, x + w, y + h, VANILLA_SLOT_HI);
    }
}
