package net.kroia.modutilities.networking.client_server.streaming;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.kroia.modutilities.networking.client_server.PacketHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;


/**
 * This Class is used by the "streaming system".
 * This packet is used to send a data chunk from client to server or vice versa.
 * It contains the stream ID and the data associated with the stream.
 */
public class GenericStreamPacket extends NetworkPacket {


    public static final Type<GenericStreamPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "generic_stream_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GenericStreamPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, p -> p.streamID,
            ExtraCodecUtils.REGISTRY_FRIENDLY_BYTE_BUF_CODEC, p -> p.data,
            GenericStreamPacket::new

    );

    public static final PacketHandler<GenericStreamPacket> HANDLER = new PacketHandler<>(){

        @Override
        public void handleServer(GenericStreamPacket packet, NetworkManager.PacketContext context) {
            StreamSystem.handlePacket(packet, (ServerPlayer) context.getPlayer());
        }

        @Override
        public void handleClient(GenericStreamPacket packet, NetworkManager.PacketContext context) {
            StreamSystem.handlePacket(packet);
        }
    };

    UUID streamID;
    RegistryFriendlyByteBuf data;


    public GenericStreamPacket(UUID streamID, RegistryFriendlyByteBuf data) {
        super();
        this.streamID = streamID;
        this.data = data;
    }


    public UUID getStreamID() {
        return streamID;
    }
    public RegistryFriendlyByteBuf getData() {
        return data;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
