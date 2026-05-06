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
     * MultiServerManager factory
     * @param mcServer
     * @param sharedSecret
     * @param tcpPort
     * @return
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


    public static boolean isInUse()
    {
        return instance != null;
    }
    public static boolean isSlave()
    {
        if(instance == null)
            return false;
        return instance.slaveClient != null;
    }
    public static boolean isMaster()
    {
        if(instance == null)
            return false;
        return instance.tcpServer != null;
    }


    public static String getSlaveID()
    {
        if(instance == null || instance.slaveClient  == null)
            return "";
        return instance.slaveClient.getServerID();
    }
    public static String getSlaveIP()
    {
        if(instance == null || instance.slaveClient  == null)
            return "";
        return instance.slaveClient.getSlaveIP();
    }
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
    public static List<String> getConnectedSlaveIDs()
    {
        if(instance == null)
            return Collections.emptyList();
        if(instance.tcpServer != null)
            return instance.tcpServer.getConnectedSlaveIDs();
        return Collections.emptyList();
    }
    public static void disconnectSlave(String slaveID, String reason)
    {
        if(instance == null)
            return;
        if(instance.tcpServer != null)
            instance.tcpServer.disconnectSlave(slaveID, reason);
    }
    public static boolean masterHasDisconnected()
    {
        if(instance == null)
            return false;
        if(instance.slaveClient != null)
            return instance.slaveClient.masterHasDisconnected();
        return false;
    }
    public static String getMasterDisconnectReason()
    {
        if(instance == null)
            return "";
        if(instance.slaveClient != null)
            return instance.slaveClient.getMasterDisconnectReason();
        return "";
    }




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

    public static boolean instanceExists()
    {
        return instance != null;
    }

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



    public static boolean sendToMaster(CustomPacketPayload packet)
    {
        if(checkSendToMaster())
            return instance.slaveClient.sendToMaster(null, packet);
        return false;
    }
    public static boolean sendToMaster(UUID senderPlayerUUID, CustomPacketPayload packet)
    {
        if(checkSendToMaster())
            return instance.slaveClient.sendToMaster(senderPlayerUUID, packet);
        return false;
    }




    public static boolean sendToSlave(String serverId, CustomPacketPayload packet)
    {
        if(checkSendToSlave(serverId))
            return instance.tcpServer.sendToSlave(null,serverId, packet);
        return false;
    }
    public static boolean sendToSlave(@Nullable UUID senderPlayerUUID, String serverId, CustomPacketPayload packet)
    {
        if(checkSendToSlave(serverId))
            return instance.tcpServer.sendToSlave(senderPlayerUUID,serverId, packet);
        return false;
    }
    public static void broadcastToSlaves(CustomPacketPayload packet)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(null,packet);
    }
    public static void broadcastToSlaves(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(senderPlayerUUID,packet);
    }
    public static void broadcastToSlaves(CustomPacketPayload packet, String excludeServerId)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(null,packet, excludeServerId);
    }
    public static void broadcastToSlaves(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet, String excludeServerId)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(senderPlayerUUID, packet, excludeServerId);
    }
    public static void broadcastToSlaves(CustomPacketPayload packet, List<String> excludeServerIds)
    {
        if(checkBroadcastToSlaves())
            instance.tcpServer.broadcastToSlaves(null,packet, excludeServerIds);
    }
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
            if(instance.slaveClient.isConnected())
            {
                error("sendToMaster(packet): Failed to establish a connection to the Master during initialization. Reason: "+instance.slaveClient.getConnectionFailReason());
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
