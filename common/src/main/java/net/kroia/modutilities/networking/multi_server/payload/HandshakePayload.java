package net.kroia.modutilities.networking.multi_server.payload;

/**
 * Sent by a child server immediately after connecting to the hub.
 * The hub uses the token to authenticate the child.
 *
 * @param serverId  A unique name for this child server, e.g. "server_a"
 * @param token     Shared secret that must match hub's configured secret
 */
public record HandshakePayload(String serverId, String token) implements Payload {
    /**
     * {@inheritDoc}
     *
     * @return {@link PacketIds#HANDSHAKE}.
     */
    @Override
    public int packetId() { return PacketIds.HANDSHAKE; }
}
