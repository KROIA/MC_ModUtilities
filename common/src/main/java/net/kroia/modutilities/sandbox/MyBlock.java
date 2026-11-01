package net.kroia.modutilities.sandbox;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.architectury.registry.menu.MenuRegistry.openExtendedMenu;
/*
public class MyBlock extends Block implements EntityBlock {
    public MyBlock() {
        super(Properties.copy(net.minecraft.world.level.block.Blocks.WHITE_WOOL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return Sandbox.BANK_TERMINAL_BLOCK_ENTITY.get().create(pos, state);
    }


    @Override
    public final @NotNull InteractionResult use(@NotNull BlockState state,
                                                @NotNull Level level,
                                                @NotNull BlockPos pos,
                                                @NotNull Player player,
                                                @NotNull InteractionHand hand,
                                                @NotNull BlockHitResult hit) {



        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MyBlockEntity blockEntity))
            return InteractionResult.SUCCESS;

        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        // open screen
        if (player instanceof ServerPlayer sPlayer) {
            MenuProvider menuProvider = blockEntity;
            // Open the menu
            openExtendedMenu(sPlayer, menuProvider, (menu) -> {
                // Set the block position
                menu.writeBlockPos(pos);
            });
        }
        return InteractionResult.SUCCESS;
    }
}
*/