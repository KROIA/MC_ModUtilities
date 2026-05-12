package net.kroia.modutilities.sandbox;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A directional block that hosts a {@link DisplayDemoBlockEntity} for rendering
 * GUI elements on its front face. This is a sandbox proof-of-concept for the
 * DisplayBlock feature.
 * <p>
 * The block faces the player on placement (like a furnace) using the
 * {@link HorizontalDirectionalBlock#FACING} property.
 * <p>
 * Adjacent same-facing DisplayDemoBlocks automatically merge into a single
 * large display group. Group recalculation is triggered on placement and removal.
 */
public class DisplayDemoBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private static final VoxelShape INSET_SHAPE = Block.box(0.2, 0.2, 0.2, 15.8, 15.8, 15.8);

    public static final MapCodec<DisplayDemoBlock> CODEC = simpleCodec(p -> new DisplayDemoBlock());

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public DisplayDemoBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.0f)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return INSET_SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Face toward the player (opposite of the player's look direction)
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // -------------------------------------------------------------------------
    // Player interaction — right-click opens an interaction screen
    // -------------------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DisplayDemoBlockEntity displayBE) || !displayBE.isActive())
            return InteractionResult.PASS;

        // Find the controller that owns the Gui
        DisplayDemoBlockEntity controller = displayBE.getControllerEntity();
        if (controller == null || controller.getGui() == null)
            return InteractionResult.PASS;

        if (!level.isClientSide()) {
            // Server: try to acquire the editor lock
            if (!controller.tryAcquireEditor(player.getUUID())) {
                // Another player is already editing
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Display is being edited by another player."),
                        true);
                return InteractionResult.FAIL;
            }
        } else {
            // Client: open the interaction screen — pass the clicked block pos,
            // the screen will find the server controller from there
            SandboxClientHooks.openDisplayInteractionScreen(pos);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Converts a 3D {@link BlockHitResult} into 2D GUI coordinates on the display.
     * <p>
     * The hit position is projected onto the block face to get UV coordinates [0..1],
     * then scaled by the block's grid position and virtual resolution to produce
     * pixel coordinates in the controller's GUI space.
     *
     * @param hit         the block hit result from the player interaction
     * @param blockPos    the position of the hit block
     * @param facing      the direction the display face is pointing
     * @param blockEntity the block entity at the hit position
     * @return a 2-element array {guiX, guiY}, or {@code null} if the facing is unsupported
     */
    public static double[] computeGuiCoords(BlockHitResult hit, BlockPos blockPos,
                                            Direction facing, DisplayDemoBlockEntity blockEntity) {
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

        // Clamp to [0, 1]
        u = Math.max(0, Math.min(1, u));
        v = Math.max(0, Math.min(1, v));

        // Convert to GUI pixel coordinates:
        // Each block covers VIRTUAL_WIDTH x VIRTUAL_HEIGHT pixels in the GUI.
        // The block at grid (gx, gy) maps to the region starting at
        // (gx * VIRTUAL_WIDTH, gy * VIRTUAL_HEIGHT).
        int gx = blockEntity.getGridX();
        int gy = blockEntity.getGridY();

        double guiX = (gx + u) * DisplayDemoBlockEntity.VIRTUAL_WIDTH;
        double guiY = (gy + v) * DisplayDemoBlockEntity.VIRTUAL_HEIGHT;

        return new double[]{guiX, guiY};
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayDemoBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof DisplayDemoBlockEntity displayBE) {
                displayBE.serverTick();
            }
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            DisplayDemoBlockEntity.recalculateGroups(level, pos, state.getValue(FACING));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(state.getBlock())) {
            Direction facing = state.getValue(FACING);
            super.onRemove(state, level, pos, newState, movedByPiston);
            if (!level.isClientSide()) {
                try {
                    DisplayDemoBlockEntity.recalculateNeighborGroups(level, pos, facing);
                } catch (Exception e) {
                    net.kroia.modutilities.ModUtilitiesMod.LOGGER.warn("Failed to recalculate display groups on removal", e);
                }
            }
        } else {
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
