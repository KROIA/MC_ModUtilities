package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.Graphics;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.minecraft.world.item.ItemStack;

/**
 * Renders a single {@link ItemStack} as a Minecraft inventory icon.
 * <p>
 * The element draws the item with optional count decoration and tooltip on
 * hover. When the element is sized smaller than the default 16x16 slot the
 * item is scaled down via the GUI pose stack; when larger the item is centered
 * within the bounds. Background and outline of the underlying
 * {@link GuiElement} are disabled by default to match a typical icon use case.
 *
 * @apiNote A {@code null} {@link ItemStack} is allowed and simply renders
 *          nothing; use {@link ItemStack#EMPTY} or {@code null} to clear.
 */
public class ItemView extends GuiElement {
    /** Default render size in pixels (matches a Minecraft slot). */
    public static final int DEFAULT_WIDTH = 16;

    private boolean showCount = false;
    private boolean showTooltip = true;
    protected ItemStack itemStack;
    protected Point itemPos = new Point(0,0);

    /**
     * Creates an empty {@code ItemView} sized to {@link #DEFAULT_WIDTH}x{@link #DEFAULT_WIDTH}
     * with no item assigned.
     */
    public ItemView() {
        super(0,0,DEFAULT_WIDTH, DEFAULT_WIDTH);
        setEnableBackground(false);
        setEnableOutline(false);
    }

    /**
     * Creates an empty {@code ItemView} at the given position and size.
     *
     * @param x      the x-coordinate relative to the parent
     * @param y      the y-coordinate relative to the parent
     * @param width  the width in pixels
     * @param height the height in pixels
     */
    public ItemView(int x, int y, int width, int height) {
        super(x, y, width, height);
        setEnableBackground(false);
        setEnableOutline(false);
    }

    /**
     * Creates an {@code ItemView} displaying the given item stack at the
     * default size.
     *
     * @param itemStack the item stack to display, may be {@code null}
     */
    public ItemView(ItemStack itemStack) {
        super(0,0,DEFAULT_WIDTH, DEFAULT_WIDTH);
        this.itemStack = itemStack;
        setEnableBackground(false);
        setEnableOutline(false);
    }

    /**
     * Creates an {@code ItemView} displaying the given item stack at the
     * given position and size.
     *
     * @param x         the x-coordinate relative to the parent
     * @param y         the y-coordinate relative to the parent
     * @param width     the width in pixels
     * @param height    the height in pixels
     * @param itemStack the item stack to display, may be {@code null}
     */
    public ItemView(int x, int y, int width, int height, ItemStack itemStack) {
        super(x, y, width, height);
        this.itemStack = itemStack;
        setEnableBackground(false);
        setEnableOutline(false);
    }

    /**
     * Sets the item stack rendered by this element.
     *
     * @param itemStack the new item stack, may be {@code null}
     */
    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    /**
     * @return the currently rendered item stack, or {@code null} if none is assigned
     */
    public ItemStack getItemStack() {
        return itemStack;
    }


    /**
     * Toggles whether the stack count decoration (number overlay) is drawn.
     *
     * @param showCount {@code true} to show the count, {@code false} to hide it
     */
    public void setShowCount(boolean showCount) {
        this.showCount = showCount;
    }

    /**
     * @return {@code true} if the stack count is rendered as a decoration
     */
    public boolean isShowCount() {
        return showCount;
    }

    /**
     * Toggles whether the item tooltip is drawn while the mouse hovers over
     * the element.
     *
     * @param showTooltip {@code true} to enable hover tooltips, {@code false} to disable
     */
    public void setShowTooltip(boolean showTooltip) {
        this.showTooltip = showTooltip;
    }

    /**
     * @return {@code true} if hover tooltips are enabled
     */
    public boolean isShowTooltip() {
        return showTooltip;
    }
    @Override
    protected void render() {
        if(itemStack == null)
            return;

        if(showCount)
            drawItemWithDecoration(itemStack, itemPos, itemStack.getCount());
        else {
            int width = getWidth();
            if(width < DEFAULT_WIDTH) {
                var graphics = getGraphics();
                width = Math.min(width, getHeight());
                graphics.pushPose();
                graphics.translate(itemPos.x, itemPos.y, 0);
                float scale = width / (float) (DEFAULT_WIDTH);
                graphics.scale(scale, scale, 1);
                drawItem(itemStack, 0,0);
                graphics.popPose();
            }
            else {
                drawItem(itemStack, itemPos);
            }
        }
        if(showTooltip && isMouseOver()) {
            drawTooltip(itemStack, getMousePos());
        }
    }

    @Override
    protected void layoutChanged() {
        itemPos.x = Math.max(0, (getWidth() - DEFAULT_WIDTH) / 2);
        itemPos.y = Math.max(0, (getHeight() - DEFAULT_WIDTH) / 2);
    }
}
