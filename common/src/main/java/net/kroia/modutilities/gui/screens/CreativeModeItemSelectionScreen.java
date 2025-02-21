package net.kroia.modutilities.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class CreativeModeItemSelectionScreen extends CreativeModeInventoryScreen {
    private final Consumer<ItemStack> onItemClicked;
    private final Runnable onClosed;
    public CreativeModeItemSelectionScreen(Player player, FeatureFlagSet enabledFeatures, boolean displayOperatorCreativeTab, Consumer<ItemStack> onItemClicked, Runnable onClosed) {
        super(player, enabledFeatures, displayOperatorCreativeTab);
        this.onItemClicked = onItemClicked;
        this.onClosed = onClosed;
    }
    public CreativeModeItemSelectionScreen(Consumer<ItemStack> onItemClicked, Runnable onClosed) {
        super(Minecraft.getInstance().player, Minecraft.getInstance().player.level().enabledFeatures(), false);
        this.onItemClicked = onItemClicked;
        this.onClosed = onClosed;
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

    @Override
    public void onClose() {
        super.onClose();
        if(onClosed != null)
        {
            onClosed.run();
        }
    }
}
