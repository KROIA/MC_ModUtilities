package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.ListView;

public class VerticalListView extends ListView {
    public VerticalListView() {
        super();
        scrollContainer.setBounds(1, 1, 0, 0);
    }
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
    protected int getContentDimension2()
    {
        return getHeight()-2*outlineThickness;
    }
    @Override
    protected void layoutChanged() {
        scrollContainer.setSize(getWidth() - scrollbarThickness-1, getHeight()-2);
        childsChanged();
        setScrollBarBounds();
        updateElementPositions();
    }
    protected void childsChanged()
    {
        int minPos = 0;
        int maxPos = 0;
        int spacing = getLayout()!=null?getLayout().spacing:0;
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
        scrollOffset = Math.max(Math.min(scrollOffset, allObjectSize - getContentDimension2()), 0);
        super.addChild(el);
    }
    @Override
    public void removeChild(GuiElement el)
    {
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
}
