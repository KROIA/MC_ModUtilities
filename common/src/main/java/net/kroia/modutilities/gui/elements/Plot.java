package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;

import java.util.ArrayList;
import java.util.List;


/**
 * 2D line graph element that plots one or more {@link PlotData} series against
 * shared X and Y axes.
 * <p>
 * The plot manages two internal axes (X and Y) with configurable value ranges,
 * label counts, and printf-style label formats. X values for each plot are
 * implied to be evenly distributed across the X range; only Y samples are
 * stored on each {@link PlotData}.
 *
 * @apiNote The internal axes guard against zero value range and fall back to
 *          a scale of 1.0 to avoid divide-by-zero in coordinate conversions.
 */
public class Plot extends GuiElement
{
    private final int pointCount = 100;


    /**
     * A single line series displayed on the {@link Plot}.
     * <p>
     * X positions are implied by the index of each value within
     * {@link #yValues} mapped uniformly across the plot's X range; only Y
     * samples are stored.
     */
    public static class PlotData
    {
        /** Line color in {@code 0xAARRGGBB} format. Default is opaque green. */
        public int color = 0xFF00FF00; // Default green color
        /** Line thickness in pixels. */
        public float thickness = 1.0f; // Default thickness
        /** Y values of the series in sample order. */
        public final List<Float> yValues = new ArrayList<>();
    }


    private class Axis
    {
        private float minValue = 0.0f;
        private float maxValue = 100.0f; // Default range

        private int minPos = 0;
        private int maxPos = 100; // Default range in GUI coordinates
       // private boolean useLogScale = false;
        private float scale = 1.0f; // Scale factor for the axis

        public int getPos(float value)
        {
            /*if (useLogScale) {
                if (value <= 0) return minPos; // Log scale cannot handle non-positive values
                float logValue = (float) Math.log10(value);
                float logMin = (float) Math.log10(minValue);
                float logMax = (float) Math.log10(maxValue);
                return minPos + (int) ((logValue - logMin) / (logMax - logMin) * (maxPos - minPos));
            } else {
                return minPos + (int) ((value - minValue) / (maxValue - minValue) * (maxPos - minPos));
            }*/
            return minPos + (int) ((value - minValue) * scale);
        }
        public float getValue(int pos)
        {
            /*if (useLogScale) {
                float logMin = (float) Math.log10(minValue);
                float logMax = (float) Math.log10(maxValue);
                float logValue = logMin + (pos - minPos) / (float)(maxPos - minPos) * (logMax - logMin);
                return (float) Math.pow(10, logValue);
            } else {
                return minValue + (pos - minPos) / (float)(maxPos - minPos) * (maxValue - minValue);
            }*/
            return minValue + (pos - minPos) / scale;
        }

        public void setValueRange(float minValue, float maxValue)
        {
            this.minValue = minValue;
            this.maxValue = maxValue;
            float range = maxValue - minValue;
            this.scale = range == 0f ? 1f : (maxPos - minPos) / range;
        }
        public void setGuiRange(int minPos, int maxPos)
        {
            this.minPos = minPos;
            this.maxPos = maxPos;
            float range = maxValue - minValue;
            this.scale = range == 0f ? 1f : (float)(maxPos - minPos) / range;
        }
        public float getMinValue() {
            return minValue;
        }
        public float getMaxValue() {
            return maxValue;
        }
        public int getMinPos() {
            return minPos;
        }
        public int getMaxPos() {
            return maxPos;
        }
        /*public void enableLogScale(boolean enable) {
            this.useLogScale = enable;
        }
        public boolean isLogScaleEnabled() {
            return useLogScale;
        }*/
    }



    private final List<PlotData> plots = new ArrayList<>();
    private final Axis xAxis = new Axis();
    private final Axis yAxis = new Axis();

    private int xAxisPadding = 0; // Padding for axes
    private int yAxisPadding = 0; // Padding for axes

    private String xAxisValueConversion = "%.0f"; // Format for X axis labels
    private String yAxisValueConversion = "%.0f"; // Format for Y axis labels
    private int backgroundGridColor = 0xFF555555;
    private int axisColor = 0xFFFFFFFF;
    private int xAxisLabelCount = 5; // Number of labels on X axis
    private int yAxisLabelCount = 5; // Number of labels on Y axis
    private String yLabel = "Y Axis"; // Y axis label
    private String xLabel = "X Axis"; // X axis label
    /**
     * Creates a new {@code Plot} with default axis ranges, default labels,
     * and a half-size text font scale.
     */
    public Plot() {
        super();
        setTextFontScale(0.5f);
       // yAxis.enableLogScale(true);
    }

    /**
     * Sets the value range covered by the X axis.
     *
     * @param minValue the value at the left edge of the plot area
     * @param maxValue the value at the right edge of the plot area
     */
    public void setXRange(float minValue, float maxValue)
    {
        xAxis.setValueRange(minValue, maxValue);
    }

    /**
     * Sets the value range covered by the Y axis.
     *
     * @param minValue the value at the bottom edge of the plot area
     * @param maxValue the value at the top edge of the plot area
     */
    public void setYRange(float minValue, float maxValue)
    {
        yAxis.setValueRange(minValue, maxValue);
    }

