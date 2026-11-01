# Stream
A Stream object is a object that can create new Streaming packets which get sent to the server or client.
The Stream object gets updated once per tick in which logic can be placed to decide when the next packet must be sent.
A Stream gets triggered by that side which needs the Stream data.
A Stream can not be started on that side which then sends the Streaming data since no destination is defined on the other side
to hande the incomming packets.

When a Stream gets started, the requestor of the Stream can provide additional data to the Stream.
The data gets sent to the other side (server or client) and can be used as context data for sending Streaming packets.
The type of the data provided during the start of a Stream is defined in the Stream object class.
The type of the data that the Stream will provide is also defined in the Stream object class.


---
## Content
- [Example Implementation](#example-implementation)


---
### Example Implementation
This example Stream sends a sin wave to the client.
For demonstration purposes a float is used as `context data`, the data provided on the start of the stream.
For each Stream a copy of the Stream object is created, that way, temporal data about the streaming state can be stored in this class.


``` Java
public class SineStream extends GenericStream<Float, Double> {

    private double lastTime = 0.0;
    private int counter = 0;

    @Override
    public SineStream copy() {
        return new SineStream(); // Provides a factory to instantiate a new derived Stream object
    }
    @Override
    public String getStreamTypeID() {
        return SineStream.class.getSimpleName(); // Unique identifier used in the Stream registry
    }

    @Override
    public void onStartStreamSendingOnSever() {
        // Gets called at the start, if it is a "server->client" Stream
        ModUtilitiesMod.LOGGER.info("SineStream started with context: " + getContextData());
    }
    @Override
    public void onStopStreamSendingOnServer() {
        // Gets called at the end of the Stream, if it is a "server->client" Stream
        ModUtilitiesMod.LOGGER.info("SineStream ended with context: " + getContextData());
    }


    // Gets called on every tick update from the server side
    @Override
    protected void updateOnServer() {
        double time = System.currentTimeMillis() / 1000.0 + getContextData();
        counter++;
        if(counter > 100) {
            counter = 0;
            stopStream(); // Stop the stream after 100 sent packets
        }
        if (Math.abs(time - lastTime) > 0.01) {
            sendPacket(); // Send the next Stream packet after 10ms
        }
        lastTime = time;
    }

    // Gets called when the "sendPacket()" is called and collects the data structure which will be sent to the client.
    @Override
    public Double provideStreamPacketOnServer()  {
        double time = System.currentTimeMillis() / 1000.0 + getContextData();
        lastTime = time;
        double value = Math.sin(time);
        ModUtilitiesMod.LOGGER.info("SineStream update: " + value);
        return value;
    }

    // Encoding functions to encode/decode the context data and the Streaming payload
    @Override
    public void encodeContextData(FriendlyByteBuf buffer, Float context) {
        buffer.writeFloat(context);
    }

    @Override
    public Float decodeContextData(FriendlyByteBuf buffer) {
        return buffer.readFloat();
    }

    @Override
    public void encodeData(FriendlyByteBuf buffer, Double aDouble) {
        buffer.writeDouble(aDouble);
    }

    @Override
    public Double decodeData(FriendlyByteBuf buffer) {
        return buffer.readDouble();
    }
}
``` 

