package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.ListView;
import net.kroia.modutilities.gui.layout.Layout;
import net.kroia.modutilities.gui.layout.LayoutHorizontal;
import net.kroia.modutilities.gui.layout.LayoutVertical;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

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
    public DropDownMenu(String label)
    {
        this(label, null);
    }


    @Override
    public void setZ(float z) {
        super.setZ(z);
        defaultZPos = z;
    }
    public void setExpandedZPos(float z) {
        this.expandedZPos = z;
    }
    public float getExpandedZPos() {
        return expandedZPos;
    }
    public void setLabelText(String text)
    {
        if(this.label != null)
            this.label.setText(text);
    }
    public String getLabelText()
    {
        if(this.label == null)
            return "";
        return this.label.getText();
    }
    public void setCustomLabelElement(GuiElement element)
    {
        if(element == null)
            return;
        if(customLabelElement != null)
            super.removeChild(customLabelElement);
        customLabelElement = element;
        super.addChild(customLabelElement);
    }

    public void setOnOptionSelected(BiConsumer<Integer, GuiElement> optionSelected)
    {
        this.optionSelected = optionSelected;
    }

    public void setMaxExpandedHeight(int maxExpandedHeight) {
        this.maxExpandedHeight = maxExpandedHeight;
    }
    public int getMaxExpandedHeight() {
        return maxExpandedHeight;
    }
    public int getExpandedHeight() {
        expandedHeight = Math.min(listView.getSizeHintHeight(), maxExpandedHeight);
        return expandedHeight;
    }
    public int getUnexpandedHeight() {
        return unexpandedHeight;
    }
    public boolean isExpanded() {
        return isExpanded;
    }
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
    public void expand()
    {
        setExpanded(true);
    }
    public void collapse()
    {
        setExpanded(false);
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

    public void clearOptions()
    {
        removeChilds();
    }

    public void addOption(String label)
    {
        Label optionLabel = new Label(label);
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
            selectedIndex = index;
            optionSelected.accept(index, el);
        }
    }
    public int getSelectedIndex() { return selectedIndex;}
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
