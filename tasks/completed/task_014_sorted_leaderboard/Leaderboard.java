package learning.task014;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Leaderboard {

    private final List<ScoreEntry> leaderboard = new ArrayList<>();

    public Leaderboard() {
    }

    public void add(ScoreEntry entry) {
        if (entry == null) throw new IllegalArgumentException();
        leaderboard.add(entry);
    }

    public int size() {
        return leaderboard.size();
    }

    public List<ScoreEntry> top(int count) {
        if (count < 0) throw new IllegalArgumentException();
        if (count == 0) return new ArrayList<>();
        List<ScoreEntry> sorted = new ArrayList<>(leaderboard);
        Collections.sort(sorted);
        int limit = Math.min(count, size());
        return new ArrayList<>(sorted.subList(0, limit));
    }
}
