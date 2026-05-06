package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.GuiTexture;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

/**
 * Renders a Minecraft player {@link Inventory} (hotbar plus the 3x9 main grid)
 * as an interactive GUI element with click-and-drag item movement.
 * <p>
 * The element draws the supplied background texture and overlays
 * 9 hotbar slots (indices 0-8) and 27 main inventory slots (indices 9-35).
 * Slot interaction supports left/right/middle clicks, shift-click quick move,
 * and split/merge stack handling via an internal {@code dragingStack}.
 *
 * @apiNote The shift-click quick-move behavior intentionally targets only the
 *          main inventory slots (range {@code 9-35}) so items are not pushed
 *          into armor/offhand slots that this view does not render.
 */
public class InventoryView extends GuiElement {

    /** Edge size in pixels of a single inventory slot. */
    public static final int SLOT_SIZE = 16;
    protected final Inventory inventory;
    protected ItemStack dragingStack = ItemStack.EMPTY;
    protected final GuiTexture backgroundTexture;
    protected final Point backgroundTexturePosition = new Point(0, 0);
    protected final ArrayList<Point> slotPositions = new ArrayList<>();

    /**
     * Creates a new {@code InventoryView} backed by the given player inventory
     * and rendered behind the supplied background texture.
     *
     * @param x             the x-coordinate relative to the parent
     * @param y             the y-coordinate relative to the parent
     * @param inventory     the player inventory whose slots are rendered and mutated
     * @param modID         the namespace of the background texture
     * @param texturePath   the resource path of the background texture
     * @param textureWidth  the texture's native width (also used as the element width)
     * @param textureHeight the texture's native height (also used as the element height)
     */
    public InventoryView(int x, int y, Inventory inventory, String modID, String texturePath, int textureWidth, int textureHeight) {
        super(x, y, textureWidth, textureHeight);

        this.inventory = inventory;
        this.backgroundTexture = new GuiTexture(modID, texturePath, textureWidth, textureHeight);
        buildSlotPositions();
    }

    protected void buildSlotPositions()
    {

        final int SLOT_SPACING = 2;
        final int SLOT_ROWS = 3;
        final int SLOT_COLUMNS = 9;

        int xOffset = 8;
        int yOffset = 76;

        // Hotbar
        for (int column = 0; column < SLOT_COLUMNS; column++) {
            int slotX = (column * (SLOT_SIZE + SLOT_SPACING));
            slotPositions.add(new Point(slotX+xOffset, yOffset));
        }

        // Inventory
        xOffset = 8;
        yOffset = 18;
        for (int row = 0; row < SLOT_ROWS; row++) {
            for (int column = 0; column < SLOT_COLUMNS; column++) {
                int slotX = (column * (SLOT_SIZE + SLOT_SPACING));
                int slotY = (row * (SLOT_SIZE + SLOT_SPACING));
                slotPositions.add(new Point(slotX+xOffset, slotY+yOffset));
            }
        }
    }

    @Override
    protected void renderBackground()
    {
        enableScissor();
        drawTexture(backgroundTexture, backgroundTexturePosition);
        disableScissor();
    }
    @Override
    protected void render() {
        enableScissor();
        for(int i=0; i<slotPositions.size(); i++)
        {
            Point point = slotPositions.get(i);
            renderSlot(point.x, point.y, i);
        }

        if(!dragingStack.isEmpty())
        {
            // Render dragging item
            drawItemWithDecoration(dragingStack, getMouseX()-8, getMouseY()-8, 210,0);
        }
        disableScissor();
    }

    @Override
    protected void layoutChanged() {

    }

    protected void renderSlot(int x, int y, int slotIndex) {
        // Render slot
        ItemStack stack = inventory.getItem(slotIndex);
        // Render slot background
        if(isMouseOverSlot(x, y))
        {
            // Render hover effect
            drawRect(x, y, SLOT_SIZE, SLOT_SIZE, 0x80FFFFFF);
        }

        // Render item stack
        if (!stack.isEmpty()) {
            drawItemWithDecoration(stack, x, y);
        }
    }
    protected boolean isMouseOverSlot(int x, int y)
    {
        return new Rectangle(x, y, SLOT_SIZE, SLOT_SIZE).contains(getMouseX(), getMouseY());
    }

