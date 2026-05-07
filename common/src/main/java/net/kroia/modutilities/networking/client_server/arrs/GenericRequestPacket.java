package net.kroia.modutilities.networking.client_server.arrs;


import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.UtilitiesPlatform;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.kroia.modutilities.networking.multi_server.ForwardPacketContext;
import net.kroia.modutilities.networking.multi_server.MultiServerManager;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * This Class is used by the "asynchronous request-response system" (ARRS).
 * This packet is used to send a generic request from client to server or vice versa.
 * It contains a request ID, a request type ID, and the data associated with the request.
 * The request type ID is used to determine how to handle the request on the server or client side.
 */
public final class GenericRequestPacket extends NetworkPacket
{

    /** The {@link CustomPacketPayload.Type} identifier for this packet. */
    public static final Type<GenericRequestPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "generic_request"));

    /** Stream codec used to (de)serialize the packet over the network. */
    public static final StreamCodec<RegistryFriendlyByteBuf, GenericRequestPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, p -> p.requestID,
            ByteBufCodecs.STRING_UTF8, p -> p.requestTypeID,
            ExtraCodecUtils.REGISTRY_FRIENDLY_BYTE_BUF_CODEC, p -> p.data,
            GenericRequestPacket::new
    );

    @Override
    protected void handleOnClient(NetworkManager.PacketContext context) {

    }

    @Override
    protected void handleOnServer(NetworkManager.PacketContext context) {
        var request = AsynchronousRequestResponseSystem.getRegisteredRequest(requestTypeID);
        if (request == null) {
            releaseData();
            return; // No factory found for this request type
        }
        RegistryFriendlyByteBuf responseData = UtilitiesPlatform.createRegistryFriendlyByteBufServerSide();
        try {
            // decodeHandleEncodeOnServer reads from `data` synchronously (in decodeInput),
            // then returns a future that completes when the handler finishes.
            // We release `data` in whenComplete to guarantee it outlives any synchronous read
            // and is freed regardless of success or failure.
            CompletableFuture<RegistryFriendlyByteBuf> fut = request.decodeHandleEncodeOnServer(data, responseData, (ServerPlayer) context.getPlayer());
            fut.whenComplete((responseBuf, ex) -> {
                releaseData();
                if (ex != null) {
                    ModUtilitiesMod.LOGGER.error("Error in async GenericRequestPacket handler: " + ex.getMessage(), ex);
                    return;
                }
                sendResponseToClient(request.getManager(), (ServerPlayer) context.getPlayer(), new GenericResponsePacket(requestID, requestTypeID, responseBuf));
            });
        }
        catch (Exception e) {
            releaseData();
            ModUtilitiesMod.LOGGER.error("Error handling GenericRequestPacket: " + e.getMessage(), e);
        }
    }
    @Override
    protected boolean needsRoutingToMaster()
    {
        var request = AsynchronousRequestResponseSystem.getRegisteredRequest(requestTypeID);
        if (request == null) {
            return false; // No factory found for this request type
        }
        return request.needsRoutingToMaster();
    }
    @Override
    protected void handleOnMaster(ForwardPacketContext context) {
        var request = AsynchronousRequestResponseSystem.getRegisteredRequest(requestTypeID);
        if (request == null) {
            releaseData();
            return; // No factory found for this request type
        }
        RegistryFriendlyByteBuf responseData = UtilitiesPlatform.createRegistryFriendlyByteBufServerSide();
        try {
            CompletableFuture<RegistryFriendlyByteBuf> fut = request.decodeHandleEncodeOnMasterServer(data, responseData, context.senderServerID, context.senderPlayerUUID);
            fut.whenComplete((responseBuf, ex) -> {
                releaseData();
                if (ex != null) {
                    ModUtilitiesMod.LOGGER.error("Error in async GenericRequestPacket master handler: " + ex.getMessage(), ex);
                    return;
                }
                sendResponseToSlave(context.senderServerID, context.senderPlayerUUID, new GenericResponsePacket(requestID, requestTypeID, responseBuf));
            });
        }
        catch (Exception e) {
            releaseData();
            ModUtilitiesMod.LOGGER.error("Error handling GenericRequestPacket: " + e.getMessage(), e);
        }
    }
    @Override
    protected void handleOnSlave(ForwardPacketContext context)
    {
        var request = AsynchronousRequestResponseSystem.getRegisteredRequest(requestTypeID);
        if (request == null) {
            releaseData();
            return; // No factory found for this request type
        }
        RegistryFriendlyByteBuf responseData = UtilitiesPlatform.createRegistryFriendlyByteBufServerSide();
        try {
            CompletableFuture<RegistryFriendlyByteBuf> fut = request.decodeHandleEncodeOnSlaveServer(data, responseData, context.senderPlayerUUID);
            fut.whenComplete((responseBuf, ex) -> {
                releaseData();
                if (ex != null) {
                    ModUtilitiesMod.LOGGER.error("Error in async GenericRequestPacket slave handler: " + ex.getMessage(), ex);
                    return;
                }
                sendResponseToMaster(context.senderPlayerUUID, new GenericResponsePacket(requestID, requestTypeID, responseBuf));
            });
        }
        catch (Exception e) {
            releaseData();
            ModUtilitiesMod.LOGGER.error("Error handling GenericRequestPacket: " + e.getMessage(), e);
        }
    }


    /**
     * Unique identifier for the request.
     * This is used to match requests with their responses.
     */
    UUID requestID;

    /**
     * Identifier for the type of request.
     * This is used to determine how to handle the request on the server or client side.
     */
    String requestTypeID;

    /**
     * Data associated with the request.
     * This is a byte buffer that contains the data to be sent with the request.
     */
    RegistryFriendlyByteBuf data;

    /**
     * Constructs a new request packet with a freshly generated request ID.
     *
     * @param requestTypeID The identifier of the request type (see {@link GenericRequest#getRequestTypeID()}).
     * @param data          The encoded input payload.
     */
    public GenericRequestPacket(String requestTypeID, RegistryFriendlyByteBuf data) {
        super();
        this.requestID = UUID.randomUUID();
        this.requestTypeID = requestTypeID;
        this.data = data;
    }

    /**
     * Constructs a new request packet with an explicit request ID.
     * Used by the stream codec when reconstructing a packet from the network.
     *
     * @param requestID     The unique identifier of this request.
     * @param requestTypeID The identifier of the request type.
     * @param data          The encoded input payload.
     */
    public GenericRequestPacket(UUID requestID, String requestTypeID, RegistryFriendlyByteBuf data) {
        this.requestID = requestID;
        this.requestTypeID = requestTypeID;
        this.data = data;
    }


    /**
     * @return The unique identifier of this request, used to correlate it with its response.
     */
    public UUID getRequestID() {
        return requestID;
    }

    /**
     * @return The request type identifier used to look up the registered {@link GenericRequest}.
     */
    public String getRequestTypeID() {
        return requestTypeID;
    }

    /**
     * @return The encoded input payload buffer.
     */
    public FriendlyByteBuf getData() {
        return data;
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Safely releases the {@link #data} buffer if it is non-null and still has a positive reference count.
     */
    private void releaseData() {
        if (data != null && data.refCnt() > 0) {
            data.release();
        }
    }




    /**
     * Sends a response packet back to the client or server based on the environment.
     * This method can be called from within the handleOnClient or handleOnServer methods.
     *
     * @param packet The packet to send as a response.
     * @return true if the response was sent successfully, false otherwise.
     */
    private static boolean sendResponseToClient(RequestManager manager, ServerPlayer player, GenericResponsePacket packet)
    {
        if(player == null)
            return false;
        manager.getNetworkManager().sendToClient(player, packet);
        return true;
    }

    private static boolean sendResponseToSlave(String slaveName, UUID player, GenericResponsePacket packet)
    {
        if(MultiServerManager.isRunning() && MultiServerManager.isMaster())
        {
            MultiServerManager.sendToSlave(player, slaveName,  packet);
        }
        return true;
    }
    private static boolean sendResponseToMaster(UUID player, GenericResponsePacket packet)
    {
        if(MultiServerManager.isRunning() && MultiServerManager.isSlave())
        {
            MultiServerManager.sendToMaster(player,  packet);
        }
        return true;
    }

}