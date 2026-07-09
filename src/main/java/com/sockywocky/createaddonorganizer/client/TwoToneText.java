package com.sockywocky.createaddonorganizer.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class TwoToneText {
    private TwoToneText() {}

    private static Integer targetHeight = null;
    private static Double targetScale = null;

    public static void setRenderTarget(int framebufferHeight, double pixelsPerUnit) {
        targetHeight = framebufferHeight;
        targetScale = pixelsPerUnit;
    }

    public static void clearRenderTarget() {
        targetHeight = null;
        targetScale = null;
    }

    public static void draw(GuiGraphics g, Font font, Component text, int x, int y, int primaryArgb, int secondaryArgb) {
        g.drawString(font, text, x, y, primaryArgb, true);
        int w = font.width(text);
        g.pose().pushPose();
        g.pose().translate(0, 0, 1);
        if (targetHeight != null) {
            enableTargetScissor(x, y + 5, x + w, y + 9);
            g.drawString(font, text, x, y, secondaryArgb, false);
            RenderSystem.disableScissor();
        } else {
            g.enableScissor(x, y + 5, x + w, y + 9);
            g.drawString(font, text, x, y, secondaryArgb, false);
            g.disableScissor();
        }
        g.pose().popPose();
    }

    private static void enableTargetScissor(int x1, int y1, int x2, int y2) {
        double scale = targetScale;
        double px = x1 * scale;
        double py = targetHeight - y2 * scale;
        double pw = (x2 - x1) * scale;
        double ph = (y2 - y1) * scale;
        RenderSystem.enableScissor((int) px, (int) py, Math.max(0, (int) pw), Math.max(0, (int) ph));
    }
}