    @Override
    protected boolean mouseClickedOverElement(int button) {

        int slotIndex = getMouseSlotIndex();
        if(slotIndex == -1)
            return false;
        switch(button)
        {
            case 0: // Left click
            {
                onLeftMouseClickOnSlot(slotIndex);
                return true;
            }
            case 1: // Right click
            {
                onRightMouseClickOnSlot(slotIndex);
                return true;
            }
            case 2: // Middle click
            {
                onMiddleMouseButtonCloickOnSlot(slotIndex);
                return true;
            }
        }
        return false;
    }

    protected void onLeftMouseClickOnSlot(int slotIndex)
    {
        boolean isShiftDown = Screen.hasShiftDown();
        if (dragingStack.isEmpty()) {
            if(isShiftDown)
            {
                if(slotIndex < 9) {
                    quickMoveInsert(slotIndex, 9, 35);
                }
                else {
                    quickMoveInsert(slotIndex, 0, 8);
                }
            }
            else {
                dragingStack = inventory.getItem(slotIndex);
                inventory.setItem(slotIndex, ItemStack.EMPTY);
            }
        } else {
            ItemStack stack = inventory.getItem(slotIndex);
            // Try to merge stacks
            if(stack.isEmpty())
            {
                inventory.setItem(slotIndex, dragingStack);
                dragingStack = ItemStack.EMPTY;
            }
            else if (stack.getItem().getDescriptionId().compareTo(dragingStack.getItem().getDescriptionId()) == 0) {
                int max = stack.getMaxStackSize();
                if (stack.getCount() + dragingStack.getCount() <= max) {
                    stack.grow(dragingStack.getCount());
                    inventory.setItem(slotIndex, stack);
                    dragingStack = ItemStack.EMPTY;
                } else {
                    int diff = max - stack.getCount();
                    stack.grow(diff);
                    dragingStack.shrink(diff);
                }
            } else {
                // Swap stacks
                inventory.setItem(slotIndex, dragingStack);
                dragingStack = stack;
            }
        }
    }
    protected void onRightMouseClickOnSlot(int slotIndex)
    {
        ItemStack stack = inventory.getItem(slotIndex);
        if (dragingStack.isEmpty()) {
            if (!stack.isEmpty()) {
                int count = stack.getCount();
                int half = count / 2;
                dragingStack = stack.split(half);
                if (stack.isEmpty()) {
                    inventory.setItem(slotIndex, ItemStack.EMPTY);
                }
            }
        } else {
            if (stack.isEmpty()) {
                ItemStack insertStack = dragingStack.split(1);
                inventory.setItem(slotIndex, insertStack);
                if(dragingStack.isEmpty())
                    dragingStack = ItemStack.EMPTY;

            } else if (stack.getItem().getDescriptionId().compareTo(dragingStack.getItem().getDescriptionId()) == 0) {
                int max = stack.getMaxStackSize();
                if (stack.getCount() + 1 <= max) {
                    stack.grow(1);
                    dragingStack.shrink(1);
                    if(dragingStack.isEmpty())
                        dragingStack = ItemStack.EMPTY;
                    inventory.setItem(slotIndex, stack);
                }
            }
        }
    }
    protected void onMiddleMouseButtonCloickOnSlot(int slotIndex)
    {

    }

    protected int getMouseSlotIndex()
    {
        for(int i=0; i<slotPositions.size(); i++)
        {
            Point point = slotPositions.get(i);
            if (isMouseOverSlot(point.x, point.y)) {
                return i;
            }
        }
        return -1;
    }

    protected void quickMoveInsert(int fromSlot, int toSlotBegin, int toSlotEnd)
    {
        ItemStack stack = inventory.getItem(fromSlot);
        if(stack.isEmpty())
            return;

        for(int i=toSlotBegin; i<=toSlotEnd; i++)
        {
            int count = stack.getCount();
            ItemStack toStack = inventory.getItem(i);
            if(toStack.isEmpty())
            {
                inventory.setItem(i, stack);
                inventory.setItem(fromSlot, ItemStack.EMPTY);
                return;
            }
            else if(toStack.getItem().getDescriptionId().compareTo(stack.getItem().getDescriptionId()) == 0)
            {
                int max = toStack.getMaxStackSize();
                if(toStack.getCount() + count <= max)
                {
                    toStack.grow(count);
                    inventory.setItem(i, toStack);
                    inventory.setItem(fromSlot, ItemStack.EMPTY);
                    return;
                }
                else
                {
                    int diff = max - toStack.getCount();
                    toStack.grow(diff);
                    stack.shrink(diff);
                    inventory.setItem(i, toStack);
                    inventory.setItem(fromSlot, stack);
                }
            }
        }
    }
}
