package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.kroia.modutilities.gui.geometry.Rectangle;

public class Label extends GuiElement {

    public static final int DEFAULT_HEIGHT = 15;
    private Alignment alignment = Alignment.LEFT;
    private String text;
    private int padding = GuiElement.DEFAULT_PADDING;
    private int textColor = GuiElement.DEFAULT_TEXT_COLOR;
    private Point textPos = new Point(0,0);
    public Label()
    {
        super(0,0,100,DEFAULT_HEIGHT);
        text = "";
    }
    public Label(String text)
    {
        super(0,0,100,DEFAULT_HEIGHT);
        this.text = text;
    }

    public void setText(String text)
    {
        this.text = text;
        layoutChangedInternal();
    }
    public String getText(){
        return text;
    }

    public void setAlignment(Alignment alignment)
    {
        this.alignment = alignment;
        layoutChangedInternal();
    }
    public Alignment getAlignment()
    {
        return alignment;
    }
    public void setPadding(int padding)
    {
        this.padding = padding;
    }
    public int getPadding()
    {
        return padding;
    }
    public void setTextColor(int textColor)
    {
        this.textColor = textColor;
    }
    public int getTextColor()
    {
        return textColor;
    }


    @Override
    public void renderBackground() {

    }

    @Override
    public void render() {
        drawText(text, textPos, textColor);
    }

    @Override
    public void layoutChanged() {
        int textHeight = getFont().lineHeight;
        int textWidth = getFont().width(text);
        int x = padding;
        int y = padding;
        int width = getWidth() - padding*2;
        int height = getHeight()-padding*2;

        Rectangle bounds = new Rectangle(0,0, textWidth, textHeight);
        bounds = getAlignedBounds(bounds, alignment, x, y, width, height);

        textPos.x = bounds.x;
        textPos.y = bounds.y;
    }
}
