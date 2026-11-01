# ARRS Request
The `GenericRequest` class works like a function on the level between two different machines, connected using the network.
This "function" takes any type of object as parameter and returns any type of object.


---
## Content
- [Example Implementation](#example-implementation)
  - [Functions in detail](#functions-in-detail)



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
    public void encodeInput(FriendlyByteBuf buf, Integer input) {
        ByteBufCodecs.INT.encode(buf, input);
    }

    @Override
    public void encodeOutput(FriendlyByteBuf buf, String output) {
        ByteBufCodecs.STRING_UTF8.encode(buf, output);
    }

    @Override
    public Integer decodeInput(FriendlyByteBuf buf) {
        return ByteBufCodecs.INT.decode(buf);
    }

    @Override
    public String decodeOutput(FriendlyByteBuf buf) {
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
public void encodeInput(FriendlyByteBuf buf, Integer input) {
    ByteBufCodecs.INT.encode(buf, input);
}
```
The `encodeInput` function fills the `buf` with the `input` data. Only the data stored in the `buf` will be sent to the request receiver.
Use the new CODEC feature for encoding
<br>

##### encodeOutput()
``` Java
@Override
public void encodeOutput(FriendlyByteBuf buf, String output) {
    ByteBufCodecs.STRING_UTF8.encode(buf, output);
}
```
The response value also needs to be stored in the `buf` in order to send it to the requestor.
<br>

##### decodeInput()
``` Java
@Override
public Integer decodeInput(FriendlyByteBuf buf) {
    return ByteBufCodecs.INT.decode(buf);
}
``` 
On the request receiver side, decoding the `buf` back to its original type is needed in order to pass it to the request handler function
<br>

##### decodeOutput()
``` Java
@Override
public String decodeOutput(FriendlyByteBuf buf) {
    return ByteBufCodecs.STRING_UTF8.decode(buf);
}
``` 
On the request side, decoding the `buf` back to its original type is needed in order to pass it to the requestor.  