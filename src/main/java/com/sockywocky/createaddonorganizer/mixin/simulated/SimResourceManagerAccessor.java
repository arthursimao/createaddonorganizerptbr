package com.sockywocky.createaddonorganizer.mixin.simulated;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.resources.ResourceLocation;

@Mixin(targets = "dev.simulated_team.simulated.api.SimpleResourceManager", remap = false)
public interface SimResourceManagerAccessor {
    @Accessor("entries")
    Map<ResourceLocation, Object> getEntries();

    @Accessor("toId")
    Map<Object, ResourceLocation> getToId();

    @Accessor("sortedValues")
    List<Object> getSortedValues();
}
