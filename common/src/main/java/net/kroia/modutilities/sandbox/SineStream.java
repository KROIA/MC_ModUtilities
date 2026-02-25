package net.kroia.modutilities.sandbox;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.streaming.GenericStream;
import net.minecraft.network.FriendlyByteBuf;
public class SineStream extends GenericStream<Float, Double> {

    private double lastTime = 0.0;
    private int counter = 0;

    @Override
    public SineStream copy() {
        return new SineStream();
    }
    @Override
    public String getStreamTypeID() {
        return SineStream.class.getSimpleName();
    }

    @Override
    public void onStartStreamSendingOnSever() {
        ModUtilitiesMod.LOGGER.info("SineStream started with context: " + getContextData());
    }
    @Override
    public void onStopStreamSendingOnServer() {
        ModUtilitiesMod.LOGGER.info("SineStream ended with context: " + getContextData());
    }

    @Override
    protected void updateOnServer() {
        double time = System.currentTimeMillis() / 1000.0 + getContextData();
        counter++;
        if(counter > 100) {
            counter = 0;
            stopStream();
        }
        if (Math.abs(time - lastTime) > 0.01) {
            sendPacket();
        }
        lastTime = time;
    }

    @Override
    public Double provideStreamPacketOnServer()  {
        double time = System.currentTimeMillis() / 1000.0 + getContextData();
        lastTime = time;
        double value = Math.sin(time);
        ModUtilitiesMod.LOGGER.info("SineStream update: " + value);
        return value;
    }

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
