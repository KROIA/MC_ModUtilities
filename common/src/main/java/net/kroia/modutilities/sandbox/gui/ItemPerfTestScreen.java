package net.kroia.modutilities.sandbox.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kroia.modutilities.ItemUtilities;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.client.GuiScreen;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.ItemView;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.VerticalListView;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.layout.LayoutGrid;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Dev-only performance test harness for the laggy item grid list.
 * <p>
 * Renders a scrollable grid of {@link ItemView}s inside a {@link VerticalListView}
 * subclass that times the list's background and foreground render passes, so a
 * baseline can be captured and compared after optimizing. Buttons grow (reinsert
 * all items) and clear the list.
 *
 * Open via: /modutilities openItemPerfScreen
 */
@Environment(EnvType.CLIENT)
public class ItemPerfTestScreen extends GuiScreen {

    /** Lowpass smoothing factor for the running averages: avg = avg*FILTER + sample*(1-FILTER). */
    private static final double FILTER = 0.95;

    /** Applies one lowpass step, seeding on the first sample so it doesn't ramp from zero. */
    private static double lowpass(double avg, double sample, boolean seeded) {
        return seeded ? avg * FILTER + sample * (1.0 - FILTER) : sample;
    }

    /**
     * VerticalListView that measures how long its background and foreground
     * render passes take (nanoseconds), keeping a lowpass-filtered average.
     */
    private static class PerfVerticalListView extends VerticalListView {
        private long lastBgNanos = 0, lastFgNanos = 0;
        private double avgBgNanos = 0, avgFgNanos = 0;
        private boolean seeded = false;

        @Override
        public void renderBackgroundInternal() {
            long start = System.nanoTime();
            super.renderBackgroundInternal();
            lastBgNanos = System.nanoTime() - start;
            avgBgNanos = lowpass(avgBgNanos, lastBgNanos, seeded);
        }

        @Override
        public void renderInternal() {
            long start = System.nanoTime();
            super.renderInternal();
            lastFgNanos = System.nanoTime() - start;
            avgFgNanos = lowpass(avgFgNanos, lastFgNanos, seeded);
            seeded = true; // both passes have run once
        }

        double lastBgMicros() { return lastBgNanos / 1000.0; }
        double lastFgMicros() { return lastFgNanos / 1000.0; }
        double lastTotalMicros() { return (lastBgNanos + lastFgNanos) / 1000.0; }

        double avgBgMicros() { return avgBgNanos / 1000.0; }
        double avgFgMicros() { return avgFgNanos / 1000.0; }
        double avgTotalMicros() { return (avgBgNanos + avgFgNanos) / 1000.0; }
    }

    private final Label fpsLabel;
    private final Label bgLabel;
    private final Label fgLabel;
    private final Label totalLabel;
    private final Label countLabel;
    private final Button reinsertButton;
    private final Button clearButton;
    private final PerfVerticalListView listView;
    private final LayoutGrid layoutGrid;
    private final List<ItemStack> allItems;
    private int itemCount = 0;

    // FPS tracking (lowpass-smoothed like the render timings).
    private long lastFrameNanos = 0;
    private double instFps = 0, avgFps = 0;
    private boolean fpsSeeded = false;

    public ItemPerfTestScreen() {
        super(Component.literal("Item List Perf Test"));

        allItems = ItemUtilities.getAllItems();

        fpsLabel = new Label("(no samples yet)");
        bgLabel = new Label("");
        fgLabel = new Label("");
        totalLabel = new Label("");
        for (Label l : new Label[]{fpsLabel, bgLabel, fgLabel, totalLabel}) {
            l.setAlignment(GuiElement.Alignment.LEFT);
            addElement(l);
        }

        countLabel = new Label("Items: 0");
        countLabel.setAlignment(GuiElement.Alignment.LEFT);
        addElement(countLabel);

        reinsertButton = new Button("Reinsert all items", this::reinsertAll);
        clearButton = new Button("Clear list", this::clearAll);
        addElement(reinsertButton);
        addElement(clearButton);

        listView = new PerfVerticalListView();
        layoutGrid = new LayoutGrid(1, 0, false, false, 0, 0, GuiElement.Alignment.TOP);
        listView.setLayout(layoutGrid);
        addElement(listView);
    }

    public static void open() {
        GuiScreen.setScreen(new ItemPerfTestScreen());
    }

    /** Bulk-insert the full item set again, growing the list linearly. */
    private void reinsertAll() {
        listView.getLayout().enabled = false;
        for (ItemStack stack : allItems) {
            listView.addChild(new ItemView(stack));
        }
        listView.getLayout().enabled = true;
        listView.layoutChangedInternal();
        itemCount += allItems.size();
    }

    private void clearAll() {
        listView.removeChilds();
        itemCount = 0;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.nanoTime();
        if (fpsSeeded && now > lastFrameNanos) {
            instFps = 1_000_000_000.0 / (now - lastFrameNanos);
            avgFps = lowpass(avgFps, instFps, true);
        } else {
            avgFps = instFps;
        }
        lastFrameNanos = now;
        fpsSeeded = true;

        fpsLabel.setText(String.format("FPS %.0f (avg %.0f)", instFps, avgFps));
        bgLabel.setText(String.format("BG %.1fus (avg %.1f)", listView.lastBgMicros(), listView.avgBgMicros()));
        fgLabel.setText(String.format("FG %.1fus (avg %.1f)", listView.lastFgMicros(), listView.avgFgMicros()));
        totalLabel.setText(String.format("Total %.1fus (avg %.1f)", listView.lastTotalMicros(), listView.avgTotalMicros()));
        countLabel.setText("Items: " + itemCount);
        super.render(g, mx, my, pt);
    }

    @Override
    protected void updateLayout(Gui gui) {
        int w = getWidth();
        int h = getHeight();
        int margin = 12;

        int lineH = 14;
        Label[] perfLabels = {fpsLabel, bgLabel, fgLabel, totalLabel};
        for (int i = 0; i < perfLabels.length; i++) {
            perfLabels[i].setBounds(margin, margin + i * lineH, w - margin * 2, lineH);
        }

        int rowY = totalLabel.getBottom() + 6;
        int rowH = 22;
        int buttonW = 150;
        countLabel.setBounds(margin, rowY, w - margin * 2 - buttonW * 2 - margin * 2, rowH);
        reinsertButton.setBounds(w - margin - buttonW * 2 - margin, rowY, buttonW, rowH);
        clearButton.setBounds(w - margin - buttonW, rowY, buttonW, rowH);

        int listTop = rowY + rowH + 8;
        listView.setBounds(margin, listTop, w - margin * 2, h - listTop - margin);
        int cols = listView.getContainerWidth() / ItemView.DEFAULT_WIDTH;
        layoutGrid.columns = Math.max(1, cols);
    }
}
