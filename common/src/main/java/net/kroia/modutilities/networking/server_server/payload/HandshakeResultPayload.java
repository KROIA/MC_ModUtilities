package net.kroia.modutilities.networking.server_server.payload;

public record HandshakeResultPayload(boolean accepted) implements Payload {
    @Override
    public int packetId() { return PacketIds.HANDSHAKE_RESULT; }
}
