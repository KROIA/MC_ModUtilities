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
        return "TestRequest_"+Integer.TYPE.getName()+"_"+String.class.getName();
    }

    @Override
    public String handleOnClient(Integer input) {
        return "Hello, your request has been processed successfully by the client! Input: " + input;
    }
    @Override
    public String handleOnServer(Integer input, ServerPlayer sender) {
        return "Hello, your request has been processed successfully by the server! Input: " + input;
    }

    @Override
    public void encodeInput(FriendlyByteBuf buf, Integer input) {
        if (input != null) {
            buf.writeInt(input.intValue());
        } else {
            buf.writeInt(0); // or some default value
        }
    }

    @Override
    public void encodeOutput(FriendlyByteBuf buf, String output) {
        if (output != null) {
            buf.writeUtf(output);
        } else {
            buf.writeUtf(""); // or some default value
        }
    }

    @Override
    public Integer decodeInput(FriendlyByteBuf buf) {
        int value = buf.readInt();
        return value;
    }

    @Override
    public String decodeOutput(FriendlyByteBuf buf) {
        return buf.readUtf(); // Assuming output is a String
    }
}
```

#### Functions in detail
##### getRequestTypeID() 
``` Java
@Override
public String getRequestTypeID() {
    return "TestRequest_"+Integer.TYPE.getName()+"_"+String.class.getName();
}
``` 
The `getRequestTypeID` function needs to return a constant string. It is used to identify each request class. 
Returning  `TestRequest.class.getName()` or `TestRequest.class.getSimpleName()` is also fine, as long as there is no other event with the same name.
<br>

##### handleOnClient()
``` Java
@Override
public String handleOnClient(Integer input) {
    return "Hello, your request has been processed successfully by the client! Input: " + input;
}
``` 
The `handleOnClient` function gets called on the client side when receiving a request from the server.
The input is provided by the server and the return value gets sent as response.
<br>

##### handleOnServer()
``` Java
@Override
public String handleOnServer(Integer input, ServerPlayer sender) {
    return "Hello, your request has been processed successfully by the server! Input: " + input;
}
```
The `handleOnServer` function gets called on the server sied when receiving a request from the client.
The input is provided by the client and also the clients `ServerPlayer` instance is provided in case it is needed to process the request.
The return value gets sent back to the client.
<br>

##### encodeInput()
``` Java
@Override
public void encodeInput(FriendlyByteBuf buf, Integer input) {
    if (input != null) {
        buf.writeInt(input.intValue());
    } else {
        buf.writeInt(0); // or some default value
    }
}
```
The `encodeInput` function fills the `buf` with the `input` data. Only the data stored in the `buf` will be sent to the request receiver.
<br>

##### encodeOutput()
``` Java
@Override
public void encodeOutput(FriendlyByteBuf buf, String output) {
    if (output != null) {
        buf.writeUtf(output);
    } else {
        buf.writeUtf(""); // or some default value
    }
}
```
The response value also needs to be stored in the `buf` in order to send it to the requestor.
<br>

##### decodeInput()
``` Java
@Override
public Integer decodeInput(FriendlyByteBuf buf) {
    int value = buf.readInt();
    return value;
}
``` 
On the request receiver side, decoding the `buf` back to its original type is needed in order to pass it to the handle function
<br>

##### decodeOutput()
``` Java
@Override
public String decodeOutput(FriendlyByteBuf buf) {
    return buf.readUtf(); // Assuming output is a String
}
``` 
On the request side, decoding the `buf` back to its original type is needed in order to pass it to the requestor.  