package com.sockywocky.createaddonorganizer.client;

import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.creativetab.TabLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import com.sockywocky.createaddonorganizer.AbsorbedTabs;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedHub;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;
import com.sockywocky.createaddonorganizer.createaddonorganizer;
import com.sockywocky.createaddonorganizer.createaddonorganizerClient;

public final class TabManager {
    private TabManager() {}

    public static void deleteSectionConfig(ResourceLocation id) {
        AbsorbedTabs.IDS.remove(id);
        Config.addForceExclude(id);
        Config.clearRoute(id);
        Config.removeForceInclude(id);
        if (Minecraft.getInstance().level != null) {
            LiveColors.remove(id);
            TabLayout.build();
            createaddonorganizer.refreshTabLayout(currentParams());
        }
    }

    public static void restoreSectionConfig(ResourceLocation id, boolean wasForceIncluded, ResourceLocation route) {
        if (wasForceIncluded) {
            Config.addForceInclude(id);
        }
        Config.removeForceExclude(id);
        ResourceLocation hub = route != null ? route : createaddonorganizer.CREATE_BASE;
        if (createaddonorganizer.CREATE_BASE.equals(hub)) {
            Config.clearRoute(id);
        } else {
            Config.setRoute(id, hub);
        }
        AbsorbedTabs.IDS.add(id);
        if (Minecraft.getInstance().level != null) {
            if (SimulatedSupport.isMainTab(hub)) {
                CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
                SimulatedHub.inject(id, tab.getDisplayName());
                SimulatedHub.foldItems(id, tab.getDisplayItems());
            } else {
                FancyTabSections.addSection(hub, createaddonorganizer.sectionFromLiveTab(id));
            }
            TabLayout.build();
            createaddonorganizer.refreshTabLayout(currentParams());
        }
    }

    private static CreativeModeTab.ItemDisplayParameters currentParams() {
        return createaddonorganizerClient.currentDisplayParams(Minecraft.getInstance());
    }
}
