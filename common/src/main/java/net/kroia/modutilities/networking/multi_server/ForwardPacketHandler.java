package net.kroia.modutilities.networking.multi_server;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface ForwardPacketHandler<T extends CustomPacketPayload> {
    void handleMaster(T packet, ForwardPacketContext context);
    void handleSlave(T packet,  ForwardPacketContext context);
}
