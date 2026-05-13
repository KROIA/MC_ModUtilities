package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;

import java.util.List;
import java.util.function.Consumer;

/**
 * A boolean toggle element that displays a clickable square checkbox next to a text label.
 * The user toggles the checked state by clicking inside the checkbox hitbox.
 * <p>
 * Several callbacks let callers react to state changes:
 * <ul>
 *     <li>{@link #setOnStateChanged(Consumer)} - fires on every toggle, with the new state</li>
 *     <li>{@link #setOnChecked(Runnable)} - fires only when the box becomes checked</li>
 *     <li>{@link #setOnUnchecked(Runnable)} - fires only when the box becomes unchecked</li>
 *     <li>{@link #setOnClick(Runnable)} - generic click callback</li>
 * </ul>
 */
public class CheckBox extends GuiElement {

    Label label;
    Runnable onClick;
    Consumer<Boolean> onStateChanged;
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
    /**
     * Creates a checkbox with default size and the given label text.
     * @param text the label text shown next to the checkbox
     */
    public CheckBox(String text) {
        super();
        label = new Label(text);
        addChild(label);
        hitboxRect = checkBoxRect;
    }
    /**
     * Creates a checkbox at the given position/size with the given label text.
     * @param x the x-coordinate (relative to the parent)
     * @param y the y-coordinate (relative to the parent)
     * @param width the width
     * @param height the height
     * @param text the label text shown next to the checkbox
     */
    public CheckBox(int x, int y, int width, int height, String text) {
        super(x, y, width, height);
        label = new Label(text);
        addChild(label);
        hitboxRect = checkBoxRect;
    }
    /**
     * Creates a checkbox with default size, a label, and a state-change callback.
     * @param text the label text shown next to the checkbox
     * @param onStateChanged callback invoked with the new boolean state on every toggle
     */
    public CheckBox(String text, Consumer<Boolean> onStateChanged) {
        super();
        label = new Label(text);
        addChild(label);
        this.onStateChanged = onStateChanged;
        hitboxRect = checkBoxRect;
    }
    /**
     * Creates a checkbox at the given position/size with a label and a state-change callback.
     * @param x the x-coordinate (relative to the parent)
     * @param y the y-coordinate (relative to the parent)
     * @param width the width
     * @param height the height
     * @param text the label text shown next to the checkbox
     * @param onStateChanged callback invoked with the new boolean state on every toggle
     */
    public CheckBox(int x, int y, int width, int height, String text, Consumer<Boolean> onStateChanged) {
        super(x, y, width, height);
        label = new Label(text);
        addChild(label);
        this.onStateChanged = onStateChanged;
        hitboxRect = checkBoxRect;
    }
    /**
     * Sets the alignment of the label text within its bounds.
     * @param alignment the alignment (e.g. {@link Alignment#LEFT})
     */
    public void setText(String text) {
        label.setText(text);
    }

    public String getText() {
        return label.getText();
    }

