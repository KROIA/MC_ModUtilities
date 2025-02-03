package net.kroia.modutilities.gui.screens;

import net.kroia.modutilities.ItemUtilities;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiScreen;
import net.kroia.modutilities.gui.elements.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.function.Consumer;

public class ItemSelectionScreen extends GuiScreen {

    private static final Component TITLE = Component.translatable("gui.modutilities.item_selection.title");
    private static final Component CANCEL_BUTTON = Component.translatable("gui.modutilities.cancel");

    private static final int menuWidth = 200;
    private final Screen parentScreen;
    private final Button backButton;
    private final ItemSelectionView itemSelectionView;



    public ItemSelectionScreen(Screen parentScreen, Consumer<String> onItemSelected) {
        this(parentScreen, ItemUtilities.getAllItemIDs(), onItemSelected);
    }
    public ItemSelectionScreen(Screen parentScreen, ArrayList<String> allowedItemsIDs, Consumer<String> onItemSelected) {
        super(TITLE);
        this.parentScreen = parentScreen;

        itemSelectionView = new ItemSelectionView(allowedItemsIDs, (s) -> {
            onItemSelected.accept(s);
            minecraft.setScreen(parentScreen);
        });
        backButton = new Button(CANCEL_BUTTON.getString());
        backButton.setOnFallingEdge(() -> minecraft.setScreen(parentScreen));

        addElement(itemSelectionView);
        addElement(backButton);
    }

    @Override
    protected void updateLayout(Gui gui) {

        int width = getWidth();
        itemSelectionView.setBounds((width - menuWidth) / 2, 10, menuWidth, getHeight()-40);
        backButton.setBounds((width - menuWidth) / 2, getHeight() - 30, menuWidth, 20);
    }
    public void sortItems() {
        itemSelectionView.sortItems();
    }
}
