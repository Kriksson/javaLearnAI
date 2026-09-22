package learning.task030;

public record PlayerProfile(String id, String nickname, int level) {

    public PlayerProfile(String id, String nickname, int level) {
        if (nickname == null || nickname.trim().isEmpty() || id == null || id.trim().isEmpty() || level < 1 ||
                level > 100 ) throw new IllegalArgumentException();
        id = id.trim();
        nickname = nickname.trim();
        this.id = id;
        this.nickname = nickname;
        this.level = level;
    }
}
