package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.elements.base.ListView;
import net.kroia.modutilities.gui.layout.Layout;
import net.kroia.modutilities.gui.layout.LayoutHorizontal;
import net.kroia.modutilities.gui.layout.LayoutVertical;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class DropDownMenu extends GuiElement {


    private final ListView listView;

    private final Label label;
    private final Button button;

    private int maxExpandedHeight = 100; // Height when expanded, 0 means collapsed
    private int unexpandedHeight = 0;
    private int expandedHeight = 0;
    private boolean isExpanded = false;

    private BiConsumer<Integer, GuiElement> optionSelected;
    public DropDownMenu(String label, BiConsumer<Integer, GuiElement> optionSelected) {
        super();
        this.optionSelected = optionSelected;

        this.label = new Label(label);
        this.label.setHeight(20);
        this.label.setAlignment(Alignment.CENTER);
        super.addChild(this.label);

        button = new Button("v", this::onExpandButtonClicked);
        button.setHeight(20);

        listView = new VerticalListView();
        Layout layout = new LayoutVertical();
        layout.stretchX = true;
        listView.setLayout(layout);
        listView.setEnabled(false);
        listView.setCheckOverlapForRendering(false);

        super.addChild(button);
        super.addChild(listView);
    }
    public DropDownMenu(String label)
    {
        this(label, null);
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
    public boolean isExpanded() {
        return isExpanded;
    }
    public void setExpanded(boolean expanded) {
        if(expanded == isExpanded)
            return;
        isExpanded = expanded;
        if(isExpanded)
        {
            expandedHeight = Math.min(listView.getSizeHintHeight(), maxExpandedHeight);
            unexpandedHeight = getHeight();
            this.setHeight(button.getHeight() + expandedHeight);
            //listView.setHeight(expandedHeight);
            listView.setEnabled(true);
            button.setLabel("^");
        }
        else
        {
            this.setHeight(unexpandedHeight);
            listView.setEnabled(false);
            button.setLabel("v");
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




        label.setBounds(0, 0, width-unexpandedHeight, unexpandedHeight);
        button.setBounds(label.getRight(), 0, unexpandedHeight, unexpandedHeight);

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
        EmptyButton optionButton = new EmptyButton();
        Layout layout = new LayoutHorizontal();
        layout.stretchX = true;
        optionButton.setLayout(layout);
        optionButton.setHeight(el.getHeight());
        optionButton.setOnFallingEdge(() -> onOptionClicked(optionButton));
        optionButton.addChild(el);
        listView.addChild(optionButton);
    }
    @Override
    public void removeChild(GuiElement el)
    {
        // Find the button that contains the element and remove it
        for(GuiElement child : listView.getChilds())
        {
            if(child instanceof Button button && button.getChilds().contains(el))
            {
                listView.removeChild(button);
                return;
            }
        }
    }

    @Override
    public void removeChilds()
    {
        listView.removeChilds();
    }
    @Override
    public List<GuiElement> getChilds()
    {
        // Return the list of buttons in the dropdown menu
        List<GuiElement> childElements = new ArrayList<>();
        for(GuiElement child : listView.getChilds())
        {
            if(child instanceof Button button)
            {
                List<GuiElement> childs = button.getChilds();
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
            GuiElement el = childs_.get(0);
            optionSelected.accept(index, el);
        }
    }
}
