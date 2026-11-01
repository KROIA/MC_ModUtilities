package net.kroia.modutilities.gui.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen; // mc<=1.21.8
//import net.minecraft.client.input.KeyEvent; // mc>=1.21.9
//import net.minecraft.client.input.MouseButtonEvent; // mc>=1.21.9
//import net.minecraft.client.input.MouseButtonInfo; // mc>=1.21.9

/**
 * Version-neutral access to client input state.
 * <p>
 * 1.21.9 moved keyboard and mouse input to event records ({@code KeyEvent}, {@code MouseButtonEvent},
 * {@code CharacterEvent}), moved the shift/control/alt queries from {@code Screen} to {@code Minecraft}
 * and made {@link InputConstants} take the {@code Window} instead of its raw GLFW handle.
 * Code that goes through this class compiles unchanged on every supported Minecraft version.
 */
@Environment(EnvType.CLIENT)
public final class InputCompat {
    private InputCompat() {}

    /** @return {@code true} while either shift key is held */
    public static boolean hasShiftDown() {
        return Screen.hasShiftDown(); // mc<=1.21.8
        //return Minecraft.getInstance().hasShiftDown(); // mc>=1.21.9
    }

    /** @return {@code true} while either control key (command on macOS) is held */
    public static boolean hasControlDown() {
        return Screen.hasControlDown(); // mc<=1.21.8
        //return Minecraft.getInstance().hasControlDown(); // mc>=1.21.9
    }

    /** @return whether touchscreen mode is on (26.2 removed the option; always false there) */
    public static boolean touchscreen() {
        return Minecraft.getInstance().options.touchscreen().get(); // mc<=26.1
        //return false; // mc>=26.2
    }

    /** @return the GLFW modifier bits (shift 1, control 2, alt 4) of the keys held right now */
    public static int currentModifiers() {
        return (hasShiftDown() ? 1 : 0) | (hasControlDown() ? 2 : 0) | (hasAltDown() ? 4 : 0);
    }

    /** @return {@code true} while either alt key is held */
    public static boolean hasAltDown() {
        return Screen.hasAltDown(); // mc<=1.21.8
        //return Minecraft.getInstance().hasAltDown(); // mc>=1.21.9
    }

    /** @return the GLFW handle of the game window */
    public static long windowHandle() {
        return Minecraft.getInstance().getWindow().getWindow(); // mc<=1.21.8
        //return Minecraft.getInstance().getWindow().handle(); // mc>=1.21.9
    }

    /**
     * @param key a GLFW key code
     * @return {@code true} while the key is held
     */
    public static boolean isKeyDown(int key) {
        return InputConstants.isKeyDown(windowHandle(), key); // mc<=1.21.8
        //return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key); // mc>=1.21.9
    }

    /**
     * @return {@code true} if the key mapping is bound to the given keyboard key
     */
    public static boolean matchesKey(KeyMapping mapping, int key, int scanCode) {
        return mapping.matches(key, scanCode); // mc<=1.21.8
        //return mapping.matches(new KeyEvent(key, scanCode, 0)); // mc>=1.21.9
    }

    /**
     * @return {@code true} if the key mapping is bound to the given mouse button
     */
    public static boolean matchesMouse(KeyMapping mapping, int button) {
        return mapping.matchesMouse(button); // mc<=1.21.8
        //return mapping.matchesMouse(new MouseButtonEvent(0, 0, new MouseButtonInfo(button, 0))); // mc>=1.21.9
    }
}
