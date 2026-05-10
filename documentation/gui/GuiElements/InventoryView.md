# InventoryView

## Overview

The `InventoryView` displays a player's inventory with interactive item slots. It supports drag-and-drop, item splitting, quick-move (shift-click), and all standard Minecraft inventory interactions. Ideal for custom inventory interfaces and container GUIs.

**When to use:**
- Custom inventory screens
- Player equipment displays
- Storage container interfaces
- Item management systems

## Constructor

```java
InventoryView(int x, int y, Inventory inventory, String modID, String texturePath, int textureWidth, int textureHeight)
```

**Parameters:**
- `x`, `y` - Position coordinates
- `inventory` - The Minecraft Inventory to display
- `modID` - Mod ID for the background texture
- `texturePath` - Path to the background texture
- `textureWidth`, `textureHeight` - Texture dimensions

## Constants

```java
public static final int SLOT_SIZE = 16;  // Size of each slot in pixels
```

## Key Methods

### Slot Interaction (Protected - Override for Custom Behavior)
```java
protected void onLeftMouseClickOnSlot(int slotIndex)
protected void onRightMouseClickOnSlot(int slotIndex)
protected void onMiddleMouseButtonClickOnSlot(int slotIndex)
protected int getMouseSlotIndex()
protected boolean isMouseOverSlot(int x, int y)
```

### Rendering (Protected - Override for Custom Rendering)
```java
protected void renderSlot(int x, int y, int slotIndex)
protected void buildSlotPositions()
```

## Default Slot Layout

The default implementation creates a standard Minecraft player inventory layout:
- **Hotbar**: 9 slots at the bottom
- **Main Inventory**: 27 slots (3 rows of 9) above the hotbar

Override `buildSlotPositions()` to create custom layouts.

## Mouse Interactions

### Left Click
- Empty hand + item in slot: Pick up entire stack
- Item in hand + empty slot: Place entire stack
- Item in hand + matching item in slot: Merge stacks
- Item in hand + different item in slot: Swap items
- Shift + Click: Quick-move to other section (hotbar ↔ inventory)

### Right Click
- Empty hand + item in slot: Pick up half the stack
- Item in hand + empty slot: Place one item
- Item in hand + matching item in slot: Add one item
- Shift + Click: (Same as left-click quick-move)

### Middle Click
- Override `onMiddleMouseButtonClickOnSlot()` for custom behavior

## Code Examples

### Basic Player Inventory
```java
Inventory playerInventory = minecraft.player.getInventory();

InventoryView invView = new InventoryView(
    10, 10,
    playerInventory,
    "modutilities",
    "textures/gui/inventory_background.png",
    176, 166
);

gui.addChild(invView);
```

### Custom Slot Layout
```java
public class CustomInventoryView extends InventoryView {
    
    public CustomInventoryView(Inventory inventory) {
        super(0, 0, inventory, "mymod", "textures/gui/custom.png", 200, 200);
    }
    
    @Override
    protected void buildSlotPositions() {
        slotPositions.clear();
        
        // Create a 5x5 grid
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int x = 10 + col * (SLOT_SIZE + 2);
                int y = 10 + row * (SLOT_SIZE + 2);
                slotPositions.add(new Point(x, y));
            }
        }
    }
}
```

### Custom Slot Rendering
```java
public class HighlightInventoryView extends InventoryView {
    
    private int highlightedSlot = -1;
    
    public void setHighlightedSlot(int slot) {
        this.highlightedSlot = slot;
    }
    
    @Override
    protected void renderSlot(int x, int y, int slotIndex) {
        super.renderSlot(x, y, slotIndex);
        
        if (slotIndex == highlightedSlot) {
            // Draw gold border around highlighted slot
            drawFrame(x, y, SLOT_SIZE, SLOT_SIZE, 0xFFFFD700, 2);
        }
    }
}
```

### Locked Slots
```java
public class LockedInventoryView extends InventoryView {
    
    private final Set<Integer> lockedSlots = new HashSet<>();
    
    public void lockSlot(int slotIndex) {
        lockedSlots.add(slotIndex);
    }
    
    @Override
    protected void onLeftMouseClickOnSlot(int slotIndex) {
        if (lockedSlots.contains(slotIndex)) {
            // Play error sound or show message
            return;
        }
        super.onLeftMouseClickOnSlot(slotIndex);
    }
    
    @Override
    protected void renderSlot(int x, int y, int slotIndex) {
        super.renderSlot(x, y, slotIndex);
        
        if (lockedSlots.contains(slotIndex)) {
            drawRect(x, y, SLOT_SIZE, SLOT_SIZE, 0x80FF0000);  // Red overlay
        }
    }
}
```

## Common Patterns

### Armor Equipment Slots
```java
@Override
protected void buildSlotPositions() {
    slotPositions.clear();
    
    // Armor slots (4 slots vertically)
    for (int i = 0; i < 4; i++) {
        slotPositions.add(new Point(8, 8 + i * 18));
    }
    
    // Offhand slot
    slotPositions.add(new Point(77, 62));
    
    // Main inventory (3x9)
    for (int row = 0; row < 3; row++) {
        for (int col = 0; col < 9; col++) {
            int x = 8 + col * 18;
            int y = 84 + row * 18;
            slotPositions.add(new Point(x, y));
        }
    }
    
    // Hotbar (1x9)
    for (int col = 0; col < 9; col++) {
        slotPositions.add(new Point(8 + col * 18, 142));
    }
}
```

### Item Filtering
```java
@Override
protected void onLeftMouseClickOnSlot(int slotIndex) {
    ItemStack draggedItem = getDragingStack();
    ItemStack slotItem = inventory.getItem(slotIndex);
    
    // Only allow tools in this slot
    if (!draggedItem.isEmpty() && !(draggedItem.getItem() instanceof ToolItem)) {
        // Reject the interaction
        return;
    }
    
    super.onLeftMouseClickOnSlot(slotIndex);
}
```

## Best Practices

1. **Background Texture**: Ensure the texture properly indicates slot positions
2. **Slot Spacing**: Standard Minecraft spacing is 18 pixels (16px item + 2px gap)
3. **Hover Feedback**: The default hover effect is a white semi-transparent overlay
4. **Dragging State**: The `dragingStack` field tracks the currently dragged item
5. **Quick Move**: Override `quickMoveInsert()` for custom shift-click behavior
6. **Slot Count**: Ensure slot positions match the inventory size

## Technical Notes

- The inventory view manages a `dragingStack` for drag-and-drop operations
- Slot positions are stored as Point objects in the `slotPositions` list
- Items are rendered with decorations (count, durability bar, enchantment glint)
- The default layout assumes a standard player inventory (36 slots)
- Scissors are enabled during rendering to clip content to bounds
- Mouse interactions support standard Minecraft inventory controls
