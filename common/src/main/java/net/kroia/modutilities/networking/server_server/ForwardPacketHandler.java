package net.kroia.modutilities.networking.server_server;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface ForwardPacketHandler<T extends CustomPacketPayload> {

    void handleMaster(T packet, ChannelHandlerContext context);
    void handleSlave(T packet,  ChannelHandlerContext context);

}
