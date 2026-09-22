package learning.task027;

public record MatchResult(String playerName, int score, int durationSeconds) {
    public MatchResult(String playerName, int score, int durationSeconds) {
        if (playerName == null || playerName.trim().isEmpty()) throw new IllegalArgumentException();
        playerName = playerName.trim();
        if (score < 0 || durationSeconds < 1) throw new IllegalArgumentException();
        this.playerName = playerName;
        this.score = score;
        this.durationSeconds = durationSeconds;
    }
}
