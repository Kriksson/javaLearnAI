package learning.task008;

import java.util.Objects;

public class ScoreEntry {
    private final String playerName;
    private final int score;

    public ScoreEntry(String playerName, int score) {
        playerName = playerName.trim();
        if (playerName.isEmpty()
                || playerName.contains(";")
                || playerName.contains("\n")
                || playerName.contains("\r")
                || score < 0) {
            throw new IllegalArgumentException("Некорректный результат игрока");
        }
        this.playerName = playerName;
        this.score = score;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ScoreEntry entry)) return false;
        return score == entry.score && playerName.equals(entry.playerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerName, score);
    }
}
