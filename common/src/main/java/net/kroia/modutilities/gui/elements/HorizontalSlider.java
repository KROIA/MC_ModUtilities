package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.Slider;

/**
 * A horizontally oriented slider where the user drags the handle left/right to set
 * a value in the range {@code [0.0, 1.0]}.
 * <p>
 * The track is rendered as a horizontal line; the draggable handle's horizontal extent
 * is controlled by {@link #setSliderWidth(int)}, while its height fills the element.
 */
public class HorizontalSlider extends Slider {

    /**
     * Creates a horizontal slider with default size and a handle width of 10 pixels.
     */
    public HorizontalSlider()
    {
        super();
        setSliderWidth(10);
    }
    /**
     * Creates a horizontal slider at the given position and size.
     * @param x the x-coordinate (relative to the parent)
     * @param y the y-coordinate (relative to the parent)
     * @param width the width of the slider track
     * @param height the height of the slider element
     */
    public HorizontalSlider(int x, int y, int width, int height) {
        super(x, y, width, height);
        setSliderWidth(10);
    }

    /**
     * Sets the width (in pixels) of the draggable slider handle.
     * @param width the handle width
     */
    public void setSliderWidth(int width) {
        sliderBounds.width = width;
    }
    /**
     * @return the width (in pixels) of the draggable slider handle
     */
    public int getSliderWidth() {
        return sliderBounds.width;
    }

    @Override
    protected void renderBackground() {
        super.renderBackground();
        // Slider background line
        int width = getWidth();
        int height = getHeight();

        drawRect(0, height/2-1, width, 1, sliderLineColor);
    }


    @Override
    protected void render() {
        super.render();
    }

    @Override
    protected void layoutChanged() {
        sliderBounds.height = getHeight();
        sliderBounds.x = (int)(getSliderValue() * (getWidth() - sliderBounds.width));
    }

    @Override
    protected void sliderMovedToPos(int x, int y) {
        int width = getWidth();
        double value = (double)x / (double)(width - sliderBounds.width);
        setSliderValue(value);
    }


    @Override
    public void setSliderValue(double value) {
        super.setSliderValue(value);
        sliderBounds.x = (int)(getSliderValue() * (getWidth() - sliderBounds.width));
    }

}
