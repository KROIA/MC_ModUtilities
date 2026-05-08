package net.kroia.modutilities.testing.tests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kroia.modutilities.setting.parser.ItemStackJsonParser;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.minecraft.nbt.*;

/**
 * Regression tests for {@link ItemStackJsonParser} NBT-JSON conversion.
 * Covers Issue #49: numeric type precision loss during NBT round-trips.
 */
public class ParserTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.PERSISTENCE;
    }

    @Override
    public void registerTests() {
        // Round-trip tests: each NBT numeric type must survive nbt -> json -> nbt
        addTest("itemstack_parser_byte_roundtrip", this::testByteRoundtrip);
        addTest("itemstack_parser_short_roundtrip", this::testShortRoundtrip);
        addTest("itemstack_parser_int_roundtrip", this::testIntRoundtrip);
        addTest("itemstack_parser_long_roundtrip", this::testLongRoundtrip);
        addTest("itemstack_parser_float_roundtrip", this::testFloatRoundtrip);
        addTest("itemstack_parser_double_roundtrip", this::testDoubleRoundtrip);

        // Combined round-trip: all numeric types in a single CompoundTag
        addTest("itemstack_parser_all_numeric_roundtrip", this::testAllNumericTypesRoundtrip);

        // Backwards compatibility: old-format plain JSON numbers must still deserialize
        addTest("itemstack_parser_legacy_plain_int_compat", this::testLegacyPlainIntCompat);
        addTest("itemstack_parser_legacy_plain_double_compat", this::testLegacyPlainDoubleCompat);
        addTest("itemstack_parser_legacy_plain_fractional_compat", this::testLegacyPlainFractionalCompat);

        // Edge cases
        addTest("itemstack_parser_string_not_affected", this::testStringNotAffected);
        addTest("itemstack_parser_nested_compound_roundtrip", this::testNestedCompoundRoundtrip);
        addTest("itemstack_parser_list_with_typed_numbers", this::testListWithTypedNumbers);
        addTest("itemstack_parser_byte_zero_roundtrip", this::testByteZeroRoundtrip);
        addTest("itemstack_parser_negative_values_roundtrip", this::testNegativeValuesRoundtrip);
        addTest("itemstack_parser_boundary_values_roundtrip", this::testBoundaryValuesRoundtrip);
    }

    // ========================================================================
    // Round-trip tests: NBT -> JSON -> NBT preserves exact type
    // ========================================================================

    private TestResult testByteRoundtrip() {
        ItemStackJsonParser parser = new ItemStackJsonParser();
        CompoundTag original = new CompoundTag();
        original.putByte("val", (byte) 5);

        JsonObject json = parser.nbtToJson(original);
        CompoundTag restored = parser.jsonToNbt(json);

        Tag tag = restored.get("val");
        if (!(tag instanceof ByteTag)) {
            return fail("Issue #49: byte should round-trip as ByteTag, got "
                    + (tag != null ? tag.getClass().getSimpleName() : "null"));
        }
        return assertEquals("Byte value should be 5", (byte) 5, ((ByteTag) tag).getAsByte());
    }

    private TestResult testShortRoundtrip() {
        ItemStackJsonParser parser = new ItemStackJsonParser();
        CompoundTag original = new CompoundTag();
        original.putShort("val", (short) 300);

        JsonObject json = parser.nbtToJson(original);
        CompoundTag restored = parser.jsonToNbt(json);

        Tag tag = restored.get("val");
        if (!(tag instanceof ShortTag)) {
            return fail("Issue #49: short should round-trip as ShortTag, got "
                    + (tag != null ? tag.getClass().getSimpleName() : "null"));
        }
        return assertEquals("Short value should be 300", (short) 300, ((ShortTag) tag).getAsShort());
    }

    private TestResult testIntRoundtrip() {
        ItemStackJsonParser parser = new ItemStackJsonParser();
        CompoundTag original = new CompoundTag();
        original.putInt("val", 100000);

        JsonObject json = parser.nbtToJson(original);
        CompoundTag restored = parser.jsonToNbt(json);

        Tag tag = restored.get("val");
        if (!(tag instanceof IntTag)) {
            return fail("Issue #49: int should round-trip as IntTag, got "
                    + (tag != null ? tag.getClass().getSimpleName() : "null"));
        }
        return assertEquals("Int value should be 100000", 100000, ((IntTag) tag).getAsInt());
    }

    private TestResult testLongRoundtrip() {
        ItemStackJsonParser parser = new ItemStackJsonParser();
        CompoundTag original = new CompoundTag();
        original.putLong("val", 5000000000L);

        JsonObject json = parser.nbtToJson(original);
        CompoundTag restored = parser.jsonToNbt(json);

        Tag tag = restored.get("val");
        if (!(tag instanceof LongTag)) {
            return fail("Issue #49: long should round-trip as LongTag, got "
                    + (tag != null ? tag.getClass().getSimpleName() : "null"));
        }
        return assertEquals("Long value should be 5000000000", 5000000000L, ((LongTag) tag).getAsLong());
    }

    private TestResult testFloatRoundtrip() {
        ItemStackJsonParser parser = new ItemStackJsonParser();
        CompoundTag original = new CompoundTag();
        original.putFloat("val", 1.5f);

        JsonObject json = parser.nbtToJson(original);
        CompoundTag restored = parser.jsonToNbt(json);

        Tag tag = restored.get("val");
        if (!(tag instanceof FloatTag)) {
            return fail("Issue #49: float should round-trip as FloatTag, got "
                    + (tag != null ? tag.getClass().getSimpleName() : "null"));
        }
        return assertTrue("Float value should be approximately 1.5",
                Math.abs(((FloatTag) tag).getAsFloat() - 1.5f) < 0.0001f);
    }

    private TestResult testDoubleRoundtrip() {
        ItemStackJsonParser parser = new ItemStackJsonParser();
        CompoundTag original = new CompoundTag();
        original.putDouble("val", 3.141592653589793);

        JsonObject json = parser.nbtToJson(original);
        CompoundTag restored = parser.jsonToNbt(json);

        Tag tag = restored.get("val");
        if (!(tag instanceof DoubleTag)) {
            return fail("Issue #49: double should round-trip as DoubleTag, got "
                    + (tag != null ? tag.getClass().getSimpleName() : "null"));
        }
        return assertTrue("Double value should be approximately pi",
                Math.abs(((DoubleTag) tag).getAsDouble() - 3.141592653589793) < 0.000000001);
    }

    // ========================================================================
    // Combined round-trip
    // ========================================================================

    private TestResult testAllNumericTypesRoundtrip() {
        ItemStackJsonParser parser = new ItemStackJsonParser();
        CompoundTag original = new CompoundTag();
        original.putByte("b", (byte) 3);
        original.putShort("s", (short) 1000);
        original.putInt("i", 50000);
        original.putLong("l", 9999999999L);
        original.putFloat("f", 2.718f);
        original.putDouble("d", 1.41421356);

        JsonObject json = parser.nbtToJson(original);
        CompoundTag restored = parser.jsonToNbt(json);

        Tag bTag = restored.get("b");
        if (!(bTag instanceof ByteTag)) {
            return fail("byte should be ByteTag, got " + (bTag != null ? bTag.getClass().getSimpleName() : "null"));
        }
        Tag sTag = restored.get("s");
        if (!(sTag instanceof ShortTag)) {
            return fail("short should be ShortTag, got " + (sTag != null ? sTag.getClass().getSimpleName() : "null"));
        }
        Tag iTag = restored.get("i");
        if (!(iTag instanceof IntTag)) {
            return fail("int should be IntTag, got " + (iTag != null ? iTag.getClass().getSimpleName() : "null"));
        }
        Tag lTag = restored.get("l");
        if (!(lTag instanceof LongTag)) {
            return fail("long should be LongTag, got " + (lTag != null ? lTag.getClass().getSimpleName() : "null"));
        }
        Tag fTag = restored.get("f");
        if (!(fTag instanceof FloatTag)) {
            return fail("float should be FloatTag, got " + (fTag != null ? fTag.getClass().getSimpleName() : "null"));
        }
        Tag dTag = restored.get("d");
        if (!(dTag instanceof DoubleTag)) {
            return fail("double should be DoubleTag, got " + (dTag != null ? dTag.getClass().getSimpleName() : "null"));
        }

        if (((ByteTag) bTag).getAsByte() != 3) return fail("byte value mismatch");
        if (((ShortTag) sTag).getAsShort() != 1000) return fail("short value mismatch");
        if (((IntTag) iTag).getAsInt() != 50000) return fail("int value mismatch");
        if (((LongTag) lTag).getAsLong() != 9999999999L) return fail("long value mismatch");

        return pass("All six numeric types preserved through round-trip");
    }

    // ========================================================================
    // Backwards compatibility: old-format plain JSON numbers
    // ========================================================================

    private TestResult testLegacyPlainIntCompat() {
        // Simulate old format: plain JSON number for an integer value
        // Old format stored all numbers as doubles, so 42.0 in JSON
        // The legacy heuristic should treat whole numbers as IntTag
        ItemStackJsonParser parser = new ItemStackJsonParser();
        JsonObject legacyJson = new JsonObject();
        legacyJson.addProperty("level", 42);  // plain JSON number

        CompoundTag restored = parser.jsonToNbt(legacyJson);
        Tag tag = restored.get("level");

        if (!(tag instanceof IntTag)) {
            return fail("Legacy plain integer should deserialize as IntTag (existing behavior), got "
                    + (tag != null ? tag.getClass().getSimpleName() : "null"));
        }
        return assertEquals("Legacy int value should be 42", 42, ((IntTag) tag).getAsInt());
    }

    private TestResult testLegacyPlainDoubleCompat() {
        // Simulate old format: a large double value that doesn't fit the int heuristic
        ItemStackJsonParser parser = new ItemStackJsonParser();
        JsonObject legacyJson = new JsonObject();
        legacyJson.addProperty("bigval", 5000000000.0);  // plain JSON number, too large for int

        CompoundTag restored = parser.jsonToNbt(legacyJson);
        Tag tag = restored.get("bigval");

        if (!(tag instanceof DoubleTag)) {
            return fail("Legacy large number should deserialize as DoubleTag, got "
                    + (tag != null ? tag.getClass().getSimpleName() : "null"));
        }
        return assertTrue("Legacy double value should be approximately 5000000000",
                Math.abs(((DoubleTag) tag).getAsDouble() - 5000000000.0) < 1.0);
    }

    private TestResult testLegacyPlainFractionalCompat() {
        // Simulate old format: a fractional double
        ItemStackJsonParser parser = new ItemStackJsonParser();
        JsonObject legacyJson = new JsonObject();
        legacyJson.addProperty("ratio", 3.14);  // plain JSON number, fractional

        CompoundTag restored = parser.jsonToNbt(legacyJson);
        Tag tag = restored.get("ratio");

        if (!(tag instanceof DoubleTag)) {
            return fail("Legacy fractional number should deserialize as DoubleTag, got "
                    + (tag != null ? tag.getClass().getSimpleName() : "null"));
        }
        return assertTrue("Legacy double value should be approximately 3.14",
                Math.abs(((DoubleTag) tag).getAsDouble() - 3.14) < 0.001);
    }

    // ========================================================================
    // Edge cases
    // ========================================================================

    private TestResult testStringNotAffected() {
        ItemStackJsonParser parser = new ItemStackJsonParser();
        CompoundTag original = new CompoundTag();
        original.putString("name", "diamond_sword");

        JsonObject json = parser.nbtToJson(original);
        CompoundTag restored = parser.jsonToNbt(json);

        Tag tag = restored.get("name");
        if (!(tag instanceof StringTag)) {
            return fail("String should remain StringTag, got "
                    + (tag != null ? tag.getClass().getSimpleName() : "null"));
        }
        return assertEquals("String value should be preserved", "diamond_sword", ((StringTag) tag).getAsString());
    }

    private TestResult testNestedCompoundRoundtrip() {
        ItemStackJsonParser parser = new ItemStackJsonParser();

        CompoundTag inner = new CompoundTag();
        inner.putByte("lvl", (byte) 3);
        inner.putShort("id_num", (short) 16);

        CompoundTag outer = new CompoundTag();
        outer.putString("enchantment", "sharpness");
        outer.put("data", inner);

        JsonObject json = parser.nbtToJson(outer);
        CompoundTag restored = parser.jsonToNbt(json);

        // Check outer string
        if (!"sharpness".equals(restored.getString("enchantment"))) {
            return fail("Outer string lost");
        }

        // Check inner compound
        Tag dataTag = restored.get("data");
        if (!(dataTag instanceof CompoundTag innerRestored)) {
            return fail("Inner compound should be CompoundTag, got "
                    + (dataTag != null ? dataTag.getClass().getSimpleName() : "null"));
        }

        Tag lvlTag = innerRestored.get("lvl");
        if (!(lvlTag instanceof ByteTag)) {
            return fail("Nested byte should be ByteTag, got "
                    + (lvlTag != null ? lvlTag.getClass().getSimpleName() : "null"));
        }

        Tag idTag = innerRestored.get("id_num");
        if (!(idTag instanceof ShortTag)) {
            return fail("Nested short should be ShortTag, got "
                    + (idTag != null ? idTag.getClass().getSimpleName() : "null"));
        }

        return pass("Nested compound with typed numbers survives round-trip");
    }

    private TestResult testListWithTypedNumbers() {
        ItemStackJsonParser parser = new ItemStackJsonParser();

        ListTag list = new ListTag();
        list.add(ByteTag.valueOf((byte) 1));
        list.add(ByteTag.valueOf((byte) 2));
        list.add(ByteTag.valueOf((byte) 3));

        CompoundTag root = new CompoundTag();
        root.put("bytes", list);

        JsonObject json = parser.nbtToJson(root);
        CompoundTag restored = parser.jsonToNbt(json);

        Tag listTag = restored.get("bytes");
        if (!(listTag instanceof ListTag restoredList)) {
            return fail("Should be ListTag, got "
                    + (listTag != null ? listTag.getClass().getSimpleName() : "null"));
        }
        if (restoredList.size() != 3) {
            return fail("List should have 3 elements, got " + restoredList.size());
        }

        for (int i = 0; i < 3; i++) {
            Tag element = restoredList.get(i);
            if (!(element instanceof ByteTag)) {
                return fail("List element " + i + " should be ByteTag, got " + element.getClass().getSimpleName());
            }
        }

        return pass("List of ByteTags survives round-trip");
    }

    private TestResult testByteZeroRoundtrip() {
        ItemStackJsonParser parser = new ItemStackJsonParser();
        CompoundTag original = new CompoundTag();
        original.putByte("zero", (byte) 0);

        JsonObject json = parser.nbtToJson(original);
        CompoundTag restored = parser.jsonToNbt(json);

        Tag tag = restored.get("zero");
        if (!(tag instanceof ByteTag)) {
            return fail("Byte zero should round-trip as ByteTag, got "
                    + (tag != null ? tag.getClass().getSimpleName() : "null"));
        }
        return assertEquals("Byte value should be 0", (byte) 0, ((ByteTag) tag).getAsByte());
    }

    private TestResult testNegativeValuesRoundtrip() {
        ItemStackJsonParser parser = new ItemStackJsonParser();
        CompoundTag original = new CompoundTag();
        original.putByte("nb", (byte) -1);
        original.putShort("ns", (short) -500);
        original.putInt("ni", -100000);
        original.putLong("nl", -9999999999L);
        original.putFloat("nf", -2.5f);
        original.putDouble("nd", -1.23456789);

        JsonObject json = parser.nbtToJson(original);
        CompoundTag restored = parser.jsonToNbt(json);

        if (!(restored.get("nb") instanceof ByteTag bt) || bt.getAsByte() != -1)
            return fail("Negative byte round-trip failed");
        if (!(restored.get("ns") instanceof ShortTag st) || st.getAsShort() != -500)
            return fail("Negative short round-trip failed");
        if (!(restored.get("ni") instanceof IntTag it) || it.getAsInt() != -100000)
            return fail("Negative int round-trip failed");
        if (!(restored.get("nl") instanceof LongTag lt) || lt.getAsLong() != -9999999999L)
            return fail("Negative long round-trip failed");
        if (!(restored.get("nf") instanceof FloatTag ft) || Math.abs(ft.getAsFloat() - (-2.5f)) > 0.0001f)
            return fail("Negative float round-trip failed");
        if (!(restored.get("nd") instanceof DoubleTag dt) || Math.abs(dt.getAsDouble() - (-1.23456789)) > 0.000001)
            return fail("Negative double round-trip failed");

        return pass("All negative numeric values survive round-trip");
    }

    private TestResult testBoundaryValuesRoundtrip() {
        ItemStackJsonParser parser = new ItemStackJsonParser();
        CompoundTag original = new CompoundTag();
        original.putByte("bmax", Byte.MAX_VALUE);
        original.putByte("bmin", Byte.MIN_VALUE);
        original.putShort("smax", Short.MAX_VALUE);
        original.putShort("smin", Short.MIN_VALUE);
        original.putInt("imax", Integer.MAX_VALUE);
        original.putInt("imin", Integer.MIN_VALUE);
        original.putLong("lmax", Long.MAX_VALUE);
        original.putLong("lmin", Long.MIN_VALUE);

        JsonObject json = parser.nbtToJson(original);
        CompoundTag restored = parser.jsonToNbt(json);

        if (!(restored.get("bmax") instanceof ByteTag bt1) || bt1.getAsByte() != Byte.MAX_VALUE)
            return fail("Byte MAX_VALUE round-trip failed");
        if (!(restored.get("bmin") instanceof ByteTag bt2) || bt2.getAsByte() != Byte.MIN_VALUE)
            return fail("Byte MIN_VALUE round-trip failed");
        if (!(restored.get("smax") instanceof ShortTag st1) || st1.getAsShort() != Short.MAX_VALUE)
            return fail("Short MAX_VALUE round-trip failed");
        if (!(restored.get("smin") instanceof ShortTag st2) || st2.getAsShort() != Short.MIN_VALUE)
            return fail("Short MIN_VALUE round-trip failed");
        if (!(restored.get("imax") instanceof IntTag it1) || it1.getAsInt() != Integer.MAX_VALUE)
            return fail("Int MAX_VALUE round-trip failed");
        if (!(restored.get("imin") instanceof IntTag it2) || it2.getAsInt() != Integer.MIN_VALUE)
            return fail("Int MIN_VALUE round-trip failed");
        if (!(restored.get("lmax") instanceof LongTag lt1) || lt1.getAsLong() != Long.MAX_VALUE)
            return fail("Long MAX_VALUE round-trip failed");
        if (!(restored.get("lmin") instanceof LongTag lt2) || lt2.getAsLong() != Long.MIN_VALUE)
            return fail("Long MIN_VALUE round-trip failed");

        return pass("All boundary values survive round-trip");
    }
}
