package com.sockywocky.createaddonorganizer.client;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

public final class ModBannerCatalog {
    public record TabEntry(ResourceLocation id, String label) {
        TabEntry(String id, String label) {
            this(ResourceLocation.parse(id), label);
        }
    }

    public record ModEntry(String modName, List<TabEntry> tabs) {
        ModEntry(String modName, TabEntry... tabs) {
            this(modName, List.of(tabs));
        }
    }

    public static final List<ModEntry> ENTRIES = List.of(
            new ModEntry("Create",
                    new TabEntry("create:base", "Create"),
                    new TabEntry("create:palettes", "Create: Palettes")),
            new ModEntry("Bits 'n' Bobs",
                    new TabEntry("bits_n_bobs:bnb_based", "Bits 'n' Bobs"),
                    new TabEntry("bits_n_bobs:bnb_palettes", "Bits 'n' Bobs' Block Palettes"),
                    new TabEntry("bits_n_bobs:bnb_deco", "Bits 'n' Bobs' Block Palettes (0.0.44)")),
            new ModEntry("Steam 'n' Rails",
                    new TabEntry("railways:main", "Steam 'n' Rails"),
                    new TabEntry("railways:tracks", "Steam 'n' Rails: Tracks"),
                    new TabEntry("railways:palettes", "Steam 'n' Rails: Palettes")),
            new ModEntry("Create: More Automation",
                    new TabEntry("create_more_automation:create_more_automation", "Create: More Automation")),
            new ModEntry("Cardboard Chalk Box",
                    new TabEntry("cardboardchalkbox:cardboard_chalk_tab", "Cardboard Chalk Box")),
            new ModEntry("Create Mobile Packages",
                    new TabEntry("create_mobile_packages:create_mobile_packages_tab", "Create Mobile Packages")));

    private ModBannerCatalog() {}
}
