package net.kroia.modutilities.sandbox.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.client.GuiScreen;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.CheckBox;
import net.kroia.modutilities.gui.elements.DropDownMenu;
import net.kroia.modutilities.gui.elements.ExpandablePanel;
import net.kroia.modutilities.gui.elements.HorizontalSlider;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.network.chat.Component;

/**
 * Usecase example: a settings/configuration screen.
 *
 * Demonstrates:
 *  - {@link HorizontalSlider} bound to a numeric value with live label feedback
 *  - {@link CheckBox} for boolean toggles
 *  - {@link DropDownMenu} populated with options and a selection callback
 *  - Mixing element types in a labelled grid layout
 *
 * Open via: /modutilities openExample settings
 */
@Environment(EnvType.CLIENT)
public class ExampleSettingsScreen extends GuiScreen {

    private static final String[] DIFFICULTY_OPTIONS = {"Peaceful", "Easy", "Normal", "Hard"};

    // Backing values
    private double volume = 0.6;
    private boolean musicEnabled = true;
    private boolean fullscreenEnabled = false;
    private int difficultyIndex = 2;

    private final Label title;
    private final Label volumeLabel;
    private final HorizontalSlider volumeSlider;
    private final Label volumeValueLabel;
    private final CheckBox musicCheckBox;
    private final CheckBox fullscreenCheckBox;
    private final Label difficultyLabel;
    private final DropDownMenu difficultyDropDown;
    private final ExpandablePanel advancedPanel;
    private final Button applyButton;

    public ExampleSettingsScreen() {
        super(Component.literal("Settings Example"));

        title = new Label("Settings");
        title.setAlignment(GuiElement.Alignment.CENTER);
        title.setTextFontScale(1.5f);
        addElement(title);

        volumeLabel = new Label("Volume:");
        addElement(volumeLabel);

        volumeSlider = new HorizontalSlider();
        volumeSlider.setSliderValue(volume);

        volumeSlider.setTooltipSupplier(() -> formatPercent(volume));
        addElement(volumeSlider);

        volumeValueLabel = new Label(formatPercent(volume));
        volumeValueLabel.setAlignment(GuiElement.Alignment.LEFT);
        addElement(volumeValueLabel);
        volumeSlider.setOnValueChanged(v -> {
            volume = v;
            volumeValueLabel.setText(formatPercent(v));
        });

        musicCheckBox = new CheckBox("Enable music", checked -> {
            musicEnabled = checked;
        });
        musicCheckBox.setChecked(musicEnabled);
        musicCheckBox.setTextAlignment(GuiElement.Alignment.LEFT);
        addElement(musicCheckBox);

        fullscreenCheckBox = new CheckBox("Fullscreen", checked -> {
            fullscreenEnabled = checked;
        });
        fullscreenCheckBox.setChecked(fullscreenEnabled);
        fullscreenCheckBox.setTextAlignment(GuiElement.Alignment.LEFT);
        addElement(fullscreenCheckBox);

        difficultyLabel = new Label("Difficulty:");
        addElement(difficultyLabel);

        difficultyDropDown = new DropDownMenu(DIFFICULTY_OPTIONS[difficultyIndex], this::onDropdownMenuSelected);
        for (String option : DIFFICULTY_OPTIONS) {
            difficultyDropDown.addOption(option);
        }
        addElement(difficultyDropDown);

        // Collapsible "Advanced" section holding extra option rows.
        advancedPanel = new ExpandablePanel("Advanced", false);
        advancedPanel.addChild(new CheckBox("Reduce particles"));
        advancedPanel.addChild(new CheckBox("Show debug overlay"));
        Label hint = new Label("These options require a restart.");
        hint.setHeight(16);
        advancedPanel.addChild(hint);
        addElement(advancedPanel);

        applyButton = new Button("Apply", this::onApply);
        addElement(applyButton);
    }

    private void onDropdownMenuSelected(Integer index, GuiElement guiElement) {
        difficultyIndex = index;
        difficultyDropDown.setLabelText(DIFFICULTY_OPTIONS[index]);
        difficultyDropDown.collapse();
    }

    public static void open() {
        GuiScreen.setScreen(new ExampleSettingsScreen());
    }

    private static String formatPercent(double v) {
        return Math.round(v * 100) + "%";
    }

    private void onApply() {
        ModUtilitiesMod.LOGGER.info(
                "[Example/Settings] Applied: volume={} music={} fullscreen={} difficulty={}",
                formatPercent(volume), musicEnabled, fullscreenEnabled, DIFFICULTY_OPTIONS[difficultyIndex]);
    }

    @Override
    protected void updateLayout(Gui gui) {
        int rowHeight = 20;
        int spacing = 6;
        int labelWidth = 90;
        int controlWidth = 180;
        int totalWidth = labelWidth + 6 + controlWidth + 6 + 50;
        int x = (getWidth() - totalWidth) / 2;
        int y = Math.max(20, getHeight() / 2 - 90);

        title.setBounds(x, y, totalWidth, 22);
        y = title.getBottom() + spacing * 2;

        volumeLabel.setBounds(x, y, labelWidth, rowHeight);
        volumeSlider.setBounds(x + labelWidth + 6, y + 4, controlWidth, rowHeight - 8);
        volumeValueLabel.setBounds(volumeSlider.getRight() + 6, y, 50, rowHeight);
        y += rowHeight + spacing;

        musicCheckBox.setBounds(x, y, labelWidth + controlWidth, rowHeight);
        y += rowHeight + spacing;

        fullscreenCheckBox.setBounds(x, y, labelWidth + controlWidth, rowHeight);
        y += rowHeight + spacing;

        difficultyLabel.setBounds(x, y, labelWidth, rowHeight);
        // Note: setBounds on a DropDownMenu in unexpanded state. Expanded list grows downward.
        difficultyDropDown.setBounds(x + labelWidth + 6, y, controlWidth, rowHeight);
        y += rowHeight + spacing * 2;

        // Note: the panel grows downward when expanded; content overlaps the
        // button below, matching the DropDownMenu caveat above (demo only).
        advancedPanel.setBounds(x, y, labelWidth + controlWidth, advancedPanel.getHeaderHeight());
        y += advancedPanel.getHeaderHeight() + spacing * 2;

        applyButton.setBounds(x + totalWidth / 2 - 50, y, 100, 22);
    }
}