    /**
     * Replaces all current plot series with a single series.
     * <p>
     * Has no effect when {@code data} is {@code null}.
     *
     * @param data the new plot series, or {@code null} to leave the plot unchanged
     */
    public void setPlotData(PlotData data)
    {
        if(data != null)
        {
            plots.clear();
            plots.add(data);
        }
    }

    /**
     * Appends an additional series to the plot.
     * <p>
     * Has no effect when {@code data} is {@code null}.
     *
     * @param data the series to add, or {@code null} to no-op
     */
    public void addPlotData(PlotData data)
    {
        if(data != null)
        {
            plots.add(data);
        }
    }

    /**
     * Removes all currently registered plot series.
     */
    public void clearPlotData()
    {
        plots.clear();
    }

    /**
     * Sets the {@link String#format} pattern used to render X axis labels.
     *
     * @param format a printf-style format string (e.g. {@code "%.2f"})
     */
    public void setXAxisValueConversion(String format)
    {
        this.xAxisValueConversion = format;
    }

    /**
     * Sets the {@link String#format} pattern used to render Y axis labels.
     *
     * @param format a printf-style format string (e.g. {@code "%.2f"})
     */
    public void setYAxisValueConversion(String format)
    {
        this.yAxisValueConversion = format;
    }

    /**
     * Sets the color used to draw the X and Y axis lines.
     *
     * @param color the color in {@code 0xAARRGGBB} format
     */
    public void setAxisColor(int color)
    {
        this.axisColor = color;
    }

    /**
     * @return the current axis line color in {@code 0xAARRGGBB} format
     */
    public int getAxisColor()
    {
        return axisColor;
    }

    /**
     * Sets the color used to draw the background grid lines (one per label).
     *
     * @param color the color in {@code 0xAARRGGBB} format
     */
    public void setBackgroundGridColor(int color)
    {
        this.backgroundGridColor = color;
    }

    /**
     * @return the current background grid color in {@code 0xAARRGGBB} format
     */
    public int getBackgroundGridColor()
    {
        return backgroundGridColor;
    }

    /**
     * Sets the number of evenly distributed labels along the X axis.
     *
     * @param count the number of labels (and grid lines) to render along X
     */
    public void setXAxisLabelCount(int count)
    {
        this.xAxisLabelCount = count;
    }

    /**
     * @return the number of labels rendered along the X axis
     */
    public int getXAxisLabelCount()
    {
        return xAxisLabelCount;
    }

    /**
     * Sets the number of evenly distributed labels along the Y axis.
     *
     * @param count the number of labels (and grid lines) to render along Y
     */
    public void setYAxisLabelCount(int count)
    {
        this.yAxisLabelCount = count;
    }

    /**
     * @return the number of labels rendered along the Y axis
     */
    public int getYAxisLabelCount()
    {
        return yAxisLabelCount;
    }

    /**
     * Sets the descriptive Y axis title rendered alongside the axis.
     *
     * @param label the Y axis label, or empty/{@code null} to hide it
     */
    public void setYAxisLabel(String label)
    {
        this.yLabel = label;
    }

    /**
     * @return the current Y axis title
     */
    public String getYAxisLabel()
    {
        return yLabel;
    }

    /**
     * Sets the descriptive X axis title rendered alongside the axis.
     *
     * @param label the X axis label, or empty/{@code null} to hide it
     */
    public void setXAxisLabel(String label)
    {
        this.xLabel = label;
    }

    /**
     * @return the current X axis title
     */
    public String getXAxisLabel()
    {
        return xLabel;
    }

    /**
     * Converts a (X, Y) data-space coordinate to its corresponding pixel position
     * within this element using the current axis ranges.
     *
     * @param xValue the X value in data-space
     * @param yValue the Y value in data-space
     * @return a new {@link Point} containing the pixel coordinates
     */
    public Point getGuiPosFromXValue(float xValue, float yValue)
    {
        int xPos = xAxis.getPos(xValue);
        int yPos = yAxis.getPos(yValue); // Assuming Y value is 0 for the X axis position
        return new Point(xPos, yPos);
    }



