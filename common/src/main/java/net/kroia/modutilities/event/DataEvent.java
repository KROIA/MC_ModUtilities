package net.kroia.modutilities.event;

import com.mojang.datafixers.util.Pair;

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


    private final List<Pair<Consumer<T>, Integer>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Registers a listener with no call-count limit.
     *
     * @param listener The consumer to invoke whenever this event fires.
     */
    public void addListener(Consumer<T> listener) {
        listeners.add(new Pair<>(listener, -1)); // -1 means no limit on calls
    }

    /**
     * Registers a listener with a maximum number of invocations.
     * The listener is automatically removed once the count reaches zero.
     *
     * @param listener The consumer to invoke whenever this event fires.
     * @param maxCalls Maximum number of times this listener will fire; {@code -1} for unlimited.
     */
    public void addListener(Consumer<T> listener, int maxCalls) {
        listeners.add(new Pair<>(listener, maxCalls));
    }

    /**
     * Removes the first registered occurrence of the given listener.
     *
     * @param listener The listener instance to remove.
     * @return {@code true} if a listener was removed, {@code false} otherwise.
     */
    public boolean removeListener(Consumer<T> listener) {
        for (Pair<Consumer<T>, Integer> pair : listeners) {
            if (pair.getFirst().equals(listener)) {
                return listeners.remove(pair);
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
        for (Pair<Consumer<T>, Integer> pair : listeners) {
            if (pair.getFirst().equals(listener)) {
                int idx = listeners.indexOf(pair);
                if (idx >= 0) {
                    listeners.set(idx, new Pair<>(listener, maxCalls));
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Returns the remaining call count for the given listener.
     *
     * @param listener The listener to query.
     * @return The remaining call count ({@code -1} for unlimited), or {@code 0} if the listener is not registered.
     */
    public int getListenerRemainingCallCount(Consumer<T> listener) {
        for (Pair<Consumer<T>, Integer> pair : listeners) {
            if (pair.getFirst().equals(listener)) {
                return pair.getSecond();
            }
        }
        return 0; // Not found, or no limit set
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
        List<Pair<Consumer<T>, Integer>> snapshot = new ArrayList<>(listeners);
        List<Consumer<T>> toRemove = new ArrayList<>();
        for (Pair<Consumer<T>, Integer> pair : snapshot) {
            Consumer<T> listener = pair.getFirst();
            listener.accept(value);
            Integer count = pair.getSecond();
            if (count == 1) {
                toRemove.add(listener);
            } else if (count > 0) {
                setListenerRemainingCallCount(listener, count - 1);
            }
        }
        for (Consumer<T> listener : toRemove) {
            removeListener(listener);
        }
    }

    /**
     * Removes every registered listener from this event.
     */
    public void removeListeners() {
        listeners.clear();
    }


}
