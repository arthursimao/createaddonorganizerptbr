package com.sockywocky.createaddonorganizer.client.simulated;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public final class SimulatedSupport {
    public static final String MOD_ID = "simulated";
    public static final ResourceLocation MAIN_TAB = ResourceLocation.fromNamespaceAndPath(MOD_ID, "main_tab");

    private SimulatedSupport() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isMainTab(ResourceLocation id) {
        return MAIN_TAB.equals(id);
    }
}
