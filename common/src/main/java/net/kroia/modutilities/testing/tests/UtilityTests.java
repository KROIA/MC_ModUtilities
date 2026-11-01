package net.kroia.modutilities.testing.tests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.kroia.modutilities.ColorUtilities;
import net.kroia.modutilities.ItemUtilities;
import net.kroia.modutilities.JsonUtilities;
import net.kroia.modutilities.TimerMillis;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;

public class UtilityTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.UTILITIES;
    }

    @Override
    public void registerTests() {
        // ColorUtilities tests
        addTest("color_getRed", this::testColorGetRed);
        addTest("color_getGreen", this::testColorGetGreen);
        addTest("color_getBlue", this::testColorGetBlue);
        addTest("color_getAlpha", this::testColorGetAlpha);
        addTest("color_getRGB_three_channels", this::testColorGetRGBThreeChannels);
        addTest("color_getRGB_four_channels", this::testColorGetRGBFourChannels);
        addTest("color_setAlpha_int", this::testColorSetAlphaInt);
        addTest("color_setRed_int", this::testColorSetRedInt);
        addTest("color_setBrightness_half", this::testColorSetBrightnessHalf);
        addTest("color_setBrightness_clamps_high", this::testColorSetBrightnessClampsHigh);
        addTest("color_interpolate_midpoint", this::testColorInterpolateMidpoint);
        addTest("color_interpolate_endpoints", this::testColorInterpolateEndpoints);

        // JsonUtilities tests
        addTest("json_roundtrip_compact", this::testJsonRoundtripCompact);
        addTest("json_pretty_contains_newline", this::testJsonPrettyContainsNewline);
        addTest("json_fromString_invalid_throws", this::testJsonFromStringInvalidThrows);

        // TimerMillis tests
        addTest("timer_initial_state", this::testTimerInitialState);
        addTest("timer_start_and_running", this::testTimerStartAndRunning);
        addTest("timer_finished_after_duration", this::testTimerFinishedAfterDuration);
        addTest("timer_stop_resets", this::testTimerStopResets);
        addTest("timer_check_with_autoRestart", this::testTimerCheckWithAutoRestart);
        addTest("timer_elapsed_and_remaining", this::testTimerElapsedAndRemaining);

        // ItemUtilities pure-string helper tests
        addTest("itemutil_searchText_from_full_id", this::testSearchTextFromFullId);
        addTest("itemutil_searchText_from_bare_id", this::testSearchTextFromBareId);
        addTest("itemutil_searchText_null_and_empty", this::testSearchTextNullAndEmpty);
    }

    // ========================================================================
    // ColorUtilities tests
    // ========================================================================

    private TestResult testColorGetRed() {
        int color = 0xFFAB12CD; // A=FF, R=AB, G=12, B=CD
        return assertEquals("Red channel should be 0xAB (171)", 0xAB, ColorUtilities.getRed(color));
    }

    private TestResult testColorGetGreen() {
        int color = 0xFFAB12CD;
        return assertEquals("Green channel should be 0x12 (18)", 0x12, ColorUtilities.getGreen(color));
    }

    private TestResult testColorGetBlue() {
        int color = 0xFFAB12CD;
        return assertEquals("Blue channel should be 0xCD (205)", 0xCD, ColorUtilities.getBlue(color));
    }

    private TestResult testColorGetAlpha() {
        int color = 0x80AB12CD;
        return assertEquals("Alpha channel should be 0x80 (128)", 0x80, ColorUtilities.getAlpha(color));
    }

    private TestResult testColorGetRGBThreeChannels() {
        // getRGB(r, g, b) should set alpha to 255
        int result = ColorUtilities.getRGB(100, 150, 200);
        int expected = 0xFF6496C8; // A=FF, R=64, G=96, B=C8
        return assertEquals("getRGB(100,150,200) should produce 0xFF6496C8", expected, result);
    }

    private TestResult testColorGetRGBFourChannels() {
        int result = ColorUtilities.getRGB(255, 0, 128, 64);
        int expected = 0x40FF0080; // A=40, R=FF, G=00, B=80
        return assertEquals("getRGB(255,0,128,64) should produce 0x40FF0080", expected, result);
    }

    private TestResult testColorSetAlphaInt() {
        int color = 0xFFAABBCC;
        int result = ColorUtilities.setAlpha(color, 0x40);
        return assertEquals("setAlpha should replace only alpha byte", 0x40AABBCC, result);
    }

    private TestResult testColorSetRedInt() {
        int color = 0xFF00BBCC;
        int result = ColorUtilities.setRed(color, 0xDD);
        return assertEquals("setRed should replace only red byte", 0xFFDDBBCC, result);
    }

    private TestResult testColorSetBrightnessHalf() {
        // White at half brightness -> each channel 255*0.5 = 127
        int white = 0xFF_FF_FF_FF; // A=FF, R=FF, G=FF, B=FF
        int result = ColorUtilities.setBrightness(white, 0.5f);
        int r = ColorUtilities.getRed(result);
        int g = ColorUtilities.getGreen(result);
        int b = ColorUtilities.getBlue(result);
        int a = ColorUtilities.getAlpha(result);
        // (int)(255 * 0.5f) = 127
        boolean channelsOk = r == 127 && g == 127 && b == 127;
        boolean alphaPreserved = a == 0xFF;
        return assertTrue("Half brightness white should have RGB=127 and alpha preserved",
                channelsOk && alphaPreserved);
    }

    private TestResult testColorSetBrightnessClampsHigh() {
        int color = ColorUtilities.getRGB(200, 200, 200, 255);
        int result = ColorUtilities.setBrightness(color, 2.0f);
        // 200*2 = 400, clamped to 255
        int r = ColorUtilities.getRed(result);
        int g = ColorUtilities.getGreen(result);
        int b = ColorUtilities.getBlue(result);
        return assertTrue("Brightness >1.0 should clamp channels to 255",
                r == 255 && g == 255 && b == 255);
    }

    private TestResult testColorInterpolateMidpoint() {
        int black = 0xFF000000;
        int white = 0xFFFFFFFF;
        int mid = ColorUtilities.interpolate(black, white, 0.5f);
        int r = ColorUtilities.getRed(mid);
        int g = ColorUtilities.getGreen(mid);
        int b = ColorUtilities.getBlue(mid);
        // (int)(0 + 255 * 0.5) = 127
        return assertTrue("Midpoint of black and white should be ~127 per channel",
                r == 127 && g == 127 && b == 127);
    }

    private TestResult testColorInterpolateEndpoints() {
        int c1 = 0xFF102030;
        int c2 = 0xFF405060;
        int atZero = ColorUtilities.interpolate(c1, c2, 0.0f);
        int atOne = ColorUtilities.interpolate(c1, c2, 1.0f);
        boolean zeroOk = atZero == c1;
        boolean oneOk = atOne == c2;
        return assertTrue("Interpolate at 0.0 should return color1 and at 1.0 should return color2",
                zeroOk && oneOk);
    }

    // ========================================================================
    // JsonUtilities tests
    // ========================================================================

    private TestResult testJsonRoundtripCompact() {
        String input = "{\"key\":\"value\",\"num\":42}";
        JsonElement parsed = JsonUtilities.fromString(input);
        String output = JsonUtilities.toString(parsed);
        // Re-parse to compare structurally (key order may differ)
        JsonElement reparsed = JsonUtilities.fromString(output);
        return assertEquals("Compact roundtrip should preserve structure", parsed, reparsed);
    }

    private TestResult testJsonPrettyContainsNewline() {
        JsonObject obj = new JsonObject();
        obj.addProperty("a", 1);
        obj.addProperty("b", 2);
        String pretty = JsonUtilities.toPrettyString(obj);
        return assertTrue("Pretty-printed JSON should contain newlines", pretty.contains("\n"));
    }

    private TestResult testJsonFromStringInvalidThrows() {
        return assertThrows("Parsing invalid JSON should throw JsonSyntaxException",
                JsonSyntaxException.class,
                () -> JsonUtilities.fromString("{not valid json!!!}"));
    }

    // ========================================================================
    // TimerMillis tests
    // ========================================================================

    private long savedOffset;

    @Override
    public void setup() {
        savedOffset = TimerMillis.TIMER_OFFSET_MS;
        TimerMillis.TIMER_OFFSET_MS = 0;
    }

    @Override
    public void teardown() {
        TimerMillis.TIMER_OFFSET_MS = savedOffset;
    }

    private TestResult testTimerInitialState() {
        TimerMillis timer = new TimerMillis(false);
        boolean notRunning = !timer.isRunning();
        boolean notFinished = !timer.isFinished();
        boolean elapsedZero = timer.getElapsedTime() == 0;
        boolean remainingZero = timer.getRemainingTime() == 0;
        return assertTrue("New timer should not be running, not finished, elapsed=0, remaining=0",
                notRunning && notFinished && elapsedZero && remainingZero);
    }

    private TestResult testTimerStartAndRunning() {
        TimerMillis timer = new TimerMillis(false);
        timer.start(10_000); // 10 seconds
        boolean running = timer.isRunning();
        boolean notFinished = !timer.isFinished();
        return assertTrue("Started timer should be running and not finished", running && notFinished);
    }

    private TestResult testTimerFinishedAfterDuration() {
        TimerMillis timer = new TimerMillis(false);
        timer.start(100);
        // Advance time past the duration using the offset
        TimerMillis.TIMER_OFFSET_MS += 200;
        boolean finished = timer.isFinished();
        boolean notRunning = !timer.isRunning();
        // Reset offset for subsequent tests
        TimerMillis.TIMER_OFFSET_MS -= 200;
        return assertTrue("Timer should be finished after duration elapses", finished && notRunning);
    }

    private TestResult testTimerStopResets() {
        TimerMillis timer = new TimerMillis(false);
        timer.start(10_000);
        timer.stop();
        boolean notRunning = !timer.isRunning();
        boolean elapsedZero = timer.getElapsedTime() == 0;
        return assertTrue("Stopped timer should not be running and elapsed should be 0",
                notRunning && elapsedZero);
    }

    private TestResult testTimerCheckWithAutoRestart() {
        TimerMillis timer = new TimerMillis(true);
        timer.start(100);
        // Advance past duration
        TimerMillis.TIMER_OFFSET_MS += 200;
        boolean checkResult = timer.check(); // should return true and auto-restart
        boolean isRunningAfter = timer.isRunning();
        // Reset offset
        TimerMillis.TIMER_OFFSET_MS -= 200;
        return assertTrue("check() should return true when finished and auto-restart should re-start the timer",
                checkResult && isRunningAfter);
    }

    private TestResult testTimerElapsedAndRemaining() {
        TimerMillis timer = new TimerMillis(false);
        timer.start(1000);
        // Advance by 400ms
        TimerMillis.TIMER_OFFSET_MS += 400;
        long elapsed = timer.getElapsedTime();
        long remaining = timer.getRemainingTime();
        // Reset offset
        TimerMillis.TIMER_OFFSET_MS -= 400;
        // Elapsed should be ~400, remaining should be ~600
        // Allow a small tolerance for execution time (up to 50ms)
        boolean elapsedOk = elapsed >= 400 && elapsed <= 450;
        boolean remainingOk = remaining >= 550 && remaining <= 600;
        return assertTrue("Elapsed should be ~400ms and remaining ~600ms (tolerance 50ms)",
                elapsedOk && remainingOk);
    }

    // ========================================================================
    // ItemUtilities string-helper tests
    // ========================================================================

    private TestResult testSearchTextFromFullId() {
        String result = ItemUtilities.getSearchTextFromItemID("minecraft:diamond_sword");
        return assertEquals("Should extract and format path from full ID",
                "diamond sword", result);
    }

    private TestResult testSearchTextFromBareId() {
        String result = ItemUtilities.getSearchTextFromItemID("oak_planks");
        return assertEquals("Should format bare ID with spaces",
                "oak planks", result);
    }

    private TestResult testSearchTextNullAndEmpty() {
        String fromNull = ItemUtilities.getSearchTextFromItemID(null);
        String fromEmpty = ItemUtilities.getSearchTextFromItemID("");
        return assertTrue("Null and empty should both return empty string",
                fromNull.isEmpty() && fromEmpty.isEmpty());
    }
}
