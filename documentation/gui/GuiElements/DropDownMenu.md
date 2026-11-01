# DropDownMenu

## Overview

The `DropDownMenu` is an expandable/collapsible menu component that displays a list of selectable options. It shows a label or custom element when collapsed and expands to show all available options when clicked. DropDownMenus are perfect for space-efficient selection interfaces with many options.

**When to use:**
- Selection from a list of predefined options
- Configuration menus with limited space
- Filters and sorting options
- Category or type selection

## Constructor

```java
// Basic dropdown with label
DropDownMenu(String label)

// Dropdown with selection callback
DropDownMenu(String label, BiConsumer<Integer, GuiElement> optionSelected)
```

**Parameters:**
- `label` - The text displayed when the menu is collapsed
- `optionSelected` - Callback invoked when an option is selected (receives index and the element)

## Key Methods

### Label Management
```java
void setLabelText(String text)                     // Set the label text
String getLabelText()                              // Get the label text
void setCustomLabelElement(GuiElement element)     // Use custom element as label
```

### Options Management
```java
void addOption(String label)                       // Add a text option
void addChild(GuiElement element)                  // Add a custom option element
void removeChild(GuiElement element)               // Remove an option
void clearOptions()                                // Remove all options
List<GuiElement> getChilds()                       // Get all option elements
```

### Expansion Control
```java
void setExpanded(boolean expanded)                 // Set expansion state
boolean isExpanded()                               // Check if expanded
void expand()                                      // Expand the menu
void collapse()                                    // Collapse the menu
void setMaxExpandedHeight(int maxHeight)           // Set max height when expanded
int getMaxExpandedHeight()                         // Get max expanded height
int getExpandedHeight()                            // Get actual expanded height
int getUnexpandedHeight()                          // Get height when collapsed
```

### Selection
```java
void setOnOptionSelected(BiConsumer<Integer, GuiElement> callback)   // Set selection callback
int getSelectedIndex()                             // Get index of selected option
void setSelectedIndex(int index)                   // Programmatically select an option
```

### Z-Position Control
```java
void setExpandedZPos(float z)                      // Set Z position when expanded
float getExpandedZPos()                            // Get expanded Z position
void setZ(float z)                                 // Set default Z position
```

## Styling

The DropDownMenu uses a `VerticalListView` internally to display options and includes visual feedback:
- Arrow icons (down when collapsed, up when expanded)
- Hover effects on options
- Click sound feedback

### Customization Example
```java
DropDownMenu menu = new DropDownMenu("Select Option");
menu.setBackgroundColor(0xFF333333);
menu.setOutlineColor(0xFF666666);
menu.setTextColor(0xFFFFFFFF);
menu.setMaxExpandedHeight(150);
```

## Code Examples

### Basic Dropdown
```java
DropDownMenu difficultyMenu = new DropDownMenu("Difficulty");
difficultyMenu.addOption("Easy");
difficultyMenu.addOption("Normal");
difficultyMenu.addOption("Hard");
difficultyMenu.addOption("Expert");

difficultyMenu.setOnOptionSelected((index, element) -> {
    String difficulty = ((Label) element).getText();
    game.setDifficulty(difficulty);
    System.out.println("Selected: " + difficulty + " (index: " + index + ")");
});

gui.addChild(difficultyMenu);
```

### Dropdown with Custom Elements
```java
DropDownMenu itemMenu = new DropDownMenu("Select Item");

for (ItemStack item : availableItems) {
    Frame itemFrame = new Frame();
    itemFrame.setLayout(new LayoutHorizontal());
    
    ItemView itemView = new ItemView(item);
    Label itemName = new Label(item.getHoverName().getString());
    
    itemFrame.addChild(itemView);
    itemFrame.addChild(itemName);
    
    itemMenu.addChild(itemFrame);
}

itemMenu.setOnOptionSelected((index, element) -> {
    ItemStack selectedItem = availableItems.get(index);
    handleItemSelection(selectedItem);
});
```

### Sorting Menu
```java
DropDownMenu sortMenu = new DropDownMenu("Sort By");
sortMenu.setSize(150, 20);
sortMenu.addOption("Name (A-Z)");
sortMenu.addOption("Name (Z-A)");
sortMenu.addOption("Price (Low to High)");
sortMenu.addOption("Price (High to Low)");
sortMenu.addOption("Date Added");

sortMenu.setOnOptionSelected((index, element) -> {
    switch (index) {
        case 0 -> sortByNameAsc();
        case 1 -> sortByNameDesc();
        case 2 -> sortByPriceAsc();
        case 3 -> sortByPriceDesc();
        case 4 -> sortByDate();
    }
    refreshList();
});
```

