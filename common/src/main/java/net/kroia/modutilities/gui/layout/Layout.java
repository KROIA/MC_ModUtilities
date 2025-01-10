package net.kroia.modutilities.gui.layout;

import net.kroia.modutilities.gui.elements.base.GuiElement;

public abstract class Layout {
    public boolean enabled = true;
    public int padding = GuiElement.DEFAULT_PADDING;
    public int spacing = GuiElement.DEFAULT_PADDING;
    public boolean stretchX = false;
    public boolean stretchY = false;

    public Layout(){

    }
    public Layout(int padding, int spacing, boolean stretchX, boolean stretchY) {
        this.padding = padding;
        this.spacing = spacing;
        this.stretchX = stretchX;
        this.stretchY = stretchY;
    }
    public abstract void apply(GuiElement element);
}

