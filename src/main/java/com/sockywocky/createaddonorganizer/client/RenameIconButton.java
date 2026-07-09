package com.sockywocky.createaddonorganizer.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class RenameIconButton extends Button {
    private static final ResourceLocation CONFIRM_ICON = ResourceLocation.withDefaultNamespace("container/beacon/confirm");
    private static final ResourceLocation CANCEL_ICON = ResourceLocation.withDefaultNamespace("container/beacon/cancel");
    private static final int SPRITE_SIZE = 18;

    private final ResourceLocation icon;
    private final int glyphU;
    private final int glyphV;
    private final int glyphW;
    private final int glyphH;

    RenameIconButton(boolean check, Component tooltip, OnPress onPress) {
        super(0, 0, 20, 20, Component.empty(), onPress, DEFAULT_NARRATION);
        this.icon = check ? CONFIRM_ICON : CANCEL_ICON;
        this.glyphU = check ? 1 : 2;
        this.glyphV = check ? 4 : 3;
        this.glyphW = check ? 14 : 13;
        this.glyphH = check ? 12 : 13;
        setTooltip(Tooltip.create(tooltip));
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(g, mouseX, mouseY, partialTick);
        int ix = getX() + (getWidth() - glyphW) / 2;
        int iy = getY() + (getHeight() - glyphH) / 2;

        RenderSystem.enableBlend();
        g.setColor(1f, 1f, 1f, this.alpha);
        g.blitSprite(icon, SPRITE_SIZE, SPRITE_SIZE, glyphU, glyphV, ix, iy, glyphW, glyphH);
        g.setColor(1f, 1f, 1f, 1f);
    }
}
