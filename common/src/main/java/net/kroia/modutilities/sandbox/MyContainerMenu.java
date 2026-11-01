package net.kroia.modutilities.sandbox;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.rmi.registry.Registry;
/*
public class MyContainerMenu extends AbstractContainerMenu
{


    public MyContainerMenu(int containerId, Inventory playerInv, FriendlyByteBuf additionalData) {
        this(   containerId,
                playerInv,
                (MyBlockEntity) playerInv.player.level().getBlockEntity(additionalData.readBlockPos()));
    }
    public MyContainerMenu(int containerId, Inventory playerInv, MyBlockEntity blockEntity) {
        super(Sandbox.MY_CONTAINER_MENU.get(), containerId);


    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }
}
*/