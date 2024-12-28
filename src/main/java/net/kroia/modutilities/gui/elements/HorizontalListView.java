package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.ListView;

public class HorizontalListView extends ListView {

    public HorizontalListView() {
        super();
        scrollContainer.setBounds(0, 0, 0, 0);
    }
    public HorizontalListView(int x, int y, int width, int height) {
        super(x, y, width, height);
        scrollContainer.setBounds(0, 0, width, height- scrollbarThickness);
    }


    @Override
    protected void renderBackground() {
        if(enableBackground)
            renderBackgroundColor();
        drawRect(0, getHeight()- scrollbarThickness, getWidth(), scrollbarThickness, scrollbarBackgroundColor);
        if(enableOutline)
            renderOutline();
    }

    @Override
    protected int getContentDimension2()
    {
        return getWidth();
    }

    @Override
    public void addChild(GuiElement el)
    {
        allObjectSize += el.getWidth();
        scrollOffset = Math.max(Math.min(scrollOffset, allObjectSize - getContentDimension2()+1), 0);
        super.addChild(el);

    }
    @Override
    public void removeChild(GuiElement el)
    {
        allObjectSize -= el.getWidth();
        scrollOffset = Math.max(Math.min(scrollOffset, allObjectSize - getContentDimension2()+1), 0);
        super.removeChild(el);
    }
    @Override
    protected void updateElementPositions()
    {
        int x = padding;
        for(GuiElement child : getChilds())
        {
            child.setX(x - scrollOffset);
            x += child.getWidth()+spacing;
        }
    }

    @Override
    protected void layoutChanged() {
        scrollContainer.setBounds(0, 0, getWidth(), getHeight()- scrollbarThickness);
        childsChanged();
    }
    protected void childsChanged()
    {
        allObjectSize = padding*2;
        for(GuiElement child : getChilds())
        {
            allObjectSize += child.getWidth()+spacing;
        }
    }

    @Override
    protected void setScrollBarBounds()
    {
        // Render scrollbar
        int scrollbarWidth = getWidth()-outlineThickness*2;
        int scrollbarX = outlineThickness;
        if(allObjectSize > 0)
        {
            scrollbarWidth = Math.min((int) ((float)(getWidth()) / (float) allObjectSize * (float)getWidth()), getWidth())-outlineThickness*2;
            scrollbarX = (int) ((float)scrollOffset / (float) allObjectSize * (float)getWidth())+outlineThickness;
        }
        scrollbarButton.setBounds(scrollbarX, getHeight() - scrollbarThickness-1, scrollbarWidth, scrollbarThickness+1);
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

        scrollOffset = Math.max(Math.min(scrollOffset + delta, allObjectSize - getContentDimension2()+1), 0);
        updateElementPositions();
        setScrollBarBounds();
    }
}
