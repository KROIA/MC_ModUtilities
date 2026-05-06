package net.kroia.modutilities.gui.layout;

import net.kroia.modutilities.gui.elements.base.GuiElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout that arranges children top-to-bottom in a single column.
 * When {@link #stretchY} is enabled each child gets an equal share of the available height;
 * when {@link #stretchX} is enabled each child fills the parent's width (minus padding).
 */
public class LayoutVertical extends Layout {
    /** Creates a vertical layout with default padding/spacing and no stretching. */
    public LayoutVertical(){
        super();
    }

    /**
     * Creates a vertical layout with the given parameters.
     * @param padding padding in pixels around the children
     * @param spacing spacing in pixels between adjacent children
     * @param stretchX whether children should fill the parent's width
     * @param stretchY whether to give every child the same height
     */
    public LayoutVertical(int padding, int spacing, boolean stretchX, boolean stretchY) {
        super(padding, spacing, stretchX, stretchY);
    }

    /**
     * Arranges the children of {@code element} into a vertical column.
     * @param element the parent whose children will be laid out
     */
    @Override
    public void apply(GuiElement element) {
        List<GuiElement> childs = element.getChilds();
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
