package net.kroia.modutilities.networking.arrs;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.modutilities.networking.NetworkPacket;
import net.kroia.modutilities.networking.PacketHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.UUID;

/**
 * This Class is used by the "asynchronous request-response system" (ARRS).
 * Packet used for responding to requests
 */
public final class GenericResponsePacket extends NetworkPacket
{

    public static final Type<GenericResponsePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "generic_response_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GenericResponsePacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, p -> p.requestID,
            ByteBufCodecs.STRING_UTF8, p -> p.requestTypeID,
            ExtraCodecUtils.FRIENDLY_BYTE_BUF_CODEC, p -> p.data,
            GenericResponsePacket::new
    );

    public static final PacketHandler<GenericResponsePacket> HANDLER = new PacketHandler<>(){

        @Override
        public void handleServer(GenericResponsePacket packet, NetworkManager.PacketContext context) {
            try {
                AsynchronousRequestResponseSystem.processResponseOnServer(packet, (ServerPlayer) context.getPlayer());
            }
            catch (Exception e) {
                // Handle any exceptions that may occur during decoding/encoding
                ModUtilitiesMod.LOGGER.error("Error processing GenericResponsePacket on server: " + e.getMessage(), e);
                return; // Exit if an error occurs
            }
        }

        @Override
        public void handleClient(GenericResponsePacket packet, NetworkManager.PacketContext context) {
            try{
                AsynchronousRequestResponseSystem.processResponseOnClient(packet);
            }
            catch (Exception e) {
                // Handle any exceptions that may occur during decoding/encoding
                ModUtilitiesMod.LOGGER.error("Error processing GenericResponsePacket on client: " + e.getMessage(), e);
                return; // Exit if an error occurs
            }
        }
    };



    /**
     * The ID of the request this specific request response is for.
     */
    UUID requestID;

    /**
     * The type ID of the request this specific request response is for.
     * This is used to identify the type of request object
     */
    String requestTypeID;

    /**
     * The data of the response.
     * This is a byte buffer that contains the serialized data of the request response.
     */
    FriendlyByteBuf data;

    public GenericResponsePacket(UUID requestID, String requestTypeID, FriendlyByteBuf data) {
        super();
        this.requestID = requestID;
        this.requestTypeID = requestTypeID;
        this.data = data;
    }

    public UUID getRequestID() {
        return requestID;
    }
    public String getRequestTypeID() {
        return requestTypeID;
    }
    public FriendlyByteBuf getData() {
        return data;
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
