package net.kroia.modutilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;

public interface PlatformAbstraction {
    ItemStack getItemStack(String itemID);
    String getItemID(Item item);
    ArrayList<ItemStack> getAllItems();
    MinecraftServer getServer();
    UtilitiesPlatform.Type getPlatformType();
}
