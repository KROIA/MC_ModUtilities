package net.kroia.modutilities.sandbox;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class SimpleDataPacketToClient extends NetworkPacket {
    public static final Type<SimpleDataPacketToClient> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "simple_data_packet_to_client"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SimpleDataPacketToClient> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.value,
            SimpleDataPacketToClient::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Data to be sent
    int value;

    public SimpleDataPacketToClient(int value) {
        super();
        this.value = value;
    }

    // Creates a packet and sends it using the MyExampleModNetworking
    public static void sendPacket(ServerPlayer receiver, int value) {
        Sandbox.network.sendToClient(receiver, new SimpleDataPacketToClient(value));
    }

    // Called when the client has received a packet from the server
    @Override
    protected void handleOnClient(NetworkManager.PacketContext context)
    {
        System.out.println("[CLIENT SIDE] Received value from server: " + value);
    }

    @Override
    protected void handleOnServer(NetworkManager.PacketContext context) {

    }
}
