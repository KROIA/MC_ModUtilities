package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.ItemUtilities;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiScreen;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.ListView;
import net.kroia.modutilities.gui.layout.LayoutGrid;
import net.kroia.modutilities.gui.screens.ItemSelectionScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ItemSelectionView extends GuiElement {

    private static final Component SEARCH_LABEL = Component.translatable("gui.modutilities.search");
    private static final Component ITEMS_LABEL = Component.translatable("gui.modutilities.items");



    private class ItemButton extends ItemView {

        public ItemButton(ItemStack stack) {
            super(stack);
        }

        @Override
        public void renderBackground()
        {
            super.renderBackground();
            if(isMouseOver())
            {
                drawRect(0,0,getWidth(),getHeight(),0x80FFFFFF);
            }
        }
        @Override
        public boolean mouseClickedOverElement(int button) {
            if (button == 0) {
                String itemID = ItemUtilities.getItemID(itemStack.getItem());
                onItemSelected.accept(itemID);
                return true;
            }
            return false;
        }
    }

    private final ArrayList<ItemStack> allowedItems;
    private final Consumer<String> onItemSelected;

    private final Label searchLabel;
    private final Label itemsLabel;
    private final TextBox searchField;
    private final ListView listView;
    private final LayoutGrid layoutGrid;
    public ItemSelectionView(Consumer<String> onItemSelected) {
        this(ItemUtilities.getAllItemIDs(), onItemSelected);
    }
    public ItemSelectionView(ArrayList<String> allowedItemsIDs, Consumer<String> onItemSelected) {
        this.onItemSelected = onItemSelected;

        this.allowedItems = new ArrayList<>();
        for(String itemId : allowedItemsIDs) {
            this.allowedItems.add(ItemUtilities.createItemStackFromId(itemId));
        }

        searchLabel = new Label(SEARCH_LABEL.getString());
        searchLabel.setAlignment(GuiElement.Alignment.RIGHT);
        itemsLabel = new Label(ITEMS_LABEL.getString());
        itemsLabel.setAlignment(GuiElement.Alignment.BOTTOM);
        searchField = new TextBox();
        searchField.setOnTextChanged(this::updateFilter);
        listView = new VerticalListView();
        layoutGrid = new LayoutGrid(1, 0, false, false,0,getWidth()/20, GuiElement.Alignment.TOP);
        listView.setLayout(layoutGrid);
        //backButton = new Button(CANCEL_BUTTON.getString());
        //backButton.setOnFallingEdge(onBackButtonClicked);

        addChild(searchLabel);
        addChild(itemsLabel);
        addChild(searchField);
        addChild(listView);
        //addChild(backButton);

        updateFilter(searchField.getText());
    }

    public void setAllowedItems(ArrayList<String> allowedItemsIDs) {
        allowedItems.clear();
        for(String itemId : allowedItemsIDs) {
            allowedItems.add(ItemUtilities.createItemStackFromId(itemId));
        }
        updateFilter(searchField.getText());
    }

    @Override
    protected void render() {

    }

    @Override
    protected void layoutChanged() {
        int width = getWidth();
        layoutGrid.columns = width/20;
        searchLabel.setBounds(0, 0, width/2, 20);
        searchField.setBounds(width/2, 0, width/2, 20);
        itemsLabel.setBounds(0, 20, width, 20);
        listView.setBounds(0, 40, width, getHeight()-40);
    }

    public void setItemLabelText(String text) {
        itemsLabel.setText(text);
    }

    private void updateFilter(String filter) {
        listView.removeChilds();
        listView.getLayout().enabled = false;
        if (filter.isEmpty()) {
            for (ItemStack stack : allowedItems) {
                listView.addChild(new ItemSelectionView.ItemButton(stack));
            }
        } else {
            String lowerFilter = filter.toLowerCase();
            for (ItemStack stack : allowedItems) {
                String name = stack.getHoverName().getString().toLowerCase();
                if (name.contains(lowerFilter)) {
                    listView.addChild(new ItemSelectionView.ItemButton(stack));
                }
            }
        }
        listView.getLayout().enabled = true;
        listView.layoutChangedInternal();
    }

    public void sortItems() {
        // Sort items with an reordered name, so that the first char is a the end and the last char is at the beginning
        allowedItems.sort(Comparator.comparing(stack -> {
            String name = stack.getHoverName().getString();
            return new StringBuilder(name).reverse().toString();
        }));
        updateFilter(searchField.getText());
    }
}
