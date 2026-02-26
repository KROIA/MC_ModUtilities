package net.kroia.modutilities.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

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
                buf.writeByteArray(b.array());
            },
            (buf) -> new FriendlyByteBuf(Unpooled.wrappedBuffer(buf.readByteArray()))
    );
}