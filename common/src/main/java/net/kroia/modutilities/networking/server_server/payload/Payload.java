package net.kroia.modutilities.networking.server_server.payload;

/**
 * Base interface for all packets sent over the TCP hub connection.
 * Each implementing record defines its own fields and packet ID.
 */
public sealed interface Payload permits
        HandshakePayload,
        BroadcastPayload,
        ForwardPacketPayload {

    /** Unique byte ID used to identify this packet type on the wire. */
    int packetId();
}
