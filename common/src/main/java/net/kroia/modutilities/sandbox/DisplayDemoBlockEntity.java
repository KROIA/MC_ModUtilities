package net.kroia.modutilities.sandbox;

import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.display.AbstractDisplayBlockEntity;
import net.kroia.modutilities.gui.display.ContentBuilder;
import net.kroia.modutilities.gui.display.DisplayConfig;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.HorizontalSlider;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.Plot;
import net.kroia.modutilities.gui.elements.TextBox;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the DisplayDemoBlock. Extends the reusable Display Block API
 * and provides demo-specific content: a live sine/cosine plot with speed slider,
 * pause button, and editable title.
 */
public class DisplayDemoBlockEntity extends AbstractDisplayBlockEntity {

    private static final int SAMPLE_COUNT = 100;

    // Demo-specific GUI element references (server-side only)
    private Plot plot;
    private Label statusLabel;

    // Demo-specific state
    private float time = 0;
    private boolean paused = false;
    private double speed = 0.5;
    private String titleText = "Live Signal Dashboard";

    public DisplayDemoBlockEntity(BlockPos pos, BlockState blockState) {
        super(SandboxRegistration.DISPLAY_DEMO_BLOCK_ENTITY.get(), pos, blockState);
    }

    // -------------------------------------------------------------------------
    // Abstract method implementations
    // -------------------------------------------------------------------------

    @Override
    public DisplayConfig getDisplayConfig() {
        return DisplayConfig.fullBlock();
    }

    @Override
    public ContentBuilder getContentBuilder() {
        return DisplayDemoBlockEntity::buildDashboard;
    }

    // -------------------------------------------------------------------------
    // Protected hooks
    // -------------------------------------------------------------------------

    @Override
    protected void wireCallbacks(Gui gui) {
        plot = null;
        statusLabel = null;

        for (var el : gui.getElements()) {
            if (el instanceof Plot p) {
                plot = p;
            }
            if (el instanceof Label l && l.getText() != null && l.getText().startsWith("Series:")) {
                statusLabel = l;
            }
            if (el instanceof HorizontalSlider slider) {
                slider.setOnValueChanged(value -> {
                    speed = value;
                    for (var e : gui.getElements()) {
                        if (e instanceof Label label && label.getText() != null && label.getText().startsWith("Speed:")) {
                            label.setText("Speed: " + (int)(value * 100) + "%");
                            break;
                        }
                    }
                    syncToClientPublic();
                });
            }
            if (el instanceof Button btn && "Pause".equals(btn.getText())) {
                btn.setOnFallingEdge(this::togglePaused);
            }
            if (el instanceof TextBox tb) {
                tb.setOnTextChanged(this::handleTextInput);
            }
        }

        syncStateToGui();
    }

    @Override
    protected void onControllerTick() {
        if (paused) return;

        time += 0.05f * (float) speed;
        if (plot == null) return;

        plot.clearPlotData();

        Plot.PlotData sineSeries = new Plot.PlotData();
        sineSeries.color = 0xFF55AAFF;
        sineSeries.thickness = 1.0f;

        Plot.PlotData cosineSeries = new Plot.PlotData();
        cosineSeries.color = 0xFFFF7755;
        cosineSeries.thickness = 1.0f;

        Plot.PlotData zeroSeries = new Plot.PlotData();
        zeroSeries.color = 0x88FFFFFF;
        zeroSeries.thickness = 1.0f;

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double phase = (i / (double) SAMPLE_COUNT) * Math.PI * 4 + time;
            sineSeries.yValues.add((float) Math.sin(phase));
            cosineSeries.yValues.add((float) Math.cos(phase));
            zeroSeries.yValues.add(0f);
        }

