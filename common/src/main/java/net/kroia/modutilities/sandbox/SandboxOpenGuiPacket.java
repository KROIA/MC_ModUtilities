package net.kroia.modutilities.sandbox;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.kroia.modutilities.networking.multi_server.ForwardPacketContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class SandboxOpenGuiPacket extends NetworkPacket {
    public static Sandbox.SandboxNetwork network;

    public static final Type<SandboxOpenGuiPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "sandbox_open_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SandboxOpenGuiPacket> STREAM_CODEC = StreamCodec.composite(
            ExtraCodecUtils.enumStreamCodec(GuiType.class), p -> p.guiType,
            SandboxOpenGuiPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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

    @Override
    protected void handleOnClient(NetworkManager.PacketContext context) {
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

    @Override
    protected void handleOnServer(NetworkManager.PacketContext context) {

    }

    @Override
    protected void handleOnMaster(ForwardPacketContext context) {

    }

    @Override
    protected void handleOnSlave(ForwardPacketContext context) {

    }


    public static void send(ServerPlayer receiver, SandboxOpenGuiPacket.GuiType guiType) {
        SandboxOpenGuiPacket packet = new SandboxOpenGuiPacket(guiType);
        network.sendToClient(receiver, packet);
    }



}
