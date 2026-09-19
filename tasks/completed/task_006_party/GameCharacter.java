package learning.task006;

public class GameCharacter {
    private final String nickname;
    private final String characterClass;
    private int level;

    public GameCharacter(String nickname, String characterClass, int level) {
        nickname = nickname.trim();
        characterClass = characterClass.trim();
        if (nickname.isEmpty() || characterClass.isEmpty() || level < 1 || level > 100) {
            throw new IllegalArgumentException("Некорректные данные");
        }
        this.nickname = nickname;
        this.characterClass = characterClass;
        this.level = level;
    }

    public String getNickname() {
        return nickname;
    }

    public String getCharacterClass() {
        return characterClass;
    }

    public int getLevel() {
        return level;
    }

    public void levelUp() {
        if (level < 100) {
            level++;
        }
    }

    public String getRank() {
        if (level <= 9) return "NOVICE";
        if (level <= 24) return "VETERAN";
        if (level <= 49) return "ELITE";
        return "LEGEND";
    }

    public String getProfile() {
        return String.format("[%s] %s — %s, уровень %d",
                getRank(), nickname, characterClass, level);
    }
}
