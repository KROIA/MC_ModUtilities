package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.ColorUtilities;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class EmptyButton extends GuiElement {
    protected boolean isPressed = false;
    protected int colorHover = DEFAULT_HOVER_BACKGROUND_COLOR;
    protected int colorPressed = DEFAULT_FOCUSED_BACKGROUND_COLOR;

    protected Runnable onFallingEdge = null;
    protected Runnable onRisingEdge = null;
    protected Runnable onDown = null;
    protected boolean isClickable = true;
    protected int triggerButton = GLFW.GLFW_MOUSE_BUTTON_LEFT;

    public EmptyButton() {
        super();
        int defaultColor = ColorUtilities.setBrightness(DEFAULT_BACKGROUND_COLOR, 0.8f);
        setBackgroundColor(defaultColor);
        setHoverColor(ColorUtilities.setBrightness(defaultColor, 0.8f));
        setPressedColor(ColorUtilities.setBrightness(defaultColor, 0.6f));
        setOutlineColor(ColorUtilities.setBrightness(defaultColor, 0.4f));
    }
    public EmptyButton(int x, int y, int width, int height) {
        super(x, y, width, height);
    }
    public EmptyButton(Runnable onFallingEdge) {
        this();
        this.onFallingEdge = onFallingEdge;
    }
    public EmptyButton(int x, int y, int width, int height, Runnable onFallingEdge) {
        this(x, y, width, height);
        this.onFallingEdge = onFallingEdge;
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

    /*public void setIdleColor(int color)
    {
        this.colorIdle = color;
    }*/
    public void setHoverColor(int color)
    {
        this.colorHover = color;
    }
    public void setPressedColor(int color)
    {
        this.colorPressed = color;
    }
    /*public int getIdleColor()
    {
        return this.colorIdle;
    }*/
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
        int color = super.getBackgroundColor();
        if(isClickable) {
            isPressed &= isMouseButtonDown(triggerButton);
            if (isPressed)
            {
                if(onDown != null) {
                    onDown.run();
                }
                color = colorPressed;
            }
            else if (isMouseOver())
                color = colorHover;
        }
        drawRect(0,0,getWidth(), getHeight(),color);
        if(enableOutline)
            renderOutline();
    }

    @Override
    protected void render() {

    }

    @Override
    protected void layoutChanged() {

    }

    @Override
    protected boolean mouseClickedOverElement(int button)
    {
        if(!isClickable || triggerButton != button)
            return false;

        if(!isPressed) {
            playLocalSound(SoundEvents.UI_BUTTON_CLICK.value(),0.5F);
            isPressed = true;
            if(onFallingEdge != null) {
                onFallingEdge.run();
            }
        }
        return true;
    }
    @Override
    protected void mouseReleased(int button)
    {
        if(!isClickable || triggerButton != button)
            return;
        if(isPressed){
            if(onRisingEdge != null)
                onRisingEdge.run();
            isPressed = false;
        }
    }

    @Override
    public void setEnabled(boolean visible)
    {
        super.setEnabled(visible);
        isPressed = false;
    }
}