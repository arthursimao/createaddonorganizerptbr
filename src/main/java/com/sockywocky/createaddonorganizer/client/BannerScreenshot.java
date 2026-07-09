package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class BannerScreenshot {
    private static final int SCALE = 8;

    private BannerScreenshot() {}

    private static Path folder(Minecraft mc) {
        return mc.gameDirectory.toPath().resolve("screenshots").resolve("banner screenshots");
    }

    public static void openFolder() {
        try {
            Path dir = folder(Minecraft.getInstance());
            Files.createDirectories(dir);
            Util.getPlatform().openPath(dir);
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to open banner screenshots folder", e);
            Notice.show(Component.translatable("createaddonorganizer.banner.screenshot.folderFailed"), Notice.RED);
        }
    }

    public static void capture(int contentWidth, int contentHeight, String label, Consumer<GuiGraphics> draw) {
        Minecraft mc = Minecraft.getInstance();
        int width = contentWidth * SCALE;
        int height = contentHeight * SCALE;

        TextureTarget target = new TextureTarget(width, height, true, Minecraft.ON_OSX);

        RenderSystem.backupProjectionMatrix();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();

        NativeImage image;
        try {
            target.bindWrite(true);
            Matrix4f projection = new Matrix4f().setOrtho(0.0F, width, height, 0.0F, 1000.0F, 3000.0F);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);
            modelView.translation(0.0F, 0.0F, -2000.0F);
            RenderSystem.applyModelViewMatrix();

            GuiGraphics g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            g.pose().pushPose();
            g.pose().scale(SCALE, SCALE, SCALE);
            TwoToneText.setRenderTarget(height, SCALE);
            try {
                draw.accept(g);
            } finally {
                TwoToneText.clearRenderTarget();
            }
            g.pose().popPose();
            g.flush();

            target.unbindWrite();
            image = Screenshot.takeScreenshot(target);
        } catch (Exception e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to render banner screenshot", e);
            Notice.show(Component.translatable("createaddonorganizer.banner.screenshot.failed"), Notice.RED);
            return;
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            mc.getMainRenderTarget().bindWrite(true);
            target.destroyBuffers();
        }

        try {
            Path dir = folder(mc);
            Files.createDirectories(dir);
            Path file = dedupedFile(dir, sanitize(label) + "_banner");
            image.writeToFile(file);
            mc.keyboardHandler.setClipboard(file.toAbsolutePath().toString());
            Notice.show(Component.translatable("createaddonorganizer.banner.screenshot.success"), Notice.GREEN);
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to save banner screenshot", e);
            Notice.show(Component.translatable("createaddonorganizer.banner.screenshot.failed"), Notice.RED);
        } finally {
            image.close();
        }
    }

    private static Path dedupedFile(Path dir, String stem) {
        Path file = dir.resolve(stem + ".png");
        int suffix = 2;
        while (Files.exists(file)) {
            file = dir.resolve(stem + "_" + suffix + ".png");
            suffix++;
        }
        return file;
    }

    private static String sanitize(String raw) {
        String stem = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return stem.isEmpty() ? "banner" : stem;
    }
}
