package net.kroia.modutilities.networking;

import dev.architectury.networking.NetworkManager;

import java.util.function.Supplier;

public interface INetworkPacket extends INetworkPayloadConverter {

    /**
     * Receives the packet and processes it in the context of the network manager.
     *
     * @param contextSupplier A supplier that provides the context for the packet.
     */
    void receive(Supplier<NetworkManager.PacketContext> contextSupplier);
}
