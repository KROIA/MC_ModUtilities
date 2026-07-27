package net.kroia.modutilities.sandbox;

import com.mojang.serialization.MapCodec;
import net.kroia.modutilities.gui.display.AbstractDisplayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Perf-tuned variant of DisplayDemoBlock &mdash; uses renderInterval + maxRenderDistance
 * for Task #94 verification. Place next to a DisplayDemoBlock to A/B compare in-world.
 */
public class DisplayDemoPerfBlock extends AbstractDisplayBlock {

    public static final MapCodec<DisplayDemoPerfBlock> CODEC = simpleCodec(p -> new DisplayDemoPerfBlock());

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public DisplayDemoPerfBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0f).noOcclusion());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayDemoPerfBlockEntity(pos, state);
    }
}
