package net.kroia.modutilities.networking.multi_server.payload;

import net.minecraft.SharedConstants;

/**
 * Sent by a child server immediately after connecting to the hub.
 * The hub uses the token to authenticate the child and then rejects it if the
 * Minecraft versions differ.
 *
 * @param serverId  A unique name for this child server, e.g. "server_a"
 * @param token     Shared secret that must match hub's configured secret
 * @param mcVersion Minecraft version of the child, e.g. "1.21.1". Empty if the
 *                  child runs an older ModUtilities that does not send it.
 */
public record HandshakePayload(String serverId, String token, String mcVersion) implements Payload {
    /**
     * {@inheritDoc}
     *
     * @return {@link PacketIds#HANDSHAKE}.
     */
    @Override
    public int packetId() { return PacketIds.HANDSHAKE; }

    /** @return The Minecraft version of the running game, e.g. "1.21.1". */
    public static String localMcVersion() {
        return SharedConstants.getCurrentVersion().getName(); // mc<=1.21.5
        //return SharedConstants.getCurrentVersion().name(); // mc>=1.21.6
    }
}
