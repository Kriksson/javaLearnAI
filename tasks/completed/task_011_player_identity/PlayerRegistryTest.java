package learning.task011;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class PlayerRegistryTest {
    @Test
    void playerTrimsValuesAndProvidesReadableText() {
        Player player = new Player(" p-42 ", " Kirill ");

        assertEquals("p-42", player.getId());
        assertEquals("Kirill", player.getDisplayName());
        assertEquals("p-42 (Kirill)", player.toString());
    }

    @Test
    void playerRejectsBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> new Player("  ", "Kirill"));
        assertThrows(IllegalArgumentException.class, () -> new Player("p-42", "\t"));
    }

    @Test
    void equalPlayersHaveSameIdAndHashCode() {
        Player first = new Player("p-42", "Kirill");
        Player loadedAgain = new Player("p-42", "Kriksson");
        Player another = new Player("p-43", "Kirill");

        assertEquals(first, first);
        assertEquals(first, loadedAgain);
        assertEquals(loadedAgain, first);
        assertEquals(first.hashCode(), loadedAgain.hashCode());
        assertNotEquals(first, another);
        assertNotEquals(first, null);
        assertNotEquals(first, "p-42");
    }

    @Test
    void playerFieldsArePrivateAndFinal() throws NoSuchFieldException {
        Field id = Player.class.getDeclaredField("id");
        Field displayName = Player.class.getDeclaredField("displayName");

        assertTrue(Modifier.isPrivate(id.getModifiers()));
        assertTrue(Modifier.isFinal(id.getModifiers()));
        assertTrue(Modifier.isPrivate(displayName.getModifiers()));
        assertTrue(Modifier.isFinal(displayName.getModifiers()));
    }

    @Test
    void registryKeepsOnlyUniquePlayerIds() {
        PlayerRegistry registry = new PlayerRegistry();
        Player first = new Player("p-42", "Kirill");
        Player duplicate = new Player("p-42", "Kriksson");
        Player another = new Player("p-43", "Lia");

        assertTrue(registry.register(first));
        assertFalse(registry.register(duplicate));
        assertTrue(registry.register(another));
        assertEquals(2, registry.size());
        assertTrue(registry.contains(new Player("p-42", "Loaded")));
        assertFalse(registry.contains(new Player("p-44", "New")));
    }

    @Test
    void registryRejectsNull() {
        PlayerRegistry registry = new PlayerRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.register(null));
        assertThrows(IllegalArgumentException.class, () -> registry.contains(null));
    }
}
