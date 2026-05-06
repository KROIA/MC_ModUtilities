package net.kroia.modutilities.gui.elements.base;

import net.kroia.modutilities.gui.elements.EmptyButton;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.kroia.modutilities.gui.layout.Layout;

import java.util.List;

/**
 * Abstract base for a scrollable list-style container.
 * <p>
 * A {@code ListView} hosts an inner scroll container that clips and offsets its
 * children, and a built-in scrollbar handle. Child elements added via
 * {@link #addChild(GuiElement)} are placed inside the inner
 * {@link ScrollContainer} rather than directly on the {@code ListView} itself.
 * <p>
 * Concrete subclasses (such as {@code VerticalListView} or
 * {@code HorizontalListView}) decide the scroll direction by implementing
 * {@link #getSizeHintWidth()}, {@link #getSizeHintHeight()},
 * {@link #updateElementPositions()}, {@link #setScrollBarBounds()}, and the
 * scissor/scrollbar interaction hooks.
 *
 * @apiNote The {@code gui/} package is client-only
 *          ({@code @Environment(EnvType.CLIENT)}); list views must only be used
 *          on the client.
 */
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


    private static final int DEFAULT_SCROLL_SPEED = 10;
    private static final int DEFAULT_SCROLLBAR_THICKNESS = 5;

    protected int scrollOffset = 0;
    protected int allObjectSize = 0;

    protected int scrolSpeed = DEFAULT_SCROLL_SPEED;
    protected int scrollbarThickness = DEFAULT_SCROLLBAR_THICKNESS;
    protected final EmptyButton scrollbarButton;
    protected final ScrollContainer scrollContainer;
    protected int scrollbarDragStartMouse = 0;
    protected int scrollbarBackgroundColor = 0xff444444;

    /**
     * Creates a new list view with zero size at the origin and an empty scroll
     * container.
     */
    public ListView() {
        super();
        scrollbarButton = new EmptyButton();

        scrollbarButton.setOnDown(this::onScrollBarDragging);
        scrollbarButton.setOnFallingEdge(this::onScrllBarFallingEdge);
        scrollContainer = new ScrollContainer(this);
        super.addChild(scrollbarButton);
        super.addChild(scrollContainer);
    }

    /**
     * Creates a new list view at the given position and size.
     *
     * @param x      the x position
     * @param y      the y position
     * @param width  the width
     * @param height the height
     */
    public ListView(int x, int y, int width, int height) {
        super(x, y, width, height);
        scrollbarButton = new EmptyButton();

        scrollbarButton.setOnDown(this::onScrollBarDragging);
        scrollbarButton.setOnFallingEdge(this::onScrllBarFallingEdge);
        scrollContainer = new ScrollContainer(this);
        super.addChild(scrollbarButton);
        super.addChild(scrollContainer);
    }

    protected abstract Rectangle getScissorRect();

    /**
     * Sets the number of GUI pixels the content scrolls per scroll-wheel notch.
     *
     * @param scrolSpeed the scroll speed in pixels per notch
     */
    public void setScrolSpeed(int scrolSpeed)
    {
        this.scrolSpeed = scrolSpeed;
    }

    /**
     * @return the scroll speed in GUI pixels per scroll-wheel notch
     */
    public int getScolSpeed()
    {
        return this.scrolSpeed;
    }

    /**
     * Sets the background color drawn behind the scrollbar.
     *
     * @param color the packed ARGB color
     */
    public void setScrollbarBackgroundColor(int color)
    {
        this.scrollbarBackgroundColor = color;
    }

    /**
     * @return the packed ARGB color drawn behind the scrollbar
     */
    public int getScrollbarBackgroundColor()
    {
        return this.scrollbarBackgroundColor;
    }

    /**
     * Sets the thickness of the scrollbar (width for vertical lists, height for
     * horizontal lists). Triggers a re-layout.
     *
     * @param scrollbarThickness the scrollbar thickness in GUI pixels
     */
    public void setScrollbarThickness(int scrollbarThickness)
    {
        this.scrollbarThickness = scrollbarThickness;
        layoutChangedInternal();
    }

    /**
     * @return the current scrollbar thickness in GUI pixels
     */
    public int getScrollbarThickness()
    {
        return scrollbarThickness;
    }

    /**
     * @return the width of the inner scroll container that hosts the children
     */
    public int getContainerWidth()
    {
        return scrollContainer.getWidth();
    }

    /**
     * @return the height of the inner scroll container that hosts the children
     */
    public int getContainerHeight()
    {
        return scrollContainer.getHeight();
    }


    /**
     * @return The width of the whole list view if it was fully expanded to fit all elements.
     */
    public abstract int getSizeHintWidth();

    /**
     * @return The height of the whole list view if it was fully expanded to fit all elements.
     */
    public abstract int getSizeHintHeight();

    protected abstract int getContentDimension2();
    protected abstract void setScrollBarBounds();

    @Override
    protected void render() {
        //if(allObjectSize != 0)

    }

    /**
     * Adds a child element to the inner scroll container so it participates in
     * scrolling and clipping.
     *
     * @param el the child to add
     */
    @Override
    public void addChild(GuiElement el)
    {
        scrollContainer.addChild(el);
        updateElementPositions();
        setScrollBarBounds();
    }

    /**
     * Removes a child element from the inner scroll container.
     *
     * @param el the child to remove
     */
    @Override
    public void removeChild(GuiElement el)
    {
        scrollContainer.removeChild(el);
        updateElementPositions();
        setScrollBarBounds();
    }

    /**
     * Removes all child elements from the inner scroll container and resets
     * the cached total content size.
     */
    @Override
    public void removeChilds()
    {
        allObjectSize = 0;
        scrollContainer.removeChilds();
        updateElementPositions();
        setScrollBarBounds();
    }

    /**
     * Returns the live list of children in the inner scroll container.
     *
     * @return the live child list
     */
    @Override
    public List<GuiElement> getChilds()
    {
        return scrollContainer.getChilds();
    }

    protected abstract void childsChanged();

    /**
     * Handles scroll events while the mouse is inside the list view, advancing
     * or rewinding {@link #scrollOffset} by the configured scroll speed.
     *
     * @param delta the scroll delta; positive values scroll towards the start,
     *              negative values scroll towards the end
     * @return {@code true} if the scroll moved the content; {@code false} if the
     *         list cannot scroll any further in the requested direction (in which
     *         case the event is allowed to bubble to other handlers)
     */
    @Override
    public boolean mouseScrolledOverElement(double delta)
    {
        if (delta > 0 && scrollOffset > 0) {
            scrollOffset-=scrolSpeed; // Scroll up
            if(scrollOffset < 0)
                scrollOffset = 0;
            updateElementPositions();
            setScrollBarBounds();
            return true;
        } else if (delta < 0 && scrollOffset < allObjectSize - getContentDimension2()) {
            scrollOffset+=scrolSpeed; // Scroll down
            if(scrollOffset > allObjectSize - getContentDimension2())
                scrollOffset = allObjectSize - getContentDimension2();
            updateElementPositions();
            setScrollBarBounds();
            return true;
        }
        return false;
    }


    /**
     * Forwards the layout to the inner scroll container so it governs the
     * arrangement of child elements.
     *
     * @param layout the layout to apply, or {@code null} to disable layouting
     */
    @Override
    public void setLayout(Layout layout)
    {
        scrollContainer.setLayout(layout);
    }

    /**
     * @return the layout currently applied to the inner scroll container
     */
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
