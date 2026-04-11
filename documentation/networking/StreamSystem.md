# Stream System
The stream system allows multiple packets to be sent from the server to the client or vice versa.
A stream can contain custom payloads.
Multiple streams of the same type can run concurrently.



---
## Content
- [Use Cases](#use-cases)
- [Setup Stream System](#setup-stream-system)
- [Create Streams](#create-streams)
- [Register Streams](#register-streams)
- [Starting a Stream](#starting-a-stream)
- [Stoping a Stream](#stoping-a-stream)




---
### Use Cases
- Updating a Screen on the client with data that changes on the server and polling is not realy suted in such a case.
- Sending huge data packets in chunks to spread the payload and therefore reduce the size of that packet.


--- 
### Setup Stream System
The setup is done inside the [NetworkManager](NetworkManager.md#stream-system-usage) and pretty simple.
Just call the `setupARRS()` inside the NetworkManager constructor and register static instances of the `requests` as shown in the example code.

---
### Create Streams
Click [here](GenericStream.md) to learn how to create streams.

---
### Register Streams
Register each stream class once by calling the `StreamSystem.register()` function.
Save the returned instance in a static member.
You can find an example here: [NetworkManager](NetworkManager.md#stream-system-usage)

``` Java
public class MyExampleModNetworking extends NetworkManager {
    ... // Instance blabla

    // Creating and registring a Streaming object
    public static SineStream SIN_STREAM = (SinusStream) StreamSystem.register(new SinusStream());

    ... // Packet registration blabla
}

```

--- 
### Starting a Stream
Let's say we use the [SineStream](GenericStream.md#example-implementation) which is used as a server->client stream.


``` Java
public class ExampleScreen extends GuiScreen {

    private final UUID sinusStreamID;

    public ExampleScreen()
    {
        // Store the returned stream ID
        sinusStreamID = MyExampleModNetworking.SINUS_STREAM.startServerToClient(0.0f,
            (value)-> // Callback handler for stream data
            {
                LOGGER.info("New value: " + value);
            },
            ()-> // Callback handler for stream stopped event
            {
                // Stream stopped handler
                LOGGER.info("[UI] SineStream stopped");
            });
    }
}

```


--- 
### Stoping a Stream
To stop the stream you need the Stream ID which is returned when the stream gets started.
I this example the Stream gets stopped once the Screen gets closed.
Otherwise the server will keep sending data even if the screen is not open anymore.

``` Java
public class ExampleScreen extends GuiScreen {

    private final UUID sinusStreamID;

    public ExampleScreen()
    {
        ...
    }

    @Override
    public void onClose() {
        super.onClose();
        StreamSystem.stopStream(sinusStreamID); // Stop the stream using the stream ID
    }
}

```