package net.kroia.modutilities.gui.layout;

import net.kroia.modutilities.gui.elements.base.GuiElement;

import java.util.ArrayList;

public class LayoutGrid extends Layout{

    int rows = 4;
    int columns = 0; // 0 means auto

    public LayoutGrid(){
        super();
    }
    public LayoutGrid(int padding, int spacing, boolean stretchX, boolean stretchY, int rows, int columns) {
        super(padding, spacing, stretchX, stretchY);
        this.rows = rows;
        this.columns = columns;
    }

    @Override
    public void apply(GuiElement element) {
        ArrayList<GuiElement> childs = element.getChilds();
        if(childs.isEmpty())
            return;

        if(rows < 0)
            rows = 0;
        if(columns < 0)
            columns = 0;

        int y = padding;
        int x = padding;
        int childCount = childs.size();

        if(columns == 0 && rows == 0) {
            columns = (int) Math.ceil(Math.sqrt(childCount));
            rows = (int) Math.ceil((double) childCount / columns);
        }else if(columns == 0) {
            columns = (int) Math.ceil((double) childCount / rows);
        }else if(rows == 0) {
            rows = (int) Math.ceil((double) childCount / columns);
        }

        int width = (element.getWidth()-padding*2+spacing)/columns - spacing;
        int height = (element.getHeight()-padding*2+spacing)/rows - spacing;

        for (int i = 0; i < childCount; i++) {
            GuiElement child = childs.get(i);
            child.setX(x);
            child.setY(y);
            if(stretchX) {
                child.setWidth(width);
            }
            if(stretchY) {
                child.setHeight(height);
            }
            x += child.getWidth() + spacing;
            if(x + width > element.getWidth() - padding) {
                x = padding;
                y += height + spacing;
            }
        }
    }
}
