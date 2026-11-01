package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.ListView;
import net.kroia.modutilities.gui.geometry.Rectangle;
import org.spongepowered.asm.mixin.SoftOverride;

/**
 * Vertically scrolling concrete subclass of {@link ListView}.
 * <p>
 * Children are laid out top-to-bottom and the scrollbar is rendered along the
 * right edge. Height is treated as the scrollable axis: {@link #getSizeHintHeight()}
 * grows with content while {@link #getSizeHintWidth()} stays at the configured
 * element width.
 */
public class VerticalListView extends ListView {

    private final Rectangle scissorRect = new Rectangle(0, 0, 0, 0);

    /**
     * Creates an empty vertical list view at the origin with default size.
     */
    public VerticalListView() {
        super();
        scrollContainer.setBounds(1, 1, 0, 0);
    }

    /**
     * Creates an empty vertical list view at the given position and size.
     *
     * @param x      the x-coordinate relative to the parent
     * @param y      the y-coordinate relative to the parent
     * @param width  the width in pixels (includes scrollbar area along the right)
     * @param height the height in pixels
     */
    public VerticalListView(int x, int y, int width, int height) {
        super(x, y, width, height);
        scrollContainer.setBounds(1,1,width - scrollbarThickness-1, height-2);
    }

    @Override
    protected void renderBackground() {
        if(enableBackground)
            renderBackgroundColor();
        drawRect(scrollbarButton.getLeft(), 0, scrollbarButton.getWidth(), getHeight(), scrollbarBackgroundColor);
        if(enableOutline)
            renderOutline();
    }


    @Override
    public int getSizeHintWidth()
    {
        return getWidth();
    }
    @Override
    public int getSizeHintHeight()
    {
        return allObjectSize + 2*outlineThickness;
    }
    @Override
    protected int getContentDimension2()
    {
        return getHeight()-2*outlineThickness;
    }
    @Override
    protected void layoutChanged() {
        scrollContainer.setBounds(1,1,getWidth() - scrollbarThickness-1, getHeight()-2);
        childsChanged();
        setScrollBarBounds();
        updateElementPositions();
    }
    protected void childsChanged()
    {
        int minPos = 0;
        int maxPos = 0;
        var layout = getLayout();
        int spacing = layout != null ? layout.spacing : 0;
        for(GuiElement child : getChilds())
        {
            minPos = Math.min(minPos, child.getTop());
            maxPos = Math.max(maxPos, child.getBottom());
        }
        allObjectSize = maxPos-minPos;
        scrollOffset = Math.max(Math.min(scrollOffset, allObjectSize - getContentDimension2()), 0);
    }
    @Override
    protected void setScrollBarBounds()
    {
        // Render scrollbar
        int scrollbarHeight = getContentDimension2();
        int scrollbarY = outlineThickness;
        if(allObjectSize > 0)
        {
            int height = getContentDimension2();
            scrollbarHeight = Math.min(Math.round(map(getContentDimension2(), 0, allObjectSize, 0, height)),height);
            scrollbarY = Math.min(Math.round(map(scrollOffset, 0, allObjectSize, 0, height) + outlineThickness), height-scrollbarHeight+outlineThickness);
        }

        scrollbarButton.setBounds(getWidth() - scrollbarThickness, scrollbarY, scrollbarThickness, scrollbarHeight);
    }

    @Override
    public void addChild(GuiElement el)
    {
        allObjectSize += el.getHeight();
        scrollOffset = Math.max(Math.min(scrollOffset, allObjectSize - getContentDimension2()), 0);
        super.addChild(el);
    }
    @Override
    public void removeChild(GuiElement el)
    {
        allObjectSize -= el.getHeight();
        scrollOffset = Math.max(Math.min(scrollOffset, allObjectSize - getContentDimension2()), 0);
        super.removeChild(el);
    }
    @Override
    protected void updateElementPositions()
    {
        scrollContainer.setY(-scrollOffset+1);
        scrollContainer.setHeight(allObjectSize);
    }

    @Override
    protected void onScrllBarFallingEdge()
    {
        scrollbarDragStartMouse = getMouseY();
    }

    @Override
    protected void onScrollBarDragging()
    {
        int delta = (getMouseY() - scrollbarDragStartMouse)*allObjectSize/getHeight();
        scrollbarDragStartMouse = getMouseY();

        scrollOffset = Math.max(Math.min(scrollOffset + delta, allObjectSize - getContentDimension2()), 0);
        updateElementPositions();
        setScrollBarBounds();
    }

    @Override
    protected Rectangle getScissorRect()
    {
        scissorRect.x = 0;
        scissorRect.y = scrollOffset;
        scissorRect.width = scrollContainer.getWidth();
        scissorRect.height = getHeight()-2;
        return scissorRect;
    }
}
