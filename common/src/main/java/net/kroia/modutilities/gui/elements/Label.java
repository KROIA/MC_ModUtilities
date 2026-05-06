package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;
import net.kroia.modutilities.gui.geometry.Rectangle;

import java.util.Objects;

/**
 * Single-line/multi-line text display element.
 * <p>
 * The text is laid out within the element's bounds according to a configurable
 * {@link GuiElement.Alignment} and an inner {@linkplain #setPadding(int) padding}.
 * By default the background and outline are disabled, producing a plain text label;
 * call {@code setEnableBackground(true)} / {@code setEnableOutline(true)} on the
 * inherited {@link GuiElement} API if a framed label is desired.
 *
 * @apiNote {@code null} text is silently coerced to the empty string; passing
 *          {@code null} to {@link #setText(String)} or the {@link #Label(String)}
 *          constructor will not throw.
 */
public class Label extends GuiElement {

    /** Default height in pixels used by the parameterless and string constructors. */
    public static final int DEFAULT_HEIGHT = 15;
    private Alignment alignment = Alignment.LEFT;
    private String text;
    private int padding = GuiElement.DEFAULT_PADDING;
    private Point textPos = new Point(0,0);

    /**
     * Creates an empty label with default size and {@link Alignment#LEFT} alignment.
     */
    public Label()
    {
        this("");
    }

    /**
     * Creates a label displaying the given text with default size and
     * {@link Alignment#LEFT} alignment.
     *
     * @param text the text to display, or {@code null} for an empty string
     */
    public Label(String text)
    {
        super(0,0,100,DEFAULT_HEIGHT);
        this.text = Objects.requireNonNullElse(text, "");
        setEnableBackground(false);
        setEnableOutline(false);
    }

    /**
     * Sets the text displayed by this label and triggers a layout recomputation.
     *
     * @param text the new text to display, or {@code null} for an empty string
     */
    public void setText(String text)
    {
        this.text = Objects.requireNonNullElse(text, "");
        layoutChangedInternal();
    }

    /**
     * @return the current label text (never {@code null})
     */
    public String getText(){
        return text;
    }

    /**
     * Sets how the text is positioned within the label bounds and triggers a
     * layout recomputation.
     *
     * @param alignment the alignment to use for the text
     */
    public void setAlignment(Alignment alignment)
    {
        this.alignment = alignment;
        layoutChangedInternal();
    }

    /**
     * @return the current text alignment
     */
    public Alignment getAlignment()
    {
        return alignment;
    }

    /**
     * Sets the inner padding (in pixels) reserved on every side of the text.
     *
     * @param padding the padding in pixels
     */
    public void setPadding(int padding)
    {
        this.padding = padding;
    }

    /**
     * @return the current inner padding in pixels
     */
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
