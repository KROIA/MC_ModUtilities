package net.kroia.modutilities.sandbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class SandboxClientHooks {

    public static void openTestScreen()
    {
        Minecraft.getInstance().submit(() -> {
            Minecraft.getInstance().setScreen(new TestScreen());
        });
    }
}
