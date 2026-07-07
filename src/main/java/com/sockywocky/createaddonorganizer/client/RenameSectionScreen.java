package com.sockywocky.createaddonorganizer.client;

import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

public class RenameSectionScreen extends Screen {
    private final Screen parent;
    private final ResourceLocation id;
    private final String initialName;
    private EditBox nameBox;

    public RenameSectionScreen(Screen parent, ResourceLocation id, String currentName) {
        super(Component.translatable("createaddonorganizer.rename.title"));
        this.parent = parent;
        this.id = id;
        this.initialName = currentName;
    }

    @Override
    protected void init() {
        nameBox = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 24, 200, 20, Component.empty());
        nameBox.setMaxLength(64);
        nameBox.setValue(initialName);
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);

        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.ok"), b -> confirm())
                .bounds(this.width / 2 - 102, this.height / 2 + 4, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.cancel"), b -> onClose())
                .bounds(this.width / 2 + 2, this.height / 2 + 4, 100, 20).build());
    }

    private void confirm() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) {
            Config.clearSectionName(id);
            LiveColors.applyTitle(id, defaultTitle());
        } else {
            Config.setSectionName(id, name);
            LiveColors.applyTitle(id, Component.literal(name));
        }
        onClose();
    }

    private Component defaultTitle() {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
        return tab != null ? tab.getDisplayName() : Component.literal(id.toString());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 54, 0xFFFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.rename.hint"),
                this.width / 2, this.height / 2 - 40, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
