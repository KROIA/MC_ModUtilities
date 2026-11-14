# NetworkManager
The abstract class `NetworkPacketManager` (`net.kroia.modutilities.networking`) is used to send and receive `NetworkPackets`.
Only one instance per mod is required.
It registers all network packet classes and holds the instances for the ARRS Requests.
On this page, "NetworkManager" means your subclass of `NetworkPacketManager`. Do not confuse it with Architectury's
`dev.architectury.networking.NetworkManager`, whose `PacketContext` the packet handlers receive.

---
## Content
- [Example Implementation](#example-implementation)
- [ARRS Usage](#arrs-usage)
- [Stream System Usage](#stream-system-usage)
- [Registration methods](#registration-methods)
- [Observing sent packets in tests](#observing-sent-packets-in-tests)


---
### Example Implementation
The implementation does not contain much logic, it contains the code for registration of packets

``` Java
public class MyExampleModNetworking extends NetworkPacketManager {
    private static final MyExampleModNetworking instance = new MyExampleModNetworking();

    public static MyExampleModNetworking getInstance(){
        return instance;
    }

    public MyExampleModNetworking() {
        super(MyExampleMod.MOD_ID);

        setupClientReceiverPackets();
        setupServerReceiverPackets();
        setupServerServerPackets();
    }

    @Override
    public void setupClientReceiverPackets()
    {
        // Server -> client packets
        registerS2C(SimpleDataPacketToClient.TYPE, SimpleDataPacketToClient.STREAM_CODEC);
    }

    @Override
    public void setupServerReceiverPackets()
    {
        // Client -> server packets
        registerC2S(SimpleDataPacketToServer.TYPE, SimpleDataPacketToServer.STREAM_CODEC);
    }

    @Override
    public void setupServerServerPackets()
    {
        // Packets that are only sent between master and slave servers (none here)
    }
}

```
All three `setup...Packets()` methods are abstract and must be implemented, even if one of them registers nothing.
Call them from the constructor; create the instance during common mod init, on both the client and the server.
---
### ARRS Usage
The codesniped shows the registration and setup of the **Asynchronous Request Response System** (ARRS).
To learn more about ARRS, click [here](ARRS.md) to visit its documentation. 

``` Java
public class MyExampleModNetworking extends NetworkPacketManager {
    ... // Instance blabla

    // Creating and registring a "Asynchronous Request Response System" (ARRS) Request object
    public static TestRequest TEST_REQUEST = (TestRequest) AsynchronousRequestResponseSystem.register(new TestRequest());

    public MyExampleModNetworking() {
        super(MyExampleMod.MOD_ID);

        setupClientReceiverPackets();
        setupServerReceiverPackets();
        setupServerServerPackets();


        // Setup the ARRS
        this.setupARRS(); 
    }

    ... // Packet registration blabla
}
```

---
### Stream System Usage
The codesniped shows the registration and setup of the **Stream System**.
To learn more about the Stream System, click [here](StreamSystem.md) to visit its documentation. 

``` Java
public class MyExampleModNetworking extends NetworkPacketManager {
    ... // Instance blabla

    // Creating and registring a Streaming object
    public static SineStream SINE_STREAM = (SineStream) StreamSystem.register(new SineStream());

    public MyExampleModNetworking() {
        super(MyExampleMod.MOD_ID);

        setupClientReceiverPackets();
        setupServerReceiverPackets();
        setupServerServerPackets();


        // Setup the Stream System
        this.setupStreamSystem();
    }

    ... // Packet registration blabla
}
```

---
### Registration methods
All methods are generic over the packet class `T extends NetworkPacket` and take its `CustomPacketPayload.Type<T>` and
`StreamCodec<RegistryFriendlyByteBuf, T>`.

| Method | Use |
|---|---|
| `registerS2C(type, codec)` | Server → client packet, handled by the packet's own `handleOnClient`. Also registers it for master/slave forwarding |
| `registerS2C(type, codec, handler)` | Same with a custom `PacketHandler`, no forwarding |
| `registerS2C(type, codec, handler, forwardHandler)` | Same with a custom `PacketHandler` and `ForwardPacketHandler` |
| `registerC2S(type, codec)` | Client → server packet, handled by `handleOnServer` (or routed to the master, see [NetworkPacket](NetworkPacket.md#routing-to-the-master-server)). Also registers it for master/slave forwarding |
| `registerC2S(type, codec, handler)` / `registerC2S(type, codec, handler, forwardHandler)` | Same with custom handlers |
| `registerS2S(type, codec)` / `registerS2S(type, codec, forwardHandler)` | Packet that is only sent between master and slave servers |

`registerS2C` is safe to call on a dedicated server: there it only registers the payload type so the server can encode the packet.

Sending: `sendToServer(packet)` on the client, `sendToClient(player, packet)` and `sendToClients(players, packet)` on the server.

---
### Observing sent packets in tests
`NetworkPacketManager.sendToClientListener` is a hook for your own tests. When it is set, every server-to-client send of
every manager (`sendToClient` and `sendToClients`) first reports each receiver and the packet to it. It is `null` by
default, and then nothing changes. Set it in a test and restore the previous value in a `finally`:
``` Java
List<NetworkPacket> sent = new ArrayList<>();
BiConsumer<ServerPlayer, NetworkPacket> previous = NetworkPacketManager.sendToClientListener;
NetworkPacketManager.sendToClientListener = (player, packet) -> {
    if (player == testPlayer) sent.add(packet);
};
try {
    doSomethingThatNotifiesThePlayer();
} finally {
    NetworkPacketManager.sendToClientListener = previous;
}
```
