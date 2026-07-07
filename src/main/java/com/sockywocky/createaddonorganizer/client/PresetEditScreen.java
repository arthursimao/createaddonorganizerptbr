package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;

import com.sockywocky.createaddonorganizer.client.Presets.PresetData;
import com.sockywocky.createaddonorganizer.client.Presets.PresetRef;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PresetEditScreen extends Screen {
    private final Screen parent;
    private final PresetRef ref;

    public PresetEditScreen(Screen parent, PresetRef ref) {
        super(Component.literal(ref.name()));
        this.parent = parent;
        this.ref = ref;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 50;

        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.presets.apply"), b -> apply())
                .bounds(x, y, 200, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.presets.export"), b -> exportPreset())
                .bounds(x, y, 200, 20).build());
        y += 24;

        if (!ref.builtin()) {
            addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.presets.saveCurrent"),
                            b -> saveCurrent())
                    .bounds(x, y, 200, 20).build());
            y += 24;
            addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.presets.delete"),
                            b -> confirmDelete())
                    .bounds(x, y, 200, 20).build());
            y += 24;
        } else if (DevMode.isUnlocked()) {
            addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.presets.saveCurrent"),
                            b -> saveCurrent())
                    .bounds(x, y, 200, 20).build());
            y += 24;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, y + 6, 200, 20).build());
    }

    private void apply() {
        PresetData data = Presets.load(ref.ref());
        if (data == null) {
            return;
        }
        Presets.applyToConfig(data);
        Presets.applyLive();
        Notice.show(Component.translatable("createaddonorganizer.colors.presets.applied", ref.name()), Notice.GREEN);
    }

    private void saveCurrent() {
        try {
            if (ref.builtin()) {
                Presets.overwriteBuiltin(ref.ref(), Presets.captureCurrent(ref.name()));
            } else {
                Presets.overwrite(ref.ref(), Presets.captureCurrent(ref.name()));
            }
            Notice.show(Component.translatable("createaddonorganizer.colors.presets.saveCurrent.success"), Notice.GREEN);
        } catch (Presets.DevWriteException e) {
            Notice.show(Component.translatable("createaddonorganizer.devmode.writeFailed", e.getMessage()), Notice.RED);
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to save preset {}", ref.ref(), e);
        }
    }

    private void exportPreset() {
        PresetData data = Presets.load(ref.ref());
        if (data == null) {
            return;
        }
        Presets.chooseExportFile(Presets.suggestedFileName(ref.name())).ifPresent(path -> {
            try {
                Presets.exportToFile(path, data);
                Notice.show(Component.translatable("createaddonorganizer.colors.presets.export.success"), Notice.GREEN);
            } catch (IOException e) {
                createaddonorganizer.LOGGER.warn("[CAO] failed to export preset {}", ref.ref(), e);
                Notice.show(Component.translatable("createaddonorganizer.colors.presets.export.failed", e.getMessage()), Notice.RED);
            }
        });
    }

    private void confirmDelete() {
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                Presets.delete(ref.ref());
                Notice.show(Component.translatable("createaddonorganizer.colors.presets.deleted"), Notice.RED);
                onClose();
            } else {
                this.minecraft.setScreen(this);
            }
        }, Component.translatable("createaddonorganizer.colors.presets.delete.title"),
                Component.translatable("createaddonorganizer.colors.presets.delete.message", ref.name())));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 74, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
