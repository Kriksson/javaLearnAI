package learning.task010;

public abstract class CombatUnit {
    private final String name;
    private int health;

    protected CombatUnit(String name, int health) {
        name = name.trim();
        if (name.isEmpty()) throw new IllegalArgumentException();
        this.name = name;
        if (health < 1) throw new IllegalArgumentException();
        this.health = health;
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void takeDamage(int damage) {
        if (damage < 0) throw new IllegalArgumentException();
        health = Math.max(0, health - damage);
    }

    public abstract int getAttackPower();

    public abstract String getUnitType();

    public String getDescription() {
        return String.format("%s [%s], HP: %d", getName(), getUnitType(), health);
    }
}
