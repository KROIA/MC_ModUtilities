package net.kroia.modutilities.sandbox;

import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.Plot;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Block entity for the DisplayDemoBlock. Supports multi-block grouping:
 * adjacent same-facing DisplayDemoBlocks merge into one large rectangular
 * display. The top-left block (the "controller") owns the Gui at
 * (groupWidth * VIRTUAL_WIDTH, groupHeight * VIRTUAL_HEIGHT) resolution
 * and ticks the animation. Non-controller blocks store the controller's
 * position and delegate rendering.
 */
public class DisplayDemoBlockEntity extends BlockEntity {

    public static final int VIRTUAL_WIDTH = 256;
    public static final int VIRTUAL_HEIGHT = 256;
    private static final int SAMPLE_COUNT = 100;

    // Group state
    private BlockPos controllerPos;
    private int groupWidth = 0;
    private int groupHeight = 0;
    private int gridX = 0;
    private int gridY = 0;

    // Only the controller has a Gui
    private Gui gui;
    private Plot plot;
    private float time = 0;

    public DisplayDemoBlockEntity(BlockPos pos, BlockState blockState) {
        super(SandboxRegistration.DISPLAY_DEMO_BLOCK_ENTITY.get(), pos, blockState);
    }

    public boolean isController() {
        return controllerPos != null && controllerPos.equals(getBlockPos());
    }

    public boolean isActive() {
        return controllerPos != null && groupWidth > 0 && groupHeight > 0;
    }

    public BlockPos getControllerPos() { return controllerPos; }
    public int getGroupWidth() { return groupWidth; }
    public int getGroupHeight() { return groupHeight; }
    public int getGridX() { return gridX; }
    public int getGridY() { return gridY; }

    public void setDisabled() {
        this.controllerPos = null;
        this.groupWidth = 0;
        this.groupHeight = 0;
        this.gridX = 0;
        this.gridY = 0;
        this.gui = null;
        this.plot = null;
        syncToClient();
    }

    /**
     * Called when the group is recalculated. Updates this block's group metadata
     * and rebuilds the GUI if this block is (or becomes) the controller.
     *
     * @param controller the controller block position (top-left of the group)
     * @param gw         group width in blocks
     * @param gh         group height in blocks
     * @param gx         this block's grid x within the group (0-based)
     * @param gy         this block's grid y within the group (0-based)
     */
    public void setGroupInfo(BlockPos controller, int gw, int gh, int gx, int gy) {
        boolean changed = this.groupWidth != gw || this.groupHeight != gh
                || this.gridX != gx || this.gridY != gy
                || (this.controllerPos == null) != (controller == null)
                || (this.controllerPos != null && !this.controllerPos.equals(controller));

        if (!changed) return;

        this.controllerPos = controller;
        this.groupWidth = gw;
        this.groupHeight = gh;
        this.gridX = gx;
        this.gridY = gy;

        if (isController()) {
            int totalW = gw * VIRTUAL_WIDTH;
            int totalH = gh * VIRTUAL_HEIGHT;
            this.gui = new Gui();
            buildDashboard(gui, totalW, totalH);
            capturePlot();
        } else {
            this.gui = null;
            this.plot = null;
        }

        syncToClient();
    }

