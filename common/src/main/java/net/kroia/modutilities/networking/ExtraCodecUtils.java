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

/**
 * Convenience factory methods and shared {@link StreamCodec} instances for
 * encoding/decoding common container and primitive types over the network.
 *
 * @apiNote
 * All codecs in this class operate on {@link RegistryFriendlyByteBuf} and may be used
 * on both the client and server side. Sparse codecs (e.g. {@link #objectArrayStreamCodec})
 * write only non-null entries to save bandwidth.
 */
public class ExtraCodecUtils {
    /**
     * Builds a position-aware StreamCodec for arbitrary object arrays.
     * Only non-null entries are written, keyed by their index, so sparse arrays
     * are encoded efficiently.
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
                    int len = buf.readVarInt();
                    if (len < 0) {
                        throw new io.netty.handler.codec.DecoderException("Negative array length: " + len);
                    }
                    T[] data = instantiator.apply(len);
                    int ct = buf.readVarInt();
                    if (ct < 0 || ct > len) {
                        throw new io.netty.handler.codec.DecoderException("Invalid element count " + ct + " for array of length " + len);
                    }
                    for (int i = 0; i < ct; i++){
                        int idx = buf.readVarInt();
                        if (idx < 0 || idx >= len) {
                            throw new io.netty.handler.codec.DecoderException("Array index out of bounds: " + idx + " (length " + len + ")");
                        }
                        data[idx] = dataCodec.decode(buf);
                    }
                    return data;
                }
        );
    }


    /**
     * Builds a {@link StreamCodec} for {@link List} of {@code T} backed by an {@link ArrayList}.
     * @param datCodec Stream codec for the element type.
     * @param <T> The element type of the list.
     * @return A codec that encodes/decodes lists of {@code T}.
     */
    public static <T> StreamCodec<RegistryFriendlyByteBuf, List<T>> listStreamCodec(StreamCodec<? super RegistryFriendlyByteBuf, T> datCodec){
        return ByteBufCodecs.collection(ArrayList::new, datCodec);
    }


    /**
     * Builds a {@link StreamCodec} for arbitrary {@link Map} types using the given key/value codecs.
     * @param keyCodec Codec used to encode/decode the keys.
     * @param valueCodec Codec used to encode/decode the values.
     * @param mapSupplier Supplier that creates a new empty map instance during decoding.
     * @param <T> The key type.
     * @param <S> The value type.
     * @param <M> The concrete map type.
     * @return A codec that encodes/decodes maps from {@code T} to {@code S}.
     */
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
                    if(size < 0) {
                        throw new io.netty.handler.codec.DecoderException("Negative map size: " + size);
                    }
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

    /**
     * Builds a {@link StreamCodec} for arbitrary {@link Set} types using the given element codec.
     * @param keyCodec Codec used to encode/decode the elements.
     * @param setSupplier Supplier that creates a new empty set instance during decoding.
     * @param <T> The element type.
     * @param <S> The concrete set type.
     * @return A codec that encodes/decodes sets of {@code T}.
     */
    public static <T, S extends Set<T>> StreamCodec<RegistryFriendlyByteBuf, S> setStreamCodec(StreamCodec<? super RegistryFriendlyByteBuf, T> keyCodec, Supplier<S> setSupplier){
        return StreamCodec.of(
                (buf, set) -> {
                    buf.writeVarInt(set.size());
                    set.forEach(key -> keyCodec.encode(buf, key));
                },
                (buf) -> {
                    int size = buf.readVarInt();
                    if(size < 0) {
                        throw new io.netty.handler.codec.DecoderException("Negative set size: " + size);
                    }
                    S set = setSupplier.get();
                    for (int i = 0; i < size; i++) {
                        T key = keyCodec.decode(buf);
                        set.add(key);
                    }

                    return set;
                }
        );
    }

    /**
     * Builds a {@link StreamCodec} for an enum type by encoding its ordinal as a varint.
     *
     * @apiNote Reordering or removing constants on either side breaks compatibility because
     * decoding relies on the ordinal index.
     *
     * @param enumClass The enum class to encode.
     * @param <E> The enum type.
     * @return A codec that encodes/decodes enum constants of {@code E}.
     */
    public static <E extends Enum<E>> StreamCodec<RegistryFriendlyByteBuf, E> enumStreamCodec(Class<E> enumClass){
        return StreamCodec.of((buf, enumValue) -> buf.writeVarInt(enumValue.ordinal()),
                (buf) -> {
                    int ordinal = buf.readVarInt();
                    E[] constants = enumClass.getEnumConstants();
                    if (ordinal < 0 || ordinal >= constants.length) {
                        throw new io.netty.handler.codec.DecoderException("Enum ordinal out of bounds: " + ordinal + " for " + enumClass.getSimpleName() + " (size " + constants.length + ")");
                    }
                    return constants[ordinal];
                }
        );
    }


    /**
     * Codec that encodes a {@link FriendlyByteBuf} payload as a length-prefixed byte array.
     * Used to embed an opaque pre-serialized buffer inside another packet.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, FriendlyByteBuf> FRIENDLY_BYTE_BUF_CODEC = StreamCodec.of(
            (buf, b) -> {
                byte[] bytes = new byte[b.readableBytes()];
                b.getBytes(b.readerIndex(), bytes);
                buf.writeByteArray(bytes);
            },
            (buf) -> new FriendlyByteBuf(Unpooled.wrappedBuffer(buf.readByteArray()))
    );
    /**
     * Codec that encodes a {@link RegistryFriendlyByteBuf} payload as a length-prefixed byte array,
     * preserving the surrounding registry access for the decoded buffer. Used by ARRS and the
     * streaming system to pass opaque buffers between layers.
     */
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