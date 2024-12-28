package net.kroia.modutilities.gui.elements.base;

import net.kroia.modutilities.gui.elements.Button;

import java.util.ArrayList;

public abstract class ListView extends GuiElement{
    protected class ScrollContainer extends GuiElement
    {
        public ScrollContainer() {
            super();
        }
        public ScrollContainer(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        protected void renderBackground() {

        }

        @Override
        protected void render() {

        }

        @Override
        protected void layoutChanged() {

        }
        @Override
        public void renderBackgroundInternal()
        {
            if(!isVisible())
                return;
            enableScissor();
            renderBackground();

            for (GuiElement child : getChilds()) {
                child.renderBackgroundInternal();
            }
            disableScissor();
        }
        @Override
        public void renderInternal()
        {
            if(!isVisible())
                return;

            enableScissor();
            render();

            for (GuiElement child : getChilds()) {
                child.renderInternal();
            }
            disableScissor();

        }
        @Override
        public void renderGizmosInternal()
        {
            if(!isVisible())
                return;
            enableScissor();
            renderGizmos();
            for (GuiElement child : getChilds()) {
                child.renderGizmosInternal();
            }
            disableScissor();
        }
    }

    protected int scrollOffset = 0;
    protected int allObjectSize = 0;

    protected int scrolSpeed = 5;
    protected int scrollbarThickness = 5;
    protected final Button scrollbarButton;
    protected final ScrollContainer scrollContainer;
    protected int spacing = 0;
    protected int padding = 0;

    protected int scrollbarDragStartMouse = 0;
    protected int scrollbarBackgroundColor = 0xff444444;

    public ListView() {
        super();
        scrollbarButton = new Button("");

        scrollbarButton.setOnDown(this::onScrollBarDragging);
        scrollbarButton.setOnFallingEdge(this::onScrllBarFallingEdge);
        scrollContainer = new ScrollContainer();
        super.addChild(scrollbarButton);
        super.addChild(scrollContainer);
    }
    public ListView(int x, int y, int width, int height) {
        super(x, y, width, height);
        scrollbarButton = new Button("");

        scrollbarButton.setOnDown(this::onScrollBarDragging);
        scrollbarButton.setOnFallingEdge(this::onScrllBarFallingEdge);
        scrollContainer = new ScrollContainer();
        super.addChild(scrollbarButton);
        super.addChild(scrollContainer);
    }

    public void setScrolSpeed(int scrolSpeed)
    {
        this.scrolSpeed = scrolSpeed;
    }
    public int getScolSpeed()
    {
        return this.scrolSpeed;
    }
    public void setScrollbarBackgroundColor(int color)
    {
        this.scrollbarBackgroundColor = color;
    }
    public int getScrollbarBackgroundColor()
    {
        return this.scrollbarBackgroundColor;
    }

    public int getScrollbarThickness()
    {
        return scrollbarThickness;
    }

    protected abstract int getContentDimension2();
    protected abstract void setScrollBarBounds();

    @Override
    protected void render() {
        //if(allObjectSize != 0)

    }

    @Override
    public void addChild(GuiElement el)
    {
        scrollContainer.addChild(el);
        updateElementPositions();
        setScrollBarBounds();
    }
    @Override
    public void removeChild(GuiElement el)
    {
        scrollContainer.removeChild(el);
        updateElementPositions();
        setScrollBarBounds();
    }

    @Override
    public void removeChilds()
    {
        allObjectSize = 0;
        scrollContainer.removeChilds();
        updateElementPositions();
        setScrollBarBounds();
    }
    @Override
    public ArrayList<GuiElement> getChilds()
    {
        return scrollContainer.getChilds();
    }

    protected abstract void childsChanged();

    @Override
    public boolean mouseScrolledOverElement(double delta)
    {
        if (delta > 0 && scrollOffset > 0) {
            scrollOffset-=scrolSpeed; // Scroll up
            if(scrollOffset < 0)
                scrollOffset = 0;
            updateElementPositions();
            setScrollBarBounds();
        } else if (delta < 0 && scrollOffset < allObjectSize - getContentDimension2()) {
            scrollOffset+=scrolSpeed; // Scroll down
            if(scrollOffset > allObjectSize - getContentDimension2()+1)
                scrollOffset = allObjectSize - getContentDimension2()+1;
            updateElementPositions();
            setScrollBarBounds();
        }
        return true;
    }

    @Override
    public void relayout(int padding, int spacing, LayoutDirection direction, boolean stretchX, boolean stretchY)
    {
        this.padding = padding;
        this.spacing = spacing;
        scrollContainer.relayout(padding, spacing, direction, stretchX, stretchY);
        setScrollBarBounds();
    }


    protected abstract void updateElementPositions();
    protected abstract void onScrllBarFallingEdge();
    protected abstract void onScrollBarDragging();



}
