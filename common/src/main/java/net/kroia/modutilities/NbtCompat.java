package net.kroia.modutilities;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Set;
import java.util.UUID;

/**
 * Version-agnostic NBT access. MC 1.21.5 changed most {@link CompoundTag}/{@link ListTag} getters
 * to return {@code Optional}, renamed the numeric tag accessors and removed the UUID helpers; these
 * methods behave the same on every supported version. A missing (or wrong-typed) key returns the
 * 1.21.1 defaults: 0 / false / "" / an empty compound, list or array — or the explicit default of the
 * overloads that take one. {@code CompoundTag.contains(String)} exists on all versions and needs no wrapper.
 */
public final class NbtCompat {
    private NbtCompat() {}

    // ---- explicit-default readers (version-agnostic) ----

    /** Byte value of {@code key}, or {@code def} if missing or not numeric. */
    public static byte getByte(CompoundTag tag, String key, byte def) {
        return tag.get(key) instanceof NumericTag n ? asByte(n) : def;
    }

    /** Short value of {@code key}, or {@code def} if missing or not numeric. */
    public static short getShort(CompoundTag tag, String key, short def) {
        return tag.get(key) instanceof NumericTag n ? asShort(n) : def;
    }

    /** Int value of {@code key}, or {@code def} if missing or not numeric. */
    public static int getInt(CompoundTag tag, String key, int def) {
        return tag.get(key) instanceof NumericTag n ? asInt(n) : def;
    }

    /** Long value of {@code key}, or {@code def} if missing or not numeric. */
    public static long getLong(CompoundTag tag, String key, long def) {
        return tag.get(key) instanceof NumericTag n ? asLong(n) : def;
    }

    /** Float value of {@code key}, or {@code def} if missing or not numeric. */
    public static float getFloat(CompoundTag tag, String key, float def) {
        return tag.get(key) instanceof NumericTag n ? asFloat(n) : def;
    }

    /** Double value of {@code key}, or {@code def} if missing or not numeric. */
    public static double getDouble(CompoundTag tag, String key, double def) {
        return tag.get(key) instanceof NumericTag n ? asDouble(n) : def;
    }

    /** Boolean value of {@code key} (numeric != 0), or {@code def} if missing or not numeric. */
    public static boolean getBoolean(CompoundTag tag, String key, boolean def) {
        return tag.get(key) instanceof NumericTag n ? asByte(n) != 0 : def;
    }

    /** String value of {@code key}, or {@code def} if missing or not a string. */
    public static String getString(CompoundTag tag, String key, String def) {
        return tag.get(key) instanceof StringTag s ? asString(s) : def;
    }

    // ---- arrays, list elements, UUIDs (version-agnostic) ----

    /** Byte array at {@code key}, or an empty array if missing or not a byte array. */
    public static byte[] getByteArray(CompoundTag tag, String key) {
        return tag.get(key) instanceof ByteArrayTag b ? b.getAsByteArray() : new byte[0];
    }

    /** Long array at {@code key}, or an empty array if missing or not a long array. */
    public static long[] getLongArray(CompoundTag tag, String key) {
        return tag.get(key) instanceof LongArrayTag t ? t.getAsLongArray() : new long[0];
    }

    /** String at {@code index}, or "" if out of range or not a string (MC 1.21.5 returns Optional). */
    public static String getString(ListTag list, int index) {
        return index >= 0 && index < list.size() && list.get(index) instanceof StringTag s ? asString(s) : "";
    }

    /** True if {@code key} holds a UUID in the 1.21.1 format (int array of length 4). MC 1.21.5 removed {@code hasUUID}. */
    public static boolean hasUUID(CompoundTag tag, String key) {
        return tag.get(key) instanceof IntArrayTag a && a.getAsIntArray().length == 4;
    }

