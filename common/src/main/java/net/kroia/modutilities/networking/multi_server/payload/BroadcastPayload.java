package net.kroia.modutilities.networking.multi_server.payload;

/**
 * Sent FROM the hub TO all child servers — e.g. to display a received message.
 * <p>
 * Carries a system chat message that the master server distributes to one or
 * more slaves so the slave can broadcast it to its connected players.
 *
 * @param senderName  Original sender's player name
 * @param fromServer  Server the message came from
 * @param message     The string content
 */
public record BroadcastPayload(
        String senderName,
        String fromServer,
        String message
) implements Payload {
    /**
     * {@inheritDoc}
     *
     * @return {@link PacketIds#BROADCAST}.
     */
    @Override
    public int packetId() { return PacketIds.BROADCAST; }
}
