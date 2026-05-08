# ItemSelectionView

## Overview

The `ItemSelectionView` is a comprehensive item selection interface with built-in search, filtering, and sorting capabilities. It displays a grid of items with a search box and provides callbacks when items are selected. Perfect for item browsers and selection dialogs.

**When to use:**
- Item selection dialogs
- Inventory browsers
- Recipe ingredient selection
- Item filtering interfaces
- Custom shop interfaces

## Constructor

```java
ItemSelectionView(Consumer<ItemStack> onItemSelected)
ItemSelectionView(List<ItemStack> allowedItems, Consumer<ItemStack> onItemSelected)
```

**Parameters:**
- `allowedItems` - List of items to display (defaults to all creative tab items including variants such as potions, enchanted books, and tipped arrows)
- `onItemSelected` - Callback invoked when an item is clicked

> **Note:** The default constructor (without `allowedItems`) now populates the view with creative tab items via `ItemUtilities.getAllItems()`, which includes all item variants. Previously it used one stack per registered item type. You can also use `ItemUtilities.getItemsByCategory()` to build category-filtered views:
>
> ```java
> // Default: all creative items including potions, enchanted books, etc.
> ItemSelectionView view = new ItemSelectionView(callback);
>
> // Or: items from a specific category
> var categories = ItemUtilities.getItemsByCategory();
> List<ItemStack> combatItems = categories.get("Combat");
> ItemSelectionView combatView = new ItemSelectionView(combatItems, callback);
> ```

## Key Methods

### Item Management
```java
void setItems(List<ItemStack> items)           // Replace all items
void addItem(ItemStack stack)                  // Add single item
void addItems(List<ItemStack> stacks)          // Add multiple items
void removeItem(ItemStack stack)               // Remove single item
void removeItems(List<ItemStack> stacks)       // Remove multiple items
void clearItems()                              // Remove all items
```

### Sorting and Filtering
```java
void setSorter(Sorter sorter)                  // Set sorting algorithm
void setFilter(Filter filter)                  // Set filter algorithm
String getSearchText()                         // Get current search text
void setItemLabelText(String text)             // Set "Items" label text
```

### Text Styling
```java
void setTextColor(int color)                   // Set all text color
void setTextFontScale(float scale)             // Set all text font scale
```

## Built-in Sorters

### NameSorter
Sorts items alphabetically by display name.
```java
itemView.setSorter(new ItemSelectionView.NameSorter());
```

### TagSorter (Default)
Sorts items by tags, then by name.
```java
itemView.setSorter(new ItemSelectionView.TagSorter());
```

### SorterByIntID
Sorts items by their registry ID.
```java
itemView.setSorter(new ItemSelectionView.SorterByIntID());
```

### Custom Sorter
```java
itemView.setSorter(items -> {
    items.sort(Comparator.comparing(stack -> 
        stack.getRarity().toString()
    ));
});
```

## Built-in Filters

### SearchFilter (Default)
Filters by name, tags, and display text matching search input.
```java
itemView.setFilter(new ItemSelectionView.SearchFilter(itemView));
```

### Custom Filter
```java
itemView.setFilter((stack, searchText) -> {
    // Custom filter logic
    return stack.isEdible() && 
           stack.getHoverName().getString().toLowerCase().contains(searchText);
});
```

## Code Examples

### Basic Item Selection
```java
ItemSelectionView itemSelector = new ItemSelectionView(selectedItem -> {
    System.out.println("Selected: " + selectedItem.getHoverName().getString());
    inventory.addItem(selectedItem);
});

itemSelector.setSize(400, 500);
gui.addChild(itemSelector);
```

### Filtered Item Selection
```java
// Only show food items
List<ItemStack> foodItems = ItemUtilities.getAllItems().stream()
    .filter(stack -> stack.isEdible())
    .collect(Collectors.toList());

ItemSelectionView foodSelector = new ItemSelectionView(foodItems, selectedItem -> {
    player.eat(selectedItem);
});

foodSelector.setItemLabelText("Food Items");
```

### Weapon Selection
```java
// Filter for swords and axes
List<ItemStack> weapons = ItemUtilities.getAllItems().stream()
    .filter(stack -> {
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem;
    })
    .collect(Collectors.toList());

ItemSelectionView weaponSelector = new ItemSelectionView(weapons, weapon -> {
    player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
});

weaponSelector.setSorter(new ItemSelectionView.NameSorter());
```

### Custom Styled Selector
```java
ItemSelectionView selector = new ItemSelectionView(this::handleSelection);
selector.setSize(500, 600);
selector.setBackgroundColor(0xFF2a2a2a);
selector.setTextColor(0xFFFFFFFF);
selector.setTextFontScale(0.9f);
selector.setItemLabelText("Choose an item");
```

## Interface Implementations

### Custom Sorter
```java
public class RaritySorter implements ItemSelectionView.Sorter {
    @Override
    public void apply(List<ItemStack> items) {
        items.sort(Comparator.comparing(stack -> 
            stack.getRarity().ordinal()
        ));
    }
}

itemView.setSorter(new RaritySorter());
```

### Custom Filter
```java
public class ModFilter implements ItemSelectionView.Filter {
    private final String modId;
    
    public ModFilter(String modId) {
        this.modId = modId;
    }
    
    @Override
    public boolean apply(ItemStack stack, String searchText) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals(modId) && 
               stack.getHoverName().getString().toLowerCase().contains(searchText);
    }
}

itemView.setFilter(new ModFilter("minecraft"));
```

## Common Patterns

### Multi-Selection
```java
List<ItemStack> selectedItems = new ArrayList<>();
ItemSelectionView multiSelect = new ItemSelectionView(item -> {
    if (selectedItems.contains(item)) {
        selectedItems.remove(item);
    } else {
        selectedItems.add(item);
    }
    updateSelectionDisplay();
});
```

### Category-Based Display
```java
Map<String, List<ItemStack>> categories = new HashMap<>();
categories.put("Tools", getToolItems());
categories.put("Armor", getArmorItems());
categories.put("Food", getFoodItems());

DropDownMenu categoryMenu = new DropDownMenu("Category");
ItemSelectionView itemView = new ItemSelectionView(this::handleSelection);

for (String category : categories.keySet()) {
    categoryMenu.addOption(category);
}

categoryMenu.setOnOptionSelected((index, element) -> {
    String category = ((Label) element).getText();
    itemView.setItems(categories.get(category));
});
```

## Best Practices

1. **Performance**: Limit the number of items for better performance (under 1000 recommended)
2. **Default Filters**: The SearchFilter is applied by default and searches names, tags, and descriptions
3. **Layout**: The internal layout uses LayoutGrid and automatically adjusts columns based on width
4. **Sorting**: Apply sorting before filtering for better user experience
5. **Localization**: Item names are automatically localized
6. **Memory**: Use `clearItems()` when changing contexts to free memory

## Technical Notes

- Uses `VerticalListView` with `LayoutGrid` internally
- Search is case-insensitive
- Default grid columns calculated as: `containerWidth / ItemView.DEFAULT_WIDTH`
- Items are wrapped in internal `ItemButton` elements for click detection
- Search updates the filter in real-time
- Tag sorting includes mod namespace in the sort key
