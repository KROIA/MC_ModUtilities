package net.kroia.modutilities.gui.elements;


import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.sounds.SoundEvents;

public class Button extends EmptyButton {

    Label label;

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
        super(onFallingEdge);
        label = new Label(text);
        label.setAlignment(Alignment.CENTER);
        addChild(label);
    }
    public Button(int x, int y, int width, int height, String text, Runnable onFallingEdge) {
        super(x, y, width, height, onFallingEdge);
        label = new Label(text);
        label.setBounds(0,0,width,height);
        label.setAlignment(Alignment.CENTER);
        addChild(label);

    }
    public void setLabel(String text)
    {
        label.setText(text);
    }
    public void setText(String text)
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
