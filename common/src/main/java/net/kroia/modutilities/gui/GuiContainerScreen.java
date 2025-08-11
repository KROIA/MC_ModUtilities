package net.kroia.modutilities.gui;


import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.minecraft.client.gui.GuiGraphics; // mc>=1.20.1
//import com.mojang.blaze3d.vertex.PoseStack; // mc<=1.19.4

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.lwjgl.glfw.GLFW;


public abstract class GuiContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected final Gui gui;
    protected boolean enableGizmos = false;
    protected boolean enableBackground = true;
    protected boolean enableForeground = true;
    protected boolean enableTooltip = true; // Enable tooltip rendering by default
    protected boolean isInitialized = false;
    public GuiContainerScreen(T pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.gui = new Gui(this);
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


    // mc>=1.20.1
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        gui.getGraphics().setGraphics(guiGraphics);
        renderBackground(guiGraphics);
        //gui.storeMousePos((int)((float)pMouseX*invGuiScale), (int)((float)pMouseY*invGuiScale));
        gui.setPartialTick(pPartialTick);
        gui.renderBackground();
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        gui.getGraphics().setGraphics(pGuiGraphics);
        gui.storeMousePos(pMouseX, pMouseY);
        if(enableBackground)
            this.renderBg(pGuiGraphics, pPartialTick, pMouseX, pMouseY);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return gui.mouseScrolled(mouseX, mouseY, delta);
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

    public void setMousePos(int x, int y)
    {
        gui.moveMouseToPos(x, y);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch(keyCode)
        {
            case GLFW.GLFW_KEY_F3:
            {
                enableGizmos = !enableGizmos;
                return true;
            }
            case GLFW.GLFW_KEY_F4:
            {
                enableBackground = !enableBackground;
                return true;
            }
            case GLFW.GLFW_KEY_F5:
            {
                enableForeground = !enableForeground;
                return true;
            }
            case GLFW.GLFW_KEY_F6:
            {
                enableTooltip = !enableTooltip;
                return true;
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
