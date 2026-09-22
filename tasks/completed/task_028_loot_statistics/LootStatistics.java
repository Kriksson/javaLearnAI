package learning.task028;

import java.util.*;

public class LootStatistics {

    private final Map<String, Integer> items = new HashMap<>();

    public void register(LootDrop drop) {
        if (drop == null) throw new IllegalArgumentException();
        items.merge(drop.itemName(), drop.quantity(), (a, b) -> a + b);
    }

    public int totalFor(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) throw new IllegalArgumentException();
        itemName = itemName.trim();
        return items.getOrDefault(itemName, 0);
    }

    public Map<String, Integer> totals() {
        return Map.copyOf(items);
    }

    public List<String> mostCommon(int limit) {
        if (limit < 0) throw new IllegalArgumentException();
        if (limit == 0) return List.of();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(items.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).
                reversed().
                thenComparing(Map.Entry::getKey));
        List<String> result  = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : entries) {
            result.add(entry.getKey());
        }
        return List.copyOf(result.subList(0, Math.min(limit, result.size())));
    }
}
