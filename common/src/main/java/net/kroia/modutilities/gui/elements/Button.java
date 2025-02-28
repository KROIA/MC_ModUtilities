package net.kroia.modutilities.gui.elements;


import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.sounds.SoundEvents;

public class Button extends GuiElement {

    Label label;
    boolean isPressed = false;
    int colorIdle = DEFAULT_BACKGROUND_COLOR;
    int colorHover = DEFAULT_HOVER_BACKGROUND_COLOR;
    int colorPressed = DEFAULT_FOCUSED_BACKGROUND_COLOR;

    Runnable onFallingEdge = null;
    Runnable onRisingEdge = null;
    Runnable onDown = null;
    boolean isClickable = true;
    int triggerButton = 0;

    public Button(String text) {
        super();
        label = new Label(text);
        label.setAlignment(Alignment.CENTER);
        addChild(label);
    }
    public Button(int x, int y, int width, int height, String text) {
        super(x, y, width, height);
        label = new Label(text);
        label.setBounds(0,0,width,height);
        label.setAlignment(Alignment.CENTER);
        addChild(label);
    }
    public Button(String text, Runnable onFallingEdge) {
        this(text);
        this.onFallingEdge = onFallingEdge;
    }
    public Button(int x, int y, int width, int height, String text, Runnable onFallingEdge) {
        this(x, y, width, height, text);
        this.onFallingEdge = onFallingEdge;
    }
    public void setLabel(String text)
    {
        label.setText(text);
    }

    public void setLayoutType(Alignment layoutType)
    {
        label.setAlignment(layoutType);
    }
    public Alignment getLayoutType()
    {
        return label.getAlignment();
    }

    public void setOnFallingEdge(Runnable onFallingEdge)
    {
        this.onFallingEdge = onFallingEdge;
    }
    public void setOnRisingEdge(Runnable onRisingEdge)
    {
        this.onRisingEdge = onRisingEdge;
    }
    public void setOnDown(Runnable onDown)
    {
        this.onDown = onDown;
    }

    public void setIdleColor(int color)
    {
        this.colorIdle = color;
    }
    public void setHoverColor(int color)
    {
        this.colorHover = color;
    }
    public void setPressedColor(int color)
    {
        this.colorPressed = color;
    }
    public int getIdleColor()
    {
        return this.colorIdle;
    }
    public int getHoverColor()
    {
        return this.colorHover;
    }
    public int getPressedColor()
    {
        return this.colorPressed;
    }
    public boolean isClickable()
    {
        return isClickable;
    }
    public void setClickable(boolean clickable)
    {
        isClickable = clickable;
    }
    public void setTriggerButton(int button)
    {
        triggerButton = button;
    }
    public int getTriggerButton()
    {
        return triggerButton;
    }
    public boolean isPressed()
    {
        return isPressed;
    }

    @Override
    protected void renderBackground() {
        int color = colorIdle;
        if(isPressed)
            color = colorPressed;
        else if(isMouseOver())
            color = colorHover;
        drawRect(0,0,getWidth(), getHeight(),color);
        if(enableOutline)
            renderOutline();
    }

    @Override
    protected void render() {

    }

    @Override
    protected void layoutChanged() {
        label.setBounds(0,0,getWidth(),getHeight());
    }

    @Override
    protected boolean mouseClickedOverElement(int button)
    {
        if(!isClickable || triggerButton != button)
            return false;

        if(!isPressed) {
            playLocalSound(SoundEvents.UI_BUTTON_CLICK.value(),0.5F);
            if(onFallingEdge != null) {
                onFallingEdge.run();
            }
        }
        isPressed = true;
        return true;
    }

    @Override
    protected boolean mouseDragged(int button, double deltaX, double deltaY)
    {
        if(!isClickable || triggerButton != button)
            return false;
        if(isPressed)
        {
            if(onDown != null) {
                onDown.run();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void mouseReleased(int button)
    {
        if(!isClickable || triggerButton != button)
            return;
        if(isPressed){
            if(onRisingEdge != null)
                onRisingEdge.run();
        }
        isPressed = false;
    }
}
