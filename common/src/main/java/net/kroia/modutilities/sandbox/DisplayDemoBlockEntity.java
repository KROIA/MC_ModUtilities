package net.kroia.modutilities.sandbox;

import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.elements.Button;
import net.kroia.modutilities.gui.elements.HorizontalSlider;
import net.kroia.modutilities.gui.elements.Label;
import net.kroia.modutilities.gui.elements.Plot;
import net.kroia.modutilities.gui.elements.TextBox;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
    private int guiBuiltWidth = 0;
    private int guiBuiltHeight = 0;
    private Plot plot;
    private Label statusLabel;
    private float time = 0;
    private boolean paused = false;
    private double speed = 0.5;
    private String titleText = "Live Signal Dashboard";

    // Interaction state (server-side only)
    /** Maximum squared distance (in blocks) a player can interact from. 8 blocks = 64 squared. */
    private static final double MAX_INTERACTION_DISTANCE_SQ = 64.0;
    private final Set<UUID> interactingPlayers = new HashSet<>();
    private final Map<UUID, double[]> lastMousePos = new HashMap<>();

    // Editor lock — only one player can have the interaction screen open at a time
    private UUID editorPlayer = null;

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

    /**
     * Returns the controller block entity for this display group.
     * If this block IS the controller, returns {@code this}.
     *
     * @return the controller entity, or {@code null} if the controller is missing/unloaded
     */
    public DisplayDemoBlockEntity getControllerEntity() {
        if (isController()) return this;
        if (level == null || controllerPos == null) return null;
        BlockEntity be = level.getBlockEntity(controllerPos);
        return be instanceof DisplayDemoBlockEntity ctrl ? ctrl : null;
    }

    // -------------------------------------------------------------------------
    // Interaction handling (server-side)
    // -------------------------------------------------------------------------

    /**
     * Handles a player right-click interaction on the display. Each right-click
     * sends a mouse-click event to the GUI at the computed coordinates. The player
     * is also tracked for per-tick raycast hover/drag updates.
     *
     * @param player the interacting player
     * @param guiX   the x coordinate in GUI space
     * @param guiY   the y coordinate in GUI space
     */
    private final Set<UUID> mouseDown = new HashSet<>();
    private final Map<UUID, Long> lastUseTime = new HashMap<>();
    private static final long RELEASE_TIMEOUT_MS = 350;

    public void handleInteraction(Player player, double guiX, double guiY) {
        UUID playerId = player.getUUID();

        if (!interactingPlayers.contains(playerId)) {
            interactingPlayers.add(playerId);
        }

        lastUseTime.put(playerId, System.currentTimeMillis());

        if (gui != null) {
            if (!mouseDown.contains(playerId)) {
                mouseDown.add(playerId);
                gui.mouseClicked(guiX, guiY, 0);
                lastMousePos.put(playerId, new double[]{guiX, guiY});
                syncToClient();
            } else {
                // Held — update position for drag (raycast tracking handles the actual drag)
                lastMousePos.put(playerId, new double[]{guiX, guiY});
            }
        }
    }

    /**
     * Handles a mouse-release event for the given player.
     *
     * @param player the player releasing the mouse
     * @param guiX   the x coordinate in GUI space
     * @param guiY   the y coordinate in GUI space
     */
    public void handleMouseRelease(Player player, double guiX, double guiY) {
        if (gui != null && interactingPlayers.contains(player.getUUID())) {
            gui.mouseReleased(guiX, guiY, 0);
            syncToClient();
        }
    }

    /**
     * Removes a player from interaction tracking. Fires a mouse-release event
     * to clean up any pressed/dragged state on GUI elements.
     *
     * @param playerId the UUID of the player to remove
     */
    public void removeInteractingPlayer(UUID playerId) {
        interactingPlayers.remove(playerId);
        double[] last = lastMousePos.remove(playerId);
        if (gui != null && last != null) {
            gui.mouseReleased(last[0], last[1], 0);
        }
    }

    public void setDisabled() {
        this.controllerPos = null;
        this.groupWidth = 0;
        this.groupHeight = 0;
        this.gridX = 0;
        this.gridY = 0;
        this.gui = null;
        this.plot = null;
        this.statusLabel = null;
        this.paused = false;
        this.interactingPlayers.clear();
        this.lastMousePos.clear();
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
            buildDashboard(gui, totalW, totalH, this);
            gui.init();
            capturePlot();
            guiBuiltWidth = totalW;
            guiBuiltHeight = totalH;
            syncStateToGui();
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
        tag.putBoolean("paused", paused);
        tag.putDouble("speed", speed);
        tag.putString("titleText", titleText);
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
        paused = tag.getBoolean("paused");
        speed = tag.getDouble("speed");
        if (tag.contains("titleText")) {
            titleText = tag.getString("titleText");
        }

        if (isActive() && isController()) {
            int neededW = groupWidth * VIRTUAL_WIDTH;
            int neededH = groupHeight * VIRTUAL_HEIGHT;
            if (gui == null || guiBuiltWidth != neededW || guiBuiltHeight != neededH) {
                gui = new Gui();
                buildDashboard(gui, neededW, neededH, this);
                gui.init();
                capturePlot();
                guiBuiltWidth = neededW;
                guiBuiltHeight = neededH;
            }
        } else {
            gui = null;
            plot = null;
            guiBuiltWidth = 0;
            guiBuiltHeight = 0;
        }

        syncStateToGui();
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
     * Server tick. Only the controller ticks — tracks interacting players
     * and animates the plot data (unless paused).
     */
    public void serverTick() {
        trySyncToClient(); // sync runs for ALL blocks, not just controllers
        if (!isActive() || !isController() || gui == null) return;

        // Track interacting players' look direction for hover/drag
        updateInteractingPlayers();

        // Skip plot animation when paused
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

    /**
     * Raycasts from each interacting player's eye position to the display face.
     * Updates stored mouse position and fires drag events if the cursor has moved.
     * Removes players who are too far away or no longer online.
     */
    private void updateInteractingPlayers() {
        if (level == null || interactingPlayers.isEmpty()) return;

        long now = System.currentTimeMillis();
        Iterator<UUID> it = interactingPlayers.iterator();
        while (it.hasNext()) {
            UUID playerId = it.next();
            Player player = level.getPlayerByUUID(playerId);
            if (player == null || player.distanceToSqr(Vec3.atCenterOf(getBlockPos())) > MAX_INTERACTION_DISTANCE_SQ) {
                releasePlayer(playerId, it);
                continue;
            }

            // Detect right-click release: useWithoutItem stops calling handleInteraction
            if (mouseDown.contains(playerId)) {
                Long lastUse = lastUseTime.get(playerId);
                if (lastUse != null && now - lastUse > RELEASE_TIMEOUT_MS) {
                    // Player released right-click — fire mouseReleased
                    double[] last = lastMousePos.get(playerId);
                    if (gui != null && last != null) {
                        gui.mouseReleased(last[0], last[1], 0);
                        syncToClient();
                    }
                    mouseDown.remove(playerId);
                }
            }

            // Raycast from the player's eye to the display face
            double[] guiCoords = raycastToDisplay(player);
            if (guiCoords != null) {
                gui.storeMousePos((int) guiCoords[0], (int) guiCoords[1]);
                if (mouseDown.contains(playerId)) {
                    double[] prev = lastMousePos.get(playerId);
                    if (prev != null) {
                        double dx = guiCoords[0] - prev[0];
                        double dy = guiCoords[1] - prev[1];
                        if (dx != 0 || dy != 0) {
                            gui.mouseDragged(guiCoords[0], guiCoords[1], 0, dx, dy);
                            syncToClient();
                        }
                    }
                }
                lastMousePos.put(playerId, guiCoords);
            }
        }
    }

    private void releasePlayer(UUID playerId, Iterator<UUID> it) {
        it.remove();
        double[] last = lastMousePos.remove(playerId);
        if (gui != null && last != null) {
            gui.mouseReleased(last[0], last[1], 0);
            syncToClient();
        }
        mouseDown.remove(playerId);
        lastUseTime.remove(playerId);
    }

    /**
     * Raycasts from the player's eye in the look direction, checking if the ray
     * hits a block in this display group. Returns GUI coordinates if it does.
     *
     * @param player the player to raycast from
     * @return 2-element array {guiX, guiY}, or {@code null} if not looking at this display
     */
    private double[] raycastToDisplay(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(8.0)); // 8 block reach

        BlockHitResult hit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        if (hit.getType() != HitResult.Type.BLOCK) return null;

        BlockEntity be = level.getBlockEntity(hit.getBlockPos());
        if (!(be instanceof DisplayDemoBlockEntity dbe) || !dbe.isActive()) return null;

        // Verify this block belongs to OUR display group
        if (!getBlockPos().equals(dbe.getControllerPos())) return null;

        Direction facing = level.getBlockState(hit.getBlockPos()).getValue(HorizontalDirectionalBlock.FACING);
        return DisplayDemoBlock.computeGuiCoords(hit, hit.getBlockPos(), facing, dbe);
    }

    public Gui getGui() { return gui; }

    /**
     * Reads interactive state from the Gui elements back into the block entity fields.
     * Called after GuiStateSync copies client input into the server Gui.
     */
    public void readStateFromGui() {
        if (gui == null) return;
        for (var el : gui.getElements()) {
            if (el instanceof HorizontalSlider slider) {
                speed = slider.getSliderValue();
            }
            if (el instanceof TextBox tb) {
                titleText = tb.getText();
            }
        }
    }

    /**
     * Toggles the paused state of the plot animation. Updates the status label
     * and button text to reflect the current state.
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

    public void handleTextInput(String text) {
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
        syncToClient();
    }

    // -------------------------------------------------------------------------
    // Editor lock — only one player can interact via the screen at a time
    // -------------------------------------------------------------------------

    /**
     * Attempts to acquire the editor lock for the given player.
     * Only one player can hold the lock at a time.
     *
     * @param playerId the UUID of the player requesting the lock
     * @return {@code true} if the lock was acquired (or was already held by this player)
     */
    public boolean tryAcquireEditor(UUID playerId) {
        if (editorPlayer == null || editorPlayer.equals(playerId)) {
            editorPlayer = playerId;
            return true;
        }
        return false;
    }

    /**
     * Releases the editor lock if held by the given player.
     *
     * @param playerId the UUID of the player releasing the lock
     */
    public void releaseEditor(UUID playerId) {
        if (editorPlayer != null && editorPlayer.equals(playerId)) {
            editorPlayer = null;
        }
    }

    /**
     * @return the UUID of the player currently holding the editor lock,
     *         or {@code null} if no player is editing
     */
    public UUID getEditorPlayer() {
        return editorPlayer;
    }

    /**
     * Public wrapper for {@link #syncToClient()} so external classes
     * (e.g. DisplayInteractionScreen) can trigger a client sync.
     */
    public void syncToClientPublic() {
        syncToClient();
    }

    void togglePaused() {
        paused = !paused;
        net.kroia.modutilities.ModUtilitiesMod.LOGGER.info(
                "[DisplayBlock] togglePaused -> paused={}", paused);
        syncToClient();
    }

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
        recalculateGroups(level, changedPos, facing, null);
    }

    public static void recalculateGroups(Level level, BlockPos changedPos, Direction facing, DisplayState inheritState) {
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

        DisplayState state = null;
        for (BlockPos p : connected) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof DisplayDemoBlockEntity dbe && dbe.isController() && dbe.gui != null) {
                state = dbe.captureState();
                break;
            }
        }
        if (state == null) {
            state = inheritState;
        }

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

        if (state != null) {
            BlockEntity newCtrlBE = level.getBlockEntity(topLeft);
            if (newCtrlBE instanceof DisplayDemoBlockEntity newCtrl) {
                newCtrl.applyState(state);
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
        recalculateNeighborGroups(level, removedPos, facing, null);
    }

    public static void recalculateNeighborGroups(Level level, BlockPos removedPos, Direction facing, DisplayState inheritState) {
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
                    recalculateGroups(level, neighbor, facing, inheritState);
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
     * Builds the dashboard layout at the given resolution. When {@code owner}
     * is non-null, callbacks (pause, speed slider) are wired to mutate the
     * block entity state directly. When {@code owner} is null, callbacks only
     * update local GUI elements (labels) — suitable for the interaction screen
     * where state is synced via {@link net.kroia.modutilities.gui.GuiStateSync}.
     *
     * @param gui   the Gui to populate with elements
     * @param w     the total width in GUI pixels
     * @param h     the total height in GUI pixels
     * @param owner the owning block entity, or {@code null} for standalone use
     */
    public static void buildDashboard(Gui gui, int w, int h, DisplayDemoBlockEntity owner) {
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
        statusLabel.setAlignment(GuiElement.Alignment.CENTER);
        gui.addElement(statusLabel);

        int controlY = plotTop + plotHeight + 18;

        Label speedLabel = new Label("Speed: 50%");
        speedLabel.setBounds(margin, controlY, 60, 14);
        speedLabel.setAlignment(GuiElement.Alignment.LEFT);
        gui.addElement(speedLabel);

        HorizontalSlider speedSlider = new HorizontalSlider(
                margin + 62, controlY, w - margin * 2 - 62 - 70, 14);
        speedSlider.setSliderValue(0.5);
        speedSlider.setOnValueChanged(value -> {
            if (owner != null) {
                owner.speed = value;
                owner.syncToClient();
            }
            speedLabel.setText("Speed: " + (int)(value * 100) + "%");
        });
        gui.addElement(speedSlider);

        Button pauseButton = new Button("Pause", () -> {
            if (owner != null) {
                owner.togglePaused();
            }
        });
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
        titleInput.setOnTextChanged(text -> {
            title.setText(text);
            echoLabel.setText(text);
            if (owner != null) {
                owner.syncToClient();
            }
        });
        gui.addElement(titleInput);

        if (owner != null) {
            owner.statusLabel = statusLabel;
        }
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

    // -------------------------------------------------------------------------
    // State transfer
    // -------------------------------------------------------------------------

    public record DisplayState(double speed, String titleText, boolean paused, float time) {}

    public DisplayState captureState() {
        return new DisplayState(speed, titleText, paused, time);
    }

    public void applyState(DisplayState state) {
        if (state == null) return;
        this.speed = state.speed;
        this.titleText = state.titleText;
        this.paused = state.paused;
        this.time = state.time;
        syncStateToGui();
        syncToClient();
    }
}
