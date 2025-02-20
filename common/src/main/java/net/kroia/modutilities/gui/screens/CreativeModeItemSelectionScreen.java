package net.kroia.modutilities.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public class CreativeModeItemSelectionScreen extends CreativeModeInventoryScreen {
    Consumer<ItemStack> onItemClicked;
    public CreativeModeItemSelectionScreen(Player player, FeatureFlagSet enabledFeatures, boolean displayOperatorCreativeTab, Consumer<ItemStack> onItemClicked) {
        super(player, enabledFeatures, displayOperatorCreativeTab);
        this.onItemClicked = onItemClicked;
        //CreativeModeTabs.tryRebuildTabContents(enabledFeatures, true, player.level().registryAccess());
    }
    public CreativeModeItemSelectionScreen(Consumer<ItemStack> onItemClicked) {
        super(Minecraft.getInstance().player, Minecraft.getInstance().player.level().enabledFeatures(), false);
        this.onItemClicked = onItemClicked;
        //CreativeModeTabs.tryRebuildTabContents(Minecraft.getInstance().player.level().enabledFeatures(), true, Minecraft.getInstance().player.level().registryAccess());
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType type)
    {
        if(slot != null)
        {
            ItemStack stack = slot.getItem();
            if(stack != null && !stack.isEmpty())
            {
                if(onItemClicked != null)
                {
                    onItemClicked.accept(stack);
                }
            }
        }
    }
}
