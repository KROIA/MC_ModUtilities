package net.kroia.modutilities.networking.multi_server.payload;

/**
 * Central registry of all packet IDs used in the hub TCP protocol.
 * Both encoder and decoder must agree on these values.
 */
public final class PacketIds {
    public static final int HANDSHAKE           = 0x01; // Child → Hub: register server
    public static final int HANDSHAKE_RESULT    = 0x02; // Child → Hub: register server
    public static final int BROADCAST           = 0x03; // Hub  → All: hub-initiated broadcast
    public static final int MANUAL_DISCONNECT   = 0x04; //
    public static final int FORWARD_PACKET      = 0x05; //

    private PacketIds() {}
}
