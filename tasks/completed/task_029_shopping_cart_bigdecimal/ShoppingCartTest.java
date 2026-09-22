package learning.task029;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShoppingCartTest {
    private static final Product POTION = new Product(" potion ", new BigDecimal("1.25"));
    private static final Product SWORD = new Product("sword", new BigDecimal("10.00"));

    @Test
    void addsProductsAndCalculatesExactTotal() {
        ShoppingCart cart = new ShoppingCart();
        cart.add(POTION, 2);
        cart.add(POTION, 3);
        cart.add(SWORD, 1);

        assertEquals(5, cart.quantityOf(POTION));
        assertEquals(2, cart.distinctProducts());
        assertEquals(new BigDecimal("16.25"), cart.total());
        assertEquals(Map.of(new Product("potion", new BigDecimal("1.25")), 5, SWORD, 1), cart.items());
    }

    @Test
    void removesLastProductAndReturnsWhetherProductExisted() {
        ShoppingCart cart = new ShoppingCart();
        cart.add(POTION, 1);

        assertTrue(cart.removeOne(POTION));
        assertEquals(0, cart.quantityOf(POTION));
        assertEquals(BigDecimal.ZERO, cart.total());
        assertFalse(cart.removeOne(POTION));
    }

    @Test
    void validatesArgumentsAndReturnsImmutableItems() {
        ShoppingCart cart = new ShoppingCart();
        cart.add(POTION, 1);

        assertThrows(UnsupportedOperationException.class, () -> cart.items().clear());
        assertThrows(IllegalArgumentException.class, () -> new Product(null, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new Product(" ", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new Product("potion", BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> cart.add(null, 1));
        assertThrows(IllegalArgumentException.class, () -> cart.add(POTION, 0));
        assertThrows(IllegalArgumentException.class, () -> cart.quantityOf(null));
    }
}
