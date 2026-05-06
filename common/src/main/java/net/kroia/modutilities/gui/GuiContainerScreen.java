package net.kroia.modutilities.gui;


import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics; // mc>=1.20.1
//import com.mojang.blaze3d.vertex.PoseStack; // mc<=1.19.4

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.lwjgl.glfw.GLFW;


/**
 * Abstract Minecraft {@link AbstractContainerScreen} that hosts a {@link Gui}
 * root, intended for inventory-style screens backed by an
 * {@link AbstractContainerMenu}.
 * <p>
 * Subclasses provide their layout in {@link #updateLayout(Gui)}, which is
 * invoked by {@link #init()} after the Minecraft container screen state has
 * been set up. After {@link #init()} completes, {@link #isInitialized()} returns
 * {@code true}.
 * <p>
 * Mouse, keyboard and render callbacks are forwarded to the embedded
 * {@link Gui}; F3-F6 debug toggles are handled when
 * {@link #setDebugKeysEnabled(boolean) debug keys} are enabled.
 *
 * @param <T> the {@link AbstractContainerMenu} subtype this screen is bound to
 * @apiNote The whole {@code gui/} package is client-only
 *          ({@code @Environment(EnvType.CLIENT)}).
 */
public abstract class GuiContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected final Gui gui;
    protected final Screen parent;
    protected boolean debugKeysEnabled = true;
    protected boolean enableGizmos = false;
    protected boolean enableBackground = true;
    protected boolean enableForeground = true;
    protected boolean enableTooltip = true; // Enable tooltip rendering by default
    protected boolean isInitialized = false;

    /**
     * Creates a new {@code GuiContainerScreen} without a parent screen.
     *
     * @param pMenu             the container menu
     * @param pPlayerInventory  the player's inventory
     * @param pTitle            the screen title
     */
    public GuiContainerScreen(T pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.gui = new Gui(this);
        this.parent = null;
    }

    /**
     * Creates a new {@code GuiContainerScreen} that returns to the given parent
     * screen when closed.
     *
     * @param pMenu             the container menu
     * @param pPlayerInventory  the player's inventory
     * @param pTitle            the screen title
     * @param parent            the screen to display after this one is closed
     */
    protected GuiContainerScreen(T pMenu, Inventory pPlayerInventory, Component pTitle, Screen parent) {
        super(pMenu, pPlayerInventory, pTitle);
        this.gui = new Gui(this);
        this.parent = parent;
    }

    /**
     * Replaces Minecraft's currently active screen with the given one, but only
     * if it is not already active.
     *
     * @param screen the screen to display
     */
    public static void setScreen(Screen screen)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.screen != screen)
        {
            mc.setScreen(screen);
        }
    }

    /**
     * Sets whether the F3-F6 debug toggle keys are processed by this screen.
     *
     * @param debugKeysEnabled {@code true} to enable the debug toggles
     */
    public void setDebugKeysEnabled(boolean debugKeysEnabled) {
        this.debugKeysEnabled = debugKeysEnabled;
    }

    /**
     * @return {@code true} if F3-F6 debug toggle keys are processed
     */
    public boolean isDebugKeysEnabled() {
        return debugKeysEnabled;
    }

    /**
     * Sets whether the gizmo (debug overlay) render pass is performed.
     *
     * @param enableGizmos {@code true} to render gizmos
     */
    public void setEnableGizmos(boolean enableGizmos) {
        this.enableGizmos = enableGizmos;
    }

    /**
     * @return {@code true} if the gizmo render pass is enabled
     */
    public boolean isEnableGizmos() {
        return enableGizmos;
    }

    /**
     * Sets whether the background render pass is performed.
     *
     * @param enableBackground {@code true} to render backgrounds
     */
    public void setEnableBackground(boolean enableBackground) {
        this.enableBackground = enableBackground;
    }

    /**
     * @return {@code true} if the background render pass is enabled
     */
    public boolean isEnableBackground() {
        return enableBackground;
    }

    /**
     * Sets whether the foreground render pass is performed.
     *
     * @param enableForeground {@code true} to render foreground content
     */
    public void setEnableForeground(boolean enableForeground) {
        this.enableForeground = enableForeground;
    }

    /**
     * @return {@code true} if the foreground render pass is enabled
     */
    public boolean isEnableForeground() {
        return enableForeground;
    }

    /**
     * Sets whether the tooltip render pass is performed.
     *
     * @param enableTooltip {@code true} to render tooltips
     */
    public void setEnableTooltip(boolean enableTooltip) {
        this.enableTooltip = enableTooltip;
    }

    /**
     * @return {@code true} if the tooltip render pass is enabled
     */
    public boolean isEnableTooltip() {
        return enableTooltip;
    }

    /**
     * @return the {@link Gui} root managed by this screen
     */
    public Gui getGui() {
        return gui;
    }

    /**
     * @return the GUI scale factor of the embedded {@link Gui}
     */
    public float getGuiScale() {
        return gui.getGuiScale();
    }

    /**
     * Sets the GUI scale factor of the embedded {@link Gui}.
     *
     * @param guiScale the new scale factor
     */
    public void setGuiScale(float guiScale) {
        gui.setGuiScale(guiScale);
    }

    /**
     * Initializes the screen. Calls {@code super.init()}, initializes the
     * embedded {@link Gui}, invokes {@link #updateLayout(Gui)}, and finally
     * marks this screen as initialized.
     */
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

    /**
     * @return {@code true} once {@link #init()} has finished, indicating that
     *         the embedded {@link Gui} is ready to be rendered and interacted with
     */
    public boolean isInitialized()
    {
        return isInitialized;
    }

    /**
     * Subclass hook invoked at the end of {@link #init()} to populate or
     * re-arrange the embedded {@link Gui}.
     *
     * @param gui the embedded GUI root to lay out
     */
    protected abstract void updateLayout(Gui gui);

    /**
     * Adds a top-level element to the embedded {@link Gui}.
     *
     * @param element the element to add
     */
    protected void addElement(GuiElement element) {
        gui.addElement(element);
    }

    /**
     * Removes a top-level element from the embedded {@link Gui}.
     *
     * @param element the element to remove
     */
    protected void removeElement(GuiElement element) {
        gui.removeElement(element);
    }

    /**
     * @return the screen width converted to GUI element coordinates
     */
    protected int getWidth() {
        return (int)((float)width*gui.getInvGuiScale());
    }

    /**
     * @return the screen height converted to GUI element coordinates
     */
    protected int getHeight() {
        return (int)((float)height*gui.getInvGuiScale());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }


    /**
     * Convenience wrapper around {@link #onClose()}.
     */
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
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        gui.getGraphics().setGraphics(guiGraphics);
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        //gui.storeMousePos((int)((float)pMouseX*invGuiScale), (int)((float)pMouseY*invGuiScale));
        gui.setPartialTick(partialTick);
        gui.renderBackground();
    }
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {

    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        gui.getGraphics().setGraphics(pGuiGraphics);
        gui.storeMousePos(pMouseX, pMouseY);
        if(enableBackground)
            renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        gui.setPartialTick(pPartialTick);
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
    protected void renderBg(PoseStack guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        gui.getGraphics().setGraphics(guiGraphics);
        renderBackground(guiGraphics);
        gui.setMousePos(pMouseX, pMouseY);
        gui.setPartialTick(pPartialTick);
        gui.renderBackground();
    }

    @Override
    public void render(PoseStack pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        gui.getGraphics().setGraphics(pGuiGraphics);
        renderBg(pGuiGraphics, pPartialTick, pMouseX, pMouseY);
        gui.setMousePos(pMouseX, pMouseY);
        gui.setPartialTick(pPartialTick);
        gui.render();
        gui.renderTooltip();
        if(enableGizmos)
            gui.renderGizmos();
    }
    */

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!gui.mouseClicked(mouseX, mouseY, button))
            return false; //return super.mouseClicked(mouseX, mouseY, button);
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

    /**
     * @return the current mouse x position in GUI element coordinates
     */
    public int getMouseX() {
        return gui.getMousePosX();
    }

    /**
     * @return the current mouse y position in GUI element coordinates
     */
    public int getMouseY() {
        return gui.getMousePosY();
    }

    /**
     * @return the current mouse position as a {@link Point} in GUI element
     *         coordinates
     */
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
    /**
     * @return {@code true} if the left control key is currently pressed
     */
    protected boolean isControlPressed()
    {
        return GLFW.glfwGetKey(gui.getWindowHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;
    }

    /**
     * @return {@code true} if the left shift key is currently pressed
     */
    protected boolean isShiftPressed()
    {
        return GLFW.glfwGetKey(gui.getWindowHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
    }

    /**
     * @return {@code true} if the left alt key is currently pressed
     */
    protected boolean isAltPressed()
    {
        return GLFW.glfwGetKey(gui.getWindowHandle(), GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
    }

    /**
     * Moves the OS-level cursor to the given position in GUI coordinates.
     *
     * @param x the target x position
     * @param y the target y position
     */
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
        if(gui.getFocusedElement() == null)
            return super.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if(!gui.charTyped(codePoint, modifiers))
            return super.charTyped(codePoint, modifiers);
        return true;
    }


}
