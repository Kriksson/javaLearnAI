package learning.task010;

public class Archer extends CombatUnit {
    private int countOfArrows;

    public Archer(String name, int arrows) {
        super(name, 70);
        if (arrows < 0 || arrows > 10) throw new IllegalArgumentException();
        this.countOfArrows = arrows;
    }

    public int getArrows() {
        return countOfArrows;
    }

    public boolean shoot(CombatUnit target) {
        if (countOfArrows > 0) {
            target.takeDamage(12);
            countOfArrows--;
            return true;
        }
        return false;
    }

    @Override
    public int getAttackPower() {
        if (countOfArrows > 0) return 12;
        return 3;
    }

    @Override
    public String getUnitType() {
        return getClass().getSimpleName().toUpperCase();
    }
}
