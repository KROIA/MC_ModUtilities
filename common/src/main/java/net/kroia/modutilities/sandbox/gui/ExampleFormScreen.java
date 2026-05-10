package net.kroia.modutilities.sandbox.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiScreen;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.TextBox;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.network.chat.Component;

/**
 * Usecase example: a simple form with three fields and validation feedback.
 *
 * Demonstrates:
 *  - Combining Labels + TextBoxes into a labelled form
 *  - Numeric regex validation via {@link TextBox#createRegex_onlyNumerical}
 *  - Live validation message updated through a status Label
 *  - A Button that performs final validation and reports the result
 *
 * Open via: /modutilities openExample form
 */
@Environment(EnvType.CLIENT)
public class ExampleFormScreen extends GuiScreen {

    private static final int FIELD_HEIGHT = 18;
    private static final int FIELD_SPACING = 6;
    private static final int LABEL_WIDTH = 90;
    private static final int FIELD_WIDTH = 160;

    private final Label title;
    private final Label nameLabel;
    private final TextBox nameField;
    private final Label ageLabel;
    private final TextBox ageField;
    private final Label emailLabel;
    private final TextBox emailField;
    private final Label statusLabel;
    private final Button submitButton;

    public ExampleFormScreen() {
        super(Component.literal("Form Example"));

        title = new Label("User Registration");
        title.setAlignment(GuiElement.Alignment.CENTER);
        title.setTextFontScale(1.5f);

        nameLabel = new Label("Name:");
        nameField = new TextBox();
        nameField.setMaxChars(32);
        nameField.setOnTextChanged(s -> validateLive());

        ageLabel = new Label("Age:");
        ageField = new TextBox();
        // Only positive integers, max 3 digits.
        ageField.setMatchRegex(TextBox.createRegex_onlyNumerical(true, false, 3, 0));
        ageField.setOnTextChanged(s -> validateLive());

        emailLabel = new Label("Email:");
        emailField = new TextBox();
        emailField.setMaxChars(64);
        emailField.setOnTextChanged(s -> validateLive());

        statusLabel = new Label("");
        statusLabel.setAlignment(GuiElement.Alignment.CENTER);

        submitButton = new Button("Submit", this::onSubmit);

        addElement(title);
        addElement(nameLabel);
        addElement(nameField);
        addElement(ageLabel);
        addElement(ageField);
        addElement(emailLabel);
        addElement(emailField);
        addElement(statusLabel);
        addElement(submitButton);

        validateLive();
    }

    public static void open() {
        GuiScreen.setScreen(new ExampleFormScreen());
    }

    private void validateLive() {
        String result = validate();
        if (result == null) {
            statusLabel.setText("Looks good - press Submit to confirm.");
            statusLabel.setTextColor(0xFF55FF55);
        } else {
            statusLabel.setText(result);
            statusLabel.setTextColor(0xFFFF7777);
        }
    }

    /**
     * @return null if the form is valid, otherwise an error message.
     */
    private String validate() {
        if (nameField.getText().trim().isEmpty()) {
            return "Name must not be empty.";
        }
        String ageText = ageField.getText().trim();
        if (ageText.isEmpty()) {
            return "Age is required.";
        }
        int age = ageField.getInt();
        if (age <= 0 || age > 200) {
            return "Age must be between 1 and 200.";
        }
        String email = emailField.getText().trim();
        if (!email.contains("@") || !email.contains(".")) {
            return "Email must contain '@' and '.'";
        }
        return null;
    }

    private void onSubmit() {
        String error = validate();
        if (error != null) {
            statusLabel.setText("Cannot submit: " + error);
            statusLabel.setTextColor(0xFFFF5555);
            return;
        }
        ModUtilitiesMod.LOGGER.info("[Example/Form] Submitted: name='{}', age={}, email='{}'",
                nameField.getText(), ageField.getInt(), emailField.getText());
        statusLabel.setText("Submitted! Check the log for output.");
        statusLabel.setTextColor(0xFF55FFFF);
    }

    @Override
    protected void updateLayout(Gui gui) {
        int totalWidth = LABEL_WIDTH + 4 + FIELD_WIDTH;
        int x = (getWidth() - totalWidth) / 2;
        int y = Math.max(20, getHeight() / 2 - 80);

        title.setBounds(x, y, totalWidth, 22);
        y = title.getBottom() + FIELD_SPACING;

        nameLabel.setBounds(x, y, LABEL_WIDTH, FIELD_HEIGHT);
        nameField.setBounds(x + LABEL_WIDTH + 4, y, FIELD_WIDTH, FIELD_HEIGHT);
        y = nameField.getBottom() + FIELD_SPACING;

        ageLabel.setBounds(x, y, LABEL_WIDTH, FIELD_HEIGHT);
        ageField.setBounds(x + LABEL_WIDTH + 4, y, FIELD_WIDTH, FIELD_HEIGHT);
        y = ageField.getBottom() + FIELD_SPACING;

        emailLabel.setBounds(x, y, LABEL_WIDTH, FIELD_HEIGHT);
        emailField.setBounds(x + LABEL_WIDTH + 4, y, FIELD_WIDTH, FIELD_HEIGHT);
        y = emailField.getBottom() + FIELD_SPACING * 2;

        statusLabel.setBounds(x, y, totalWidth, 16);
        y = statusLabel.getBottom() + FIELD_SPACING;

        submitButton.setBounds(x + totalWidth / 2 - 60, y, 120, 22);
    }
}
