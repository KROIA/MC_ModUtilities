package net.kroia.modutilities.sandbox;

import net.kroia.modutilities.ColorUtilities;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.kroia.modutilities.gui.geometry.Rectangle;
import net.kroia.modutilities.gui.InputConstants;
import net.minecraft.nbt.CompoundTag;

import java.awt.Point;
import java.util.*;

/**
 * Minimal interactive line chart with scissor-based clipping.
 * Used for testing display block scissor behavior in the offscreen framebuffer.
 * <p>
 * Generates dummy sine/cosine data on construction. Supports pan (drag)
 * and zoom (scroll). Lines are drawn inside a scissor rectangle to test
 * that clipping works correctly in both the interaction screen and the
 * offscreen block face renderer.
 */
public class SandboxLineChart extends GuiElement {

    public static class Series {
        public final String name;
        public final int color;
        public final List<float[]> points = new ArrayList<>();

        public Series(String name, int color) {
            this.name = name;
            this.color = color;
        }
    }

    private static final int COLOR_GRID = 0x20202020;
    private static final int COLOR_FRAME = 0xFF505050;

    private final List<Series> seriesList = new ArrayList<>();
    private final Rectangle canvasRect = new Rectangle(1, 1, 0, 0);
    private final Rectangle canvasScissorRect = new Rectangle(1, 1, 0, 0);

    private float viewX = 0, viewY = -1.5f, viewWidth = 100, viewHeight = 3.0f;

    private final Point lastDragPos = new Point();
    private boolean dragging = false;

    public SandboxLineChart() {
        generateDummyData();
    }

    private void generateDummyData() {
        Series sine = new Series("Sine", 0xFF55AAFF);
        Series cosine = new Series("Cosine", 0xFFFF7755);
        Series square = new Series("Square", 0xFF55FF55);

        for (int i = 0; i < 200; i++) {
            float x = i;
            float phase = (float)(i / 200.0 * Math.PI * 8);
            sine.points.add(new float[]{x, (float) Math.sin(phase)});
            cosine.points.add(new float[]{x, (float) Math.cos(phase)});
            square.points.add(new float[]{x, Math.sin(phase) > 0 ? 0.8f : -0.8f});
        }

        seriesList.add(sine);
        seriesList.add(cosine);
        seriesList.add(square);
    }

    public List<Series> getSeries() {
        return seriesList;
    }

    public void autoCenterView() {
        viewX = 0;
        viewY = -1.5f;
        viewWidth = 100;
        viewHeight = 3.0f;
    }

    // ── Rendering ──

    @Override
    protected void renderBackground() {
        super.renderBackground();
        updateCanvasRect();

        drawFrame(canvasScissorRect, ColorUtilities.getRGB(255,0,0), 3);
        enableScissor(canvasScissorRect);

        // Grid
        int gridLines = 6;
        for (int i = 0; i <= gridLines; i++) {
            float frac = (float) i / gridLines;
            int yPos = canvasRect.y + (int)(frac * canvasRect.height);
            drawRect(canvasRect.x, yPos, canvasRect.width, 1, COLOR_GRID);
            int xPos = canvasRect.x + (int)(frac * canvasRect.width);
            drawRect(xPos, canvasRect.y, 1, canvasRect.height, COLOR_GRID);
        }

        // Series
        for (Series s : seriesList) {
            renderSeries(s);
        }

        disableScissor();

        drawFrame(canvasRect, COLOR_FRAME, 1);

        // Axis labels outside scissor
        String yMin = String.format("%.1f", viewY);
        String yMax = String.format("%.1f", viewY + viewHeight);
        drawText(yMin, canvasRect.x + canvasRect.width + 3,
                canvasRect.y + canvasRect.height - getTextHeight());
        drawText(yMax, canvasRect.x + canvasRect.width + 3, canvasRect.y);
    }

    @Override
    protected void render() {}

