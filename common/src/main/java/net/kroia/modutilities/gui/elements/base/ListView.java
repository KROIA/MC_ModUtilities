package net.kroia.modutilities.gui.elements.base;

import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.layout.Layout;

import java.util.ArrayList;

public abstract class ListView extends GuiElement{
    protected class ScrollContainer extends GuiElement
    {
        private final ListView parentListView;
        public ScrollContainer(ListView parentListView) {
            super();
            this.parentListView = parentListView;
        }
        public ScrollContainer(int x, int y, int width, int height, ListView parentListView) {
            super(x, y, width, height);
            this.parentListView = parentListView;
        }

        @Override
        protected void renderBackground() {

        }

        @Override
        protected void render() {

        }

        @Override
        protected void layoutChanged() {
            parentListView.childsChanged();
        }

        /*@Override
        public void layoutChangedInternal() {
            super.layoutChangedInternal();
            super.getBounds().height = getHeight();
        }*/



    }

    protected int scrollOffset = 0;
    protected int allObjectSize = 0;

    protected int scrolSpeed = 5;
    protected int scrollbarThickness = 5;
    protected final Button scrollbarButton;
    protected final ScrollContainer scrollContainer;
    protected int scrollbarDragStartMouse = 0;
    protected int scrollbarBackgroundColor = 0xff444444;

    public ListView() {
        super();
        scrollbarButton = new Button("");

        scrollbarButton.setOnDown(this::onScrollBarDragging);
        scrollbarButton.setOnFallingEdge(this::onScrllBarFallingEdge);
        scrollContainer = new ScrollContainer(this);
        super.addChild(scrollbarButton);
        super.addChild(scrollContainer);
    }
    public ListView(int x, int y, int width, int height) {
        super(x, y, width, height);
        scrollbarButton = new Button("");

        scrollbarButton.setOnDown(this::onScrollBarDragging);
        scrollbarButton.setOnFallingEdge(this::onScrllBarFallingEdge);
        scrollContainer = new ScrollContainer(this);
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

    public int getContainerWidth()
    {
        return scrollContainer.getWidth();
    }
    public int getContainerHeight()
    {
        return scrollContainer.getHeight();
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
            if(scrollOffset > allObjectSize - getContentDimension2())
                scrollOffset = allObjectSize - getContentDimension2();
            updateElementPositions();
            setScrollBarBounds();
        }
        return true;
    }


    @Override
    public void setLayout(Layout layout)
    {
        scrollContainer.setLayout(layout);
    }
    @Override
    public Layout getLayout()
    {
        return scrollContainer.getLayout();
    }

    @Override
    public void renderBackgroundInternal()
    {
        if(!isVisible())
            return;
        enableScissor();
        super.renderBackgroundInternal();
        disableScissor();
    }
    @Override
    public void renderInternal()
    {
        if(!isVisible())
            return;
        enableScissor();
        super.renderInternal();
        disableScissor();
    }
    @Override
    public void renderGizmosInternal()
    {
        if(!isVisible())
            return;
        enableScissor();
        super.renderGizmosInternal();
        disableScissor();
    }



    protected abstract void updateElementPositions();
    protected abstract void onScrllBarFallingEdge();
    protected abstract void onScrollBarDragging();



}
