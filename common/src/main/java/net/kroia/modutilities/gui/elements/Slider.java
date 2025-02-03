package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.minecraft.sounds.SoundEvents;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class Slider extends GuiElement {


    protected boolean isPressed = false;
    protected boolean isMovable = true;
    protected int triggerButton = 0;
    protected int sliderLineColor = DEFAULT_OUTLINE_COLOR;
    protected int colorIdle = DEFAULT_BACKGROUND_COLOR;
    protected int colorHover = DEFAULT_HOVER_BACKGROUND_COLOR;
    protected int colorPressed = DEFAULT_FOCUSED_BACKGROUND_COLOR;
    protected int sliderOutlineColor = DEFAULT_OUTLINE_COLOR;
    protected double sliderValue = 0.0;

    protected final Rectangle sliderBounds;
    private final Point relativeMousePos = new Point(0,0);

    Consumer<Double> onValueChanged = null;
    Supplier<String> tooltipSupplier = null;

    public Slider()
    {
        super();
        sliderBounds = new Rectangle(0,0,0,0);
    }
    public Slider(int x, int y, int width, int height) {
        super(x, y, width, height);
        sliderBounds = new Rectangle(0,0,0,0);
    }



    public void setOnValueChanged(Consumer<Double> onValueChanged) {
        this.onValueChanged = onValueChanged;
    }



    @Override
    protected void render() {
        // Slider
        int color = colorIdle;
        boolean contains = sliderBounds.contains(getMouseX(), getMouseY());
        if(isPressed)
            color = colorPressed;
        else if(contains) {
            color = colorHover;
        }
        if(contains && tooltipSupplier != null)
            drawTooltipLater(tooltipSupplier.get(), getMouseX(), getMouseY()-getFont().lineHeight);

        drawRect(sliderBounds.x, sliderBounds.y, sliderBounds.width, sliderBounds.height, sliderOutlineColor);
        drawRect(sliderBounds.x+1, sliderBounds.y+1, sliderBounds.width-2, sliderBounds.height-2, color);
    }

    public void setSliderValue(double value) {
        if(value < 0.0)
            value = 0.0;
        if(value > 1.0)
            value = 1.0;
        this.sliderValue = value;
    }
    public double getSliderValue() {
        return sliderValue;
    }

    public void setTooltipSupplier(Supplier<String> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
    }
    public void setSliderLineColor(int color) {
        this.sliderLineColor = color;
    }
    public int getSliderLineColor() {
        return sliderLineColor;
    }

    public void setIdleColor(int color) {
        this.colorIdle = color;
    }
    public int getIdleColor() {
        return colorIdle;
    }
    public void setSliderOutlineColor(int color) {
        this.sliderOutlineColor = color;
    }
    public int getSliderOutlineColor() {
        return sliderOutlineColor;
    }

    public void setHoverColor(int color)
    {
        this.colorHover = color;
    }
    public void setPressedColor(int color)
    {
        this.colorPressed = color;
    }
    public int getHoverColor()
    {
        return this.colorHover;
    }
    public int getPressedColor()
    {
        return this.colorPressed;
    }

    public void setMovable(boolean movable) {
        isMovable = movable;
    }
    public boolean isMovable() {
        return isMovable;
    }
    public void setTriggerButton(int triggerButton) {
        this.triggerButton = triggerButton;
    }
    public int getTriggerButton() {
        return triggerButton;
    }


    abstract protected void sliderMovedToPos(int x, int y);

    @Override
    protected boolean mouseClickedOverElement(int button)
    {
        if(!isMovable || triggerButton != button)
            return false;

        if(sliderBounds.contains(getMouseX(), getMouseY())) {
            if (!isPressed) {
                playLocalSound(SoundEvents.UI_BUTTON_CLICK, 0.5F);
                relativeMousePos.x = getMouseX() - sliderBounds.x;
                relativeMousePos.y = getMouseY() - sliderBounds.y;
            }
            isPressed = true;
        }
        return true;
    }

    @Override
    protected boolean mouseDragged(int button, double deltaX, double deltaY)
    {
        if(!isMovable || triggerButton != button)
            return false;

        if(isPressed)
        {
            int x = getMouseX();
            int y = getMouseY();
            sliderMovedToPos(x-relativeMousePos.x, y-relativeMousePos.y);
            if(onValueChanged != null)
                onValueChanged.accept(sliderValue);
            return true;
        }
        return false;
    }

    @Override
    protected void mouseReleased(int button)
    {
        if(!isMovable || triggerButton != button)
            return;
        isPressed = false;
    }
}
