package net.kroia.modutilities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.stream.Collectors;

import static net.kroia.modutilities.gui.Gui.getMinecraft;

/**
 * Client-side helpers for working with the local player.
 * <p>
 * This class is annotated with {@link Environment @Environment(EnvType.CLIENT)} and must only
 * be referenced from client code; loading it on a dedicated server will fail.
 *
 * @apiNote Use {@link ServerPlayerUtilities} for the server-side counterpart.
 */
@Environment(EnvType.CLIENT)
public class ClientPlayerUtilities {


    /**
     * Builds a human-readable display string for the given stack, including its full tooltip
     * lines as they would appear in the inventory UI.
     * <p>
     * If the local player is not yet available or the stack has no tooltip lines, falls back to
     * just the item's hover name.
     *
     * @param itemStack the stack to render
     * @return the multi-line tooltip text, or the hover name if no tooltip is available
     */
    public static String getItemDisplayText(ItemStack itemStack)
    {
        Player player = getMinecraft().player;
        if(player == null)
        {
            return itemStack.getHoverName().getString();
        }
        Item.TooltipContext context = Item.TooltipContext.of(getMinecraft().level);
        List<Component> tooltip = itemStack.getTooltipLines(context, getMinecraft().player, TooltipFlag.Default.NORMAL);

        List<String> tooltipStrings = tooltip.stream()
                .map(Component::getString)
                .collect(Collectors.toList());

        String fullTooltip = String.join("\n", tooltipStrings);
        return fullTooltip.isEmpty() ? itemStack.getHoverName().getString() : fullTooltip;
    }

    /**
     * Prints a message to the local player's chat, or to standard output if no player is
     * currently available (e.g. the player is on the title screen).
     *
     * @param message the message to display
     */
    public static void printToConsole(String message) {
        if (getMinecraft().player != null) {
            getMinecraft().player.sendSystemMessage(Component.literal(message));
        } else {
            System.out.println(message);
        }
    }
}
