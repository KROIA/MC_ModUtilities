package net.kroia.modutilities.gui.elements.base;

import net.kroia.modutilities.gui.InputConstants;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Abstract base class for slider widgets in the GUI framework.
 * <p>
 * A slider exposes a normalized value in the range [0.0, 1.0] that can be
 * adjusted by dragging a handle ({@link #sliderBounds}) inside the element.
 * Concrete subclasses (e.g. horizontal/vertical sliders) implement
 * {@link #sliderMovedToPos(int, int)} to translate handle motion into a value
 * update and to lay the handle out within the slider's bounds.
 *
 * @apiNote The {@code gui/} package is client-only
 *          ({@code @Environment(EnvType.CLIENT)}); slider widgets must only be
 *          used on the client.
 */
public abstract class Slider extends GuiElement {


    protected boolean isPressed = false;
    protected boolean isMovable = true;
    protected int triggerButton = InputConstants.MOUSE_BUTTON_LEFT;
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

    /**
     * Creates a new slider with zero size at the origin.
     */
    public Slider()
    {
        super();
        sliderBounds = new Rectangle(0,0,0,0);
    }

    /**
     * Creates a new slider with the given bounds.
     *
     * @param x      the x position
     * @param y      the y position
     * @param width  the width of the slider
     * @param height the height of the slider
     */
    public Slider(int x, int y, int width, int height) {
        super(x, y, width, height);
        sliderBounds = new Rectangle(0,0,0,0);
    }



    /**
     * Sets a callback to be invoked whenever the slider's normalized value changes
     * due to user interaction. The new value is supplied to the consumer.
     *
     * @param onValueChanged the callback to invoke, or {@code null} to clear it
     */
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
            drawTooltip(tooltipSupplier.get(), getMouseX(), getMouseY()-getTextHeight());

        drawRect(sliderBounds.x, sliderBounds.y, sliderBounds.width, sliderBounds.height, sliderOutlineColor);
        drawRect(sliderBounds.x+1, sliderBounds.y+1, sliderBounds.width-2, sliderBounds.height-2, color);
    }

    /**
     * Sets the slider's normalized value, clamped to {@code [0.0, 1.0]}.
     * <p>
     * This setter does not invoke the value-changed callback; it is intended for
     * programmatic updates rather than mirroring user input.
     *
     * @param value the new value, clamped to the closed unit interval
     */
    public void setSliderValue(double value) {
        if(value < 0.0)
            value = 0.0;
        if(value > 1.0)
            value = 1.0;
        if(Math.abs(this.sliderValue - value) > 0.001)
            markDirty();
        this.sliderValue = value;
    }

    @Override
    public SyncCategory getSyncCategory() { return SyncCategory.INPUT; }

    @Override
    public CompoundTag serializeState() {
        CompoundTag tag = super.serializeState();
        tag.putDouble("value", sliderValue);
        return tag;
    }

    @Override
    public void deserializeState(CompoundTag tag) {
        super.deserializeState(tag);
        if(tag.contains("value"))
            setSliderValue(tag.getDouble("value"));
    }

    /**
     * @return the slider's current normalized value in the range {@code [0.0, 1.0]}
     */
    public double getSliderValue() {
        return sliderValue;
    }

    /**
     * Sets a supplier for the tooltip shown while the mouse hovers over the
     * slider handle.
     *
     * @param tooltipSupplier the supplier that yields the tooltip string, or
     *                        {@code null} to disable the hover tooltip
     */
    public void setTooltipSupplier(Supplier<String> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
    }

    /**
     * Sets the color of the slider track/line.
     *
     * @param color the packed ARGB color
     */
    public void setSliderLineColor(int color) {
        this.sliderLineColor = color;
    }

    /**
     * @return the packed ARGB color used for the slider track/line
     */
    public int getSliderLineColor() {
        return sliderLineColor;
    }

    /**
     * Sets the color of the slider handle when it is in its idle (not hovered,
     * not pressed) state.
     *
     * @param color the packed ARGB color
     */
    public void setIdleColor(int color) {
        this.colorIdle = color;
    }

    /**
     * @return the packed ARGB color used for the idle handle
     */
    public int getIdleColor() {
        return colorIdle;
    }

    /**
     * Sets the color of the outline drawn around the slider handle.
     *
     * @param color the packed ARGB color
     */
    public void setSliderOutlineColor(int color) {
        this.sliderOutlineColor = color;
    }

    /**
     * @return the packed ARGB color used for the slider handle outline
     */
    public int getSliderOutlineColor() {
        return sliderOutlineColor;
    }

    /**
     * Sets the color of the slider handle when the mouse hovers over it.
     *
     * @param color the packed ARGB color
     */
    public void setHoverColor(int color)
    {
        this.colorHover = color;
    }

    /**
     * Sets the color of the slider handle while it is being pressed/dragged.
     *
     * @param color the packed ARGB color
     */
    public void setPressedColor(int color)
    {
        this.colorPressed = color;
    }

    /**
     * @return the packed ARGB color used for the hovered handle
     */
    public int getHoverColor()
    {
        return this.colorHover;
    }

    /**
     * @return the packed ARGB color used for the pressed handle
     */
    public int getPressedColor()
    {
        return this.colorPressed;
    }

    /**
     * Sets whether the slider handle can currently be moved by the user.
     * Disabling motion still allows the slider to be rendered, but mouse input
     * does not affect its value.
     *
     * @param movable {@code true} to enable user interaction, {@code false} to
     *                disable it
     */
    public void setMovable(boolean movable) {
        isMovable = movable;
    }

    /**
     * @return {@code true} if the user can drag the slider handle
     */
    public boolean isMovable() {
        return isMovable;
    }

    /**
     * Sets the mouse button that initiates a drag interaction.
     *
     * @param triggerButton the mouse button code (e.g.
     *                      {@link InputConstants#MOUSE_BUTTON_LEFT})
     */
    public void setTriggerButton(int triggerButton) {
        this.triggerButton = triggerButton;
    }

    /**
     * @return the mouse button code that triggers slider dragging
     */
    public int getTriggerButton() {
        return triggerButton;
    }


    /**
     * Called by the framework when the mouse is dragged while pressing the slider
     * handle. Implementations should update {@link #sliderValue} and any internal
     * layout based on the requested handle position.
     *
     * @param x the desired new handle x position, in the slider's local coordinates
     * @param y the desired new handle y position, in the slider's local coordinates
     */
    abstract protected void sliderMovedToPos(int x, int y);

    @Override
    protected boolean mouseClickedOverElement(int button)
    {
        if(!isMovable || triggerButton != button)
            return false;

        if(sliderBounds.contains(getMouseX(), getMouseY())) {
            if (!isPressed) {
                playLocalSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F);
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
