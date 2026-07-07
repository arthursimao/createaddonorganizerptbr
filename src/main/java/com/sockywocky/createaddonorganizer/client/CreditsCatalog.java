package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class CreditsCatalog {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation MANIFEST =
            ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID, "credits/credits.json");
    private static final int DEFAULT_NAME_COLOR = 0xFFFFFFFF;

    private CreditsCatalog() {}

    public record Contributor(String name, List<String> banners, String color) {}

    public record Entry(boolean header, String label, int nameColor, ResourceLocation texture) {}

    public static List<Entry> rows() {
        List<Entry> out = new ArrayList<>();
        for (Contributor contributor : loadContributors()) {
            List<ResourceLocation> textures = new ArrayList<>();
            for (String filename : contributor.banners()) {
                ResourceLocation texture = resolveBanner(contributor.name(), filename);
                if (texture != null) {
                    textures.add(texture);
                }
            }
            if (textures.isEmpty()) {
                continue;
            }
            int color = nameColor(contributor);
            out.add(new Entry(true, contributor.name(), color, null));
            for (ResourceLocation texture : textures) {
                out.add(new Entry(false, null, color, texture));
            }
        }
        return out;
    }

    private static int nameColor(Contributor contributor) {
        if (contributor.color() == null) {
            return DEFAULT_NAME_COLOR;
        }
        Integer parsed = Config.parseColor(contributor.color());
        return parsed != null ? parsed : DEFAULT_NAME_COLOR;
    }

    private static List<Contributor> loadContributors() {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(MANIFEST);
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            Contributor[] parsed = GSON.fromJson(reader, Contributor[].class);
            return parsed != null ? List.of(parsed) : List.of();
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to load credits manifest", e);
            return List.of();
        } catch (RuntimeException e) {
            createaddonorganizer.LOGGER.warn("[CAO] malformed credits manifest", e);
            return List.of();
        }
    }

    private static ResourceLocation resolveBanner(String contributorName, String filename) {
        String ref = "res:" + createaddonorganizer.MODID + ":textures/banner/" + filename;
        ResourceLocation texture = BannerTextures.resolve(ref);
        if (texture == null || Minecraft.getInstance().getResourceManager().getResource(texture).isEmpty()) {
            createaddonorganizer.LOGGER.warn("[CAO] credits manifest entry for '{}' references missing banner texture '{}'",
                    contributorName, filename);
            return null;
        }
        return texture;
    }
}
