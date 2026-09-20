package learning.task012;

public enum GameMode {
    SOLO(1),
    DUO(2),
    SQUAD(4);

    private final int maxPlayers;

    GameMode(int i) {
        this.maxPlayers = i;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
}
