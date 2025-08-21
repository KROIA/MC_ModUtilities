package net.kroia.modutilities.sandbox;

import net.kroia.modutilities.networking.NetworkPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class SandboxOpenGuiPacket extends NetworkPacket {
    public static Sandbox.SandboxNetwork network;

    enum GuiType {
        TEST_SCREEN,
        ANOTHER_SCREEN
    }
    private SandboxOpenGuiPacket.GuiType guiType;
    public SandboxOpenGuiPacket(SandboxOpenGuiPacket.GuiType guiType) {
        this.guiType = guiType;
    }
    public SandboxOpenGuiPacket() {
        super();
    }
    public SandboxOpenGuiPacket(FriendlyByteBuf friendlyByteBuf) {
        super(friendlyByteBuf);
    }

    public static void send(ServerPlayer receiver, SandboxOpenGuiPacket.GuiType guiType) {
        SandboxOpenGuiPacket packet = new SandboxOpenGuiPacket(guiType);
        network.sendToClient(receiver, packet);
    }
    @Override
    public void decode(FriendlyByteBuf buf) {
        int ordinal = buf.readInt();
        this.guiType = SandboxOpenGuiPacket.GuiType.values()[ordinal];
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(guiType.ordinal());
    }

    @Override
    protected void handleOnClient() {
        switch(guiType) {
            case TEST_SCREEN:
                SandboxClientHooks.openTestScreen();
                break;
            case ANOTHER_SCREEN:
                // Open another GUI
                break;
            default:
        }
    }
}
