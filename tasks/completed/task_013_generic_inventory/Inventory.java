package learning.task013;

import java.util.ArrayList;
import java.util.List;

public class Inventory<T> {
    private final List<T> inventory = new ArrayList<>();

    public Inventory() {}

    public void add(T item) {
        if (item == null) throw new IllegalArgumentException();
        inventory.add(item);
    }

    public T get(int index) {
        return inventory.get(index);
    }

    public boolean remove(T item) {
        return inventory.remove(item);
    }

    public boolean contains(T item) {
        if (item == null) throw new IllegalArgumentException();
        return inventory.contains(item);
    }

    public int size() {
        return inventory.size();
    }
}
