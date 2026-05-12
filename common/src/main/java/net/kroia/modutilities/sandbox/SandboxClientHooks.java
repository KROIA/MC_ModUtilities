package net.kroia.modutilities.sandbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Environment(EnvType.CLIENT)
public class SandboxClientHooks {

    public static void openTestScreen()
    {
        Minecraft.getInstance().submit(() -> {
            Minecraft.getInstance().setScreen(new TestScreen());
        });
    }

    /**
     * Opens the {@link DisplayInteractionScreen} for the given controller
     * block position. Must only be called on the client side.
     *
     * @param controllerPos the position of the display group's controller block
     */
    public static void openDisplayInteractionScreen(BlockPos controllerPos) {
        DisplayInteractionScreen.open(controllerPos);
    }
}
