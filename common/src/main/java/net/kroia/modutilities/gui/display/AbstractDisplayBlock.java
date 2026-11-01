package net.kroia.modutilities.gui.display;

import com.mojang.serialization.MapCodec;
import net.kroia.modutilities.gui.display.client.DisplayClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Base block class for display blocks. Handles horizontal facing, player interaction
 * (editor lock + interaction screen), and multi-block group recalculation on
 * place/remove. Subclasses only need to implement {@link #newBlockEntity} and {@link #codec()}.
 */
public abstract class AbstractDisplayBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private static final VoxelShape FALLBACK_SHAPE = Block.box(0.2, 0.2, 0.2, 15.8, 15.8, 15.8);

    protected AbstractDisplayBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected abstract MapCodec<? extends HorizontalDirectionalBlock> codec();

    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractDisplayBlockEntity displayBE) {
                Direction facing = state.getValue(FACING);
                return displayBE.getDisplayConfig().shapeProvider().getShape(facing);
            }
        }
        return FALLBACK_SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // -------------------------------------------------------------------------
    // Player interaction
    // -------------------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AbstractDisplayBlockEntity displayBE) || !displayBE.isActive())
            return InteractionResult.PASS;

        AbstractDisplayBlockEntity controller = displayBE.getControllerEntity();
        if (controller == null || controller.getGui() == null)
            return InteractionResult.PASS;

        if (!controller.opensSyncedScreenOnUse())
            return InteractionResult.PASS;

        if (!level.isClientSide()) {
            if (!controller.tryAcquireEditor(player.getUUID())) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Display is being edited by another player."),
                        true);
                return InteractionResult.FAIL;
            }
        } else {
            openInteractionScreen(pos.immutable());
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Opens the interaction screen on the client.
     * Only called when {@code level.isClientSide()} is true.
     */
    protected void openInteractionScreen(BlockPos pos) {
        DisplayClientHooks.openInteractionScreen(pos);
    }

    // -------------------------------------------------------------------------
    // UV computation
    // -------------------------------------------------------------------------

    public static double[] computeGuiCoords(BlockHitResult hit, BlockPos blockPos,
                                            Direction facing, AbstractDisplayBlockEntity blockEntity) {
        return AbstractDisplayBlockEntity.computeGuiCoordsFromHit(hit, blockPos, facing, blockEntity);
    }

    // -------------------------------------------------------------------------
    // Block entity ticker
    // -------------------------------------------------------------------------

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof AbstractDisplayBlockEntity displayBE) {
                displayBE.serverTick();
            }
        };
    }

    // -------------------------------------------------------------------------
    // Placement / removal — trigger group recalculation
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            AbstractDisplayBlockEntity.recalculateGroups(level, pos, state.getValue(FACING));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(state.getBlock())) {
            Direction facing = state.getValue(FACING);
            CompoundTag capturedState = null;
            String channelId = null;
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof AbstractDisplayBlockEntity dbe) {
                    channelId = dbe.getChannelId();
                    AbstractDisplayBlockEntity ctrl = dbe.getControllerEntity();
                    if (ctrl != null) {
                        capturedState = ctrl.captureTransferState();
                    }
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
            if (!level.isClientSide()) {
                try {
                    if (channelId != null) {
                        AbstractDisplayBlockEntity.recalculateNeighborGroups(level, pos, facing, channelId, capturedState);
                    } else {
                        AbstractDisplayBlockEntity.recalculateNeighborGroups(level, pos, facing);
                    }
                } catch (Exception e) {
                    net.kroia.modutilities.ModUtilitiesMod.LOGGER.warn("Failed to recalculate display groups on removal", e);
                }
            }
        } else {
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
