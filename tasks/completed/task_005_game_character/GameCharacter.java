package learning.task005;

public class GameCharacter {
    private final String nickname;
    private final String characterClass;
    private int level = 0;

    public GameCharacter(String nickname, String characterClass, int level) {
        nickname = nickname.trim();
        characterClass = characterClass.trim();
        if (nickname.trim().isEmpty()) throw new IllegalArgumentException("Некорректные данные");
        if (characterClass.trim().isEmpty()) throw new IllegalArgumentException("Некорректные данные");
        if (!(level >= 1 && level <= 100)) throw new IllegalArgumentException("Некорректные данные");
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
        if (this.level < 100) {
            this.level++;
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
                this.getRank(), this.getNickname(), this.getCharacterClass(), this.getLevel());
    }
}
