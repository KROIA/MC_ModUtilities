#  Asynchronous Request Response System
The `Asynchronous Request Response System` (ARRS) works like a function on the level between two different machines, connected using the network.
This "function" takes any type of object as parameter and returns any type of object.
Since the data has to move via the network, the ARRS uses the [Networking](Networking.md) infrastructure from this utility mod.

For example, the client executes a request on the server, providing its parameters. The server receives the request with the data and does something with it.
It returns then the requested object which gets sent back to the client. On the client side is already a runnable function waiting to receive the data from the server.

This can also be done in the opposite direction.


---
## Content
- [Use Cases](#use-cases)
  - [Visualizing all players on a custom GUI](#visualizing-all-players-on-a-custom-gui)
- [Setup ARRS](#setup-arrs)
- [Create requests](#create-requests)
- [Register requests](#register-requests)
- [Sending requests](#sending-requests)
- [Master/slave requests](#masterslave-requests)
- [Request failures](#request-failures)


---

### Use Cases
#### Visualizing all players on a custom GUI
- **Goal:**   
Let's assume the mod needs to show a list of players who have ever been on the server on a GUI.

On the server side mod code is some way of gathering this information using a function. This function returns a list of strings.

``` Java
public static List<String> getAllPlayerNames() // Callable only on server side
{
    ... // blabla, doesen't matter for this use case
}
``` 

On the client side mod code is a function that takes a list of strings to visualize it to the screen or so.
``` Java
public static void visualizeList(List<String> list) // Callable only on client side
{
    ... // blabla, doesen't matter for this use case
}
```

It would be nice if the modder just could programm it the following way:
``` Java
public static void visualizeNames() // Callable only on client side
{
    // ! Will not work !
    visualizeList(getAllPlayerNames());    
}
```
Done, as simple as that... But sadly it's not that easy, Servercode can not always be called on the client side and visa versa

One way to solve the problem is by sending a [NetworkPacket](NetworkPacket.md) so that the server then also sends a [NetworkPacket](NetworkPacket.md) back with the namelist. This is asynchronous and therefore difficult to handle because two packets are used and they are independant of each other, so it may be possible that the second packet does not know what to do with the received name list.

Thats where the ARRS comes handy.
The minecraft modding API is not very nice for such kind of simple problems, thats why I implemented the ARRS.
It lets create different [Request](ARRSGenericRequest.md) classes that represent a specific request for any data.

Let's say we already have a request defined and registered in our network manager class. 
``` Java
public class MyExampleModNetworking extends NetworkPacketManager {
    ... // Instance blabla

    // Creating and registring a "Asynchronous Request Response System" (ARRS) Request object
    public static PlayerNameRequest GET_PLAYER_NAMES_REQUEST = (PlayerNameRequest) AsynchronousRequestResponseSystem.register(new PlayerNameRequest());

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


> [!NOTE]
> **This "function" takes any type of object as parameter and returns any type of object.**
> A input is always required, but can be as simple as it needs.
>
> This means using the ARRS the functions look symbolicly like this
> ``` Java
> List<String> getAllPlayerNames(int input) // Callable only on server side
> {
> }
>
> void visualizeList(List<String> list) // Callable only on client side
> {
> }
>
> visualizeList(getAllPlayerNames(0));   
> ``` 



With that we could just do something like:
``` Java
public static void visualizeNames() // Callable only on client side 
{
    int dummy = 0; // dummy input data which gets sent to the server
    MyExampleModNetworking.GET_PLAYER_NAMES_REQUEST.sendRequestToServer(dummy).thenAccept(receivedPlayerList -> {
            // This section gets called when the responce was received (asynchronous)
            if (receivedPlayerList != null)
                visualizeList(receivedPlayerList);
            // Further process the list or something...
        });

    // Do not work on the data (playerlist in this example) directly after sending the request, 
    // because it is not received yet!
}
```


---
### Setup ARRS
The setup is done inside the [NetworkManager](NetworkManager.md#arrs-usage) and pretty simple.
Just call the `setupARRS()` inside the NetworkManager constructor and register static instances of the `requests` as shown in the example code.

---
### Create requests
Click [here](ARRSGenericRequest.md) to learn how to create requets.

---
### Register requests
Register each request class once by calling the `AsynchronousRequestResponseSystem.register()` function.
Save the returned instance in a static member.

``` Java
public class MyExampleModNetworking extends NetworkPacketManager {
    ... // Instance blabla

    // Creating and registring a "Asynchronous Request Response System" (ARRS) Request object
    public static TestRequest TEST_REQUEST = (TestRequest) AsynchronousRequestResponseSystem.register(new TestRequest());

    ... // Packet registration blabla
}

```

---
### Sending requests
Sending a request returns a `CompletableFuture` that completes with the response.

Sending a request from the **Client** to the **Server**:
``` Java
int inputData = 5;
CompletableFuture<String> future = MyExampleModNetworking.TEST_REQUEST.sendRequestToServer(inputData);
future.thenAccept(outputData -> System.out.println("Response received from server: " + outputData));
```

``` Java
int inputData = 5;
CompletableFuture<String> future = AsynchronousRequestResponseSystem.sendRequestToServer(
                MyExampleModNetworking.TEST_REQUEST,
                inputData);
```
Both variants have the same effect. If no buffer could be created (for example while the client is not in a world),
nothing is sent and the returned future is already completed with `null`, so check the response for `null`.

---
### Master/slave requests
In a [multi-server setup](MultiServerNetworking.md), servers can send requests to each other:
``` Java
// On a slave
CompletableFuture<String> fromMaster = MyExampleModNetworking.TEST_REQUEST.sendRequestToMaster(inputData);
// On the master
CompletableFuture<String> fromSlave = MyExampleModNetworking.TEST_REQUEST.sendRequestToSlave("slave_1", inputData);
```
The receiver handles them in `handleOnMasterServer(input, slaveID, playerSender)` or `handleOnSlaveServer(input, playerSender)`.
If no server buffer could be created (server not running), nothing is sent and the future is already completed with `null`.
The futures complete on the server thread.

A client request whose class overrides `needsRoutingToMaster()` to return `true` is forwarded by a slave to the master.
The master's response is relayed back to the client. The slave can filter it on the way with
[`transformResponseOnSlave`](ARRSGenericRequest.md#transformresponseonslave).

---
### Request failures
By default a request that never gets a response stays pending forever, and its future never completes.
Set the switch below to let such requests fail instead:
``` Java
RequestManager.ENABLE_REQUEST_FAILURES = true;
```
The switch is off by default. It is JVM-wide and covers every mod that uses the ARRS, so turn it on only when every
caller that moves value (money, items) on failure can tell the two failure types below apart.

While it is on:
- Pending requests time out. The sweep runs every 20 server and client ticks. The timeout comes from
  [`getTimeoutMs()`](ARRSGenericRequest.md#gettimeoutms) (default `RequestManager.DEFAULT_TIMEOUT_MS`, 30 s).
- A master/slave send that is refused (link not connected) fails at once.
- When a master/slave connection is lost, the requests sent over it fail.
- Client requests that a slave forwards to the master fail on the client as well (see below).

A future can fail with two exceptions (both in `net.kroia.modutilities.networking.client_server.arrs`):

| Exception | Meaning | What to do |
|-----------|---------|------------|
| `RequestNotSentException` (extends `IllegalStateException`) | The request was never sent, e.g. the target server was not connected. | Safe to refund or retry. |
| `RequestOutcomeUnknownException` (extends `TimeoutException`) | The request was sent, then it timed out or the connection was lost. The receiver may have applied it. | Do not refund or retry blindly. Ask the receiver for the current state first. |

Treat any other exception like an unknown outcome. `CompletableFuture` callbacks may wrap the cause in a
`CompletionException`, so unwrap it before you check the type:
``` Java
MyExampleModNetworking.TRANSFER_REQUEST.sendRequestToMaster(input).whenComplete((result, error) -> {
    Throwable cause = error instanceof CompletionException ? error.getCause() : error;
    if (cause instanceof RequestNotSentException) {
        refund();                 // never reached the master
    } else if (cause != null) {
        markPending();            // outcome unknown, check the state later
    } else if (result == null) {
        refund();                 // no buffer, nothing was sent
    } else {
        onSuccess(result);
    }
});
```
On the client, `sendRequestToServer` fails with `RequestOutcomeUnknownException` on timeout. The client's own send
is not checked. For a request that a slave forwards to the master, the client gets `RequestNotSentException` if the
slave could not forward it, and `RequestOutcomeUnknownException` if the slave lost the master link after forwarding it.

On the wire, `GenericResponsePacket` carries a status byte: `STATUS_OK` (0), `STATUS_NOT_SENT` (1) and
`STATUS_OUTCOME_UNKNOWN` (2). A slave uses it to pass a failed forward on to the client, which maps it to the matching exception.

`RequestManager.cleanupExpiredRequests(boolean clientSide)` fails the expired requests on one side and returns their number.
It replaces the former `cleanupExpiredRequests(long maxAgeMs)`. The library calls it from its tick, so you normally do not need it.

---