    /**
     * UUID stored at {@code key} (1.21.1 int-array format), or {@code null} if missing or invalid.
     * Unlike 1.21.1's {@code CompoundTag.getUUID}, which threw on a missing key, this never throws.
     */
    public static UUID getUUID(CompoundTag tag, String key) {
        return hasUUID(tag, key) ? UUIDUtil.uuidFromIntArray(((IntArrayTag) tag.get(key)).getAsIntArray()) : null;
    }

    /** Stores {@code uuid} at {@code key} as a 4-int array, the on-disk format of 1.21.1's {@code putUUID}. */
    public static void putUUID(CompoundTag tag, String key, UUID uuid) {
        tag.putIntArray(key, UUIDUtil.uuidToIntArray(uuid));
    }

    // ---- readers with the 1.21.1 defaults ----

    /** Byte value of {@code key}, or 0 if missing (MC 1.21.5 Optional getters). */
    public static byte getByte(CompoundTag tag, String key) {
        return tag.getByte(key); // mc<=1.21.4
        //return tag.getByteOr(key, (byte) 0); // mc>=1.21.5
    }

    /** Short value of {@code key}, or 0 if missing (MC 1.21.5 Optional getters). */
    public static short getShort(CompoundTag tag, String key) {
        return tag.getShort(key); // mc<=1.21.4
        //return tag.getShortOr(key, (short) 0); // mc>=1.21.5
    }

    /** Int value of {@code key}, or 0 if missing (MC 1.21.5 Optional getters). */
    public static int getInt(CompoundTag tag, String key) {
        return tag.getInt(key); // mc<=1.21.4
        //return tag.getIntOr(key, 0); // mc>=1.21.5
    }

    /** Long value of {@code key}, or 0 if missing (MC 1.21.5 Optional getters). */
    public static long getLong(CompoundTag tag, String key) {
        return tag.getLong(key); // mc<=1.21.4
        //return tag.getLongOr(key, 0L); // mc>=1.21.5
    }

    /** Float value of {@code key}, or 0 if missing (MC 1.21.5 Optional getters). */
    public static float getFloat(CompoundTag tag, String key) {
        return tag.getFloat(key); // mc<=1.21.4
        //return tag.getFloatOr(key, 0.0f); // mc>=1.21.5
    }

    /** Double value of {@code key}, or 0 if missing (MC 1.21.5 Optional getters). */
    public static double getDouble(CompoundTag tag, String key) {
        return tag.getDouble(key); // mc<=1.21.4
        //return tag.getDoubleOr(key, 0.0); // mc>=1.21.5
    }

    /** Boolean value of {@code key}, or false if missing (MC 1.21.5 Optional getters). */
    public static boolean getBoolean(CompoundTag tag, String key) {
        return tag.getBoolean(key); // mc<=1.21.4
        //return tag.getBooleanOr(key, false); // mc>=1.21.5
    }

    /** String value of {@code key}, or "" if missing (MC 1.21.5 Optional getters). */
    public static String getString(CompoundTag tag, String key) {
        return tag.getString(key); // mc<=1.21.4
        //return tag.getStringOr(key, ""); // mc>=1.21.5
    }

    /** Compound at {@code key}, or a new empty compound if missing (MC 1.21.5 Optional getters). */
    public static CompoundTag getCompound(CompoundTag tag, String key) {
        return tag.getCompound(key); // mc<=1.21.4
        //return tag.getCompoundOrEmpty(key); // mc>=1.21.5
    }

    /** Int array at {@code key}, or an empty array if missing (MC 1.21.5 Optional getters). */
    public static int[] getIntArray(CompoundTag tag, String key) {
        return tag.getIntArray(key); // mc<=1.21.4
        //return tag.getIntArray(key).orElse(new int[0]); // mc>=1.21.5
    }

