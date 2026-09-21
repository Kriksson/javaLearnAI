package learning.task014;

import java.util.Comparator;

public class ScoreEntry implements Comparable<ScoreEntry> {
    private final int score;
    private final String playerId;

    public ScoreEntry(String playerId, int score) {
        playerId = playerId.trim();
        if (playerId.isEmpty()) throw new IllegalArgumentException();
        if (score < 0) throw new IllegalArgumentException();
        this.score = score;
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public int getScore() {
        return score;
    }

    @Override
    public int compareTo(ScoreEntry other) {
        return Comparator.comparingInt(ScoreEntry::getScore)
                .reversed()
                .thenComparing(ScoreEntry::getPlayerId)
                .compare(this,other);
    }
}
