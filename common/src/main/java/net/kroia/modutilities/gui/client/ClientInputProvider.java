package net.kroia.modutilities.gui.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.IInputProvider;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side {@link IInputProvider} backed by GLFW and the Minecraft keyboard handler.
 * <p>
 * Created by the hosting screen and injected into the {@link net.kroia.modutilities.gui.Gui}
 * so that common GUI element code can query input state without depending on GLFW directly.
 */
@Environment(EnvType.CLIENT)
public class ClientInputProvider implements IInputProvider {

    private final long windowHandle;

    /**
     * Creates a new provider bound to the given GLFW window.
     *
     * @param windowHandle the GLFW window handle (typically from
     *                     {@code Minecraft.getInstance().getWindow().getWindow()})
     */
    public ClientInputProvider(long windowHandle) {
        this.windowHandle = windowHandle;
    }

    @Override
    public boolean isKeyDown(int keyCode) {
        return GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean isMouseButtonDown(int button) {
        return GLFW.glfwGetMouseButton(windowHandle, button) == GLFW.GLFW_PRESS;
    }

    @Override
    public String getClipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    @Override
    public void setClipboard(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }

    @Override
    public void setCursorPos(double x, double y) {
        GLFW.glfwSetCursorPos(windowHandle, x, y);
    }
}
