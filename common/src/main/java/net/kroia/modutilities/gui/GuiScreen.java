package net.kroia.modutilities.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics; // mc>=1.20.1
//import com.mojang.blaze3d.vertex.PoseStack; // mc<=1.19.4
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;


@Environment(EnvType.CLIENT)
public abstract class GuiScreen extends Screen {

    protected final Gui gui;
    protected final Screen parent;
    protected boolean debugKeysEnabled = true;
    protected boolean enableGizmos = false;
    protected boolean enableBackground = true;
    protected boolean enableForeground = true;
    protected boolean enableTooltip = true; // Enable tooltip rendering by default
    protected GuiScreen(Component pTitle) {
        super(pTitle);
        this.gui = new Gui(this);
        this.parent = null;
    }
    protected GuiScreen(Component pTitle, Screen parent) {
        super(pTitle);
        this.gui = new Gui(this);
        this.parent = parent;
    }

    public static void setScreen(Screen screen)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.screen != screen)
        {
            mc.setScreen(screen);
        }
    }
    protected boolean isInitialized = false;
    public void setDebugKeysEnabled(boolean debugKeysEnabled) {
        this.debugKeysEnabled = debugKeysEnabled;
    }
    public boolean isDebugKeysEnabled() {
        return debugKeysEnabled;
    }
    public void setEnableGizmos(boolean enableGizmos) {
        this.enableGizmos = enableGizmos;
    }
    public boolean isEnableGizmos() {
        return enableGizmos;
    }
    public void setEnableBackground(boolean enableBackground) {
        this.enableBackground = enableBackground;
    }
    public boolean isEnableBackground() {
        return enableBackground;
    }
    public void setEnableForeground(boolean enableForeground) {
        this.enableForeground = enableForeground;
    }
    public boolean isEnableForeground() {
        return enableForeground;
    }
    public void setEnableTooltip(boolean enableTooltip) {
        this.enableTooltip = enableTooltip;
    }
    public boolean isEnableTooltip() {
        return enableTooltip;
    }
    public Gui getGui() {
        return gui;
    }

    public float getGuiScale() {
        return gui.getGuiScale();
    }
    public void setGuiScale(float guiScale) {
        gui.setGuiScale(guiScale);
    }

    @Override
    public final void init() {
        super.init();
        gui.init();
        updateLayout(gui);
        isInitialized = true;
    }
    @Override
    protected void rebuildWidgets() {
        super.rebuildWidgets();
    }
    public boolean isInitialized()
    {
        return isInitialized;
    }

    protected abstract void updateLayout(Gui gui);

    protected void addElement(GuiElement element) {
        gui.addElement(element);
    }
    protected void removeElement(GuiElement element) {
        gui.removeElement(element);
    }
    protected void removeAllElements() {
        gui.removeAllElements();
    }
    protected GuiElement getFocusedElement() {
        return gui.getFocusedElement();
    }
    public List<GuiElement> getElements() {
        return gui.getElements();
    }

    protected int getWidth() {
        return (int)((float)width*gui.getInvGuiScale());
    }
    protected int getHeight() {
        return (int)((float)height*gui.getInvGuiScale());
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void close()
    {
        this.onClose();
    }

    @Override
    public void onClose() {
        super.onClose();
        if(this.minecraft != null) {
            int mousePosX = getMouseX();
            int mousePosY = getMouseY();
            this.minecraft.setScreen(parent);
            setMousePos(mousePosX, mousePosY);
        }
    }


    // mc>=1.20.1
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if(this.minecraft == null || !enableBackground)
        {
            return;
        }
        gui.getGraphics().setGraphics(guiGraphics);
        super.renderBackground(guiGraphics, pMouseX, pMouseY, pPartialTick);
        gui.renderBackground();
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        gui.getGraphics().setGraphics(pGuiGraphics);
        gui.storeMousePos(pMouseX,pMouseY);
        gui.setPartialTick(pPartialTick);
        if(enableBackground)
            this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        if(enableForeground)
            gui.render();
        if(enableTooltip)
            gui.renderTooltip();
        if(enableGizmos)
            gui.renderGizmos();
    }


    /*
    // mc<=1.19.4
    @Override
    public void renderBackground(PoseStack guiGraphics) {
        if(this.minecraft == null)
        {
            return;
        }
        gui.getGraphics().setGraphics(guiGraphics);
        super.renderBackground(guiGraphics);
        gui.renderBackground();
    }

    @Override
    public void render(PoseStack pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        gui.getGraphics().setGraphics(pGuiGraphics);
        gui.setMousePos(pMouseX, pMouseY);
        gui.setPartialTick(pPartialTick);
        this.renderBackground(pGuiGraphics);
        gui.render();
        gui.renderTooltip();
        if(enableGizmos)
            gui.renderGizmos();
    }
    */

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!gui.mouseClicked(mouseX, mouseY, button))
            return super.mouseClicked(mouseX, mouseY, button);
        return true;
    }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return gui.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return gui.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double scrollY) {
        return gui.mouseScrolled(mouseX, mouseY, scrollY);
    }

    public int getMouseX() {
        return gui.getMousePosX();
    }
    public int getMouseY() {
        return gui.getMousePosY();
    }
    public Point getMousePos() {
        return new Point(gui.getMousePosX(), gui.getMousePosY());
    }

    /**
     * Checks if the the keyboard key is pressed down.
     * @see GLFW.GLFW_KEY_SPACE... Keys
     *
     * @return true if the given key is pressed
     */
    protected boolean isKeyPressed(int keyCode)
    {
        return GLFW.glfwGetKey(gui.getWindowHandle(), keyCode) == GLFW.GLFW_PRESS;
    }
    protected boolean isControlPressed()
    {
        return GLFW.glfwGetKey(gui.getWindowHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;
    }
    protected boolean isShiftPressed()
    {
        return GLFW.glfwGetKey(gui.getWindowHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
    }
    protected boolean isAltPressed()
    {
        return GLFW.glfwGetKey(gui.getWindowHandle(), GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
    }

    public void setMousePos(int x, int y)
    {
        gui.moveMouseToPos(x, y);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        if(debugKeysEnabled) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_F3: {
                    enableGizmos = !enableGizmos;
                    return true;
                }
                case GLFW.GLFW_KEY_F4: {
                    enableBackground = !enableBackground;
                    return true;
                }
                case GLFW.GLFW_KEY_F5: {
                    enableForeground = !enableForeground;
                    return true;
                }
                case GLFW.GLFW_KEY_F6: {
                    enableTooltip = !enableTooltip;
                    return true;
                }
            }
        }

        boolean ret = gui.keyPressed(keyCode, scanCode, modifiers);
        if(ret)
            return true;
        if(gui.getFocusedElement() == null) {
            ret = super.keyPressed(keyCode, scanCode, modifiers);
            if(ret)
                return true;
        }
        if(keyCode == GLFW.GLFW_KEY_E) {
            onClose();
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if(!gui.charTyped(codePoint, modifiers))
            return super.charTyped(codePoint, modifiers);
        return true;
    }


}