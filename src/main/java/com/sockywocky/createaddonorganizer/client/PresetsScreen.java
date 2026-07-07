package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.util.List;

import com.sockywocky.createaddonorganizer.client.Presets.PresetData;
import com.sockywocky.createaddonorganizer.client.Presets.PresetRef;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PresetsScreen extends Screen {
    private final Screen parent;
    private PresetList list;

    public PresetsScreen(Screen parent) {
        super(Component.translatable("createaddonorganizer.colors.presets.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.presets.saveNew"),
                        b -> this.minecraft.setScreen(new NewPresetScreen(this)))
                .bounds(this.width / 2 - 100, 54, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.presets.import"),
                        b -> importPreset())
                .bounds(this.width / 2 - 100, 78, 200, 20).build());

        int listTop = 106;
        int listBottom = this.height - 40;
        list = new PresetList(this.minecraft, this.width, listBottom - listTop, listTop, 24);
        for (PresetRef ref : Presets.gallery()) {
            list.add(ref);
        }
        addRenderableWidget(list);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        float scale = 1.6f;
        g.pose().pushPose();
        g.pose().scale(scale, scale, scale);
        g.drawCenteredString(this.font, this.title, Math.round(this.width / 2 / scale), Math.round(16 / scale), 0xFFFFFFFF);
        g.pose().popPose();

        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.colors.presets.description"),
                this.width / 2, 34, 0xAAAAAAAA);
    }

    private void importPreset() {
        Presets.chooseImportFile().ifPresent(path -> {
            PresetData data = Presets.loadExternal(path);
            if (data == null) {
                Notice.show(Component.translatable("createaddonorganizer.colors.presets.import.failed"), Notice.RED);
                return;
            }
            try {
                Presets.save(data);
                Notice.show(Component.translatable("createaddonorganizer.colors.presets.import.success", data.name()), Notice.GREEN);
                this.rebuildWidgets();
            } catch (IOException e) {
                createaddonorganizer.LOGGER.warn("[CAO] failed to import preset {}", path, e);
            }
        });
    }

    private void applyWithConfirm(PresetRef ref) {
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                PresetData data = Presets.load(ref.ref());
                if (data != null) {
                    Presets.applyToConfig(data);
                    Presets.applyLive();
                    Notice.show(Component.translatable("createaddonorganizer.colors.presets.applied", ref.name()), Notice.GREEN);
                }
            }
            this.minecraft.setScreen(this);
        }, Component.translatable("createaddonorganizer.colors.presets.applyConfirm.title"),
                Component.translatable("createaddonorganizer.colors.presets.applyConfirm.message")));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private class PresetList extends ContainerObjectSelectionList<PresetList.Row> {
        PresetList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        void add(PresetRef ref) {
            addEntry(new Row(ref));
        }

        @Override
        public int getRowWidth() {
            return 320;
        }

        private class Row extends ContainerObjectSelectionList.Entry<Row> {
            private final PresetRef ref;
            private final Button edit;

            Row(PresetRef ref) {
                this.ref = ref;
                this.edit = Button.builder(Component.translatable("createaddonorganizer.colors.edit"),
                                b -> minecraft.setScreen(new PresetEditScreen(PresetsScreen.this, ref)))
                        .size(44, 20).build();
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(edit);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(edit);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (super.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (button == 0) {
                    applyWithConfirm(ref);
                    return true;
                }
                return false;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, boolean hovered, float partialTick) {
                int textY = top + (rowHeight - 8) / 2;
                String tag = ref.builtin() ? Component.translatable("createaddonorganizer.colors.presets.builtin").getString() : "";

                edit.setX(left + rowWidth - edit.getWidth());
                edit.setY(top + (rowHeight - 20) / 2);
                int labelX = edit.getX() - 10 - font.width(tag);

                g.drawString(font, ref.name(), left + 4, textY, 0xFFFFFFFF);
                if (!tag.isEmpty()) {
                    g.drawString(font, tag, labelX, textY, 0xFFAAAAAA);
                }
                edit.render(g, mouseX, mouseY, partialTick);
            }
        }
    }
}
