package net.kroia.modutilities.gui.layout;

import net.kroia.modutilities.gui.elements.base.GuiElement;

import java.util.ArrayList;

public class LayoutVertical extends Layout {
    public LayoutVertical(){
        super();
    }
    public LayoutVertical(int padding, int spacing, boolean stretchX, boolean stretchY) {
        super(padding, spacing, stretchX, stretchY);
    }

    @Override
    public void apply(GuiElement element) {
        ArrayList<GuiElement> childs = element.getChilds();
        if(childs.isEmpty())
            return;
        int y = padding;
        int height = (element.getHeight()-padding*2+spacing)/childs.size()-spacing;
        for (GuiElement child : childs) {
            child.setX(padding);
            child.setY(y);
            if(stretchX) {
                child.setWidth(element.getWidth()-2*padding);
            }
            if(stretchY) {
                child.setHeight(height);
            }
            y += child.getHeight() + spacing;
        }
    }
}
