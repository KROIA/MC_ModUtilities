package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.minecraft.world.item.ItemStack;

public class ItemView extends GuiElement {
    public static final int DEFAULT_WIDTH = 16;

    ItemStack itemStack;
    Point itemPos = new Point(0,0);
    public ItemView() {
        super(0,0,DEFAULT_WIDTH,DEFAULT_WIDTH);
    }
    public ItemView(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public ItemView(ItemStack itemStack) {
        super();
        this.itemStack = itemStack;
    }
    public ItemView(int x, int y, int width, int height, ItemStack itemStack) {
        super(x, y, width, height);
        this.itemStack = itemStack;
    }
    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }


    @Override
    protected void render() {
        if(itemStack == null)
            return;

        drawItem(itemStack, itemPos);
    }

    @Override
    protected void layoutChanged() {
        itemPos.x = (getWidth() - DEFAULT_WIDTH) / 2;
        itemPos.y = (getHeight() - DEFAULT_WIDTH) / 2;
    }
}