    public void setTextAlignment(Alignment alignment)
    {
        label.setAlignment(alignment);
    }
    /**
     * Sets a generic click callback fired whenever the checkbox is clicked.
     * @param onClick the runnable to invoke on click, or {@code null} to clear
     */
    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }
    /**
     * Sets a callback invoked on every state change with the new boolean value.
     * @param onStateChanged the consumer to invoke, or {@code null} to clear
     */
    public void setOnStateChanged(Consumer<Boolean> onStateChanged) {
        this.onStateChanged = onStateChanged;
    }
    /**
     * Sets a callback invoked only when the checkbox transitions to the checked state.
     * @param onChecked the runnable to invoke, or {@code null} to clear
     */
    public void setOnChecked(Runnable onChecked) {
        this.onChecked = onChecked;
    }
    /**
     * Sets a callback invoked only when the checkbox transitions to the unchecked state.
     * @param onUnchecked the runnable to invoke, or {@code null} to clear
     */
    public void setOnUnchecked(Runnable onUnchecked) {
        this.onUnchecked = onUnchecked;
    }
    /**
     * @return {@code true} if the checkbox is currently checked
     */
    public boolean isChecked() {
        return isChecked;
    }
    /**
     * Sets the checked state programmatically and fires state-change callbacks
     * if the value actually changed.
     * @param checked the new state
     */
    public void setChecked(boolean checked)
    {
        if(isChecked == checked)
            return;
        isChecked = checked;
        markDirty();
        if(onStateChanged != null)
        {
            onStateChanged.accept(isChecked);
        }
        if(isChecked && onChecked != null)
        {
            onChecked.run();
        }
        if(!isChecked && onUnchecked != null)
        {
            onUnchecked.run();
        }
    }

    @Override
    public List<GuiElement> getSerializableChildren() {
        return List.of();
    }

    @Override
    public SyncCategory getSyncCategory() { return SyncCategory.INPUT; }

    @Override
    public CompoundTag serializeState() {
        CompoundTag tag = super.serializeState();
        tag.putBoolean("checked", isChecked);
        tag.putString("label", label.getText());
        return tag;
    }

    @Override
    public void deserializeState(CompoundTag tag) {
        super.deserializeState(tag);
        if(tag.contains("checked"))
            setChecked(tag.getBoolean("checked"));
        if(tag.contains("label"))
            label.setText(tag.getString("label"));
    }
    /**
     * Sets whether the user can toggle the checkbox by clicking it.
     * When non-checkable, the checkbox is rendered with a darkened overlay.
     * @param checkable {@code true} to allow user interaction, {@code false} to lock the state
     */
    public void setCheckable(boolean checkable)
    {
        isCheckable = checkable;
    }
    /**
     * @return {@code true} if the user is allowed to toggle the checkbox
     */
    public boolean isCheckable()
    {
        return isCheckable;
    }
    /**
     * Sets which mouse button toggles the checkbox.
     * @param triggerButton the GLFW mouse button code (e.g. {@code GLFW_MOUSE_BUTTON_LEFT})
     */
    public void setTriggerButton(int triggerButton)
    {
        this.triggerButton = triggerButton;
    }
    /**
     * @return the GLFW mouse button code that toggles the checkbox
     */
    public int getTriggerButton()
    {
        return triggerButton;
    }
    /**
     * Sets the overlay color drawn over the checkbox when the mouse hovers over it.
     * @param color the ARGB color
     */
    public void setHoverColor(int color)
    {
        this.colorHover = color;
    }
    /**
     * @return the ARGB color of the checkbox frame (border)
     */
    public int getCheckBoxFrameColor()
    {
        return checkBoxFrameColor;
    }
    /**
     * Sets the color of the checkbox frame (border).
     * @param color the ARGB color
     */
    public void setCheckBoxFrameColor(int color)
    {
        checkBoxFrameColor = color;
    }
    /**
     * @return the ARGB color of the inner check mark drawn when the box is checked
     */
    public int getCheckBoxCheckedColor()
    {
        return checkBoxCheckedColor;
    }
    /**
     * Sets the color of the inner check mark drawn when the box is checked.
     * @param color the ARGB color
     */
    public void setCheckBoxCheckedColor(int color)
    {
        checkBoxCheckedColor = color;
    }

    @Override
    protected void renderBackground() {
        super.renderBackground();
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



        label.setBounds(padding, padding, getWidth()-height-padding*2-1, height);

    }


    @Override
    protected boolean mouseClickedOverElement(int buttton)
    {
        if(!isCheckable || buttton != triggerButton)
            return false;

        if(hitboxRect.contains(getMouseX(),getMouseY()))
        {
            setChecked(!isChecked);
            playLocalSound(SoundEvents.UI_BUTTON_CLICK.value(),0.5F);
            return true;
        }
        return false;
    }

    @Override
    public void setTextColor(int color) {
        label.setTextColor(color);
    }
    @Override
    public int getTextColor() {
        return label.getTextColor();
    }
    @Override
    public void setTextFontScale(float scale) {
        label.setTextFontScale(scale);
    }
    @Override
    public float getTextFontScale() {
        return label.getTextFontScale();
    }
}
