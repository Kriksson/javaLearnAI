package learning.task032;

public record QueueTicket(String playerId, int rating) {
    public QueueTicket(String playerId, int rating) {
        if (playerId == null || playerId.trim().isEmpty() || rating < 0 || rating > 5000) throw new IllegalArgumentException();
        playerId = playerId.trim();
        this.playerId = playerId;
        this.rating = rating;
    }
}
