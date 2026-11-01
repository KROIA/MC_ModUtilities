package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.layout.Layout;
import net.kroia.modutilities.gui.layout.LayoutVertical;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.function.Consumer;

/**
 * A collapsible/expandable container element.
 * <p>
 * An {@code ExpandablePanel} is composed of a clickable <b>header</b> (a title
 * {@link Label} plus an expand/collapse arrow indicator) and a <b>content
 * area</b> that hosts arbitrary child {@link GuiElement}s stacked vertically.
 * Clicking the header (or calling {@link #setExpanded(boolean)} / {@link #toggle()})
 * shows or hides the content area and changes the panel's effective height
 * accordingly.
 * <p>
 * <b>Reflow:</b> when the panel expands or collapses its own height changes and it
 * notifies the surrounding layout so a containing element (for example a
 * {@link VerticalListView}) re-lays-out and pushes sibling elements. This reuses
 * the framework's existing {@link #layoutChangedInternal()} mechanism (the same
 * one {@link DropDownMenu} and {@link net.kroia.modutilities.gui.elements.base.ListView}
 * rely on) rather than introducing a parallel notification path: after resizing,
 * the panel triggers a layout pass on its {@linkplain #getRootParent() root
 * parent} so the whole top-level branch (including any enclosing list) recomputes.
 * <p>
 * While collapsed the content area is {@linkplain #setEnabled(boolean) disabled},
 * so hidden children are neither rendered nor receive mouse/keyboard input.
 * <p>
 * Children are added, removed and queried through the overridden
 * {@link #addChild(GuiElement)}, {@link #removeChild(GuiElement)},
 * {@link #removeChilds()} and {@link #getChilds()} methods, which route to the
 * inner content container (mirroring how {@link DropDownMenu} and
 * {@link net.kroia.modutilities.gui.elements.base.ListView} expose their
 * children).
 *
 * @apiNote The {@code gui/} package is client-only
 *          ({@code @Environment(EnvType.CLIENT)}); expandable panels must only be
 *          used on the client.
 */
public class ExpandablePanel extends GuiElement {

    /** Default header height in pixels used when none is specified. */
    public static final int DEFAULT_HEADER_HEIGHT = 20;
    /** Default inner padding (in pixels) reserved around the content children. */
    public static final int DEFAULT_CONTENT_PADDING = 2;
    /** Default spacing (in pixels) between adjacent content children. */
    public static final int DEFAULT_CONTENT_SPACING = 2;
    /** Default horizontal padding (in pixels) before the title text in the header. */
    public static final int DEFAULT_HEADER_TEXT_PADDING = 4;

    /**
     * Concrete container used to host the panel's content children.
     * <p>
     * It carries the {@link LayoutVertical} that stacks children top-to-bottom and
     * has no visuals of its own; the panel positions and sizes it.
     */
    private static final class ContentContainer extends GuiElement {
        private ContentContainer() {
            super();
            setEnableBackground(false);
            setEnableOutline(false);
        }

        @Override
        protected void render() {
            // The container itself draws nothing; its children render normally.
        }

        @Override
        protected void layoutChanged() {
            // Child arrangement is delegated to the attached Layout, which the
            // framework applies from layoutChangedInternal() before this hook runs.
        }
    }

    private final EmptyButton headerButton;
    private final Label titleLabel;
    private final TextureElement arrowCollapsed; // shown while collapsed (points down: click to expand)
    private final TextureElement arrowExpanded;  // shown while expanded  (points up:   click to collapse)
    private final ContentContainer contentContainer;

    private int headerHeight = DEFAULT_HEADER_HEIGHT;
    private int contentPadding = DEFAULT_CONTENT_PADDING;
    private int contentSpacing = DEFAULT_CONTENT_SPACING;
    private int headerTextPadding = DEFAULT_HEADER_TEXT_PADDING;
    private boolean expanded;

    private Consumer<Boolean> onToggle = null;

    /**
     * Creates a collapsed panel with the given title.
     *
     * @param title the header title text
     */
    public ExpandablePanel(String title) {
        this(title, false);
    }

    /**
     * Creates a panel with the given title and initial expansion state.
     *
     * @param title            the header title text
     * @param initiallyExpanded {@code true} to start expanded, {@code false} to start collapsed
     */
    public ExpandablePanel(String title, boolean initiallyExpanded) {
        super();
        this.expanded = initiallyExpanded;

        // Panel frame: outline only, transparent fill so the header/content provide the visuals.
        setEnableBackground(false);
        setEnableOutline(true);

        // Clickable header background.
        headerButton = new EmptyButton(this::toggle);

        // Title text, vertically centered and left aligned.
        titleLabel = new Label(title);
        titleLabel.setAlignment(Alignment.LEFT);
        headerButton.addChild(titleLabel);

        // Arrow indicators, reusing the shared ModUtilities dropdown arrow textures.
        arrowCollapsed = new TextureElement(ModUtilitiesMod.MOD_ID, "textures/gui/arrow_down.png", 16, 16);
        arrowExpanded = new TextureElement(ModUtilitiesMod.MOD_ID, "textures/gui/arrow_up.png", 20, 20);
        headerButton.addChild(expanded ? arrowExpanded : arrowCollapsed);

        // Content container with a vertical, width-stretching layout.
        contentContainer = new ContentContainer();
        Layout contentLayout = new LayoutVertical(contentPadding, contentSpacing, true, false);
        contentContainer.setLayout(contentLayout);
        contentContainer.setEnabled(expanded);

        super.addChild(headerButton);
        super.addChild(contentContainer);

        setSize(120, headerHeight);
    }

