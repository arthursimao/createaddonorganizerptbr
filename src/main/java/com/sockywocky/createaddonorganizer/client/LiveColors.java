package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.sockywocky.createaddonorganizer.client.simulated.SimulatedHub;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.Section.Section;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class LiveColors {
    private LiveColors() {}

    public static void apply(ResourceLocation id, int argb) {
        if (isSimulated(id)) {
            return;
        }
        replace(id, s -> s.withBanner(argb));
    }

    public static void applyTexture(ResourceLocation id, ResourceLocation texture) {
        if (isSimulated(id)) {
            return;
        }
        replace(id, s -> s.withTexture(texture));
    }

    public static void applyTextColor(ResourceLocation id, int argb) {
        if (isSimulated(id)) {
            return;
        }
        replace(id, s -> s.withTextColor(argb));
    }

    public static void applyTitle(ResourceLocation id, Component title) {
        if (isSimulated(id)) {
            SimulatedHub.applyTitle(id, title);
            return;
        }
        replace(id, s -> s.withTitle(title));
    }

    public static void applyOrder(ResourceLocation parent, List<ResourceLocation> orderedAddonIds) {
        if (SimulatedSupport.isMainTab(parent)) {
            SimulatedHub.reorder(orderedAddonIds);
            return;
        }
        List<Section> sections = FancyTabSections.SECTIONS_MAP.get(parent);
        if (sections == null || sections.isEmpty()) {
            return;
        }
        Map<ResourceLocation, Section> byId = new HashMap<>();
        for (Section section : sections) {
            byId.put(section.id(), section);
        }
        List<Section> rebuilt = new ArrayList<>(sections.size());
        Section own = byId.remove(parent);
        if (own != null) {
            rebuilt.add(own);
        }
        for (ResourceLocation id : orderedAddonIds) {
            Section section = byId.remove(id);
            if (section != null) {
                rebuilt.add(section);
            }
        }
        rebuilt.addAll(byId.values());
        FancyTabSections.SECTIONS_MAP.put(parent, rebuilt);
    }

    public static ResourceLocation findParent(ResourceLocation id) {
        if (SimulatedSupport.isLoaded()) {
            ResourceLocation owner = SimulatedHub.ownerIfAny(id);
            if (owner != null) {
                return owner;
            }
        }
        for (Map.Entry<ResourceLocation, List<Section>> entry : FancyTabSections.SECTIONS_MAP.entrySet()) {
            for (Section section : entry.getValue()) {
                if (section.id().equals(id)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public static Section remove(ResourceLocation id) {
        if (isSimulated(id)) {
            SimulatedHub.remove(id);
            return null;
        }
        Section first = null;
        for (ResourceLocation parent : new ArrayList<>(FancyTabSections.SECTIONS_MAP.keySet())) {
            List<Section> sections = FancyTabSections.SECTIONS_MAP.get(parent);
            if (sections == null) {
                continue;
            }
            List<Section> rebuilt = new ArrayList<>(sections.size());
            for (Section section : sections) {
                if (section.id().equals(id)) {
                    if (first == null) {
                        first = section;
                    }
                } else {
                    rebuilt.add(section);
                }
            }
            if (rebuilt.size() != sections.size()) {
                FancyTabSections.SECTIONS_MAP.put(parent, rebuilt);
            }
        }
        return first;
    }

    public static void moveToParent(ResourceLocation id, ResourceLocation newParent) {
        boolean toSimulated = SimulatedSupport.isMainTab(newParent);
        if (isSimulated(id)) {
            if (toSimulated) {
                return;
            }
            SimulatedHub.remove(id);
            FancyTabSections.addSection(newParent, createaddonorganizer.sectionFromLiveTab(id));
            return;
        }
        if (toSimulated) {
            Section removed = remove(id);
            if (removed != null) {
                SimulatedHub.inject(id, CaoSection.titleOf(removed));
                SimulatedHub.foldItems(id, removed.items().toStacks());
            }
            return;
        }
        Section section = remove(id);
        if (section != null) {
            FancyTabSections.addSection(newParent, section);
        }
    }

    private static boolean isSimulated(ResourceLocation id) {
        return SimulatedSupport.isLoaded() && SimulatedHub.ownerIfAny(id) != null;
    }

    private static void replace(ResourceLocation id, UnaryOperator<CaoSection> rebuild) {
        for (ResourceLocation parent : new ArrayList<>(FancyTabSections.SECTIONS_MAP.keySet())) {
            List<Section> sections = FancyTabSections.SECTIONS_MAP.get(parent);
            if (sections == null) {
                continue;
            }
            boolean changed = false;
            List<Section> rebuilt = new ArrayList<>(sections.size());
            for (Section section : sections) {
                if (section.id().equals(id) && section instanceof CaoSection cao) {
                    rebuilt.add(rebuild.apply(cao));
                    changed = true;
                } else {
                    rebuilt.add(section);
                }
            }
            if (changed) {
                FancyTabSections.SECTIONS_MAP.put(parent, rebuilt);
            }
        }
    }
}
