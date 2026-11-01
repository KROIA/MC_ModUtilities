# Plot

## Overview

The `Plot` element is a graph visualization component that displays one or more data series as line plots with customizable X and Y axes. It supports axis labels, grid lines, and multiple plot overlays with different colors and thicknesses. Ideal for visualizing time series, functions, and data trends.

**When to use:**
- Performance metrics and monitoring graphs
- Mathematical function visualization
- Price charts and market trends
- Scientific data visualization
- Real-time data monitoring

## Constructor

```java
Plot()  // Creates a plot with default settings
```

## Key Methods

### Axis Configuration
```java
void setXRange(float minValue, float maxValue)        // Set X axis value range
void setYRange(float minValue, float maxValue)        // Set Y axis value range
void setXAxisLabel(String label)                      // Set X axis label text
void setYAxisLabel(String label)                      // Set Y axis label text
String getXAxisLabel()                                // Get X axis label
String getYAxisLabel()                                // Get Y axis label
```

### Axis Formatting
```java
void setXAxisValueConversion(String format)           // Set X axis number format (e.g. "%.2f")
void setYAxisValueConversion(String format)           // Set Y axis number format (e.g. "%.0f")
void setXAxisLabelCount(int count)                    // Set number of X axis labels
void setYAxisLabelCount(int count)                    // Set number of Y axis labels
int getXAxisLabelCount()                              // Get X axis label count
int getYAxisLabelCount()                              // Get Y axis label count
```

### Plot Data
```java
void setPlotData(PlotData data)                       // Set single plot (clears existing)
void addPlotData(PlotData data)                       // Add additional plot overlay
void clearPlotData()                                  // Remove all plots
```

### Styling
```java
void setAxisColor(int color)                          // Set axis line color
int getAxisColor()                                    // Get axis color
void setBackgroundGridColor(int color)                // Set grid line color
int getBackgroundGridColor()                          // Get grid color
void setTextFontScale(float scale)                    // Set label font scale
```

### Utility
```java
Point getGuiPosFromXValue(float xValue, float yValue) // Convert data coordinates to GUI position
```

## PlotData Class

The `Plot.PlotData` class holds data for a single plot line:

```java
PlotData data = new PlotData();
data.color = 0xFF00FF00;           // Line color (default: green)
data.thickness = 1.0f;             // Line thickness (default: 1.0)
data.yValues.add(10.0f);           // Add Y values (X is auto-distributed)
data.yValues.add(20.0f);
data.yValues.add(15.0f);
```

## Styling

### Default Colors
- **Axis Color**: White (0xFFFFFFFF)
- **Grid Color**: Gray (0xFF555555)
- **Default Plot Color**: Green (0xFF00FF00)

### Customization Example
```java
Plot plot = new Plot();
plot.setBackgroundColor(0xFF1a1a1a);
plot.setAxisColor(0xFFFFFFFF);
plot.setBackgroundGridColor(0xFF333333);
plot.setTextColor(0xFFCCCCCC);
plot.setTextFontScale(0.5f);
```

## Code Examples

### Simple Line Plot
```java
Plot plot = new Plot();
plot.setSize(400, 300);
plot.setXRange(0, 10);
plot.setYRange(0, 100);
plot.setXAxisLabel("Time (s)");
plot.setYAxisLabel("Value");

Plot.PlotData data = new Plot.PlotData();
data.color = 0xFF2266FF;
data.thickness = 2.0f;

// Add data points
for (int i = 0; i <= 10; i++) {
    float value = (float)(50 + 30 * Math.sin(i * 0.5));
    data.yValues.add(value);
}

plot.setPlotData(data);
gui.addChild(plot);
```

### Multiple Overlaid Plots
```java
Plot plot = new Plot();
plot.setSize(500, 350);
plot.setXRange(0, 100);
plot.setYRange(0, 200);
plot.setXAxisLabel("X Axis");
plot.setYAxisLabel("Y Axis");

// First dataset (blue)
Plot.PlotData data1 = new Plot.PlotData();
data1.color = 0xFF2266FF;
data1.thickness = 1.5f;
for (int i = 0; i <= 100; i++) {
    data1.yValues.add((float)(100 + 50 * Math.sin(i * 0.1)));
}

// Second dataset (red)
Plot.PlotData data2 = new Plot.PlotData();
data2.color = 0xFFFF2266;
data2.thickness = 1.5f;
for (int i = 0; i <= 100; i++) {
    data2.yValues.add((float)(100 + 50 * Math.cos(i * 0.1)));
}

plot.addPlotData(data1);
plot.addPlotData(data2);
```

