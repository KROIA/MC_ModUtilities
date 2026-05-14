package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.ListView;
import net.kroia.modutilities.gui.layout.Layout;
import net.kroia.modutilities.gui.layout.LayoutHorizontal;
import net.kroia.modutilities.gui.layout.LayoutVertical;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;

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
 */
public class DropDownMenu extends GuiElement {

    private class EmptyButtonChanged extends EmptyButton {
        private final DropDownMenu parent;
        public EmptyButtonChanged(DropDownMenu parent) {
            super();
            this.parent = parent;
        }

        @Override
        protected boolean mouseClickedOverElement(int button)
        {
            return false;
        }
        @Override
        protected void mouseClicked(int button) {
            if(!isMouseOverIgnoreParents())
                return;
            if(!isClickable || triggerButton != button)
                return;

            if(!isPressed) {
                playLocalSound(SoundEvents.UI_BUTTON_CLICK.value(),0.5F);
                if(onFallingEdge != null) {
                    onFallingEdge.run();
                }
            }
            isPressed = true;
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


        listView = new VerticalListView();
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
     * @param expanded {@code true} to expand, {@code false} to collapse
     */
    public void setExpanded(boolean expanded) {
        if(expanded == isExpanded)
            return;
        isExpanded = expanded;
        if(isExpanded)
        {
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
            setSelectedIndex(tag.getInt("selectedIndex"));
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
     * @param label the option's display text
     */
    public void addOption(String label)
    {
        Label optionLabel = new Label(label);
        // Height must be set before addChild, which wraps the label in a button sized to this height
        optionLabel.setHeight(20);
        optionLabel.setAlignment(Alignment.CENTER);
        addChild(optionLabel);
    }

    @Override
    public void addChild(GuiElement el)
    {
        if(el == null)
            return;
        EmptyButtonChanged optionButton = new EmptyButtonChanged(this);
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

    private void onOptionClicked(GuiElement option)
    {
        if(optionSelected == null)
            return;
        int index = -1;
        List<GuiElement> childs = listView.getChilds();
        for(int i=0; i<childs.size(); i++)
        {
            GuiElement child = childs.get(i);
            if(child == option)
            {
                index = i;
                break;
            }
        }
        if(index != -1)
        {
            // Notify listeners about the selection
            List<GuiElement> childs_ = option.getChilds();
            if (childs_.isEmpty()) {
                return;
            }
            GuiElement el = childs_.get(0);
            int oldIndex = selectedIndex;
            selectedIndex = index;
            if (oldIndex != selectedIndex) markDirty();
            optionSelected.accept(index, el);
        }
    }
    /**
     * @return the index of the currently selected option, or {@code -1} if no option is selected
     */
    public int getSelectedIndex() { return selectedIndex;}
    /**
     * Programmatically selects the option at the given index, firing the selection callback.
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
}
