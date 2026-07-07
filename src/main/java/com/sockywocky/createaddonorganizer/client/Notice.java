package com.sockywocky.createaddonorganizer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public final class Notice {
    public static final int RED = 0xFF5555;
    public static final int GREEN = 0x55FF55;

    private static final long VISIBLE_MS = 700;
    private static final long FADE_MS = 300;
    private static final long TOTAL_MS = VISIBLE_MS + FADE_MS;

    private static Component message;
    private static int rgb;
    private static long shownAtMillis;

    private Notice() {}

    public static void show(Component message, int rgb) {
        Notice.message = message;
        Notice.rgb = rgb;
        Notice.shownAtMillis = System.currentTimeMillis();
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
    }

    public static void render(GuiGraphics g, Minecraft mc) {
        if (message == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - shownAtMillis;
        if (elapsed >= TOTAL_MS) {
            message = null;
            return;
        }
        float alpha = elapsed <= VISIBLE_MS ? 1f : 1f - (float) (elapsed - VISIBLE_MS) / FADE_MS;
        int argb = (Math.round(alpha * 255) << 24) | rgb;
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        g.drawCenteredString(mc.font, message, width / 2, height - 55, argb);
    }
}
