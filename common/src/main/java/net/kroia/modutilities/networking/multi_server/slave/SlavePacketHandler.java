package net.kroia.modutilities.networking.multi_server.slave;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.multi_server.ForwardPacketContext;
import net.kroia.modutilities.networking.multi_server.MultiServerPacketRegistry;
import net.kroia.modutilities.networking.multi_server.payload.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;

/**
 * Netty inbound handler that dispatches {@link Payload} messages received by a
 * slave server from its master.
 * <p>
 * Handles the four payload types the master may send:
 * <ul>
 *   <li>{@link HandshakeResultPayload} — completes the connect callback chain and closes the channel on rejection.</li>
 *   <li>{@link BroadcastPayload} — broadcasts a system chat message to all players on this server.</li>
 *   <li>{@link ManualDisconnectionPayload} — notifies the {@link SlaveServerClient} of an intentional disconnect.</li>
 *   <li>{@link ForwardPacketPayload} — decodes the wrapped packet and dispatches it through the slave-side packet registry.</li>
 * </ul>
 *
 * @apiNote
 * {@link #channelRead0(ChannelHandlerContext, Payload)} is invoked on the Netty
 * I/O thread. Payloads that touch Minecraft world state (such as
 * {@link BroadcastPayload}) are forwarded to the server thread via
 * {@code mcServer.execute(...)}. {@link ForwardPacketPayload} handling wraps
 * the byte buffer in a try-finally so the buffer is released after dispatch.
 */
public class SlavePacketHandler extends SimpleChannelInboundHandler<Payload> {

    /** We need the MC server reference to broadcast messages to players. */
    private final MinecraftServer mcServer;

    private final SlaveServerClient connector;
    private final Consumer<SlaveServerClient.ConnectionEstablishState> onConnection;

    /**
     * Creates a new handler bound to a specific slave context.
     *
     * @param mcServer     The Minecraft server reference, used to access the registry
     *                     and to schedule chat broadcasts on the main thread.
     * @param connector    The owning {@link SlaveServerClient}; used to trigger
     *                     reconnect scheduling and to propagate manual master disconnects.
     * @param onConnection Callback invoked once the master replies with a
     *                     {@link HandshakeResultPayload}, receiving the result state.
     */
    public SlavePacketHandler(MinecraftServer mcServer, SlaveServerClient connector, Consumer<SlaveServerClient.ConnectionEstablishState> onConnection) {
        this.mcServer = mcServer;
        this.connector = connector;
        this.onConnection = onConnection;
    }

    // ── Inbound packets from hub ──────────────────────────────────────────────

    /**
     * Dispatches a single {@link Payload} received from the master.
     *
     * @param ctx     The Netty channel context for the master connection.
     * @param payload The decoded payload to handle.
     *
     * @apiNote
     * Runs on the Netty I/O thread. Game-state modifications are deferred to
     * the Minecraft server thread via {@code mcServer.execute(...)}.
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Payload payload) {
        switch (payload) {
            case HandshakeResultPayload hp ->
            {
                info("HandshakeResultPayload received from master '"+hp.result()+"'");
                switch(hp.result())
                {
                    case SlaveServerClient.ConnectionEstablishState.SUCCESS:
                        break;
                    case SlaveServerClient.ConnectionEstablishState.INVALID_SHARED_SECRET,
                         SlaveServerClient.ConnectionEstablishState.SLAVE_ID_ALREADY_USED:
                        ctx.close();
                        break;
                }
                onConnection.accept(hp.result());
            }
            // Hub routed a string message to this server — display it to players
            case BroadcastPayload bc -> {
                info("BroadcastPayload received from master: ["+bc.fromServer()+"] "+bc.senderName()+": "+bc.message());

                if (mcServer != null) {
                    // mcServer.execute() ensures we run on the MC main thread
                    mcServer.execute(() -> {
                        Component chat = Component.literal(
                                "§7[§b" + bc.fromServer() + "§7→§aThis Server§7] §f" +
                                        bc.senderName() + "§7: §e" + bc.message()
                        );
                        mcServer.getPlayerList().broadcastSystemMessage(chat, false);
                    });
                }
            }
            case ManualDisconnectionPayload dp -> {
                info("ManualDisconnectionPayload received from master with message:\n"+dp.reason());
                connector.onMasterDisconnected(dp.reason());
            }
            case ForwardPacketPayload bb -> {
                ResourceLocation packetResouceLoc = bb.packetType();
                ByteBuf buf = Unpooled.buffer();
                buf.writeBytes(bb.data());
                RegistryFriendlyByteBuf dataBuf = new RegistryFriendlyByteBuf(buf, mcServer.registryAccess());
                try {
                    ForwardPacketContext context = new ForwardPacketContext(ctx, bb.senderServerID(), bb.senderPlayerUUID());
                    MultiServerPacketRegistry.handleByteBufOnSlaveSide(packetResouceLoc, dataBuf, context);
                } finally {
                    dataBuf.release();
                }
            }

            default ->
                    warn("Unhandled payload from hub: "+payload.getClass().getSimpleName());
        }
    }

    // ── Connection events ─────────────────────────────────────────────────────

    /**
     * Called by Netty when the underlying channel becomes inactive.
     * <p>
     * If the owning {@link SlaveServerClient} is not shutting down, schedules a
     * reconnect attempt; otherwise the inactivity is treated as part of an
     * orderly disconnect and ignored.
     *
     * @param ctx The Netty channel context whose channel just went inactive.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if(connector.isShuttingDown())
            return;
        warn("Lost connection to master - scheduling reconnect...");
        connector.scheduleReconnect();
    }

    /**
     * Logs uncaught exceptions raised in the inbound pipeline and closes the
     * channel so the standard reconnect logic can take over.
     *
     * @param ctx   The Netty channel context in which the exception occurred.
     * @param cause The {@link Throwable} that bubbled up the pipeline.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        error("Exception in child inbound handler", cause);
        ctx.close();
    }


    private static void info(String message) {
        ModUtilitiesMod.LOGGER.info("[SlavePacketHandler]: "+message);
    }
    private static void error(String message) {
        ModUtilitiesMod.LOGGER.error("[SlavePacketHandler]: "+message);
    }
    private static void error(String message, Throwable throwable) {
        ModUtilitiesMod.LOGGER.error("[SlavePacketHandler]: "+message, throwable);
    }
    private static void warn(String message) {
        ModUtilitiesMod.LOGGER.warn("[SlavePacketHandler]: "+message);
    }
    private static void debug(String message) {
        ModUtilitiesMod.LOGGER.debug("[SlavePacketHandler]: "+message);
    }
}
