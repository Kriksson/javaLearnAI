package learning.task027;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class BattleHistoryTest {
    @Test
    void topSortsByScoreThenDurationAndName() {
        BattleHistory history = new BattleHistory();
        history.add(new MatchResult("Mira", 120, 80));
        history.add(new MatchResult("Zed", 100, 40));
        history.add(new MatchResult("Alex", 120, 75));
        history.add(new MatchResult("Aria", 120, 75));

        assertEquals(List.of("Alex", "Aria", "Mira"),
                history.top(3).stream().map(MatchResult::playerName).toList());
    }

    @Test
    void topDoesNotChangeInsertionOrderAndResultCannotBeModified() {
        BattleHistory history = new BattleHistory();
        MatchResult first = new MatchResult("Mira", 100, 50);
        MatchResult second = new MatchResult("Alex", 200, 60);
        history.add(first);
        history.add(second);

        List<MatchResult> top = history.top(1);

        assertEquals(List.of(second), top);
        assertThrows(UnsupportedOperationException.class, () -> top.add(first));
        assertEquals(List.of(first, second), history.all());
        assertThrows(UnsupportedOperationException.class, () -> history.all().add(first));
    }

    @Test
    void validatesLimitsAndResultFields() {
        BattleHistory history = new BattleHistory();

        assertThrows(IllegalArgumentException.class, () -> history.top(-1));
        assertEquals(List.of(), history.top(0));
        assertThrows(IllegalArgumentException.class, () -> new MatchResult(null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new MatchResult(" ", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new MatchResult("Mira", -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new MatchResult("Mira", 1, 0));
    }
}
