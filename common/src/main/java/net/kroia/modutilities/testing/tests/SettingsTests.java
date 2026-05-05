package net.kroia.modutilities.testing.tests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kroia.modutilities.setting.Setting;
import net.kroia.modutilities.setting.SettingsGroup;
import net.kroia.modutilities.setting.SettingsStore;
import net.kroia.modutilities.setting.parser.NBTJsonParser;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.minecraft.nbt.*;

import java.util.ArrayList;
import java.util.List;

public class SettingsTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.SETTINGS;
    }

    @Override
    public void registerTests() {
        // Setting tests
        addTest("setting_set_get", this::testSettingSetGet);
        addTest("setting_listener_fires", this::testSettingListenerFires);
        addTest("setting_default_value_reset", this::testSettingDefaultValueReset);
        addTest("setting_same_value_no_notify", this::testSettingSameValueNoNotify);
        addTest("setting_null_value", this::testSettingNullValue);
        addTest("setting_remove_listener", this::testSettingRemoveListener);

        // SettingsGroup tests
        addTest("group_register_and_retrieve", this::testGroupRegisterAndRetrieve);
        addTest("group_get_all_settings", this::testGroupGetAllSettings);
        addTest("group_get_nonexistent", this::testGroupGetNonexistent);
        addTest("group_reset_to_defaults", this::testGroupResetToDefaults);

        // SettingsStore tests
        addTest("store_json_roundtrip", this::testStoreJsonRoundtrip);
        addTest("store_multi_group_roundtrip", this::testStoreMultiGroupRoundtrip);
        addTest("store_numeric_type_preservation", this::testStoreNumericTypePreservation);
        addTest("store_string_preservation", this::testStoreStringPreservation);
        addTest("store_boolean_preservation", this::testStoreBooleanPreservation);

        // NBTJsonParser tests
        addTest("nbt_compound_roundtrip", this::testNbtCompoundRoundtrip);
        addTest("nbt_numeric_type_inference", this::testNbtNumericTypeInference);
        addTest("nbt_boolean_handling", this::testNbtBooleanHandling);
        addTest("nbt_nested_tags", this::testNbtNestedTags);
        addTest("nbt_list_tags", this::testNbtListTags);
        addTest("nbt_string_tag", this::testNbtStringTag);
    }

    // ========================================================================
    // Helper: testable SettingsGroup subclass (registerSetting is protected)
    // ========================================================================

    private static class TestGroup extends SettingsGroup {
        public TestGroup(String name) {
            super(name);
        }

        public <T> Setting<T> addSetting(String settingName, T defaultValue, java.lang.reflect.Type type) {
            return registerSetting(settingName, defaultValue, type);
        }
    }

    // ========================================================================
    // Setting tests
    // ========================================================================

    private TestResult testSettingSetGet() {
        Setting<Integer> setting = new Setting<>("volume", 50, Integer.class);
        setting.set(75);
        return assertEquals("Setting value should be 75 after set", 75, setting.get());
    }

    private TestResult testSettingListenerFires() {
        Setting<String> setting = new Setting<>("name", "default", String.class);
        String[] received = {null};
        setting.addListener(val -> received[0] = val);
        setting.set("updated");
        return assertEquals("Listener should receive the new value", "updated", received[0]);
    }

    private TestResult testSettingDefaultValueReset() {
        Setting<Integer> setting = new Setting<>("brightness", 100, Integer.class);
        setting.set(42);
        setting.setToDefaultValue();
        return assertEquals("Setting should reset to default value", 100, setting.get());
    }

    private TestResult testSettingSameValueNoNotify() {
        Setting<Integer> setting = new Setting<>("level", 10, Integer.class);
        setting.set(10); // same as initial
        int[] callCount = {0};
        setting.addListener(val -> callCount[0]++);
        setting.set(10); // same value again
        return assertEquals("Listener should not fire when value unchanged", 0, callCount[0]);
    }

    private TestResult testSettingNullValue() {
        Setting<String> setting = new Setting<>("optional", "hello", String.class);
        setting.set(null);
        return assertNull("Setting should accept null value", setting.get());
    }

    private TestResult testSettingRemoveListener() {
        Setting<Integer> setting = new Setting<>("count", 0, Integer.class);
        int[] callCount = {0};
        java.util.function.Consumer<Integer> listener = val -> callCount[0]++;
        setting.addListener(listener);
        setting.removeListener(listener);
        setting.set(5);
        return assertEquals("Removed listener should not fire", 0, callCount[0]);
    }

    // ========================================================================
    // SettingsGroup tests
    // ========================================================================

    private TestResult testGroupRegisterAndRetrieve() {
        TestGroup group = new TestGroup("TestGroup");
        Setting<Integer> setting = group.addSetting("volume", 50, Integer.class);
        setting.set(80);
        Setting<?> retrieved = group.getSetting("volume");
        if (retrieved == null) {
            return fail("getSetting returned null for registered setting");
        }
        return assertEquals("Retrieved setting should have the updated value", 80, retrieved.get());
    }

    private TestResult testGroupGetAllSettings() {
        TestGroup group = new TestGroup("TestGroup");
        group.addSetting("a", 1, Integer.class);
        group.addSetting("b", 2, Integer.class);
        group.addSetting("c", 3, Integer.class);
        return assertEquals("Group should contain 3 settings", 3, group.getAllSettings().size());
    }

    private TestResult testGroupGetNonexistent() {
        TestGroup group = new TestGroup("TestGroup");
        group.addSetting("exists", true, Boolean.class);
        Setting<?> result = group.getSetting("does_not_exist");
        return assertNull("Non-existent setting should return null", result);
    }

    private TestResult testGroupResetToDefaults() {
        TestGroup group = new TestGroup("TestGroup");
        Setting<Integer> s1 = group.addSetting("x", 10, Integer.class);
        Setting<String> s2 = group.addSetting("y", "default", String.class);
        s1.set(999);
        s2.set("changed");
        group.setToDefaultValue();
        if (!Integer.valueOf(10).equals(s1.get())) {
            return fail("Integer setting should reset to default 10, got " + s1.get());
        }
        return assertEquals("String setting should reset to default", "default", s2.get());
    }

    // ========================================================================
    // SettingsStore tests
    // ========================================================================

    private TestResult testStoreJsonRoundtrip() {
        TestGroup group = new TestGroup("MyGroup");
        Setting<Integer> intSetting = group.addSetting("intVal", 42, Integer.class);
        Setting<String> strSetting = group.addSetting("strVal", "hello", String.class);

        SettingsStore store = new SettingsStore();
        JsonElement json = store.toJson(group);

        // Change values before loading
        intSetting.set(0);
        strSetting.set("changed");

        store.fromJson(group, json);

        if (!Integer.valueOf(42).equals(intSetting.get())) {
            return fail("Integer setting should be restored to 42, got " + intSetting.get());
        }
        return assertEquals("String setting should be restored", "hello", strSetting.get());
    }

    private TestResult testStoreMultiGroupRoundtrip() {
        TestGroup group1 = new TestGroup("Group1");
        Setting<Integer> s1 = group1.addSetting("val", 10, Integer.class);

        TestGroup group2 = new TestGroup("Group2");
        Setting<String> s2 = group2.addSetting("name", "test", String.class);

        List<SettingsGroup> groups = new ArrayList<>();
        groups.add(group1);
        groups.add(group2);

        SettingsStore store = new SettingsStore();
        JsonElement json = store.toJson(groups);

        // Change values
        s1.set(999);
        s2.set("modified");

        store.fromJson(groups, json);

        if (!Integer.valueOf(10).equals(s1.get())) {
            return fail("Group1 integer setting should be restored to 10, got " + s1.get());
        }
        return assertEquals("Group2 string setting should be restored", "test", s2.get());
    }

    private TestResult testStoreNumericTypePreservation() {
        // Issue #24: Gson deserializes integers as doubles.
        // This test verifies that integer values survive a JSON roundtrip through SettingsStore.
        TestGroup group = new TestGroup("NumericGroup");
        Setting<Integer> intSetting = group.addSetting("count", 7, Integer.class);
        Setting<Double> dblSetting = group.addSetting("ratio", 3.14, Double.class);

        SettingsStore store = new SettingsStore();
        JsonElement json = store.toJson(group);

        // Reset and reload
        intSetting.set(0);
        dblSetting.set(0.0);

        store.fromJson(group, json);

        // The critical check: Integer must come back as exactly 7, not 7.0
        Object intVal = intSetting.get();
        if (!(intVal instanceof Integer)) {
            return fail("Integer setting should remain Integer type after roundtrip, got " + intVal.getClass().getSimpleName());
        }
        if (!Integer.valueOf(7).equals(intVal)) {
            return fail("Integer setting should be 7 after roundtrip, got " + intVal);
        }
        return assertEquals("Double setting should be preserved", 3.14, dblSetting.get());
    }

    private TestResult testStoreStringPreservation() {
        TestGroup group = new TestGroup("StringGroup");
        Setting<String> setting = group.addSetting("msg", "hello world", String.class);

        SettingsStore store = new SettingsStore();
        JsonElement json = store.toJson(group);

        setting.set("overwritten");
        store.fromJson(group, json);

        return assertEquals("String should survive roundtrip", "hello world", setting.get());
    }

    private TestResult testStoreBooleanPreservation() {
        TestGroup group = new TestGroup("BoolGroup");
        Setting<Boolean> setting = group.addSetting("enabled", true, Boolean.class);

        SettingsStore store = new SettingsStore();
        JsonElement json = store.toJson(group);

        setting.set(false);
        store.fromJson(group, json);

        return assertTrue("Boolean should be true after roundtrip", setting.get());
    }

    // ========================================================================
    // NBTJsonParser tests
    // ========================================================================

    private TestResult testNbtCompoundRoundtrip() {
        NBTJsonParser parser = new NBTJsonParser();

        CompoundTag original = new CompoundTag();
        original.putInt("health", 20);
        original.putString("name", "Steve");
        original.putDouble("speed", 0.7);

        JsonElement json = parser.toJson(original);
        Tag restored = parser.fromJson(json);

        if (!(restored instanceof CompoundTag compound)) {
            return fail("Restored tag should be CompoundTag, got " + restored.getClass().getSimpleName());
        }
        if (compound.getInt("health") != 20) {
            return fail("health should be 20, got " + compound.getInt("health"));
        }
        if (!compound.getString("name").equals("Steve")) {
            return fail("name should be Steve, got " + compound.getString("name"));
        }
        return assertTrue("speed should be approximately 0.7",
                Math.abs(compound.getDouble("speed") - 0.7) < 0.0001);
    }

    private TestResult testNbtNumericTypeInference() {
        NBTJsonParser parser = new NBTJsonParser();

        CompoundTag original = new CompoundTag();
        original.putByte("small", (byte) 5);
        original.putShort("medium", (short) 300);
        original.putInt("large", 100000);
        original.putLong("huge", 5000000000L);
        original.putFloat("fracF", 1.5f);
        original.putDouble("fracD", 3.141592653589793);

        JsonElement json = parser.toJson(original);
        Tag restored = parser.fromJson(json);

        if (!(restored instanceof CompoundTag compound)) {
            return fail("Restored tag should be CompoundTag");
        }

        // Byte-range integers should come back as ByteTag
        Tag smallTag = compound.get("small");
        if (!(smallTag instanceof ByteTag)) {
            return fail("Byte-range value should restore as ByteTag, got " + (smallTag != null ? smallTag.getClass().getSimpleName() : "null"));
        }

        // Short-range integers should come back as ShortTag
        Tag mediumTag = compound.get("medium");
        if (!(mediumTag instanceof ShortTag)) {
            return fail("Short-range value should restore as ShortTag, got " + (mediumTag != null ? mediumTag.getClass().getSimpleName() : "null"));
        }

        // Int-range integers should come back as IntTag
        Tag largeTag = compound.get("large");
        if (!(largeTag instanceof IntTag)) {
            return fail("Int-range value should restore as IntTag, got " + (largeTag != null ? largeTag.getClass().getSimpleName() : "null"));
        }

        // Long-range integers should come back as LongTag
        Tag hugeTag = compound.get("huge");
        if (!(hugeTag instanceof LongTag)) {
            return fail("Long-range value should restore as LongTag, got " + (hugeTag != null ? hugeTag.getClass().getSimpleName() : "null"));
        }

        return pass("All numeric types inferred correctly");
    }

    private TestResult testNbtBooleanHandling() {
        NBTJsonParser parser = new NBTJsonParser();

        // Boolean true in JSON should become ByteTag(1), false -> ByteTag(0)
        com.google.gson.JsonObject jsonObj = new com.google.gson.JsonObject();
        jsonObj.addProperty("alive", true);
        jsonObj.addProperty("invulnerable", false);

        Tag tag = parser.fromJson(jsonObj);
        if (!(tag instanceof CompoundTag compound)) {
            return fail("Result should be CompoundTag");
        }

        Tag aliveTag = compound.get("alive");
        if (!(aliveTag instanceof ByteTag)) {
            return fail("Boolean true should become ByteTag, got " + (aliveTag != null ? aliveTag.getClass().getSimpleName() : "null"));
        }
        if (((ByteTag) aliveTag).getAsByte() != 1) {
            return fail("Boolean true should map to byte 1, got " + ((ByteTag) aliveTag).getAsByte());
        }

        Tag invTag = compound.get("invulnerable");
        if (!(invTag instanceof ByteTag)) {
            return fail("Boolean false should become ByteTag");
        }
        return assertEquals("Boolean false should map to byte 0", (byte) 0, ((ByteTag) invTag).getAsByte());
    }

    private TestResult testNbtNestedTags() {
        NBTJsonParser parser = new NBTJsonParser();

        CompoundTag inner = new CompoundTag();
        inner.putString("material", "diamond");
        inner.putInt("durability", 1561);

        CompoundTag outer = new CompoundTag();
        outer.putString("type", "sword");
        outer.put("properties", inner);

        JsonElement json = parser.toJson(outer);
        Tag restored = parser.fromJson(json);

        if (!(restored instanceof CompoundTag outerRestored)) {
            return fail("Outer tag should be CompoundTag");
        }
        if (!outerRestored.getString("type").equals("sword")) {
            return fail("Outer string should be 'sword'");
        }

        Tag propsTag = outerRestored.get("properties");
        if (!(propsTag instanceof CompoundTag innerRestored)) {
            return fail("Nested tag should be CompoundTag, got " + (propsTag != null ? propsTag.getClass().getSimpleName() : "null"));
        }
        if (!innerRestored.getString("material").equals("diamond")) {
            return fail("Nested string should be 'diamond', got " + innerRestored.getString("material"));
        }
        return assertEquals("Nested int should be preserved", 1561, innerRestored.getInt("durability"));
    }

    private TestResult testNbtListTags() {
        NBTJsonParser parser = new NBTJsonParser();

        ListTag list = new ListTag();
        list.add(IntTag.valueOf(10));
        list.add(IntTag.valueOf(20));
        list.add(IntTag.valueOf(30));

        CompoundTag root = new CompoundTag();
        root.put("scores", list);

        JsonElement json = parser.toJson(root);
        Tag restored = parser.fromJson(json);

        if (!(restored instanceof CompoundTag compound)) {
            return fail("Root should be CompoundTag");
        }
        Tag scoresTag = compound.get("scores");
        if (!(scoresTag instanceof ListTag restoredList)) {
            return fail("scores should be ListTag, got " + (scoresTag != null ? scoresTag.getClass().getSimpleName() : "null"));
        }
        if (restoredList.size() != 3) {
            return fail("ListTag should have 3 elements, got " + restoredList.size());
        }
        // Values 10, 20, 30 are in byte range, so they come back as ByteTag
        int sum = 0;
        for (Tag t : restoredList) {
            if (t instanceof NumericTag num) {
                sum += num.getAsInt();
            }
        }
        return assertEquals("Sum of list elements should be 60", 60, sum);
    }

    private TestResult testNbtStringTag() {
        NBTJsonParser parser = new NBTJsonParser();

        CompoundTag tag = new CompoundTag();
        tag.putString("greeting", "Hello, World!");

        JsonElement json = parser.toJson(tag);
        Tag restored = parser.fromJson(json);

        if (!(restored instanceof CompoundTag compound)) {
            return fail("Restored should be CompoundTag");
        }
        Tag greetTag = compound.get("greeting");
        if (!(greetTag instanceof StringTag)) {
            return fail("String value should restore as StringTag, got " + (greetTag != null ? greetTag.getClass().getSimpleName() : "null"));
        }
        return assertEquals("String content should match", "Hello, World!", ((StringTag) greetTag).getAsString());
    }
}
