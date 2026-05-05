# ContainerView

## Overview

The `ContainerView` is a sophisticated container interface that integrates with Minecraft's `AbstractContainerMenu` system. It provides full inventory interaction capabilities including drag-and-drop, quick-craft, item stacking, and keyboard shortcuts. This is a complete implementation of Minecraft's standard container interface.

**When to use:**
- Custom chest/storage GUIs
- Crafting table interfaces
- Furnace and processing GUIs
- Any container that uses AbstractContainerMenu

## Constructor

```java
ContainerView(T menu, Inventory playerInventory, Component title, GuiTexture backgroundTexture)
```

**Type Parameter:**
- `T extends AbstractContainerMenu` - The container menu type

**Parameters:**
- `menu` - The AbstractContainerMenu instance
- `playerInventory` - The player's inventory
- `title` - The container's display title
- `backgroundTexture` - The background GUI texture

## Key Methods

### Menu Access
```java
T getMenu()                                    // Get the container menu
Slot getSlotUnderMouse()                       // Get the hovered slot
int getSlotColor(int index)                    // Get slot highlight color
```

### Lifecycle
```java
void init()                                    // Initialize the container
void tick()                                    // Tick the container (called each frame)
void removed()                                 // Clean up when container closes
void onClose()                                 // Close the container
void setOnCloseEvent(Runnable callback)        // Set callback for close event
```

### Label Positioning
```java
protected int titleLabelX                      // X position of title label
protected int titleLabelY                      // Y position of title label
protected int inventoryLabelX                  // X position of inventory label
protected int inventoryLabelY                  // Y position of inventory label
```

### Protected Methods (Override for Custom Behavior)
```java
protected void renderLabels(int mouseX, int mouseY)
protected void renderTooltip(int mouseX, int mouseY)
protected void renderSlot(Slot slot)
protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type)
protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button)
protected void containerTick()
```

## Click Types

The container supports all Minecraft click types:
- **PICKUP** - Standard click to pick up/place items
- **QUICK_MOVE** - Shift-click to quick-move items
- **SWAP** - Number keys to swap with hotbar
- **CLONE** - Middle-click in creative mode
- **THROW** - Q key to drop items
- **QUICK_CRAFT** - Drag to distribute items
- **PICKUP_ALL** - Double-click to gather items

## Code Examples

### Basic Chest Container
```java
public class ChestContainerView extends ContainerView<ChestMenu> {
    
    public ChestContainerView(ChestMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, 
            new GuiTexture("minecraft", "textures/gui/container/generic_54.png", 176, 222));
    }
    
    @Override
    protected void containerTick() {
        // Custom tick logic
    }
}
```

### Custom Slot Rendering
```java
@Override
protected void renderSlot(Slot slot) {
    super.renderSlot(slot);
    
    // Highlight certain slots
    if (slot.index < 9) {  // Hotbar slots
        int x = slot.x;
        int y = slot.y;
        renderSlotHighlight(x, y, 0, 0x803366FF);  // Blue highlight
    }
}
```

### Custom Click Behavior
```java
@Override
protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
    // Prevent removing items from certain slots
    if (slot != null && slot.index < 5 && type == ClickType.PICKUP) {
        // Play error sound
        return;
    }
    
    super.slotClicked(slot, slotId, mouseButton, type);
}
```

### Container with Close Callback
```java
ContainerView<?> container = new ContainerView<>(menu, playerInv, title, texture);

container.setOnCloseEvent(() -> {
    System.out.println("Container closed!");
    saveContainerState();
    playCloseSound();
});
```

### Custom Label Positioning
```java
public class CustomContainer extends ContainerView<MyMenu> {
    
    public CustomContainer(MyMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, texture);
        
        // Customize label positions
        this.titleLabelX = 10;
        this.titleLabelY = 5;
        this.inventoryLabelX = 10;
        this.inventoryLabelY = 85;
    }
}
```

## Keyboard Shortcuts

The container handles standard Minecraft keyboard shortcuts:
- **Number Keys (1-9)**: Swap items with hotbar slots
- **F**: Swap item with offhand
- **Q**: Drop single item
- **Ctrl+Q**: Drop entire stack
- **Shift+Click**: Quick-move items
- **Middle Click**: Clone item (creative mode)
- **Escape/E**: Close container

## Common Patterns

### Locked Slots
```java
@Override
protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
    if (isSlotLocked(slot)) {
        playLocalSound(SoundEvents.ITEM_BREAK, 1.0f);
        return;  // Prevent interaction
    }
    super.slotClicked(slot, slotId, mouseButton, type);
}

private boolean isSlotLocked(Slot slot) {
    return slot != null && lockedSlotIndices.contains(slot.index);
}
```

### Animated Slots
```java
private int tickCounter = 0;

@Override
protected void containerTick() {
    tickCounter++;
}

@Override
protected void renderSlot(Slot slot) {
    super.renderSlot(slot);
    
    if (shouldAnimateSlot(slot)) {
        float pulse = (float)(Math.sin(tickCounter * 0.1) * 0.5 + 0.5);
        int alpha = (int)(pulse * 128);
        renderSlotHighlight(slot.x, slot.y, 0, 0xFFFF00 | (alpha << 24));
    }
}
```

### Custom Tooltip
```java
@Override
protected void renderTooltip(int mouseX, int mouseY) {
    if (getMenu().getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.hasItem()) {
        ItemStack stack = hoveredSlot.getItem();
        
        // Add custom tooltip lines
        List<Component> tooltip = new ArrayList<>(getTooltipFromContainerItem(stack));
        tooltip.add(Component.literal("Slot: " + hoveredSlot.index).withStyle(ChatFormatting.GRAY));
        
        drawTooltip(stack, mouseX, mouseY);
    }
}
```

## Best Practices

1. **Menu Integration**: Ensure your AbstractContainerMenu is properly implemented
2. **Texture Size**: The background texture should match your container layout
3. **Slot Positions**: Slot positions are defined in your AbstractContainerMenu
4. **State Management**: Use `containerTick()` for updates, not `render()`
5. **Cleanup**: Always call `super.removed()` in overridden `removed()` methods
6. **Click Handling**: Call `super.slotClicked()` unless you want to completely override behavior
7. **Thread Safety**: Container operations should be on the main thread

## Technical Notes

- Extends GuiElement and implements MenuAccess interface
- Integrates with Minecraft's client-side inventory system via GameMode
- Supports touchscreen mode for mobile/tablet devices
- Handles all standard Minecraft container interactions
- Uses RenderSystem for depth testing and rendering
- Snap-back animation for invalid item placements
- Quick-craft system for distributing items across slots
- Double-click to gather matching items
