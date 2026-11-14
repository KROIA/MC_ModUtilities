# ARRS Request
The `GenericRequest` class works like a function on the level between two different machines, connected using the network.
This "function" takes any type of object as parameter and returns any type of object.


---
## Content
- [Example Implementation](#example-implementation)
  - [Functions in detail](#functions-in-detail)
- [Optional overrides](#optional-overrides)
  - [needsRoutingToMaster()](#needsroutingtomaster)
  - [getTimeoutMs()](#gettimeoutms)
  - [transformResponseOnSlave()](#transformresponseonslave)



---
### Example Implementation
This example creates a request called `TestRequest` which takes an `Integer` and returns a `String`.
> [!NOTE] Pseudocode for this example:
> ``` Java
> String testRequest_handleOnClient(Integer input)
> {
>   return "Hello, your request has been processed successfully by the client! Input: " + input;
> }
> String testRequest_handleOnServer(Integer input)
> {
>   return "Hello, your request has been processed successfully by the server! Input: " + input;
> }
> ```

``` Java
// Define the Request to receive an Integer and respont with a String
public class TestRequest extends GenericRequest<Integer, String>
{
    @Override
    public String getRequestTypeID() {
        return TestRequest.class.getName();
    }

    @Override
    public CompletableFuture<String> handleOnServer(Integer input, ServerPlayer sender) {
        return CompletableFuture.completedFuture("Hello, your request has been processed successfully by the server! Input: " + input);
    }

    @Override
    public void encodeInput(RegistryFriendlyByteBuf buf, Integer input) {
        ByteBufCodecs.INT.encode(buf, input);
    }

    @Override
    public void encodeOutput(RegistryFriendlyByteBuf buf, String output) {
        ByteBufCodecs.STRING_UTF8.encode(buf, output);
    }

    @Override
    public Integer decodeInput(RegistryFriendlyByteBuf buf) {
        return ByteBufCodecs.INT.decode(buf);
    }

    @Override
    public String decodeOutput(RegistryFriendlyByteBuf buf) {
        return ByteBufCodecs.STRING_UTF8.decode(buf);
    }
}
```

#### Functions in detail
##### getRequestTypeID() 
``` Java
@Override
public String getRequestTypeID() {
    return TestRequest.class.getName();
}
``` 
The `getRequestTypeID` function needs to return a constant string. It is used to identify each request class. 
Returning  `TestRequest.class.getName()` or `TestRequest.class.getSimpleName()` is also fine, as long as there is no other event with the same name.
<br>

##### handleOnServer()
``` Java
@Override
public CompletableFuture<String> handleOnServer(Integer input, ServerPlayer sender) {
    return CompletableFuture.completedFuture("Hello, your request has been processed successfully by the server! Input: " + input);
}
```
The `handleOnServer` function gets called on the server sied when receiving a request from the client.
The input is provided by the client and also the clients `ServerPlayer` instance is provided. In case it is not needed to process the request.
The return value gets sent back to the client.
Returning the value wrapped in a CompletableFuture<> to be able to have time for gathering the data for the response.
Once the Future is completed, the response packet gets sent automatically.
<br>

##### encodeInput()
``` Java
@Override
public void encodeInput(RegistryFriendlyByteBuf buf, Integer input) {
    ByteBufCodecs.INT.encode(buf, input);
}
```
The `encodeInput` function fills the `buf` with the `input` data. Only the data stored in the `buf` will be sent to the request receiver.
Use the new CODEC feature for encoding
<br>

##### encodeOutput()
``` Java
@Override
public void encodeOutput(RegistryFriendlyByteBuf buf, String output) {
    ByteBufCodecs.STRING_UTF8.encode(buf, output);
}
```
The response value also needs to be stored in the `buf` in order to send it to the requestor.
<br>

##### decodeInput()
``` Java
@Override
public Integer decodeInput(RegistryFriendlyByteBuf buf) {
    return ByteBufCodecs.INT.decode(buf);
}
``` 
On the request receiver side, decoding the `buf` back to its original type is needed in order to pass it to the request handler function
<br>

##### decodeOutput()
``` Java
@Override
public String decodeOutput(RegistryFriendlyByteBuf buf) {
    return ByteBufCodecs.STRING_UTF8.decode(buf);
}
``` 
On the request side, decoding the `buf` back to its original type is needed in order to pass it to the requestor.
<br>


---
### Optional overrides
These methods have defaults. Override them only when a request needs different behaviour.

##### needsRoutingToMaster()
``` Java
@Override
public boolean needsRoutingToMaster() { return true; }
```
Default `false`. In a master/slave setup, a slave forwards client requests of this type to the master,
which handles them in `handleOnMasterServer(input, slaveID, playerSender)`. The master's response is relayed back
to the client. On a standalone or master server the request is handled locally in `handleOnServer`.
<br>

##### getTimeoutMs()
``` Java
@Override
public long getTimeoutMs() { return 60_000; }
```
How long the requester waits for a response before the future fails with a `RequestOutcomeUnknownException`.
Default `RequestManager.DEFAULT_TIMEOUT_MS` (30 s). Override it for requests that legitimately take longer.
The timeout only applies while `RequestManager.ENABLE_REQUEST_FAILURES` is on; see [Request failures](ARRS.md#request-failures).
<br>

##### transformResponseOnSlave()
For a request whose output type is `List<Entry>`:
``` Java
@Override
public RegistryFriendlyByteBuf transformResponseOnSlave(RegistryFriendlyByteBuf response, UUID playerSender) {
    List<Entry> entries = decodeOutput(response);
    entries.removeIf(e -> !isKnownOnThisServer(e));
    RegistryFriendlyByteBuf filtered = UtilitiesPlatform.createRegistryFriendlyByteBufServerSide();
    encodeOutput(filtered, entries);
    return filtered;
}
```
Lets a slave change the master's response before it relays it to the client, for example to drop entries this
slave cannot resolve. It is called on a slave only, for client requests the slave forwarded to the master
(`needsRoutingToMaster()` returns `true`), and only for regular responses, never for failures.

- `response` holds the master's encoded output. You may read it with `decodeOutput`, but do not release it.
- `playerSender` is the UUID of the player the response is relayed to.
- Return `response` itself to relay it unchanged (its reader index is reset first), or return a new buffer that
  holds the filtered output written with `encodeOutput`.

The default returns `response` and relays the master's bytes without decoding them.
