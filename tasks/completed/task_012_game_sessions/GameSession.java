package learning.task012;

public class GameSession {
    public static final int MAX_TITLE_LENGTH = 20;

    private static int createdCount;

    private final String title;
    private final GameMode gameMode;
    private final int id;


    public GameSession(String title, GameMode mode) {
        title = title.trim();
        if (title.length() > MAX_TITLE_LENGTH || title.isEmpty()) throw new IllegalArgumentException();
        if (mode == null) throw new IllegalArgumentException();
        this.id = ++createdCount;
        this.gameMode = mode;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public GameMode getMode() {
        return gameMode;
    }

    public boolean canJoin(int currentPlayers) {
        if (currentPlayers < 0) throw new IllegalArgumentException();
        return gameMode.getMaxPlayers() - currentPlayers >= 1;
    }

    public static int getCreatedCount() {
        return createdCount;
    }
}
