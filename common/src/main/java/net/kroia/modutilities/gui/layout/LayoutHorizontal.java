package net.kroia.modutilities.gui.layout;

import net.kroia.modutilities.gui.elements.base.GuiElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout that arranges children left-to-right in a single row.
 * When {@link #stretchX} is enabled each child is given an equal share of the available width;
 * when {@link #stretchY} is enabled each child fills the parent's height (minus padding).
 */
public class LayoutHorizontal extends Layout {

    /** Creates a horizontal layout with default padding/spacing and no stretching. */
    public LayoutHorizontal(){
        super();
    }

    /**
     * Creates a horizontal layout with the given parameters.
     * @param padding padding in pixels around the children
     * @param spacing spacing in pixels between adjacent children
     * @param stretchX whether to give every child the same width
     * @param stretchY whether children should fill the parent's height
     */
    public LayoutHorizontal(int padding, int spacing, boolean stretchX, boolean stretchY) {
        super(padding, spacing, stretchX, stretchY);
    }

    /**
     * Arranges the children of {@code element} into a horizontal row.
     * @param element the parent whose children will be laid out
     */
    @Override
    public void apply(GuiElement element) {
        List<GuiElement> childs = element.getChilds();
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
