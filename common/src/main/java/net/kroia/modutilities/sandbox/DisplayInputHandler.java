package net.kroia.modutilities.sandbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Client-side handler that monitors whether the player is looking at a
 * display block. Currently unused after the interaction model was changed
 * to open the interaction screen directly from
 * {@link net.kroia.modutilities.gui.display.AbstractDisplayBlock#useWithoutItem}.
 * <p>
 * Retained as a stub for potential future client-side tick logic
 * (e.g. visual feedback when looking at a locked display).
 */
@Environment(EnvType.CLIENT)
public class DisplayInputHandler {

    public static void clientTick() {
        // Interaction is now handled by the generic DisplayInteractionScreen
        // opened from AbstractDisplayBlock.useWithoutItem on right-click.
        // This tick handler is kept as a stub for future client-side logic.
    }
}
