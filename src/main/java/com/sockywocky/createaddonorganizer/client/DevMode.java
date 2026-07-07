package com.sockywocky.createaddonorganizer.client;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

public final class DevMode {
    private static final long GESTURE_WINDOW_MS = 2000L;
    private static final int GESTURE_PRESSES = 3;

    private static final Set<String> AUTHORIZED_USERNAMES = Set.of("Dev", "SockyWocky7");

    private static final String CODE_HASH =
            "c5207bfa4a80d8368344d018ffd43c0819c6320db6c6505ed4ac1f1fd6f396c4";

    private static final Field CONFIG_SCREEN_MOD_FIELD = resolveField(ConfigurationScreen.class, "mod");
    private static final Field SECTION_SCREEN_CONTEXT_FIELD = resolveField(ConfigurationSectionScreen.class, "context");

    private static boolean unlocked;
    private static int shiftPressCount;
    private static long firstPressAtMillis;
    private static boolean shiftWasDown;

    private DevMode() {}

    public static boolean isUnlocked() {
        return unlocked;
    }

    public static void tick(Minecraft mc) {
        if (!(mc.screen instanceof Screen screen) || !isOurScreen(screen)) {
            shiftPressCount = 0;
            shiftWasDown = false;
            return;
        }
        long window = mc.getWindow().getWindow();
        boolean down = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (down && !shiftWasDown) {
            registerPress(mc, screen);
        }
        shiftWasDown = down;
    }

    private static void registerPress(Minecraft mc, Screen current) {
        long now = System.currentTimeMillis();
        if (shiftPressCount == 0 || now - firstPressAtMillis > GESTURE_WINDOW_MS) {
            shiftPressCount = 1;
            firstPressAtMillis = now;
        } else {
            shiftPressCount++;
        }
        if (shiftPressCount >= GESTURE_PRESSES) {
            shiftPressCount = 0;
            if (unlocked) {
                unlocked = false;
                Notice.show(Component.translatable("createaddonorganizer.devmode.deactivated"), Notice.RED);
            } else {
                mc.setScreen(new DevCodeScreen(current));
            }
        }
    }

    public static boolean isAuthorizedUser() {
        return AUTHORIZED_USERNAMES.contains(Minecraft.getInstance().getUser().getName());
    }

    public static boolean checkCode(String digits) {
        boolean correct = CODE_HASH.equals(sha256Hex(digits));
        if (correct) {
            unlocked = true;
        }
        return correct;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private static boolean isOurScreen(Screen screen) {
        try {
            if (screen instanceof ConfigurationScreen cs && CONFIG_SCREEN_MOD_FIELD != null) {
                ModContainer mod = (ModContainer) CONFIG_SCREEN_MOD_FIELD.get(cs);
                return mod != null && createaddonorganizer.MODID.equals(mod.getModId());
            }
            if (screen instanceof ConfigurationSectionScreen css && SECTION_SCREEN_CONTEXT_FIELD != null) {
                ConfigurationSectionScreen.Context context =
                        (ConfigurationSectionScreen.Context) SECTION_SCREEN_CONTEXT_FIELD.get(css);
                return context != null && createaddonorganizer.MODID.equals(context.modId());
            }
        } catch (ReflectiveOperationException e) {
            return false;
        }
        return false;
    }

    private static Field resolveField(Class<?> owner, String name) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException e) {
            createaddonorganizer.LOGGER.warn("[CAO] devmode: could not access {}#{}", owner.getSimpleName(), name, e);
            return null;
        }
    }
}
