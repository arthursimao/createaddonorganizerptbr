package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.SectionCatalog;
import com.sockywocky.createaddonorganizer.createaddonorganizer;
import com.sockywocky.createaddonorganizer.createaddonorganizerClient;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.fml.loading.FMLPaths;

public final class Presets {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PRESETS_DIR = FMLPaths.CONFIGDIR.get().resolve("createaddonorganizer/presets");

    private Presets() {}

    public record PresetData(String name, int bannerColor, List<String> sectionColors, List<String> banners,
            List<String> animatedBanners, boolean tintedBox, int boxColor, List<String> boxColors,
            int textColor, List<String> textColors, boolean twoTone, int textSecondaryColor,
            List<String> textSecondaryColors, List<String> sectionOrder, List<String> sectionNames,
            List<String> forceInclude, List<String> forceExclude, List<String> routes, List<String> extraMainSections,
            List<String> highlightColors, boolean showAllBanners, List<String> extraBannerPool) {
    }

    private static List<String> orEmpty(List<String> list) {
        return Objects.requireNonNullElse(list, List.of());
    }

    public record PresetRef(String ref, String name) {
        public boolean builtin() {
            return ref.startsWith("res:");
        }
    }

    public static PresetData captureCurrent(String name) {
        return new PresetData(name,
                Config.DEFAULT_BANNER_COLOR.get(), List.copyOf(Config.SECTION_COLORS.get()), List.copyOf(Config.BANNERS.get()),
                List.copyOf(Config.ANIMATED_BANNERS.get()), Config.tintedTextBox(), Config.DEFAULT_BOX_COLOR.get(),
                List.copyOf(Config.BOX_COLORS.get()), Config.DEFAULT_TEXT_COLOR.get(), List.copyOf(Config.TEXT_COLORS.get()),
                Config.TWO_TONE_TEXT.get(), Config.DEFAULT_TEXT_SECONDARY_COLOR.get(), List.copyOf(Config.TEXT_SECONDARY_COLORS.get()),
                List.copyOf(Config.SECTION_ORDER.get()), List.copyOf(Config.SECTION_NAMES.get()),
                List.copyOf(Config.FORCE_INCLUDE.get()), List.copyOf(Config.FORCE_EXCLUDE.get()),
                List.copyOf(Config.ROUTES.get()), List.copyOf(Config.EXTRA_MAIN_SECTIONS.get()),
                List.copyOf(Config.HIGHLIGHT_COLORS.get()), Config.showAllBanners(),
                List.copyOf(Config.EXTRA_BANNER_POOL.get()));
    }

    public static void applyToConfig(PresetData data) {
        Config.applyAppearance(data.bannerColor(), data.sectionColors(), data.banners(), data.animatedBanners(),
                data.tintedBox(), data.boxColor(), data.boxColors(), data.textColor(), data.textColors(),
                data.twoTone(), data.textSecondaryColor(), data.textSecondaryColors(), orEmpty(data.highlightColors()),
                data.showAllBanners(), orEmpty(data.extraBannerPool()));
        Config.applyOrganization(orEmpty(data.sectionOrder()), orEmpty(data.sectionNames()));
        Config.applyAbsorption(orEmpty(data.forceInclude()), orEmpty(data.forceExclude()),
                orEmpty(data.routes()), orEmpty(data.extraMainSections()));
    }

