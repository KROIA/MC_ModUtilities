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
public class MyExampleModNetworking extends NetworkManager {
    ... // Instance blabla

    // Creating and registring a "Asynchronous Request Response System" (ARRS) Request object
    public static PlayerNameRequest GET_PLAYER_NAMES_REQUEST = (PlayerNameRequest) AsynchronousRequestResponseSystem.register(new PlayerNameRequest());

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


> [NOTE]
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
    MyExampleModNetworking.getInstance().GET_PLAYER_NAMES_REQUEST.sendRequestToServer(dummy, (receivedPlayerList) -> {
            // This section gets called when the responce was received (asynchronous)
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
public class MyExampleModNetworking extends NetworkManager {
    ... // Instance blabla

    // Creating and registring a "Asynchronous Request Response System" (ARRS) Request object
    public static TestRequest TEST_REQUEST = (TestRequest) AsynchronousRequestResponseSystem.register(new TestRequest());

    ... // Packet registration blabla
}

```

---
### Sending requests
Sending a request from the **Server** to the **Client** can be done the following ways:
``` Java
ServerPlayer receiver = ...;
int inputData = 5;
MyExampleModNetworking.TEST_REQUEST.sendRequestToClient(inputData, receiver, (outputData, player) -> {
            System.out.println("Response received from client: " + outputData);
        });
```


``` Java
ServerPlayer receiver = ...;
int inputData = 5;
AsynchronousRequestResponseSystem.sendRequestToClient(
                MyExampleModNetworking.TEST_REQUEST,
                inputData,
                receiver,
                (outputData, player) -> {
                    System.out.println("Response received from client: " + outputData);
                }
        );
```

Both variants have the same effect.

Sending a request from the **Client** to the **Server** can be done the following ways:
``` Java
int inputData = 5;
MyExampleModNetworking.TEST_REQUEST.sendRequestToServer(inputData, (outputData) -> {
            System.out.println("Response received from server: " + outputData);
        });
```


``` Java
int inputData = 5;
AsynchronousRequestResponseSystem.sendRequestToServer(
                MyExampleModNetworking.TEST_REQUEST,
                inputData,
                (outputData) -> {
                    System.out.println("Response received from server: " + outputData);
                }
        );
```
Again both variants have the same effect.

---
