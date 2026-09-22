package learning.task029;

import java.math.BigDecimal;

public record Product(String sku, BigDecimal price) {
    public Product(String sku, BigDecimal price) {
        if (sku == null || sku.trim().isEmpty()) throw new IllegalArgumentException();
        sku = sku.trim();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException();
        this.sku = sku;
        this.price = price;
    }
}
