package net.kroia.modutilities.gui.layout;

import net.kroia.modutilities.gui.elements.base.GuiElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout that arranges children in a 2D grid.
 * <p>
 * The grid dimensions are controlled by {@link #rows} and {@link #columns}:
 * <ul>
 *   <li>If both are 0 a near-square grid is computed automatically from the child count.</li>
 *   <li>If exactly one of them is 0 the other is computed to fit all children.</li>
 *   <li>Negative values trigger a size-based estimation from average child sizes.</li>
 * </ul>
 * Each cell is sized equally; children are placed within their cell using {@link #alignment}.
 */
public class LayoutGrid extends Layout{

    /** Desired number of rows. {@code 0} = auto, negative = derive from sizes. */
    public int rows = 4;
    /** Desired number of columns. {@code 0} = auto, negative = derive from sizes. */
    public int columns = 0; // 0 means auto

    /** Alignment used to position each child inside its grid cell. */
    public GuiElement.Alignment alignment = GuiElement.Alignment.TOP;

    /** Creates a grid layout with default padding/spacing and four rows. */
    public LayoutGrid(){
        super();
    }

    /**
     * Creates a grid layout with the given parameters.
     * @param padding padding in pixels around the grid
     * @param spacing spacing in pixels between cells
     * @param stretchX whether children should be stretched to fill the cell horizontally
     * @param stretchY whether children should be stretched to fill the cell vertically
     * @param rows desired number of rows (0 = auto, negative = derive from child sizes)
     * @param columns desired number of columns (0 = auto, negative = derive from child sizes)
     * @param alignment how to position the child within its cell
     */
    public LayoutGrid(int padding, int spacing, boolean stretchX, boolean stretchY, int rows, int columns, GuiElement.Alignment alignment) {
        super(padding, spacing, stretchX, stretchY);
        this.rows = rows;
        this.columns = columns;
        this.alignment = alignment;
    }

    /**
     * Arranges the children of {@code element} into a grid.
     * @param element the parent whose children will be laid out
     */
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
            rowsInternal = (int) Math.ceil((double) childCount / columnsInternal);
        }else if(columnsInternal == 0) {
            columnsInternal = (int) Math.ceil((double) childCount / rowsInternal);
        }else if(rowsInternal == 0) {
            rowsInternal = (int) Math.ceil((double) childCount / columnsInternal);
        }

        if(rowsInternal * columnsInternal < childCount)
        {
            rowsInternal = (int) Math.ceil((double) childCount / columnsInternal);
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
