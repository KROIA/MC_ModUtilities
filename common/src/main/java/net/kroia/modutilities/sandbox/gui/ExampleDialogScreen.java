package net.kroia.modutilities.sandbox.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiScreen;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.Frame;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.network.chat.Component;

/**
 * Usecase example: a modal confirmation dialog.
 *
 * The screen displays a base "page" with a button that opens an
 * overlay {@link Frame} containing a question and OK / Cancel
 * buttons. While the dialog is enabled, the trigger button is
 * disabled and a tinted backdrop is rendered to focus attention.
 *
 * Demonstrates:
 *  - Layered UI built with a single Frame acting as a modal panel
 *  - Toggling enabled state to make a section "blocking"
 *  - Wiring a callback (Runnable) to the OK button
 *
 * Open via: /modutilities openExample dialog
 */
@Environment(EnvType.CLIENT)
public class ExampleDialogScreen extends GuiScreen {

    private final Label title;
    private final Label resultLabel;
    private final Button triggerButton;

    private final Frame backdrop;
    private final Frame dialog;
    private final Label dialogTitle;
    private final Label dialogMessage;
    private final Button okButton;
    private final Button cancelButton;

    public ExampleDialogScreen() {
        super(Component.literal("Dialog Example"));

        title = new Label("Modal Dialog Demo");
        title.setAlignment(GuiElement.Alignment.CENTER);
        title.setTextFontScale(1.5f);
        addElement(title);

        resultLabel = new Label("No action taken yet.");
        resultLabel.setAlignment(GuiElement.Alignment.CENTER);
        addElement(resultLabel);

        triggerButton = new Button("Delete world (just kidding)", this::showDialog);
        addElement(triggerButton);

        // Backdrop covers the whole screen with a translucent overlay so the
        // user perceives the dialog as modal.
        backdrop = new Frame();
        backdrop.setBackgroundColor(0xAA000000);
        backdrop.setEnableOutline(false);
        backdrop.setEnabled(false);
        addElement(backdrop);

        dialog = new Frame();
        addElement(dialog);
        dialog.setEnabled(false);

        dialogTitle = new Label("Confirm");
        dialogTitle.setAlignment(GuiElement.Alignment.CENTER);
        dialogTitle.setTextFontScale(1.3f);

        dialogMessage = new Label("Are you really sure you want to do that?");
        dialogMessage.setAlignment(GuiElement.Alignment.CENTER);

        okButton = new Button("OK", this::onConfirm);
        cancelButton = new Button("Cancel", this::onCancel);

        dialog.addChild(dialogTitle);
        dialog.addChild(dialogMessage);
        dialog.addChild(okButton);
        dialog.addChild(cancelButton);
    }

    public static void open() {
        GuiScreen.setScreen(new ExampleDialogScreen());
    }

    private void showDialog() {
        backdrop.setEnabled(true);
        dialog.setEnabled(true);
        triggerButton.setClickable(false);
    }

    private void hideDialog() {
        backdrop.setEnabled(false);
        dialog.setEnabled(false);
        triggerButton.setClickable(true);
    }

    private void onConfirm() {
        ModUtilitiesMod.LOGGER.info("[Example/Dialog] User confirmed.");
        resultLabel.setText("You clicked OK.");
        resultLabel.setTextColor(0xFF55FF55);
        hideDialog();
    }

    private void onCancel() {
        ModUtilitiesMod.LOGGER.info("[Example/Dialog] User cancelled.");
        resultLabel.setText("You clicked Cancel.");
        resultLabel.setTextColor(0xFFFFAA55);
        hideDialog();
    }

    @Override
    protected void updateLayout(Gui gui) {
        int w = getWidth();
        int h = getHeight();

        title.setBounds(0, 30, w, 22);
        resultLabel.setBounds(0, title.getBottom() + 8, w, 16);
        triggerButton.setBounds(w / 2 - 110, resultLabel.getBottom() + 16, 220, 22);

        // Backdrop fills the whole screen.
        backdrop.setBounds(0, 0, w, h);

        // Dialog panel: 240 x 110, centered.
        int dw = 240;
        int dh = 110;
        dialog.setBounds(w / 2 - dw / 2, h / 2 - dh / 2, dw, dh);

        dialogTitle.setBounds(0, 6, dw, 18);
        dialogMessage.setBounds(8, dialogTitle.getBottom() + 6, dw - 16, 30);

        int btnW = 80;
        int btnH = 22;
        int btnY = dh - btnH - 8;
        okButton.setBounds(dw / 2 - btnW - 6, btnY, btnW, btnH);
        cancelButton.setBounds(dw / 2 + 6, btnY, btnW, btnH);
    }
}
