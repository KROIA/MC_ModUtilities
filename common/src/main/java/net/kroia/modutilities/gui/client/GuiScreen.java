package net.kroia.modutilities.gui.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.InputConstants;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics; // mc>=1.20.1
//import com.mojang.blaze3d.vertex.PoseStack; // mc<=1.19.4
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;

import java.util.List;


/**
 * Abstract Minecraft {@link Screen} that hosts a {@link Gui} root.
 * <p>
 * Subclasses provide their layout in {@link #updateLayout(Gui)}, which is called
 * by {@link #init()} after the underlying screen state has been prepared. After
 * {@link #init()} completes, {@link #isInitialized()} returns {@code true}.
 * <p>
 * The screen forwards all mouse, keyboard and render callbacks from Minecraft
 * to the embedded {@link Gui}, and exposes a number of debug toggles that can
 * be controlled with the F3-F6 keys when {@link #setDebugKeysEnabled(boolean)
 * debug keys} are enabled.
 *
 * @apiNote This class is client-only ({@code @Environment(EnvType.CLIENT)}).
 */
@Environment(EnvType.CLIENT)
public abstract class GuiScreen extends Screen {

    protected final Gui gui;
    protected final Screen parent;
    private final ClientGraphics clientGraphics;
    protected boolean debugKeysEnabled = true;
    protected boolean enableGizmos = false;
    protected boolean enableBackground = true;
    protected boolean enableForeground = true;
    protected boolean enableTooltip = true; // Enable tooltip rendering by default

    /**
     * Creates a new {@code GuiScreen} without a parent screen. Closing this
     * screen will return to no screen at all.
     *
     * @param pTitle the screen title
     */
    protected GuiScreen(Component pTitle) {
        super(pTitle);
        this.gui = new Gui();
        this.parent = null;
        this.clientGraphics = new ClientGraphics();
        injectClientDependencies();
    }

    /**
     * Creates a new {@code GuiScreen} that returns to the given parent screen
     * when closed.
     *
     * @param pTitle the screen title
     * @param parent the screen to display after this one is closed
     */
    protected GuiScreen(Component pTitle, Screen parent) {
        super(pTitle);
        this.gui = new Gui();
        this.parent = parent;
        this.clientGraphics = new ClientGraphics();
        injectClientDependencies();
    }

    /**
     * Injects client-only backends into the embedded Gui.
     */
    private void injectClientDependencies() {
        gui.setGraphicsBackend(clientGraphics);
        gui.setInputProvider(new ClientInputProvider(Minecraft.getInstance().getWindow().getWindow()));
        gui.setFont(Minecraft.getInstance().font);
        gui.setDisplayScaleFactor(Minecraft.getInstance().getWindow().getGuiScale());
        gui.setSoundPlayer((sound, volume, pitch) -> playLocalSound(sound, volume, pitch));
        clientGraphics.setFont(Minecraft.getInstance().font);
    }

    /**
     * Returns the client graphics backend used by the embedded Gui.
     *
     * @return the {@link ClientGraphics} instance
     */
    public ClientGraphics getClientGraphics() {
        return clientGraphics;
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

    protected boolean isInitialized = false;

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
        // Re-inject display scale (may have changed on resize)
        gui.setDisplayScaleFactor(Minecraft.getInstance().getWindow().getGuiScale());
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
     * Removes every top-level element from the embedded {@link Gui}.
     */
    protected void removeAllElements() {
        gui.removeAllElements();
    }

    /**
     * @return the currently focused element of the embedded {@link Gui}, or
     *         {@code null} if no element has focus
     */
    protected GuiElement getFocusedElement() {
        return gui.getFocusedElement();
    }

    /**
     * @return the live list of top-level elements on the embedded {@link Gui}
     */
    public List<GuiElement> getElements() {
        return gui.getElements();
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
    public void renderBackground(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if(this.minecraft == null || !enableBackground)
        {
            return;
        }
        clientGraphics.setGraphics(guiGraphics);
        super.renderBackground(guiGraphics, pMouseX, pMouseY, pPartialTick);
        gui.renderBackground();
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        clientGraphics.setGraphics(pGuiGraphics);
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
        clientGraphics.setGraphics(guiGraphics);
        super.renderBackground(guiGraphics);
        gui.renderBackground();
    }

    @Override
    public void render(PoseStack pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        clientGraphics.setGraphics(pGuiGraphics);
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
     *
     * @return true if the given key is pressed
     */
    protected boolean isKeyPressed(int keyCode)
    {
        return gui.getInputProvider().isKeyDown(keyCode);
    }
    /**
     * @return {@code true} if the left control key is currently pressed
     */
    protected boolean isControlPressed()
    {
        return gui.getInputProvider().isKeyDown(InputConstants.KEY_LEFT_CONTROL);
    }

    /**
     * @return {@code true} if the left shift key is currently pressed
     */
    protected boolean isShiftPressed()
    {
        return gui.getInputProvider().isKeyDown(InputConstants.KEY_LEFT_SHIFT);
    }

    /**
     * @return {@code true} if the left alt key is currently pressed
     */
    protected boolean isAltPressed()
    {
        return gui.getInputProvider().isKeyDown(InputConstants.KEY_LEFT_ALT);
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

    /**
     * @return the Minecraft window's reported GUI scale factor (1, 2, 3, ...)
     */
    public static double getMinecraftGuiScale()
    {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    /**
     * Plays a sound at the local player's position. Has no effect if no level or
     * player is currently available (e.g. on the main menu).
     *
     * @param sound  the sound event to play
     * @param volume the volume multiplier
     * @param pitch  the pitch multiplier
     */
    public static void playLocalSound(SoundEvent sound, float volume, float pitch)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        minecraft.level.playLocalSound(
                minecraft.player.getX(),
                minecraft.player.getY(),
                minecraft.player.getZ(),
                sound,
                SoundSource.PLAYERS,
                volume,
                pitch,
                false
        );
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
            if(keyCode == GLFW.GLFW_KEY_E) {
                onClose();
                return true;
            }
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
