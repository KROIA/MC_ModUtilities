package net.kroia.modutilities.networking.multi_server;

import com.google.gson.JsonObject;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Context object passed alongside every forwarded packet in the multi-server
 * topology. It carries information about the original sender (server and,
 * optionally, player) as well as the raw Netty channel the packet arrived on.
 * <p>
 * Instances are constructed by the framework when a {@code ForwardPacketPayload}
 * is unwrapped, and handed to the matching {@link ForwardPacketHandler}.
 *
 * @apiNote
 * All fields are immutable. Because handlers are invoked on the Netty event
 * loop, the embedded {@link ChannelHandlerContext} should not be used to call
 * back into Minecraft game state directly.
 */
public class ForwardPacketContext
{
    /**
     * Raw Netty channel context for the connection on which this packet was received.
     * Provided so advanced handlers can inspect or interact with the underlying channel.
     */
    public final ChannelHandlerContext channelContext;

    /**
     * The Server ID of the slave that originally sent this packet. On the master side
     * this identifies the source slave; on a slave receiving a relayed packet it
     * identifies the originating slave.
     */
    public final String senderServerID;

    /**
     * UUID of the player that originated the packet, if any. May be {@code null} for
     * server-initiated packets.
     *
     * @apiNote
     * The corresponding {@code ServerPlayer} instance is generally NOT available on the
     * receiving side because the player is logged in on a different Minecraft server.
     */
    public final @Nullable UUID senderPlayerUUID;

    //public final @Nullable UUID packetIdentifier;

    /**
     * Creates a new forwarding context.
     *
     * @param channelContext   The Netty channel context the packet arrived on.
     * @param senderServerID   The server ID of the originating slave.
     * @param senderPlayerUUID The UUID of the originating player, or {@code null} if
     *                         the packet was not initiated by a player.
     */
    public ForwardPacketContext(ChannelHandlerContext channelContext, String senderServerID, @Nullable UUID senderPlayerUUID)
    {
        this.channelContext = channelContext;
        this.senderServerID = senderServerID;
        this.senderPlayerUUID = senderPlayerUUID;
    }

    @Override
    public String toString()
    {
        // this class to Json
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("channelContext", channelContext.toString());
        jsonObject.addProperty("senderServerID", senderServerID);
        jsonObject.addProperty("senderPlayerUUID", (senderPlayerUUID==null?"null":senderPlayerUUID.toString()));
        return jsonObject.toString();
    }
}
