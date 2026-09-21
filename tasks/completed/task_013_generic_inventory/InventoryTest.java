package learning.task013;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {
    @Test
    void storesAndReturnsValuesOfSpecifiedType() {
        Inventory<String> inventory = new Inventory<>();

        inventory.add("health potion");
        inventory.add("mana potion");

        assertEquals(2, inventory.size());
        assertEquals("health potion", inventory.get(0));
        assertEquals("mana potion", inventory.get(1));
    }

    @Test
    void worksWithCustomObjectsToo() {
        Inventory<Loot> inventory = new Inventory<>();
        Loot sword = new Loot("sword");

        inventory.add(sword);

        assertTrue(inventory.contains(sword));
        assertSame(sword, inventory.get(0));
    }

    @Test
    void removeDeletesFirstMatchingValue() {
        Inventory<String> inventory = new Inventory<>();
        inventory.add("key");
        inventory.add("map");

        assertTrue(inventory.remove("key"));
        assertEquals(1, inventory.size());
        assertEquals("map", inventory.get(0));
        assertFalse(inventory.remove("key"));
    }

    @Test
    void rejectsNullValues() {
        Inventory<String> inventory = new Inventory<>();

        assertThrows(IllegalArgumentException.class, () -> inventory.add(null));
        assertThrows(IllegalArgumentException.class, () -> inventory.contains(null));
    }

    @Test
    void getUsesListIndexValidation() {
        Inventory<String> inventory = new Inventory<>();
        inventory.add("key");

        assertThrows(IndexOutOfBoundsException.class, () -> inventory.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> inventory.get(1));
    }

    @Test
    void usesPrivateGenericListField() {
        Field listField = null;
        for (Field field : Inventory.class.getDeclaredFields()) {
            if (List.class.isAssignableFrom(field.getType())) {
                listField = field;
                break;
            }
        }

        assertNotNull(listField, "Нужно поле типа List<T>");
        assertTrue(Modifier.isPrivate(listField.getModifiers()));
        assertEquals("java.util.List<T>", listField.getGenericType().getTypeName());
    }

    private record Loot(String name) {
    }
}
