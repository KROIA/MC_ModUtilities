package net.kroia.modutilities.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A typed observer-pattern event that delivers a value of type {@code T} to its
 * {@link Consumer} listeners. Backed by a {@link CopyOnWriteArrayList} and dispatched
 * via a snapshot, so listeners may add or remove themselves (or other listeners) during
 * {@link #notifyListeners(Object)} without triggering {@link java.util.ConcurrentModificationException}.
 * <p>
 * Each listener may have a maximum invocation count: when its remaining count reaches
 * zero it is automatically removed. A count of {@code -1} disables the limit.
 *
 * @param <T> The payload type delivered to listeners.
 */
public class DataEvent<T> {

    /**
     * Mutable holder for a listener and its remaining call count.
     * Using a mutable field avoids replacing entries in the CopyOnWriteArrayList
     * (which would clone the entire backing array on every {@code set()} call).
     * The {@code volatile} keyword ensures cross-thread visibility of count updates.
     */
    private static class ListenerEntry<T> {
        final Consumer<T> listener;
        volatile int remainingCalls;

        ListenerEntry(Consumer<T> listener, int remainingCalls) {
            this.listener = listener;
            this.remainingCalls = remainingCalls;
        }
    }

    private final List<ListenerEntry<T>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Registers a listener with no call-count limit.
     *
     * @param listener The consumer to invoke whenever this event fires.
     */
    public void addListener(Consumer<T> listener) {
        listeners.add(new ListenerEntry<>(listener, -1)); // -1 means no limit on calls
    }

    /**
     * Registers a listener with a maximum number of invocations.
     * The listener is automatically removed once the count reaches zero.
     *
     * @param listener The consumer to invoke whenever this event fires.
     * @param maxCalls Maximum number of times this listener will fire; {@code -1} for unlimited.
     */
    public void addListener(Consumer<T> listener, int maxCalls) {
        listeners.add(new ListenerEntry<>(listener, maxCalls));
    }

    /**
     * Removes the first registered occurrence of the given listener.
     *
     * @param listener The listener instance to remove.
     * @return {@code true} if a listener was removed, {@code false} otherwise.
     */
    public boolean removeListener(Consumer<T> listener) {
        for (ListenerEntry<T> entry : listeners) {
            if (entry.listener.equals(listener)) {
                return listeners.remove(entry);
            }
        }
        return false;
    }
    /**
     * Updates the remaining call count for an already-registered listener.
     *
     * @param listener The listener whose limit should be updated.
     * @param maxCalls The new remaining call count; {@code -1} for unlimited.
     * @return {@code true} if the listener was found and updated, {@code false} otherwise.
     */
    public boolean setListenerRemainingCallCount(Consumer<T> listener, int maxCalls) {
        for (ListenerEntry<T> entry : listeners) {
            if (entry.listener.equals(listener)) {
                entry.remainingCalls = maxCalls;
                return true;
            }
        }
        return false;
    }
    /**
     * Returns the remaining call count for the given listener.
     *
     * @param listener The listener to query.
     * @return The remaining call count ({@code -1} for unlimited), or {@code -1} if the listener is not registered.
     */
    public int getListenerRemainingCallCount(Consumer<T> listener) {
        for (ListenerEntry<T> entry : listeners) {
            if (entry.listener.equals(listener)) {
                return entry.remainingCalls;
            }
        }
        return -1; // Listener not found
    }
    /**
     * Fires this event, delivering the given value to every registered listener exactly
     * once in registration order. Listeners with a finite call count have their remaining
     * count decremented; those reaching zero are removed after dispatch completes.
     *
     * @param value The payload to deliver to each listener.
     *
     * @apiNote
     * Iteration uses a snapshot of the listener list, so callbacks may safely call
     * {@link #addListener(Consumer)} or {@link #removeListener(Consumer)} during dispatch
     * without provoking {@link java.util.ConcurrentModificationException}. Such modifications
     * take effect for subsequent fires, not the current one.
     */
    public void notifyListeners(T value) {
        List<ListenerEntry<T>> snapshot = new ArrayList<>(listeners);
        List<ListenerEntry<T>> toRemove = new ArrayList<>();
        for (ListenerEntry<T> entry : snapshot) {
            entry.listener.accept(value);
            int count = entry.remainingCalls;
            if (count == 1) {
                toRemove.add(entry);
            } else if (count > 0) {
                entry.remainingCalls = count - 1;
            }
        }
        if (!toRemove.isEmpty()) {
            listeners.removeAll(toRemove);
        }
    }

    /**
     * Removes every registered listener from this event.
     */
    public void removeListeners() {
        listeners.clear();
    }


}
