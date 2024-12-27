package net.kroia.modutilities.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;


public abstract class GuiContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected final Gui gui;
    protected boolean enableGizmos = false;
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
    public Gui getGui() {
        return gui;
    }

    @Override
    public final void init() {
        super.init();
        gui.init();
    }

    public abstract void initGui(Gui gui);

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {

    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        gui.setMousePos(pMouseX, pMouseY);
        gui.setPartialTick(pPartialTick);
        gui.renderBackground(pGuiGraphics);
        gui.render(pGuiGraphics);
        if(enableGizmos)
            gui.renderGizmos();
    }

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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return gui.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
