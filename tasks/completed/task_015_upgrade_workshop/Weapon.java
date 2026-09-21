package learning.task015;

public class Weapon implements Upgradeable {
   private final String name;
   private final int basePower;
   private int level;

    public Weapon(String name, int basePower) {
        name = name.trim();
        if (name.isEmpty()) throw new IllegalArgumentException();
        if (basePower < 1) throw new IllegalArgumentException();
        this.basePower = basePower;
        this.name = name;
        this.level = 0;
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
        if (level < 3) {
            level++;
            return true;
        }
        return false;
    }

    public int getPower() {
        return basePower + level * 5;
    }
}
