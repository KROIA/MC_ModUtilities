package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.ListView;

public class HorizontalListView extends ListView {

    public HorizontalListView() {
        super();
        scrollContainer.setBounds(1, 1, 0, 0);
    }
    public HorizontalListView(int x, int y, int width, int height) {
        super(x, y, width, height);
        scrollContainer.setBounds(1,1, width, height- scrollbarThickness-1);
    }


    @Override
    protected void renderBackground() {
        if(enableBackground)
            renderBackgroundColor();
        drawRect(0, scrollbarButton.getTop(), getWidth(), scrollbarButton.getHeight(), scrollbarBackgroundColor);
        if(enableOutline)
            renderOutline();
    }

    @Override
    protected int getContentDimension2()
    {
        return getWidth()-2*outlineThickness;
    }

    @Override
    protected void layoutChanged() {
        scrollContainer.setSize(getWidth()-2, getHeight()- scrollbarThickness-1);
        childsChanged();
        setScrollBarBounds();
        updateElementPositions();
    }

    protected void childsChanged()
    {
        int minPos = 0;
        int maxPos = 0;
        for(GuiElement child : getChilds())
        {
            minPos = Math.min(minPos, child.getLeft());
            maxPos = Math.max(maxPos, child.getRight());
        }
        allObjectSize = maxPos-minPos;
        scrollOffset = Math.max(Math.min(scrollOffset, allObjectSize - getContentDimension2()), 0);
    }

    @Override
    protected void setScrollBarBounds()
    {
        // Render scrollbar
        int scrollbarWidth = getContentDimension2();
        int scrollbarX = outlineThickness;
        if(allObjectSize > 0)
        {
            int width = getContentDimension2();
            scrollbarWidth = Math.min(Math.round(map(getContentDimension2(), 0, allObjectSize, 0, width)),width);
            scrollbarX = Math.min(Math.round(map(scrollOffset, 0, allObjectSize, 0, width) + outlineThickness), width-scrollbarWidth+outlineThickness);
        }
        scrollbarButton.setBounds(scrollbarX, getHeight() - scrollbarThickness, scrollbarWidth, scrollbarThickness);
    }

    @Override
    public void addChild(GuiElement el)
    {
        allObjectSize += el.getWidth();
        scrollOffset = Math.max(Math.min(scrollOffset, allObjectSize - getContentDimension2()), 0);
        super.addChild(el);
    }
    @Override
    public void removeChild(GuiElement el)
    {
        allObjectSize -= el.getWidth();
        scrollOffset = Math.max(Math.min(scrollOffset, allObjectSize - getContentDimension2()), 0);
        super.removeChild(el);
    }
    @Override
    protected void updateElementPositions()
    {
        scrollContainer.setX(-scrollOffset+1);
        scrollContainer.setWidth(allObjectSize);
    }






    @Override
    protected void onScrllBarFallingEdge()
    {
        scrollbarDragStartMouse = getMouseX();
    }

    @Override
    protected void onScrollBarDragging()
    {
        int delta = (getMouseX() - scrollbarDragStartMouse)*allObjectSize/getWidth();
        scrollbarDragStartMouse = getMouseX();

        scrollOffset = Math.max(Math.min(scrollOffset + delta, allObjectSize - getContentDimension2()), 0);
        updateElementPositions();
        setScrollBarBounds();
    }
}
