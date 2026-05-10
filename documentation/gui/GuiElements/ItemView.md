# ItemView

## Overview

The `ItemView` element displays a Minecraft ItemStack with optional count decoration and tooltip. It automatically centers the item in its bounds and can scale the item for smaller displays.

**When to use:**
- Display items in inventory-like interfaces
- Item selection menus
- Recipe displays
- Item information panels

## Constructor

```java
ItemView()                                          // Default 16x16 item view
ItemView(int x, int y, int width, int height)      // Positioned item view
ItemView(ItemStack itemStack)                      // Item view with item
ItemView(int x, int y, int width, int height, ItemStack itemStack)
```

**Parameters:**
- `x`, `y` - Position coordinates
- `width`, `height` - Size dimensions
- `itemStack` - The ItemStack to display

## Key Methods

### Item Management
```java
void setItemStack(ItemStack itemStack)     // Set the item to display
ItemStack getItemStack()                   // Get the current item
```

### Display Options
```java
void setShowCount(boolean showCount)       // Show/hide item count
boolean isShowCount()                      // Check if count is shown
void setShowTooltip(boolean showTooltip)   // Enable/disable tooltip on hover
boolean isShowTooltip()                    // Check if tooltip is enabled
```

## Constants

```java
public static final int DEFAULT_WIDTH = 16;  // Default item view size (16x16)
```

## Code Examples

### Basic Item Display
```java
ItemStack diamond = new ItemStack(Items.DIAMOND, 5);
ItemView itemView = new ItemView(diamond);
itemView.setShowCount(true);
itemView.setShowTooltip(true);
gui.addChild(itemView);
```

### Item Grid
```java
Frame itemGrid = new Frame(0, 0, 200, 200);
itemGrid.setLayout(new LayoutGrid(4, 0, false, false, 0, 0, Alignment.TOP));

for (ItemStack item : inventory) {
    ItemView view = new ItemView(item);
    view.setShowTooltip(true);
    itemGrid.addChild(view);
}
```

### Scaled Item Display
```java
// Large item display
ItemView largeView = new ItemView(0, 0, 64, 64, itemStack);
largeView.setShowCount(false);  // Count looks odd at large scales

// Small item display
ItemView smallView = new ItemView(0, 0, 8, 8, itemStack);
// Automatically scales down
```

## Best Practices

1. **Default Size**: Use DEFAULT_WIDTH (16) for standard Minecraft item size
2. **Count Display**: Enable for stacks, disable for single items or large displays
3. **Tooltips**: Enable tooltips for interactive item views
4. **Scaling**: Items smaller than 16x16 are automatically scaled down
5. **Background**: Background and outline are disabled by default

## Technical Notes

- Items are automatically centered in the element bounds
- Items smaller than DEFAULT_WIDTH are scaled proportionally
- Tooltip shows the item's hover name and additional information
- The element extends GuiElement with background/outline disabled by default
