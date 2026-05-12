package net.kroia.modutilities.sandbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Client-side handler that monitors whether the player is looking at a
 * display block. Currently unused after the interaction model was changed
 * to open {@link DisplayInteractionScreen} directly from
 * {@link DisplayDemoBlock#useWithoutItem}.
 * <p>
 * Retained as a stub for potential future client-side tick logic
 * (e.g. visual feedback when looking at a locked display).
 */
@Environment(EnvType.CLIENT)
public class DisplayInputHandler {

    public static void clientTick() {
        // Interaction is now handled by DisplayInteractionScreen opened
        // from DisplayDemoBlock.useWithoutItem on right-click.
        // This tick handler is kept as a stub for future client-side logic.
    }
}
