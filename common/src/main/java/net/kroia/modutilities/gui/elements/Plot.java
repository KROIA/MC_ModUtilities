package net.kroia.modutilities.gui.elements;

import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Point;

import java.util.ArrayList;
import java.util.List;


/**
 * Plots points to a graph with X and Y axes.
 */
public class Plot extends GuiElement
{
    private final int pointCount = 100;


    public static class PlotData
    {
        public int color = 0xFF00FF00; // Default green color
        public float thickness = 1.0f; // Default thickness
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
            this.scale = (maxPos - minPos) / (maxValue - minValue);
        }
        public void setGuiRange(int minPos, int maxPos)
        {
            this.minPos = minPos;
            this.maxPos = maxPos;
            this.scale = (float)(maxPos - minPos) / (maxValue - minValue);
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
    public Plot() {
        super();
        setTextFontScale(0.5f);
       // yAxis.enableLogScale(true);
    }

    public void setXRange(float minValue, float maxValue)
    {
        xAxis.setValueRange(minValue, maxValue);
    }
    public void setYRange(float minValue, float maxValue)
    {
        yAxis.setValueRange(minValue, maxValue);
    }

    public void setPlotData(PlotData data)
    {
        if(data != null)
        {
            plots.clear();
            plots.add(data);
        }
    }
    public void addPlotData(PlotData data)
    {
        if(data != null)
        {
            plots.add(data);
        }
    }
    public void clearPlotData()
    {
        plots.clear();
    }

    public void setXAxisValueConversion(String format)
    {
        this.xAxisValueConversion = format;
    }
    public void setYAxisValueConversion(String format)
    {
        this.yAxisValueConversion = format;
    }
    public void setAxisColor(int color)
    {
        this.axisColor = color;
    }
    public int getAxisColor()
    {
        return axisColor;
    }
    public void setBackgroundGridColor(int color)
    {
        this.backgroundGridColor = color;
    }
    public int getBackgroundGridColor()
    {
        return backgroundGridColor;
    }
    public void setXAxisLabelCount(int count)
    {
        this.xAxisLabelCount = count;
    }
    public int getXAxisLabelCount()
    {
        return xAxisLabelCount;
    }
    public void setYAxisLabelCount(int count)
    {
        this.yAxisLabelCount = count;
    }
    public int getYAxisLabelCount()
    {
        return yAxisLabelCount;
    }
    public void setYAxisLabel(String label)
    {
        this.yLabel = label;
    }
    public String getYAxisLabel()
    {
        return yLabel;
    }
    public void setXAxisLabel(String label)
    {
        this.xLabel = label;
    }
    public String getXAxisLabel()
    {
        return xLabel;
    }

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
            float xValue = xAxis.getMinValue() + (xAxis.getMaxValue() - xAxis.getMinValue()) * i / (xAxisLabelCount-1);
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
            float yValue = yAxis.getMinValue() + (yAxis.getMaxValue() - yAxis.getMinValue()) * i / (yAxisLabelCount-1);
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
