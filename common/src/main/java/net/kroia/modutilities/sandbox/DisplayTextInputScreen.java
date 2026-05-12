package net.kroia.modutilities.sandbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A small popup screen for typing text into a TextBox on a DisplayDemoBlock.
 * Opens when the player right-clicks a TextBox element on the display.
 * On Enter or ESC, sends the text to the server block entity.
 */
@Environment(EnvType.CLIENT)
public class DisplayTextInputScreen extends Screen {

    private final BlockPos controllerPos;
    private final String initialText;
    private EditBox editBox;

    public DisplayTextInputScreen(BlockPos controllerPos, String initialText) {
        super(Component.literal("Display Text Input"));
        this.controllerPos = controllerPos;
        this.initialText = initialText;
    }

    public static void open(BlockPos controllerPos, String currentText) {
        Minecraft.getInstance().setScreen(new DisplayTextInputScreen(controllerPos, currentText));
    }

    @Override
    protected void init() {
        int boxWidth = Math.min(300, this.width - 40);
        int boxX = (this.width - boxWidth) / 2;
        int boxY = this.height / 2 - 10;

        editBox = new EditBox(this.font, boxX, boxY, boxWidth, 20, Component.literal("Text Input"));
        editBox.setMaxLength(100);
        editBox.setValue(initialText);
        editBox.setFocused(true);
        this.addRenderableWidget(editBox);
        this.setFocused(editBox);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Semi-transparent dark overlay
        guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Label above the text box
        guiGraphics.drawCenteredString(this.font, "Type and press Enter to confirm:",
                this.width / 2, this.height / 2 - 25, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // ENTER or KP_ENTER
            submitAndClose();
            return true;
        }
        if (keyCode == 256) { // ESC
            submitAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void submitAndClose() {
        String text = editBox.getValue();
        sendTextToServer(text);
        this.onClose();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    private void sendTextToServer(String text) {
        // For integrated server PoC: access server block entity directly
        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (server == null || mc.level == null) return;

        var serverLevel = server.getLevel(mc.level.dimension());
        if (serverLevel == null) return;

        BlockEntity be = serverLevel.getBlockEntity(controllerPos);
        if (be instanceof DisplayDemoBlockEntity controller && controller.isController()) {
            server.execute(() -> controller.handleTextInput(text));
        }
    }
}