        plot.addPlotData(sineSeries);
        plot.addPlotData(cosineSeries);
        plot.addPlotData(zeroSeries);
    }

    @Override
    protected void saveCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("paused", paused);
        tag.putDouble("speed", speed);
        tag.putString("titleText", titleText);
    }

    @Override
    protected void loadCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        paused = tag.getBoolean("paused");
        speed = tag.getDouble("speed");
        if (tag.contains("titleText")) {
            titleText = tag.getString("titleText");
        }
        syncStateToGui();
    }

    @Override
    public void onInputSynced() {
        if (gui == null) return;
        for (var el : gui.getElements()) {
            if (el instanceof HorizontalSlider slider) {
                speed = slider.getSliderValue();
            }
            if (el instanceof TextBox tb) {
                titleText = tb.getText();
            }
        }
        syncStateToGui();
    }

    // -------------------------------------------------------------------------
    // Demo-specific methods
    // -------------------------------------------------------------------------

    /**
     * Builds the dashboard layout. This is the {@link ContentBuilder} used both
     * by the block entity (server-side) and the interaction screen (client-side).
     * Creates visual elements only — no server callbacks. Callbacks are wired
     * separately via {@link #wireCallbacks(Gui)}.
     */
    private static void buildDashboard(Gui gui, int w, int h) {
        int margin = 10;

        Label title = new Label("Live Signal Dashboard");
        title.setBounds(0, margin, w, 16);
        title.setAlignment(GuiElement.Alignment.CENTER);
        gui.addElement(title);

        int plotTop = margin + 16 + 8;
        int controlsHeight = 68;
        int plotHeight = h - plotTop - controlsHeight;

        Plot plot = new Plot();
        plot.setBounds(margin, plotTop, w - margin * 2, plotHeight);
        plot.setXRange(0, SAMPLE_COUNT);
        plot.setYRange(-1.2f, 1.2f);
        plot.setXAxisLabel("Sample");
        plot.setYAxisLabel("Amplitude");
        plot.setXAxisValueConversion("%.0f");
        plot.setYAxisValueConversion("%.2f");

        Plot.PlotData sineSeries = new Plot.PlotData();
        sineSeries.color = 0xFF55AAFF;
        sineSeries.thickness = 1.0f;

        Plot.PlotData cosineSeries = new Plot.PlotData();
        cosineSeries.color = 0xFFFF7755;
        cosineSeries.thickness = 1.0f;

        Plot.PlotData zeroSeries = new Plot.PlotData();
        zeroSeries.color = 0x88FFFFFF;
        zeroSeries.thickness = 1.0f;

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double phase = (i / (double) SAMPLE_COUNT) * Math.PI * 4;
            sineSeries.yValues.add((float) Math.sin(phase));
            cosineSeries.yValues.add((float) Math.cos(phase));
            zeroSeries.yValues.add(0f);
        }

        plot.addPlotData(sineSeries);
        plot.addPlotData(cosineSeries);
        plot.addPlotData(zeroSeries);
        gui.addElement(plot);

        Label statusLabel = new Label("Series: sine (blue), cosine (orange), zero (white)");
        statusLabel.setBounds(0, plotTop + plotHeight + 4, w, 12);
        gui.addElement(statusLabel);
        statusLabel.setAlignment(GuiElement.Alignment.CENTER);

        int controlY = plotTop + plotHeight + 18;

        Label speedLabel = new Label("Speed: 50%");
        speedLabel.setBounds(margin, controlY, 60, 14);
        speedLabel.setAlignment(GuiElement.Alignment.LEFT);
        gui.addElement(speedLabel);

        HorizontalSlider speedSlider = new HorizontalSlider(
                margin + 62, controlY, w - margin * 2 - 62 - 70, 14);
        speedSlider.setSliderValue(0.5);
        gui.addElement(speedSlider);

        Button pauseButton = new Button("Pause");
        pauseButton.setBounds(w - margin - 65, controlY, 65, 14);
        gui.addElement(pauseButton);

        // Second control row: text input
        int textRowY = controlY + 18;

        Label inputLabel = new Label("Title:");
        inputLabel.setBounds(margin, textRowY, 35, 14);
        inputLabel.setAlignment(GuiElement.Alignment.LEFT);
        gui.addElement(inputLabel);

        Label echoLabel = new Label("");
        echoLabel.setBounds(w / 2 + 5, textRowY, w / 2 - margin - 5, 14);
        echoLabel.setAlignment(GuiElement.Alignment.LEFT);
        gui.addElement(echoLabel);

        TextBox titleInput = new TextBox(margin + 37, textRowY, w / 2 - margin - 37);
        titleInput.setText("Live Signal Dashboard");
        titleInput.setMaxChars(40);
        gui.addElement(titleInput);
    }

    /**
     * Applies the current field values to the GUI elements.
     */
    private void syncStateToGui() {
        if (gui == null) return;
        for (var el : gui.getElements()) {
            if (el instanceof HorizontalSlider slider) {
                slider.setSliderValue(speed);
            }
            if (el instanceof Label label && label.getText() != null && label.getText().startsWith("Speed:")) {
                label.setText("Speed: " + (int) (speed * 100) + "%");
            }
            if (el instanceof TextBox tb) {
                tb.setText(titleText);
            }
        }
        if (!gui.getElements().isEmpty() && gui.getElements().get(0) instanceof Label title
                && !(gui.getElements().get(0) instanceof TextBox)) {
            title.setText(titleText);
        }
    }

    /**
     * Handles text input changes from the title TextBox.
     */
    private void handleTextInput(String text) {
        if (gui == null) return;
        this.titleText = text;
        for (var el : gui.getElements()) {
            if (el instanceof TextBox tb) {
                tb.setText(text);
                break;
            }
        }
        if (!gui.getElements().isEmpty() && gui.getElements().get(0) instanceof Label title) {
            title.setText(text);
        }
        syncToClientPublic();
    }

    /**
     * Toggles the paused state of the plot animation.
     */
    private void togglePaused() {
        paused = !paused;
        net.kroia.modutilities.ModUtilitiesMod.LOGGER.info(
                "[DisplayBlock] togglePaused -> paused={}", paused);
        syncToClientPublic();
    }
}
