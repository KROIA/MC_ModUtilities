package net.kroia.modutilities.event;

import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A simple observer-pattern signal that fires {@link Runnable} listeners with no payload.
 * Backed by a {@link CopyOnWriteArrayList} and dispatches via a snapshot, so listeners
 * may add or remove themselves (or other listeners) during {@link #notifyListeners()}
 * without triggering {@link java.util.ConcurrentModificationException}.
 * <p>
 * Each listener may have a maximum invocation count: when its remaining count reaches
 * zero it is automatically removed. A count of {@code -1} disables the limit.
 */
public class Signal {
    private final List<Pair<Runnable, Integer>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Registers a listener with no call-count limit.
     *
     * @param listener The runnable to invoke whenever this signal fires.
     */
    public void addListener(Runnable listener) {
        listeners.add(new Pair<>(listener, -1)); // -1 means no limit on calls
    }

    /**
     * Registers a listener with a maximum number of invocations.
     * The listener is automatically removed once the count reaches zero.
     *
     * @param listener The runnable to invoke whenever this signal fires.
     * @param maxCalls Maximum number of times this listener will fire; {@code -1} for unlimited.
     */
    public void addListener(Runnable listener, int maxCalls) {
        listeners.add(new Pair<>(listener, maxCalls));
    }

    /**
     * Removes the first registered occurrence of the given listener.
     *
     * @param listener The listener instance to remove.
     * @return {@code true} if a listener was removed, {@code false} otherwise.
     */
    public boolean removeListener(Runnable listener) {
        for (Pair<Runnable, Integer> pair : listeners) {
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
    public boolean setListenerRemainingCallCount(Runnable listener, int maxCalls) {
        for (Pair<Runnable, Integer> pair : listeners) {
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
    public int getListenerRemainingCallCount(Runnable listener) {
        for (Pair<Runnable, Integer> pair : listeners) {
            if (pair.getFirst().equals(listener)) {
                return pair.getSecond();
            }
        }
        return 0; // Not found, or no limit set
    }
    /**
     * Fires this signal, invoking every registered listener exactly once in registration order.
     * Listeners with a finite call count have their remaining count decremented; those reaching
     * zero are removed after dispatch completes.
     *
     * @apiNote
     * Iteration uses a snapshot of the listener list, so callbacks may safely call
     * {@link #addListener(Runnable)} or {@link #removeListener(Runnable)} during dispatch
     * without provoking {@link java.util.ConcurrentModificationException}. Such modifications
     * take effect for subsequent fires, not the current one.
     */
    public void notifyListeners() {
        List<Pair<Runnable, Integer>> snapshot = new ArrayList<>(listeners);
        List<Runnable> toRemove = new ArrayList<>();
        for (Pair<Runnable, Integer> pair : snapshot) {
            Runnable listener = pair.getFirst();
            listener.run();
            Integer count = pair.getSecond();
            if (count == 1) {
                toRemove.add(listener);
            } else if (count > 0) {
                setListenerRemainingCallCount(listener, count - 1);
            }
        }
        for (Runnable listener : toRemove) {
            removeListener(listener);
        }
    }

    /**
     * Removes every registered listener from this signal.
     */
    public void removeListeners() {
        listeners.clear();
    }


}
