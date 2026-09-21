package learning.task015;

public class Armor implements Upgradeable {
    private final String name;
    private final int baseDefense;
    private int level;

    public Armor(String name, int baseDefense) {
        name = name.trim();
        if (name.isEmpty()) throw new IllegalArgumentException();
        if (baseDefense < 0) throw new IllegalArgumentException();
        this.baseDefense = baseDefense;
        this.level = 0;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public boolean upgrade() {
        if (level < 2) {
            level++;
            return true;
        }
        return false;
    }

    public int getDefense() {
        return baseDefense + level * 3;
    }
}
