package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sockywocky.createaddonorganizer.SectionCatalog;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public class BannerAssignmentScreen extends Screen {
    private record TabRow(ResourceLocation id, String label) {}

    private final Screen parent;

    public BannerAssignmentScreen(Screen parent) {
        super(Component.translatable("createaddonorganizer.bannerAssign.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int listTop = 54;
        int listBottom = this.height - 34;
        AssignList list = new AssignList(this.minecraft, this.width, listBottom - listTop, listTop, 24);
        for (Map.Entry<String, List<TabRow>> mod : groupedTabs().entrySet()) {
            list.addHeader(mod.getKey());
            for (TabRow tab : mod.getValue()) {
                list.addTab(tab);
            }
        }
        addRenderableWidget(list);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    private static Map<String, List<TabRow>> groupedTabs() {
        Set<ResourceLocation> seen = new LinkedHashSet<>();
        Map<String, List<TabRow>> byMod = new LinkedHashMap<>();

        for (SectionCatalog.Entry entry : SectionCatalog.colorables()) {
            if (entry.readOnly() || !seen.add(entry.id())) {
                continue;
            }
            byMod.computeIfAbsent(modNameFor(entry.id()), k -> new ArrayList<>())
                    .add(new TabRow(entry.id(), entry.name().getString()));
        }

        for (ModBannerCatalog.ModEntry mod : ModBannerCatalog.ENTRIES) {
            for (ModBannerCatalog.TabEntry tab : mod.tabs()) {
                if (!seen.add(tab.id())) {
                    continue;
                }
                byMod.computeIfAbsent(mod.modName(), k -> new ArrayList<>()).add(new TabRow(tab.id(), tab.label()));
            }
        }
        return byMod;
    }

    private static String modNameFor(ResourceLocation id) {
        return ModList.get().getModContainerById(id.getNamespace())
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(id.getNamespace());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.bannerAssign.description"),
                this.width / 2, 32, 0xAAAAAAAA);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private class AssignList extends ContainerObjectSelectionList<AssignList.Row> {
        AssignList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        void addHeader(String modName) {
            addEntry(new Row(modName, null));
        }

        void addTab(TabRow tab) {
            addEntry(new Row(null, tab));
        }

        @Override
        public int getRowWidth() {
            return 320;
        }

        private class Row extends ContainerObjectSelectionList.Entry<Row> {
            private final String modName;
            private final TabRow tab;
            private final Button edit;

            Row(String modName, TabRow tab) {
                this.modName = modName;
                this.tab = tab;
                this.edit = tab == null ? null
                        : Button.builder(Component.translatable("createaddonorganizer.colors.edit"),
                                        b -> minecraft.setScreen(
                                                new BannerPoolEditScreen(BannerAssignmentScreen.this, tab.id(), tab.label())))
                                .size(44, 20).build();
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return edit != null ? List.of(edit) : List.of();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return edit != null ? List.of(edit) : List.of();
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, boolean hovered, float partialTick) {
                int textY = top + (rowHeight - 8) / 2;

                if (tab == null) {
                    if (index != 0) {
                        g.fill(left, top, left + rowWidth, top + 1, 0x60FFFFFF);
                    }
                    g.drawString(font, Component.literal(modName).withStyle(ChatFormatting.BOLD), left, textY, 0xFFE4E4E4);
                    return;
                }

                int previewW = 48;
                int contentLeft = left + 14;
                List<String> pool = BannerPools.poolFor(tab.id());
                String rightLabel;
                if (!pool.isEmpty()) {
                    int th = BannerTextures.HEIGHT;
                    int ty = top + (rowHeight - th) / 2;
                    g.fill(contentLeft - 1, ty - 1, contentLeft + previewW + 1, ty + th + 1, 0xFF000000);
                    ResourceLocation tex = BannerTextures.resolve(pool.get(0));
                    if (tex != null) {
                        int texHeight = BannerAnimation.preview(tex, false, 1)
                                .map(BannerAnimation.AnimInfo::frameCount).orElse(1) * BannerTextures.HEIGHT;
                        BannerTextures.blitCropped(g, tex, contentLeft, ty, previewW, th, texHeight);
                    }
                    rightLabel = Component.translatable("createaddonorganizer.bannerAssign.count", pool.size()).getString();
                } else {
                    int th = BannerTextures.HEIGHT;
                    int ty = top + (rowHeight - th) / 2;
                    g.fill(contentLeft - 1, ty - 1, contentLeft + previewW + 1, ty + th + 1, 0xFF000000);
                    g.fill(contentLeft, ty, contentLeft + previewW, ty + th, 0xFF303030);
                    rightLabel = Component.translatable("createaddonorganizer.bannerAssign.unrestricted").getString();
                }

                edit.setX(left + rowWidth - edit.getWidth());
                edit.setY(top + (rowHeight - 20) / 2);
                int labelX = edit.getX() - 10 - font.width(rightLabel);
                int nameX = contentLeft + previewW + 8;

                g.drawString(font, tab.label(), nameX, textY, 0xFFFFFFFF);
                g.drawString(font, rightLabel, labelX, textY, 0xFFAAAAAA);
                edit.render(g, mouseX, mouseY, partialTick);
            }
        }
    }
}
