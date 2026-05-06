package net.kroia.modutilities.sandbox.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiScreen;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.ItemSelectionView;
import net.kroia.modutilities.gui.elements.ItemView;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Usecase example: an item selection interface with search and a "current selection"
 * preview.
 *
 * Demonstrates:
 *  - Constructing an {@link ItemSelectionView} backed by all registered items
 *  - Reacting to item clicks via the constructor callback
 *  - Switching the {@link ItemSelectionView.Sorter} between Name and Tag
 *  - Showing the selected stack in an {@link ItemView}
 *
 * Open via: /modutilities openExample itemSelection
 */
@Environment(EnvType.CLIENT)
public class ExampleItemSelectionScreen extends GuiScreen {

    private final Label title;
    private final ItemSelectionView selectionView;
    private final ItemView previewView;
    private final Label previewLabel;
    private final Button sortByNameButton;
    private final Button sortByTagButton;

    private ItemStack selectedStack = new ItemStack(Items.AIR);

    public ExampleItemSelectionScreen() {
        super(Component.literal("Item Selection Example"));

        title = new Label("Pick an item");
        title.setAlignment(GuiElement.Alignment.CENTER);
        title.setTextFontScale(1.4f);
        addElement(title);

        selectionView = new ItemSelectionView(this::onItemPicked);
        selectionView.setSorter(new ItemSelectionView.NameSorter());
        addElement(selectionView);

        previewView = new ItemView();
        addElement(previewView);

        previewLabel = new Label("(nothing selected)");
        previewLabel.setAlignment(GuiElement.Alignment.LEFT);
        addElement(previewLabel);

        sortByNameButton = new Button("Sort: Name", () -> {
            selectionView.setSorter(new ItemSelectionView.NameSorter());
        });
        sortByTagButton = new Button("Sort: Tag", () -> {
            selectionView.setSorter(new ItemSelectionView.TagSorter());
        });
        addElement(sortByNameButton);
        addElement(sortByTagButton);
    }

    public static void open() {
        GuiScreen.setScreen(new ExampleItemSelectionScreen());
    }

    private void onItemPicked(ItemStack stack) {
        selectedStack = stack.copy();
        previewView.setItemStack(selectedStack);
        previewLabel.setText(selectedStack.getHoverName().getString());
        ModUtilitiesMod.LOGGER.info("[Example/ItemSelection] Picked item: {}", selectedStack);
    }

    @Override
    protected void updateLayout(Gui gui) {
        int w = getWidth();
        int h = getHeight();
        int margin = 12;

        title.setBounds(0, margin, w, 20);

        // Right side: sidebar with preview and sort buttons.
        int sidebarWidth = 160;
        int contentTop = title.getBottom() + 8;
        int contentHeight = h - contentTop - margin;

        // Selection view on the left, sidebar on the right.
        int selectionWidth = w - sidebarWidth - margin * 3;
        selectionView.setBounds(margin, contentTop, selectionWidth, contentHeight);

        int sx = selectionView.getRight() + margin;
        int sy = contentTop;
        int previewSize = 32;
        previewView.setBounds(sx, sy, previewSize, previewSize);
        previewLabel.setBounds(sx + previewSize + 6, sy + (previewSize - 14) / 2, sidebarWidth - previewSize - 6, 14);
        sy += previewSize + 12;

        sortByNameButton.setBounds(sx, sy, sidebarWidth, 22);
        sy += 28;
        sortByTagButton.setBounds(sx, sy, sidebarWidth, 22);
    }
}
