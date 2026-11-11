package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.NbtCompat;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.ListView;
import net.kroia.modutilities.gui.layout.Layout;
import net.kroia.modutilities.gui.layout.LayoutHorizontal;
import net.kroia.modutilities.gui.layout.LayoutVertical;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A clickable drop-down menu showing a label, an expand button, and a vertically scrolling
 * list of options. When expanded, the menu renders above sibling elements at an elevated
 * Z-position; clicking outside the menu collapses it.
 * <p>
 * Options are added with {@link #addOption(String)} or by passing arbitrary {@link GuiElement}s to
 * {@link #addChild(GuiElement)}. The currently selected option index is tracked and reported
 * to the {@link BiConsumer} registered via {@link #setOnOptionSelected(BiConsumer)}.
 * <p>
 * Selecting an option collapses the menu, shows the option's text in the label (for options
 * added with {@link #addOption(String)}) and highlights that row the next time the menu opens.
 * The callback runs afterwards, so it may set a fixed label text or re-open the menu.
 */
public class DropDownMenu extends GuiElement {

    /** Option row; marker type so {@link #getChilds()}/{@link #removeChild} can find the wrapped element. */
    private static class EmptyButtonChanged extends EmptyButton {
        private final int idleColor = getBackgroundColor();
        /** Highlights the selected row with the row's own pressed color. */
        void setSelected(boolean selected) {
            setBackgroundColor(selected ? getPressedColor() : idleColor);
        }
    }

    private final ListView listView;

    private final Label label;
    private GuiElement customLabelElement = null;
    private final Button button;
    private final TextureElement arrowTextureDown;
    private final TextureElement arrowTextureUp;


    private int maxExpandedHeight = 100; // Height when expanded, 0 means collapsed
    private int unexpandedHeight = 0;
    private int expandedHeight = 0;
    private boolean isExpanded = false;
    private int selectedIndex = -1;
    private float expandedZPos = 200.f;
    private float defaultZPos = 0.f;

    private BiConsumer<Integer, GuiElement> optionSelected;
    /**
     * Creates a drop-down menu with the given label text and a selection callback.
     * @param label the label text shown in the collapsed menu
     * @param optionSelected callback invoked with the selected index and the option element
     */
    public DropDownMenu(String label, BiConsumer<Integer, GuiElement> optionSelected) {
        super();
        this.optionSelected = optionSelected;

        this.label = new Label(label);
        this.label.setHeight(20);
        this.label.setAlignment(Alignment.CENTER);
        customLabelElement = this.label;
        super.addChild(customLabelElement);

        button = new Button("", this::onExpandButtonClicked);
        button.setHeight(20);

        arrowTextureDown = new TextureElement(ModUtilitiesMod.MOD_ID ,"textures/gui/arrow_down.png", 16,16);
        arrowTextureUp = new TextureElement(ModUtilitiesMod.MOD_ID ,"textures/gui/arrow_up.png", 20,20);
        button.addChild(arrowTextureDown);


        listView = new VerticalListView() {
            // The open list hangs below the menu, outside its parents' bounds. Plain parents don't
            // clip it when drawn, only an enclosing ListView's scissor does: hit-test against that.
            @Override
            public boolean isOver(int globalPosX, int globalPosY) {
                return isOverIgnoreParents(globalPosX, globalPosY)
                        && isInsideEnclosingListView(DropDownMenu.this, globalPosX, globalPosY);
            }
            // Input dispatch gates on the parents' bounds too: re-seed it so isOver() above decides.
            // Not for clicks when the Gui tracks this menu: Gui.mouseClicked() already delivered them first.
            @Override
            public boolean mouseClickedInternal(int button, boolean isOverParent) {
                Gui root = getRoot();
                return super.mouseClickedInternal(button, isOverParent || root == null || root.getOpenDropDown() != DropDownMenu.this);
            }
            @Override
            public boolean mouseScrolledInternal(double delta, boolean isOverParent) {
                return super.mouseScrolledInternal(delta, true);
            }
        };
        Layout layout = new LayoutVertical();
        layout.stretchX = true;
        listView.setLayout(layout);
        listView.setEnabled(false);
        listView.setCheckOverlapForRendering(false);

        super.addChild(button);
        super.addChild(listView);

        this.setSize(100, 20);

    }
    /**
     * Creates a drop-down menu with the given label text and no selection callback.
     * @param label the label text shown in the collapsed menu
     */
    public DropDownMenu(String label)
    {
        this(label, null);
    }


    @Override
    public void setZ(float z) {
        super.setZ(z);
        defaultZPos = z;
    }
    /**
     * Sets the Z-position used while the menu is expanded so it can render above sibling elements.
     * @param z the Z value to use when expanded
     */
    public void setExpandedZPos(float z) {
        this.expandedZPos = z;
    }
    /**
     * @return the Z-position used while the menu is expanded
     */
    public float getExpandedZPos() {
        return expandedZPos;
    }
    /**
     * Sets the text shown on the default label element.
     * Has no effect if a custom label element has fully replaced the default label.
     * @param text the new label text
     */
    public void setLabelText(String text)
    {
        if(this.label != null)
            this.label.setText(text);
    }
    /**
     * @return the current text of the default label, or empty string if there is no default label
     */
    public String getLabelText()
    {
        if(this.label == null)
            return "";
        return this.label.getText();
    }
    /**
     * Replaces the label area of the drop-down with a custom element.
     * The previous label element is removed.
     * @param element the new label element; ignored if {@code null}
     */
    public void setCustomLabelElement(GuiElement element)
    {
        if(element == null)
            return;
        if(customLabelElement != null)
            super.removeChild(customLabelElement);
        customLabelElement = element;
        super.addChild(customLabelElement);
    }

    /**
     * Sets the callback invoked when the user picks an option from the list.
     * It runs after the menu has collapsed and updated its label.
     * @param optionSelected callback receiving (index, option element), or {@code null} to clear
     */
    public void setOnOptionSelected(BiConsumer<Integer, GuiElement> optionSelected)
    {
        this.optionSelected = optionSelected;
    }

    /**
     * Sets the maximum height (in pixels) of the option list area when expanded.
     * Excess content scrolls within this bound.
     * @param maxExpandedHeight the maximum height of the expanded list area
     */
    public void setMaxExpandedHeight(int maxExpandedHeight) {
        this.maxExpandedHeight = maxExpandedHeight;
    }
    /**
     * @return the maximum height of the expanded option list
     */
    public int getMaxExpandedHeight() {
        return maxExpandedHeight;
    }
    /**
     * @return the current expanded option-list height, capped at {@link #getMaxExpandedHeight()}
     */
    public int getExpandedHeight() {
        expandedHeight = Math.min(listView.getSizeHintHeight(), maxExpandedHeight);
        return expandedHeight;
    }
    /**
     * @return the height the menu had before being expanded (i.e. the collapsed height)
     */
    public int getUnexpandedHeight() {
        return unexpandedHeight;
    }
    /**
     * @return {@code true} if the option list is currently visible
     */
    public boolean isExpanded() {
        return isExpanded;
    }
    /**
     * Expands or collapses the option list.
     * Calling with the current state is a no-op.
     * While expanded, the selected option row is highlighted and clicks on the open list
     * are delivered to it before any other element (see {@link Gui#mouseClicked(double, double, int)}).
     * @param expanded {@code true} to expand, {@code false} to collapse
     */
    public void setExpanded(boolean expanded) {
        if(expanded == isExpanded)
            return;
        isExpanded = expanded;
        Gui root = getRoot();
        if(root != null)
            root.setOpenDropDown(this, expanded);
        if(isExpanded)
        {
            updateSelectionHighlight();
            super.setZ(expandedZPos);
            expandedHeight = Math.min(listView.getSizeHintHeight(), maxExpandedHeight);
            unexpandedHeight = getHeight();
            this.setHeight(button.getHeight() + expandedHeight);
            //listView.setHeight(expandedHeight);
            listView.setEnabled(true);
            //button.setLabel("^");
            button.removeChild(arrowTextureDown);
            button.addChild(arrowTextureUp);
        }
        else
        {
            super.setZ(defaultZPos);
            this.setHeight(unexpandedHeight);
            listView.setEnabled(false);
            //button.setLabel("v");
            button.removeChild(arrowTextureUp);
            button.addChild(arrowTextureDown);
        }
        layoutChangedInternal();
    }
    /**
     * Convenience method equivalent to {@code setExpanded(true)}.
     */
    public void expand()
    {
        setExpanded(true);
    }
    /**
     * Convenience method equivalent to {@code setExpanded(false)}.
     */
    public void collapse()
    {
        setExpanded(false);
    }

    @Override
    public List<GuiElement> getSerializableChildren() {
        return List.of();
    }

    @Override
    public CompoundTag serializeState() {
        CompoundTag tag = super.serializeState();
        tag.putInt("selectedIndex", selectedIndex);
        return tag;
    }

    @Override
    public void deserializeState(CompoundTag tag) {
        super.deserializeState(tag);
        if (tag.contains("selectedIndex")) {
            setSelectedIndex(NbtCompat.getInt(tag, "selectedIndex"));
        }
    }

    @Override
    protected void render() {

    }

    @Override
    protected void layoutChanged() {
        int width = getWidth();
        int height = getHeight();

        if(!isExpanded) {
            unexpandedHeight = height;
        }




        customLabelElement.setBounds(0, 0, width-unexpandedHeight, unexpandedHeight);
        button.setBounds(customLabelElement.getRight(), 0, unexpandedHeight, unexpandedHeight);
        arrowTextureDown.setBounds(0,0,button.getHeight(), button.getHeight());
        arrowTextureUp.setBounds(0,0,button.getHeight(), button.getHeight());

        if(isExpanded) {
            this.setHeight(button.getHeight() + expandedHeight);
            listView.setPosition(0, button.getBottom());
            listView.setWidth(width);
            listView.setHeight(expandedHeight);
        }
    }

    private void onExpandButtonClicked()
    {
        setExpanded(!isExpanded);
    }

    /**
     * Removes all options from the menu.
     */
    public void clearOptions()
    {
        removeChilds();
    }

    /**
     * Adds a new option using a centered text label.
     * For custom option elements use {@link #addChild(GuiElement)}.
     * When this option is selected, its text is shown in the collapsed menu.
     * @param label the option's display text
     * @return the created option label, e.g. to attach a tooltip
     */
    public Label addOption(String label)
    {
        Label optionLabel = new Label(label);
        // Height must be set before addChild, which wraps the label in a button sized to this height
        optionLabel.setHeight(20);
        optionLabel.setAlignment(Alignment.CENTER);
        addChild(optionLabel);
        return optionLabel;
    }

    @Override
    public void addChild(GuiElement el)
    {
        if(el == null)
            return;
        EmptyButtonChanged optionButton = new EmptyButtonChanged();
        Layout layout = new LayoutHorizontal();
        layout.stretchX = true;
        optionButton.setLayout(layout);
        optionButton.setHeight(el.getHeight());
        optionButton.setOnFallingEdge(() -> onOptionClicked(optionButton));
        optionButton.addChild(el);
        listView.addChild(optionButton);
        if(isExpanded)
            expandedHeight = Math.min(listView.getSizeHintHeight(), maxExpandedHeight);
    }
    @Override
    public void removeChild(GuiElement el)
    {
        // Find the button that contains the element and remove it
        for(GuiElement child : listView.getChilds())
        {
            if(child instanceof EmptyButtonChanged button && button.getChilds().contains(el))
            {
                listView.removeChild(button);
                if(isExpanded)
                    expandedHeight = Math.min(listView.getSizeHintHeight(), maxExpandedHeight);
                return;
            }
        }
    }

    @Override
    public void removeChilds()
    {
        listView.removeChilds();
        if(isExpanded)
            expandedHeight = Math.min(listView.getSizeHintHeight(), maxExpandedHeight);
    }
    @Override
    public List<GuiElement> getChilds()
    {
        // Return the list of buttons in the dropdown menu
        List<GuiElement> childElements = new ArrayList<>();
        for(GuiElement child : listView.getChilds())
        {
            if(child instanceof EmptyButtonChanged button)
            {
                List<GuiElement> childs = button.getChilds();
                if (childs.isEmpty()) {
                    continue;
                }
                GuiElement el = childs.get(0);
                if(el != null)
                {
                    childElements.add(el);
                }
            }
        }
        return childElements;
    }

    /**
     * Selects the option: label shows a {@link Label} option's text, the menu collapses, then the
     * callback runs, so it can still override the label ({@link #setLabelText}) or re-open ({@link #expand}).
     */
    private void onOptionClicked(GuiElement option)
    {
        int index = listView.getChilds().indexOf(option);
        List<GuiElement> optionChilds = option.getChilds();
        if(index == -1 || optionChilds.isEmpty())
            return;
        GuiElement el = optionChilds.get(0);
        if (selectedIndex != index) markDirty();
        selectedIndex = index;
        if(el instanceof Label optionLabel)
            setLabelText(optionLabel.getText());
        updateSelectionHighlight();
        collapse();
        if(optionSelected != null)
            optionSelected.accept(index, el);
    }

    private void updateSelectionHighlight()
    {
        List<GuiElement> rows = listView.getChilds();
        for(int i=0; i<rows.size(); i++)
            if(rows.get(i) instanceof EmptyButtonChanged row)
                row.setSelected(i == selectedIndex);
    }

    /**
     * Delivers the current click to the open option list if the mouse is over it.
     * Called by {@link Gui#mouseClicked(double, double, int)} before the regular dispatch, so an
     * element underneath the open list never receives the click. Not meant to be called directly.
     * @param button the mouse button
     * @return {@code true} if the mouse is over the open list (the click is consumed)
     */
    public boolean clickOpenList(int button)
    {
        if(!isExpanded || !listView.isMouseOver())
            return false;
        listView.mouseClickedInternal(button, true);
        return true;
    }
    /**
     * @return the index of the currently selected option, or {@code -1} if no option is selected
     */
    public int getSelectedIndex() { return selectedIndex;}
    /**
     * Programmatically selects the option at the given index, as if the user clicked it:
     * updates the label for text options, collapses the menu and fires the selection callback.
     * Out-of-range indices are silently ignored.
     * @param index the option index to select
     */
    public void setSelectedIndex(int index) {
        if (index < 0 || index >= listView.getChilds().size())
            return;
        GuiElement option = listView.getChilds().get(index);
        onOptionClicked(option);
    }

    @Override
    protected void mouseClicked(int button) {
        if(!isMouseOverIgnoreParents() && isExpanded)
            collapse();
    }

    /** {@code true} if no ListView encloses {@code el}, else whether the point is visible in the nearest one. */
    private static boolean isInsideEnclosingListView(GuiElement el, int globalPosX, int globalPosY) {
        for(GuiElement p = el.getParent(); p != null; p = p.getParent())
            if(p instanceof ListView enclosing)
                return enclosing.isOver(globalPosX, globalPosY);
        return true;
    }
}
