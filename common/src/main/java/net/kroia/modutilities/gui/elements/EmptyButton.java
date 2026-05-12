package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.ColorUtilities;
import net.kroia.modutilities.gui.InputConstants;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.sounds.SoundEvents;

/**
 * An invisible (or color-only) clickable region without label or icon.
 * Useful as a click overlay above other elements or as the base for {@link Button}.
 * <p>
 * Provides three lifecycle callbacks tied to the trigger mouse button:
 * <ul>
 *     <li>{@link #setOnFallingEdge(Runnable)} - fired once when the button is initially pressed</li>
 *     <li>{@link #setOnRisingEdge(Runnable)} - fired once when the button is released after a press</li>
 *     <li>{@link #setOnDown(Runnable)} - fired every render frame while the button is held down</li>
 * </ul>
 * @apiNote The "falling edge" / "rising edge" naming is inverted from the typical digital
 * signal-processing convention. Here, <b>falling edge</b> means the mouse press (button goes
 * from up to down) and <b>rising edge</b> means the mouse release (button goes from down to up).
 */
public class EmptyButton extends GuiElement {
    protected boolean isPressed = false;
    protected int colorHover = DEFAULT_HOVER_BACKGROUND_COLOR;
    protected int colorPressed = DEFAULT_FOCUSED_BACKGROUND_COLOR;

    protected Runnable onFallingEdge = null;
    protected Runnable onRisingEdge = null;
    protected Runnable onDown = null;
    protected boolean isClickable = true;
    protected int triggerButton = InputConstants.MOUSE_BUTTON_LEFT;

    /**
     * Creates an empty button with default size and no callbacks.
     */
    public EmptyButton() {
        super();
        initColors();
    }
    /**
     * Creates an empty button at the given position and size.
     * @param x the x-coordinate (relative to the parent)
     * @param y the y-coordinate (relative to the parent)
     * @param width the width
     * @param height the height
     */
    public EmptyButton(int x, int y, int width, int height) {
        super(x, y, width, height);
        initColors();
    }
    private void initColors() {
        int defaultColor = ColorUtilities.setBrightness(DEFAULT_BACKGROUND_COLOR, 0.8f);
        setBackgroundColor(defaultColor);
        setHoverColor(ColorUtilities.setBrightness(defaultColor, 0.8f));
        setPressedColor(ColorUtilities.setBrightness(defaultColor, 0.6f));
        setOutlineColor(ColorUtilities.setBrightness(defaultColor, 0.4f));
    }

    /**
     * Creates an empty button with default size and a press callback.
     * @param onFallingEdge runnable executed when the button is pressed (mouse down)
     */
    public EmptyButton(Runnable onFallingEdge) {
        this();
        this.onFallingEdge = onFallingEdge;
    }
    /**
     * Creates an empty button at the given position/size with a press callback.
     * @param x the x-coordinate (relative to the parent)
     * @param y the y-coordinate (relative to the parent)
     * @param width the width
     * @param height the height
     * @param onFallingEdge runnable executed when the button is pressed (mouse down)
     */
    public EmptyButton(int x, int y, int width, int height, Runnable onFallingEdge) {
        this(x, y, width, height);
        this.onFallingEdge = onFallingEdge;
    }


    /**
     * Sets the runnable that is executed when the button is pressed (mouse-down event).
     * @param onFallingEdge the runnable to invoke on press, or {@code null} to clear
     * @apiNote Despite the name, "falling edge" here refers to the press (mouse going down),
     * not the release. This is the inverse of the digital signal convention.
     */
    public void setOnFallingEdge(Runnable onFallingEdge)
    {
        this.onFallingEdge = onFallingEdge;
    }
    /**
     * Sets the runnable that is executed when the button is released after being pressed.
     * @param onRisingEdge the runnable to invoke on release, or {@code null} to clear
     * @apiNote Despite the name, "rising edge" here refers to the release (mouse going up),
     * not the press. This is the inverse of the digital signal convention.
     */
    public void setOnRisingEdge(Runnable onRisingEdge)
    {
        this.onRisingEdge = onRisingEdge;
    }
    /**
     * Sets the runnable that is executed every render frame while the button is held down.
     * @param onDown the runnable to invoke each frame while pressed, or {@code null} to clear
     */
    public void setOnDown(Runnable onDown)
    {
        this.onDown = onDown;
    }

    /*public void setIdleColor(int color)
    {
        this.colorIdle = color;
    }*/
    /**
     * Sets the background color shown when the mouse is hovering over the button (and not pressed).
     * @param color the ARGB color
     */
    public void setHoverColor(int color)
    {
        this.colorHover = color;
    }
    /**
     * Sets the background color shown while the button is pressed.
     * @param color the ARGB color
     */
    public void setPressedColor(int color)
    {
        this.colorPressed = color;
    }
    /*public int getIdleColor()
    {
        return this.colorIdle;
    }*/
    /**
     * @return the ARGB color shown when the mouse is hovering over the button
     */
    public int getHoverColor()
    {
        return this.colorHover;
    }
    /**
     * @return the ARGB color shown while the button is pressed
     */
    public int getPressedColor()
    {
        return this.colorPressed;
    }
    /**
     * @return {@code true} if the button currently reacts to mouse clicks
     */
    public boolean isClickable()
    {
        return isClickable;
    }
    /**
     * Sets whether the button should react to mouse clicks.
     * Disabled buttons still render but ignore input.
     * @param clickable {@code true} to enable clicks, {@code false} to disable
     */
    public void setClickable(boolean clickable)
    {
        isClickable = clickable;
    }
    /**
     * Sets which mouse button triggers the click callbacks.
     * @param button the mouse button code (e.g. {@link InputConstants#MOUSE_BUTTON_LEFT})
     */
    public void setTriggerButton(int button)
    {
        triggerButton = button;
    }
    /**
     * @return the mouse button code that triggers the click callbacks
     */
    public int getTriggerButton()
    {
        return triggerButton;
    }
    /**
     * @return {@code true} while the trigger mouse button is held down on this button
     */
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