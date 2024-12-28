package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.ListView;

public class VerticalListView extends ListView {
    public VerticalListView(int x, int y, int width, int height) {
        super(x, y, width, height);
        scrollContainer.setBounds(0, 0, width - scrollbarThickness, height);
    }

    @Override
    protected void renderBackground() {
        if(enableBackground)
            renderBackgroundColor();
        drawRect(getWidth()- scrollbarThickness, 0, scrollbarThickness, getHeight(), scrollbarBackgroundColor);
        if(enableOutline)
            renderOutline();
    }
    @Override
    protected int getContentDimension2()
    {
        return getHeight();
    }
    @Override
    protected void layoutChanged() {
        scrollContainer.setBounds(0, 0, getWidth() - scrollbarThickness, getHeight());
        childsChanged();
    }
    protected void childsChanged()
    {
        allObjectSize = padding*2;
        for(GuiElement child : getChilds())
        {
            allObjectSize += child.getHeight()+spacing;
        }
    }
    @Override
    protected void setScrollBarBounds()
    {

        // Render scrollbar
        int scrollbarHeight = getHeight()-outlineThickness*2;
        int scrollbarY = outlineThickness;
        if(allObjectSize > 0)
        {
            scrollbarHeight = Math.min((int) ((float)(getHeight()) / (float) allObjectSize * (float)getHeight()), getHeight())-outlineThickness*2;
            scrollbarY = (int) ((float)scrollOffset / (float) allObjectSize * (float)getHeight())+outlineThickness;
        }

        scrollbarButton.setBounds(getWidth() - scrollbarThickness-1, scrollbarY, scrollbarThickness+1, scrollbarHeight);
    }

    @Override
    public void addChild(GuiElement el)
    {
        allObjectSize += el.getHeight();
        scrollOffset = Math.max(Math.min(scrollOffset, allObjectSize - getContentDimension2()+1), 0);
        super.addChild(el);
    }
    @Override
    public void removeChild(GuiElement el)
    {
        allObjectSize -= el.getHeight();
        scrollOffset = Math.max(Math.min(scrollOffset, allObjectSize - getContentDimension2()+1), 0);
        super.removeChild(el);
    }
    @Override
    protected void updateElementPositions()
    {
        int y = padding;
        for(GuiElement child : getChilds())
        {
            child.setY(y - scrollOffset);
            y += child.getHeight()+spacing;
        }
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

        scrollOffset = Math.max(Math.min(scrollOffset + delta, allObjectSize - getContentDimension2()+1), 0);
        updateElementPositions();
        setScrollBarBounds();
    }
}
