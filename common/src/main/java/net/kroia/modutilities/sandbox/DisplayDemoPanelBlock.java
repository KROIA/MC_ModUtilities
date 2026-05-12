package net.kroia.modutilities.sandbox;

import com.mojang.serialization.MapCodec;
import net.kroia.modutilities.gui.display.AbstractDisplayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A flat-panel display block that hosts a {@link DisplayDemoPanelBlockEntity}.
 * Uses a thin glass-pane-like shape. Players can walk through the empty space
 * beside the panel.
 */
public class DisplayDemoPanelBlock extends AbstractDisplayBlock {

    private static final VoxelShape PANEL_NS = Block.box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape PANEL_EW = Block.box(7, 0, 0, 9, 16, 16);

    public static final MapCodec<DisplayDemoPanelBlock> CODEC = simpleCodec(p -> new DisplayDemoPanelBlock());

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public DisplayDemoPanelBlock() {
        super(BlockBehaviour.Properties.of().strength(1.0f).noOcclusion());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case NORTH, SOUTH -> PANEL_NS;
            case EAST, WEST -> PANEL_EW;
            default -> PANEL_NS;
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayDemoPanelBlockEntity(pos, state);
    }
}
