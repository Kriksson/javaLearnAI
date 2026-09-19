package learning.task007;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryTest {
    @Test
    void storesItemsInPrivateMap() {
        var fields = Inventory.class.getDeclaredFields();

        assertTrue(Arrays.stream(fields)
                .allMatch(field -> Modifier.isPrivate(field.getModifiers())));
        assertTrue(Arrays.stream(fields)
                .anyMatch(field -> Map.class.isAssignableFrom(field.getType())));
    }

    @Test
    void addsItemsAndCombinesEqualKeys() {
        Inventory inventory = new Inventory();
        Item potion = item("Potion", ItemType.POTION, 50);

        inventory.addItem(potion, 2);
        inventory.addItem(new Item("Potion", ItemType.POTION, 50), 3);

        assertEquals(5, inventory.getQuantity(potion));
        assertEquals(1, inventory.getDistinctItemCount());
        assertThrows(IllegalArgumentException.class, () -> inventory.addItem(potion, 0));
    }

    @Test
    void removesItemsAndDeletesEmptyEntry() {
        Inventory inventory = new Inventory();
        Item potion = item("Potion", ItemType.POTION, 50);
        inventory.addItem(potion, 3);

        assertFalse(inventory.removeItem(potion, 4));
        assertTrue(inventory.removeItem(potion, 2));
        assertEquals(1, inventory.getQuantity(potion));
        assertTrue(inventory.removeItem(potion, 1));
        assertEquals(0, inventory.getQuantity(potion));
        assertEquals(0, inventory.getDistinctItemCount());
        assertThrows(IllegalArgumentException.class, () -> inventory.removeItem(potion, -1));
    }

    @Test
    void calculatesCountsAndTotalValue() {
        Inventory inventory = filledInventory();

        assertEquals(3, inventory.getDistinctItemCount());
        assertEquals(6, inventory.getTotalItemCount());
        assertEquals(330, inventory.getTotalValue());
    }

    @Test
    void calculatesValueByItemType() {
        Inventory inventory = filledInventory();

        assertEquals(150, inventory.getTotalValueByType(ItemType.POTION));
        assertEquals(120, inventory.getTotalValueByType(ItemType.WEAPON));
        assertEquals(0, inventory.getTotalValueByType(ItemType.ARMOR));
    }

    @Test
    void findsItemByNameIgnoringCaseAndOuterSpaces() {
        Inventory inventory = new Inventory();
        Item potion = item("Health Potion", ItemType.POTION, 50);
        inventory.addItem(potion, 1);

        assertSame(potion, inventory.findByName("  health POTION "));
        assertNull(inventory.findByName("Unknown"));
    }

    @Test
    void returnsZerosForEmptyInventory() {
        Inventory inventory = new Inventory();

        assertEquals(0, inventory.getDistinctItemCount());
        assertEquals(0, inventory.getTotalItemCount());
        assertEquals(0, inventory.getTotalValue());
    }

    private Inventory filledInventory() {
        Inventory inventory = new Inventory();
        inventory.addItem(item("Potion", ItemType.POTION, 50), 3);
        inventory.addItem(item("Sword", ItemType.WEAPON, 120), 1);
        inventory.addItem(item("Ore", ItemType.RESOURCE, 30), 2);
        return inventory;
    }

    private Item item(String name, ItemType type, int price) {
        return new Item(name, type, price);
    }
}
