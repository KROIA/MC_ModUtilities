package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.Slider;

/**
 * A vertically oriented slider where the user drags the handle up/down to set
 * a value in the range {@code [0.0, 1.0]}.
 * <p>
 * The track is rendered as a vertical line; the draggable handle's vertical extent
 * is controlled by {@link #setSliderHeight(int)}, while its width fills the element.
 */
public class VerticalSlider extends Slider {

    /**
     * Creates a vertical slider with default size and a handle height of 10 pixels.
     */
    public VerticalSlider()
    {
        super();
        setSliderHeight(10);
    }
    /**
     * Creates a vertical slider at the given position and size.
     * @param x the x-coordinate (relative to the parent)
     * @param y the y-coordinate (relative to the parent)
     * @param width the width of the slider element
     * @param height the height of the slider track
     */
    public VerticalSlider(int x, int y, int width, int height) {
        super(x, y, width, height);
        setSliderHeight(10);
    }

    /**
     * Sets the height (in pixels) of the draggable slider handle.
     * @param height the handle height
     */
    public void setSliderHeight(int height) {
        sliderBounds.height = height;
    }
    /**
     * @return the height (in pixels) of the draggable slider handle
     */
    public int getSliderHeight() {
        return sliderBounds.height;
    }

    @Override
    protected void renderBackground() {
        super.renderBackground();
        // Slider background line
        int width = getWidth();
        int height = getHeight();

        drawRect(width/2-1, 0, 1, height, sliderLineColor);
    }


    @Override
    protected void render() {
        super.render();
    }

    @Override
    protected void layoutChanged() {
        sliderBounds.width = getWidth();
        sliderBounds.y = (int)(getSliderValue() * (getHeight() - sliderBounds.height));
    }

    @Override
    protected void sliderMovedToPos(int x, int y) {
        int height = getHeight();
        double value = (double)y / (double)(height - sliderBounds.height);
        setSliderValue(value);
    }


    @Override
    public void setSliderValue(double value) {
        super.setSliderValue(value);
        sliderBounds.y = (int)(getSliderValue() * (getHeight() - sliderBounds.height));
    }

}
