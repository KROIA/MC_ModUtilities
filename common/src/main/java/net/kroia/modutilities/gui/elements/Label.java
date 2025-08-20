package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.kroia.modutilities.gui.geometry.Rectangle;

import java.util.Objects;

public class Label extends GuiElement {

    public static final int DEFAULT_HEIGHT = 15;
    private Alignment alignment = Alignment.LEFT;
    private String text;
    private int padding = GuiElement.DEFAULT_PADDING;
    private Point textPos = new Point(0,0);
    public Label()
    {
        this("");
    }
    public Label(String text)
    {
        super(0,0,100,DEFAULT_HEIGHT);
        this.text = Objects.requireNonNullElse(text, "");
        setEnableBackground(false);
        setEnableOutline(false);
    }

    public void setText(String text)
    {
        this.text = Objects.requireNonNullElse(text, "");
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

    /*@Override
    public void renderBackground() {

    }*/

    @Override
    public void render() {
        drawText(text, textPos);
    }
    @Override
    protected void renderGizmos()
    {
        super.renderGizmos();
        if(isMouseOver() && !text.isEmpty())
        {
            int textHeight = getTextHeight();
            int textWidth = getTextWidth(text);
            //drawCross(textPos.x, textPos.y, 3, 0xFFFF0000);
            //drawCross(textWidth+textPos.x, textHeight+textPos.y, 3, 0xFFFF0000);
            int cornerSize = 3;
            int cornerColor = 0xFFFF0000;
            drawCornerTL(textPos.x, textPos.y, cornerSize, cornerColor);
            drawCornerTR(textPos.x + textWidth, textPos.y, cornerSize, cornerColor);
            drawCornerBL(textPos.x, textPos.y + textHeight, cornerSize, cornerColor);
            drawCornerBR(textPos.x + textWidth, textPos.y + textHeight, cornerSize, cornerColor);
        }
    }

    @Override
    public void layoutChanged() {
        int textHeight = getTextHeight();
        int textWidth = getTextWidth(text);
        int x = padding;
        int y = padding;
        int width = getWidth() - padding*2;
        int height = getHeight()-padding*2;

        Rectangle bounds = getAlignedBounds(0,0, textWidth, textHeight, alignment, x, y, width, height);

        textPos.x = bounds.x;
        textPos.y = bounds.y;
    }
}
