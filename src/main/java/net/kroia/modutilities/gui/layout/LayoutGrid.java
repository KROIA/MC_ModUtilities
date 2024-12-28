package net.kroia.modutilities.gui.layout;

import net.kroia.modutilities.gui.elements.base.GuiElement;

import java.util.ArrayList;

public class LayoutGrid extends Layout{

    public int rows = 4;
    public int columns = 0; // 0 means auto

    public GuiElement.Alignment alignment = GuiElement.Alignment.CENTER;

    public LayoutGrid(){
        super();
    }
    public LayoutGrid(int padding, int spacing, boolean stretchX, boolean stretchY, int rows, int columns, GuiElement.Alignment alignment) {
        super(padding, spacing, stretchX, stretchY);
        this.rows = rows;
        this.columns = columns;
        this.alignment = alignment;
    }

    @Override
    public void apply(GuiElement element) {
        ArrayList<GuiElement> childs = element.getChilds();
        if(childs.isEmpty())
            return;

        if(rows < 0)
        {
            int widthSum = 0;
            float averageWidth = 0;
            for(GuiElement child : childs)
            {
                widthSum += child.getWidth();
                averageWidth += child.getWidth();
            }
            averageWidth /= childs.size();
            if(widthSum > 0)
            {
                rows = (int)(widthSum/averageWidth);
            }
        }
        if(columns < 0){
            int heightSum = 0;
            float averageHeight = 0;
            for(GuiElement child : childs)
            {
                heightSum += child.getHeight();
                averageHeight += child.getHeight();
            }
            averageHeight /= childs.size();
            if(heightSum > 0)
            {
                columns = (int)(heightSum/averageHeight);
            }
        }

        int childCount = childs.size();

        if(columns == 0 && rows == 0) {
            columns = (int) Math.ceil(Math.sqrt(childCount));
            rows = (int) Math.ceil((double) childCount / columns);
        }else if(columns == 0) {
            columns = (int) Math.ceil((double) childCount / rows);
        }else if(rows == 0) {
            rows = (int) Math.ceil((double) childCount / columns);
        }

        int elementWidth = element.getWidth()-padding*2;
        int elementHeight = element.getHeight()-padding*2;

        int width = (elementWidth-spacing)/columns + spacing;
        int height = (elementHeight-spacing)/rows + spacing;

        int i=0;
        int xPos = padding;
        int yPos = padding;
        for(int y=0; y<rows; y++){
            int maxHeight = 0;
            for(int x=0; x<columns; x++) {
                if(i >= childCount)
                    break;
                GuiElement child = childs.get(i);

                //child.setPosition(xPos, yPos);
                if(stretchX) {
                    child.setWidth(width);
                }
                if(stretchY) {
                    child.setHeight(height);
                }
                child.applyAlignment(alignment, xPos, yPos, width, height);
                maxHeight = Math.max(maxHeight, child.getHeight());

                xPos += width;
                i++;
            }
            xPos = padding;
            yPos += maxHeight;
        }
    }


}