    /**
     * Sets the header title text.
     *
     * @param title the new title text (may be {@code null}, coerced to empty by {@link Label})
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    /**
     * @return the current header title text (never {@code null})
     */
    public String getTitle() {
        return titleLabel.getText();
    }

    /**
     * @return {@code true} if the panel is currently expanded (content visible)
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * Expands or collapses the panel. Calling with the current state is a no-op.
     * <p>
     * On a state change the content area is enabled/disabled, the arrow indicator
     * is swapped, the panel's height is recomputed and the surrounding layout is
     * notified so any enclosing list reflows. The {@link #setOnToggle(Consumer)
     * toggle listener} is fired with the new state.
     *
     * @param expanded {@code true} to expand, {@code false} to collapse
     */
    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded)
            return;
        this.expanded = expanded;
        markDirty();

        contentContainer.setEnabled(expanded);

        // Swap the arrow indicator to reflect the new state.
        headerButton.removeChild(expanded ? arrowCollapsed : arrowExpanded);
        headerButton.addChild(expanded ? arrowExpanded : arrowCollapsed);

        // Recompute our own bounds (height depends on the expansion state) ...
        layoutChangedInternal();
        // ... then let the surrounding layout (e.g. a VerticalListView) reflow.
        notifyLayoutReflow();

        if (onToggle != null)
            onToggle.accept(expanded);
    }

    /**
     * Toggles the panel between expanded and collapsed.
     */
    public void toggle() {
        setExpanded(!expanded);
    }

    /**
     * Registers a listener invoked whenever the expansion state changes.
     *
     * @param onToggle a consumer receiving the new expanded state, or {@code null} to clear
     */
    public void setOnToggle(Consumer<Boolean> onToggle) {
        this.onToggle = onToggle;
    }

    /**
     * @return the toggle listener, or {@code null} if none is registered
     */
    public Consumer<Boolean> getOnToggle() {
        return onToggle;
    }

    /**
     * Sets the height (in pixels) of the clickable header row and triggers a
     * layout pass.
     *
     * @param headerHeight the header height in pixels
     */
    public void setHeaderHeight(int headerHeight) {
        this.headerHeight = headerHeight;
        layoutChangedInternal();
        notifyLayoutReflow();
    }

    /**
     * @return the height of the clickable header row in pixels
     */
    public int getHeaderHeight() {
        return headerHeight;
    }

    /**
     * Sets the inner padding (in pixels) reserved around the content children and
     * triggers a layout pass.
     *
     * @param contentPadding the content padding in pixels
     */
    public void setContentPadding(int contentPadding) {
        this.contentPadding = contentPadding;
        Layout layout = contentContainer.getLayout();
        if (layout != null)
            layout.padding = contentPadding;
        layoutChangedInternal();
        notifyLayoutReflow();
    }

    /**
     * @return the inner content padding in pixels
     */
    public int getContentPadding() {
        return contentPadding;
    }

    /**
     * Sets the vertical spacing (in pixels) between adjacent content children and
     * triggers a layout pass.
     *
     * @param contentSpacing the spacing in pixels
     */
    public void setContentSpacing(int contentSpacing) {
        this.contentSpacing = contentSpacing;
        Layout layout = contentContainer.getLayout();
        if (layout != null)
            layout.spacing = contentSpacing;
        layoutChangedInternal();
        notifyLayoutReflow();
    }

    /**
     * @return the vertical spacing between adjacent content children in pixels
     */
    public int getContentSpacing() {
        return contentSpacing;
    }

    /**
     * @return the total height (in pixels) the content area occupies when
     *         expanded, computed from the current content children
     */
    public int getContentHeight() {
        return computeContentHeight();
    }

    // ------------------------------------------------------------------
    // Styling hooks (delegate to the header button / inherited element API)
    // ------------------------------------------------------------------

    /**
     * Sets the header background color (idle state).
     *
     * @param color the packed ARGB color
     */
    public void setHeaderColor(int color) {
        headerButton.setBackgroundColor(color);
    }

    /**
     * @return the header background color (idle state) in packed ARGB
     */
    public int getHeaderColor() {
        return headerButton.getBackgroundColor();
    }

    /**
     * Sets the header background color shown while the mouse hovers the header.
     *
     * @param color the packed ARGB color
     */
    public void setHeaderHoverColor(int color) {
        headerButton.setHoverColor(color);
    }

    /**
     * Sets the header background color shown while the header is pressed.
     *
     * @param color the packed ARGB color
     */
    public void setHeaderPressedColor(int color) {
        headerButton.setPressedColor(color);
    }

    /**
     * Sets the color of the header title text.
     *
     * @param color the packed ARGB color
     */
    public void setHeaderTextColor(int color) {
        titleLabel.setTextColor(color);
    }

    /**
     * @return the packed ARGB color of the header title text
     */
    public int getHeaderTextColor() {
        return titleLabel.getTextColor();
    }

    /**
     * Sets the background color of the content area. The content background is
     * disabled by default; call {@link #setContentBackgroundEnabled(boolean)} to
     * make it visible.
     *
     * @param color the packed ARGB color
     */
    public void setContentBackgroundColor(int color) {
        contentContainer.setBackgroundColor(color);
    }

    /**
     * Enables or disables the content area background fill.
     *
     * @param enabled {@code true} to draw the content background
     */
    public void setContentBackgroundEnabled(boolean enabled) {
        contentContainer.setEnableBackground(enabled);
    }

    // ------------------------------------------------------------------
    // Child management (routed to the content container)
    // ------------------------------------------------------------------

    /**
     * Adds a child element to the content area. The child participates in the
     * vertical content layout and is only visible/interactive while the panel is
     * expanded.
     *
     * @param el the child to add; {@code null} is ignored
     */
    @Override
    public void addChild(GuiElement el) {
        if (el == null)
            return;
        contentContainer.addChild(el);
        if (expanded) {
            layoutChangedInternal();
            notifyLayoutReflow();
        }
    }

    /**
     * Removes a child element from the content area.
     *
     * @param el the child to remove
     */
    @Override
    public void removeChild(GuiElement el) {
        contentContainer.removeChild(el);
        if (expanded) {
            layoutChangedInternal();
            notifyLayoutReflow();
        }
    }

    /**
     * Removes every child element from the content area.
     */
    @Override
    public void removeChilds() {
        contentContainer.removeChilds();
        if (expanded) {
            layoutChangedInternal();
            notifyLayoutReflow();
        }
    }

    /**
     * @return the live list of content children (excludes the internal header and
     *         content-container elements)
     */
    @Override
    public List<GuiElement> getChilds() {
        return contentContainer.getChilds();
    }

    /**
     * @return an empty list; content children are managed by the panel and are not
     *         serialized through the GUI state-sync system (mirrors
     *         {@link DropDownMenu})
     */
    @Override
    public List<GuiElement> getSerializableChildren() {
        return List.of();
    }

    @Override
    public CompoundTag serializeState() {
        CompoundTag tag = super.serializeState();
        tag.putBoolean("expanded", expanded);
        return tag;
    }

    @Override
    public void deserializeState(CompoundTag tag) {
        super.deserializeState(tag);
        if (tag.contains("expanded")) {
            setExpanded(tag.getBoolean("expanded"));
        }
    }

    @Override
    protected void render() {
        // The panel itself only draws its optional background/outline (handled by
        // the base class); header and content render as children.
    }

    @Override
    protected void layoutChanged() {
        int width = getWidth();

        // Header spans the full width along the top.
        headerButton.setBounds(0, 0, width, headerHeight);

        // Title fills the header minus the arrow column on the right.
        int arrowColumn = headerHeight;
        int titleWidth = Math.max(0, width - arrowColumn - headerTextPadding);
        titleLabel.setBounds(headerTextPadding, 0, titleWidth, headerHeight);

        // Arrow indicator, right-aligned and square within the header row.
        int arrowPadding = Math.max(0, (headerHeight - 12) / 2);
        int arrowSize = Math.max(1, headerHeight - 2 * arrowPadding);
        int arrowX = width - arrowColumn + (arrowColumn - arrowSize) / 2;
        arrowCollapsed.setBounds(arrowX, arrowPadding, arrowSize, arrowSize);
        arrowExpanded.setBounds(arrowX, arrowPadding, arrowSize, arrowSize);

        // Content area sits below the header; its height depends on the state.
        int contentHeight = expanded ? computeContentHeight() : 0;
        contentContainer.setBounds(0, headerHeight, width, contentHeight);

        // Enforce the panel's effective height. setHeight() re-enters
        // layoutChangedInternal(), but the base-class re-entrancy guard makes that
        // a no-op here, so this only updates the stored height (same pattern used
        // by DropDownMenu).
        setHeight(headerHeight + contentHeight);
    }

    /**
     * @return the pixel height required to display all content children, including
     *         top/bottom padding and inter-child spacing; {@code 0} when empty
     */
    private int computeContentHeight() {
        List<GuiElement> children = contentContainer.getChilds();
        if (children.isEmpty())
            return 0;
        int total = contentPadding * 2;
        for (int i = 0; i < children.size(); i++) {
            total += children.get(i).getHeight();
            if (i < children.size() - 1)
                total += contentSpacing;
        }
        return total;
    }

    /**
     * Triggers a layout pass on the {@linkplain #getRootParent() root parent} so a
     * surrounding container (such as a {@link VerticalListView}) reflows after this
     * panel changes height. No-op when the panel is a detached or top-level
     * element.
     */
    private void notifyLayoutReflow() {
        GuiElement rootParent = getRootParent();
        if (rootParent != null && rootParent != this) {
            rootParent.layoutChangedInternal();
        }
    }
}
