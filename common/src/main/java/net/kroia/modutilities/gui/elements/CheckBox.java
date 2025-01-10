package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.minecraft.sounds.SoundEvents;

public class CheckBox extends GuiElement {

    Label label;
    Runnable onClick;
    Runnable onStateChanged;
    Runnable onChecked;
    Runnable onUnchecked;
    boolean isChecked = false;
    boolean isCheckable = true;
    int colorHover = DEFAULT_HOVER_BACKGROUND_COLOR;
    Rectangle hitboxRect;
    final Rectangle checkBoxRect = new Rectangle(0,0,0,0);
    final Rectangle checkBoxCheckedRect = new Rectangle(0,0,0,0);
    int triggerButton = 0;
    int checkBoxFrameColor = 0xff000000;
    int checkBoxCheckedColor = 0xff000000;
    public CheckBox(String text) {
        super();
        label = new Label(text);
        addChild(label);
        hitboxRect = checkBoxRect;
    }
    public CheckBox(int x, int y, int width, int height, String text) {
        super(x, y, width, height);
        label = new Label(text);
        addChild(label);
        hitboxRect = checkBoxRect;
    }
    public CheckBox(String text, Runnable onStateChanged) {
        super();
        label = new Label(text);
        addChild(label);
        this.onStateChanged = onStateChanged;
        hitboxRect = checkBoxRect;
    }
    public CheckBox(int x, int y, int width, int height, String text, Runnable onStateChanged) {
        super(x, y, width, height);
        label = new Label(text);
        addChild(label);
        this.onStateChanged = onStateChanged;
        hitboxRect = checkBoxRect;
    }
    public void setTextAlignment(Alignment alignment)
    {
        label.setAlignment(alignment);
    }
    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }
    public void setOnStateChanged(Runnable onStateChanged) {
        this.onStateChanged = onStateChanged;
    }
    public void setOnChecked(Runnable onChecked) {
        this.onChecked = onChecked;
    }
    public void setOnUnchecked(Runnable onUnchecked) {
        this.onUnchecked = onUnchecked;
    }
    public boolean isChecked() {
        return isChecked;
    }
    public void setChecked(boolean checked)
    {
        isChecked = checked;
    }
    public void setCheckable(boolean checkable)
    {
        isCheckable = checkable;
    }
    public boolean isCheckable()
    {
        return isCheckable;
    }
    public void setTriggerButton(int triggerButton)
    {
        this.triggerButton = triggerButton;
    }
    public int getTriggerButton()
    {
        return triggerButton;
    }
    public void setHoverColor(int color)
    {
        this.colorHover = color;
    }
    public int getCheckBoxFrameColor()
    {
        return checkBoxFrameColor;
    }
    public void setCheckBoxFrameColor(int color)
    {
        checkBoxFrameColor = color;
    }
    public int getCheckBoxCheckedColor()
    {
        return checkBoxCheckedColor;
    }
    public void setCheckBoxCheckedColor(int color)
    {
        checkBoxCheckedColor = color;
    }

    @Override
    protected void renderBackground() {
        if(hitboxRect.contains(getMouseX(),getMouseY()))
        {
            drawRect(checkBoxRect.x, checkBoxRect.y, checkBoxRect.width, checkBoxRect.height, colorHover);
        }
        drawFrame(checkBoxRect.x, checkBoxRect.y, checkBoxRect.width, checkBoxRect.height, checkBoxFrameColor, 1);
    }

    @Override
    protected void render() {
        if(isChecked)
        {
            drawRect(checkBoxCheckedRect.x, checkBoxCheckedRect.y, checkBoxCheckedRect.width, checkBoxCheckedRect.height, checkBoxCheckedColor);
        }
        if(!isCheckable)
        {
            drawRect(checkBoxRect.x, checkBoxRect.y, checkBoxRect.width, checkBoxRect.height, 0x88000000);
        }
    }

    @Override
    protected void layoutChanged() {
        int padding = 2;
        int height = getHeight()-padding*2;
        int width = getWidth()-padding*2;
        checkBoxRect.x = getWidth()-height-padding;
        checkBoxRect.y = padding;
        checkBoxRect.width = height;
        checkBoxRect.height = height;

        int checkedSize = height/2;
        checkBoxCheckedRect.x = checkBoxRect.x + (checkBoxRect.width-checkedSize)/2;
        checkBoxCheckedRect.y = checkBoxRect.y + (checkBoxRect.height-checkedSize)/2;
        checkBoxCheckedRect.width = checkedSize;
        checkBoxCheckedRect.height = checkedSize;



        label.setBounds(padding, padding, getWidth()-height-padding, height);

    }


    @Override
    protected boolean mouseClickedOverElement(int buttton)
    {
        if(!isCheckable || buttton != triggerButton)
            return false;

        if(hitboxRect.contains(getMouseX(),getMouseY()))
        {
            isChecked = !isChecked;
            playLocalSound(SoundEvents.UI_BUTTON_CLICK.value(),0.5F);
            if(onStateChanged != null)
            {
                onStateChanged.run();
            }
            if(isChecked && onChecked != null)
            {
                onChecked.run();
            }
            if(!isChecked && onUnchecked != null)
            {
                onUnchecked.run();
            }
            return true;
        }
        return false;
    }
}
