package learning.task030;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlayerDirectoryTest {
    @Test
    void findsPlayerAndReplacesProfileWithSameId() {
        PlayerDirectory directory = new PlayerDirectory();
        directory.register(new PlayerProfile(" p-1 ", " Mira ", 20));
        directory.register(new PlayerProfile("p-1", "MiraPro", 25));

        assertEquals(1, directory.size());
        assertEquals(Optional.of(new PlayerProfile("p-1", "MiraPro", 25)),
                directory.findById(" p-1 "));
        assertFalse(directory.findById("missing").isPresent());
    }

    @Test
    void filtersSortsAndProtectsResult() {
        PlayerDirectory directory = new PlayerDirectory();
        PlayerProfile mira = new PlayerProfile("p-1", "Mira", 20);
        PlayerProfile alex = new PlayerProfile("p-2", "Alex", 30);
        PlayerProfile aria = new PlayerProfile("p-3", "Aria", 30);
        directory.register(mira);
        directory.register(alex);
        directory.register(aria);

        List<PlayerProfile> selected = directory.withMinimumLevel(20);

        assertEquals(List.of(alex, aria, mira), selected);
        assertThrows(UnsupportedOperationException.class, () -> selected.add(mira));
    }

    @Test
    void validatesInput() {
        PlayerDirectory directory = new PlayerDirectory();

        assertThrows(IllegalArgumentException.class, () -> new PlayerProfile(null, "Mira", 1));
        assertThrows(IllegalArgumentException.class, () -> new PlayerProfile("p-1", " ", 1));
        assertThrows(IllegalArgumentException.class, () -> new PlayerProfile("p-1", "Mira", 0));
        assertThrows(IllegalArgumentException.class, () -> new PlayerProfile("p-1", "Mira", 101));
        assertThrows(IllegalArgumentException.class, () -> directory.register(null));
        assertThrows(IllegalArgumentException.class, () -> directory.findById(" "));
        assertThrows(IllegalArgumentException.class, () -> directory.withMinimumLevel(0));
        assertThrows(IllegalArgumentException.class, () -> directory.withMinimumLevel(101));
    }
}
