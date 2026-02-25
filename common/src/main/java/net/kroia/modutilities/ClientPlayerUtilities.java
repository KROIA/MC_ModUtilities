package net.kroia.modutilities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.stream.Collectors;

import static net.kroia.modutilities.gui.Gui.getMinecraft;

@Environment(EnvType.CLIENT)
public class ClientPlayerUtilities {


    public static String getItemDisplayText(ItemStack itemStack)
    {
        Player player = getMinecraft().player;
        if(player == null)
        {
            return itemStack.getHoverName().getString();
        }
        List<Component> tooltip = itemStack.getTooltipLines(getMinecraft().player, TooltipFlag.Default.NORMAL);

        List<String> tooltipStrings = tooltip.stream()
                .map(Component::getString)
                .collect(Collectors.toList());

        String fullTooltip = String.join("\n", tooltipStrings);
        return fullTooltip.isEmpty() ? itemStack.getHoverName().getString() : fullTooltip;
    }

    public static void printToConsole(String message) {
        if (getMinecraft().player != null) {
            getMinecraft().player.sendSystemMessage(Component.literal(message));
        } else {
            System.out.println(message);
        }
    }
}
