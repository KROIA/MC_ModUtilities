# Data Visualization Dashboard

A live data dashboard with three series rendered through `Plot` and a Pause / Resume control.

## What this shows

- Configuring a `Plot` with X and Y axis ranges, axis labels and number formats.
- Adding multiple `Plot.PlotData` series with different colours and thicknesses.
- Pushing live samples into the plot from the screen's render loop.
- Adding a Pause button that freezes the time accumulator without removing the plot.

## How to run

```
/modutilities openExample dashboard
```

## What you see

- A title "Live Signal Dashboard".
- A `Plot` filling most of the screen showing a sliding sine wave (blue), a cosine wave (orange) and a flat zero reference line (white).
- A status caption listing which colour maps to which series.
- A Pause button at the bottom that toggles to "Resume" and back; while paused the curves stay frozen.

## Code walkthrough

The plot is created and configured up-front:

```java
plot = new Plot();
plot.setXRange(0, SAMPLE_COUNT);
plot.setYRange(-1.2f, 1.2f);
plot.setXAxisLabel("Sample");
plot.setYAxisLabel("Amplitude");
plot.setXAxisValueConversion("%.0f");
plot.setYAxisValueConversion("%.2f");
```

Three series are constructed as `Plot.PlotData` instances and added to the plot:

```java
sineSeries = new Plot.PlotData();
sineSeries.color = 0xFF55AAFF;
plot.addPlotData(sineSeries);
plot.addPlotData(cosineSeries);
plot.addPlotData(zeroSeries);
```

To animate the data, the screen overrides `render(...)` to bump a `time` accumulator and rebuild the y-value lists each frame:

```java
@Override
public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    if (!paused) {
        time += 0.05f * partialTick;
        rebuildSeries();
    }
    super.render(graphics, mouseX, mouseY, partialTick);
}
```

`rebuildSeries()` clears each series' `yValues` list and writes a fresh sine / cosine sample for each x-position. The Pause button just flips a boolean.

## Choosing X / Y axis ranges

Plot data is plotted as `(index → y)` pairs by default, so the X range maps to indices `0..N-1` of `yValues`. Set the Y range a little larger than your signal extremes (this example uses `[-1.2, 1.2]` for a unit sine wave) so the curve doesn't touch the frame.

## Key takeaways

- A `Plot` is just a renderer. Any state mutation (populating `yValues`) must come from your code.
- For high-frequency updates, drive the data from `render(...)` or a tick handler. Pause by guarding the update path - the plot itself doesn't need to be removed.
- Use `setXAxisValueConversion("%.0f")` / `setYAxisValueConversion("%.2f")` to format axis tick labels.
