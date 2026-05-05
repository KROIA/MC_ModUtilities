# Multi-Server Networking

## Overview

The Multi-Server Networking system provides TCP-based communication between multiple Minecraft server instances, enabling cross-server data synchronization and distributed systems. Built on Netty, this system implements a Master/Slave architecture where one master server coordinates communication with multiple slave servers.

**Package**: `net.kroia.modutilities.networking.multi_server`

### Key Features

- TCP-based reliable communication between servers
- Master/Slave architecture for centralized coordination
- Automatic reconnection handling for slave servers
- Secure authentication via shared secrets
- Custom packet forwarding between servers
- Broadcast messaging to all connected servers
- Player-aware packet routing (track player UUID across servers)

### Use Cases

1. **Multi-World Synchronization**: Synchronize data across multiple server instances (e.g., survival, creative, minigame worlds)
2. **Cross-Server Communication**: Send messages and data between different servers
3. **Distributed Systems**: Build load-balanced or specialized server clusters
4. **Global Events**: Broadcast events to all connected servers
5. **Cross-Server Player Tracking**: Track player actions across multiple servers

---

## Architecture

The system uses a **Master/Slave pattern** where:

### Master Server

The **Master Server** acts as a central hub that:
- Listens on a TCP port for incoming slave connections
- Authenticates slaves using a shared secret
- Routes packets between connected slaves
- Broadcasts messages to all slaves
- Manages slave connection/disconnection lifecycle
- Validates unique slave IDs

**Implementation**: `MasterTCPServer`

### Slave Server

**Slave Servers** are individual Minecraft servers that:
- Connect to the master via TCP
- Authenticate using a shared secret and unique slave ID
- Send packets to the master for routing
- Receive broadcasts and forwarded packets
- Automatically reconnect if connection is lost

**Implementation**: `SlaveServerClient`

### Architecture Diagram

```
┌─────────────────┐
│  Master Server  │
│   (TCP Hub)     │
│   Port: 25575   │
└────────┬────────┘
         │
    ┌────┼────┬─────────┐
    │    │    │         │
┌───▼──┐ │ ┌──▼───┐ ┌──▼───┐
│Slave1│ │ │Slave2│ │Slave3│
│  A   │ │ │  B   │ │  C   │
└──────┘ │ └──────┘ └──────┘
         │
      ┌──▼───┐
      │SlaveN│
      └──────┘
```

All slave-to-slave communication flows through the master server.

---

## Core Concepts

### 1. MultiServerManager

`MultiServerManager` is the main entry point for all multi-server operations. It's a singleton that manages either a master or slave instance (never both).

**Key Methods**:
- `createMaster()` - Initialize as master server
- `createSlave()` - Initialize as slave server
- `start()` - Start the TCP server or connection
- `stop()` - Stop the server or disconnect
- `sendToMaster()` - Send packet to master (slave only)
- `sendToSlave()` - Send packet to specific slave (master only)
- `broadcastToSlaves()` - Broadcast to all slaves (master only)

### 2. MultiServerConfig

Configuration loaded from `config/MultiServerConfig.json`:

**Master Configuration**:
```json
{
  "enable": true,
  "isMaster": true,
  "masterTcpPort": 25575,
  "sharedSecret": "change-me-please"
}
```

**Slave Configuration**:
```json
{
  "enable": true,
  "isMaster": false,
  "slaveID": "slave_a",
  "masterHost": "127.0.0.1",
  "masterTcpPort": 25575,
  "sharedSecret": "change-me-please"
}
```

**Security Note**: Always change the default `sharedSecret` in production!

### 3. Payload Types

All TCP communication uses typed payload objects:

#### HandshakePayload
Sent by slave to authenticate with master.
- `serverId` - Unique identifier for the slave
- `token` - Shared secret for authentication

#### HandshakeResultPayload
Master's response to handshake.
- `result` - Connection state: `SUCCESS`, `INVALID_SHARED_SECRET`, `SLAVE_ID_ALREADY_USED`

#### ForwardPacketPayload
Wraps Minecraft CustomPacketPayload for cross-server transmission.
- `senderPlayerUUID` - UUID of player who sent packet (nullable)
- `senderServerID` - ID of originating server
- `packetType` - ResourceLocation identifying packet type
- `data` - Serialized packet data

