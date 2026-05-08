package net.kroia.modutilities.networking.multi_server.master;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.multi_server.ForwardPacketContext;
import net.kroia.modutilities.networking.multi_server.MultiServerPacketRegistry;
import net.kroia.modutilities.networking.multi_server.payload.HandshakePayload;
import net.kroia.modutilities.networking.multi_server.payload.ForwardPacketPayload;
import net.kroia.modutilities.networking.multi_server.payload.HandshakeResultPayload;
import net.kroia.modutilities.networking.multi_server.payload.Payload;
import net.kroia.modutilities.networking.multi_server.slave.SlaveServerClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * Netty inbound handler installed at the end of the master pipeline.
 * <p>
 * Responsible for processing the per-connection handshake from a slave server,
 * registering the slave with the {@link MasterTCPServer} on success, and
 * dispatching subsequent {@link ForwardPacketPayload}s to the
 * {@link MultiServerPacketRegistry}.
 *
 * @apiNote
 * Methods on this class run on the Netty I/O thread for the connection. The
 * shared-secret check uses {@link java.security.MessageDigest#isEqual(byte[], byte[])},
 * which is constant-time and therefore resistant to timing attacks.
 */
public class MasterPacketHandler extends SimpleChannelInboundHandler<Payload> {



    private final MinecraftServer mcServer;
    private final MasterTCPServer  masterTCPServer;

    /** Set after a successful handshake. Null until then. */
    private String serverId = null;

    /**
     * Constructs a new handler bound to the given Minecraft server and master TCP server.
     *
     * @param mcServer        The Minecraft server instance, used for registry access when
     *                        decoding forwarded packets.
     * @param serverTCPServer The owning {@link MasterTCPServer}, used to register/unregister
     *                        slave connections and to access the shared secret.
     */
    public MasterPacketHandler(MinecraftServer mcServer, MasterTCPServer serverTCPServer) {
        this.mcServer = mcServer;
        this.masterTCPServer = serverTCPServer;
    }

    // ── Channel events ────────────────────────────────────────────────────────

    /**
     * Dispatches an incoming {@link Payload} based on its concrete type.
     * <p>
     * Handles the slave handshake (validating the shared secret and rejecting
     * duplicate slave IDs) and forwards data packets to the
     * {@link MultiServerPacketRegistry}. Unknown payload types are logged and ignored.
     *
     * @param ctx     The Netty channel handler context for this connection.
     * @param payload The decoded payload to dispatch.
     *
     * @apiNote
     * Runs on the Netty I/O thread. The shared-secret comparison uses
     * {@link java.security.MessageDigest#isEqual(byte[], byte[])} for
     * constant-time evaluation.
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Payload payload) {
        switch (payload) {

            // ── Handshake: child server identifies itself ─────────────────────
            case HandshakePayload hs -> {
                info("HandshakePayload request received for server '"+hs.serverId()+"'.");
                if (!java.security.MessageDigest.isEqual(
                        hs.token().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        masterTCPServer.getSharedSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                    HandshakeResultPayload response = new HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState.INVALID_SHARED_SECRET);
                    warn("Rejected connection from '"+hs.serverId()+"' - "+response.result());
                    Channel channel = ctx.channel();
                    if (channel != null && channel.isActive()) {
                        channel.writeAndFlush(response);
                    }
                    ctx.close();
                    return;
                }

                if(masterTCPServer.getConnectedSlaveIDs().contains(hs.serverId())) {
                    HandshakeResultPayload response = new HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState.SLAVE_ID_ALREADY_USED);
                    warn("Rejected connection from '"+hs.serverId()+"' - "+response.result());
                    Channel channel = ctx.channel();
                    if (channel != null && channel.isActive()) {
                        channel.writeAndFlush(response);
                    }
                    ctx.close();
                    return;
                }
                serverId = hs.serverId();
                masterTCPServer.putChildConnection(serverId, ctx.channel());
                masterTCPServer.sendToSlave(serverId, new HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState.SUCCESS));
            }
            case ForwardPacketPayload bb -> {
                if (serverId == null) {
                    warn("Received data packet before handshake — closing.");
                    ctx.close();
                    return;
                }

                ResourceLocation packetResouceLoc = bb.packetType();
                ByteBuf buf = ctx.alloc().buffer();
                buf.writeBytes(bb.data());
                RegistryFriendlyByteBuf dataBuf = new RegistryFriendlyByteBuf(buf, mcServer.registryAccess());
                ForwardPacketContext context = new ForwardPacketContext(ctx, bb.senderServerID(), bb.senderPlayerUUID());
                // buffer ownership transferred to handleByteBufOnMasterSide — do NOT release here
                MultiServerPacketRegistry.handleByteBufOnMasterSide(packetResouceLoc, dataBuf, context);
            }

            default ->
                    warn("Unhandled payload type: "+ payload.getClass().getSimpleName());
        }
    }

    /**
     * Called by Netty when the underlying channel becomes inactive.
     * <p>
     * Removes the associated slave from the {@link MasterTCPServer} registry
     * (only if the slave previously completed the handshake).
     *
     * @param ctx The Netty channel handler context for this connection.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (serverId != null) {
            masterTCPServer.removeChildConnection(serverId);
        }
    }

    /**
     * Called by Netty when an uncaught exception bubbles up the pipeline.
     * <p>
     * Logs the exception and closes the connection.
     *
     * @param ctx   The Netty channel handler context for this connection.
     * @param cause The exception that propagated through the pipeline.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        error("Exception in child handler for '"+ serverId+"'", cause);
        ctx.close();
    }
    
    
    
    
    
    


    private static void info(String message) {
        ModUtilitiesMod.LOGGER.info("[MasterPacketHandler]: "+message);
    }
    private static void error(String message) {
        ModUtilitiesMod.LOGGER.error("[MasterPacketHandler]: "+message);
    }
    private static void error(String message, Throwable throwable) {
        ModUtilitiesMod.LOGGER.error("[MasterPacketHandler]: "+message, throwable);
    }
    private static void warn(String message) {
        ModUtilitiesMod.LOGGER.warn("[MasterPacketHandler]: "+message);
    }
    private static void debug(String message) {
        ModUtilitiesMod.LOGGER.debug("[MasterPacketHandler]: "+message);
    }
}
