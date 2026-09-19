package learning.task009;

public class Warrior extends GameUnit {

    private final int armor;

    public Warrior(String name, int armor) {
        super(name, 120);
        if (armor < 0 || armor > 20) throw new IllegalArgumentException();
        this.armor = armor;
    }

    public int getArmor() {
        return armor;
    }

    @Override
    public int getAttackPower() {
        return 18;
    }

    @Override
    public void takeDamage(int damage) {
        if (damage < 0) throw new IllegalArgumentException();
        super.takeDamage(Math.max(0, damage-armor));
    }

    @Override
    public String getDescription() {
        return String.format("%s [%s], HP: %d, armor: %d", this.getName(), this.getClass().getSimpleName().toUpperCase(), this.getHealth(), this.getArmor());
    }
}