    public static void applyLive() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            createaddonorganizer.reapplyAbsorption(createaddonorganizerClient.currentDisplayParams(mc));
        }
        List<SectionCatalog.Entry> entries = SectionCatalog.colorables();
        Map<ResourceLocation, String> namesById = new HashMap<>();
        for (SectionCatalog.Entry entry : entries) {
            namesById.put(entry.id(), entry.name().getString());
            if (entry.readOnly()) {
                continue;
            }
            LiveColors.applyTitle(entry.id(), entry.name());

            String ref = Config.bannerRefFor(entry.id());
            ResourceLocation texture = ref != null ? BannerTextures.resolve(ref) : null;
            if (texture != null) {
                LiveColors.applyTexture(entry.id(), texture);
            } else {
                LiveColors.apply(entry.id(), Config.bannerColorFor(entry.id()));
            }
            LiveColors.applyTextColor(entry.id(), Config.textColorFor(entry.id()));
        }

        ResourceLocation currentParent = null;
        List<ResourceLocation> group = new ArrayList<>();
        for (SectionCatalog.Entry entry : entries) {
            if (entry.parent()) {
                applyOrderedGroup(currentParent, group, namesById);
                currentParent = entry.id();
                group = new ArrayList<>();
            } else if (!entry.readOnly()) {
                group.add(entry.id());
            }
        }
        applyOrderedGroup(currentParent, group, namesById);

        if (mc.level != null && mc.player != null) {
            createaddonorganizer.refreshTabLayout(createaddonorganizerClient.currentDisplayParams(mc));
        }
    }

    private static void applyOrderedGroup(ResourceLocation parent, List<ResourceLocation> group,
            Map<ResourceLocation, String> namesById) {
        if (parent == null || group.isEmpty()) {
            return;
        }
        List<ResourceLocation> ordered = Config.applyOrder(group, id -> namesById.getOrDefault(id, id.toString()));
        LiveColors.applyOrder(parent, ordered);
    }

    public static List<PresetRef> gallery() {
        List<PresetRef> bundled = new ArrayList<>();
        Map<ResourceLocation, Resource> found = Minecraft.getInstance().getResourceManager()
                .listResources("presets", p -> p.getPath().endsWith(".json"));
        for (ResourceLocation res : found.keySet()) {
            String ref = "res:" + res;
            PresetData data = load(ref);
            if (data != null) {
                bundled.add(new PresetRef(ref, data.name()));
            }
        }
        bundled.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

        List<PresetRef> user = new ArrayList<>();
        if (Files.isDirectory(PRESETS_DIR)) {
            try (var files = Files.list(PRESETS_DIR)) {
                files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
                    String ref = "file:" + p.getFileName();
                    PresetData data = load(ref);
                    if (data != null) {
                        user.add(new PresetRef(ref, data.name()));
                    }
                });
            } catch (IOException e) {
                createaddonorganizer.LOGGER.warn("[CAO] failed to list user presets", e);
            }
        }
        user.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

        List<PresetRef> out = new ArrayList<>(bundled.size() + user.size());
        out.addAll(bundled);
        out.addAll(user);
        return out;
    }

    public static PresetData load(String ref) {
        if (ref == null) {
            return null;
        }
        if (ref.startsWith("res:")) {
            ResourceLocation res = ResourceLocation.tryParse(ref.substring(4));
            if (res == null) {
                return null;
            }
            try (InputStream in = Minecraft.getInstance().getResourceManager().open(res);
                    Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, PresetData.class);
            } catch (IOException e) {
                createaddonorganizer.LOGGER.warn("[CAO] failed to load bundled preset {}", ref, e);
                return null;
            }
        }
        if (ref.startsWith("file:")) {
            Path path = PRESETS_DIR.resolve(ref.substring(5));
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, PresetData.class);
            } catch (IOException e) {
                createaddonorganizer.LOGGER.warn("[CAO] failed to load user preset {}", ref, e);
                return null;
            }
        }
        return null;
    }

    public static String save(PresetData data) throws IOException {
        Files.createDirectories(PRESETS_DIR);
        String fileName = dedupedName(sanitizeStem(data.name()));
        writeFile(PRESETS_DIR.resolve(fileName), data);
        return "file:" + fileName;
    }

    public static void overwrite(String ref, PresetData data) throws IOException {
        if (ref == null || !ref.startsWith("file:")) {
            return;
        }
        writeFile(PRESETS_DIR.resolve(ref.substring(5)), data);
    }

    public static final class DevWriteException extends Exception {
        public DevWriteException(String message) {
            super(message);
        }
    }

    public static void overwriteBuiltin(String ref, PresetData data) throws IOException, DevWriteException {
        if (ref == null || !ref.startsWith("res:")) {
            return;
        }
        ResourceLocation res = ResourceLocation.tryParse(ref.substring(4));
        if (res == null) {
            throw new DevWriteException("invalid built-in preset ref: " + ref);
        }
        Path devDir = resolveDevPresetsDir();
        if (devDir == null || !Files.isDirectory(devDir)) {
            throw new DevWriteException("not running from source (gradlew runClient)");
        }
        String path = res.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        writeFile(devDir.resolve(fileName), data);
    }

    private static Path resolveDevPresetsDir() {
        Path cwd = Path.of("").toAbsolutePath();
        Path projectRoot = cwd.getParent();
        return projectRoot == null ? null
                : projectRoot.resolve("src/main/resources/assets/createaddonorganizer/presets");
    }

    public static void delete(String ref) {
        if (ref == null || !ref.startsWith("file:")) {
            return;
        }
        try {
            Files.deleteIfExists(PRESETS_DIR.resolve(ref.substring(5)));
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to delete preset {}", ref, e);
        }
    }

    private static void writeFile(Path path, PresetData data) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    public static Optional<Path> chooseImportFile() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.json"));
            filters.flip();
            String result = TinyFileDialogs.tinyfd_openFileDialog(
                    "Choose a preset JSON file to import", "", filters, "Preset JSON (*.json)", false);
            if (result == null || result.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Path.of(result));
        } catch (Throwable t) {
            createaddonorganizer.LOGGER.warn("[CAO] native file dialog unavailable", t);
            return Optional.empty();
        }
    }

    public static Optional<Path> chooseExportFile(String suggestedName) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.json"));
            filters.flip();
            String result = TinyFileDialogs.tinyfd_saveFileDialog(
                    "Export preset to...", suggestedName, filters, "Preset JSON (*.json)");
            if (result == null || result.isBlank()) {
                return Optional.empty();
            }
            String path = result.toLowerCase(Locale.ROOT).endsWith(".json") ? result : result + ".json";
            return Optional.of(Path.of(path));
        } catch (Throwable t) {
            createaddonorganizer.LOGGER.warn("[CAO] native file dialog unavailable", t);
            return Optional.empty();
        }
    }

    public static String suggestedFileName(String presetName) {
        return sanitizeStem(presetName) + ".json";
    }

    public static PresetData loadExternal(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, PresetData.class);
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to load external preset {}", path, e);
            return null;
        } catch (RuntimeException e) {
            createaddonorganizer.LOGGER.warn("[CAO] malformed external preset {}", path, e);
            return null;
        }
    }

    public static void exportToFile(Path destination, PresetData data) throws IOException {
        writeFile(destination, data);
    }

    private static String sanitizeStem(String rawName) {
        String stem = rawName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return stem.isEmpty() ? "preset" : stem;
    }

    private static String dedupedName(String stem) {
        String name = stem + ".json";
        int suffix = 2;
        while (Files.exists(PRESETS_DIR.resolve(name))) {
            name = stem + "_" + suffix + ".json";
            suffix++;
        }
        return name;
    }
}
