package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.platform.NativeImage;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public final class BannerAnimation {
    private static final int DEFAULT_AUTO_FRAME_TICKS = 2;

    private static final Map<ResourceLocation, Optional<AnimInfo>> CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Integer> FRAME_COUNT_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, FrameState> FRAME_STATE = new HashMap<>();

    private BannerAnimation() {}

    public record AnimInfo(int frameCount, int frameTicks, boolean cycles) {}

    private static final class FrameState {
        int frame;
        long lastAdvanceMillis;
    }

    public static Optional<AnimInfo> get(ResourceLocation texture) {
        return CACHE.computeIfAbsent(texture, BannerAnimation::load);
    }

    public static Optional<AnimInfo> preview(ResourceLocation texture, boolean animated, int frameTicks) {
        int count = frameCount(texture);
        return count > 1 ? Optional.of(new AnimInfo(count, Math.max(1, frameTicks), animated)) : Optional.empty();
    }

    public static void invalidate(ResourceLocation texture) {
        CACHE.remove(texture);
        FRAME_STATE.remove(texture);
    }

    public static int currentFrame(ResourceLocation texture, AnimInfo info, boolean hovered) {
        if (!info.cycles()) {
            return 0;
        }
        FrameState state = FRAME_STATE.computeIfAbsent(texture, t -> new FrameState());
        if (!hovered) {
            state.lastAdvanceMillis = 0L;
            return state.frame;
        }
        long now = System.currentTimeMillis();
        if (state.lastAdvanceMillis == 0L) {
            state.lastAdvanceMillis = now;
            return state.frame;
        }
        long frameDurationMillis = info.frameTicks() * 50L;
        long elapsed = now - state.lastAdvanceMillis;
        if (elapsed >= frameDurationMillis) {
            long steps = elapsed / frameDurationMillis;
            state.frame = (int) ((state.frame + steps) % info.frameCount());
            state.lastAdvanceMillis += steps * frameDurationMillis;
        }
        return state.frame;
    }

    public static boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static boolean isAnimatable(ResourceLocation texture) {
        return frameCount(texture) > 1;
    }

    private static Optional<AnimInfo> load(ResourceLocation texture) {
        int count = frameCount(texture);
        if (count <= 1) {
            return Optional.empty();
        }
        Integer declared = Config.animatedFrameTicks(texture);
        if (declared != null) {
            return Optional.of(new AnimInfo(count, Math.max(1, declared), true));
        }
        Optional<Integer> mcmetaTicks = mcmetaFrameTicks(texture);
        if (mcmetaTicks.isPresent()) {
            return Optional.of(new AnimInfo(count, mcmetaTicks.get(), true));
        }
        return Optional.of(new AnimInfo(count, DEFAULT_AUTO_FRAME_TICKS, true));
    }

    private static int frameCount(ResourceLocation texture) {
        return FRAME_COUNT_CACHE.computeIfAbsent(texture, BannerAnimation::readFrameCount);
    }

    private static int readFrameCount(ResourceLocation texture) {
        Optional<Integer> uploadedHeight = BannerTextures.frameHeightFor(texture);
        if (uploadedHeight.isPresent()) {
            return Math.max(1, uploadedHeight.get() / BannerTextures.HEIGHT);
        }
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            return 1;
        }
        try (NativeImage image = NativeImage.read(resource.get().open())) {
            return Math.max(1, image.getHeight() / BannerTextures.HEIGHT);
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to read banner image dimensions for {}", texture, e);
            return 1;
        }
    }

    private static Optional<Integer> mcmetaFrameTicks(ResourceLocation texture) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            return Optional.empty();
        }
        try {
            Optional<AnimationMetadataSection> anim = resource.get().metadata().getSection(AnimationMetadataSection.SERIALIZER);
            return anim.map(a -> Math.max(1, a.getDefaultFrameTime()));
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to read animation metadata for {}", texture, e);
            return Optional.empty();
        }
    }
}