#### BroadcastPayload
Simple broadcast message to all slaves.
- `senderName` - Name of sender
- `fromServer` - Originating server ID
- `message` - Message content

#### ManualDisconnectionPayload
Sent by master to gracefully disconnect a slave.
- `reason` - Human-readable disconnect reason

### 4. MultiServerPacketRegistry

Central registry for packets that can be forwarded between servers. Packets must be registered to be transmitted across the multi-server network.

**Registration**:
```java
MultiServerPacketRegistry.register(
    MyPacket.TYPE,
    MyPacket.STREAM_CODEC,
    new ForwardPacketHandler<MyPacket>() {
        @Override
        public void handleMaster(MyPacket packet, ForwardPacketContext context) {
            // Handle packet on master server
        }
        
        @Override
        public void handleSlave(MyPacket packet, ForwardPacketContext context) {
            // Handle packet on slave server
        }
    }
);
```

### 5. ForwardPacketHandler

Interface for handling forwarded packets on both master and slave sides:

```java
public interface ForwardPacketHandler<T extends CustomPacketPayload> {
    void handleMaster(T packet, ForwardPacketContext context);
    void handleSlave(T packet, ForwardPacketContext context);
}
```

### 6. ForwardPacketContext

Context information provided when handling forwarded packets:

- `channelContext` - Netty channel context
- `senderServerID` - ID of server that sent the packet
- `senderPlayerUUID` - UUID of player who sent packet (if applicable)

---

## Setup Guide

### Step 1: Configure Master Server

Create `config/MultiServerConfig.json` on the master server:

```json
{
  "enable": true,
  "isMaster": true,
  "masterTcpPort": 25575,
  "sharedSecret": "your-secure-secret-here"
}
```

### Step 2: Configure Slave Servers

Create `config/MultiServerConfig.json` on each slave server:

**Slave A**:
```json
{
  "enable": true,
  "isMaster": false,
  "slaveID": "survival",
  "masterHost": "192.168.1.100",
  "masterTcpPort": 25575,
  "sharedSecret": "your-secure-secret-here"
}
```

**Slave B**:
```json
{
  "enable": true,
  "isMaster": false,
  "slaveID": "creative",
  "masterHost": "192.168.1.100",
  "masterTcpPort": 25575,
  "sharedSecret": "your-secure-secret-here"
}
```

**Important**: Each slave must have a unique `slaveID`.

### Step 3: Initialize in Server Startup

```java
public class MyModEvents {
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        MultiServerConfig config = MultiServerConfig.get();
        
        if (!config.enable) {
            return; // Multi-server disabled
        }
        
        if (config.isMaster) {
            setupMaster(server, config);
        } else {
            setupSlave(server, config);
        }
    }
}
```

### Step 4: Connection Handshake Process

1. Slave connects to master's TCP port
2. Slave sends `HandshakePayload` with ID and shared secret
3. Master validates:
   - Shared secret matches
   - Slave ID is unique (not already connected)
4. Master sends `HandshakeResultPayload` with result
5. If successful, connection is established
6. If failed, connection is closed

```
Slave                          Master
  │                              │
  ├──── TCP Connect ────────────>│
  │                              │
  ├──── HandshakePayload ───────>│
  │     (slaveID, token)         │
  │                              │
  │                       [Validate]
  │                              │
  │<──── HandshakeResultPayload ─┤
  │     (SUCCESS)                │
  │                              │
  │<════ Connection Ready ══════>│
```

---

## Code Examples

### Example 1: Setting Up a Master Server

```java
import net.kroia.modutilities.networking.multi_server.*;
import net.minecraft.server.MinecraftServer;

public class MasterServerSetup {
    
    public static void setupMaster(MinecraftServer server, MultiServerConfig config) {
        boolean success = MultiServerManager.createMaster(
            server,
            config.sharedSecret,
            config.masterTcpPort,
            
            // On server start success
            () -> {
                ModLogger.info("Master server started on port " + config.masterTcpPort);
            },
            
            // On server start failure
            (error) -> {
                ModLogger.error("Failed to start master server", error);
            },
            
            // On slave connected
            (slaveId) -> {
                ModLogger.info("Slave connected: " + slaveId);
                broadcastWelcome(slaveId);
            },
            
            // On slave disconnected
            (slaveId) -> {
                ModLogger.info("Slave disconnected: " + slaveId);
            }
        );
        
        if (success) {
            MultiServerManager.start();
        }
    }
    
    private static void broadcastWelcome(String newSlaveId) {
        // Notify all slaves about new connection
        String message = "Server '" + newSlaveId + "' has joined the network!";
        // Custom packet broadcasting implementation here
    }
}
```

