package net.kroia.modutilities.networking.client_server.arrs.requestholder;

import net.kroia.modutilities.networking.client_server.arrs.GenericRequest;
import net.kroia.modutilities.networking.client_server.arrs.GenericRequestPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.concurrent.CompletableFuture;

/**
 * Internal holder for a client-to-server (or master/slave) request awaiting a response.
 * Not part of the public API.
 *
 * @param <IN>  The type of input data for the request.
 * @param <OUT> The type of output data for the request.
 * @apiNote {@code creationTimeMs} is consulted by
 *          {@link net.kroia.modutilities.networking.client_server.arrs.RequestManager#cleanupExpiredRequests(boolean)}
 *          to expire abandoned entries with a
 *          {@link net.kroia.modutilities.networking.client_server.arrs.RequestOutcomeUnknownException}.
 */
public class ServerRequestHolder<IN, OUT>
{
    public CompletableFuture<OUT> responseFuture;
    public GenericRequestPacket requestPacket;
    public GenericRequest<IN, OUT> request;
    /** Target slave of a master-to-slave request; {@code null} for requests to the server or the master. */
    public String targetSlaveID;
    /** Wall-clock time (ms) at which this holder was created; used for timeout tracking. */
    public final long creationTimeMs = System.currentTimeMillis();
    public void processResponse(RegistryFriendlyByteBuf buf)
    {
        OUT response = request.decodeOutput(buf);
        if(responseFuture != null)
        {
            responseFuture.complete(response);
        }
    }
}