package learning.task029;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class ShoppingCart {

    private final Map<Product, Integer> shoppingCart = new HashMap<>();

    public void add(Product product, int quantity) {
        if (product == null) throw new IllegalArgumentException();
        if (quantity <= 0) throw new IllegalArgumentException();
        shoppingCart.merge(product, quantity, Integer::sum);
    }

    public boolean removeOne(Product product) {
        if (!shoppingCart.containsKey(product)) return false;
        shoppingCart.replace(product, shoppingCart.get(product) - 1);
        if (shoppingCart.get(product) == 0) shoppingCart.remove(product);
        return true;
    }

    public int quantityOf(Product product) {
        if (product == null) throw new IllegalArgumentException();
        if (!shoppingCart.containsKey(product)) return 0;
        return shoppingCart.get(product);
    }

    public int distinctProducts() {
        return shoppingCart.size();
    }

    public BigDecimal total() {
        BigDecimal total = new BigDecimal("0");
        for (Map.Entry<Product, Integer> entry : shoppingCart.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            total = total.add(product.price().multiply(BigDecimal.valueOf(quantity)));
        }
        return total;
    }

    public Map<Product, Integer> items() {
        return Map.copyOf(shoppingCart);
    }
}
