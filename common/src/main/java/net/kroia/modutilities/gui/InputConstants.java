package net.kroia.modutilities.gui;

/**
 * Key and mouse button constants for use by common (server + client) GUI code.
 * <p>
 * Every value is an alias of Minecraft's own {@code com.mojang.blaze3d.platform.InputConstants}
 * so that key codes received from Minecraft's input pipeline can be compared directly.
 * The values are compile-time constants (inlined by javac), so common code that uses them
 * never loads the client-only class at runtime. 26.3 switched from GLFW to SDL3, which
 * changed every value; compare against these names, never against raw numbers.
 */
public final class InputConstants {
    private InputConstants() {}

    // --- Mouse buttons (values match Minecraft) ---
    public static final int MOUSE_BUTTON_LEFT   = com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
    public static final int MOUSE_BUTTON_RIGHT  = com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
    public static final int MOUSE_BUTTON_MIDDLE = com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_MIDDLE;

    // --- Key codes (values match Minecraft) ---
    public static final int KEY_A             = com.mojang.blaze3d.platform.InputConstants.KEY_A;
    public static final int KEY_C             = com.mojang.blaze3d.platform.InputConstants.KEY_C;
    public static final int KEY_E             = com.mojang.blaze3d.platform.InputConstants.KEY_E;
    public static final int KEY_V             = com.mojang.blaze3d.platform.InputConstants.KEY_V;
    public static final int KEY_X             = com.mojang.blaze3d.platform.InputConstants.KEY_X;

    public static final int KEY_ESCAPE        = com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE;
    public static final int KEY_ENTER         = com.mojang.blaze3d.platform.InputConstants.KEY_RETURN;
    public static final int KEY_BACKSPACE     = com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE;
    public static final int KEY_DELETE        = com.mojang.blaze3d.platform.InputConstants.KEY_DELETE;
    public static final int KEY_RIGHT         = com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT;
    public static final int KEY_LEFT          = com.mojang.blaze3d.platform.InputConstants.KEY_LEFT;
    public static final int KEY_DOWN          = com.mojang.blaze3d.platform.InputConstants.KEY_DOWN;
    public static final int KEY_UP            = com.mojang.blaze3d.platform.InputConstants.KEY_UP;
    public static final int KEY_PAGE_UP       = com.mojang.blaze3d.platform.InputConstants.KEY_PAGEUP;
    public static final int KEY_PAGE_DOWN     = com.mojang.blaze3d.platform.InputConstants.KEY_PAGEDOWN;
    public static final int KEY_HOME          = com.mojang.blaze3d.platform.InputConstants.KEY_HOME;
    public static final int KEY_END           = com.mojang.blaze3d.platform.InputConstants.KEY_END;

    public static final int KEY_F3            = com.mojang.blaze3d.platform.InputConstants.KEY_F3;
    public static final int KEY_F4            = com.mojang.blaze3d.platform.InputConstants.KEY_F4;
    public static final int KEY_F5            = com.mojang.blaze3d.platform.InputConstants.KEY_F5;
    public static final int KEY_F6            = com.mojang.blaze3d.platform.InputConstants.KEY_F6;

    public static final int KEY_KP_ENTER      = com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER;

    public static final int KEY_LEFT_SHIFT    = com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT;
    public static final int KEY_LEFT_CONTROL  = com.mojang.blaze3d.platform.InputConstants.KEY_LCONTROL;
    public static final int KEY_LEFT_ALT      = com.mojang.blaze3d.platform.InputConstants.KEY_LALT;
    public static final int KEY_SPACE         = com.mojang.blaze3d.platform.InputConstants.KEY_SPACE;

    /**
     * Maps a key press to the key that Ctrl shortcuts (select all, copy, cut, paste) should test, following the
     * printed letter of the user's keyboard layout like vanilla's EditBox. Switch on the result and compare with
     * {@link #KEY_A}, {@link #KEY_C}, {@link #KEY_V} and {@link #KEY_X}.
     * <p>
     * Up to 26.2 the GLFW key code is already layout-aware, so it is returned unchanged. 26.3 (SDL3) delivers the
     * physical key as {@code keyCode} and the layout key code as {@code scanCode}.
     *
     * @param keyCode  the key code received in {@code keyPressed}
     * @param scanCode the scan code received in {@code keyPressed} (26.3+: the layout key code)
     * @return the key to test shortcuts against; {@code -1} if the physical A/C/V/X position carries another letter
     */
    public static int shortcutKey(int keyCode, int scanCode) {
        //return keyCode; // mc<=26.2
        return switch (scanCode) { case 'a' -> KEY_A; case 'c' -> KEY_C; case 'v' -> KEY_V; case 'x' -> KEY_X; default -> keyCode == KEY_A || keyCode == KEY_C || keyCode == KEY_V || keyCode == KEY_X ? -1 : keyCode; }; // mc>=26.3
    }
}