### Real-Time Data Plot
```java
Plot realtimePlot = new Plot();
realtimePlot.setSize(600, 250);
realtimePlot.setXRange(0, 60);  // 60 second window
realtimePlot.setYRange(0, 100);
realtimePlot.setXAxisLabel("Time (seconds)");
realtimePlot.setYAxisLabel("FPS");

Plot.PlotData fpsData = new Plot.PlotData();
fpsData.color = 0xFF44FF44;
fpsData.thickness = 2.0f;

// Update every tick
void updateFPSPlot(float currentFPS) {
    fpsData.yValues.add(currentFPS);
    
    // Keep only last 60 data points
    if (fpsData.yValues.size() > 60) {
        fpsData.yValues.remove(0);
    }
    
    realtimePlot.setPlotData(fpsData);
}
```

### Stock Price Chart
```java
Plot stockChart = new Plot();
stockChart.setSize(700, 400);
stockChart.setXRange(0, 365);  // 1 year
stockChart.setYRange(0, 200);
stockChart.setXAxisLabel("Day of Year");
stockChart.setYAxisLabel("Price ($)");
stockChart.setYAxisValueConversion("$%.2f");

Plot.PlotData priceData = new Plot.PlotData();
priceData.color = 0xFF4488FF;
priceData.thickness = 1.5f;

for (StockPrice price : historicalData) {
    priceData.yValues.add(price.getValue());
}

stockChart.setPlotData(priceData);
```

### Scientific Data with Custom Formatting
```java
Plot scientificPlot = new Plot();
scientificPlot.setSize(500, 400);
scientificPlot.setXRange(0, 1000);
scientificPlot.setYRange(0.001f, 10000);
scientificPlot.setXAxisLabel("Frequency (Hz)");
scientificPlot.setYAxisLabel("Amplitude");
scientificPlot.setXAxisValueConversion("%.0f");
scientificPlot.setYAxisValueConversion("%.3f");
scientificPlot.setXAxisLabelCount(6);
scientificPlot.setYAxisLabelCount(8);

Plot.PlotData amplitudeData = new Plot.PlotData();
amplitudeData.color = 0xFFFF9944;
// Add scientific measurement data
scientificPlot.setPlotData(amplitudeData);
```

## Common Patterns

### Dynamic Y-Axis Range
```java
Plot autoScalePlot = new Plot();
Plot.PlotData data = new Plot.PlotData();

void updatePlotWithAutoScale(List<Float> newValues) {
    data.yValues.clear();
    data.yValues.addAll(newValues);
    
    // Find min and max
    float min = Collections.min(newValues);
    float max = Collections.max(newValues);
    
    // Add 10% padding
    float padding = (max - min) * 0.1f;
    autoScalePlot.setYRange(min - padding, max + padding);
    autoScalePlot.setPlotData(data);
}
```

### Multiple Metrics Dashboard
```java
Frame dashboard = new Frame(0, 0, 800, 600);
dashboard.setLayout(new LayoutGrid(2, 2, true, true, 0, 0, Alignment.CENTER));

Plot cpuPlot = createMetricPlot("CPU Usage", 0xFF4488FF);
Plot memoryPlot = createMetricPlot("Memory Usage", 0xFF44FF88);
Plot networkPlot = createMetricPlot("Network Traffic", 0xFFFF8844);
Plot diskPlot = createMetricPlot("Disk I/O", 0xFFFF44FF);

dashboard.addChild(cpuPlot);
dashboard.addChild(memoryPlot);
dashboard.addChild(networkPlot);
dashboard.addChild(diskPlot);
```

## Best Practices

1. **Axis Ranges**: Set appropriate min/max values to ensure all data is visible
2. **Label Count**: Use 5-10 axis labels for readability; too many clutters the display
3. **Color Choice**: Use contrasting colors for multiple plots on the same graph
4. **Data Density**: Limit the number of data points to prevent performance issues
5. **Font Scale**: Use smaller font scales (0.4-0.6) for compact plots
6. **Value Formatting**: Use appropriate format strings (%.0f for integers, %.2f for decimals)
7. **Grid Lines**: Use subtle grid colors that don't overpower the data
8. **Update Frequency**: For real-time plots, limit updates to avoid overwhelming the renderer

## Technical Notes

- The plot automatically distributes Y values evenly across the X axis range
- X values for data points are calculated as: `xValue = xMin + (xMax - xMin) * index / (dataPoints - 1)`
- Grid lines are drawn at each axis label position
- The plot uses internal `Axis` classes to manage coordinate transformations
- Labels are drawn with rotation for the Y-axis label
- The font scale is set to 0.5 by default for better label fit
- Axis padding is automatically calculated based on label sizes