### Example 2: Setting Up a Slave Server

```java
import net.kroia.modutilities.networking.multi_server.*;
import net.kroia.modutilities.networking.multi_server.slave.SlaveServerClient;
import net.minecraft.server.MinecraftServer;

public class SlaveServerSetup {
    
    public static void setupSlave(MinecraftServer server, MultiServerConfig config) {
        boolean success = MultiServerManager.createSlave(
            server,
            config.sharedSecret,
            config.slaveID,
            config.masterHost,
            config.masterTcpPort,
            
            // On connection accepted
            () -> {
                ModLogger.info("Successfully connected to master server");
                sendInitialData();
            },
            
            // On connection failure
            (state) -> {
                switch (state) {
                    case INVALID_SHARED_SECRET:
                        ModLogger.error("Authentication failed: invalid shared secret");
                        break;
                    case SLAVE_ID_ALREADY_USED:
                        ModLogger.error("Slave ID '" + config.slaveID + "' is already in use");
                        break;
                    default:
                        ModLogger.error("Connection failed: " + state);
                }
            },
            
            // On connection lost
            (error) -> {
                ModLogger.warn("Lost connection to master, will retry...", error);
            },
            
            // On manual disconnect
            () -> {
                if (MultiServerManager.masterHasDisconnected()) {
                    String reason = MultiServerManager.getMasterDisconnectReason();
                    ModLogger.info("Master disconnected us: " + reason);
                }
            }
        );
        
        if (success) {
            MultiServerManager.start();
        }
    }
    
    private static void sendInitialData() {
        // Send initial synchronization data to master
    }
}
```

### Example 3: Broadcasting Messages

**From Master to All Slaves**:

```java
import net.kroia.modutilities.networking.multi_server.MultiServerManager;

public class MasterBroadcast {
    
    public static void broadcastEvent(CustomPacketPayload packet) {
        if (!MultiServerManager.isMaster()) {
            return; // Only master can broadcast
        }
        
        // Broadcast to all connected slaves
        MultiServerManager.broadcastToSlaves(packet);
    }
    
    public static void broadcastEventExcluding(
        CustomPacketPayload packet, 
        String excludeSlaveId
    ) {
        // Broadcast to all except one slave
        MultiServerManager.broadcastToSlaves(packet, excludeSlaveId);
    }
    
    public static void broadcastEventExcluding(
        CustomPacketPayload packet, 
        List<String> excludeSlaveIds
    ) {
        // Broadcast to all except multiple slaves
        MultiServerManager.broadcastToSlaves(packet, excludeSlaveIds);
    }
    
    public static void broadcastPlayerAction(
        UUID playerUUID, 
        CustomPacketPayload packet
    ) {
        // Broadcast with player context
        MultiServerManager.broadcastToSlaves(playerUUID, packet);
    }
}
```

### Example 4: Forwarding Packets Between Servers

**Slave to Master**:

```java
import net.kroia.modutilities.networking.multi_server.MultiServerManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class SlaveToMasterCommunication {
    
    public static void sendToMaster(CustomPacketPayload packet) {
        if (!MultiServerManager.isSlave()) {
            return; // Only slaves can send to master
        }
        
        boolean sent = MultiServerManager.sendToMaster(packet);
        if (!sent) {
            ModLogger.warn("Failed to send packet to master");
        }
    }
    
    public static void sendPlayerActionToMaster(UUID playerUUID, CustomPacketPayload packet) {
        if (!MultiServerManager.isSlave()) {
            return;
        }
        
        // Include player UUID for context
        MultiServerManager.sendToMaster(playerUUID, packet);
    }
}
```

**Master to Specific Slave**:

