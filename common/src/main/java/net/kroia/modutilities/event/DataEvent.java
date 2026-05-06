package net.kroia.modutilities.event;

import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class DataEvent<T> {


    private final List<Pair<Consumer<T>, Integer>> listeners = new CopyOnWriteArrayList<>();

    public void addListener(Consumer<T> listener) {
        listeners.add(new Pair<>(listener, -1)); // -1 means no limit on calls
    }
    public void addListener(Consumer<T> listener, int maxCalls) {
        listeners.add(new Pair<>(listener, maxCalls));
    }
    public boolean removeListener(Consumer<T> listener) {
        for (Pair<Consumer<T>, Integer> pair : listeners) {
            if (pair.getFirst().equals(listener)) {
                return listeners.remove(pair);
            }
        }
        return false;
    }
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
    public int getListenerRemainingCallCount(Consumer<T> listener) {
        for (Pair<Consumer<T>, Integer> pair : listeners) {
            if (pair.getFirst().equals(listener)) {
                return pair.getSecond();
            }
        }
        return 0; // Not found, or no limit set
    }
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

    public void removeListeners() {
        listeners.clear();
    }


}