    /**
     * List at {@code key}, or an empty list if missing. Before MC 1.21.5 a list whose elements are not
     * of {@code elementType} also yields an empty list; 1.21.5 lists are untyped and the type is ignored.
     */
    public static ListTag getList(CompoundTag tag, String key, int elementType) {
        return tag.getList(key, elementType); // mc<=1.21.4
        //return tag.getListOrEmpty(key); // mc>=1.21.5
    }

    /** Compound at {@code index}, or a new empty compound if out of range or not a compound (MC 1.21.5 Optional getters). */
    public static CompoundTag getCompound(ListTag list, int index) {
        return list.getCompound(index); // mc<=1.21.4
        //return list.getCompoundOrEmpty(index); // mc>=1.21.5
    }

    /** All keys of the compound; MC 1.21.5 renamed {@code getAllKeys()} to {@code keySet()}. */
    public static Set<String> keys(CompoundTag tag) {
        return tag.getAllKeys(); // mc<=1.21.4
        //return tag.keySet(); // mc>=1.21.5
    }

    /** True if {@code key} exists with exactly the given tag type ID; MC 1.21.5 removed {@code contains(key, type)}. */
    public static boolean contains(CompoundTag tag, String key, int tagType) {
        Tag value = tag.get(key);
        return value != null && value.getId() == tagType;
    }

    /** Type ID of the list's elements, or 0 ({@code Tag.TAG_END}) if empty; MC 1.21.5 removed {@code ListTag.getElementType()}. */
    public static byte elementType(ListTag list) {
        return list.isEmpty() ? Tag.TAG_END : list.get(0).getId();
    }

    /** Numeric tag as byte; MC 1.21.5 renamed {@code getAsByte()} to {@code byteValue()}. */
    public static byte asByte(NumericTag tag) {
        return tag.getAsByte(); // mc<=1.21.4
        //return tag.byteValue(); // mc>=1.21.5
    }

    /** Numeric tag as short; MC 1.21.5 renamed {@code getAsShort()} to {@code shortValue()}. */
    public static short asShort(NumericTag tag) {
        return tag.getAsShort(); // mc<=1.21.4
        //return tag.shortValue(); // mc>=1.21.5
    }

    /** Numeric tag as int; MC 1.21.5 renamed {@code getAsInt()} to {@code intValue()}. */
    public static int asInt(NumericTag tag) {
        return tag.getAsInt(); // mc<=1.21.4
        //return tag.intValue(); // mc>=1.21.5
    }

    /** Numeric tag as long; MC 1.21.5 renamed {@code getAsLong()} to {@code longValue()}. */
    public static long asLong(NumericTag tag) {
        return tag.getAsLong(); // mc<=1.21.4
        //return tag.longValue(); // mc>=1.21.5
    }

    /** Numeric tag as float; MC 1.21.5 renamed {@code getAsFloat()} to {@code floatValue()}. */
    public static float asFloat(NumericTag tag) {
        return tag.getAsFloat(); // mc<=1.21.4
        //return tag.floatValue(); // mc>=1.21.5
    }

    /** Numeric tag as double; MC 1.21.5 renamed {@code getAsDouble()} to {@code doubleValue()}. */
    public static double asDouble(NumericTag tag) {
        return tag.getAsDouble(); // mc<=1.21.4
        //return tag.doubleValue(); // mc>=1.21.5
    }

    /** Numeric tag as boxed Number; MC 1.21.5 renamed {@code getAsNumber()} to {@code box()}. */
    public static Number asNumber(NumericTag tag) {
        return tag.getAsNumber(); // mc<=1.21.4
        //return tag.box(); // mc>=1.21.5
    }

    /**
     * String content of a StringTag, or the SNBT text of any other tag (pre-1.21.5 {@code getAsString()}
     * semantics); MC 1.21.5 replaced it with {@code StringTag.value()} / {@code Tag.asString()}.
     */
    public static String asString(Tag tag) {
        return tag.getAsString(); // mc<=1.21.4
        //return tag instanceof StringTag s ? s.value() : tag.toString(); // mc>=1.21.5
    }
}
