package net.kroia.modutilities.networking.multi_server.payload;

/**
 * Central registry of all packet IDs used in the hub TCP protocol.
 * Both encoder and decoder must agree on these values.
 *
 * @apiNote
 * These IDs are written as the leading bytes of every encoded {@link Payload}
 * frame and are used by the decoder to dispatch to the correct record type.
 * Changing existing values will break wire compatibility between master and
 * slave servers, so new IDs should always be appended rather than reordered.
 */
public final class PacketIds {
    /** Slave to master: register server with handshake credentials. */
    public static final int HANDSHAKE           = 0x01; // Child → Hub: register server
    /** Master to slave: result of the handshake authentication. */
    public static final int HANDSHAKE_RESULT    = 0x02; // Child → Hub: register server
    /** Master to slave: broadcast a system chat message originating from another server. */
    public static final int BROADCAST           = 0x03; // Hub  → All: hub-initiated broadcast
    /** Master to slave: signal that the master is intentionally closing the connection. */
    public static final int MANUAL_DISCONNECT   = 0x04; //
    /** Bidirectional: forward an arbitrary {@code NetworkPacket} between servers. */
    public static final int FORWARD_PACKET      = 0x05; //

    private PacketIds() {}
}
