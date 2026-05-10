package net.kroia.modutilities.networking.client_server.arrs.requestholder;

import net.kroia.modutilities.networking.client_server.arrs.GenericRequest;
import net.kroia.modutilities.networking.client_server.arrs.GenericRequestPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

/**
 * Internal holder for a server-to-client request awaiting a response. Not part of the public API.
 *
 * @param <IN>  The type of input data for the request.
 * @param <OUT> The type of output data for the request.
 * @apiNote {@code creationTimeMs} is consulted by
 *          {@link net.kroia.modutilities.networking.client_server.arrs.RequestManager#cleanupExpiredRequests(long)}
 *          to expire abandoned entries.
 */
public class ClientRequestHolder<IN, OUT>
{
    public BiConsumer<OUT, ServerPlayer> responseHandler;
    public GenericRequestPacket requestPacket;
    public GenericRequest<IN, OUT> request;
    /** Wall-clock time (ms) at which this holder was created; used for timeout tracking. */
    public final long creationTimeMs = System.currentTimeMillis();
    public void processResponse(RegistryFriendlyByteBuf buf, ServerPlayer player)
    {
        // Decode the response using the request's decodeOutput method
        OUT response = request.decodeOutput(buf);
        if(responseHandler != null)
        {
            // Pass the response and player to the handler
            responseHandler.accept(response, player);
        }
    }
}