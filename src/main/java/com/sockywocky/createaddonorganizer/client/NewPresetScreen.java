package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;

import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NewPresetScreen extends Screen {
    private final Screen parent;
    private EditBox nameBox;
    private Button okButton;

    public NewPresetScreen(Screen parent) {
        super(Component.translatable("createaddonorganizer.colors.presets.saveNew"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        nameBox = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 24, 200, 20, Component.empty());
        nameBox.setMaxLength(48);
        nameBox.setResponder(s -> okButton.active = !s.trim().isEmpty());
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);

        okButton = addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.ok"), b -> confirm())
                .bounds(this.width / 2 - 102, this.height / 2 + 4, 100, 20).build());
        okButton.active = false;
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.cancel"), b -> onClose())
                .bounds(this.width / 2 + 2, this.height / 2 + 4, 100, 20).build());
    }

    private void confirm() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        try {
            Presets.save(Presets.captureCurrent(name));
            Notice.show(Component.translatable("createaddonorganizer.colors.presets.created", name), Notice.GREEN);
            onClose();
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to save new preset", e);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 54, 0xFFFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.colors.presets.namePrompt"),
                this.width / 2, this.height / 2 - 40, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
