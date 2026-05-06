package net.kroia.modutilities.event;

import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Signal {
    private final List<Pair<Runnable, Integer>> listeners = new CopyOnWriteArrayList<>();

    public void addListener(Runnable listener) {
        listeners.add(new Pair<>(listener, -1)); // -1 means no limit on calls
    }

    public void addListener(Runnable listener, int maxCalls) {
        listeners.add(new Pair<>(listener, maxCalls));
    }
    public boolean removeListener(Runnable listener) {
        for (Pair<Runnable, Integer> pair : listeners) {
            if (pair.getFirst().equals(listener)) {
                return listeners.remove(pair);
            }
        }
        return false;
    }

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
    public int getListenerRemainingCallCount(Runnable listener) {
        for (Pair<Runnable, Integer> pair : listeners) {
            if (pair.getFirst().equals(listener)) {
                return pair.getSecond();
            }
        }
        return 0; // Not found, or no limit set
    }
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

    public void removeListeners() {
        listeners.clear();
    }


}