### Cascading Dropdowns
```java
DropDownMenu categoryMenu = new DropDownMenu("Category");
DropDownMenu subcategoryMenu = new DropDownMenu("Subcategory");
subcategoryMenu.setEnabled(false);

categoryMenu.addOption("Electronics");
categoryMenu.addOption("Clothing");
categoryMenu.addOption("Food");

categoryMenu.setOnOptionSelected((index, element) -> {
    subcategoryMenu.clearOptions();
    subcategoryMenu.setEnabled(true);
    
    switch (index) {
        case 0 -> {  // Electronics
            subcategoryMenu.addOption("Computers");
            subcategoryMenu.addOption("Phones");
            subcategoryMenu.addOption("Tablets");
        }
        case 1 -> {  // Clothing
            subcategoryMenu.addOption("Shirts");
            subcategoryMenu.addOption("Pants");
            subcategoryMenu.addOption("Shoes");
        }
        case 2 -> {  // Food
            subcategoryMenu.addOption("Fruits");
            subcategoryMenu.addOption("Vegetables");
            subcategoryMenu.addOption("Meat");
        }
    }
});
```

## Common Patterns

### Setting Initial Selection
```java
DropDownMenu menu = new DropDownMenu("Language");
menu.addOption("English");
menu.addOption("Spanish");
menu.addOption("French");
menu.addOption("German");

// Load saved preference
int savedLanguage = config.getLanguageIndex();
menu.setSelectedIndex(savedLanguage);
```

### Dynamic Options
```java
DropDownMenu serverMenu = new DropDownMenu("Select Server");

void updateServerList(List<String> servers) {
    serverMenu.clearOptions();
    for (String server : servers) {
        serverMenu.addOption(server);
    }
}

// Call whenever server list changes
updateServerList(getAvailableServers());
```

### Filtering with Dropdown
```java
DropDownMenu filterMenu = new DropDownMenu("Filter");
filterMenu.addOption("All Items");
filterMenu.addOption("Weapons");
filterMenu.addOption("Armor");
filterMenu.addOption("Tools");

filterMenu.setOnOptionSelected((index, element) -> {
    String filter = index == 0 ? null : ((Label) element).getText();
    itemList.applyFilter(filter);
});
```

### Save Selection State
```java
DropDownMenu menu = new DropDownMenu("Graphics Quality");
menu.addOption("Low");
menu.addOption("Medium");
menu.addOption("High");
menu.addOption("Ultra");

// Load saved setting
menu.setSelectedIndex(settings.getGraphicsQuality());

menu.setOnOptionSelected((index, element) -> {
    settings.setGraphicsQuality(index);
    settings.save();
    applyGraphicsSettings(index);
});
```

## Best Practices

1. **Z-Layering**: Use `setExpandedZPos()` to ensure the expanded menu renders above other elements
2. **Max Height**: Set `setMaxExpandedHeight()` to prevent the menu from extending off-screen
3. **Auto-Collapse**: The menu automatically collapses when clicking outside its bounds
4. **Label Updates**: Update the label to reflect the current selection for clarity
5. **Loading State**: Disable the menu during async operations to prevent invalid selections
6. **Option Count**: For more than 10-15 options, consider using a searchable list instead
7. **Custom Elements**: Use custom elements for options that need icons or complex layouts

## Events and Callbacks

The selection callback receives:
- **index**: The zero-based index of the selected option
- **element**: The GuiElement that was clicked (can be cast to specific type)

```java
menu.setOnOptionSelected((index, element) -> {
    if (element instanceof Label label) {
        String text = label.getText();
        System.out.println("Selected: " + text + " at index " + index);
    }
});
```

## Technical Notes

- The dropdown uses a `VerticalListView` with `LayoutVertical` for option display
- Options are wrapped in internal `EmptyButtonChanged` elements for click detection
- The expand button shows arrow icons from mod textures (`arrow_down.png`, `arrow_up.png`)
- Clicking outside the expanded menu automatically collapses it
- The menu changes its Z position when expanded to render above other elements
- Internal layout is automatically recalculated when options are added or removed
