package net.kroia.modutilities.networking.client_server;


import dev.architectury.networking.NetworkManager;

public interface PacketHandler<T extends NetworkPacket> {

    void handleServer(T packet, NetworkManager.PacketContext context);
    void handleClient(T packet,  NetworkManager.PacketContext context);




}
