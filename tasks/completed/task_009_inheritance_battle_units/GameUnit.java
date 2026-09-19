package learning.task009;

public class GameUnit {
    private final String name;
    private int health;

    public GameUnit(String name, int health) {
        name = name.trim();
        if (name.isEmpty() || health <= 0) {
            throw new IllegalArgumentException("Некорректные данные персонажа");
        }
        this.name = name;
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
        if (damage < 0) {
            throw new IllegalArgumentException("Урон не может быть отрицательным");
        }
        health = Math.max(0, health - damage);
    }

    public int getAttackPower() {
        return 5;
    }

    public String getDescription() {
        return name + " [UNIT], HP: " + health;
    }
}