```java
public class MasterToSlaveCommunication {
    
    public static void sendToSlave(String slaveId, CustomPacketPayload packet) {
        if (!MultiServerManager.isMaster()) {
            return; // Only master can send to slaves
        }
        
        boolean sent = MultiServerManager.sendToSlave(slaveId, packet);
        if (!sent) {
            ModLogger.warn("Failed to send packet to slave: " + slaveId);
        }
    }
    
    public static void sendPlayerDataToSlave(
        String slaveId, 
        UUID playerUUID, 
        CustomPacketPayload packet
    ) {
        if (!MultiServerManager.isMaster()) {
            return;
        }
        
        // Send with player context
        MultiServerManager.sendToSlave(playerUUID, slaveId, packet);
    }
}
```

### Example 5: Custom Payload Implementation

**Define Custom Packet**:

```java
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncDataPacket(
    String key,
    String value,
    long timestamp
) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SyncDataPacket> TYPE = 
        new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("mymod", "sync_data")
        );
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDataPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncDataPacket::key,
            ByteBufCodecs.STRING_UTF8, SyncDataPacket::value,
            ByteBufCodecs.VAR_LONG, SyncDataPacket::timestamp,
            SyncDataPacket::new
        );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

**Register for Multi-Server**:

```java
import net.kroia.modutilities.networking.multi_server.*;

public class MultiServerPacketRegistration {
    
    public static void register() {
        MultiServerPacketRegistry.register(
            SyncDataPacket.TYPE,
            SyncDataPacket.STREAM_CODEC,
            new ForwardPacketHandler<SyncDataPacket>() {
                @Override
                public void handleMaster(SyncDataPacket packet, ForwardPacketContext context) {
                    // Master received sync data from slave
                    String fromServer = context.senderServerID;
                    ModLogger.info("Master received: " + packet.key() + 
                                   " from " + fromServer);
                    
                    // Broadcast to all other slaves
                    MultiServerManager.broadcastToSlaves(packet, fromServer);
                }
                
                @Override
                public void handleSlave(SyncDataPacket packet, ForwardPacketContext context) {
                    // Slave received sync data from master
                    String fromServer = context.senderServerID;
                    ModLogger.info("Slave received: " + packet.key() + 
                                   " from " + fromServer);
                    
                    // Process the synchronized data
                    processSync(packet);
                }
            }
        );
    }
    
    private static void processSync(SyncDataPacket packet) {
        // Handle synchronized data
        MyDataStore.put(packet.key(), packet.value(), packet.timestamp());
    }
}
```

**Send Custom Packet**:

```java
public class SyncDataSender {
    
    public static void syncToAll(String key, String value) {
        SyncDataPacket packet = new SyncDataPacket(
            key, 
            value, 
            System.currentTimeMillis()
        );
        
        if (MultiServerManager.isMaster()) {
            // Master broadcasts to all slaves
            MultiServerManager.broadcastToSlaves(packet);
        } else if (MultiServerManager.isSlave()) {
            // Slave sends to master, which will broadcast to others
            MultiServerManager.sendToMaster(packet);
        }
    }
}
```

### Example 6: Player Tracking Across Servers

```java
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class CrossServerPlayerTracker {
    
    // Track player transfer between servers
    public static void onPlayerTransfer(ServerPlayer player, String targetServerId) {
        UUID playerUUID = player.getUUID();
        
        PlayerTransferPacket packet = new PlayerTransferPacket(
            playerUUID,
            player.getName().getString(),
            player.getX(),
            player.getY(),
            player.getZ()
        );
        
        if (MultiServerManager.isMaster()) {
            // Master sends to target slave
            MultiServerManager.sendToSlave(playerUUID, targetServerId, packet);
        } else {
            // Slave sends to master to route to target
            MultiServerManager.sendToMaster(playerUUID, packet);
        }
    }
    
    // Broadcast player action to all servers
    public static void broadcastPlayerAction(ServerPlayer player, String action) {
        UUID playerUUID = player.getUUID();
        
        PlayerActionPacket packet = new PlayerActionPacket(
            playerUUID,
            player.getName().getString(),
            action,
            System.currentTimeMillis()
        );
        
        if (MultiServerManager.isSlave()) {
            // Send to master, which broadcasts to all
            MultiServerManager.sendToMaster(playerUUID, packet);
        } else if (MultiServerManager.isMaster()) {
            // Master broadcasts to all slaves
            MultiServerManager.broadcastToSlaves(playerUUID, packet);
        }
    }
}
```

---

## Payload Reference

### Core Payloads

| Payload Type | Direction | Purpose | Fields |
|-------------|-----------|---------|--------|
| `HandshakePayload` | Slave → Master | Initial authentication | `serverId`, `token` |
| `HandshakeResultPayload` | Master → Slave | Authentication result | `result` (enum) |
| `ForwardPacketPayload` | Bidirectional | Wrap and forward custom packets | `senderPlayerUUID`, `senderServerID`, `packetType`, `data` |
| `BroadcastPayload` | Master → Slaves | Simple broadcast message | `senderName`, `fromServer`, `message` |
| `ManualDisconnectionPayload` | Master → Slave | Graceful disconnect | `reason` |

### Packet IDs

Defined in `PacketIds` class:
- `HANDSHAKE` = 0x01
- `HANDSHAKE_RESULT` = 0x02
- `BROADCAST` = 0x03
- `FORWARD_PACKET` = 0x04
- `MANUAL_DISCONNECT` = 0x05

---

## Use Cases

### 1. Multi-World Synchronization

Synchronize player inventories, achievements, or economy across survival, creative, and minigame worlds.

```java
public class InventorySyncSystem {
    
