package learning.task007;

import java.util.HashMap;
import java.util.Map;

public class Inventory {

    private final Map<Item, Integer> items = new HashMap<>();

    public void addItem(Item item, int amount) {
        if (amount < 1) throw new IllegalArgumentException();
        if (items.containsKey(item)) {
            items.put(item, items.get(item) + amount);
        } else {
            items.put(item, amount);
        }
    }

    public boolean removeItem(Item item, int amount) {
        if (amount < 1) throw new IllegalArgumentException();
        if (items.containsKey(item)) {
            if (items.get(item) >= amount) {
                items.put(item, items.get(item) - amount);
                if (items.get(item) <= 0) items.remove(item);
                return true;
            }
        }
        return false;
    }

    public int getQuantity(Item item) {
        if  (items.containsKey(item)) {
            return items.get(item);
        }
        return 0;
    }

    public int getDistinctItemCount() {
        return items.size();
    }

    public int getTotalItemCount() {
        int total = 0;
        for (Item item : items.keySet()) {
            total += items.get(item);
        }
        return total;
    }

    public int getTotalValue() {
        int total = 0;
        for (Item item : items.keySet()) {
            total += item.getPrice() * items.get(item);
        }
        return total;
    }

    public int getTotalValueByType(ItemType type) {
        int total = 0;
        for (Item item : items.keySet()) {
            if (item.getType().equals(type)) {
                total += item.getPrice() * items.get(item);
            }
        }
        return total;
    }

    public Item findByName(String name) {
        for (Item item : items.keySet()) {
            if (item.getName().equalsIgnoreCase(name.trim())) {
                return item;
            }
        }
        return null;
    }
}
