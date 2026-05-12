package net.kroia.modutilities.gui.elements.base;

import net.kroia.modutilities.ClientPlayerUtilities;
import net.kroia.modutilities.TimerMillis;
import net.kroia.modutilities.gui.IGraphics;
import net.kroia.modutilities.gui.InputConstants;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiTexture;
import net.kroia.modutilities.gui.geometry.Point;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.kroia.modutilities.gui.layout.Layout;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Abstract base class for every widget in the GUI framework.
 * <p>
 * Elements form a tree rooted at a {@link Gui} instance: each element has a
 * {@link #getParent() parent}, a {@link #getRootParent() root parent} (the
 * top-most non-{@code Gui} element in its branch), and a {@link #getRoot()
 * root} reference back to the owning {@link Gui}. Children are added/removed
 * with {@link #addChild(GuiElement)} and {@link #removeChild(GuiElement)};
 * top-level elements are added directly to a {@link Gui} via
 * {@link Gui#addElement(GuiElement)}.
 * <p>
 * Elements participate in a render lifecycle ({@link #init()},
 * {@link #render()}, {@link #renderBackground()}, {@link #renderTooltipInternal()},
 * {@link #renderGizmos()}) and an input lifecycle (mouse click/release/drag/scroll
 * and key/char events). Most public methods that depend on a non-null
 * {@link Gui} root null-check it (e.g. {@link #setFocused()},
 * {@link #isFocused()}, {@link #isMouseOver()}, {@link #getMouseX()},
 * {@link #getMouseY()}) and degrade gracefully when the element is detached.
 * <p>
 * Subclasses must implement {@link #render()} and {@link #layoutChanged()},
 * and should override the appropriate {@code mouse*} / {@code key*} hooks to
 * react to user input.
 *
 * @apiNote This class has no client-only dependencies and can be used on
 *          both client and server. Client-only rendering is handled through
 *          the {@link IGraphics} and {@link net.kroia.modutilities.gui.IInputProvider}
 *          abstractions.
 */
public abstract class GuiElement {

    /**
     * Anchor positions used when aligning sub-rectangles (e.g. labels, tooltip
     * text or child elements) within a parent rectangle.
     */
    public enum Alignment
    {
        CENTER,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    /**
     * Mutable bundle of settings describing the tooltip that an element shows
     * when the mouse hovers over it. Used by {@link #setHoverTooltipData(HoverTooltipData)}
     * and the corresponding {@code setHoverTooltip*} convenience setters.
     */
    public static final class HoverTooltipData
    {
        public Supplier<String> textSupplier = null;
        public boolean enableBackground = true;
        public int textColor = DEFAULT_TEXT_COLOR;
        public int backgroundColor = DEFAULT_TOOLTIP_BACKGROUND_COLOR;
        public int backgroundPadding = DEFAULT_TOOLTIP_BACKGROUND_PADDING;
        public float fontScale = 1.0f;
        public Alignment mousePositionAlignment = Alignment.TOP_LEFT;
    }



    public static int DEFAULT_PADDING = 1;
    public static int DEFAULT_TOOLTIP_BACKGROUND_PADDING = 2;
    public static int DEFAULT_TOOLTIP_BACKGROUND_COLOR = 0xDD555555;
    public static int DEFAULT_TEXT_COLOR = 0xFFFFFFFF;
    public static int DEFAULT_BACKGROUND_COLOR = 0xAA888888;
    public static int DEFAULT_FOCUSED_BACKGROUND_COLOR = 0xAA666666;
    public static int DEFAULT_HOVER_BACKGROUND_COLOR = 0xFFAAAAAA;
    public static int DEFAULT_OUTLINE_COLOR = 0xFF333333;
    public static int DEFAULT_HOVER_TOOLTIP_MOUSE_OFFSET = 5;

    private Gui root;
    private GuiElement parent = null;
    private GuiElement rootParent = null;
    private final Rectangle bounds;
    private float zPos = 0.0f;
    private final Point globalPositon = new Point(0,0);
    private final List<GuiElement> childs = new ArrayList<>();

    private boolean isEnabled = true;
    private boolean checkOverlapForRendering = true; // If true, the element will only be rendered if it is visible (overlaps with the parent)
    private int gizmoColor = 0x55FF0000;
    private int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private int outlineColor = DEFAULT_OUTLINE_COLOR;
    protected boolean enableBackground = true;
    protected boolean enableOutline = true;
    protected int outlineThickness = 1;

    protected Layout layout = null;
    private boolean isRelayoutingThis = false;

    private final TimerMillis tooltipTimer = new TimerMillis(false);
    private boolean tooltipCreateBackground = true;
    private int tooltipBackgroundColor = DEFAULT_TOOLTIP_BACKGROUND_COLOR;
    private int tooltipBackgroundPadding = DEFAULT_TOOLTIP_BACKGROUND_PADDING;
    private Alignment tooltipPositionAlingment = Alignment.TOP_LEFT;
    private int tooltipDelay = 700; // milliseconds
    private boolean tooltipTimerStarted = false;
    private int textColor = DEFAULT_TEXT_COLOR;
    private int tooltipTextColor = DEFAULT_TEXT_COLOR;
    private float textFontScale = 1.0f;
    private float tooltipFontScale = 1.0f;
    private int tooltipHoverMouseOffset = DEFAULT_HOVER_TOOLTIP_MOUSE_OFFSET;

    HoverTooltipData hoverTooltipData = new HoverTooltipData();
    public class TooltipData
    {
        public int x,y;
        public ItemStack item;
        public Component component;
        public String customString;
        public boolean createBackground = tooltipCreateBackground;
        public int backgroundColor = tooltipBackgroundColor;
        public int textColor = tooltipTextColor;
        public int backgroundPadding = tooltipBackgroundPadding;
        public Alignment alignment = tooltipPositionAlingment;
        public float fontScale = tooltipFontScale;
    }
    private final List<TooltipData> drawTooltipLater = new ArrayList<>();



    /**
     * Creates a new element with zero size at the origin.
     */
    public GuiElement() {
        this(0, 0, 0, 0);
    }

    /**
     * Creates a new element with the given bounds.
     *
     * @param x      the x position
     * @param y      the y position
     * @param width  the width
     * @param height the height
     */
    public GuiElement(int x, int y, int width, int height) {
        bounds = new Rectangle(x,y,width, height);
        rootParent = this;
    }

    /**
     * Assigns the {@link Gui} that owns this element and propagates the same
     * root to all descendants.
     *
     * @param root the new owning GUI, or {@code null} to detach
     */
    public void setRoot(Gui root) {
        this.root = root;
        for (GuiElement child : childs) {
            child.setRoot(root);
        }
    }

    /**
     * Initializes this element and all of its descendants, then triggers a
     * layout pass so initial positions are computed.
     */
    public void init()
    {
        for (GuiElement child : childs) {
            child.init();
        }
        layoutChangedInternal();
    }

    /**
     * Assigns a {@link Layout} algorithm responsible for arranging this
     * element's children. The new layout is applied during the next layout
     * pass; pass {@code null} to disable layouting.
     *
     * @param layout the layout to use, or {@code null} to disable
     */
    public void setLayout(Layout layout)
    {
        this.layout = layout;
    }

    /**
     * @return the layout currently applied to this element, or {@code null} if
     *         none is set
     */
    public Layout getLayout()
    {
        return layout;
    }

    /**
     * @return the {@link Gui} that owns this element, or {@code null} if the
     *         element is detached
     */
    public Gui getRoot() {
        return root;
    }

    /**
     * @return this element's direct parent, or {@code null} if it is a top-level
     *         element
     */
    public GuiElement getParent() {
        return parent;
    }

    /**
     * @return the topmost element in this element's branch (the one that is
     *         directly registered with the {@link Gui})
     */
    public GuiElement getRootParent() {
        return rootParent;
    }

    /**
     * @return the font object from the owning Gui, or {@code null} if detached
     *         or running on the server
     */
    public Object getFont()
    {
        return root != null ? root.getFont() : null;
    }

    /**
     * Enables or disables this element. Disabled elements are skipped during
     * rendering and input dispatch; if the element being disabled currently
     * holds focus, focus is cleared on the root.
     *
     * @param visible {@code true} to enable, {@code false} to disable
     */
    public void setEnabled(boolean visible)
    {
        isEnabled = visible;
        if(!isEnabled)
        {
            if(root != null && root.getFocusedElement() == this)
                root.setFocusedElement(null);
        }
    }

    /**
     * @return {@code true} if this element is enabled
     */
    public boolean isEnabled()
    {
        return isEnabled;
    }

    /**
     * Sets whether this element checks for overlap with its parent before
     * rendering. When enabled, elements that lie entirely outside their parent
     * are skipped during rendering.
     *
     * @param checkOverlapForRendering {@code true} to enable the visibility
     *                                  check
     */
    public void setCheckOverlapForRendering(boolean checkOverlapForRendering)
    {
        this.checkOverlapForRendering = checkOverlapForRendering;
    }

    /**
     * @return {@code true} if the parent-overlap visibility check is enabled
     */
    public boolean isCheckOverlapForRendering() {
        return checkOverlapForRendering;
    }

    /**
     * @return {@code true} if this element should currently be rendered
     *         (it is enabled, and either has no parent or intersects its
     *         parent's bounds when overlap checking is enabled)
     */
    public boolean isVisible()
    {
        if(!isEnabled)
            return false;
        if(parent != null && checkOverlapForRendering)
        {
            Rectangle rect1 = new Rectangle(parent.globalPositon.x, parent.globalPositon.y, parent.getWidth(), parent.getHeight());
            Rectangle rect2 = new Rectangle(globalPositon.x, globalPositon.y, bounds.width, bounds.height);
            return rect1.intersects(rect2);
        }
        return true;
    }

    /**
     * Marks this element as the focused element on its root {@link Gui}. Has
     * no effect if the element is detached.
     */
    public void setFocused()
    {
        if (root != null) root.setFocusedElement(this);
    }

    /**
     * Removes focus from this element if it currently holds it. Has no effect
     * if the element is detached or another element is focused.
     */
    public void removeFocus()
    {
        if(root != null && root.getFocusedElement() == this)
            root.setFocusedElement(null);
    }

    /**
     * @return {@code true} if this element currently holds keyboard focus on
     *         its root {@link Gui}; always {@code false} when detached
     */
    public boolean isFocused()
    {
        return root != null && root.getFocusedElement() == this;
    }
    /**
     * Sets the color used to draw the debug gizmo outline.
     *
     * @param color the packed ARGB color
     */
    public void setGizmoColor(int color)
    {
        gizmoColor = color;
    }

    /**
     * @return the packed ARGB color used for the debug gizmo outline
     */
    public int getGizmoColor()
    {
        return gizmoColor;
    }

    /**
     * Sets the background color drawn behind this element when its background
     * is enabled.
     *
     * @param color the packed ARGB color
     */
    public void setBackgroundColor(int color)
    {
        backgroundColor = color;
    }

    /**
     * @return the packed ARGB background color of this element
     */
    public int getBackgroundColor()
    {
        return backgroundColor;
    }

    /**
     * Sets the color used to draw the element's outline.
     *
     * @param outlineColor the packed ARGB outline color
     */
    public void setOutlineColor(int outlineColor) {
        this.outlineColor = outlineColor;
    }

    /**
     * @return the packed ARGB outline color of this element
     */
    public int getOutlineColor() {
        return outlineColor;
    }

    /**
     * Sets the thickness of the element's outline in GUI pixels.
     *
     * @param outlineThickness the new thickness
     */
    public void setOutlineThickness(int outlineThickness) {
        this.outlineThickness = outlineThickness;
    }

    /**
     * @return the outline thickness in GUI pixels
     */
    public int getOutlineThickness() {
        return outlineThickness;
    }

    /**
     * Sets whether the element draws its background fill.
     *
     * @param enableBackground {@code true} to draw the background
     */
    public void setEnableBackground(boolean enableBackground) {
        this.enableBackground = enableBackground;
    }

    /**
     * @return {@code true} if the background fill is enabled
     */
    public boolean isBackgroundEnabled() {
        return enableBackground;
    }

    /**
     * Sets whether the element draws its outline.
     *
     * @param enableOutline {@code true} to draw the outline
     */
    public void setEnableOutline(boolean enableOutline) {
        this.enableOutline = enableOutline;
    }

    /**
     * @return {@code true} if the outline is enabled
     */
    public boolean isOutlineEnabled() {
        return enableOutline;
    }



    // Hover Tooltip
    public void setHoverTooltipSupplier(Supplier<String> hoverTooltipSupplier)
    {
        hoverTooltipData.textSupplier = hoverTooltipSupplier;
    }
    public Supplier<String> getHoverTooltipSupplier()
    {
        return hoverTooltipData.textSupplier;
    }
    public void setHoverTooltipBackgroundColor(int tooltipBackgroundColor) {
        hoverTooltipData.backgroundColor = tooltipBackgroundColor;
    }
    public int getHoverTooltipBackgroundColor() {
        return hoverTooltipData.backgroundColor;
    }
    public void setHoverTooltipEnableBackground(boolean enableTooltipBackground) {
        hoverTooltipData.enableBackground = enableTooltipBackground;
    }
    public boolean isHoverTooltipBackgroundEnabled() {
        return hoverTooltipData.enableBackground;
    }
    public void setHoverTooltipTextColor(int tooltipTextColor) {
        hoverTooltipData.textColor = tooltipTextColor;
    }
    public int getHoverTooltipTextColor() {
        return hoverTooltipData.textColor;
    }
    public void setHoverTooltipBackgroundPadding(int tooltipBackgroundPadding) {
        hoverTooltipData.backgroundPadding = tooltipBackgroundPadding;
    }
    public int getHoverTooltipBackgroundPadding() {
        return hoverTooltipData.backgroundPadding;
    }
    public void setHoverTooltipFontScale(float tooltipFontScale) {
        hoverTooltipData.fontScale = tooltipFontScale;
    }
    public float getHoverTooltipFontScale() {
        return hoverTooltipData.fontScale;
    }
    public void setHoverTooltipMouseOffset(int offset) {
        tooltipHoverMouseOffset = offset;
    }
    public int getHoverTooltipMouseOffset() {
        return tooltipHoverMouseOffset;
    }
    public void setHoverTooltipMousePositionAlignment(Alignment alignment) {
        hoverTooltipData.mousePositionAlignment = alignment;
    }
    public Alignment getHoverTooltipMousePositionAlignment() {
        return hoverTooltipData.mousePositionAlignment;
    }
    public void setHoverTooltipData(HoverTooltipData data)
    {
        if(data == null)
            return;
        hoverTooltipData = data;
    }
    public HoverTooltipData getHoverTooltipData()
    {
        return hoverTooltipData;
    }





    public void setTooltipBackgroundColor(int tooltipBackgroundColor) {
        this.tooltipBackgroundColor = tooltipBackgroundColor;
    }
    public int getTooltipBackgroundColor() {
        return tooltipBackgroundColor;
    }
    public void setTooltipEnableBackground(boolean enableTooltipBackground) {
        this.tooltipCreateBackground = enableTooltipBackground;
    }
    public boolean isTooltipBackgroundEnabled() {
        return tooltipCreateBackground;
    }
    public void setTooltipBackgroundPadding(int tooltipBackgroundPadding) {
        this.tooltipBackgroundPadding = tooltipBackgroundPadding;
    }
    public int getTooltipBackgroundPadding() {
        return tooltipBackgroundPadding;
    }
    public void setTooltipPositionAlignment(Alignment alignment) {
        this.tooltipPositionAlingment = alignment;
    }
    public Alignment getTooltipPositionAlignment() {
        return tooltipPositionAlingment;
    }
    public void setHoverTooltipDelay(int tooltipDelay) {
        this.tooltipDelay = tooltipDelay;
    }
    public int getHoverTooltipDelay() {
        return tooltipDelay;
    }

    public void setTooltipFontScale(float tooltipFontScale) {
        this.tooltipFontScale = tooltipFontScale;
    }
    public float getTooltipFontScale() {
        return tooltipFontScale;
    }

    public void setTooltipTextColor(int tooltipTextColor) {
        this.tooltipTextColor = tooltipTextColor;
    }
    public int getTooltipTextColor() {
        return tooltipTextColor;
    }


    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }
    public int getTextColor() {
        return textColor;
    }
    public void setTextFontScale(float textFontScale) {
        this.textFontScale = textFontScale;
    }
    public float getTextFontScale() {
        return textFontScale;
    }

    /**
     * Hook for subclasses to render the element's background. The default
     * implementation draws the configured background fill and outline.
     */
    protected void renderBackground()
    {
        if(enableBackground)
            renderBackgroundColor();
        if(enableOutline)
            renderOutline();
    }

    /**
     * Renders the foreground (main visual content) of the element. Implemented
     * by every concrete subclass; the framework calls this from
     * {@link #renderInternal()} after applying the local transform.
     */
    protected abstract void render();

    /**
     * Hook for subclasses to render debug gizmos. The default implementation
     * draws the element's bounding box in {@link #gizmoColor}.
     */
    protected void renderGizmos()
    {
        // Draw debug infos to help debug the layout and so on
        drawOutline(0,0,bounds.width, bounds.height, gizmoColor);
    }
    protected void renderBackgroundColor()
    {
        drawRect(0,0,getWidth(), getHeight(),backgroundColor);
    }
    protected void renderOutline()
    {
        drawFrame(0,0,getWidth(),getHeight(), outlineColor,outlineThickness);
    }

    /**
     * Framework hook that runs the background render pass for this element and
     * its descendants, applying the local transform on the pose stack. Called
     * by the parent {@link Gui} or parent element; subclasses normally do not
     * need to override this.
     */
    public void renderBackgroundInternal()
    {
        if(!isVisible())
            return;
        IGraphics graphics = root.getGraphics();
        graphics.pushPose();
        graphics.translate((float)getX(), (float)getY(), zPos);
        renderBackground();
        for (GuiElement child : childs) {
            child.renderBackgroundInternal();
        }
        graphics.popPose();
    }

    /**
     * Framework hook that runs the foreground render pass for this element and
     * its descendants, applying the local transform on the pose stack.
     */
    public void renderInternal()
    {
        if(!isVisible())
            return;
        IGraphics graphics = root.getGraphics();
        graphics.pushPose();
        graphics.translate((float)getX(), (float)getY(), zPos);
        render();
        for (GuiElement child : childs) {
            child.renderInternal();
        }
        graphics.popPose();

    }
    /**
     * Framework hook that runs the tooltip render pass for this element and
     * its descendants. Handles the hover-tooltip delay timer and flushes any
     * tooltips queued via {@code drawTooltip*} calls.
     */
    public void renderTooltipInternal()
    {
        if(!isVisible())
            return;
        if(hoverTooltipData.textSupplier != null && isMouseOver())
        {
            if(!tooltipTimerStarted) {
                tooltipTimer.start(tooltipDelay);
                tooltipTimerStarted = true;
            }
            else if(tooltipTimer.isFinished())
            {
                String tooltip = hoverTooltipData.textSupplier.get();
                if(tooltip != null && !tooltip.isEmpty())
                {
                    Point pos = getMousePos();
                    TooltipData data = new TooltipData();
                    data.x = pos.x;
                    data.y = pos.y;
                    data.customString = tooltip;
                    data.createBackground = hoverTooltipData.enableBackground;
                    data.backgroundColor = hoverTooltipData.backgroundColor;
                    data.backgroundPadding = hoverTooltipData.backgroundPadding;
                    data.alignment = hoverTooltipData.mousePositionAlignment;
                    data.textColor = hoverTooltipData.textColor;
                    data.fontScale = hoverTooltipData.fontScale;
                    drawTooltip(data);
                }
            }
        }
        else {
            tooltipTimer.stop();
            tooltipTimerStarted = false;
        }

        IGraphics graphics = root.getGraphics();
        graphics.pushPose();
        graphics.translate((float)getX(), (float)getY(), 200.0F + zPos);
        for(TooltipData data : drawTooltipLater)
        {
            if(data.item != null)
                drawTooltipInternal(data.item, data.x, data.y);
            else if(data.component != null)
                drawText(data.component, data.x, data.y, data.textColor, data.fontScale);
            else if(data.customString != null) {


                Point size = getTextBounds(data.customString, data.fontScale);
                int x = 0;
                int y = 0;
                switch(data.alignment)
                {
                    case CENTER:
                    case LEFT:
                        x = tooltipHoverMouseOffset;
                        break;
                    case RIGHT:
                        x = -tooltipHoverMouseOffset;
                        break;
                    case TOP:
                        y = tooltipHoverMouseOffset;
                        break;
                    case BOTTOM:
                        y = -tooltipHoverMouseOffset;
                        break;
                    case TOP_LEFT:
                        x = tooltipHoverMouseOffset;
                        y = tooltipHoverMouseOffset;
                        break;
                    case TOP_RIGHT:
                        x = -tooltipHoverMouseOffset;
                        y = tooltipHoverMouseOffset;
                        break;
                    case BOTTOM_LEFT:
                        x = tooltipHoverMouseOffset;
                        y = -tooltipHoverMouseOffset;
                        break;
                    case BOTTOM_RIGHT:
                        x = -tooltipHoverMouseOffset;
                        y = -tooltipHoverMouseOffset;
                        break;
                }

                Rectangle rect = getAlignedBounds(0,0, size.x, size.y, data.alignment,
                        data.x+x, data.y+y,0,0);
                if(data.createBackground)
                {
                    drawRect(rect.x - data.backgroundPadding,
                            rect.y - data.backgroundPadding,
                            size.x + data.backgroundPadding * 2,
                            size.y + data.backgroundPadding * 2, data.backgroundColor);
                }
                drawText(data.customString, rect.x, rect.y, data.textColor, data.fontScale);
            }
        }
        drawTooltipLater.clear();
        for (GuiElement child : childs) {
            child.renderTooltipInternal();
        }
        graphics.popPose();

    }
    /**
     * Framework hook that runs the gizmo (debug overlay) render pass for this
     * element and its descendants.
     */
    public void renderGizmosInternal()
    {
        if(!isVisible())
            return;
        IGraphics graphics = root.getGraphics();
        graphics.pushPose();
        graphics.translate((float)getX(), (float)getY(), zPos);
        renderGizmos();
        for (GuiElement child : childs) {
            child.renderGizmosInternal();
        }
        graphics.popPose();
    }

    protected void enableGlobalScissor(Rectangle area)
    {
        root.enableScissor(area);
    }
    protected void enableScissor(int x, int y, int width, int height)
    {
        enableGlobalScissor(new Rectangle(globalPositon.x+x, globalPositon.y+y, width, height));
    }
    protected void enableScissor(Rectangle area)
    {
        enableGlobalScissor(new Rectangle(globalPositon.x+area.x, globalPositon.y+area.y, area.width, area.height));
    }
    protected void enableScissor()
    {
        enableGlobalScissor(new Rectangle(globalPositon.x, globalPositon.y, bounds.width, bounds.height));
    }

    protected void disableScissor()
    {
        root.disableScissor();
    }
    protected void scissorPause()
    {
        root.scissorPause();
    }
    protected void scissorResume()
    {
        root.scissorResume();
    }

    /**
     * Subclass hook called whenever the element's bounds, position, or layout
     * change. Subclasses use this to recompute internal sub-element placements.
     */
    protected abstract void layoutChanged();

    /**
     * Framework hook that runs a layout pass for this element and propagates
     * it to all descendants. Applies an attached {@link Layout} (if enabled)
     * before invoking {@link #layoutChanged()} and updates the cached global
     * positions of this branch.
     */
    public void layoutChangedInternal()
    {
        if(isRelayoutingThis)
            return;
        if(getGui() == null)
            return;
        if(layout != null) {
            if(layout.enabled)
                layout.apply(this);
        }
        isRelayoutingThis = true;
        layoutChanged();
        isRelayoutingThis = false;

        for (GuiElement child : childs) {
            child.layoutChangedInternal();
        }
        if(rootParent == this)
        {
            updateTransform(0,0);
        }
        else {
            if(parent != null)
                updateTransform(parent.globalPositon.x, parent.globalPositon.y);
        }
    }
    private void updateTransform(int parentX, int parentY)
    {
        globalPositon.x = parentX + bounds.x;
        globalPositon.y = parentY + bounds.y;
        for(GuiElement child : childs)
        {
            child.updateTransform(globalPositon.x, globalPositon.y);
        }
    }

    /**
     * Subclass hook invoked when this element becomes the focused element on
     * its root {@link Gui}. The default implementation does nothing.
     */
    public void focusGained()
    {

    }

    /**
     * Subclass hook invoked when this element loses focus. The default
     * implementation does nothing.
     */
    public void focusLost()
    {

    }
    /**
     * Checks whether the given global point lies inside this element's bounds,
     * taking parent bounds into account (i.e. a point that is inside this
     * element but clipped by an ancestor returns {@code false}).
     *
     * @param globalPosX the x coordinate in global GUI space
     * @param globalPosY the y coordinate in global GUI space
     * @return {@code true} if the point is over this element and visible
     *         through all ancestors
     */
    public boolean isOver(int globalPosX, int globalPosY) {
        if(parent != null && !parent.isOver(globalPosX, globalPosY))
            return false;
        return isOverIgoreParents(globalPosX, globalPosY);
    }

    /**
     * Checks whether the given global point lies inside this element's bounds,
     * ignoring whether ancestors might clip the point.
     *
     * @param globalPosX the x coordinate in global GUI space
     * @param globalPosY the y coordinate in global GUI space
     * @return {@code true} if the point is inside this element's local bounds
     *
     * @deprecated Use {@link #isOverIgnoreParents(int, int)} instead.
     */
    @Deprecated
    public boolean isOverIgoreParents(int globalPosX, int globalPosY) {
        return (globalPosX - globalPositon.x) >= 0 && (globalPosX - globalPositon.x) < bounds.width &&
                (globalPosY - globalPositon.y) >= 0 && (globalPosY - globalPositon.y) < bounds.height;
    }

    /**
     * Checks whether the given global point lies inside this element's bounds,
     * ignoring whether ancestors might clip the point.
     *
     * @param globalPosX the x coordinate in global GUI space
     * @param globalPosY the y coordinate in global GUI space
     * @return {@code true} if the point is inside this element's local bounds
     */
    public boolean isOverIgnoreParents(int globalPosX, int globalPosY) {
        return isOverIgoreParents(globalPosX, globalPosY);
    }

    /**
     * @return {@code true} if the mouse is currently over this element (and not
     *         clipped by an ancestor); always {@code false} when the element is
     *         detached
     */
    public boolean isMouseOver() {
        if (root == null) return false;
        return isOver(root.getMousePosX(), root.getMousePosY());
    }

    /**
     * @return {@code true} if the mouse is over this element's local bounds,
     *         ignoring ancestor clipping; always {@code false} when the element
     *         is detached
     */
    public boolean isMouseOverIgnoreParents() {
        if (root == null) return false;
        return isOverIgoreParents(root.getMousePosX(), root.getMousePosY());
    }

    /**
     * Called when the mouse is clicked
     * @param button The mouse button that was clicked
     */
    protected void mouseClicked(int button) {
    }

    /**
     * Called when the mouse is clicked over the element
     * @param button The mouse button that was clicked
     * @see InputConstants#MOUSE_BUTTON_LEFT
     * @return true if the click was consumed, false otherwise
     */
    protected boolean mouseClickedOverElement(int button) {
        return false;
    }

    /**
     * Called when the mouse is dragged
     * @see InputConstants#MOUSE_BUTTON_LEFT
     * @param button The mouse button that was dragged
     * @param deltaX The change in the x position of the mouse
     * @param deltaY The change in the y position of the mouse
     * @return true if the drag was consumed, false otherwise
     */
    protected boolean mouseDragged(int button, double deltaX, double deltaY) {
        return false;
    }

    /**
     * Called when the mouse is released
     * @see InputConstants#MOUSE_BUTTON_LEFT
     * @param button The mouse button that was released
     */
    protected void mouseReleased(int button) {
    }

    /**
     * Called when the mouse is released over the element
     * @see InputConstants#MOUSE_BUTTON_LEFT
     * @param button The mouse button that was released
     * @return true if the release was consumed, false otherwise
     */
    protected boolean mouseReleasedOverElement(int button) {
        return false;
    }

    /**
     * Called when the mouse is scrolled
     * @param delta The amount of scrolling, positive for scrolling up, negative for scrolling down
     */
    protected void mouseScrolled(double delta) {
    }

    /**
     * Called when the mouse is scrolled over the element
     * @param delta The amount of scrolling, positive for scrolling up, negative for scrolling down
     * @return true if the scroll was consumed, false otherwise
     */
    protected boolean mouseScrolledOverElement(double delta) {
        return false;
    }

    /**
     * Called when a key is pressed
     * @param keyCode The key code of the pressed key
     *                @see InputConstants
     * @param scanCode The scan code of the pressed key
     * @param modifiers The modifiers that were pressed (e.g. shift, ctrl, alt)
     * @return true if the key press was consumed, false otherwise
     */
    protected boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Called when a character is typed
     * @param codePoint The character that was typed
     * @param modifiers The modifiers that were pressed (e.g. shift, ctrl, alt)
     * @return true if the character was consumed, false otherwise
     */
    protected boolean charTyped(char codePoint, int modifiers) {
        return false;
    }


    /**
     * Checks if the the keyboard key is pressed down.
     * @see InputConstants
     *
     * @return true if the given key is pressed
     */
    protected boolean isKeyPressed(int keyCode)
    {
        if (getRoot() == null) return false;
        return getRoot().getInputProvider().isKeyDown(keyCode);
    }
    protected boolean isControlPressed()
    {
        if (getRoot() == null) return false;
        return getRoot().getInputProvider().isKeyDown(InputConstants.KEY_LEFT_CONTROL);
    }
    protected boolean isShiftPressed()
    {
        if (getRoot() == null) return false;
        return getRoot().getInputProvider().isKeyDown(InputConstants.KEY_LEFT_SHIFT);
    }
    protected boolean isAltPressed()
    {
        if (getRoot() == null) return false;
        return getRoot().getInputProvider().isKeyDown(InputConstants.KEY_LEFT_ALT);
    }
    /**
     * Polls the OS for the state of a specific mouse button.
     *
     * @param button the mouse button code (see {@link InputConstants})
     * @return {@code true} if the button is currently held;
     *         always {@code false} when the element is detached
     */
    public boolean isMouseButtonDown(int button)
    {
        if (getRoot() == null) return false;
        return getRoot().getInputProvider().isMouseButtonDown(button);
    }

    /**
     * @return {@code true} if the left mouse button is currently held;
     *         always {@code false} when the element is detached
     */
    public boolean isLeftMouseButtonDown()
    {
        if (getRoot() == null) return false;
        return getRoot().getInputProvider().isMouseButtonDown(InputConstants.MOUSE_BUTTON_LEFT);
    }

    /**
     * @return {@code true} if the right mouse button is currently held;
     *         always {@code false} when the element is detached
     */
    public boolean isRightMouseButtonDown()
    {
        if (getRoot() == null) return false;
        return getRoot().getInputProvider().isMouseButtonDown(InputConstants.MOUSE_BUTTON_RIGHT);
    }

    /**
     * @return {@code true} if the middle mouse button is currently held;
     *         always {@code false} when the element is detached
     */
    public boolean isMiddleMouseButtonDown()
    {
        if (getRoot() == null) return false;
        return getRoot().getInputProvider().isMouseButtonDown(InputConstants.MOUSE_BUTTON_MIDDLE);
    }


    public boolean mouseClickedInternal(int button, boolean isOverParent)
    {
        if(!isEnabled)
            return false;
        isOverParent &= isMouseOver();
        boolean consumed = false;
        for(GuiElement child : childs)
        {
            if(child.mouseClickedInternal(button, isOverParent && !consumed)) {
                consumed = true;
            }
        }
        mouseClicked(button);
        if (isOverParent && !consumed)
            return mouseClickedOverElement(button);
        return consumed;
    }
    public boolean mouseReleasedInternal(int button, boolean isOverParent)
    {
        if(!isEnabled)
            return false;
        isOverParent &= isMouseOver();
        boolean consumed = false;
        for(GuiElement child : childs)
        {
            if(child.mouseReleasedInternal(button, isOverParent && !consumed))
                consumed = true;
        }

        mouseReleased(button);
        if (isOverParent && !consumed)
            return mouseReleasedOverElement(button);

        return consumed;
    }
    public boolean mouseDraggedInternal(int button, double deltaX, double deltaY)
    {
        if(!isEnabled)
            return false;
        for(GuiElement child : childs)
        {
            if(child.mouseDraggedInternal(button, deltaX, deltaY))
                return true;
        }
        return mouseDragged(button, deltaX, deltaY);
    }
    public boolean mouseScrolledInternal(double delta, boolean isOverParent)
    {
        if(!isEnabled)
            return false;
        isOverParent &= isMouseOver();
        boolean consumed = false;
        for(GuiElement child : childs)
        {
            if(child.mouseScrolledInternal(delta, isOverParent && !consumed))
                consumed = true;
        }
        mouseScrolled(delta);
        if (isOverParent && !consumed)
            return mouseScrolledOverElement(delta);
        return consumed;
    }
    public boolean keyPressedInternal(int keyCode, int scanCode, int modifiers)
    {
        if(!isEnabled)
            return false;
        for(GuiElement child : childs)
        {
            if(child.keyPressedInternal(keyCode, scanCode, modifiers))
                return true;
        }
        return keyPressed(keyCode, scanCode, modifiers);
    }
    public boolean charTypedInternal(char codePoint, int modifiers)
    {
        if(!isEnabled)
            return false;
        for(GuiElement child : childs)
        {
            if(child.isEnabled() && child.charTypedInternal(codePoint, modifiers))
                return true;
        }
        return charTyped(codePoint, modifiers);
    }



    /**
     * @return the {@link IGraphics} wrapper of this element's root
     * @throws NullPointerException if this element has no root
     */
    public IGraphics getGraphics() {
        return root.getGraphics();
    }

    public void graphicsPushPose() {
        root.pushPose();
    }
    public void graphicsPopPose() {
        root.popPose();
    }
    public void graphicsTranslate(float x, float y, float z) {
        root.translate(x, y, z);
    }
    public void graphicsTranslate(float x, float y) {
        root.translate(x, y, 0);
    }
    public void graphicsScale(float x, float y, float z)
    {
        root.scale(x, y, z);
    }
    public void graphicsScale(float x, float y)
    {
        root.scale(x, y, 1.0f);
    }
    public void graphicMulPose(Quaternionf quaternion)
    {
        root.mulPose(quaternion);
    }
    public void graphicsRotateAround(Quaternionf quaternion, float x, float y, float z)
    {
        root.rotateAround(quaternion, x, y, z);
    }
    /**
     * @return the {@link Gui} root of this element, or {@code null} if detached
     */
    public Gui getGui()
    {
        return root;
    }


    /**
     * @return the mouse x position relative to this element's local origin;
     *         returns {@code 0} when the element is detached
     */
    public int getMouseX() {
        return root != null ? root.getMousePosX()-globalPositon.x : 0;
    }

    /**
     * @return the mouse y position relative to this element's local origin;
     *         returns {@code 0} when the element is detached
     */
    public int getMouseY() {
        return root != null ? root.getMousePosY()-globalPositon.y : 0;
    }

    /**
     * @return the mouse position relative to this element's local origin
     */
    public Point getMousePos()
    {
        return new Point(getMouseX(), getMouseY());
    }

    /**
     * @return the mouse x position in global GUI coordinates; returns {@code 0}
     *         when the element is detached
     */
    public int getMouseXGlobal() {
        return root != null ? root.getMousePosX() : 0;
    }

    /**
     * @return the mouse y position in global GUI coordinates; returns {@code 0}
     *         when the element is detached
     */
    public int getMouseYGlobal() {
        return root != null ? root.getMousePosY() : 0;
    }

    /**
     * @return the mouse position in global GUI coordinates
     */
    public Point getMousePosGlobal()
    {
        return new Point(getMouseXGlobal(), getMouseYGlobal());
    }

    /**
     * Moves the OS-level cursor to the given position relative to this
     * element's local origin.
     *
     * @param x the local x position
     * @param y the local y position
     */
    public void setMousePos(int x, int y)
    {
        root.moveMouseToPos(x + globalPositon.x, globalPositon.y + y);
    }

    /**
     * Moves the OS-level cursor to the given position in global GUI
     * coordinates.
     *
     * @param x the global x position
     * @param y the global y position
     */
    public void setMousePosGlobal(int x, int y)
    {
        root.moveMouseToPos(x, y);
    }

    /**
     * @return the partial tick value of the current frame from the root
     */
    public float getPartialTick() {
        return root.getPartialTick();
    }

    protected void setParent(GuiElement parent, GuiElement rootParent)
    {
        this.parent = parent;
        this.rootParent = rootParent;
        for(GuiElement child : childs)
        {
            child.setParent(this, rootParent);
        }
    }
    /**
     * Adds a child element to this element. If the child already has a parent,
     * it is first detached from that parent. The child's root and parent
     * references are updated, and a layout pass is triggered.
     *
     * @param el the child element to add
     */
    public void addChild(GuiElement el)
    {
        if(el.getParent() != null)
        {
            el.getParent().removeChild(el);
        }
        el.setRoot(root);
        el.setParent(this, rootParent);
        childs.add(el);
        layoutChangedInternal();
    }

    /**
     * Removes a child element from this element, clearing its parent and root
     * references and triggering a layout pass.
     *
     * @param el the child element to remove
     */
    public void removeChild(GuiElement el)
    {
        el.setRoot(null);
        el.setParent(null, el);
        childs.remove(el);
        layoutChangedInternal();
    }

    /**
     * Removes every child element, clearing their parent and root references
     * and triggering a layout pass.
     */
    public void removeChilds()
    {
        for(GuiElement child : childs)
        {
            child.setRoot(null);
            child.setParent(null, child);
        }
        childs.clear();
        layoutChangedInternal();
    }

    /**
     * @return the live list of children for this element
     */
    public List<GuiElement> getChilds()
    {
        return childs;
    }



    /**
     * @return this element's local x position
     */
    public int getX() {
        return bounds.x;
    }

    /**
     * @return this element's local y position
     */
    public int getY() {
        return bounds.y;
    }

    /**
     * @return this element's local z position
     */
    public float getZ() {
        return zPos;
    }

    /**
     * @return this element's local (x, y) position
     */
    public Point getPosition()
    {
        return new Point(bounds.x, bounds.y);
    }

    /**
     * @return this element's width in GUI pixels
     */
    public int getWidth() {
        return bounds.width;
    }

    /**
     * @return this element's height in GUI pixels
     */
    public int getHeight() {
        return bounds.height;
    }

    /**
     * @return this element's size as a {@link Point} (width, height)
     */
    public Point getSize()
    {
        return new Point(bounds.width, bounds.height);
    }

    /**
     * Sets the element's local x position and triggers a layout pass.
     *
     * @param x the new x position
     */
    public void setX(int x) {
        bounds.x = x;
        layoutChangedInternal();
    }

    /**
     * Sets the element's local y position and triggers a layout pass.
     *
     * @param y the new y position
     */
    public void setY(int y) {
        bounds.y = y;
        layoutChangedInternal();
    }

    /**
     * Sets the element's local z position. Does not trigger a layout pass.
     *
     * @param z the new z position
     */
    public void setZ(float z) {
        zPos = z;
    }

    /**
     * Sets the element's local position and triggers a layout pass.
     *
     * @param x the new x position
     * @param y the new y position
     */
    public void setPosition(int x, int y)
    {
        bounds.x = x;
        bounds.y = y;
        layoutChangedInternal();
    }

    /**
     * Sets the element's local position and triggers a layout pass.
     *
     * @param pos the new position
     */
    public void setPosition(Point pos)
    {
        bounds.x = pos.x;
        bounds.y = pos.y;
        layoutChangedInternal();
    }

    /**
     * Sets the element's width and triggers a layout pass.
     *
     * @param width the new width in GUI pixels
     */
    public void setWidth(int width) {
        bounds.width = width;
        layoutChangedInternal();
    }

    /**
     * Sets the element's height and triggers a layout pass.
     *
     * @param height the new height in GUI pixels
     */
    public void setHeight(int height) {
        bounds.height = height;
        layoutChangedInternal();
    }

    /**
     * Sets the element's size and triggers a layout pass.
     *
     * @param width  the new width in GUI pixels
     * @param height the new height in GUI pixels
     */
    public void setSize(int width, int height)
    {
        bounds.width = width;
        bounds.height = height;
        layoutChangedInternal();
    }

    /**
     * Sets the element's size and triggers a layout pass.
     *
     * @param size the new size as a {@link Point} (x = width, y = height)
     */
    public void setSize(Point size)
    {
        bounds.width = size.x;
        bounds.height = size.y;
        layoutChangedInternal();
    }

    /**
     * Sets the element's local bounds and triggers a layout pass.
     *
     * @param rect the new bounds
     */
    public void setBounds(Rectangle rect)
    {
        bounds.x = rect.x;
        bounds.y = rect.y;
        bounds.width = rect.width;
        bounds.height = rect.height;
        layoutChangedInternal();
    }

    /**
     * Sets the element's local bounds and triggers a layout pass.
     *
     * @param x      the new x position
     * @param y      the new y position
     * @param width  the new width in GUI pixels
     * @param height the new height in GUI pixels
     */
    public void setBounds(int x, int y, int width, int height)
    {
        bounds.x = x;
        bounds.y = y;
        bounds.width = width;
        bounds.height = height;
        layoutChangedInternal();
    }

    /**
     * @return the y coordinate of the top edge of this element (alias for
     *         {@link #getY()})
     */
    public int getTop()
    {
        return bounds.y;
    }

    /**
     * @return the y coordinate of the bottom edge of this element
     */
    public int getBottom()
    {
        return bounds.y+bounds.height;
    }

    /**
     * @return the x coordinate of the left edge of this element (alias for
     *         {@link #getX()})
     */
    public int getLeft()
    {
        return bounds.x;
    }

    /**
     * @return the x coordinate of the right edge of this element
     */
    public int getRight()
    {
        return bounds.x+bounds.width;
    }

    /**
     * @return the live local bounds rectangle of this element
     */
    public Rectangle getBounds()
    {
        return bounds;
    }

    /**
     * @return the live global position of this element (top-left corner in
     *         global GUI coordinates)
     *
     * @deprecated Use {@link #getGlobalPosition()} instead.
     */
    @Deprecated
    public Point getGlobalPositon()
    {
        return globalPositon;
    }

    /**
     * @return the live global position of this element (top-left corner in
     *         global GUI coordinates)
     */
    public Point getGlobalPosition()
    {
        return getGlobalPositon();
    }
    public Rectangle getChildFrame()
    {
        Rectangle frame = new Rectangle(0,0,0,0);
        var localChilds = getChilds();
        for (GuiElement child : localChilds) {
            Rectangle childFrame = child.getChildFrame();
            if(childFrame.x < frame.x)
                frame.x = childFrame.x;
            if(childFrame.y < frame.y)
                frame.y = childFrame.y;
            if(childFrame.x+childFrame.width > frame.x+frame.width)
                frame.width = childFrame.x+childFrame.width-frame.x;
            if(childFrame.y+childFrame.height > frame.y+frame.height)
                frame.height = childFrame.y+childFrame.height-frame.y;
        }
        return frame;
    }

    /**
     * Returns the rendered width of a single line of text using this element's
     * current text font scale.
     *
     * @param text the text to measure
     * @return the width in GUI pixels
     */
    public int getTextWidth(String text)
    {
        if(root == null) return 0;
        return (int)((float)root.getGraphics().getTextWidth(text) * textFontScale);
    }

    /**
     * @return the line height of the default font scaled by this element's
     *         text font scale
     */
    public int getTextHeight()
    {
        if(root == null) return (int)(9 * textFontScale);
        return (int)((float)root.getGraphics().getFontLineHeight() * textFontScale);
    }

    /**
     * Aligns this element's bounds within the given rectangle, preserving its
     * current size.
     *
     * @param alignment the desired alignment within the rectangle
     * @param x         the x position of the alignment rectangle
     * @param y         the y position of the alignment rectangle
     * @param width     the width of the alignment rectangle
     * @param height    the height of the alignment rectangle
     */
    public void applyAlignment(Alignment alignment, int x, int y, int width, int height) {
        var b = getBounds();
        Rectangle bounds = getAlignedBounds(b.x, b.y ,b.width ,b.height , alignment, x, y, width, height);
        setBounds(bounds);
    }


    /**
     * Takes a rectangle1 defined by "x1", "y1", "width1", "height1" and a rectangle2 defined by "x2", "y2", "width2", "height2" and then returns a rectangle
     * that is the same size as rectangle1 but moved inside rectangle2 according to the specified alignment.
     * It moves the rectangle1 inside rectangle2 so that it is aligned according to the specified alignment.
     *
     * @param x1        the x position of the rectangle being aligned
     * @param y1        the y position of the rectangle being aligned
     * @param width1    the width of the rectangle being aligned
     * @param height1   the height of the rectangle being aligned
     * @param alignment the alignment to apply within the second rectangle
     * @param x2        the x position of the alignment rectangle
     * @param y2        the y position of the alignment rectangle
     * @param width2    the width of the alignment rectangle
     * @param height2   the height of the alignment rectangle
     * @return The new bounds of the rectangle, aligned according to the specified alignment.
     */
    public static Rectangle getAlignedBounds(int x1, int y1, int width1, int height1, Alignment alignment, int x2, int y2, int width2, int height2) {
        Rectangle bounds = new Rectangle(x1, y1, width1, height1);
        int xCenter = x2+(width2 - width1) / 2;
        int yCenter = y2+(height2 - height1) / 2;
        switch (alignment) {
            case CENTER:
                bounds.x = xCenter;
                bounds.y = yCenter;
                break;
            case LEFT:
                bounds.x = x2;
                bounds.y = yCenter;
                break;
            case RIGHT:
                bounds.x = width2 - bounds.width+x2;
                bounds.y = yCenter;
                break;
            case TOP:
                bounds.x = xCenter;
                bounds.y = y2;
                break;
            case BOTTOM:
                bounds.x = xCenter;
                bounds.y = height2 - bounds.height+y2;
                break;
            case TOP_LEFT:
                bounds.x = x2;
                bounds.y = y2;
                break;
            case TOP_RIGHT:
                bounds.x = width2 - bounds.width+x2;
                bounds.y = y2;
                break;
            case BOTTOM_LEFT:
                bounds.x = x2;
                bounds.y = height2 - bounds.height+y2;
                break;
            case BOTTOM_RIGHT:
                bounds.x = width2 - bounds.width+x2;
                bounds.y = height2 - bounds.height+y2;
                break;
        }
        return bounds;
    }



    public void drawText(String text, int x, int y, int color)
    {
        root.drawText(text, x,y, color, textFontScale);
    }
    public void drawText(String text, int x, int y)
    {
        drawText(text, x, y, textColor, textFontScale);
    }
    public void drawText(String text, Point pos, int color)
    {
        drawText(text, pos.x, pos.y, color, textFontScale);
    }
    public void drawText(String text, Point pos)
    {
        drawText(text, pos.x, pos.y, textColor, textFontScale);
    }
    public void drawText(Component text, int x, int y, int color)
    {
        root.drawText(text, x, y, color, textFontScale);
    }
    public void drawText(Component text, int x, int y)
    {
        drawText(text, x, y, textColor, textFontScale);
    }
    public void drawText(Component text, Point pos, int color)
    {
        drawText(text, pos.x, pos.y, color, textFontScale);
    }
    public void drawText(Component text, Point pos)
    {
        drawText(text, pos.x, pos.y, textColor, textFontScale);
    }
    public void drawText(Component text, int x, int y, int color, boolean dropShadow)
    {
        root.drawText(text, x, y, color, dropShadow, textFontScale);
    }
    public void drawText(Component text, int x, int y, boolean dropShadow)
    {
        drawText(text, x, y, textColor, dropShadow, textFontScale);
    }
    public void drawText(Component text, Point pos, int color, boolean dropShadow)
    {
        drawText(text, pos.x, pos.y, color, dropShadow, textFontScale);
    }
    public void drawText(Component text, Point pos, boolean dropShadow)
    {
        drawText(text, pos.x, pos.y, textColor, dropShadow, textFontScale);
    }
    public void drawText(String text, Point pos, Alignment posAlignment)
    {
        Point size = getTextBounds(text);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, textColor, textFontScale);
    }
    public void drawText(String text, int x, int y, Alignment posAlignment)
    {
        Point size = getTextBounds(text);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, textColor, textFontScale);
    }
    public void drawText(String text, Point pos, int color, Alignment posAlignment)
    {
        Point size = getTextBounds(text);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, color, textFontScale);
    }
    public void drawText(String text, int x, int y, int color, Alignment posAlignment)
    {
        Point size = getTextBounds(text);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, color, textFontScale);
    }
    public void drawText(Component text, Point pos, boolean dropShadow, Alignment posAlignment)
    {
        Point size = getTextBounds(text.getString());
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, textColor, dropShadow, textFontScale);
    }
    public void drawText(Component text, int x, int y, boolean dropShadow, Alignment posAlignment)
    {
        Point size = getTextBounds(text.getString());
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, textColor, dropShadow, textFontScale);
    }
    public void drawText(Component text, Point pos, int color, boolean dropShadow, Alignment posAlignment)
    {
        Point size = getTextBounds(text.getString());
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, color, dropShadow, textFontScale);
    }
    public void drawText(Component text, int x, int y, int color, boolean dropShadow, Alignment posAlignment)
    {
        Point size = getTextBounds(text.getString());
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, color, dropShadow, textFontScale);
    }


    public void drawText(String text, int x, int y, int color, float fontScale)
    {
        root.drawText(text, x,y, color, fontScale);
    }
    public void drawText(String text, int x, int y, float fontScale)
    {
        drawText(text, x, y, textColor, fontScale);
    }
    public void drawText(String text, Point pos, int color, float fontScale)
    {
        drawText(text, pos.x, pos.y, color, fontScale);
    }
    public void drawText(String text, Point pos, float fontScale)
    {
        drawText(text, pos.x, pos.y, textColor, fontScale);
    }

    public void drawText(Component text, int x, int y, int color, float fontScale)
    {
        root.drawText(text, x, y, color, fontScale);
    }
    public void drawText(Component text, int x, int y, float fontScale)
    {
        drawText(text, x, y, textColor, fontScale);
    }
    public void drawText(Component text, Point pos, int color, float fontScale)
    {
        drawText(text, pos.x, pos.y, color, fontScale);
    }
    public void drawText(Component text, Point pos, float fontScale)
    {
        drawText(text, pos.x, pos.y, textColor, fontScale);
    }
    public void drawText(Component text, int x, int y, int color, boolean dropShadow, float fontScale)
    {
        root.drawText(text, x, y, color, dropShadow, fontScale);
    }
    public void drawText(Component text, int x, int y, boolean dropShadow, float fontScale)
    {
        drawText(text, x, y, textColor, dropShadow, fontScale);
    }
    public void drawText(Component text, Point pos, int color, boolean dropShadow, float fontScale)
    {
        drawText(text, pos.x, pos.y, color, dropShadow, fontScale);
    }
    public void drawText(Component text, Point pos, boolean dropShadow, float fontScale)
    {
        drawText(text, pos.x, pos.y, textColor, dropShadow, fontScale);
    }
    public void drawText(String text, Point pos, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text, fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, fontScale);
    }
    public void drawText(String text, int x, int y, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text, fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, fontScale);
    }
    public void drawText(String text, Point pos, int color, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text, fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, color, fontScale);
    }
    public void drawText(String text, int x, int y, int color, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text, fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, color, fontScale);
    }
    public void drawText(Component text, Point pos, boolean dropShadow, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text.getString(), fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, textColor, dropShadow, fontScale);
    }
    public void drawText(Component text, int x, int y, boolean dropShadow, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text.getString(), fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, textColor, dropShadow, fontScale);
    }
    public void drawText(Component text, Point pos, int color, boolean dropShadow, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text.getString(), fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                pos.x, pos.y,0,0);
        drawText(text, rect.x, rect.y, color, dropShadow, fontScale);
    }
    public void drawText(Component text, int x, int y, int color, boolean dropShadow, Alignment posAlignment, float fontScale)
    {
        Point size = getTextBounds(text.getString(), fontScale);
        Rectangle rect = getAlignedBounds(0,0, size.x, size.y, posAlignment,
                x, y,0,0);
        drawText(text, rect.x, rect.y, color, dropShadow, fontScale);
    }






    public void drawLine(int x1, int y1, int x2, int y2, float thickness, int color)
    {
        root.drawLine(x1,y1,x2,y2,thickness, color);
    }
    public void drawLine(Point start, Point end, float thickness,int color)
    {
        drawLine(start.x, start.y, end.x, end.y, thickness, color);
    }
    public void drawLine(Point start, Point end, float thickness)
    {
        drawLine(start.x, start.y, end.x, end.y, thickness, 0xFFFFFFFF);
    }
    public void drawLine(Point start, Point end)
    {
        drawLine(start.x, start.y, end.x, end.y, 1, 0xFFFFFFFF);
    }

    public void drawVertexBuffer_QUADS(VertexBuffer buffer)
    {
        root.drawVertexBuffer_QUADS(buffer);
    }

    public void drawCross(int x, int y, int size, int color)
    {
        drawRect(x - size, y, size * 2 + 1, 1, color);
        drawRect(x, y - size, 1, size * 2 + 1, color);
    }

    /**
     *   ╔═
     */
    public void drawCornerTL(int x, int y, int size, int color)
    {
        drawRect(x, y, size, 1, color);
        drawRect(x , y, 1, size, color);
    }

    /**
     *   ═╗
     */
    public void drawCornerTR(int x, int y, int size, int color)
    {
        drawRect(x - size+1, y, size, 1, color);
        drawRect(x, y, 1, size, color);
    }

    /**
     *   ╚═
     */
    public void drawCornerBL(int x, int y, int size, int color)
    {
        drawRect(x, y, size, 1, color);
        drawRect(x , y-size+1, 1, size, color);
    }

    /**
     *   ═╝
     */
    public void drawCornerBR(int x, int y, int size, int color)
    {
        drawRect(x - size+1, y, size, 1, color);
        drawRect(x , y- size+1, 1, size, color);
    }



    public void drawRect(int x,int y, int width, int height, int color)
    {
        root.drawRect(x,y,width, height, color);
    }
    public void drawRect(Rectangle rect, int color)
    {
        drawRect(rect.x,rect.y, rect.width, rect.height, color);
    }

    public void drawGradient(int x, int y, int width, int height, int colorFrom, int colorTo)
    {
        root.drawGradient(x,y,width, height, colorFrom, colorTo);
    }
    public void drawGradient(Rectangle rect, int colorFrom, int colorTo)
    {
        drawGradient(rect.x,rect.y, rect.width, rect.height, colorFrom, colorTo);
    }

    public void drawOutline(int x, int y, int width, int height, int color)
    {
        root.drawOutline(x,y,width, height, color);
    }
    public void drawOutline(Rectangle rect, int color)
    {
        drawOutline(rect.x,rect.y, rect.width, rect.height, color);
    }
    public void drawItem(ItemStack item, int x, int y, int seed)
    {
        root.drawItem(item, x, y, seed);
    }
    public void drawItem(ItemStack item, int x, int y)
    {
        drawItem(item, x, y, 0);
    }
    public void drawItem(ItemStack item, Point pos)
    {
        drawItem(item, pos.x, pos.y, 0);
    }
    public void drawItem(ItemStack item, Point pos, int seed)
    {
        drawItem(item, pos.x, pos.y, seed);
    }
    public void drawItemWithDecoration(ItemStack item, int x, int y, int seed)
    {
        root.drawItemWithDecoration(item, x,y, seed);
    }
    public void drawItemWithDecoration(ItemStack item, int x, int y)
    {
        drawItemWithDecoration(item, x, y, 0);
    }
    public void drawItemWithDecoration(ItemStack item, Point pos)
    {
        drawItemWithDecoration(item, pos.x, pos.y, 0);
    }

    public void drawItemWithDecoration(ItemStack item, Point pos, int seed)
    {
        drawItemWithDecoration(item, pos.x, pos.y, seed);
    }
    public void drawItemWithDecoration(ItemStack item, int x, int y, int z, int seed)
    {
        root.drawItemWithDecoration(item, x,y, z, seed);
    }
    public void drawItemWithDecoration(ItemStack item, Point pos, int z, int seed)
    {
        drawItemWithDecoration(item, pos.x, pos.y, z, seed);
    }

    public void drawTexture(GuiTexture texture, int x, int y)
    {
        root.drawTexture(texture.getResourceLocation(), x, y, texture.getUVOffsetX(), texture.getUVOffsetY(), texture.getWidth(), texture.getHeight(), texture.getWidth(), texture.getHeight());
    }
    public void drawTexture(GuiTexture texture, int x, int y, int width, int height)
    {
        root.drawTexture(texture.getResourceLocation(), x, y, texture.getUVOffsetX(), texture.getUVOffsetY(), width, height, width, height);
    }
    public void drawTexture(ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight)
    {
        root.drawTexture(atlasLocation, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }
    public void drawTextureFillArea(GuiTexture texture, int x, int y, int areaWidth, int areaHeight)
    {
        root.drawTexture(texture.getResourceLocation(), x, y, texture.getUVOffsetX(), texture.getUVOffsetY(), areaWidth, areaHeight, texture.getWidth(), texture.getHeight());
    }

    public void drawTexture(GuiTexture texture, Point pos)
    {
        drawTexture(texture, pos.x, pos.y);
    }



    private void drawTooltipInternal(Component tooltip, int x, int y)
    {
        root.drawTooltip(tooltip, x,y);
    }
    private void drawTooltipInternal(Component tooltip, Point pos) {
        drawTooltipInternal(tooltip, pos.x, pos.y);
    }

    private void drawTooltipInternal(ItemStack stack, int x, int y)
    {
        root.drawTooltip(stack, x,y);
    }
    private void drawTooltipInternal(ItemStack stack, Point pos)
    {
        drawTooltipInternal(stack, pos.x, pos.y);
    }

    private void drawTooltipInternal(String msg, int x, int y)
    {
        root.drawTooltip(msg, x,y);
    }
    private void drawTooltipInternal(String msg, Point pos)
    {
        drawTooltipInternal(msg, pos.x, pos.y);
    }
    public void drawTooltipNative(ItemStack stack, Point pos)
    {
        TooltipData data = new TooltipData();
        data.x = pos.x;
        data.y = pos.y;
        data.item = stack;
        data.createBackground = true; // Default to true, can be changed later
        drawTooltipLater.add(data);
    }


    public void drawTooltip(String tooltip, Point pos)
    {
        drawTooltip(tooltip, pos.x, pos.y);
    }
    public void drawTooltip(ItemStack stack, int x, int y)
    {
        drawTooltip(ClientPlayerUtilities.getItemDisplayText(stack), x, y);
    }
    public void drawTooltip(ItemStack stack, Point pos)
    {
        drawTooltip(stack, pos.x, pos.y);
    }

    public void drawTooltip(Component component, int x, int y)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.component = component;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(Component component, Point pos)
    {
        drawTooltip(component, pos.x, pos.y);
    }
    public void drawTooltip(String tooltip, int x, int y)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true; // Default to true, can be changed later
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, boolean createBackground)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = createBackground;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, int backgroundColor, int backgroundPadding, Alignment alignment)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true;
        data.backgroundColor = backgroundColor;
        data.backgroundPadding = backgroundPadding;
        data.alignment = alignment;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(ItemStack stack, int x, int y, int textColor)
    {
        drawTooltip(ClientPlayerUtilities.getItemDisplayText(stack), x, y);
    }
    public void drawTooltip(ItemStack stack, Point pos, int textColor)
    {
        drawTooltip(stack, pos.x, pos.y);
    }
    public void drawTooltip(Component component, int x, int y, int textColor)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.component = component;
        data.textColor = textColor;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(Component component, Point pos, int textColor)
    {
        drawTooltip(component, pos.x, pos.y, textColor);
    }
    public void drawTooltip(String tooltip, int x, int y, int textColor)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true; // Default to true, can be changed later
        data.textColor = textColor;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, boolean createBackground, int textColor)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = createBackground;
        data.textColor = textColor;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, int backgroundColor, int backgroundPadding, Alignment alignment, int textColor)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true;
        data.backgroundColor = backgroundColor;
        data.backgroundPadding = backgroundPadding;
        data.alignment = alignment;
        data.textColor = textColor;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(ItemStack stack, int x, int y, float fontScale)
    {
        drawTooltip(ClientPlayerUtilities.getItemDisplayText(stack), x, y, fontScale);
    }
    public void drawTooltip(ItemStack stack, Point pos, float fontScale)
    {
        drawTooltip(stack, pos.x, pos.y, fontScale);
    }
    public void drawTooltip(Component component, int x, int y, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.component = component;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(Component component, Point pos, float fontScale)
    {
        drawTooltip(component, pos.x, pos.y, fontScale);
    }
    public void drawTooltip(String tooltip, int x, int y, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true; // Default to true, can be changed later
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, boolean createBackground, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = createBackground;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, int backgroundColor, int backgroundPadding, Alignment alignment, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true;
        data.backgroundColor = backgroundColor;
        data.backgroundPadding = backgroundPadding;
        data.alignment = alignment;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(ItemStack stack, int x, int y, int textColor, float fontScale)
    {
        drawTooltip(ClientPlayerUtilities.getItemDisplayText(stack), x, y, fontScale);
    }
    public void drawTooltip(ItemStack stack, Point pos, int textColor, float fontScale)
    {
        drawTooltip(stack, pos.x, pos.y, fontScale);
    }
    public void drawTooltip(Component component, int x, int y, int textColor, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.component = component;
        data.textColor = textColor;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(Component component, Point pos, int textColor, float fontScale)
    {
        drawTooltip(component, pos.x, pos.y, textColor, fontScale);
    }
    public void drawTooltip(String tooltip, int x, int y, int textColor, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true; // Default to true, can be changed later
        data.textColor = textColor;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, boolean createBackground, int textColor, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = createBackground;
        data.textColor = textColor;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public void drawTooltip(String tooltip, int x, int y, int backgroundColor, int backgroundPadding, Alignment alignment, int textColor, float fontScale)
    {
        TooltipData data = new TooltipData();
        data.x = x;
        data.y = y;
        data.customString = tooltip;
        data.createBackground = true;
        data.backgroundColor = backgroundColor;
        data.backgroundPadding = backgroundPadding;
        data.alignment = alignment;
        data.textColor = textColor;
        data.fontScale = fontScale;
        drawTooltipLater.add(data);
    }
    public final void drawTooltip(TooltipData data)
    {
        drawTooltipLater.add(data);
    }





    public Point getTextBounds(String text)
    {
        var lines = text.split("\n");
        int maxWidth = 0;
        for (String line : lines) {
            int width = getTextWidth(line);
            if (width > maxWidth)
                maxWidth = width;
        }
        int height = getTextHeight() * lines.length;
        return new Point(maxWidth, height);
    }
    public Point getTextBounds(String text, float fontScale)
    {
        var lines = text.split("\n");
        int maxWidth = 0;
        IGraphics g = root != null ? root.getGraphics() : null;
        for (String line : lines) {
            int width = g != null ? g.getTextWidth(line) : 0;
            if (width > maxWidth)
                maxWidth = width;
        }
        int lineHeight = g != null ? g.getFontLineHeight() : 9;
        int height = (int)((float)(lineHeight * lines.length) * fontScale);
        return new Point((int)((float)maxWidth*fontScale), height);
    }


    public void drawFrame(int x, int y, int width, int height, int color, int thickness)
    {
        // Horizontal
        drawRect(x, y, width, thickness, color);
        drawRect(x, y+height-thickness, width, thickness, color);

        // Vertical
        drawRect(x, y+thickness, thickness, height-2*thickness, color);
        drawRect(x+width-thickness, y+thickness, thickness, height-2*thickness, color);
    }
    public void drawFrame(Rectangle rect, int color, int thickness)
    {
        drawFrame(rect.x, rect.y, rect.width, rect.height, color, thickness);
    }
    /**
     * @return the display scale factor from the owning Gui, or {@code 1.0} if
     *         detached
     */
    public double getGuiScale()
    {
        return root != null ? root.getDisplayScaleFactor() : 1.0;
    }

    /**
     * Plays a local sound. Delegates to {@link Gui#playLocalSound} which
     * calls through to a client-injected sound player callback. On the
     * server this is a no-op.
     *
     * @param sound  the sound event
     * @param volume volume multiplier
     * @param pitch  pitch multiplier
     */
    public void playLocalSound(SoundEvent sound, float volume, float pitch)
    {
        if(root != null)
            root.playLocalSound(sound, volume, pitch);
    }
    public void playLocalSound(SoundEvent sound, float volume)
    {
        playLocalSound(sound, volume, 1.0F);
    }
    public void playLocalSound(SoundEvent sound)
    {
        playLocalSound(sound, 1.0F, 1.0F);
    }

    public static float map(float value, float start1, float stop1, float start2, float stop2)
    {
        return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
    }
}