    @Override
    protected void render() {
        int height = getHeight();
        int width = getWidth();


        int maxYValueTextWidth = 0;
        int maxXValueTextWidth = 0;


        // Draw X Labels
        for (int i = 0; i < xAxisLabelCount; i++) {
            float xValue = xAxisLabelCount > 1
                    ? xAxis.getMinValue() + (xAxis.getMaxValue() - xAxis.getMinValue()) * i / (xAxisLabelCount-1)
                    : xAxis.getMinValue();
            int xPos = xAxis.getPos(xValue);
            String text = String.format(xAxisValueConversion, xValue);
            if(i==xAxisLabelCount-1)
            {
                maxXValueTextWidth = getTextWidth(text);
            }

            drawText(text, xPos, yAxis.getMinPos() + 3, Alignment.TOP);

            // Draw vertical line for X axis labels
            drawRect(xPos, yAxis.getMaxPos(), 1, yAxis.getMinPos()-yAxis.getMaxPos(), backgroundGridColor);
        }
        // Draw X Axis Label
        if(xLabel != null && !xLabel.isEmpty()) {
            drawText(xLabel, (xAxis.getMinPos()+xAxis.getMaxPos())/2, yAxis.getMinPos()+getTextHeight()+3, Alignment.TOP);
        }



        // Draw Y Labels
        for (int i = 0; i < yAxisLabelCount; i++) {
            float yValue = yAxisLabelCount > 1
                    ? yAxis.getMinValue() + (yAxis.getMaxValue() - yAxis.getMinValue()) * i / (yAxisLabelCount-1)
                    : yAxis.getMinValue();
            int yPos = yAxis.getPos(yValue);
            String text = String.format(yAxisValueConversion, yValue);
            maxYValueTextWidth = Math.max(maxYValueTextWidth, getTextWidth(text));
            drawText(text, yAxisPadding-1, yPos, Alignment.RIGHT);

            // Draw horizontal line for Y axis labels
            drawRect(xAxis.getMinPos(), yPos, xAxis.getMaxPos()-xAxis.getMinPos(), 1, backgroundGridColor);
        }

        // Draw Y Axis Label
        if(yLabel != null && !yLabel.isEmpty()) {
            graphicsPushPose();
            graphicsTranslate((float)getTextHeight()/2+1, (float)(yAxis.getMinPos()+yAxis.getMaxPos())/2, 0);
            getPoseStack().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90));
            drawText(yLabel, 0,0, Alignment.TOP);
            graphicsPopPose();
        }

        // Draw Plots
        for (PlotData plot : plots) {
            if(plot.yValues.size() > 1) {
                int lastX = xAxis.getPos(0);
                int lastY = yAxis.getPos(plot.yValues.get(0));

                for (int i = 1; i < plot.yValues.size(); i++) {
                    float nextXValue = xAxis.getMinValue() + (xAxis.getMaxValue() - xAxis.getMinValue()) * i / (plot.yValues.size() - 1);
                    float nextYValue = plot.yValues.get(i);
                    int nextX = xAxis.getPos(nextXValue);
                    int nextY = yAxis.getPos(nextYValue);

                    drawLine(lastX, lastY, nextX, nextY, plot.thickness, plot.color); // Draw line in specified color

                    lastX = nextX;
                    lastY = nextY;
                }
            }
        }

        // Draw X axis
        drawRect(xAxis.getMinPos(), yAxis.getMinPos(), xAxis.getMaxPos()-xAxis.getMinPos()+1, 1, axisColor);
        // Draw Y axis
        drawRect(xAxis.getMinPos(), yAxis.getMaxPos(), 1, yAxis.getMinPos()-yAxis.getMaxPos(), axisColor);

        xAxisPadding = getTextHeight()*2 + 5; // Adjust padding for X axis based on text height
        yAxisPadding = maxYValueTextWidth + getTextHeight()+5; // Adjust padding for Y axis based on text width
        xAxis.setGuiRange(yAxisPadding, getWidth() - maxXValueTextWidth/2);
        yAxis.setGuiRange(height-xAxisPadding, getTextHeight()/2); // Leave space for X axis labels







        /*int height = getHeight();
        int width = getWidth();

        float maxXValue = getXMaxPrice();
        float maxYValue = getYValue(maxXValue);

        float scaleX = width / maxXValue;
        float scaleY = height / maxYValue;

        int lastX = 0;
        int lastY = (int) (height - getYValue(0) * scaleY);
        for(int i = 0; i < pointCount; i++)
        {
            float nextX = (float) i / (pointCount - 1) * maxXValue;
            float nextY = getYValue(nextX);

            int x = (int) (nextX * scaleX);
            int y = (int) (height - nextY * scaleY); // Invert Y axis for GUI coordinates

            drawLine( lastX, lastY, x, y,1.0f, 0xFF00FF00); // Draw line in green color

            lastX = x;
            lastY = y;
        }

        if(Plot.this.isMouseOver())
        {
            int mouseX = getMouseX();
            int mouseY = getMouseY();

            Alignment textAlignment = Alignment.BOTTOM_RIGHT;
            if(mouseY < height / 2)
                textAlignment = Alignment.TOP_RIGHT;

            float yValue = getYValue(mouseX / scaleX);
            int yPos = (int) (height - yValue * scaleY);

            drawRect(mouseX, yPos-2, 1, 4, 0xFFFF0000); // Vertical line in red color
            drawText(String.format("X: %.2f\nY: %.2f", mouseX / scaleX, yValue), mouseX, yPos, textAlignment);
        }*/
    }

    @Override
    protected void layoutChanged() {


    }



    /*int getXMaxPrice()
    {
        int maxPrice = 0;
        for(MarketFactory.DefaultMarketSetupData data : defaultMarketSetupDataList)
        {
            maxPrice = Math.max(maxPrice, data.getDefaultPrice());
        }
        return maxPrice;
    }
    float getYValue(float X)
    {
        return MarketFactory.getAjustedPriceF(X, factors.linearFactor, factors.quadraticFactor, factors.exponentialFactor);
    }*/

}