    public static void syncInventory(ServerPlayer player) {
        InventorySyncPacket packet = createInventoryPacket(player);
        
        // Broadcast to all servers
        if (MultiServerManager.isSlave()) {
            MultiServerManager.sendToMaster(player.getUUID(), packet);
        }
    }
    
    private static InventorySyncPacket createInventoryPacket(ServerPlayer player) {
        // Serialize player inventory
        return new InventorySyncPacket(
            player.getUUID(),
            serializeInventory(player.getInventory())
        );
    }
}
```

### 2. Cross-Server Chat System

Allow players on different servers to chat with each other.

```java
public class CrossServerChat {
    
    public static void sendChatMessage(ServerPlayer player, String message) {
        ChatPacket packet = new ChatPacket(
            player.getUUID(),
            player.getName().getString(),
            message,
            MultiServerManager.getSlaveID()
        );
        
        // Send to master, which broadcasts to all slaves
        MultiServerManager.sendToMaster(player.getUUID(), packet);
    }
    
    // Handler receives messages from other servers
    public static void onChatReceived(ChatPacket packet, ForwardPacketContext context) {
        String serverName = context.senderServerID;
        String formatted = String.format(
            "[%s] %s: %s", 
            serverName, 
            packet.playerName(), 
            packet.message()
        );
        
        // Broadcast to local players
        broadcastToLocalPlayers(formatted);
    }
}
```

### 3. Load Balancing

Distribute players across multiple server instances.

```java
public class LoadBalancer {
    
    public static String findBestServer() {
        if (!MultiServerManager.isMaster()) {
            return "";
        }
        
        List<String> slaves = MultiServerManager.getConnectedSlaveIDs();
        
        // Request player count from each slave
        PlayerCountRequestPacket request = new PlayerCountRequestPacket();
        
        Map<String, Integer> playerCounts = new HashMap<>();
        for (String slaveId : slaves) {
            // Would need response mechanism
            MultiServerManager.sendToSlave(slaveId, request);
        }
        
        // Return slave with lowest player count
        return findLowestLoad(playerCounts);
    }
}
```

### 4. Global Event System

Trigger events across all servers simultaneously.

```java
public class GlobalEventSystem {
    
    public static void triggerGlobalEvent(String eventType) {
        if (!MultiServerManager.isMaster()) {
            return;
        }
        
        GlobalEventPacket packet = new GlobalEventPacket(
            eventType,
            System.currentTimeMillis()
        );
        
        // Broadcast to all slaves
        MultiServerManager.broadcastToSlaves(packet);
    }
    
