package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.event.DataEvent;
import net.kroia.modutilities.event.Signal;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;

public class EventTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.EVENTS;
    }

    @Override
    public void registerTests() {
        addTest("signal_fires", this::testSignalFires);
        addTest("signal_multiple_listeners", this::testSignalMultipleListeners);
        addTest("signal_remove_listener", this::testSignalRemoveListener);
        addTest("signal_max_calls", this::testSignalMaxCalls);
        addTest("signal_unlimited_calls", this::testSignalUnlimitedCalls);
        addTest("signal_clear_listeners", this::testSignalClearListeners);
        addTest("data_event_carries_data", this::testDataEventCarriesData);
        addTest("data_event_multiple_listeners", this::testDataEventMultipleListeners);
        addTest("data_event_remove_listener", this::testDataEventRemoveListener);
        addTest("data_event_max_calls", this::testDataEventMaxCalls);
    }

    private TestResult testSignalFires() {
        Signal signal = new Signal();
        boolean[] fired = {false};
        signal.addListener(() -> fired[0] = true);
        signal.notifyListeners();
        return assertTrue("Signal should fire listener", fired[0]);
    }

    private TestResult testSignalMultipleListeners() {
        Signal signal = new Signal();
        int[] count = {0};
        signal.addListener(() -> count[0]++);
        signal.addListener(() -> count[0]++);
        signal.addListener(() -> count[0]++);
        signal.notifyListeners();
        return assertEquals("All 3 listeners should fire", 3, count[0]);
    }

    private TestResult testSignalRemoveListener() {
        Signal signal = new Signal();
        int[] count = {0};
        Runnable listener = () -> count[0]++;
        signal.addListener(listener);
        signal.removeListener(listener);
        signal.notifyListeners();
        return assertEquals("Removed listener should not fire", 0, count[0]);
    }

    private TestResult testSignalMaxCalls() {
        Signal signal = new Signal();
        int[] count = {0};
        signal.addListener(() -> count[0]++, 2);
        signal.notifyListeners();
        signal.notifyListeners();
        signal.notifyListeners();
        return assertEquals("Listener with maxCalls=2 should fire exactly 2 times", 2, count[0]);
    }

    private TestResult testSignalUnlimitedCalls() {
        Signal signal = new Signal();
        int[] count = {0};
        signal.addListener(() -> count[0]++, -1);
        for (int i = 0; i < 10; i++) {
            signal.notifyListeners();
        }
        return assertEquals("Unlimited listener should fire 10 times", 10, count[0]);
    }

    private TestResult testSignalClearListeners() {
        Signal signal = new Signal();
        int[] count = {0};
        signal.addListener(() -> count[0]++);
        signal.addListener(() -> count[0]++);
        signal.removeListeners();
        signal.notifyListeners();
        return assertEquals("Cleared listeners should not fire", 0, count[0]);
    }

    private TestResult testDataEventCarriesData() {
        DataEvent<String> event = new DataEvent<>();
        String[] received = {null};
        event.addListener(data -> received[0] = data);
        event.notifyListeners("hello");
        return assertEquals("Event data should be received", "hello", received[0]);
    }

    private TestResult testDataEventMultipleListeners() {
        DataEvent<Integer> event = new DataEvent<>();
        int[] sum = {0};
        event.addListener(data -> sum[0] += data);
        event.addListener(data -> sum[0] += data);
        event.notifyListeners(5);
        return assertEquals("Both listeners should receive data", 10, sum[0]);
    }

    private TestResult testDataEventRemoveListener() {
        DataEvent<String> event = new DataEvent<>();
        int[] count = {0};
        java.util.function.Consumer<String> listener = data -> count[0]++;
        event.addListener(listener);
        event.removeListener(listener);
        event.notifyListeners("test");
        return assertEquals("Removed listener should not fire", 0, count[0]);
    }

    private TestResult testDataEventMaxCalls() {
        DataEvent<String> event = new DataEvent<>();
        int[] count = {0};
        event.addListener(data -> count[0]++, 1);
        event.notifyListeners("first");
        event.notifyListeners("second");
        return assertEquals("Listener with maxCalls=1 should fire once", 1, count[0]);
    }
}
