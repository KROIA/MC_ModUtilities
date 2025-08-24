package net.kroia.modutilities.gui.elements.base;

import net.kroia.modutilities.gui.Graphics;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.kroia.modutilities.gui.layout.Layout;

import java.util.ArrayList;
import java.util.List;

public abstract class ListView extends GuiElement{
    protected static class ScrollContainer extends GuiElement
    {
        private final ListView parentListView;
        public ScrollContainer(ListView parentListView) {
            super();
            this.parentListView = parentListView;
            this.setEnableBackground(false);
            this.setEnableOutline(false);
        }

        @Override
        protected void render() {

        }

        @Override
        public void renderBackgroundInternal() {
            if(!isVisible())
                return;
            Rectangle bounds = parentListView.getScissorRect();
            enableScissor(bounds);
            super.renderBackgroundInternal();
            disableScissor();
        }

        @Override
        public void renderInternal() {
            if(!isVisible())
                return;
            Rectangle bounds = parentListView.getScissorRect();
            enableScissor(bounds);
            super.renderInternal();
            disableScissor();
        }

        @Override
        public void renderGizmosInternal() {
            if(!isVisible())
                return;
            Rectangle bounds = parentListView.getScissorRect();
            enableScissor(bounds);
            super.renderGizmosInternal();
            disableScissor();
        }

        @Override
        protected void layoutChanged() {
            parentListView.childsChanged();
        }
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

    protected abstract Rectangle getScissorRect();

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

    public void setScrollbarThickness(int scrollbarThickness)
    {
        this.scrollbarThickness = scrollbarThickness;
        layoutChangedInternal();
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
    public List<GuiElement> getChilds()
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

   /* @Override
    public void renderBackgroundInternal()
    {
        if(!isVisible())
            return;






        //Rectangle bounds = getBounds();
        //enableScissor(1, 1, bounds.width-2, bounds.height-2);
        super.renderBackgroundInternal();
        //disableScissor();
    }
    @Override
    public void renderInternal()
    {
        if(!isVisible())
            return;
        super.renderInternal();
        //disableScissor();
    }
    @Override
    public void renderGizmosInternal()
    {
        if(!isVisible())
            return;
       // Rectangle bounds = getBounds();
       // enableScissor(1, 1, bounds.width-2, bounds.height-2);
        super.renderGizmosInternal();
        //disableScissor();
    }*/



    protected abstract void updateElementPositions();
    protected abstract void onScrllBarFallingEdge();
    protected abstract void onScrollBarDragging();



}
