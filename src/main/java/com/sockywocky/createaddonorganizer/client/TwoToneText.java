package com.sockywocky.createaddonorganizer.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class TwoToneText {
    private TwoToneText() {}

    public static void draw(GuiGraphics g, Font font, Component text, int x, int y, int primaryArgb, int secondaryArgb) {
        g.drawString(font, text, x, y, primaryArgb, true);
        int w = font.width(text);
        g.pose().pushPose();
        g.pose().translate(0, 0, 1);
        g.enableScissor(x, y + 5, x + w, y + 9);
        g.drawString(font, text, x, y, secondaryArgb, false);
        g.disableScissor();
        g.pose().popPose();
    }
}
