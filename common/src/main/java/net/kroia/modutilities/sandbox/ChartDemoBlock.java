package net.kroia.modutilities.sandbox;

import com.mojang.serialization.MapCodec;
import net.kroia.modutilities.gui.display.AbstractDisplayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sandbox display block for testing {@link SandboxLineChart} scissor behavior
 * in the offscreen framebuffer renderer.
 */
public class ChartDemoBlock extends AbstractDisplayBlock {

    public static final MapCodec<ChartDemoBlock> CODEC = simpleCodec(p -> new ChartDemoBlock());

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public ChartDemoBlock() {
        super(BlockBehaviour.Properties.of().strength(1.0f).noOcclusion()); // mc<=1.21.1
        //super(BlockBehaviour.Properties.of().strength(1.0f).noOcclusion() // mc>=1.21.2
        //        .setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, // mc>=1.21.2
        //                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.kroia.modutilities.ModUtilitiesMod.MOD_ID, "chart_demo_block")))); // mc>=1.21.2
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChartDemoBlockEntity(pos, state);
    }
}
