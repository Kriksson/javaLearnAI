package learning.task031;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ResourceStockTest {
    @Test
    void addsAndReservesResources() throws InsufficientStockException {
        ResourceStock stock = new ResourceStock();
        stock.add(" crystal ", 5);
        stock.add("crystal", 2);
        stock.add("wood", 4);

        stock.reserve(" crystal ", 3);

        assertEquals(4, stock.available("crystal"));
        assertEquals(4, stock.available("wood"));
        assertEquals(Map.of("crystal", 4, "wood", 4), stock.snapshot());
    }

    @Test
    void keepsStateWhenStockIsInsufficient() {
        ResourceStock stock = new ResourceStock();
        stock.add("crystal", 2);

        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                () -> stock.reserve("crystal", 3));
        assertThrows(InsufficientStockException.class, () -> stock.reserve("wood", 1));

        assertEquals(2, stock.available("crystal"));
        assertEquals(true, exception.getMessage().contains("crystal"));
        assertEquals(true, exception.getMessage().contains("3"));
        assertEquals(true, exception.getMessage().contains("2"));
    }

    @Test
    void validatesInputAndProtectsSnapshot() {
        ResourceStock stock = new ResourceStock();
        stock.add("wood", 1);

        assertThrows(IllegalArgumentException.class, () -> stock.add(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> stock.add("wood", 0));
        assertThrows(IllegalArgumentException.class, () -> stock.reserve(null, 1));
        assertThrows(IllegalArgumentException.class, () -> stock.reserve("wood", 0));
        assertThrows(UnsupportedOperationException.class, () -> stock.snapshot().clear());
    }
}
