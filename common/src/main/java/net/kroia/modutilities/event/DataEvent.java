package net.kroia.modutilities.event;

import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DataEvent<T> {


    private List<Pair<Consumer<T>, Integer>> listeners = new ArrayList<>();

    public void addListener(Consumer<T> listener) {
        listeners.add(new Pair<>(listener, -1)); // -1 means no limit on calls
    }
    public void addListener(Consumer<T> listener, int maxCalls) {
        listeners.add(new Pair<>(listener, maxCalls));
    }
    public boolean removeListener(Consumer<T> listener) {
        for(int i = 0; i < listeners.size(); i++) {
            if (listeners.get(i).getFirst().equals(listener)) {
                listeners.remove(i);
                return true;
            }
        }
        return false;
    }
    public boolean setListenerRemainingCallCount(Consumer<T> listener, int maxCalls) {
        for (int i = 0; i < listeners.size(); i++) {
            Pair<Consumer<T>, Integer> pair = listeners.get(i);
            if (pair.getFirst().equals(listener)) {
                listeners.set(i, new Pair<>(listener, maxCalls));
                return true;
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
        List<Integer> toRemove = new ArrayList<>();
        int index = 0;
        for (Pair<Consumer<T>, Integer> pair : listeners) {
            pair.getFirst().accept(value);
            Integer count = pair.getSecond();
            if(count == 1)
                toRemove.add(index);
            else if(count > 0 && count != -1)
            {
                pair = new Pair<>(pair.getFirst(), count - 1);
                listeners.set(index, pair);
            }
            index++;
        }
        for (int i= toRemove.size() - 1; i >= 0; i--) {
            int indexToRemove = toRemove.get(i);
            if (indexToRemove >= 0 && indexToRemove < listeners.size()) {
                listeners.remove(indexToRemove);
            }
        }
    }

    public void removeListeners() {
        listeners.clear();
    }


}
