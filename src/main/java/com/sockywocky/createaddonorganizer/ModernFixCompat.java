package com.sockywocky.createaddonorganizer;

import java.lang.reflect.Field;

import net.minecraft.world.item.CreativeModeTab;

final class ModernFixCompat {
    private static final Field OLD_PARAMETERS = locate();

    private ModernFixCompat() {}

    private static Field locate() {
        try {
            Field f = CreativeModeTab.class.getDeclaredField("mfix$oldParameters");
            f.setAccessible(true);
            createaddonorganizer.LOGGER.info(
                    "[CAO] ModernFix creative-tab build memoization detected; will clear it before collection passes");
            return f;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    static void clearMemoizedParams(CreativeModeTab tab) {
        if (OLD_PARAMETERS == null) {
            return;
        }
        try {
            OLD_PARAMETERS.set(tab, null);
        } catch (IllegalAccessException ignored) {

        }
    }
}
