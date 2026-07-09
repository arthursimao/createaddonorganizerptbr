package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import com.mojang.blaze3d.platform.NativeImage;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.fml.loading.FMLPaths;

public final class BannerTextures {
    public static final int WIDTH = 160;
    public static final int HEIGHT = 17;

    public static void blitCropped(net.minecraft.client.gui.GuiGraphics g, ResourceLocation tex,
            int x, int y, int w, int h, int textureTotalHeight) {
        int u = Math.max(0, (WIDTH - w) / 2);
        g.blit(tex, x, y, w, h, u, 0.0F, w, h, WIDTH, textureTotalHeight);
    }

    private static final Path BANNERS_DIR = FMLPaths.CONFIGDIR.get().resolve("createaddonorganizer/banners");

    private static final Map<String, ResourceLocation> FILE_REGISTERED = new HashMap<>();
    private static final Map<ResourceLocation, Integer> FILE_HEIGHTS = new HashMap<>();

    private BannerTextures() {}

    public static ResourceLocation resolve(String ref) {
        if (ref == null) {
            return null;
        }
        if (ref.startsWith("res:")) {
            return ResourceLocation.tryParse(ref.substring(4));
        }
        if (ref.startsWith("file:")) {
            String fileName = ref.substring(5);
            ResourceLocation cached = FILE_REGISTERED.get(fileName);
            return cached != null ? cached : loadFileAndCache(BANNERS_DIR.resolve(fileName), fileName);
        }
        return ResourceLocation.tryParse(ref);
    }

    public static List<String> gallery() {
        Map<ResourceLocation, Resource> found = Minecraft.getInstance().getResourceManager()
                .listResources("textures/banner", p -> p.getPath().endsWith(".png"));
        List<String> bundled = new ArrayList<>();
        for (ResourceLocation tex : found.keySet()) {
            bundled.add(resRef(tex));
        }
        bundled.sort(String.CASE_INSENSITIVE_ORDER);

        List<String> uploads = new ArrayList<>();
        if (Files.isDirectory(BANNERS_DIR)) {
            try (var files = Files.list(BANNERS_DIR)) {
                files.filter(p -> p.getFileName().toString().endsWith(".png"))
                        .forEach(p -> uploads.add("file:" + p.getFileName()));
            } catch (IOException e) {
                createaddonorganizer.LOGGER.warn("[CAO] failed to list uploaded banners", e);
            }
        }
        uploads.sort(String.CASE_INSENSITIVE_ORDER);

        List<String> out = new ArrayList<>(bundled.size() + uploads.size());
        out.addAll(bundled);
        out.addAll(uploads);
        return out;
    }

    public static String resRef(ResourceLocation texture) {
        return "res:" + texture;
    }

    public static Optional<Integer> frameHeightFor(ResourceLocation texture) {
        return Optional.ofNullable(FILE_HEIGHTS.get(texture));
    }

    public static Optional<Path> chooseFile() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();
            String result = TinyFileDialogs.tinyfd_openFileDialog(
                    "Choose a banner PNG (160 wide; a multiple of 17 tall for animation)", "", filters, "PNG image (*.png)", false);
            if (result == null || result.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Path.of(result));
        } catch (Throwable t) {

            createaddonorganizer.LOGGER.warn("[CAO] native file dialog unavailable", t);
            return Optional.empty();
        }
    }

    public static void deleteFile(String ref) {
        if (ref == null || !ref.startsWith("file:")) {
            return;
        }
        String fileName = ref.substring(5);
        ResourceLocation registered = FILE_REGISTERED.remove(fileName);
        if (registered != null) {
            Minecraft.getInstance().getTextureManager().release(registered);
            FILE_HEIGHTS.remove(registered);
        }
        try {
            Files.deleteIfExists(BANNERS_DIR.resolve(fileName));
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to delete banner image {}", fileName, e);
        }
    }

    public static String importFile(Path source) throws IOException {
        Files.createDirectories(BANNERS_DIR);
        String fileName = dedupedName(sanitizeStem(source.getFileName().toString()));
        Path dst = BANNERS_DIR.resolve(fileName);
        Files.copy(source, dst, StandardCopyOption.REPLACE_EXISTING);
        loadFileAndCache(dst, fileName);
        return "file:" + fileName;
    }

    private static ResourceLocation loadFileAndCache(Path path, String fileName) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID,
                "custom_banner/" + sanitizeStem(fileName));
        try (InputStream in = Files.newInputStream(path)) {
            NativeImage resized = resizeForImport(NativeImage.read(in));
            FILE_HEIGHTS.put(rl, resized.getHeight());
            Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(resized));
            FILE_REGISTERED.put(fileName, rl);
            return rl;
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to load banner image {}", path, e);
            return null;
        }
    }

    private static NativeImage resizeForImport(NativeImage src) {
        int targetHeight = (src.getHeight() % HEIGHT == 0 && src.getHeight() > 0) ? src.getHeight() : HEIGHT;
        if (src.getWidth() == WIDTH && src.getHeight() == targetHeight) {
            return src;
        }
        NativeImage dst = new NativeImage(WIDTH, targetHeight, false);
        for (int y = 0; y < targetHeight; y++) {
            int sy = y * src.getHeight() / targetHeight;
            for (int x = 0; x < WIDTH; x++) {
                int sx = x * src.getWidth() / WIDTH;
                dst.setPixelRGBA(x, y, src.getPixelRGBA(sx, sy));
            }
        }
        src.close();
        return dst;
    }

    private static String sanitizeStem(String rawFileName) {
        String stem = rawFileName.replaceFirst("(?i)\\.png$", "");
        return stem.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }

    private static String dedupedName(String stem) {
        String name = stem + ".png";
        int suffix = 2;
        while (Files.exists(BANNERS_DIR.resolve(name))) {
            name = stem + "_" + suffix + ".png";
            suffix++;
        }
        return name;
    }
}
