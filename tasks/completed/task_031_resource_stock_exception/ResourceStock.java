package learning.task031;

import java.util.HashMap;
import java.util.Map;

public class ResourceStock {

    private final Map<String, Integer> stock = new HashMap<>();

    public void add(String resourceName, int quantity) {
        if (resourceName == null || quantity < 1) throw new IllegalArgumentException();
        resourceName = resourceName.trim();
        if (resourceName.isEmpty()) throw new IllegalArgumentException();
        if (stock.containsKey(resourceName)) {
            stock.put(resourceName, stock.get(resourceName) + quantity);
        } else {
            stock.put(resourceName, quantity);
        }
    }

    public void reserve(String resourceName, int quantity) throws InsufficientStockException {
        if (resourceName == null || quantity < 1) throw new IllegalArgumentException();
        resourceName = resourceName.trim();
        if (resourceName.isEmpty()) throw new IllegalArgumentException();
        if (stock.containsKey(resourceName)) {
            if (stock.get(resourceName) >= quantity) {
                stock.put(resourceName, stock.get(resourceName) - quantity);
                if (stock.get(resourceName) == 0) {
                    stock.remove(resourceName);
                }
            } else {
                throw new InsufficientStockException(resourceName, quantity, stock.get(resourceName));
            }
        } else {
            throw new InsufficientStockException(resourceName, quantity, 0);
        }
    }

    public int available(String resourceName) {
        if (resourceName == null) throw new IllegalArgumentException();
        resourceName = resourceName.trim();
        if (resourceName.isEmpty()) throw new IllegalArgumentException();
        return stock.getOrDefault(resourceName, 0);
    }

    public Map<String, Integer> snapshot() {
        return Map.copyOf(stock);
    }
}
