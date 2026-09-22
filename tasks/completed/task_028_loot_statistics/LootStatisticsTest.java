package learning.task028;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LootStatisticsTest {
    @Test
    void accumulatesDropsAndFindsMostCommonItems() {
        LootStatistics statistics = new LootStatistics();
        statistics.register(new LootDrop(" gold ", 5));
        statistics.register(new LootDrop("potion", 3));
        statistics.register(new LootDrop("gold", 2));
        statistics.register(new LootDrop("arrow", 7));

        assertEquals(7, statistics.totalFor(" gold "));
        assertEquals(0, statistics.totalFor("sword"));
        assertEquals(List.of("arrow"), statistics.mostCommon(1));
        assertEquals(List.of("arrow", "gold", "potion"), statistics.mostCommon(3));
    }

    @Test
    void ordersTiesByNameAndReturnsImmutableSnapshots() {
        LootStatistics statistics = new LootStatistics();
        statistics.register(new LootDrop("zombie tooth", 4));
        statistics.register(new LootDrop("apple", 4));

        assertEquals(List.of("apple", "zombie tooth"), statistics.mostCommon(10));
        Map<String, Integer> totals = statistics.totals();
        assertEquals(Map.of("zombie tooth", 4, "apple", 4), totals);
        assertThrows(UnsupportedOperationException.class, () -> totals.put("gold", 1));
        assertThrows(UnsupportedOperationException.class, () -> statistics.mostCommon(0).add("gold"));
    }

    @Test
    void validatesArguments() {
        LootStatistics statistics = new LootStatistics();

        assertThrows(IllegalArgumentException.class, () -> new LootDrop(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new LootDrop(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> new LootDrop("gold", 0));
        assertThrows(IllegalArgumentException.class, () -> statistics.register(null));
        assertThrows(IllegalArgumentException.class, () -> statistics.totalFor(" "));
        assertThrows(IllegalArgumentException.class, () -> statistics.mostCommon(-1));
    }
}
