package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.ItemUtilities;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.ListView;
import net.kroia.modutilities.gui.layout.LayoutGrid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;

public class ItemSelectionView extends GuiElement {
    public interface Sorter
    {
        public abstract void apply(ArrayList<ItemStack> items);
    }
    public interface Filter
    {
        public abstract boolean apply(ItemStack stack);
    }

    public static final class NameSorter implements Sorter
    {
        @Override
        public void apply(ArrayList<ItemStack> items) {
            items.sort(Comparator.comparing(stack -> {
                return stack.getHoverName().getString();
            }));
        }
    }
    public static class SorterByIntID implements ItemSelectionView.Sorter {
        @Override
        public void apply(ArrayList<ItemStack> items) {

            items.sort(Comparator.comparing(stack -> {
                // Compare item id index
                return BuiltInRegistries.ITEM.getId(stack.getItem());
            }));
        }
    }
    public static final class SearchFilter implements Filter
    {
        private final ItemSelectionView view;
        public SearchFilter(ItemSelectionView view)
        {
            this.view = view;
        }
        @Override
        public boolean apply(ItemStack stack) {
            String name = stack.getHoverName().getString().toLowerCase();
            return name.contains(view.getSearchText());
        }
    }




    private static final Component SEARCH_LABEL = Component.translatable("gui.modutilities.search");
    private static final Component ITEMS_LABEL = Component.translatable("gui.modutilities.items");

    private Sorter sorter = new NameSorter();
    private Filter filter = new SearchFilter(this);

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
                onItemSelected.accept(itemStack);
                return true;
            }
            return false;
        }
    }

    private final ArrayList<ItemStack> allowedItems;
    private final Consumer<ItemStack> onItemSelected;

    private final Label searchLabel;
    private final Label itemsLabel;
    private final TextBox searchField;
    private final ListView listView;
    private final LayoutGrid layoutGrid;
    public ItemSelectionView(Consumer<ItemStack> onItemSelected) {
        this(ItemUtilities.getAllItems(), onItemSelected);
    }
    public ItemSelectionView(ArrayList<ItemStack> allowedItemsIDs, Consumer<ItemStack> onItemSelected) {
        this.onItemSelected = onItemSelected;

        this.allowedItems = new ArrayList<>();
        this.allowedItems.addAll(allowedItemsIDs);

        searchLabel = new Label(SEARCH_LABEL.getString());
        searchLabel.setAlignment(GuiElement.Alignment.RIGHT);
        itemsLabel = new Label(ITEMS_LABEL.getString());
        itemsLabel.setAlignment(GuiElement.Alignment.BOTTOM);
        searchField = new TextBox();
        searchField.setOnTextChanged((s)->updateFilter());
        listView = new VerticalListView();
        layoutGrid = new LayoutGrid(1, 0, false, false,0,getWidth()/20, GuiElement.Alignment.TOP);
        listView.setLayout(layoutGrid);

        addChild(searchLabel);
        addChild(itemsLabel);
        addChild(searchField);
        addChild(listView);

        sortItems();
    }

    public void setItems(ArrayList<ItemStack> allowedItemsIDs) {
        allowedItems.clear();
        allowedItems.addAll(allowedItemsIDs);
        sortItems();
    }
    public void addItem(ItemStack stack) {
        allowedItems.add(stack);
        sortItems();
    }
    public void addItems(ArrayList<ItemStack> stacks) {
        allowedItems.addAll(stacks);
        sortItems();
    }
    public void removeItem(ItemStack stack) {
        allowedItems.remove(stack);
        updateFilter();
    }
    public void removeItems(ArrayList<ItemStack> stacks) {
        allowedItems.removeAll(stacks);
        updateFilter();
    }
    public void clearItems() {
        allowedItems.clear();
        updateFilter();
    }
    public void setSorter(Sorter sorter) {
        this.sorter = sorter;
        sortItems();
    }
    public void setFilter(Filter filter) {
        this.filter = filter;
        updateFilter();
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

    public String getSearchText() {
        return searchField.getText();
    }

    public void setItemLabelText(String text) {
        itemsLabel.setText(text);
    }

    private void updateFilter() {
        listView.removeChilds();
        listView.getLayout().enabled = false;

        if(filter != null)
        {
            for(ItemStack stack : allowedItems)
            {
                if(filter.apply(stack))
                {
                    listView.addChild(new ItemButton(stack));
                }
            }
        }
        listView.getLayout().enabled = true;
        listView.layoutChangedInternal();
    }

    public void sortItems() {

        if(sorter != null) {
            sorter.apply(allowedItems);
            updateFilter();
        }
    }
}
