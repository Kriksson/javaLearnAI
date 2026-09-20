package learning.task010;

import java.util.ArrayList;
import java.util.List;

public class BattleSquad {

    private final List<CombatUnit> squad =  new ArrayList<>();

    public void add(CombatUnit unit) {
        if (unit == null) throw new IllegalArgumentException();
        squad.add(unit);
    }

    public int size() {
        return squad.size();
    }

    public int getAliveAttackPower() {
        int totalDamage = 0;
        for (CombatUnit unit : squad) {
            if (unit.isAlive()){
                totalDamage += unit.getAttackPower();
            }
        }
        return totalDamage;
    }
}
