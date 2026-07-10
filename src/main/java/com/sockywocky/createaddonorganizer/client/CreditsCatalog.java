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

    private record UnifiedContributor(String name, String color, List<String> bannerFiles) {}

    public static List<Entry> rows() {
        List<UnifiedContributor> source = RemoteBanners.hasEverCached()
                ? toUnified(RemoteBanners.contributors())
                : toUnifiedFromJar(loadJarContributors());
        return buildRows(source);
    }

    private static List<Entry> buildRows(List<UnifiedContributor> contributors) {
        List<Entry> out = new ArrayList<>();
        for (UnifiedContributor contributor : contributors) {
            List<ResourceLocation> textures = new ArrayList<>();
            for (String filename : contributor.bannerFiles()) {
                ResourceLocation texture = resolveBanner(contributor.name(), filename);
                if (texture != null) {
                    textures.add(texture);
                }
            }
            if (textures.isEmpty()) {
                continue;
            }
            int color = nameColor(contributor.color());
            out.add(new Entry(true, contributor.name(), color, null));
            for (ResourceLocation texture : textures) {
                out.add(new Entry(false, null, color, texture));
            }
        }
        return out;
    }

    private static List<UnifiedContributor> toUnifiedFromJar(List<Contributor> contributors) {
        List<UnifiedContributor> out = new ArrayList<>(contributors.size());
        for (Contributor c : contributors) {
            out.add(new UnifiedContributor(c.name(), c.color(), c.banners()));
        }
        return out;
    }

    private static List<UnifiedContributor> toUnified(List<RemoteBanners.RemoteContributor> contributors) {
        List<UnifiedContributor> out = new ArrayList<>(contributors.size());
        for (RemoteBanners.RemoteContributor c : contributors) {
            List<String> files = c.banners().stream().map(RemoteBanners.RemoteBannerFile::file).toList();
            out.add(new UnifiedContributor(c.name(), c.color(), files));
        }
        return out;
    }

    private static int nameColor(String color) {
        if (color == null) {
            return DEFAULT_NAME_COLOR;
        }
        Integer parsed = Config.parseColor(color);
        return parsed != null ? parsed : DEFAULT_NAME_COLOR;
    }

    private static List<Contributor> loadJarContributors() {
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
        ResourceLocation bundled = BannerTextures.resolve("res:" + createaddonorganizer.MODID + ":textures/banner/" + filename);
        if (bundled != null && bundledResourceExists(bundled)) {
            return bundled;
        }
        if (RemoteBanners.isAvailable(filename)) {
            return BannerTextures.resolve("remote:" + filename);
        }
        createaddonorganizer.LOGGER.warn("[CAO] credits manifest entry for '{}' references unresolvable banner '{}'",
                contributorName, filename);
        return null;
    }

    private static boolean bundledResourceExists(ResourceLocation texture) {
        return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
    }
}
