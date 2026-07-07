package com.sockywocky.createaddonorganizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sockywocky.createaddonorganizer.client.simulated.SimulatedHub;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;

import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.Section.Section;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

public final class SectionCatalog {
    private SectionCatalog() {}

    public record Entry(ResourceLocation id, Component name, boolean parent, boolean readOnly,
            Integer nativeTextColor, Integer nativeSecondaryTextColor) {
        public Entry(ResourceLocation id, Component name, boolean parent) {
            this(id, name, parent, false, null, null);
        }
    }

    public static Set<ResourceLocation> knownHubs() {
        Set<ResourceLocation> hubs = new HashSet<>();
        hubs.add(createaddonorganizer.CREATE_BASE);
        hubs.addAll(createaddonorganizer.MANAGED_PARENTS);
        hubs.addAll(Config.allRouteTargets());
        hubs.addAll(Config.extraMainSections());
        if (SimulatedSupport.isLoaded()) {
            hubs.add(SimulatedSupport.MAIN_TAB);
        }
        hubs.removeIf(Config::isForceExcluded);
        return hubs;
    }

    public static List<Entry> colorables() {
        Map<ResourceLocation, Component> names = new HashMap<>();
        Map<ResourceLocation, List<ResourceLocation>> addonsByParent = new LinkedHashMap<>();
        Set<ResourceLocation> hubCandidates = knownHubs();
        for (ResourceLocation parent : hubCandidates) {
            addonsByParent.putIfAbsent(parent, new ArrayList<>());
        }

        for (Map.Entry<ResourceKey<CreativeModeTab>, CreativeModeTab> e : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
            ResourceLocation id = e.getKey().location();
            String nameOverride = Config.sectionNameOverride(id);
            names.put(id, nameOverride != null ? Component.literal(nameOverride) : e.getValue().getDisplayName());
            if (AddonDetection.isAbsorbTarget(id)) {
                addonsByParent.computeIfAbsent(Config.parentFor(id), k -> new ArrayList<>()).add(id);
            }
        }

        Set<ResourceLocation> managed = addonsByParent.keySet();

        List<Entry> out = new ArrayList<>();
        for (ResourceLocation parent : orderedParents(managed)) {
            List<ResourceLocation> sectionIds = sectionOrder(parent, addonsByParent.getOrDefault(parent, List.of()), names);
            boolean firstRow = true;
            for (ResourceLocation sid : sectionIds) {
                out.add(new Entry(sid, names.getOrDefault(sid, Component.literal(sid.toString())), sid.equals(parent)));
                if (firstRow && SimulatedSupport.isLoaded() && SimulatedSupport.isMainTab(parent)) {
                    for (SimulatedHub.NativeSection nativeSection : SimulatedHub.nativeSections()) {
                        out.add(new Entry(nativeSection.id(), nativeSection.title(), false, true,
                                nativeSection.textColor(), nativeSection.secondaryTextColor()));
                    }
                }
                firstRow = false;
            }
        }
        return out;
    }

    private static List<ResourceLocation> orderedParents(Set<ResourceLocation> managed) {
        List<ResourceLocation> order = new ArrayList<>();
        for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id != null && managed.contains(id) && !order.contains(id)) {
                order.add(id);
            }
        }
        for (ResourceLocation id : managed) {
            if (!order.contains(id)) {
                order.add(id);
            }
        }
        return order;
    }

    private static List<ResourceLocation> sectionOrder(ResourceLocation parent, List<ResourceLocation> predictedAddons,
            Map<ResourceLocation, Component> names) {
        List<Section> live = FancyTabSections.SECTIONS_MAP.get(parent);
        if (live != null && !live.isEmpty()) {
            List<ResourceLocation> ids = new ArrayList<>(live.size());
            for (Section section : live) {
                ids.add(section.id());
            }
            return ids;
        }
        List<ResourceLocation> ids = new ArrayList<>(predictedAddons.size() + 1);
        ids.add(parent);
        ids.addAll(Config.applyOrder(predictedAddons,
                id -> names.getOrDefault(id, Component.literal(id.toString())).getString()));
        return ids;
    }
}
