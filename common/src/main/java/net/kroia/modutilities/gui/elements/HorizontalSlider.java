package net.kroia.modutilities.gui.elements;

public class HorizontalSlider extends Slider {

    public HorizontalSlider()
    {
        super();
        setSliderWidth(10);
    }
    public HorizontalSlider(int x, int y, int width, int height) {
        super(x, y, width, height);
        setSliderWidth(10);
    }

    public void setSliderWidth(int width) {
        sliderBounds.width = width;
    }
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
        int height = getHeight();
        double value = (double)x / (double)(width - sliderBounds.width);
        setSliderValue(value);
    }


    @Override
    public void setSliderValue(double value) {
        super.setSliderValue(value);
        sliderBounds.x = (int)(getSliderValue() * (getWidth() - sliderBounds.width));
    }

}
