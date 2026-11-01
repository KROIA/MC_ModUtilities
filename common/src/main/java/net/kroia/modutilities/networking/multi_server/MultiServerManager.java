package net.kroia.modutilities.networking.multi_server;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.multi_server.master.MasterTCPServer;
import net.kroia.modutilities.networking.multi_server.slave.SlaveServerClient;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Singleton entry point for the multi-server (Master/Slave) TCP networking layer.
 * <p>
 * This system is independent of the regular Minecraft client&harr;server packet
 * pipeline. It allows multiple Minecraft servers to communicate with each other
 * over a dedicated TCP link so that cross-world state (e.g. global bank balances,
 * shared markets) can be synchronized.
 * <p>
 * A single JVM may act as either the master or one slave at any given time, never
 * both. Use {@link #createMaster} or {@link #createSlave} to initialize, then
 * {@link #start()} to begin listening / connecting and {@link #stop()} to tear
 * down. {@link #cleanup()} releases the singleton entirely.
 *
 * @apiNote
 * All methods are static; the underlying instance is held in a {@code volatile}
 * field. The class is thread-safe for the typical lifecycle (create &rarr; start
 * &rarr; use &rarr; stop &rarr; cleanup) but not for concurrent create/cleanup races.
 */
public class MultiServerManager
{
    private static volatile MultiServerManager instance;

    private final @Nullable MasterTCPServer  tcpServer;
    private final @Nullable SlaveServerClient slaveClient;



    private MultiServerManager(@Nullable MasterTCPServer master, @Nullable SlaveServerClient slave) {
        this.tcpServer = master;
        this.slaveClient = slave;
    }

    /**
     * Creates the singleton in master mode and prepares it to listen on the given
     * TCP port for connecting slaves. The instance must still be started by calling
     * {@link #start()}.
     *
     * @param mcServer              The current {@link MinecraftServer} (used to obtain {@code RegistryAccess}).
     * @param sharedSecret          Token slaves must present in their handshake.
     * @param tcpPort               TCP port the master listens on.
     * @param onServerStartSuccess  Callback invoked when the TCP server has started successfully.
     * @param onServerStartFailure  Callback invoked with the cause if the TCP server fails to start.
     * @param onSlaveConnected      Callback invoked with the slave ID when a slave connects.
     * @param onSlaveDisconnected   Callback invoked with the slave ID when a slave disconnects.
     * @return                      {@code true} if the master instance was created;
     *                              {@code false} if an instance already exists.
     */
    public static boolean createMaster(MinecraftServer mcServer, String sharedSecret, int tcpPort,
                                       Runnable onServerStartSuccess, Consumer<Throwable> onServerStartFailure,
                                       Consumer<String> onSlaveConnected, Consumer<String> onSlaveDisconnected)
    {
        if(instance != null)
        {
            warn("Can't create master since there is already an existing instance of type: "+ ((instance.isSlave())?"Slave":"Master"));
            return false;
        }
        info("Create master on port: "+tcpPort);
        MultiServerPacketRegistry.onCreate(mcServer);
        MasterTCPServer master = new MasterTCPServer(mcServer, sharedSecret, tcpPort,
                onServerStartSuccess, onServerStartFailure,
                onSlaveConnected, onSlaveDisconnected);
        instance = new MultiServerManager(master, null);
        return true;
    }
    /**
     * Creates the singleton in slave mode and prepares it to connect to the given
     * master host. The connection is opened by calling {@link #start()}.
     *
     * @param mcServer              The current {@link MinecraftServer} (used to obtain {@code RegistryAccess}).
     * @param sharedSecret          Token sent during the handshake; must match the master's secret.
     * @param slaveServerID         Unique ID identifying this slave to the master.
     * @param masterHostIP          Hostname or IP of the master server.
     * @param masterHostTcpPort     TCP port the master is listening on.
     * @param onConnectionAccepted  Callback invoked once the master accepts the handshake.
     * @param onConnectionFailure   Callback invoked when the connection cannot be established;
     *                              receives the {@link SlaveServerClient.ConnectionEstablishState}.
     * @param onConnectionLost      Callback invoked with the cause when an established
     *                              connection drops unexpectedly.
     * @param onDisconnect          Callback invoked on a clean disconnect.
     * @return                      {@code true} if the slave instance was created;
     *                              {@code false} if an instance already exists.
     */
    public static boolean createSlave(MinecraftServer mcServer, String sharedSecret, String slaveServerID, String masterHostIP, int masterHostTcpPort,
                                      Runnable onConnectionAccepted, Consumer<SlaveServerClient.ConnectionEstablishState> onConnectionFailure, Consumer<Throwable> onConnectionLost, Runnable onDisconnect)
    {
        if(instance != null)
        {
            warn("Can't create slave since there is already an existing instance of type: "+ ((instance.isSlave())?"Slave":"Master"));
            return false;
        }
        info("Create slave and connect to master:"+ masterHostIP+":"+masterHostTcpPort);
        MultiServerPacketRegistry.onCreate(mcServer);
        SlaveServerClient slave = new SlaveServerClient(mcServer, sharedSecret, slaveServerID,  masterHostIP, masterHostTcpPort,
                onConnectionAccepted,  onConnectionFailure, onConnectionLost, onDisconnect);
        instance = new MultiServerManager(null, slave);
        return true;
    }


    /**
     * Indicates whether a {@link MultiServerManager} singleton currently exists,
     * irrespective of master/slave mode or running state.
     *
     * @return {@code true} if {@link #createMaster} or {@link #createSlave} has been called
     *         and {@link #cleanup()} has not yet run.
     */
    public static boolean isInUse()
    {
        return instance != null;
    }

    /**
     * Tests whether the current instance, if any, was created in slave mode.
     *
     * @return {@code true} if an instance exists and it is operating as a slave.
     */
    public static boolean isSlave()
    {
        if(instance == null)
            return false;
        return instance.slaveClient != null;
    }

    /**
     * Tests whether the current instance, if any, was created in master mode.
     *
     * @return {@code true} if an instance exists and it is operating as the master.
     */
    public static boolean isMaster()
    {
        if(instance == null)
            return false;
        return instance.tcpServer != null;
    }


    /**
     * Returns this slave's configured server ID.
     *
     * @return The slave's server ID, or an empty string if no instance exists or this
     *         server is not running in slave mode.
     */
    public static String getSlaveID()
    {
        if(instance == null || instance.slaveClient  == null)
            return "";
        return instance.slaveClient.getServerID();
    }
    /**
     * Returns the local IP this slave is connecting from, as reported by the slave client.
     *
     * @return The slave's local IP, or an empty string if no instance exists or this
     *         server is not running in slave mode.
     */
    public static String getSlaveIP()
    {
        if(instance == null || instance.slaveClient  == null)
            return "";
        return instance.slaveClient.getSlaveIP();
    }

    /**
     * Returns the master server's IP address. On a slave this is the configured remote
     * master; on the master itself it is the bind/host address of the TCP server.
     *
     * @return The master's IP, or an empty string if no instance exists.
     */
    public static String getMasterIP()
    {
        if(instance == null)
            return "";
        if(instance.slaveClient  != null)
            return instance.slaveClient.getMasterIP();
        if(instance.tcpServer != null)
            return instance.tcpServer.getMasterIP();
        return "";
    }
    /**
     * Returns the master TCP port. On a slave this is the configured master's port;
     * on the master it is the actual listening port.
     *
     * @return The master's TCP port, or {@code -1} if no instance exists.
     */
    public static int getMasterPort()
    {
        if(instance == null)
            return -1;
        if(instance.slaveClient  != null)
            return instance.slaveClient.getMasterPort();
        if(instance.tcpServer != null)
            return instance.tcpServer.getPort();
        return -1;
    }

    /**
     * Returns the IDs of all slaves currently connected to this master.
     *
     * @return A list of connected slave IDs, or an empty list if no instance exists or
     *         this server is not running in master mode.
     *
     * @apiNote
     * Only meaningful on the master side.
     */
    public static List<String> getConnectedSlaveIDs()
    {
        if(instance == null)
            return Collections.emptyList();
        if(instance.tcpServer != null)
            return instance.tcpServer.getConnectedSlaveIDs();
        return Collections.emptyList();
    }
    /**
     * Forcibly disconnects the slave with the given ID, sending a reason string.
     *
     * @param slaveID The ID of the slave to disconnect.
     * @param reason  Human-readable reason transmitted to the slave before the channel is closed.
     *
     * @apiNote
     * No-op if no instance exists or this server is not running in master mode.
     */
    public static void disconnectSlave(String slaveID, String reason)
    {
        if(instance == null)
            return;
        if(instance.tcpServer != null)
            instance.tcpServer.disconnectSlave(slaveID, reason);
    }

    /**
     * (Slave only) Indicates whether the master closed the connection on its side.
     *
     * @return {@code true} if this slave's connection was terminated by the master,
     *         {@code false} otherwise (including when this server is the master or
     *         no instance exists).
     */
    public static boolean masterHasDisconnected()
    {
        if(instance == null)
            return false;
        if(instance.slaveClient != null)
            return instance.slaveClient.masterHasDisconnected();
        return false;
    }
    /**
     * (Slave only) Returns the reason transmitted by the master when it disconnected this slave.
     *
     * @return The disconnect reason, or an empty string if none is available.
     */
    public static String getMasterDisconnectReason()
    {
        if(instance == null)
            return "";
        if(instance.slaveClient != null)
            return instance.slaveClient.getMasterDisconnectReason();
        return "";
    }




    /**
     * Starts the underlying TCP server (master) or initiates the connection to the
     * master (slave). Must be preceded by a successful {@link #createMaster} or
     * {@link #createSlave} call.
     *
     * @return {@code true} if the start was attempted (or the link was already up);
     *         {@code false} if no instance exists.
     */
    public static boolean start()
    {
        if(instance == null)
        {
            error("Can't start master/slave since no master/slave instance exists");
            return false;
        }
        if(instance.tcpServer != null)
        {
            if(instance.tcpServer.isRunning())
            {
                warn("Server already running");
                return true;
            }
            instance.tcpServer.start();
        }
        else
        {
            assert instance.slaveClient != null;
            if(instance.slaveClient.isConnected())
            {
                warn("Already connected to the master");
                return true;
            }
            instance.slaveClient.connect();
        }
        return true;
    }

    /**
     * Stops the master TCP server or disconnects the slave from the master, depending
     * on the current mode. The singleton instance is preserved; call {@link #cleanup()}
     * to release it entirely.
     *
     * @return {@code true} if a stop was issued; {@code false} if no instance exists.
     */
    public static boolean stop()
    {
        if(instance == null)
        {
            error("Can't stop master/slave since no master/slave instance exists");
            return false;
        }
        if(instance.tcpServer != null)
        {
            instance.tcpServer.stop();
        }
        else
        {
            instance.slaveClient.disconnect();
        }
        return true;
    }

    /**
     * Indicates whether the master TCP server is currently listening, or the slave
     * has an active connection to its master.
     *
     * @return {@code true} if the link is currently active.
     */
    public static boolean isRunning()
    {
        if(instance == null)
            return false;
        if(instance.tcpServer != null)
        {
            return instance.tcpServer.isRunning();
        }
        else
        {
            assert instance.slaveClient != null;
            return instance.slaveClient.isConnected();
        }
    }

    /**
     * Indicates whether a singleton instance currently exists. Equivalent to
     * {@link #isInUse()}.
     *
     * @return {@code true} if an instance exists.
     */
    public static boolean instanceExists()
    {
        return instance != null;
    }

    /**
     * Stops any running master or slave and clears the singleton instance so a new
     * one can be created later. Safe to call when no instance exists.
     */
    public static void cleanup()
    {
        if(instance == null)
            return;
        if(isRunning())
        {
            if(instance.tcpServer != null)
            {
                instance.tcpServer.stop();
            }
            else
            {
                assert instance.slaveClient != null;
                instance.slaveClient.disconnect();
            }
        }
        instance = null;
    }



    /**
     * (Slave only) Sends a registered packet to the master server with no associated player UUID.
     *
     * @param packet The payload to send. Its type must be registered with
     *               {@link MultiServerPacketRegistry}.
     * @return       {@code true} if the packet was queued for sending; {@code false} if the
     *               link is not in slave mode, not connected, or otherwise unable to send.
     */
    public static boolean sendToMaster(CustomPacketPayload packet)
    {
        if(checkSendToMaster())
            return instance.slaveClient.sendToMaster(null, packet);
        return false;
    }

    /**
     * (Slave only) Sends a registered packet to the master server, tagged with the given
     * originating player UUID.
     *
     * @param senderPlayerUUID UUID of the player that triggered this packet.
     * @param packet           The payload to send. Its type must be registered with
     *                         {@link MultiServerPacketRegistry}.
     * @return                 {@code true} if the packet was queued for sending;
     *                         {@code false} otherwise.
     */
    public static boolean sendToMaster(UUID senderPlayerUUID, CustomPacketPayload packet)
    {
        if(checkSendToMaster())
            return instance.slaveClient.sendToMaster(senderPlayerUUID, packet);
        return false;
    }




    /**
     * (Master only) Sends a registered packet to a specific slave with no associated player UUID.
     *
     * @param serverId ID of the target slave.
     * @param packet   The payload to send. Its type must be registered with
     *                 {@link MultiServerPacketRegistry}.
     * @return         {@code true} if the packet was queued for sending; {@code false} if
     *                 the link is not in master mode or the slave is not connected.
     */
    public static boolean sendToSlave(String serverId, CustomPacketPayload packet)
    {
        if(checkSendToSlave(serverId))
            return instance.tcpServer.sendToSlave(null,serverId, packet);
        return false;
    }

    /**
     * (Master only) Sends a registered packet to a specific slave, tagged with the given
     * originating player UUID.
     *
     * @param senderPlayerUUID UUID of the player that triggered this packet, or {@code null}.
     * @param serverId         ID of the target slave.
     * @param packet           The payload to send. Its type must be registered with
     *                         {@link MultiServerPacketRegistry}.
     * @return                 {@code true} if the packet was queued for sending;
     *                         {@code false} otherwise.
     */
    public static boolean sendToSlave(@Nullable UUID senderPlayerUUID, String serverId, CustomPacketPayload packet)
    {
        if(checkSendToSlave(serverId))
            return instance.tcpServer.sendToSlave(senderPlayerUUID,serverId, packet);
        return false;
    }

    /**
     * (Master only) Broadcasts a packet to every connected slave with no associated player UUID.
     *
     * @param packet The payload to broadcast. Its type must be registered with
     *               {@link MultiServerPacketRegistry}.
     */
    public static void broadcastToSlaves(CustomPacketPayload packet)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(null,packet);
    }

    /**
     * (Master only) Broadcasts a packet to every connected slave, tagged with the given
     * originating player UUID.
     *
     * @param senderPlayerUUID UUID of the player that triggered this packet, or {@code null}.
     * @param packet           The payload to broadcast.
     */
    public static void broadcastToSlaves(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(senderPlayerUUID,packet);
    }

    /**
     * (Master only) Broadcasts a packet to every connected slave except the one with the
     * given ID. Useful for re-broadcasting a packet that originated from a slave back to
     * the others without echoing it to the source.
     *
     * @param packet          The payload to broadcast.
     * @param excludeServerId ID of the slave that should not receive this packet.
     */
    public static void broadcastToSlaves(CustomPacketPayload packet, String excludeServerId)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(null,packet, excludeServerId);
    }

    /**
     * (Master only) Broadcasts a packet to every connected slave except the one with the
     * given ID, tagged with the given originating player UUID.
     *
     * @param senderPlayerUUID UUID of the player that triggered this packet, or {@code null}.
     * @param packet           The payload to broadcast.
     * @param excludeServerId  ID of the slave that should not receive this packet.
     */
    public static void broadcastToSlaves(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet, String excludeServerId)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(senderPlayerUUID, packet, excludeServerId);
    }

    /**
     * (Master only) Broadcasts a packet to every connected slave except those whose IDs
     * appear in {@code excludeServerIds}.
     *
     * @param packet           The payload to broadcast.
     * @param excludeServerIds IDs of slaves that should not receive this packet.
     */
    public static void broadcastToSlaves(CustomPacketPayload packet, List<String> excludeServerIds)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(null,packet, excludeServerIds);
    }

    /**
     * (Master only) Broadcasts a packet to every connected slave except those whose IDs
     * appear in {@code excludeServerIds}, tagged with the given originating player UUID.
     *
     * @param senderPlayerUUID UUID of the player that triggered this packet, or {@code null}.
     * @param packet           The payload to broadcast.
     * @param excludeServerIds IDs of slaves that should not receive this packet.
     */
    public static void broadcastToSlaves(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet, List<String> excludeServerIds)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(senderPlayerUUID,packet, excludeServerIds);
    }



    private static boolean checkSendToMaster()
    {
        if(instance == null)
        {
            error("sendToMaster(packet): Cant send a packet before initializing the MultiServerManager as slave by calling MultiServerManager.createSlave(...)");
            return false;
        }
        if(instance.slaveClient == null)
        {
            if(instance.tcpServer != null)
            {
                error("sendToMaster(packet): It seems like this MultiServerManager is initialized as master. A master can't send packets to another master!");
            }
            else  {
                throw new IllegalStateException("sendToMaster(packet): It should not be possible that the MultiServerManager is initialized, but neither as master nor as slave");
            }
            return false;
        }
        if(!instance.slaveClient.isConnected())
        {
            Throwable failReason = instance.slaveClient.getConnectionFailReason();
            if(failReason != null)
            {
                error("sendToMaster(packet): Failed to establish a connection to the Master during initialization. Reason: " + failReason);
            }
            else
                error("sendToMaster(packet): The connection to the Master has not yet been established. Have you forgotten to call MultiServerManager.start()?");
            return false;
        }
        return true;
    }
    private static boolean checkSendToSlave(String serverId)
    {
        if(instance == null)
        {
            error("sendToSlave(targetServer='"+serverId+"', packet): Cant send a packet before initializing the MultiServerManager as master by calling MultiServerManager.createMaster(...)");
            return false;
        }

        if(instance.tcpServer == null)
        {
            if(instance.slaveClient != null)
            {
                error("sendToSlave(targetServer='"+serverId+"', packet): It seems like this MultiServerManager is initialized as slave. A slave can't send packets to another slave!");
            }
            else  {
                throw new IllegalStateException("sendToSlave(targetServer='"+serverId+"', packet): It should not be possible that the MultiServerManager is initialized, but neither as master nor as slave");
            }
            return false;
        }
        if(!instance.tcpServer.isRunning())
        {
            if(instance.tcpServer.isStartupFailed())
            {
                error("sendToSlave(targetServer='"+serverId+"', packet): The TCP server had failed to startup during initialization. Reason: "+instance.tcpServer.getStartupFailReason());
            }
            else
                error("sendToSlave(targetServer='"+serverId+"', packet): The TCP server is not running. Have you forgotten to call MultiServerManager.start()?");
            return false;
        }
        return true;
    }
    private static boolean checkBroadcastToSlaves()
    {
        if(instance == null)
        {
            error("broadcastToSlaves(...): Cant broadcast a packet before initializing the MultiServerManager as master by calling MultiServerManager.createMaster(...)");
            return false;
        }

        if(instance.tcpServer == null)
        {
            if(instance.slaveClient != null)
            {
                error("broadcastToSlaves(...): It seems like this MultiServerManager is initialized as slave. A slave can't broadcast packets to another slave!");
            }
            else  {
                throw new IllegalStateException("broadcastToSlaves(...): It should not be possible that the MultiServerManager is initialized, but neither as master nor as slave");
            }
            return false;
        }
        if(!instance.tcpServer.isRunning())
        {
            if(instance.tcpServer.isStartupFailed())
            {
                error("broadcastToSlaves(...): The TCP server had failed to startup during initialization. Reason: "+instance.tcpServer.getStartupFailReason());
            }
            else
                error("broadcastToSlaves(...): The TCP server is not running. Have you forgotten to call MultiServerManager.start()?");
            return false;
        }
        return true;
    }

    private static void info(String message) {
        ModUtilitiesMod.LOGGER.info("[MultiServerManager]: "+message);
    }
    private static void error(String message) {
        ModUtilitiesMod.LOGGER.error("[MultiServerManager]: "+message);
    }
    private static void error(String message, Throwable throwable) {
        ModUtilitiesMod.LOGGER.error("[MultiServerManager]: "+message, throwable);
    }
    private static void warn(String message) {
        ModUtilitiesMod.LOGGER.warn("[MultiServerManager]: "+message);
    }
    private static void debug(String message) {
        ModUtilitiesMod.LOGGER.debug("[MultiServerManager]: "+message);
    }
}
