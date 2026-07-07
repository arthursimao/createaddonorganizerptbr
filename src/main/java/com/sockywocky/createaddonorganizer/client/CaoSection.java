package com.sockywocky.createaddonorganizer.client;

import com.mojang.blaze3d.platform.Window;

import com.sockywocky.createaddonorganizer.Config;

import net.mcexpanded.fancytabsections.Section.Section;
import net.mcexpanded.fancytabsections.creativetab.ConglomerateOfItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record CaoSection(ResourceLocation id, Component title, int bannerColor, ResourceLocation texture,
        int textColor, ConglomerateOfItems items) implements Section {

    private static final int CONTENT_W = 160;
    private static final int CONTENT_H = 16;
    private static final int ROW_H = 18;
    private static final int BEVEL_DARK = 0xFF373737;
    private static final int BEVEL_WHITE = 0xFFFFFFFF;

    public CaoSection(ResourceLocation id, Component title, int bannerColor, int textColor, ConglomerateOfItems items) {
        this(id, title, bannerColor, null, textColor, items);
    }

    public CaoSection withBanner(int argb) {
        return new CaoSection(id, title, argb, null, textColor, items);
    }

    public CaoSection withTexture(ResourceLocation newTexture) {
        return new CaoSection(id, title, bannerColor, newTexture, textColor, items);
    }

    public CaoSection withTextColor(int argb) {
        return new CaoSection(id, title, bannerColor, texture, argb, items);
    }

    public CaoSection withTitle(Component newTitle) {
        return new CaoSection(id, newTitle, bannerColor, texture, textColor, items);
    }

    @Override
    public void render(GuiGraphics g, Font font, int topLeftX, int topLeftY) {
        int x1 = topLeftX + 1;
        int x2 = x1 + CONTENT_W;
        int contentTop = topLeftY + 1;
        int contentBottom = contentTop + CONTENT_H;

        g.fill(x1 - 1, topLeftY, x2, topLeftY + 1, BEVEL_DARK);
        g.fill(x1, topLeftY + ROW_H - 1, x2 + 1, topLeftY + ROW_H, BEVEL_WHITE);

        if (texture != null) {
            var anim = BannerAnimation.get(texture);
            float v = 0f;
            int texHeight = BannerTextures.HEIGHT;
            if (anim.isPresent()) {
                boolean hovered = isHoveredNow(x1, contentTop);
                int frame = BannerAnimation.currentFrame(texture, anim.get(), hovered);
                v = frame * BannerTextures.HEIGHT;
                texHeight = anim.get().frameCount() * BannerTextures.HEIGHT;
            }
            g.blit(texture, x1, contentTop, 0f, v, CONTENT_W, CONTENT_H, BannerTextures.WIDTH, texHeight);
        } else {
            g.fill(x1, contentTop, x2, contentBottom, bannerColor);
        }

        int textX = topLeftX + 6;
        int textY = topLeftY + 5;
        if (Config.tintedTextBox()) {
            int w = font.width(title);
            g.fill(textX - 4, textY - 3, textX + w + 3, textY + 9 + 2, Config.boxColorFor(id));
        }
        Integer secondary = Config.textSecondaryColorFor(id);
        if (secondary != null) {
            TwoToneText.draw(g, font, title, textX, textY, textColor, secondary);
        } else {
            g.drawString(font, title, textX, textY, textColor, true);
        }
    }

    private static boolean isHoveredNow(int x, int y) {
        Window window = Minecraft.getInstance().getWindow();
        double mouseX = Minecraft.getInstance().mouseHandler.xpos() / window.getGuiScale();
        double mouseY = Minecraft.getInstance().mouseHandler.ypos() / window.getGuiScale();
        return BannerAnimation.isHovering(x, y, CONTENT_W, CONTENT_H, mouseX, mouseY);
    }
}
