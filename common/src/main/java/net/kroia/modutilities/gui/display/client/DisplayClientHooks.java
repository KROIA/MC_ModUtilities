package net.kroia.modutilities.gui.display.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;

/**
 * Client-only hooks for the display block system.
 * Called from common code only when {@code level.isClientSide()} is true.
 */
@Environment(EnvType.CLIENT)
public final class DisplayClientHooks {

    private DisplayClientHooks() {}

    public static void openInteractionScreen(BlockPos controllerPos) {
        DisplayInteractionScreen.open(controllerPos);
    }
}