    public static void handleGlobalEvent(GlobalEventPacket packet) {
        switch (packet.eventType()) {
            case "boss_spawn":
                spawnBossOnThisServer();
                break;
            case "server_wide_buff":
                applyBuffToAllPlayers();
                break;
            case "announcement":
                broadcastAnnouncement(packet.data());
                break;
        }
    }
}
```

---

## Best Practices

### Security

1. **Change Default Secret**: Always change the `sharedSecret` from the default value
2. **Use Strong Secrets**: Use long, random strings (minimum 32 characters)
3. **Unique Slave IDs**: Ensure each slave has a unique identifier
4. **Firewall Configuration**: Restrict TCP port access to known server IPs
5. **Validate Packets**: Always validate packet data before processing

```java
// Example: Strong secret generation
String sharedSecret = UUID.randomUUID().toString() + UUID.randomUUID().toString();
```

### Error Handling

1. **Check Connection State**: Always verify connection before sending
2. **Handle Send Failures**: Check return values of send operations
3. **Log Errors**: Log all connection failures and packet errors
4. **Graceful Degradation**: Handle disconnections gracefully

```java
public static void safeSend(CustomPacketPayload packet) {
    if (!MultiServerManager.isRunning()) {
        ModLogger.warn("Cannot send packet: MultiServerManager not running");
        return;
    }
    
    if (MultiServerManager.isSlave()) {
        boolean sent = MultiServerManager.sendToMaster(packet);
        if (!sent) {
            ModLogger.error("Failed to send packet to master");
            // Queue for retry or handle failure
        }
    }
}
```

### Reconnection Strategy

1. **Automatic Retry**: Slave servers automatically reconnect after 5 seconds
2. **Exponential Backoff**: Consider implementing exponential backoff for repeated failures
3. **Connection Monitoring**: Monitor connection state and alert on prolonged disconnections
4. **State Resync**: Resynchronize state after reconnection

```java
private static final AtomicInteger reconnectAttempts = new AtomicInteger(0);

public static void onConnectionLost(Throwable error) {
    int attempts = reconnectAttempts.incrementAndGet();
    
    if (attempts > 10) {
        ModLogger.error("Failed to reconnect after 10 attempts");
        // Alert administrators
    } else {
        ModLogger.info("Connection lost, attempt " + attempts);
    }
}

public static void onConnectionSuccess() {
    reconnectAttempts.set(0);
    resyncServerState();
}
```

### Performance Optimization

1. **Batch Packets**: Send multiple updates in batches rather than individually
2. **Compress Large Data**: Compress large payloads before sending
3. **Avoid Flooding**: Rate-limit packet sending to prevent network congestion
4. **Selective Broadcasting**: Use exclusion lists to avoid unnecessary sends

```java
public class BatchedUpdates {
    private static final List<CustomPacketPayload> pendingPackets = new ArrayList<>();
    
    public static void queuePacket(CustomPacketPayload packet) {
        synchronized (pendingPackets) {
            pendingPackets.add(packet);
        }
    }
    
    public static void flushPending() {
        List<CustomPacketPayload> toSend;
        synchronized (pendingPackets) {
            if (pendingPackets.isEmpty()) return;
            toSend = new ArrayList<>(pendingPackets);
            pendingPackets.clear();
        }
        
        BatchPacket batch = new BatchPacket(toSend);
        MultiServerManager.sendToMaster(batch);
    }
}
```

### Thread Safety

1. **Netty Threading**: Netty handlers run on separate threads
2. **Server Thread**: Use `MinecraftServer.execute()` for game logic
3. **Synchronized Access**: Synchronize shared data structures
4. **Concurrent Collections**: Use thread-safe collections

```java
public static void handlePacketOnSlave(MyPacket packet, ForwardPacketContext context) {
    MinecraftServer server = getMinecraftServer();
    
    // Execute on server thread, not Netty thread
    server.execute(() -> {
        // Safe to access game state here
        processGameLogic(packet);
    });
}
```

---

## Troubleshooting

### Common Issues

#### 1. Connection Refused

**Problem**: Slave cannot connect to master.

**Solutions**:
- Verify master server is running and started
- Check master IP address in slave config
- Verify TCP port is not blocked by firewall
- Ensure port is not already in use

```bash
# Check if port is listening (Linux)
netstat -tulpn | grep 25575

