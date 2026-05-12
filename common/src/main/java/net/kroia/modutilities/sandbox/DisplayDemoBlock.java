package net.kroia.modutilities.sandbox;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
