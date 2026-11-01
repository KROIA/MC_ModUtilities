package net.kroia.modutilities.networking.multi_server.payload;

/**
 * Base interface for all packets sent over the TCP hub connection.
 * Each implementing record defines its own fields and packet ID.
 *
 * @apiNote
 * This is a sealed interface; all permitted record implementations represent
 * the complete set of message types exchanged between master and slave servers
 * in the multi-server networking protocol.
 */
public sealed interface Payload permits
        HandshakePayload,
        HandshakeResultPayload,
        BroadcastPayload,
        ManualDisconnectionPayload,
        ForwardPacketPayload {

    /**
     * Unique byte ID used to identify this packet type on the wire.
     * The encoder writes this ID as a header so the decoder can reconstruct
     * the correct payload subtype.
     *
     * @return The packet ID, as defined in {@link PacketIds}.
     */
    int packetId();
}
