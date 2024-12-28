package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.minecraft.world.item.ItemStack;

public class ItemView extends GuiElement {
    public static final int DEFAULT_WIDTH = 16;

    private boolean showCount = false;
    private boolean showTooltip = true;
    protected ItemStack itemStack;
    protected Point itemPos = new Point(0,0);
    public ItemView() {
        super(0,0,DEFAULT_WIDTH,DEFAULT_WIDTH);
        setEnableBackground(false);
        setEnableOutline(false);
    }
    public ItemView(int x, int y, int width, int height) {
        super(x, y, width, height);
        setEnableBackground(false);
        setEnableOutline(false);
    }

    public ItemView(ItemStack itemStack) {
        super(0,0,DEFAULT_WIDTH,DEFAULT_WIDTH);
        this.itemStack = itemStack;
        setEnableBackground(false);
        setEnableOutline(false);
    }
    public ItemView(int x, int y, int width, int height, ItemStack itemStack) {
        super(x, y, width, height);
        this.itemStack = itemStack;
        setEnableBackground(false);
        setEnableOutline(false);
    }
    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }


    public void setShowCount(boolean showCount) {
        this.showCount = showCount;
    }
    public boolean isShowCount() {
        return showCount;
    }
    public void setShowTooltip(boolean showTooltip) {
        this.showTooltip = showTooltip;
    }
    public boolean isShowTooltip() {
        return showTooltip;
    }
    @Override
    protected void render() {
        if(itemStack == null)
            return;

        if(showCount)
            drawItemWithDecoration(itemStack, itemPos, itemStack.getCount());
        else
            drawItem(itemStack, itemPos);
        if(showTooltip && isMouseOver())
            drawTooltip(itemStack, getMousePos());
    }

    @Override
    protected void layoutChanged() {
        itemPos.x = (getWidth() - DEFAULT_WIDTH) / 2;
        itemPos.y = (getHeight() - DEFAULT_WIDTH) / 2;
    }
}
