package learning.task014;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeaderboardTest {
    @Test
    void entryTrimsIdAndValidatesArguments() {
        ScoreEntry entry = new ScoreEntry("  mira  ", 120);

        assertEquals("mira", entry.getPlayerId());
        assertEquals(120, entry.getScore());
        assertThrows(IllegalArgumentException.class, () -> new ScoreEntry("  ", 1));
        assertThrows(IllegalArgumentException.class, () -> new ScoreEntry("mira", -1));
    }

    @Test
    void compareToSortsByScoreDescending() {
        ScoreEntry higher = new ScoreEntry("mira", 150);
        ScoreEntry lower = new ScoreEntry("alex", 120);

        assertTrue(higher.compareTo(lower) < 0);
        assertTrue(lower.compareTo(higher) > 0);
    }

    @Test
    void equalScoresAreSortedByPlayerId() {
        ScoreEntry alex = new ScoreEntry("alex", 120);
        ScoreEntry boris = new ScoreEntry("boris", 120);
        ScoreEntry same = new ScoreEntry("alex", 120);

        assertTrue(alex.compareTo(boris) < 0);
        assertTrue(boris.compareTo(alex) > 0);
        assertEquals(0, alex.compareTo(same));
    }

    @Test
    void topReturnsRequestedBestEntries() {
        Leaderboard board = new Leaderboard();
        ScoreEntry mira = new ScoreEntry("mira", 120);
        ScoreEntry alex = new ScoreEntry("alex", 150);
        ScoreEntry boris = new ScoreEntry("boris", 120);
        board.add(mira);
        board.add(alex);
        board.add(boris);

        List<ScoreEntry> top = board.top(2);

        assertEquals(List.of(alex, boris), top);
        assertEquals(3, board.size());
    }

    @Test
    void topDoesNotChangeStoredInsertionOrder() throws IllegalAccessException {
        Leaderboard board = new Leaderboard();
        ScoreEntry first = new ScoreEntry("mira", 120);
        ScoreEntry second = new ScoreEntry("alex", 150);
        board.add(first);
        board.add(second);

        board.top(1);

        Field entriesField = Arrays.stream(Leaderboard.class.getDeclaredFields())
                .filter(field -> List.class.isAssignableFrom(field.getType()))
                .findFirst()
                .orElseThrow();
        entriesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ScoreEntry> storedEntries = (List<ScoreEntry>) entriesField.get(board);

        assertEquals(List.of(first, second), storedEntries);
    }

    @Test
    void validatesLeaderboardArguments() {
        Leaderboard board = new Leaderboard();

        assertThrows(IllegalArgumentException.class, () -> board.add(null));
        assertThrows(IllegalArgumentException.class, () -> board.top(-1));
        assertEquals(List.of(), board.top(0));
    }
}
