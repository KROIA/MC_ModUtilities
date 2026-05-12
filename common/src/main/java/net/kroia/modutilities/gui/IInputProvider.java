package net.kroia.modutilities.gui;

/**
 * Abstraction over platform-specific input queries (key/mouse state, clipboard).
 * <p>
 * The default implementations return "nothing pressed / empty clipboard" so that
 * a {@link Gui} running on the server (or in a headless test) does not crash.
 * On the client, {@link net.kroia.modutilities.gui.client.ClientInputProvider}
 * supplies real GLFW + Minecraft keyboard handler implementations.
 */
public interface IInputProvider {

    /**
     * Polls the OS for whether a keyboard key is currently held.
     *
     * @param keyCode the key code (see {@link InputConstants})
     * @return {@code true} if the key is pressed
     */
    default boolean isKeyDown(int keyCode) { return false; }

    /**
     * Polls the OS for whether a mouse button is currently held.
     *
     * @param button the mouse button code (see {@link InputConstants})
     * @return {@code true} if the button is pressed
     */
    default boolean isMouseButtonDown(int button) { return false; }

    /**
     * Reads the system clipboard as a string.
     *
     * @return the clipboard contents, or {@code ""} if unavailable
     */
    default String getClipboard() { return ""; }

    /**
     * Writes a string to the system clipboard.
     *
     * @param text the text to store in the clipboard
     */
    default void setClipboard(String text) {}

    /**
     * Moves the OS-level cursor to the specified window coordinates.
     *
     * @param x the x coordinate in window pixels
     * @param y the y coordinate in window pixels
     */
    default void setCursorPos(double x, double y) {}
}
