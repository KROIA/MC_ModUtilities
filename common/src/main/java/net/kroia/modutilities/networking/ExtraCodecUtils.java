package net.kroia.modutilities.networking;

import com.google.gson.JsonElement;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.kroia.modutilities.JsonUtilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExtraCodecUtils {
    /**
     * Builds a position-aware StreamCodec for arbitrary object arrays.
     * @param dataCodec Stream codec for the object type
     * @param instantiator Method that returns an array of the given size
     * @return The StreamCodec for the array data.
     * @param <T> The object type
     */
    public static <T> StreamCodec<RegistryFriendlyByteBuf, T[]> objectArrayStreamCodec(StreamCodec<RegistryFriendlyByteBuf, T> dataCodec, Function<Integer, T[]> instantiator){
        return StreamCodec.of(
                (buf, data) -> {
                    buf.writeVarInt(data.length);
                    int ct = 0;
                    for (T datum : data) {
                        ct += datum != null ? 1 : 0;
                    }
                    buf.writeVarInt(ct);
                    for (int i = 0; i < data.length; i++) {
                        if (data[i] != null) {
                            buf.writeVarInt(i);
                            dataCodec.encode(buf, data[i]);
                        }
                    }
                },
                (buf) -> {
                    T[] data = instantiator.apply(buf.readVarInt());
                    int ct = buf.readVarInt();
                    for (int i = 0; i < ct; i++){
                        data[buf.readVarInt()] = dataCodec.decode(buf);
                    }
                    return data;
                }
        );
    }


    public static <T> StreamCodec<RegistryFriendlyByteBuf, List<T>> listStreamCodec(StreamCodec<? super RegistryFriendlyByteBuf, T> datCodec){
        return ByteBufCodecs.collection(ArrayList::new, datCodec);
    }


    public static <T, S, M extends Map<T,S>> StreamCodec<RegistryFriendlyByteBuf, M>
    mapStreamCodec(StreamCodec<? super RegistryFriendlyByteBuf, T> keyCodec,
                   StreamCodec<? super RegistryFriendlyByteBuf, S> valueCodec, Supplier<M> mapSupplier){
        return StreamCodec.of(
                (buf, map) -> {
                    buf.writeVarInt(map.size());
                    map.forEach((key, value) -> {
                        keyCodec.encode(buf, key);
                        valueCodec.encode(buf, value);
                    });
                },
                (buf) -> {
                    int size = buf.readVarInt();
                    M map = mapSupplier.get();
                    for (int i = 0; i < size; i++) {
                        T key = keyCodec.decode(buf);
                        S value = valueCodec.decode(buf);
                        map.put(key, value);
                    }
                    return map;
                }
        );

    }

    public static <T, S extends Set<T>> StreamCodec<RegistryFriendlyByteBuf, S> setStreamCodec(StreamCodec<? super RegistryFriendlyByteBuf, T> keyCodec, Supplier<S> setSupplier){
        return StreamCodec.of(
                (buf, set) -> {
                    buf.writeVarInt(set.size());
                    set.forEach(key -> keyCodec.encode(buf, key));
                },
                (buf) -> {
                    int size = buf.readVarInt();
                    S set = setSupplier.get();
                    for (int i = 0; i < size; i++) {
                        T key = keyCodec.decode(buf);
                        set.add(key);
                    }

                    return set;
                }
        );
    }

    public static <E extends Enum<E>> StreamCodec<RegistryFriendlyByteBuf, E> enumStreamCodec(Class<E> enumClass){
        return StreamCodec.of((buf, enumValue) -> buf.writeVarInt(enumValue.ordinal()),
                (buf) -> enumClass.getEnumConstants()[buf.readVarInt()]
        );
    }


    public static final StreamCodec<RegistryFriendlyByteBuf, FriendlyByteBuf> FRIENDLY_BYTE_BUF_CODEC = StreamCodec.of(
            (buf, b) -> {
                byte[] bytes = new byte[b.readableBytes()];
                b.getBytes(b.readerIndex(), bytes);
                buf.writeByteArray(bytes);
            },
            (buf) -> new FriendlyByteBuf(Unpooled.wrappedBuffer(buf.readByteArray()))
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RegistryFriendlyByteBuf> REGISTRY_FRIENDLY_BYTE_BUF_CODEC = StreamCodec.of(
            (buf, b) -> {
                byte[] bytes = new byte[b.readableBytes()];
                b.getBytes(b.readerIndex(), bytes);
                buf.writeByteArray(bytes);
            },
            (buf) -> new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(buf.readByteArray()), buf.registryAccess())
    );


    /**
     * If a object can be null, use a modified version of the original codec
     * @param innerCodec
     * @return
     * @param <T>
     */
    public static <T, B extends ByteBuf> StreamCodec<B, T> nullable(StreamCodec<B, T> innerCodec) {
        return StreamCodec.of(
                (buf, value) -> {
                    buf.writeBoolean(value != null); // write a flag
                    if (value != null) {
                        innerCodec.encode(buf, value); // encode if not null
                    }
                },
                buf -> buf.readBoolean() ? innerCodec.decode(buf) : null // decode based on flag
        );
    }


    public static final StreamCodec<RegistryFriendlyByteBuf, JsonElement> JSON_ELEMENT_CODEC = StreamCodec.of(
            (buf, jsonElement) -> {
                // To String
                String jsonString = JsonUtilities.toString(jsonElement);
                ByteBufCodecs.STRING_UTF8.encode(buf, jsonString);
            },
            (buf) ->
            {
                String jsonString = ByteBufCodecs.STRING_UTF8.decode(buf);
                JsonElement jsonElement = JsonUtilities.fromString(jsonString);
                return jsonElement;
            }
    );


    public static StreamCodec<RegistryFriendlyByteBuf, float[]> FLOAT_ARRAY_CODEC = new StreamCodec<RegistryFriendlyByteBuf, float[]>() {
        @Override
        public float @NotNull [] decode(RegistryFriendlyByteBuf buf) {
            int tiles = buf.readInt();
            float[] volume = new float[tiles];
            for (int i = 0; i < tiles; i++) {
                volume[i] = buf.readFloat();
            }
            return volume;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, float[] data) {
            buf.writeInt(data.length);
            for (float v : data) {
                buf.writeFloat(v);
            }
        }
    };
}