package learning.task010;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class BattleSquadTest {
    @Test
    void combatUnitIsAbstractAndKeepsCommonState() {
        assertTrue(Modifier.isAbstract(CombatUnit.class.getModifiers()));

        CombatUnit knight = new Knight("  Bronn  ", 6);

        assertEquals("Bronn", knight.getName());
        assertEquals(110, knight.getHealth());
        assertTrue(knight.isAlive());
        assertEquals("Bronn [KNIGHT], HP: 110", knight.getDescription());
    }

    @Test
    void commonValidationAndDamageAreHandled() {
        assertThrows(IllegalArgumentException.class, () -> new Knight("   ", 0));
        assertThrows(IllegalArgumentException.class, () -> new Archer("Lia", -1));

        CombatUnit knight = new Knight("Bronn", 0);
        knight.takeDamage(200);

        assertEquals(0, knight.getHealth());
        assertFalse(knight.isAlive());
        assertThrows(IllegalArgumentException.class, () -> knight.takeDamage(-1));
    }

    @Test
    void knightHasFixedAttackAndValidatesArmor() {
        Knight knight = new Knight("Bronn", 15);

        assertEquals(15, knight.getArmor());
        assertEquals(17, knight.getAttackPower());
        assertThrows(IllegalArgumentException.class, () -> new Knight("Bronn", -1));
        assertThrows(IllegalArgumentException.class, () -> new Knight("Bronn", 16));
    }

    @Test
    void archerShootsAndChangesAttackAfterArrowsRunOut() {
        Archer archer = new Archer("Lia", 1);
        CombatUnit target = new Knight("Bronn", 0);

        assertEquals(12, archer.getAttackPower());
        assertTrue(archer.shoot(target));
        assertEquals(0, archer.getArrows());
        assertEquals(98, target.getHealth());
        assertEquals(3, archer.getAttackPower());
        assertFalse(archer.shoot(target));
        assertEquals(98, target.getHealth());
        assertEquals("Lia [ARCHER], HP: 70", archer.getDescription());
    }

    @Test
    void squadUsesPolymorphicAttackOfAliveUnits() {
        BattleSquad squad = new BattleSquad();
        CombatUnit knight = new Knight("Bronn", 3);
        Archer archer = new Archer("Lia", 1);

        squad.add(knight);
        squad.add(archer);
        assertEquals(2, squad.size());
        assertEquals(29, squad.getAliveAttackPower());

        archer.shoot(knight);
        assertEquals(20, squad.getAliveAttackPower());

        knight.takeDamage(200);
        assertEquals(3, squad.getAliveAttackPower());
    }

    @Test
    void squadRejectsNullUnit() {
        BattleSquad squad = new BattleSquad();

        assertThrows(IllegalArgumentException.class, () -> squad.add(null));
    }
}
