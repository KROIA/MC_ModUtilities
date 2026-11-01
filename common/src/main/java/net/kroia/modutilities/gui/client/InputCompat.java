package net.kroia.modutilities.gui.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.screens.Screen; // mc<=1.21.8
import net.minecraft.client.input.KeyEvent; // mc>=1.21.9
import net.minecraft.client.input.MouseButtonEvent; // mc>=1.21.9
import net.minecraft.client.input.MouseButtonInfo; // mc>=1.21.9

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
        //return Screen.hasShiftDown(); // mc<=1.21.8
        return Minecraft.getInstance().hasShiftDown(); // mc>=1.21.9
    }

    /** @return {@code true} while either control key (command on macOS) is held */
    public static boolean hasControlDown() {
        //return Screen.hasControlDown(); // mc<=1.21.8
        return Minecraft.getInstance().hasControlDown(); // mc>=1.21.9
    }

    /** @return whether touchscreen mode is on (26.2 removed the option; always false there) */
    public static boolean touchscreen() {
        //return Minecraft.getInstance().options.touchscreen().get(); // mc<=26.1
        return false; // mc>=26.2
    }

    /**
     * @param button a mouse button as delivered by Minecraft ({@code InputConstants.MOUSE_BUTTON_*})
     * @return the legacy container click index (0 left, 1 right, others unchanged) that
     *         {@code AbstractContainerMenu} and the click packet expect. 26.3 (SDL3) numbers the
     *         buttons 1 left, 2 middle, 3 right.
     */
    public static int containerClickButton(int button) {
        return button; // mc<=26.2
        //return button == InputConstants.MOUSE_BUTTON_LEFT ? 0 : button == InputConstants.MOUSE_BUTTON_RIGHT ? 1 : button; // mc>=26.3
    }

    /** @return the modifier bits ({@code InputConstants.MOD_*}) of the keys held right now */
    public static int currentModifiers() {
        return (hasShiftDown() ? 1 : 0) | (hasControlDown() ? 2 : 0) | (hasAltDown() ? 4 : 0); // mc<=26.2
        //return (hasShiftDown() ? InputConstants.MOD_SHIFT : 0) | (hasControlDown() ? InputConstants.MOD_CONTROL : 0) | (hasAltDown() ? InputConstants.MOD_ALT : 0); // mc>=26.3
    }

    /** @return {@code true} while either alt key is held */
    public static boolean hasAltDown() {
        //return Screen.hasAltDown(); // mc<=1.21.8
        return Minecraft.getInstance().hasAltDown(); // mc>=1.21.9
    }

    /** @return the native handle of the game window (GLFW, or SDL3 from 26.3) */
    public static long windowHandle() {
        //return Minecraft.getInstance().getWindow().getWindow(); // mc<=1.21.8
        return Minecraft.getInstance().getWindow().handle(); // mc>=1.21.9
    }

    /**
     * @param key a key code ({@code InputConstants.KEY_*})
     * @return {@code true} while the key is held
     */
    public static boolean isKeyDown(int key) {
        //return InputConstants.isKeyDown(windowHandle(), key); // mc<=1.21.8
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key); // mc>=1.21.9 mc<=26.2
        //return InputConstants.isKeyDown(key); // mc>=26.3
    }

    /**
     * @param button a mouse button ({@code InputConstants.MOUSE_BUTTON_*})
     * @return {@code true} while the button is held
     */
    public static boolean isMouseButtonDown(int button) {
        return org.lwjgl.glfw.GLFW.glfwGetMouseButton(windowHandle(), button) == org.lwjgl.glfw.GLFW.GLFW_PRESS; // mc<=26.2
        //return (org.lwjgl.sdl.SDLMouse.SDL_GetMouseState((java.nio.FloatBuffer) null, null) & (1 << (button - 1))) != 0; // mc>=26.3
    }

    /** Moves the mouse cursor to the given window position (in window coordinates). */
    public static void setCursorPos(double x, double y) {
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(windowHandle(), x, y); // mc<=26.2
        //org.lwjgl.sdl.SDLMouse.SDL_WarpMouseInWindow(windowHandle(), (float) x, (float) y); // mc>=26.3
    }

    /**
     * @return {@code true} if the key mapping is bound to the given keyboard key
     */
    public static boolean matchesKey(KeyMapping mapping, int key, int scanCode) {
        //return mapping.matches(key, scanCode); // mc<=1.21.8
        return mapping.matches(new KeyEvent(key, scanCode, 0)); // mc>=1.21.9
    }

    /**
     * @return {@code true} if the key mapping is bound to the given mouse button
     */
    public static boolean matchesMouse(KeyMapping mapping, int button) {
        //return mapping.matchesMouse(button); // mc<=1.21.8
        return mapping.matchesMouse(new MouseButtonEvent(0, 0, new MouseButtonInfo(button, 0))); // mc>=1.21.9
    }
}
