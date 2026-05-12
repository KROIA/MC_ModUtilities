package net.kroia.modutilities.sandbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Client-side handler that detects when a TextBox on a display block needs
 * text input, and opens a popup screen for typing.
 */
@Environment(EnvType.CLIENT)
public class DisplayInputHandler {

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;

        // Check if looking at a display block
        if (!(mc.hitResult instanceof BlockHitResult blockHit)
                || blockHit.getType() != HitResult.Type.BLOCK) return;

        // The client block entity might not know the controller pos yet
        // Check both client and server side
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        var serverLevel = server.getLevel(mc.level.dimension());
        if (serverLevel == null) return;

        // Use server-side block entity which has the correct group info
        BlockEntity serverBE = serverLevel.getBlockEntity(blockHit.getBlockPos());
        if (!(serverBE instanceof DisplayDemoBlockEntity dbe) || !dbe.isActive()) return;

        BlockPos controllerPos = dbe.getControllerPos();
        if (controllerPos == null) return;

        BlockEntity ctrlBE = serverLevel.getBlockEntity(controllerPos);
        if (ctrlBE instanceof DisplayDemoBlockEntity controller && controller.isController()) {
            if (controller.hasPendingTextInput()) {
                String text = controller.consumePendingTextInput();
                net.kroia.modutilities.ModUtilitiesMod.LOGGER.info(
                        "[DisplayBlock] Opening text input screen with: {}", text);
                DisplayTextInputScreen.open(controllerPos, text);
            }
        }
    }
}
