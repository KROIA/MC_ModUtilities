package net.kroia.modutilities.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public abstract class GuiScreen extends Screen {

    protected final Gui gui;
    protected boolean enableGizmos = false;
    protected GuiScreen(Component pTitle) {
        super(pTitle);
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
        updateLayout(gui);
        gui.init();
    }

    protected abstract void updateLayout(Gui gui);

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        super.renderBackground(guiGraphics);
        gui.renderBackground(guiGraphics);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        gui.setMousePos(pMouseX, pMouseY);
        gui.setPartialTick(pPartialTick);
        this.renderBackground(pGuiGraphics);
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
        // Check for F3 Key
        if(keyCode == GLFW.GLFW_KEY_F3) {
            enableGizmos = !enableGizmos;
            return true;
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