package net.kroia.modutilities.gui.elements;

public class VerticalSlider extends Slider {

    public VerticalSlider()
    {
        super();
        setSliderHeight(10);
    }
    public VerticalSlider(int x, int y, int width, int height) {
        super(x, y, width, height);
        setSliderHeight(10);
    }

    public void setSliderHeight(int height) {
        sliderBounds.height = height;
    }
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
        int width = getWidth();
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
