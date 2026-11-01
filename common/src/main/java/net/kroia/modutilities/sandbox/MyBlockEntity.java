package net.kroia.modutilities.sandbox;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
/*
public class MyBlockEntity extends BlockEntity implements MenuProvider {
    public MyBlockEntity(BlockPos pos, BlockState blockState) {
        super(Sandbox.BANK_TERMINAL_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return null;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new MyContainerMenu(pContainerId, pPlayerInventory, this);
    }


    public Container getInventory(UUID playerID) {
        Container inventory = new Container() {
            @Override
            public void clearContent() {

            }

            @Override
            public int getContainerSize() {
                return 0; // Return the size of your inventory
            }

            @Override
            public boolean isEmpty() {
                return true; // Check if the inventory is empty
            }

            @Override
            public ItemStack getItem(int slot) {
                if(slot == 5)
                    return new ItemStack(Item.byBlock(Blocks.IRON_BLOCK), 5); // Example item for slot 5
                return ItemStack.EMPTY; // Return the item in the specified slot
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                return ItemStack.EMPTY; // Remove and return the item from the specified slot
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                return null;
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                // Set the item in the specified slot
            }

            @Override
            public void setChanged() {

            }

            @Override
            public boolean stillValid(Player player) {
                return false;
            }
        };
        return inventory;
    }
}
*/