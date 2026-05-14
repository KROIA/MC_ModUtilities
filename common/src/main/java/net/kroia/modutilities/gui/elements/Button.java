package net.kroia.modutilities.gui.elements;


import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * A standard clickable button rendered with a centered text {@link Label}.
 * Extends {@link EmptyButton}, inheriting its callback-based input handling
 * (falling edge = press, rising edge = release, down = held).
 */
public class Button extends EmptyButton {

    Label label;

    /**
     * Creates a button with default size and the given label text.
     * @param text the button label
     */
    public Button(String text) {
        super(0,0,10,20);
        label = new Label(text);
        addChild(label);
        label.setAlignment(Alignment.CENTER);
    }
    /**
     * Creates a button at the given position and size with the specified label text.
     * @param x the x-coordinate (relative to the parent)
     * @param y the y-coordinate (relative to the parent)
     * @param width the width
     * @param height the height
     * @param text the button label
     */
    public Button(int x, int y, int width, int height, String text) {
        super(x, y, width, height);
        label = new Label(text);
        label.setBounds(0,0,width,height);
        label.setAlignment(Alignment.CENTER);
        addChild(label);
    }
    /**
     * Creates a button with default size, a label, and a press callback.
     * @param text the button label
     * @param onFallingEdge runnable executed when the button is pressed (mouse-down)
     */
    public Button(String text, Runnable onFallingEdge) {
        super(onFallingEdge);
        label = new Label(text);
        label.setAlignment(Alignment.CENTER);
        addChild(label);
    }
    /**
     * Creates a button at the given position/size with a label and a press callback.
     * @param x the x-coordinate (relative to the parent)
     * @param y the y-coordinate (relative to the parent)
     * @param width the width
     * @param height the height
     * @param text the button label
     * @param onFallingEdge runnable executed when the button is pressed (mouse-down)
     */
    public Button(int x, int y, int width, int height, String text, Runnable onFallingEdge) {
        super(x, y, width, height, onFallingEdge);
        label = new Label(text);
        label.setBounds(0,0,width,height);
        label.setAlignment(Alignment.CENTER);
        addChild(label);

    }
    /**
     * Sets the label text shown on the button.
     * @param text the new label text
     */
    public void setLabel(String text)
    {
        label.setText(text);
    }
    /**
     * Sets the label text shown on the button.
     * Equivalent to {@link #setLabel(String)}.
     * @param text the new label text
     */
    public void setText(String text)
    {
        label.setText(text);
    }
    /**
     * @return the current label text
     */
    public String getText()
    {
        return label.getText();
    }

    /**
     * Sets the alignment of the label inside the button bounds.
     * @param layoutType the alignment (e.g. {@link Alignment#CENTER})
     */
    public void setLayoutType(Alignment layoutType)
    {
        label.setAlignment(layoutType);
    }
    /**
     * @return the alignment of the label inside the button bounds
     */
    public Alignment getLayoutType()
    {
        return label.getAlignment();
    }


    @Override
    public List<GuiElement> getSerializableChildren() {
        return List.of();
    }

    @Override
    public CompoundTag serializeState() {
        CompoundTag tag = super.serializeState();
        tag.putString("label", label.getText());
        return tag;
    }

    @Override
    public void deserializeState(CompoundTag tag) {
        super.deserializeState(tag);
        if (tag.contains("label")) {
            label.setText(tag.getString("label"));
        }
    }

    @Override
    protected void render() {

    }

    @Override
    protected void layoutChanged() {
        label.setBounds(0,0,getWidth(),getHeight());
    }

    @Override
    public void setTextColor(int color) {
        label.setTextColor(color);
        super.setTextColor(color);
    }
    @Override
    public int getTextColor() {
        return label.getTextColor();
    }
    @Override
    public void setTextFontScale(float scale) {
        label.setTextFontScale(scale);
        super.setTextFontScale(scale);
    }
    @Override
    public float getTextFontScale() {
        return label.getTextFontScale();
    }
}
