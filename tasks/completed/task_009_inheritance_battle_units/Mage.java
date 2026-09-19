package learning.task009;

public class Mage extends GameUnit {

    private int mana;

    public Mage(String name) {
        super(name, 80);
        this.mana = 30;
    }

    public int getMana() {
        return mana;
    }

    @Override
    public int getAttackPower() {
        return 10;
    }

    public boolean castSpell(GameUnit target) {
        if (mana >= 10) {
            mana -= 10;
            target.takeDamage(25);
            return true;
        }
        return false;
    }

    public void restoreMana(int amount) {
        if (amount <= 0) throw new IllegalArgumentException();
        if (30 - mana < amount) {
            mana = 30;
        } else {
            mana += amount;
        }
    }

    @Override
    public String getDescription() {
        return String.format("%s [%s], HP: %d, mana: %d", this.getName(), this.getClass().getSimpleName().toUpperCase(), this.getHealth(), mana);
    }
}
