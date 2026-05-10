package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.ClientPlayerUtilities;
import net.kroia.modutilities.ItemUtilities;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.ListView;
import net.kroia.modutilities.gui.layout.LayoutGrid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Searchable item picker that displays a scrollable grid of items with a
 * filter text box and forwards selection events through a callback.
 * <p>
 * The displayed item list is configurable via the constructors and runtime
 * mutators ({@link #addItem(ItemStack)}, {@link #setItems(List)}, etc.). A
 * {@link Sorter} controls the ordering of the grid and a {@link Filter}
 * controls which items pass the user-entered search text. Default
 * implementations are provided as the inner classes {@link NameSorter},
 * {@link TagSorter}, {@link SorterByIntID}, and {@link SearchFilter}.
 *
 * @apiNote The supplied {@code onItemSelected} consumer is invoked when the
 *          user left-clicks an item entry; the consumer receives the selected
 *          {@link ItemStack} and is responsible for closing/dismissing the
 *          picker if desired.
 */
public class ItemSelectionView extends GuiElement {
    /**
     * Strategy interface that orders the displayed item list in place.
     */
    public interface Sorter
    {
        /**
         * Sorts {@code items} in place using the implementation's ordering rule.
         *
         * @param items the list of items to sort
         */
        public abstract void apply(List<ItemStack> items);
    }

    /**
     * Strategy interface that decides whether an item passes the active search.
     */
    public interface Filter
    {
        /**
         * @param stack      the item being tested
         * @param searchText the lowercased search text entered by the user
         * @return {@code true} if {@code stack} should be visible in the grid
         */
        public abstract boolean apply(ItemStack stack, String searchText);
    }

    /**
     * {@link Sorter} that orders items alphabetically by their localized
     * hover name.
     */
    public static final class NameSorter implements Sorter
    {
        @Override
        public void apply(List<ItemStack> items) {
            items.sort(Comparator.comparing(stack -> {
                return stack.getHoverName().getString();
            }));
        }
    }

    /**
     * {@link Sorter} that orders items first by their concatenated, sorted
     * tag set (descending) and breaks ties by description ID.
     */
    public static final class TagSorter implements Sorter
    {
        @Override
        public void apply(List<ItemStack> items) {
            // Sort by item tags, then by name
            items.sort((stack1, stack2) -> {
                Item item1 = stack1.getItem();
                Item item2 = stack2.getItem();

                // Get the registry keys for both items
                ResourceKey<Item> key1 = BuiltInRegistries.ITEM.getResourceKey(item1).orElseThrow();
                ResourceKey<Item> key2 = BuiltInRegistries.ITEM.getResourceKey(item2).orElseThrow();

                // Get all tags for both items
                Set<TagKey<Item>> tags1 = BuiltInRegistries.ITEM.getHolderOrThrow(key1).tags().collect(Collectors.toSet());
                Set<TagKey<Item>> tags2 = BuiltInRegistries.ITEM.getHolderOrThrow(key2).tags().collect(Collectors.toSet());

                // Convert tags to sorted string representation for comparison
                String tagString1 = tags1.stream()
                        .map(tag -> tag.location().toString())
                        .sorted()
                        .collect(Collectors.joining(","));

                String tagString2 = tags2.stream()
                        .map(tag -> tag.location().toString())
                        .sorted()
                        .collect(Collectors.joining(","));

                // First compare by tags
                int tagComparison = tagString1.compareTo(tagString2);
                if (tagComparison != 0) {
                    return -tagComparison;
                }

                // If tags are the same, compare by item name
                String name1 = item1.getDescriptionId();
                String name2 = item2.getDescriptionId();

                return name1.compareTo(name2);
            });
        }
    }
    /**
     * {@link Sorter} that orders items by their numeric ID in
     * {@link BuiltInRegistries#ITEM}.
     */
    public static class SorterByIntID implements ItemSelectionView.Sorter {
        @Override
        public void apply(List<ItemStack> items) {

            items.sort(Comparator.comparing(stack -> {
                // Compare item id index
                return BuiltInRegistries.ITEM.getId(stack.getItem());
            }));
        }
    }

    /**
     * Default {@link Filter} that matches the search text against the item's
     * hover name, its display decoration text, and its tag identifiers
     * (case-insensitive substring match).
     */
    public static final class SearchFilter implements Filter
    {
        public SearchFilter()
        {
        }
        @Override
        public boolean apply(ItemStack stack, String searchText) {
            String name = stack.getHoverName().getString().toLowerCase();

            if(name.contains(searchText))
                return true;
            String itemDecoration = ClientPlayerUtilities.getItemDisplayText(stack).toLowerCase();
            if(itemDecoration.contains(searchText))
                return true;

            // Check tags
            Item item = stack.getItem();
            ResourceKey<Item> key = BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow();
            Set<TagKey<Item>> tags = BuiltInRegistries.ITEM.getHolderOrThrow(key).tags().collect(Collectors.toSet());
            for(TagKey<Item> tag : tags)
            {
                ResourceLocation loc = tag.location();
                String tagName = loc.toString().toLowerCase();
                if(tagName.contains(searchText))
                    return true;
            }
            return false;
        }
    }




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
                onItemSelected.accept(itemStack);
                return true;
            }
            return false;
        }
    }

    private Sorter sorter = new TagSorter();
    private Filter filter = new SearchFilter();

    private final List<ItemStack> allowedItems;
    private final Consumer<ItemStack> onItemSelected;

    private final Label searchLabel;
    private final Label itemsLabel;
    private final TextBox searchField;
    private final ListView listView;
    private final LayoutGrid layoutGrid;
    /**
     * Creates an item selection view that lists every registered item.
     *
     * @param onItemSelected callback invoked with the selected stack on click
     */
    public ItemSelectionView(Consumer<ItemStack> onItemSelected) {
        this(ItemUtilities.getAllItems(), onItemSelected);
    }

    /**
     * Creates an item selection view limited to the supplied items.
     *
     * @param allowedItemsIDs the items the user may pick from (copied internally)
     * @param onItemSelected  callback invoked with the selected stack on click
     */
    public ItemSelectionView(List<ItemStack> allowedItemsIDs, Consumer<ItemStack> onItemSelected) {
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
        layoutGrid = new LayoutGrid(1, 0, false, false,0,0, GuiElement.Alignment.TOP);
        listView.setLayout(layoutGrid);

        addChild(searchLabel);
        addChild(itemsLabel);
        addChild(searchField);
        addChild(listView);

        sortItems();
    }

    /**
     * Replaces the displayed item set with the supplied list and re-sorts.
     *
     * @param allowedItemsIDs the new list of selectable items
     */
    public void setItems(List<ItemStack> allowedItemsIDs) {
        allowedItems.clear();
        allowedItems.addAll(allowedItemsIDs);
        sortItems();
    }

    /**
     * Appends a single item to the selectable list and re-sorts.
     *
     * @param stack the item to add
     */
    public void addItem(ItemStack stack) {
        allowedItems.add(stack);
        sortItems();
    }

    /**
     * Appends multiple items to the selectable list and re-sorts.
     *
     * @param stacks the items to add
     */
    public void addItems(List<ItemStack> stacks) {
        allowedItems.addAll(stacks);
        sortItems();
    }

    /**
     * Removes the first occurrence of the given item from the selectable list
     * and refreshes the filtered display.
     *
     * @param stack the item to remove
     */
    public void removeItem(ItemStack stack) {
        allowedItems.remove(stack);
        updateFilter();
    }

    /**
     * Removes all occurrences of the given items from the selectable list and
     * refreshes the filtered display.
     *
     * @param stacks the items to remove
     */
    public void removeItems(List<ItemStack> stacks) {
        allowedItems.removeAll(stacks);
        updateFilter();
    }

    /**
     * Removes every item from the selectable list and refreshes the
     * filtered display.
     */
    public void clearItems() {
        allowedItems.clear();
        updateFilter();
    }

    /**
     * Sets the {@link Sorter} that controls item ordering and re-sorts the
     * selectable list immediately.
     *
     * @param sorter the new sorter, or {@code null} to leave the list as-is
     */
    public void setSorter(Sorter sorter) {
        this.sorter = sorter;
        sortItems();
    }

    /**
     * Sets the {@link Filter} that decides which items pass the active search
     * and refreshes the filtered display immediately.
     *
     * @param filter the new filter, or {@code null} to disable filtering
     */
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

        searchLabel.setBounds(0, 0, width/2, 15);
        searchField.setBounds(searchLabel.getRight(), searchLabel.getTop(), width-searchLabel.getWidth(), searchLabel.getHeight());
        itemsLabel.setBounds(0, searchLabel.getBottom(), width, searchLabel.getHeight());
        listView.setBounds(0, itemsLabel.getBottom(), width, getHeight()-itemsLabel.getBottom());
        layoutGrid.columns = listView.getContainerWidth()/ItemView.DEFAULT_WIDTH;
    }

    /**
     * @return the text currently entered into the search box
     */
    public String getSearchText() {
        return searchField.getText();
    }

    /**
     * Overrides the label rendered above the item grid (default is the
     * "Items" translation).
     *
     * @param text the label text to display
     */
    public void setItemLabelText(String text) {
        itemsLabel.setText(text);
    }

    private void updateFilter() {
        listView.removeChilds();
        listView.getLayout().enabled = false;

        if(filter != null)
        {
            String searchText = getSearchText().toLowerCase();
            for(ItemStack stack : allowedItems)
            {
                if(filter.apply(stack, searchText))
                {
                    listView.addChild(new ItemButton(stack));
                }
            }
        }
        listView.getLayout().enabled = true;
        listView.layoutChangedInternal();
    }

    /**
     * Re-sorts the selectable item list using the active {@link Sorter} and
     * refreshes the filtered display.
     * <p>
     * Has no effect if no sorter is set.
     */
    public void sortItems() {

        if(sorter != null) {
            sorter.apply(allowedItems);
            updateFilter();
        }
    }


    @Override
    public void setTextColor(int color) {
        searchLabel.setTextColor(color);
        itemsLabel.setTextColor(color);
        searchField.setTextColor(color);
    }
    @Override
    public int getTextColor() {
        return searchLabel.getTextColor();
    }
    @Override
    public void setTextFontScale(float scale) {
        searchLabel.setTextFontScale(scale);
        itemsLabel.setTextFontScale(scale);
        searchField.setTextFontScale(scale);
    }
    @Override
    public float getTextFontScale() {
        return searchLabel.getTextFontScale();
    }
}
