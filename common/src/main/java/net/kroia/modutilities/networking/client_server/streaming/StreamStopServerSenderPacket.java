package net.kroia.modutilities.networking.client_server.streaming;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.kroia.modutilities.networking.client_server.PacketHandler;
import net.kroia.modutilities.networking.server_server.ForwardPacketContext;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class StreamStopServerSenderPacket  extends NetworkPacket {
    public static final Type<StreamStopServerSenderPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "stream_stop_server_sender_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StreamStopServerSenderPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, p -> p.streamID,
            StreamStopServerSenderPacket::new
    );

    UUID streamID;

    public StreamStopServerSenderPacket(UUID streamID) {
        super();
        this.streamID = streamID;
    }


    public UUID getStreamID() {
        return streamID;
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    protected void handleOnClient(NetworkManager.PacketContext context) {
        StreamSystem.handlePacket(this);
    }

    @Override
    protected void handleOnServer(NetworkManager.PacketContext context) {
        //StreamSystem.handlePacket(packet, (ServerPlayer) context.getPlayer());
    }

    @Override
    protected void handleOnMaster(ForwardPacketContext context) {

    }

    @Override
    protected void handleOnSlave(ForwardPacketContext context) {

    }
}
