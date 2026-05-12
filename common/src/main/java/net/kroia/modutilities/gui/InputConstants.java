package net.kroia.modutilities.gui;

/**
 * GLFW-free key and mouse button constants for use by common (server + client) GUI code.
 * <p>
 * Every value matches the corresponding {@code GLFW_*} constant so that key codes
 * received from Minecraft's input pipeline can be compared directly. This class
 * exists solely to avoid importing {@code org.lwjgl.glfw.GLFW} in common element
 * classes.
 */
public final class InputConstants {
    private InputConstants() {}

    // --- Mouse buttons (values match GLFW) ---
    public static final int MOUSE_BUTTON_LEFT   = 0;
    public static final int MOUSE_BUTTON_RIGHT  = 1;
    public static final int MOUSE_BUTTON_MIDDLE = 2;

    // --- Key codes (values match GLFW) ---
    public static final int KEY_A             = 65;
    public static final int KEY_C             = 67;
    public static final int KEY_E             = 69;
    public static final int KEY_V             = 86;
    public static final int KEY_X             = 88;

    public static final int KEY_ESCAPE        = 256;
    public static final int KEY_ENTER         = 257;
    public static final int KEY_BACKSPACE     = 259;
    public static final int KEY_DELETE        = 261;
    public static final int KEY_RIGHT         = 262;
    public static final int KEY_LEFT          = 263;

    public static final int KEY_F3            = 292;
    public static final int KEY_F4            = 293;
    public static final int KEY_F5            = 294;
    public static final int KEY_F6            = 295;

    public static final int KEY_KP_ENTER      = 335;

    public static final int KEY_LEFT_SHIFT    = 340;
    public static final int KEY_LEFT_CONTROL  = 341;
    public static final int KEY_LEFT_ALT      = 342;
}
