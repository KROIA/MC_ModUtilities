package net.kroia.modutilities.networking.client_server.arrs;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.UtilitiesPlatform;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.kroia.modutilities.networking.client_server.PacketHandler;
import net.kroia.modutilities.networking.server_server.ForwardPacketContext;
import net.kroia.modutilities.networking.server_server.ForwardPacketHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

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
            ExtraCodecUtils.REGISTRY_FRIENDLY_BYTE_BUF_CODEC, p -> p.data,
            GenericResponsePacket::new
    );

    @Override
    protected void handleOnClient(NetworkManager.PacketContext context) {
        try{
            ModUtilitiesMod.LOGGER.info("Handling GenericResponsePacket on client: "+requestTypeID);
            AsynchronousRequestResponseSystem.processResponseOnClient(this);
        }
        catch (Exception e) {
            // Handle any exceptions that may occur during decoding/encoding
            ModUtilitiesMod.LOGGER.error("Error processing GenericResponsePacket on client: " + e.getMessage(), e);
            return; // Exit if an error occurs
        }
    }

    @Override
    protected void handleOnServer(NetworkManager.PacketContext context) {
        try {
            AsynchronousRequestResponseSystem.processResponseOnServer(this, (ServerPlayer) context.getPlayer());
        }
        catch (Exception e) {
            // Handle any exceptions that may occur during decoding/encoding
            ModUtilitiesMod.LOGGER.error("Error processing GenericResponsePacket on server: " + e.getMessage(), e);
            return; // Exit if an error occurs
        }
    }

    @Override
    protected void handleOnMaster(ForwardPacketContext context) {

    }

    @Override
    protected void handleOnSlave(ForwardPacketContext context) {
        var request = AsynchronousRequestResponseSystem.getRegisteredRequest(requestTypeID);
        if (request == null) {
            return; // No factory found for this request type
        }
        ModUtilitiesMod.LOGGER.info("Handle response on slave server: "+requestTypeID);
        //RegistryFriendlyByteBuf responseData = UtilitiesPlatform.createRegistryFriendlyByteBufServerSide();
        try {
            MinecraftServer server = UtilitiesPlatform.getServer();
            if(server == null)
                return;
            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(context.senderPlayerUUID);
            if(targetPlayer == null)
                return;
            request.getManager().getNetworkManager().sendToClient(targetPlayer, this);
        }
        catch (Exception e) {
            // Handle any exceptions that may occur during decoding/encoding
            ModUtilitiesMod.LOGGER.error("Error handling GenericResponsePacket: " + e.getMessage(), e);
            return; // Exit if an error occurs
        }
    }


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
    RegistryFriendlyByteBuf data;

    public GenericResponsePacket(UUID requestID, String requestTypeID, RegistryFriendlyByteBuf data) {
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
    public RegistryFriendlyByteBuf getData() {
        return data;
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
