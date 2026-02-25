# NetworkManager
The `NetworkManager` class is used to send and receive `NetworkPackets`. 
Only one Instance per mod is required. 
It registers all network packet classes and holds the instances for the ARRS Requests.

---
## Content
- [Example Implementation](#example-implementation)
- [ARRS Usage](#arrs-usage)
- [Stream System Usage](#stream-system-usage)


---
### Example Implementation
The implementation does not contain much logic, it contains the code for registration of packets

``` Java
public class MyExampleModNetworking extends NetworkManager {
    private static final MyExampleModNetworking instance = new MyExampleModNetworking();

    public static MyExampleModNetworking getInstance(){
        return instance;
    }

    public MyExampleModNetworking() {
        super(MyExampleMod.MOD_ID);

        setupClientReceiverPackets();
        setupServerReceiverPackets();
    }

    @Override
    public void setupClientReceiverPackets()
    {
        // Register packets
        register(SimpleDataPacketToClient.class, SimpleDataPacketToClient::encode, SimpleDataPacketToClient::new, SimpleDataPacketToClient::receive);
    }

    @Override
    public void setupServerReceiverPackets()
    {
        // Register packets
        register(SimpleDataPacketToServer.class, SimpleDataPacketToServer::encode, SimpleDataPacketToServer::new, SimpleDataPacketToServer::receive);
    }
}

```
---
### ARRS Usage
The codesniped shows the registration and setup of the **Asynchronous Request Response System** (ARRS).
To learn more about ARRS, click [here](ARRS.md) to visit its documentation. 

``` Java
public class MyExampleModNetworking extends NetworkManager {
    ... // Instance blabla

    // Creating and registring a "Asynchronous Request Response System" (ARRS) Request object
    public static TestRequest TEST_REQUEST = (TestRequest) AsynchronousRequestResponseSystem.register(new TestRequest());

    public MyExampleModNetworking() {
        super(MyExampleMod.MOD_ID);

        setupClientReceiverPackets();
        setupServerReceiverPackets();

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
public class MyExampleModNetworking extends NetworkManager {
    ... // Instance blabla

    // Creating and registring a Streaming object
    public static SineStream SIN_STREAM = (SinusStream) StreamSystem.register(new SinusStream());

    public MyExampleModNetworking() {
        super(MyExampleMod.MOD_ID);

        setupClientReceiverPackets();
        setupServerReceiverPackets();

        // Setup the Stream System
        this.setupStreamSystem();
    }

    ... // Packet registration blabla
}
```