# Test connection (from slave machine)
telnet <master-ip> 25575
```

#### 2. Authentication Failed (Invalid Shared Secret)

**Problem**: `INVALID_SHARED_SECRET` error.

**Solutions**:
- Verify `sharedSecret` matches exactly on master and slave
- Check for whitespace or encoding issues in config file
- Ensure config file is saved in UTF-8 encoding

#### 3. Slave ID Already Used

**Problem**: `SLAVE_ID_ALREADY_USED` error.

**Solutions**:
- Ensure each slave has a unique `slaveID`
- Check if another slave is already connected with same ID
- Restart master server to clear stale connections

#### 4. Packets Not Being Received

**Problem**: Packets sent but not received on other side.

**Solutions**:
- Verify packet is registered in `MultiServerPacketRegistry`
- Check packet handlers are implemented correctly
- Ensure `handleMaster()` and `handleSlave()` are both implemented
- Verify packet serialization/deserialization works correctly

```java
// Debug logging
ModLogger.info("Sending packet: " + packet.getClass().getSimpleName());
ModLogger.info("Packet registered: " + 
    MultiServerPacketRegistry.isRegistered(packet.type().id()));
```

#### 5. Reconnection Loop

**Problem**: Slave continuously disconnects and reconnects.

**Solutions**:
- Check for exceptions in packet handlers
- Verify network stability between servers
- Review logs for error patterns
- Ensure handlers don't throw unhandled exceptions

#### 6. Master Server Won't Start

**Problem**: Master server fails to bind to port.

**Solutions**:
- Check if port is already in use
- Verify user has permission to bind to port (especially < 1024)
- Try a different port number
- Check firewall allows inbound connections

```java
// Check startup failure reason
if (MultiServerManager.isStartupFailed()) {
    Throwable reason = MultiServerManager.getStartupFailReason();
    ModLogger.error("Startup failed", reason);
}
```

### Debugging Tips

1. **Enable Debug Logging**: Set log level to DEBUG for detailed information
2. **Monitor Connection State**: Log all connection/disconnection events
3. **Validate Packets**: Add validation to packet handlers
4. **Test Locally**: Test with all servers on localhost first
5. **Incremental Testing**: Start with 1 master + 1 slave before scaling

```java
// Connection state monitoring
public static void monitorConnectionState() {
    if (MultiServerManager.isMaster()) {
        List<String> slaves = MultiServerManager.getConnectedSlaveIDs();
        ModLogger.info("Connected slaves: " + slaves);
    } else if (MultiServerManager.isSlave()) {
        boolean connected = MultiServerManager.isRunning();
        ModLogger.info("Connected to master: " + connected);
    }
}
```

### Diagnostic Commands

Implement admin commands for diagnostics:

```java
public class MultiServerCommands {
    
    @Command("msinfo")
    public static void showInfo() {
        if (MultiServerManager.isMaster()) {
            info("Role: Master");
            info("Port: " + MultiServerManager.getMasterPort());
            info("IP: " + MultiServerManager.getMasterIP());
            info("Connected slaves: " + MultiServerManager.getConnectedSlaveIDs());
        } else if (MultiServerManager.isSlave()) {
            info("Role: Slave");
            info("Slave ID: " + MultiServerManager.getSlaveID());
            info("Master: " + MultiServerManager.getMasterIP() + 
                 ":" + MultiServerManager.getMasterPort());
            info("Connected: " + MultiServerManager.isRunning());
        }
    }
    
    @Command("msdisconnect")
    public static void disconnectSlave(String slaveId, String reason) {
        if (!MultiServerManager.isMaster()) {
            error("Only master can disconnect slaves");
            return;
        }
        
        MultiServerManager.disconnectSlave(slaveId, reason);
    }
}
```

---

## Additional Resources

### Related Documentation
- [Networking Overview](Networking.md)
- [NetworkPacket System](NetworkPacket.md)
- [Stream System](StreamSystem.md)

### Code References
- `MultiServerManager` - Main API entry point
- `MultiServerConfig` - Configuration management
- `MasterTCPServer` - Master server implementation
- `SlaveServerClient` - Slave client implementation
- `MultiServerPacketRegistry` - Packet registration
- `ForwardPacketHandler` - Packet handler interface

### Example Projects
Look for usage examples in the mod's test or example code demonstrating multi-server setups.

---

**Version**: 1.21.1
**Last Updated**: 2026-05-05
