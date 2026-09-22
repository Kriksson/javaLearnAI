package learning.task034;

public record DamageEvent(String playerId, int damage) {
    public DamageEvent(String playerId, int damage) {
        if (playerId == null || playerId.trim().isEmpty() || damage < 0) throw new IllegalArgumentException();
        playerId = playerId.trim();
        this.playerId = playerId;
        this.damage = damage;
    }
}
