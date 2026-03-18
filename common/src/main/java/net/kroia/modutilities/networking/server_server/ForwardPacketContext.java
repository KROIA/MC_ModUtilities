package net.kroia.modutilities.networking.server_server;

import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ForwardPacketContext
{
    /**
     * @brief
     * Networking contect, provided by the netty library
     */
    public final ChannelHandlerContext channelContext;

    /**
     * @brief
     * The Server ID from which the packet is comming
     */
    public final String senderServerID;

    /**
     * @brief
     * If a player has sent a packet from another server to this,
     * the Player UUID is set and can be used.
     *
     * @note
     * The ServerPlayer instance may not be available since the player is on another server
     */
    public final @Nullable UUID senderPlayerUUID;

    public ForwardPacketContext(ChannelHandlerContext channelContext, String senderServerID, @Nullable UUID senderPlayerUUID)
    {
        this.channelContext = channelContext;
        this.senderServerID = senderServerID;
        this.senderPlayerUUID = senderPlayerUUID;
    }
}
