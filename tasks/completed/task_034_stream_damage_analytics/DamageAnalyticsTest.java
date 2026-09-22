package learning.task034;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DamageAnalyticsTest {
    @Test
    void groupsPositiveDamageAndIgnoresNullAndZeroEvents() {
        DamageAnalytics analytics = new DamageAnalytics();
        List<DamageEvent> events = Arrays.asList(
                new DamageEvent(" Mira ", 10),
                null,
                new DamageEvent("Alex", 15),
                new DamageEvent("Mira", 5),
                new DamageEvent("Zed", 0));

        Map<String, Integer> totals = analytics.totalDamageByPlayer(events);
        assertEquals(Map.of("Mira", 15, "Alex", 15), totals);
        assertThrows(UnsupportedOperationException.class, () -> totals.clear());
    }

    @Test
    void ranksPlayersByTotalDamageThenId() {
        DamageAnalytics analytics = new DamageAnalytics();
        List<DamageEvent> events = List.of(
                new DamageEvent("Mira", 10),
                new DamageEvent("Alex", 15),
                new DamageEvent("Mira", 5),
                new DamageEvent("Zed", 7));

        assertEquals(List.of("Alex", "Mira"), analytics.topPlayers(events, 2));
        assertEquals(List.of(), analytics.topPlayers(events, 0));
        assertThrows(UnsupportedOperationException.class, () -> analytics.topPlayers(events, 1).add("Zed"));
    }

    @Test
    void validatesArguments() {
        DamageAnalytics analytics = new DamageAnalytics();

        assertThrows(IllegalArgumentException.class, () -> new DamageEvent(null, 0));
        assertThrows(IllegalArgumentException.class, () -> new DamageEvent("Mira", -1));
        assertThrows(IllegalArgumentException.class, () -> analytics.totalDamageByPlayer(null));
        assertThrows(IllegalArgumentException.class, () -> analytics.topPlayers(null, 1));
        assertThrows(IllegalArgumentException.class, () -> analytics.topPlayers(List.of(), -1));
    }
}
