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
        //super(BlockBehaviour.Properties.of().strength(2.0f).noOcclusion()); // mc<=1.21.1
        super(BlockBehaviour.Properties.of().strength(2.0f).noOcclusion() // mc>=1.21.2
                .setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, // mc>=1.21.2
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.kroia.modutilities.ModUtilitiesMod.MOD_ID, "display_demo_perf_block")))); // mc>=1.21.2
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayDemoPerfBlockEntity(pos, state);
    }
}
