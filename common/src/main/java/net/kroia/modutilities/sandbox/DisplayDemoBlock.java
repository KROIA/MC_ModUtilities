package net.kroia.modutilities.sandbox;

import com.mojang.serialization.MapCodec;
import net.kroia.modutilities.gui.display.AbstractDisplayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A directional block that hosts a {@link DisplayDemoBlockEntity} for rendering
 * GUI elements on its front face. Extends the reusable {@link AbstractDisplayBlock}
 * which provides facing, interaction, group recalculation, and ticker logic.
 */
public class DisplayDemoBlock extends AbstractDisplayBlock {

    public static final MapCodec<DisplayDemoBlock> CODEC = simpleCodec(p -> new DisplayDemoBlock());

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public DisplayDemoBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0f).noOcclusion());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayDemoBlockEntity(pos, state);
    }
}