    @Override
    protected void layoutChanged() {}

    private void updateCanvasRect() {
        canvasRect.x = 1;
        canvasRect.y = 1;
        canvasRect.width = Math.max(2, getWidth() - 40);
        canvasRect.height = Math.max(2, getHeight() - 4);
        canvasScissorRect.x = canvasRect.x + 1;
        canvasScissorRect.y = canvasRect.y + 1;
        canvasScissorRect.width = Math.max(1, canvasRect.width - 1);
        canvasScissorRect.height = Math.max(1, canvasRect.height - 1);
    }

    private void renderSeries(Series series) {
        if (series.points.size() < 2) return;
        for (int i = 1; i < series.points.size(); i++) {
            float[] prev = series.points.get(i - 1);
            float[] curr = series.points.get(i);
            int x1 = toScreenX(prev[0]);
            int y1 = toScreenY(prev[1]);
            int x2 = toScreenX(curr[0]);
            int y2 = toScreenY(curr[1]);
            drawLine(x1, y1, x2, y2, 1.5f, series.color);
        }
    }

    private int toScreenX(float dataX) {
        if (viewWidth == 0) return canvasRect.x;
        return canvasRect.x + (int)(((dataX - viewX) / viewWidth) * canvasRect.width);
    }

    private int toScreenY(float dataY) {
        if (viewHeight == 0) return canvasRect.y + canvasRect.height;
        return (canvasRect.y + canvasRect.height) - (int)(((dataY - viewY) / viewHeight) * canvasRect.height);
    }

    // ── Input ──

    @Override
    protected boolean mouseScrolledOverElement(double delta) {
        float zoomFactor = (delta > 0) ? 0.9f : 1.1f;
        float cx = viewX + viewWidth * 0.5f;
        float cy = viewY + viewHeight * 0.5f;
        viewWidth = Math.max(1f, viewWidth * zoomFactor);
        viewHeight = Math.max(0.1f, viewHeight * zoomFactor);
        viewX = cx - viewWidth * 0.5f;
        viewY = cy - viewHeight * 0.5f;
        markDirty();
        return true;
    }

    @Override
    protected boolean mouseClickedOverElement(int button) {
        lastDragPos.x = getMouseX();
        lastDragPos.y = getMouseY();
        dragging = true;
        return true;
    }

    @Override
    protected void mouseReleased(int button) {
        dragging = false;
    }

    @Override
    protected boolean mouseDragged(int button, double deltaX, double deltaY) {
        if (!dragging) return false;
        int mx = getMouseX(), my = getMouseY();
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            int dx = lastDragPos.x - mx;
            int dy = lastDragPos.y - my;
            lastDragPos.x = mx;
            lastDragPos.y = my;
            viewX += dx * viewWidth / canvasRect.width;
            viewY -= dy * viewHeight / canvasRect.height;
            markDirty();
            return true;
        }
        return false;
    }

    @Override
    protected boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 32) { // SPACE
            autoCenterView();
            markDirty();
            return true;
        }
        return false;
    }

    // ── Serialization ──

    @Override
    public SyncCategory getSyncCategory() {
        return SyncCategory.INPUT;
    }

    @Override
    public CompoundTag serializeState() {
        CompoundTag tag = super.serializeState();
        tag.putFloat("vx", viewX);
        tag.putFloat("vy", viewY);
        tag.putFloat("vw", viewWidth);
        tag.putFloat("vh", viewHeight);
        return tag;
    }

    @Override
    public void deserializeState(CompoundTag tag) {
        super.deserializeState(tag);
        if (tag.contains("vx")) {
            viewX = tag.getFloat("vx");
            viewY = tag.getFloat("vy");
            viewWidth = tag.getFloat("vw");
            viewHeight = tag.getFloat("vh");
        }
        markDirty();
    }

    @Override
    public List<GuiElement> getSerializableChildren() {
        return List.of();
    }
}
