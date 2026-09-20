package learning.task010;

public class Knight extends CombatUnit {

    private final int armor;

    public Knight(String name, int armor) {
        super(name, 110);
        if (armor < 0 || armor > 15) throw new IllegalArgumentException();
        this.armor = armor;
    }

    public int getArmor() {
        return armor;
    }

    @Override
    public int getAttackPower() {
        return 17;
    }

    @Override
    public String getUnitType() {
        return getClass().getSimpleName().toUpperCase();
    }
}
