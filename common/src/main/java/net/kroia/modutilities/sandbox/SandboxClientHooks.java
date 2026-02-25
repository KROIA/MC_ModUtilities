package net.kroia.modutilities.sandbox;

import net.minecraft.client.Minecraft;

public class SandboxClientHooks {

    public static void openTestScreen()
    {
        Minecraft.getInstance().submit(() -> {
            Minecraft.getInstance().setScreen(new TestScreen());
        });
    }
}
