package net.kroia.modutilities.gui.layout;

import net.kroia.modutilities.gui.elements.base.GuiElement;

import java.util.ArrayList;

public class LayoutHorizontal extends Layout {

    public LayoutHorizontal(){
        super();
    }
    public LayoutHorizontal(int padding, int spacing, boolean stretchX, boolean stretchY) {
        super(padding, spacing, stretchX, stretchY);
    }
    @Override
    public void apply(GuiElement element) {
        ArrayList<GuiElement> childs = element.getChilds();
        if(childs.isEmpty())
            return;
        int x = padding;
        int width = (element.getWidth()-padding*2+spacing)/childs.size() - spacing;
        for (GuiElement child : childs) {
            child.setX(x);
            child.setY(padding);
            if(stretchX) {
                child.setWidth(width);
            }
            if(stretchY) {
                child.setHeight(element.getHeight()-2*padding);
            }
            x += child.getWidth() + spacing;
        }
    }
}
