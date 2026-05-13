package net.kroia.modutilities.gui.display;

import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.display.client.DisplayClientHooks;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public abstract class AbstractDisplayBlockEntity extends BlockEntity {

    private static final double MAX_INTERACTION_DISTANCE_SQ = 64.0;
    private static final long RELEASE_TIMEOUT_MS = 350;

    // Group state
    protected BlockPos controllerPos;
    protected int groupWidth = 0;
    protected int groupHeight = 0;
    protected int gridX = 0;
    protected int gridY = 0;

    // GUI
    protected Gui gui;
    protected int guiBuiltWidth = 0;
    protected int guiBuiltHeight = 0;

    // Interaction state (server-side only)
    private final Set<UUID> interactingPlayers = new HashSet<>();
    private final Map<UUID, double[]> lastMousePos = new HashMap<>();
    private final Set<UUID> mouseDown = new HashSet<>();
    private final Map<UUID, Long> lastUseTime = new HashMap<>();

    // Editor lock
    private UUID editorPlayer = null;

    // Sync
    private boolean needsSync = false;

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    public abstract DisplayConfig getDisplayConfig();

    public abstract ContentBuilder getContentBuilder();

    // -------------------------------------------------------------------------
    // Protected hooks
    // -------------------------------------------------------------------------

    protected void wireCallbacks(Gui gui) {}

    protected void onControllerTick() {}

    protected void saveCustomData(CompoundTag tag, HolderLookup.Provider registries) {}

    protected void loadCustomData(CompoundTag tag, HolderLookup.Provider registries) {}

    public String getChannelId() { return "default"; }

    /**
     * Called after the interaction screen syncs input state into this entity's GUI.
     * Override to read GUI element values back into block entity fields and update
     * dependent elements (e.g. labels that reflect slider values).
     */
    public void onInputSynced() {}

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    protected AbstractDisplayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // -------------------------------------------------------------------------
    // Public API — group state
    // -------------------------------------------------------------------------

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
    public Gui getGui() { return gui; }

    public AbstractDisplayBlockEntity getControllerEntity() {
        if (isController()) return this;
        if (level == null || controllerPos == null) return null;
        BlockEntity be = level.getBlockEntity(controllerPos);
        return be instanceof AbstractDisplayBlockEntity ctrl ? ctrl : null;
    }

    // -------------------------------------------------------------------------
    // Interaction handling (server-side)
    // -------------------------------------------------------------------------

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
                lastMousePos.put(playerId, new double[]{guiX, guiY});
            }
        }
    }

    public void handleMouseRelease(Player player, double guiX, double guiY) {
        if (gui != null && interactingPlayers.contains(player.getUUID())) {
            gui.mouseReleased(guiX, guiY, 0);
            syncToClient();
        }
    }

    public void removeInteractingPlayer(UUID playerId) {
        interactingPlayers.remove(playerId);
        double[] last = lastMousePos.remove(playerId);
        if (gui != null && last != null) {
            gui.mouseReleased(last[0], last[1], 0);
        }
    }

    // -------------------------------------------------------------------------
    // Editor lock
    // -------------------------------------------------------------------------

    public boolean tryAcquireEditor(UUID playerId) {
        if (editorPlayer == null || editorPlayer.equals(playerId)) {
            editorPlayer = playerId;
            return true;
        }
        return false;
    }

    public void releaseEditor(UUID playerId) {
        if (editorPlayer != null && editorPlayer.equals(playerId)) {
            editorPlayer = null;
        }
    }

    public UUID getEditorPlayer() {
        return editorPlayer;
    }

    // -------------------------------------------------------------------------
    // Group management
    // -------------------------------------------------------------------------

    public void setDisabled() {
        this.controllerPos = null;
        this.groupWidth = 0;
        this.groupHeight = 0;
        this.gridX = 0;
        this.gridY = 0;
        this.gui = null;
        this.interactingPlayers.clear();
        this.lastMousePos.clear();
        syncToClient();
    }

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
            DisplayConfig config = getDisplayConfig();
            int totalW = gw * config.virtualWidth();
            int totalH = gh * config.virtualHeight();
            buildAndInitGui(totalW, totalH);
            guiBuiltWidth = totalW;
            guiBuiltHeight = totalH;
        } else {
            this.gui = null;
        }

        syncToClient();
    }

    public void syncToClientPublic() {
        syncToClient();
    }

    // -------------------------------------------------------------------------
    // Server tick
    // -------------------------------------------------------------------------

    public void serverTick() {
        trySyncToClient();
        if (!isActive() || !isController() || gui == null) return;

        updateInteractingPlayers();
        onControllerTick();

        if (gui.hasStructuralChanges()) {
            gui.getAndClearStructuralChanges();
            syncToClient();
        }
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
        if (gui != null && gui.getStructureVersion() > 0) {
            tag.put("guiTree", gui.serializeTree());
        }
        if (gui != null) {
            tag.put("guiInput", GuiInputSerializer.serializeInput(gui));
        }
        saveCustomData(tag, registries);
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
        loadCustomData(tag, registries);

        if (isActive() && isController()) {
            DisplayConfig config = getDisplayConfig();
            int neededW = groupWidth * config.virtualWidth();
            int neededH = groupHeight * config.virtualHeight();
            if (gui == null || guiBuiltWidth != neededW || guiBuiltHeight != neededH) {
                buildAndInitGui(neededW, neededH);
                guiBuiltWidth = neededW;
                guiBuiltHeight = neededH;
            }
            if (tag.contains("guiTree") && tag.getCompound("guiTree").getInt("structureVersion") > 0) {
                gui.deserializeTree(tag.getCompound("guiTree"));
                gui.init();
            }
            if (tag.contains("guiInput")) {
                GuiInputSerializer.applyInput(tag.getCompound("guiInput"), gui);
            }
        } else {
            gui = null;
            guiBuiltWidth = 0;
            guiBuiltHeight = 0;
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

    // -------------------------------------------------------------------------
    // State transfer for group recalculation
    // -------------------------------------------------------------------------

    public CompoundTag captureTransferState() {
        if (level == null) return new CompoundTag();
        CompoundTag tag = new CompoundTag();
        saveCustomData(tag, level.registryAccess());
        return tag;
    }

    public void applyTransferState(CompoundTag state) {
        if (state == null || level == null) return;
        loadCustomData(state, level.registryAccess());
        syncToClient();
    }

    // -------------------------------------------------------------------------
    // Static group recalculation
    // -------------------------------------------------------------------------

    public static void recalculateGroups(Level level, BlockPos changedPos, Direction facing) {
        BlockEntity changedBE = level.getBlockEntity(changedPos);
        String channelId = changedBE instanceof AbstractDisplayBlockEntity adbe ? adbe.getChannelId() : "default";
        recalculateGroups(level, changedPos, facing, channelId, null);
    }

    public static void recalculateGroups(Level level, BlockPos changedPos, Direction facing, CompoundTag inheritState) {
        BlockEntity changedBE = level.getBlockEntity(changedPos);
        String channelId = changedBE instanceof AbstractDisplayBlockEntity adbe ? adbe.getChannelId() : "default";
        recalculateGroups(level, changedPos, facing, channelId, inheritState);
    }

    private static void recalculateGroups(Level level, BlockPos changedPos, Direction facing,
                                           String channelId, CompoundTag inheritState) {
        Set<BlockPos> connected = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(changedPos);

        Direction rightDir = getRightDirection(facing);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (connected.contains(current)) continue;
            BlockEntity be = level.getBlockEntity(current);
            if (!(be instanceof AbstractDisplayBlockEntity adbe)) continue;
            if (!channelId.equals(adbe.getChannelId())) continue;
            BlockState state = level.getBlockState(current);
            if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) continue;
            if (state.getValue(HorizontalDirectionalBlock.FACING) != facing) continue;
            connected.add(current);
            queue.add(current.relative(rightDir));
            queue.add(current.relative(rightDir.getOpposite()));
            queue.add(current.above());
            queue.add(current.below());
        }

        if (connected.isEmpty()) return;

        CompoundTag transferState = null;
        for (BlockPos p : connected) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof AbstractDisplayBlockEntity adbe && adbe.isController() && adbe.gui != null) {
                transferState = adbe.captureTransferState();
                break;
            }
        }
        if (transferState == null) {
            transferState = inheritState;
        }

        BlockPos topLeft = findTopLeft(connected, facing);

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

        Set<BlockPos> inRect = new HashSet<>();
        for (int gy = 0; gy < maxHeight; gy++) {
            for (int gx = 0; gx < maxWidth; gx++) {
                BlockPos memberPos = offsetGrid(topLeft, gx, gy, facing);
                inRect.add(memberPos);
                BlockEntity be = level.getBlockEntity(memberPos);
                if (be instanceof AbstractDisplayBlockEntity member) {
                    member.setGroupInfo(topLeft, maxWidth, maxHeight, gx, gy);
                }
            }
        }

        for (BlockPos p : connected) {
            if (!inRect.contains(p)) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof AbstractDisplayBlockEntity solo) {
                    solo.setDisabled();
                }
            }
        }

        if (transferState != null) {
            BlockEntity newCtrlBE = level.getBlockEntity(topLeft);
            if (newCtrlBE instanceof AbstractDisplayBlockEntity newCtrl) {
                newCtrl.applyTransferState(transferState);
            }
        }
    }

    public static void recalculateNeighborGroups(Level level, BlockPos removedPos, Direction facing) {
        recalculateNeighborGroups(level, removedPos, facing, "default", null);
    }

    public static void recalculateNeighborGroups(Level level, BlockPos removedPos, Direction facing,
                                                  String channelId, CompoundTag inheritState) {
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
            if (be instanceof AbstractDisplayBlockEntity adbe) {
                if (!channelId.equals(adbe.getChannelId())) continue;
                BlockState state = level.getBlockState(neighbor);
                if (state.hasProperty(HorizontalDirectionalBlock.FACING)
                        && state.getValue(HorizontalDirectionalBlock.FACING) == facing) {
                    recalculateGroups(level, neighbor, facing, channelId, inheritState);
                    processed.add(neighbor);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Direction helpers
    // -------------------------------------------------------------------------

    static Direction getRightDirection(Direction facing) {
        return switch (facing) {
            case SOUTH -> Direction.EAST;
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            default -> Direction.EAST;
        };
    }

    static BlockPos offsetGrid(BlockPos origin, int gx, int gy, Direction facing) {
        Direction rightDir = getRightDirection(facing);
        return origin.relative(rightDir, gx).relative(Direction.DOWN, gy);
    }

    // -------------------------------------------------------------------------
    // UV computation helper
    // -------------------------------------------------------------------------

    public static double[] computeGuiCoordsFromHit(BlockHitResult hit, BlockPos blockPos,
                                             Direction facing, AbstractDisplayBlockEntity blockEntity) {
        Vec3 hitPos = hit.getLocation();
        double localX = hitPos.x - blockPos.getX();
        double localY = hitPos.y - blockPos.getY();
        double localZ = hitPos.z - blockPos.getZ();

        double u, v;
        switch (facing) {
            case SOUTH -> { u = localX;       v = 1.0 - localY; }
            case NORTH -> { u = 1.0 - localX; v = 1.0 - localY; }
            case EAST  -> { u = 1.0 - localZ; v = 1.0 - localY; }
            case WEST  -> { u = localZ;        v = 1.0 - localY; }
            default    -> { return null; }
        }

        u = Math.max(0, Math.min(1, u));
        v = Math.max(0, Math.min(1, v));

        int gx = blockEntity.getGridX();
        int gy = blockEntity.getGridY();
        DisplayConfig config = blockEntity.getDisplayConfig();

        double guiX = (gx + u) * config.virtualWidth();
        double guiY = (gy + v) * config.virtualHeight();

        return new double[]{guiX, guiY};
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void buildAndInitGui(int w, int h) {
        gui = new Gui();
        if (level != null && level.isClientSide()) {
            DisplayClientHooks.ensureGraphics();
        }
        gui.setGraphicsBackend(Gui.getFallbackGraphics());
        getContentBuilder().build(gui, w, h);
        wireCallbacks(gui);
        gui.init();
        gui.resetStructureVersion();
        if (level != null && !level.isClientSide()) {
            gui.setTrackStructuralChanges(true);
        }
    }

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

            if (mouseDown.contains(playerId)) {
                Long lastUse = lastUseTime.get(playerId);
                if (lastUse != null && now - lastUse > RELEASE_TIMEOUT_MS) {
                    double[] last = lastMousePos.get(playerId);
                    if (gui != null && last != null) {
                        gui.mouseReleased(last[0], last[1], 0);
                        syncToClient();
                    }
                    mouseDown.remove(playerId);
                }
            }

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

    private double[] raycastToDisplay(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(8.0));

        BlockHitResult hit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        if (hit.getType() != HitResult.Type.BLOCK) return null;

        BlockEntity be = level.getBlockEntity(hit.getBlockPos());
        if (!(be instanceof AbstractDisplayBlockEntity dbe) || !dbe.isActive()) return null;

        if (!getBlockPos().equals(dbe.getControllerPos())) return null;

        Direction facing = level.getBlockState(hit.getBlockPos()).getValue(HorizontalDirectionalBlock.FACING);
        return computeGuiCoordsFromHit(hit, hit.getBlockPos(), facing, dbe);
    }

    private static BlockPos findTopLeft(Set<BlockPos> blocks, Direction facing) {
        Direction rightDir = getRightDirection(facing);
        BlockPos best = null;
        for (BlockPos p : blocks) {
            if (best == null) {
                best = p;
                continue;
            }
            if (p.getY() > best.getY()) {
                best = p;
            } else if (p.getY() == best.getY()) {
                int pCoord = getHorizontalCoord(p, rightDir);
                int bestCoord = getHorizontalCoord(best, rightDir);
                if (pCoord < bestCoord) {
                    best = p;
                }
            }
        }
        return best;
    }

    private static int getHorizontalCoord(BlockPos pos, Direction rightDir) {
        return switch (rightDir) {
            case EAST -> pos.getX();
            case WEST -> -pos.getX();
            case SOUTH -> pos.getZ();
            case NORTH -> -pos.getZ();
            default -> 0;
        };
    }
}
