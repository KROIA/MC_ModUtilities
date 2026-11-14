# NetworkPacket
A `NetworkPacket` is a object, containing data which gets sent to the server or the client via the minecraft networking infrastructure.
The class implements methodes for encoding and deconding the data needed for the transmission.
It also provides functions which can be overwritten to process the received packeds.

---
## Content
- [Example Implementation](#example-implementation)
  - [Server :arrow_right: Client packet](#server-arrow_right-client-packet)
  - [Client :arrow_right: Server packet](#client-arrow_right-server-packet)
- [:link: Packet Registration](#packet-registration)
- [:arrow_heading_up: Sending a Packet](#sending-a-packet)
- [:arrow_heading_down: Receiving a Packet](#receiving-a-packet)
- [Routing to the master server](#routing-to-the-master-server)


---
### Example Implementation
#### Server :arrow_right: Client packet
This simple example packet sends a integer value from the server to a client. 
It gets printed to the console on the client side.
Visit the [NetworkManager](NetworkManager.md) documentation the see what the [MyExampleModNetworking](NetworkManager.md#example-implementation) class is.

``` Java
public class SimpleDataPacketToClient extends NetworkPacket {
    public static final Type<SimpleDataPacketToClient> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "simple_data_packet_to_client"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SimpleDataPacketToClient> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.value,
            SimpleDataPacketToClient::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    // Data to be sent
    int value;

    public SimpleDataPacketToClient(int value) {
        super();
        this.value = value;
    }

    // Creates a packet and sends it using the MyExampleModNetworking
    public static void sendPacket(ServerPlayer receiver, int value) {
        MyExampleModNetworking.getInstance().sendToClient(receiver, new SimpleDataPacketToClient(value));
    }
    
    // Called when the client has received a packet from the server
    @Override
    protected void handleOnClient(NetworkManager.PacketContext context)
    {
        System.out.println("[CLIENT SIDE] Received value from server: " + value);
    }

    @Override
    protected void handleOnServer(NetworkManager.PacketContext context) {

    }
}
```

#### Client :arrow_right: Server packet
This simple example packet sends a integer value from a cleint to the server. 
It gets printed to the console on the server side.
Visit the [NetworkManager](NetworkManager.md) documentation the see what the [MyExampleModNetworking](NetworkManager.md#example-implementation) class is.

``` Java
public class SimpleDataPacketToServer extends NetworkPacket {
    public static final Type<SimpleDataPacketToServer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, "simple_data_packet_to_server"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SimpleDataPacketToServer> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.value,
            SimpleDataPacketToServer::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    // Data to be sent
    int value;

    public SimpleDataPacketToServer(int value) {
        super();
        this.value = value;
    }

    // Creates a packet and sends it using the MyExampleModNetworking
    public static void sendPacket(ServerPlayer receiver, int value) {
        MyExampleModNetworking.getInstance().sendToServer(new SimpleDataPacketToServer(value));
    }
    
    // Called when the client has received a packet from the server
    @Override
    protected void handleOnClient(NetworkManager.PacketContext context)
    {
        
    }

    @Override
    protected void handleOnServer(NetworkManager.PacketContext context) {
        System.out.println("[SERVER SIDE] Received value from client: " + value);
    }
}
```
---
### Packet Registration
Before a packet can be sent, it must be registred.
Visit the [NetworkManager](NetworkManager.md) documentation and have a look at the [MyExampleModNetworking](NetworkManager.md#example-implementation) class which shows how to register packets.

---
### Sending a Packet
Since a static function is already implemented in the packet it self, it is very easy to send the packet.
Otherwise a packet can also be sent manually from outside the packet itself.

Just create a instance of the packet, and pass it to the NetworkManager instance.
Use the `sendToClient` or `sendToServer` functions, depending on your needs.

``` Java
public class SimpleDataPacketToClient extends NetworkPacket {
    ...
    // Creates a packet and sends it using the MyExampleModNetworking
    public static void sendPacket(ServerPlayer receiver, int value) {
        MyExampleModNetworking.getInstance().sendToClient(receiver, new SimpleDataPacketToClient(value));
    }
    ...
}
```
---
### Receiving a Packet
Receiving a packet is done automatically by the NetworkManager.
It decodes the received bytes with the packet's `STREAM_CODEC`, which creates an instance of the packet on the target (client/server).
After that, depending on the side (client/server), one of the below functions gets called on the main thread.
Place the code here, that needs to be executed when the packet gets received.


``` Java
public class SimpleDataPacketToClient extends NetworkPacket {
    ...
    // Called when the client has received a packet from the server
    @Override
    protected void handleOnClient(NetworkManager.PacketContext context)
    {
        System.out.println("[CLIENT SIDE] Received value from server: " + value);
    }
    ...
}
```

``` Java
public class SimpleDataPacketToServer extends NetworkPacket {
    ...
    // Called when the server has received a packet from the client
    @Override
    protected void handleOnServer(NetworkManager.PacketContext context)
    {
        ServerPlayer sender = (ServerPlayer) context.getPlayer();
        System.out.println("[SERVER SIDE] Received value from " + sender.getName().getString() + ": " + value);
    }
    ...
}
```
`NetworkManager.PacketContext` is Architectury's `dev.architectury.networking.NetworkManager.PacketContext`. On the server,
`context.getPlayer()` is the sending `ServerPlayer`.

---
### Routing to the master server
In a [multi-server setup](MultiServerNetworking.md), a client packet can be handled on the master instead of the slave
the player is connected to. Override `needsRoutingToMaster()` to return `true`; the master then handles the packet in
`handleOnMaster(ForwardPacketContext context)`.

Routing goes by the server's role, not by the link state:
- On a slave (`MultiServerManager.isSlave()`), a packet with `needsRoutingToMaster()` is passed to `routeToMaster(context)`.
  The slave never handles such a packet in `handleOnServer`, not even while its master link is down.
- On a standalone or master server, every client packet is handled in `handleOnServer`.

The default `routeToMaster` sends the packet to the master with the sending player's UUID. While the master link is down,
that send is refused and the packet is dropped with a rate-limited log line. Override `routeToMaster` to react to a refused
send, for example to answer the client:
``` Java
@Override
protected boolean needsRoutingToMaster() { return true; }

@Override
protected void routeToMaster(NetworkManager.PacketContext context) {
    ServerPlayer player = (ServerPlayer) context.getPlayer(); // routeToMaster only runs on the server
    if (!MultiServerManager.sendToMaster(player.getUUID(), this)) {
        PlayerCompat.sendMessage(player, Component.literal("The main server is not reachable, try again later."), false);
    }
}
```

> [!NOTE]
> The former `NetworkPacketC2S` and `NetworkPacketS2C` base classes were removed. Extend `NetworkPacket` for both directions.
