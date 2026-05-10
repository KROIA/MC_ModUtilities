package net.kroia.modutilities.sandbox.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiScreen;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.Plot;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.network.chat.Component;

/**
 * Usecase example: a data visualization dashboard.
 *
 * Renders a {@link Plot} with three series:
 *  - A 1 Hz sine wave
 *  - A cosine wave 90° out of phase
 *  - A static reference line at y=0
 *
 * The plot updates live each render frame from a time accumulator.
 * Provides a "Pause/Resume" button that toggles the animation.
 *
 * Demonstrates:
 *  - Adding multiple {@link Plot.PlotData} series with custom colours
 *  - Configuring axis ranges, labels and number formats
 *  - Pushing live data into the plot from the screen render loop
 *
 * Open via: /modutilities openExample dashboard
 */
@Environment(EnvType.CLIENT)
public class ExampleDashboardScreen extends GuiScreen {

    private static final int SAMPLE_COUNT = 100;

    private final Label title;
    private final Plot plot;
    private final Plot.PlotData sineSeries;
    private final Plot.PlotData cosineSeries;
    private final Plot.PlotData zeroSeries;
    private final Button pauseButton;
    private final Label statusLabel;

    private float time = 0f;
    private boolean paused = false;

    public ExampleDashboardScreen() {
        super(Component.literal("Dashboard Example"));

        title = new Label("Live Signal Dashboard");
        title.setAlignment(GuiElement.Alignment.CENTER);
        title.setTextFontScale(1.5f);
        addElement(title);

        plot = new Plot();
        plot.setXRange(0, SAMPLE_COUNT);
        plot.setYRange(-1.2f, 1.2f);
        plot.setXAxisLabel("Sample");
        plot.setYAxisLabel("Amplitude");
        plot.setXAxisValueConversion("%.0f");
        plot.setYAxisValueConversion("%.2f");

        sineSeries = new Plot.PlotData();
        sineSeries.color = 0xFF55AAFF; // blue-ish
        sineSeries.thickness = 1.0f;

        cosineSeries = new Plot.PlotData();
        cosineSeries.color = 0xFFFF7755; // orange-ish
        cosineSeries.thickness = 1.0f;

        zeroSeries = new Plot.PlotData();
        zeroSeries.color = 0x88FFFFFF;
        zeroSeries.thickness = 1.0f;

        plot.addPlotData(sineSeries);
        plot.addPlotData(cosineSeries);
        plot.addPlotData(zeroSeries);

        addElement(plot);

        statusLabel = new Label("Series: sine (blue), cosine (orange), zero (white)");
        statusLabel.setAlignment(GuiElement.Alignment.CENTER);
        addElement(statusLabel);

        pauseButton = new Button("Pause", this::togglePause);
        addElement(pauseButton);

        rebuildSeries();
    }

    public static void open() {
        GuiScreen.setScreen(new ExampleDashboardScreen());
    }

    private void togglePause() {
        paused = !paused;
        pauseButton.setText(paused ? "Resume" : "Pause");
    }

    private void rebuildSeries() {
        sineSeries.yValues.clear();
        cosineSeries.yValues.clear();
        zeroSeries.yValues.clear();
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double phase = (i / (double) SAMPLE_COUNT) * Math.PI * 4 + time;
            sineSeries.yValues.add((float) Math.sin(phase));
            cosineSeries.yValues.add((float) Math.cos(phase));
            zeroSeries.yValues.add(0f);
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!paused) {
            time += 0.05f * partialTick;
            rebuildSeries();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateLayout(Gui gui) {
        int w = getWidth();
        int h = getHeight();
        int margin = 20;

        title.setBounds(0, margin, w, 22);

        int plotTop = title.getBottom() + 8;
        int controlsHeight = 60;
        int plotHeight = h - plotTop - controlsHeight;
        plot.setBounds(margin, plotTop, w - margin * 2, plotHeight);

        statusLabel.setBounds(0, plot.getBottom() + 6, w, 14);

        pauseButton.setBounds(w / 2 - 60, statusLabel.getBottom() + 6, 120, 22);
    }
}
