package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BannerPoolEditScreen extends Screen {
    private final Screen parent;
    private final ResourceLocation tabId;
    private final Set<String> pool;
    private PoolList list;

    public BannerPoolEditScreen(Screen parent, ResourceLocation tabId, String label) {
        super(Component.translatable("createaddonorganizer.bannerPool.title", label));
        this.parent = parent;
        this.tabId = tabId;
        this.pool = new LinkedHashSet<>(BannerPools.poolFor(tabId));
    }

    @Override
    protected void init() {
        int listTop = 54;
        int listBottom = this.height - 34;
        list = new PoolList(this.minecraft, this.width, listBottom - listTop, listTop, BannerTextures.HEIGHT + 6);
        List<String> gallery = BannerTextures.gallery();
        for (String ref : gallery) {
            list.add(ref);
        }
        for (String ref : pool) {
            if (!gallery.contains(ref)) {
                list.add(ref);
            }
        }
        addRenderableWidget(list);

        int buttonsY = this.height - 28;
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.banner.upload"), b -> upload())
                .bounds(this.width / 2 - 200, buttonsY, 130, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.bannerPool.save"), b -> save())
                .bounds(this.width / 2 - 66, buttonsY, 132, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(this.width / 2 + 70, buttonsY, 130, 20).build());
    }

    private void upload() {
        Optional<Path> chosen = BannerTextures.chooseFile();
        if (chosen.isEmpty()) {
            return;
        }
        try {
            String ref = BannerTextures.importFile(chosen.get());
            pool.add(ref);
            rebuildWidgets();
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to import banner image", e);
        }
    }

    private void save() {
        try {
            BannerPools.setPool(tabId, List.copyOf(pool));
            Notice.show(Component.translatable("createaddonorganizer.bannerPool.saved"), Notice.GREEN);
            onClose();
        } catch (BannerPools.DevWriteException e) {
            Notice.show(Component.translatable("createaddonorganizer.devmode.writeFailed", e.getMessage()), Notice.RED);
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to save banner pool for {}", tabId, e);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.bannerPool.description"),
                this.width / 2, 32, 0xAAAAAAAA);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private class PoolList extends ContainerObjectSelectionList<PoolList.Row> {
        PoolList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        void add(String ref) {
            addEntry(new Row(ref));
        }

        @Override
        public int getRowWidth() {
            return BannerTextures.WIDTH + 12;
        }

        private class Row extends ContainerObjectSelectionList.Entry<Row> {
            private final String ref;
            private final ResourceLocation texture;

            Row(String ref) {
                this.ref = ref;
                this.texture = BannerTextures.resolve(ref);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of();
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (button == 0) {
                    if (!pool.add(ref)) {
                        pool.remove(ref);
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, boolean hovered, float partialTick) {
                int bx = left + (rowWidth - BannerTextures.WIDTH) / 2;
                int by = top + (rowHeight - BannerTextures.HEIGHT) / 2;
                boolean inPool = pool.contains(ref);
                int border = inPool ? 0xFF55FF55 : (hovered ? 0xFF808080 : 0xFF000000);
                g.fill(bx - 1, by - 1, bx + BannerTextures.WIDTH + 1, by + BannerTextures.HEIGHT + 1, border);
                if (texture != null) {
                    int texHeight = BannerAnimation.preview(texture, false, 1)
                            .map(BannerAnimation.AnimInfo::frameCount).orElse(1) * BannerTextures.HEIGHT;
                    g.blit(texture, bx, by, 0.0F, 0.0F, BannerTextures.WIDTH, BannerTextures.HEIGHT,
                            BannerTextures.WIDTH, texHeight);
                }
                if (inPool) {
                    Component check = Component.literal("✓");
                    g.drawString(font, check, bx + BannerTextures.WIDTH - font.width(check) - 2,
                            by + BannerTextures.HEIGHT - 10, 0xFF55FF55);
                }
            }
        }
    }
}
