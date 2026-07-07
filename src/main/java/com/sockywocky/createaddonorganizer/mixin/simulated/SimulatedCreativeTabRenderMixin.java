package com.sockywocky.createaddonorganizer.mixin.simulated;

import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.sockywocky.createaddonorganizer.client.simulated.SimulatedHub;

import dev.simulated_team.simulated.api.SimpleResourceManager;
import dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@Mixin(targets = "dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab", remap = false)
public class SimulatedCreativeTabRenderMixin {

    private static ResourceLocation createaddonorganizer$currentId;
    private static boolean createaddonorganizer$currentOwned;
    private static int createaddonorganizer$left;
    private static int createaddonorganizer$top;

    @Inject(method = "renderBanners", at = @At("HEAD"))
    private static void createaddonorganizer$verify(CreativeModeInventoryScreen screen, GuiGraphics graphics,
            int mouseX, int mouseY, CallbackInfo ci) {
        SimulatedHub.verifyInjected();
    }

    @Redirect(method = "renderBanners", at = @At(value = "INVOKE",
            target = "Ldev/simulated_team/simulated/api/SimpleResourceManager;getId(Ljava/lang/Object;)Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation createaddonorganizer$captureId(SimpleResourceManager manager, Object section,
            @Local(ordinal = 0) int left, @Local(ordinal = 1) int top) {
        ResourceLocation id = manager.getId(section);
        createaddonorganizer$currentId = id;
        createaddonorganizer$currentOwned = SimulatedHub.owns(id);
        createaddonorganizer$left = left;
        createaddonorganizer$top = top;
        return id;
    }

    @Redirect(method = "renderBanners", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private static void createaddonorganizer$blitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h) {
        if (createaddonorganizer$currentOwned) {
            return;
        }
        g.blitSprite(sprite, x, y, w, h);
    }

    @Redirect(method = "renderBanners", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private static void createaddonorganizer$fill(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        if (createaddonorganizer$currentOwned) {
            return;
        }
        g.fill(x1, y1, x2, y2, color);
    }

    @Redirect(method = "renderBanners", at = @At(value = "INVOKE",
            target = "Ldev/simulated_team/simulated/registrate/simulated_tab/SimulatedCreativeTab;drawAuraText(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/network/chat/Component;IIII)V"))
    private static void createaddonorganizer$drawAuraText(GuiGraphics g, Component text, int color1, int color2, int x, int y) {
        if (createaddonorganizer$currentOwned) {
            int sectionRow = SimulatedCreativeTab.SECTION_Y_VALUES.getInt(createaddonorganizer$currentId) - SimulatedCreativeTab.CURRENT_ROW;
            SimulatedHub.renderOwned(g, Minecraft.getInstance().font, createaddonorganizer$currentId,
                    createaddonorganizer$left, createaddonorganizer$top, sectionRow);
            return;
        }
        SimulatedCreativeTab.drawAuraText(g, text, color1, color2, x, y);
    }
}
