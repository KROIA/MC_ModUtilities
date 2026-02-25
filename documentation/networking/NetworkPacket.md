# NetworkPacket
A `NetworkPacket` is a object, containing data which gets sent to the server or the client via the minecraft networking infrastructure.
The class implements methodes for encoding and deconding the data needed for the transmission.
It also provides functions which can be overwritten to process the received packeds.

---
## Content
- [Example Implementation](#example-implementation)
  - [Server :arrow_right: Client packet](#server-️-client-packett)
  - [Client :arrow_right: Server packet](#client---server-packet)
- [:link: Packet Registration](#packet-registration)
- [:arrow_heading_up: Sending a Packet](#sending-a-packet)
- [:arrow_heading_down: Receiving a Packet](#receiving-a-packet)


---
### Example Implementation
#### Server :arrow_right: Client packet
This simple example packet sends a integer value from the server to a client. 
It gets printed to the console on the client side.
Visit the [NetworkManager](NetworkManager.md) documentation the see what the [MyExampleModNetworking](NetworkManager.md#example-implementation) class is.

``` Java
public class SimpleDataPacketToClient extends NetworkPacket {

    // Data to be sent
    int value;

    public SimpleDataPacketToClient(int value) {
        super();
        this.value = value;
    }
    public SimpleDataPacketToClient(FriendlyByteBuf friendlyByteBuf) {
        super(friendlyByteBuf);
    }

    // Creates a packet and sends it using the MyExampleModNetworking
    public static void sendPacket(ServerPlayer receiver, int value) {
        MyExampleModNetworking.getInstance().sendToClient(receiver, new SimpleDataPacketToClient(value));
    }


    // Called when the client has received a packet from the server
    @Override
    protected void handleOnClient()
    {
        System.out.println("[CLIENT SIDE] Received value from server: " + value);
    }

    // Fill the data we want to send in the "buf"
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(value);
    }

    // Read the received "buf" and save the data back to the members
    @Override
    public void decode(FriendlyByteBuf buf) {
        value = buf.readInt();
    }
}
```

#### Client :arrow_right: Server packet
This simple example packet sends a integer value from a cleint to the server. 
It gets printed to the console on the server side.
Visit the [NetworkManager](NetworkManager.md) documentation the see what the [MyExampleModNetworking](NetworkManager.md#example-implementation) class is.

``` Java
public class SimpleDataPacketToServer extends NetworkPacket {

    // Data to be sent
    int value;

    public SimpleDataPacketToServer(int value) {
        super();
        this.value = value;
    }
    public SimpleDataPacketToServer(FriendlyByteBuf friendlyByteBuf) {
        super(friendlyByteBuf);
    }

    // Creates a packet and sends it using the MyExampleModNetworking
    public static void sendPacket(int value) {
        MyExampleModNetworking.getInstance().sendToServer(new SimpleDataPacketToServer(value));
    }


    // Called when the server has received a packet from the client
    @Override
    protected void handleOnServer(ServerPlayer sender)
    {
        System.out.println("[SERVER SIDE] Received value from client: " + value);
    }

    // Fill the data we want to send in the "buf"
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(value);
    }

    // Read the received "buf" and save the data back to the members
    @Override
    public void decode(FriendlyByteBuf buf) {
        value = buf.readInt();
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
Receiving a packet is done automatically by the NetworkManager
It automatically creates a instance of the packet that gets received on the target (client/server).
It then calls the `fromBytes` function to parse the received byteBuf back to the member variables of the packet.
After that, depending on the enviroment (client/server) one of the below functions get called.
Place the code here, that needs to be executed when the packet gets received.


``` Java
public class SimpleDataPacketToClient extends NetworkPacket {
    ...
    // Called when the client has received a packet from the server
    @Override
    protected void handleOnClient()
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
    protected void handleOnServer(ServerPlayer sender)
    {
        System.out.println("[SERVER SIDE] Received value from client: " + value);
    }
    ...
}
```
