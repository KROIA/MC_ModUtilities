package net.kroia.modutilities.gui.layout;

import net.kroia.modutilities.gui.elements.base.GuiElement;

import java.util.ArrayList;
import java.util.List;

public class LayoutGrid extends Layout{

    public int rows = 4;
    public int columns = 0; // 0 means auto

    public GuiElement.Alignment alignment = GuiElement.Alignment.TOP;

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
        List<GuiElement> childs = element.getChilds();
        if(childs.isEmpty())
            return;

        int rowsInternal = rows;
        int columnsInternal = columns;

        if(rowsInternal < 0)
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
                rowsInternal = (int)(widthSum/averageWidth);
            }
        }
        if(columnsInternal < 0){
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
                columnsInternal = (int)(heightSum/averageHeight);
            }
        }

        int childCount = childs.size();

        if(columnsInternal == 0 && rowsInternal == 0) {
            columnsInternal = (int) Math.ceil(Math.sqrt(childCount));
            rowsInternal = (int) Math.ceil((double) childCount / columnsInternal) + (childCount % columnsInternal == 0 ? 0 : 1);
        }else if(columnsInternal == 0) {
            columnsInternal = (int) Math.ceil((double) childCount / rowsInternal) + (childCount % rowsInternal == 0 ? 0 : 1);
        }else if(rowsInternal == 0) {
            rowsInternal = (int) Math.ceil((double) childCount / columnsInternal) + (childCount % columnsInternal == 0 ? 0 : 1);
        }

        if(rowsInternal * columnsInternal < childCount)
        {
            rowsInternal = (int) Math.ceil((double) childCount / columnsInternal) + (childCount % columnsInternal == 0 ? 0 : 1);
        }

        int elementWidth = element.getWidth()-padding*2;
        int elementHeight = element.getHeight()-padding*2;

        int width = (elementWidth+spacing)/columnsInternal - spacing;
        int height = (elementHeight+spacing)/rowsInternal - spacing;

        int i=0;
        int xPos = padding;
        int yPos = padding;
        for(int y=0; y<rowsInternal; y++){
            int maxHeight = 0;
            for(int x=0; x<columnsInternal; x++) {
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

                xPos += child.getWidth() + spacing;
                i++;
            }
            xPos = padding;
            yPos += maxHeight + spacing;
        }
    }


}