    // -------------------------------------------------------------------------
    // NBT save/load + client sync
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controllerPos != null) {
            tag.putInt("ctrlX", controllerPos.getX());
            tag.putInt("ctrlY", controllerPos.getY());
            tag.putInt("ctrlZ", controllerPos.getZ());
        }
        tag.putInt("gw", groupWidth);
        tag.putInt("gh", groupHeight);
        tag.putInt("gx", gridX);
        tag.putInt("gy", gridY);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ctrlX")) {
            controllerPos = new BlockPos(tag.getInt("ctrlX"), tag.getInt("ctrlY"), tag.getInt("ctrlZ"));
        }
        groupWidth = tag.getInt("gw");
        groupHeight = tag.getInt("gh");
        gridX = tag.getInt("gx");
        gridY = tag.getInt("gy");

        if (isActive() && isController()) {
            gui = new Gui();
            buildDashboard(gui, groupWidth * VIRTUAL_WIDTH, groupHeight * VIRTUAL_HEIGHT);
            capturePlot();
        } else {
            gui = null;
            plot = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Notifies clients of a state change by marking dirty and sending a block update.
     */
    private boolean needsSync = false;

    private void syncToClient() {
        if (level != null && !level.isClientSide() && !isRemoved()) {
            setChanged();
            needsSync = true;
        }
    }

    private void trySyncToClient() {
        if (needsSync && level != null && !level.isClientSide() && !isRemoved()) {
            needsSync = false;
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
        }
    }

    /**
     * Server tick. Only the controller ticks — animates the plot data.
     */
    public void serverTick() {
        trySyncToClient(); // sync runs for ALL blocks, not just controllers
        if (!isActive() || !isController() || gui == null) return;

        time += 0.05f;
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

    public Gui getGui() { return gui; }

    // -------------------------------------------------------------------------
    // Group recalculation (static)
    // -------------------------------------------------------------------------

    /**
     * Recalculates the display group containing the block at {@code changedPos}.
     * Flood-fills to find all connected same-facing DisplayDemoBlocks, determines
     * the top-left (controller), finds the largest rectangle anchored there, and
     * updates all block entities.
     *
     * @param level      the world
     * @param changedPos the position that triggered the recalculation
     * @param facing     the facing direction of the blocks in the group
     */
    public static void recalculateGroups(Level level, BlockPos changedPos, Direction facing) {
        // 1. Flood-fill to collect all connected same-facing DisplayDemoBlocks
        Set<BlockPos> connected = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(changedPos);

        Direction rightDir = getRightDirection(facing);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (connected.contains(current)) continue;
            BlockEntity be = level.getBlockEntity(current);
            if (!(be instanceof DisplayDemoBlockEntity)) continue;
            BlockState state = level.getBlockState(current);
            if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) continue;
            if (state.getValue(HorizontalDirectionalBlock.FACING) != facing) continue;
            connected.add(current);
            // Check neighbors on the display plane (right, left, up, down)
            queue.add(current.relative(rightDir));
            queue.add(current.relative(rightDir.getOpposite()));
            queue.add(current.above());
            queue.add(current.below());
        }

        if (connected.isEmpty()) return;

        // 2. Find the top-left block (controller)
        BlockPos topLeft = findTopLeft(connected, facing);

        // 3. Find largest rectangle from top-left by expanding right then down
        int maxWidth = 1;
        while (connected.contains(offsetGrid(topLeft, maxWidth, 0, facing))) {
            maxWidth++;
        }

        int maxHeight = 1;
        outer:
        while (true) {
            for (int x = 0; x < maxWidth; x++) {
                if (!connected.contains(offsetGrid(topLeft, x, maxHeight, facing))) {
                    break outer;
                }
            }
            maxHeight++;
        }

        // 4. Update all blocks in the rectangle
        Set<BlockPos> inRect = new HashSet<>();
        for (int gy = 0; gy < maxHeight; gy++) {
            for (int gx = 0; gx < maxWidth; gx++) {
                BlockPos memberPos = offsetGrid(topLeft, gx, gy, facing);
                inRect.add(memberPos);
                BlockEntity be = level.getBlockEntity(memberPos);
                if (be instanceof DisplayDemoBlockEntity member) {
                    member.setGroupInfo(topLeft, maxWidth, maxHeight, gx, gy);
                }
            }
        }

        // 5. Connected blocks outside the rectangle are disabled (show nothing)
        for (BlockPos p : connected) {
            if (!inRect.contains(p)) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof DisplayDemoBlockEntity solo) {
                    solo.setDisabled();
                }
            }
        }
    }

    /**
     * Recalculates groups for all neighbors of a removed block position.
     * Called from {@link DisplayDemoBlock#onRemove} after the block entity has been
     * removed, so the removed position itself is no longer valid.
     *
     * @param level      the world
     * @param removedPos the position that was removed
     * @param facing     the facing direction of the removed block
     */
    public static void recalculateNeighborGroups(Level level, BlockPos removedPos, Direction facing) {
        Direction rightDir = getRightDirection(facing);
        BlockPos[] neighbors = {
                removedPos.relative(rightDir),
                removedPos.relative(rightDir.getOpposite()),
                removedPos.above(),
                removedPos.below()
        };
        Set<BlockPos> processed = new HashSet<>();
        for (BlockPos neighbor : neighbors) {
            if (processed.contains(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof DisplayDemoBlockEntity) {
                BlockState state = level.getBlockState(neighbor);
                if (state.hasProperty(HorizontalDirectionalBlock.FACING)
                        && state.getValue(HorizontalDirectionalBlock.FACING) == facing) {
                    recalculateGroups(level, neighbor, facing);
                    processed.add(neighbor);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Direction helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the "right" direction when looking at the display face from the front.
     * <ul>
     *   <li>NORTH/SOUTH: right = EAST (+X)</li>
     *   <li>EAST: right = NORTH (-Z)</li>
     *   <li>WEST: right = SOUTH (+Z)</li>
     * </ul>
     */
    static Direction getRightDirection(Direction facing) {
        return switch (facing) {
            case SOUTH -> Direction.EAST;
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            default -> Direction.EAST;
        };
    }

    /**
     * Converts a grid coordinate (gx, gy) to a world {@link BlockPos}, where gx
     * goes "right" and gy goes "down" relative to the display face orientation.
     */
    static BlockPos offsetGrid(BlockPos origin, int gx, int gy, Direction facing) {
        Direction rightDir = getRightDirection(facing);
        return origin.relative(rightDir, gx).relative(Direction.DOWN, gy);
    }

    /**
     * Finds the top-left block in the connected set for the given facing direction.
     * "Top" = max Y. "Left" = smallest coordinate in the "right" axis direction.
     */
    private static BlockPos findTopLeft(Set<BlockPos> blocks, Direction facing) {
        Direction rightDir = getRightDirection(facing);
        BlockPos best = null;
        for (BlockPos p : blocks) {
            if (best == null) {
                best = p;
                continue;
            }
            // Higher Y is better (top)
            if (p.getY() > best.getY()) {
                best = p;
            } else if (p.getY() == best.getY()) {
                // More "left" (smallest coord in the right-axis) is better
                int pCoord = getHorizontalCoord(p, rightDir);
                int bestCoord = getHorizontalCoord(best, rightDir);
                if (pCoord < bestCoord) {
                    best = p;
                }
            }
        }
        return best;
    }

    /**
     * Returns the horizontal coordinate of a position along the "right" axis.
     * For directions that go in the negative axis (e.g. NORTH = -Z), the
     * coordinate is negated so that "more left" always means "smaller value".
     */
    private static int getHorizontalCoord(BlockPos pos, Direction rightDir) {
        return switch (rightDir) {
            case EAST -> pos.getX();
            case WEST -> -pos.getX();
            case SOUTH -> pos.getZ();
            case NORTH -> -pos.getZ();
            default -> 0;
        };
    }

    // -------------------------------------------------------------------------
    // Dashboard builder
    // -------------------------------------------------------------------------

    /**
     * Builds the dashboard layout (same as ExampleDashboardScreen) at the given
     * resolution.
     */
    public static void buildDashboard(Gui gui, int w, int h) {
        int margin = 10;

        Label title = new Label("Live Signal Dashboard");
        title.setBounds(0, margin, w, 16);
        title.setAlignment(GuiElement.Alignment.CENTER);
        gui.addElement(title);

        int plotTop = margin + 16 + 8;
        int controlsHeight = 50;
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
        statusLabel.setBounds(0, plotTop + plotHeight + 6, w, 14);
        statusLabel.setAlignment(GuiElement.Alignment.CENTER);
        gui.addElement(statusLabel);

        Button pauseButton = new Button("Pause");
        pauseButton.setBounds(w / 2 - 60, plotTop + plotHeight + 24, 120, 22);
        gui.addElement(pauseButton);
    }

    private void capturePlot() {
        if (gui == null) return;
        for (var el : gui.getElements()) {
            if (el instanceof Plot p) {
                plot = p;
                return;
            }
        }
        plot = null;
    }
}
