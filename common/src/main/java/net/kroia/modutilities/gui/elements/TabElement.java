package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.ColorUtilities;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Rectangle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TabElement extends GuiElement {


    private final class Tab
    {
        private GuiElement titleElement = null;
        private GuiElement tabElement = null;


        public Tab()
        {

        }
        public Tab(GuiElement titleElement, GuiElement tabElement)
        {
            this.titleElement = titleElement;
            this.tabElement = tabElement;
        }
        public GuiElement getTitleElement() {
            return titleElement;
        }
        public void setTitleElement(GuiElement element) {
            titleElement = element;
        }
        public GuiElement getElement() {
            return tabElement;
        }
        public void setElement(GuiElement element) {
            tabElement = element;
        }
    }


    private final List<Tab> tabs = new ArrayList<>();
    private int selectedTabIndex = -1; // Index of the currently selected tab
    private int titleHeight = 20; // Default height for tab titles, can be adjusted
    private int selectedTitleHeight = 20; // Scale for selected tab titles, can be adjusted
    private int unselectedTitleHeight = 15; // Scale for unselected tab titles, can be adjusted
    private int mouseOverTitleIndex = -1; // Index of the tab title currently under mouse cursor
    private int titleHoverColor = ColorUtilities.setBrightness(DEFAULT_BACKGROUND_COLOR, 0.8f);; // Color for hovered tab title
    private int titleSelectColor = ColorUtilities.setBrightness(titleHoverColor, 0.8f); // Color for hovered tab title
    private int selectOutlineThickness = 2; // Thickness of the outline around the selected tab title
    private Alignment selectedTitleLabelAlignment = Alignment.BOTTOM; // Alignment for the selected tab title label
    private Alignment unselectedTitleLabelAlignment = Alignment.CENTER; // Alignment for unselected tab title labels
    private Function<Integer, String> titleElementTooltipSupplier = null; // Optional tooltip supplier for tab titles
    private Alignment titleElementTooltipMouseAlignment = Alignment.TOP_LEFT;
    public TabElement()
    {

    }


    /**
     * Creates a new tab using a custom guiElement for the tab title button
     * @param titleElement the element to use as the title of the tab
     * @param tabElement the element to use as the content of the tab
     * @return the index of the newly added tab
     */
    public int addTab(GuiElement titleElement, GuiElement tabElement) {
        if(titleElement == null || tabElement == null) {
            throw new IllegalArgumentException("Title and tab elements cannot be null");
        }
        Tab tab = new Tab(titleElement, tabElement);
        tabs.add(tab);
        super.addChild(tab.titleElement);
        if(tab.titleElement.getHoverTooltipSupplier() == null) {
            tab.titleElement.setHoverTooltipSupplier(this::titleElementTooltipSupplierFunc);
            tab.titleElement.setHoverTooltipMousePositionAlignment(titleElementTooltipMouseAlignment);
        }

        if (selectedTabIndex == -1) {
            selectTab(0);
        }
        return tabs.size() - 1; // Return the index of the newly added tab
    }

    /**
     * Creates a new tab using a label as the title of the tab
     * @param tabName the name of the tab to be displayed
     * @param tabElement the element to use as the content of the tab
     * @return the index of the newly added tab
     */
    public int addTab(String tabName, GuiElement tabElement)
    {
        if(tabElement == null) {
            throw new IllegalArgumentException("Tab element cannot be null");
        }
        Label titleElement = new Label(tabName);
        titleElement.setEnableBackground(true);
        //titleElement.setEnableOutline(true);
        titleElement.setTextFontScale(this.getTextFontScale());
        titleElement.setWidth(titleElement.getTextWidth(tabName)+6); // Set default width for the title
        titleElement.setAlignment(Label.Alignment.CENTER);
        return addTab(titleElement, tabElement);
    }

    /**
     * Removes a tab at the specified index.
     * @param index the index of the tab to remove
     * @return true if the tab was successfully removed, false if the index was invalid
     */
    public boolean removeTab(int index)
    {
        if(index < 0 || index >= tabs.size()) {
            return false;
        }
        Tab selectedTab = getSelectedTabInstance();
        if(selectedTabIndex == index)
        {
            super.removeChild(selectedTab.tabElement);
            selectTab(-1);
        }else if(selectedTabIndex > index)
        {
            selectedTabIndex--; // Adjust selected index if removing a tab before the currently selected one
        }
        if(selectedTab != null)
        {
            super.removeChild(selectedTab.titleElement);
        }
        tabs.remove(index);
        return true;
    }

    /**
     * Reorders a tab from one index to another.
     * @param fromIndex the current index of the tab to move
     * @param toIndex the new index for the tab.
     * @return true if the tab was successfully reordered, false if the indices were invalid
     */
    public boolean reorderTab(int fromIndex, int toIndex)
    {
        if(fromIndex < 0 || fromIndex >= tabs.size() || toIndex < 0 || toIndex >= tabs.size()) {
            return false; // Invalid indices
        }
        if(fromIndex == toIndex) {
            return true; // No change needed
        }
        Tab tab = tabs.remove(fromIndex);

        if(selectedTabIndex == fromIndex) {
            selectedTabIndex = toIndex; // Update selected index if it was the moved tab
        } else if(selectedTabIndex > fromIndex && selectedTabIndex <= toIndex) {
            selectedTabIndex--; // Adjust selected index if it was after the moved tab
        } else if(selectedTabIndex < fromIndex && selectedTabIndex >= toIndex) {
            selectedTabIndex++; // Adjust selected index if it was before the moved tab
        }
        //if(toIndex > fromIndex) {
        //    toIndex--; // Adjust toIndex if moving down
        //}
        tabs.add(toIndex, tab); // Insert the tab at the new position
        layoutChangedInternal();
        return true;
    }

    /**
     * @return the number of tabs
     */
    public int getTabCount() {
        return tabs.size();
    }

    /**
     * Clears all tabs from the element.
     * This will remove all tab titles and their associated content elements
     */
    public void clearTabs() {
        for(Tab tab : tabs) {
            super.removeChild(tab.titleElement);
            super.removeChild(tab.tabElement);
        }
        tabs.clear();
        selectedTabIndex = -1; // Reset selected tab index
    }

    /**
     * Sets the overlay color for the selected tab title.
     * @param color the color to set for the selected tab title overlay
     */
    public void setTitleSelectColor(int color) {
        this.titleSelectColor = color;
    }

    /**
     * @return the color of the selected tab title overlay
     */
    public int getTitleSelectColor() {
        return titleSelectColor;
    }

    /**
     * Sets the overlay color for when the mouse hovers over a tab title.
     * @param color the color to set for the hovered tab title overlay
     */
    public void setTitleHoverColor(int color) {
        this.titleHoverColor = color;
    }

    /**
     * @return the color of the hovered tab title overlay
     */
    public int getTitleHoverColor() {
        return titleHoverColor;
    }

    /**
     * Sets the height of the tab title bar.
     * This is not the height of the individual tab titles, but the height of the entire title bar area.
     * @param height the height to set for the tab title bar
     */
    public void setTitleHeight(int height) {
        this.titleHeight = height;
        layoutChangedInternal();
    }
    /**
     * @return the height of the tab title bar
     */
    public int getTitleHeight() {
        return titleHeight;
    }

    /**
     * Sets the height of the selected tab title.
     * This is the height of the tab title when it is selected.
     * @param height the height to set for the selected tab title
     */
    public void setSelectedTitleHeight(int height) {
        this.selectedTitleHeight = Math.min(height, titleHeight); // Ensure selected title height does not exceed total title height
        layoutChangedInternal();
    }

    /**
     * @return the height of the selected tab title
     */
    public int getSelectedTitleHeight() {
        return selectedTitleHeight;
    }

    /**
     * Sets the height of unselected tab titles.
     * This is the height of the tab titles when they are not selected.
     * @param height the height to set for unselected tab titles
     */
    public void setUnselectedTitleHeight(int height) {
        this.unselectedTitleHeight = Math.min(height, titleHeight); // Ensure unselected title height does not exceed total title height
        layoutChangedInternal();
    }

    /**
     * @return the height of unselected tab titles
     */
    public int getUnselectedTitleHeight() {
        return unselectedTitleHeight;
    }

    /**
     * Sets the thickness of the outline around the selected tab title.
     * @param thickness the thickness to set for the outline
     */
    public void setSelectOutlineThickness(int thickness) {
        this.selectOutlineThickness = Math.min(0, thickness);
    }

    /**
     * @return the thickness of the outline around the selected tab title
     */
    public int getSelectOutlineThickness() {
        return selectOutlineThickness;
    }

    /**
     * Sets the alignment for the selected tab title label.
     * The alignment is only applied if the title element is a Label.
     * @param alignment the alignment to set for the selected tab title label
     */
    public void setSelectedTitleLabelAlignment(Alignment alignment) {
        this.selectedTitleLabelAlignment = alignment;
        Tab selectedTab = getSelectedTabInstance();
        for(Tab tab : tabs) {
            if(selectedTab == tab && tab.titleElement instanceof Label label) {
                label.setAlignment(alignment);
            }
        }
    }

    /**
     * @return the alignment for the selected tab title label
     */
    public Alignment getSelectedTitleLabelAlignment() {
        return selectedTitleLabelAlignment;
    }

    /**
     * Sets the alignment for unselected tab title labels.
     * The alignment is only applied if the title element is a Label.
     * @param alignment the alignment to set for unselected tab title labels
     */
    public void setUnselectedTitleLabelAlignment(Alignment alignment) {
        this.unselectedTitleLabelAlignment = alignment;
        Tab selectedTab = getSelectedTabInstance();
        for(Tab tab : tabs) {
            if(selectedTab != tab && tab.titleElement instanceof Label label) {
                label.setAlignment(alignment);
            }
        }
    }

    /**
     * @return the alignment for unselected tab title labels
     */
    public Alignment getUnselectedTitleLabelAlignment() {
        return unselectedTitleLabelAlignment;
    }

    /**
     * Sets a tooltip supplier for the tab title elements.
     * The supplier should return a tooltip string based on the index of the tab title which is provided to the supplier.
     * @param supplier the supplier function that provides the tooltip text for each tab title
     */
    public void setTitleElementHoverTooltipSupplier(Function<Integer, String> supplier) {
        this.titleElementTooltipSupplier = supplier;
    }

    /**
     * Sets the mouse alignment for the hover tooltip of tab title elements.
     * This is the location of the mouse, relative to the tooltip. TOP_LEFT means the mouse position
     * will be in the top left corner of the tooltip.
     * @param alignment the alignment to set for the hover tooltip mouse position
     */
    public void setTitleElementHoverTooltipMouseAlignment(Alignment alignment) {
        this.titleElementTooltipMouseAlignment = alignment;
        for(Tab tab : tabs) {
            tab.titleElement.setHoverTooltipMousePositionAlignment(alignment);
        }
    }


    /**
     * Selects a tab by its index.
     * @param index the index of the tab to select
     */
    public void selectTab(int index)
    {
        if (index >= 0 && index < tabs.size()) {
            // remove old tab elements
            Tab oldTab = getSelectedTabInstance();
            if(oldTab != null) {
                if(oldTab.titleElement instanceof Label label)
                {
                    label.setAlignment(unselectedTitleLabelAlignment);
                }
                super.removeChild(oldTab.tabElement);
            }

            selectedTabIndex = index;
            Tab tab = tabs.get(selectedTabIndex);
            if(tab.titleElement instanceof Label label)
            {
                label.setAlignment(selectedTitleLabelAlignment);
            }
            super.addChild(tab.tabElement);
        }
        else
        {
            // Deselect the current tab
            Tab oldTab = getSelectedTabInstance();
            if(oldTab != null) {
                super.removeChild(oldTab.tabElement);
            }
            selectedTabIndex = -1;
        }
    }

    /**
     * Gets the index of the currently selected tab.
     * @return the index of the selected tab, or -1 if no tab is selected
     */
    public int getSelectedTab()
    {
        return selectedTabIndex;
    }


    /**
     * Gets the currently selected tab content element
     * @return the content element of the selected tab, or null if no tab is selected
     */
    public @Nullable GuiElement getSelectedTabElement()
    {
        Tab selectedTab = getSelectedTabInstance();
        if(selectedTab != null) {
            return selectedTab.tabElement;
        }
        return null; // No tab is selected
    }

    /**
     * Gets the currently selected tab title element
     * @return the title element of the selected tab, or null if no tab is selected
     */
    public @Nullable GuiElement getSelectedTabTitleElement()
    {
        Tab selectedTab = getSelectedTabInstance();
        if(selectedTab != null) {
            return selectedTab.titleElement;
        }
        return null; // No tab is selected
    }

    /**
     * Gets the tab content element at the specified index.
     * @param index the index of the tab to get
     * @return the content element of the tab at the specified index, or null if the index is invalid
     */
    public @Nullable GuiElement getTabElement(int index)
    {
        if(index >= 0 && index < tabs.size()) {
            return tabs.get(index).tabElement;
        }
        return null; // Invalid index
    }

    /**
     * Gets the tab title element at the specified index.
     * @param index the index of the tab title to get
     * @return the title element of the tab at the specified index, or null if the index is invalid
     */
    public @Nullable GuiElement getTabTitleElement(int index)
    {
        if(index >= 0 && index < tabs.size()) {
            return tabs.get(index).titleElement;
        }
        return null; // Invalid index
    }




    @Override
    protected void render() {
        mouseOverTitleIndex = -1; // Reset mouse over title index
        int lightOutlineColor = ColorUtilities.setAlpha(getOutlineColor(), 100);
        for(int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            GuiElement titleElement = tab.getTitleElement();
            if (titleElement.isMouseOver()) {
                mouseOverTitleIndex = i; // Update the index of the tab title under mouse cursor
                drawRect(titleElement.getBounds(), titleHoverColor);
            }
            drawTitleOutline(titleElement.getBounds(), lightOutlineColor, 1); // Draw outline around the tab title
        }
        Tab selectedTab = getSelectedTabInstance();
        if(selectedTab != null) {
            Rectangle titleBounds = selectedTab.titleElement.getBounds();
            drawRect(titleBounds, titleSelectColor); // Highlight the selected tab

            int lineColor = getOutlineColor();
            drawTitleOutline(titleBounds, lineColor, selectOutlineThickness); // Draw outline around the selected tab title
        }
    }
    protected void drawTitleOutline(Rectangle bounds, int color, int lineWidth)
    {
        drawRect(bounds.x, bounds.y, lineWidth, bounds.height, color); // Draw left border
        drawRect(bounds.x + bounds.width - lineWidth, bounds.y, lineWidth, bounds.height, color); // Draw right border
        drawRect(bounds.x, bounds.y, bounds.width, lineWidth, color); // Draw top border
    }

    @Override
    protected void layoutChanged() {
        Tab tab = getSelectedTabInstance();
        if(tab == null)
            return;

        int width = getWidth();
        int height = getHeight() - titleHeight; // Adjust height for the title bar

        int titleX = 0;
        int yOffset = titleHeight - unselectedTitleHeight; // Offset for unselected titles to center them vertically
        for(Tab t : tabs) {
            if(t.titleElement != null) {
                int titleWidth = t.titleElement.getWidth();
                if(t == tab) {
                    t.titleElement.setBounds(titleX, titleHeight - selectedTitleHeight, titleWidth, selectedTitleHeight); // Set title height to the full title height
                }
                else {
                    t.titleElement.setBounds(titleX, yOffset, titleWidth, unselectedTitleHeight); // Set title height to 80% of the total title height
                }
                titleX += titleWidth;
            }
        }
        tab.tabElement.setBounds(0, titleHeight, width, height);
    }




    protected String titleElementTooltipSupplierFunc()
    {
        if(titleElementTooltipSupplier != null && mouseOverTitleIndex >= 0 && mouseOverTitleIndex < tabs.size())
        {
            return titleElementTooltipSupplier.apply(mouseOverTitleIndex);
        }
        return null;
    }


    @Override
    protected boolean mouseClickedOverElement(int button)
    {
        if(button == 0 && mouseOverTitleIndex > -1)
        {
            selectTab(mouseOverTitleIndex);
        }
        return false; // No tab title clicked
    }

    private @Nullable Tab getSelectedTabInstance()
    {
        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
            return tabs.get(selectedTabIndex);
        }
        return null;
    }

    @Override
    public void addChild(GuiElement el)
    {

    }

    @Override
    public void removeChild(GuiElement el)
    {

    }

    @Override
    public void removeChilds()
    {

    }

    @Override
    public List<GuiElement> getChilds()
    {
        return new ArrayList<>();
    }

    @Override
    public void setTextFontScale(float textFontScale) {
        super.setTextFontScale(textFontScale);
        // Propagate text font scale to all tab title elements
        for(Tab tab : tabs) {
            if(tab.titleElement != null) {
                tab.titleElement.setTextFontScale(textFontScale);
            }
        }
    }

}
