package net.kroia.modutilities.sandbox;

import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.display.AbstractDisplayBlockEntity;
import net.kroia.modutilities.gui.display.ContentBuilder;
import net.kroia.modutilities.gui.display.DisplayConfig;
import net.kroia.modutilities.UtilitiesPlatform;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.CheckBox;
import net.kroia.modutilities.gui.elements.ItemView;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.TextBox;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the flat-panel display demo. Uses {@link DisplayConfig#flatPanel()}
 * and a "panel" channel ID so it won't merge with full-block displays. Demonstrates
 * a simple status panel with a click counter, checkbox, and text input.
 */
public class DisplayDemoPanelBlockEntity extends AbstractDisplayBlockEntity {

    private int clickCount = 0;
    private boolean notificationsEnabled = true;
    private String statusMessage = "All systems nominal";

    public DisplayDemoPanelBlockEntity(BlockPos pos, BlockState blockState) {
        super(SandboxRegistration.DISPLAY_DEMO_PANEL_BLOCK_ENTITY.get(), pos, blockState);
    }

    // -------------------------------------------------------------------------
    // Abstract method implementations
    // -------------------------------------------------------------------------

    @Override
    public DisplayConfig getDisplayConfig() {
        return DisplayConfig.flatPanel();
    }

    @Override
    public ContentBuilder getContentBuilder() {
        return DisplayDemoPanelBlockEntity::buildPanel;
    }

    @Override
    public String getChannelId() {
        return "panel";
    }

    // -------------------------------------------------------------------------
    // Protected hooks
    // -------------------------------------------------------------------------

    @Override
    protected void wireCallbacks(Gui gui) {
        for (var el : gui.getElements()) {
            if (el instanceof Button btn && btn.getText() != null && btn.getText().startsWith("Click")) {
                btn.setOnFallingEdge(() -> {
                    clickCount++;
                    for (var e : gui.getElements()) {
                        if (e instanceof Label l && l.getText() != null && l.getText().startsWith("Clicks:")) {
                            l.setText("Clicks: " + clickCount);
                            break;
                        }
                    }
                    syncToClientPublic();
                });
            }
            if (el instanceof CheckBox cb) {
                cb.setOnStateChanged(checked -> {
                    notificationsEnabled = checked;
                    syncToClientPublic();
                });
            }
            if (el instanceof TextBox tb) {
                tb.setOnTextChanged(text -> {
                    statusMessage = text;
                    syncToClientPublic();
                });
            }
        }
        syncStateToGui();
    }

    @Override
    protected void onControllerTick() {
        // No animation needed for the status panel
    }

    @Override
    protected void saveCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("clickCount", clickCount);
        tag.putBoolean("notificationsEnabled", notificationsEnabled);
        tag.putString("statusMessage", statusMessage);
    }

    @Override
    protected void loadCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        clickCount = tag.getInt("clickCount");
        notificationsEnabled = tag.getBoolean("notificationsEnabled");
        if (tag.contains("statusMessage")) {
            statusMessage = tag.getString("statusMessage");
        }
        syncStateToGui();
    }

    @Override
    public void onInputSynced() {
        if (gui == null) return;
        for (var el : gui.getElements()) {
            if (el instanceof CheckBox cb) {
                notificationsEnabled = cb.isChecked();
            }
            if (el instanceof TextBox tb) {
                statusMessage = tb.getText();
            }
        }
        syncStateToGui();
    }

    // -------------------------------------------------------------------------
    // Content builder
    // -------------------------------------------------------------------------

    /**
     * Builds the status panel layout. Creates visual elements only — no server
     * callbacks. Callbacks are wired separately via {@link #wireCallbacks(Gui)}.
     */
    private static void buildPanel(Gui gui, int w, int h) {
        int margin = 10;
        int y = margin;

        // Title
        Label title = new Label("Status Panel");
        title.setBounds(0, y, w, 16);
        title.setAlignment(GuiElement.Alignment.CENTER);
        gui.addElement(title);
        y += 24;

        // Click counter display
        Label clickLabel = new Label("Clicks: 0");
        clickLabel.setBounds(margin, y, w - margin * 2, 14);
        clickLabel.setAlignment(GuiElement.Alignment.LEFT);
        gui.addElement(clickLabel);
        y += 18;

        // Click button
        Button clickButton = new Button("Click me!");
        clickButton.setBounds(margin, y, w - margin * 2, 18);
        gui.addElement(clickButton);
        y += 26;

        // Checkbox
        CheckBox notifCheckbox = new CheckBox("Notifications");
        notifCheckbox.setBounds(margin, y, w - margin * 2, 14);
        notifCheckbox.setChecked(true);
        gui.addElement(notifCheckbox);
        y += 22;

        // Status message label
        Label msgLabel = new Label("Message:");
        msgLabel.setBounds(margin, y, 50, 14);
        msgLabel.setAlignment(GuiElement.Alignment.LEFT);
        gui.addElement(msgLabel);

        // Status text input
        TextBox statusInput = new TextBox(margin + 52, y, w - margin * 2 - 52);
        statusInput.setText("All systems nominal");
        statusInput.setMaxChars(30);
        gui.addElement(statusInput);
        y += 22;

        // Enchanted book item view
        ItemView bookView = new ItemView();
        bookView.setShowTooltip(true);
        bookView.setBounds(margin, y, 16, 16);
        RegistryAccess registryAccess = UtilitiesPlatform.getRegistryAccess();
        if (registryAccess != null) {
            var sharpness = registryAccess.registryOrThrow(Registries.ENCHANTMENT)
                    .getHolder(Enchantments.SHARPNESS);
            sharpness.ifPresent(holder ->
                    bookView.setItemStack(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, 1))));
        }
        gui.addElement(bookView);
    }

    // -------------------------------------------------------------------------
    // State synchronization
    // -------------------------------------------------------------------------

    /**
     * Applies the current field values to the GUI elements.
     */
    private void syncStateToGui() {
        if (gui == null) return;
        for (var el : gui.getElements()) {
            if (el instanceof Label l && l.getText() != null && l.getText().startsWith("Clicks:")) {
                l.setText("Clicks: " + clickCount);
            }
            if (el instanceof CheckBox cb) {
                cb.setChecked(notificationsEnabled);
            }
            if (el instanceof TextBox tb) {
                tb.setText(statusMessage);
            }
        }
    }
}
