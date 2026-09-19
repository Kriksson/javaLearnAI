package learning.task009;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleUnitTest {
    @Test
    void warriorInheritsBaseStateAndOverridesAttackPower() {
        GameUnit unit = new Warrior("  Tank  ", 5);

        assertInstanceOf(Warrior.class, unit);
        assertEquals("Tank", unit.getName());
        assertEquals(120, unit.getHealth());
        assertEquals(18, unit.getAttackPower());
        assertEquals("Tank [WARRIOR], HP: 120, armor: 5", unit.getDescription());
    }

    @Test
    void warriorReducesDamageByArmor() {
        Warrior warrior = new Warrior("Tank", 5);

        warrior.takeDamage(20);
        assertEquals(105, warrior.getHealth());

        warrior.takeDamage(3);
        assertEquals(105, warrior.getHealth());

        warrior.takeDamage(200);
        assertEquals(0, warrior.getHealth());
        assertFalse(warrior.isAlive());
    }

    @Test
    void warriorValidatesArmorAndNegativeDamage() {
        assertThrows(IllegalArgumentException.class, () -> new Warrior("Tank", -1));
        assertThrows(IllegalArgumentException.class, () -> new Warrior("Tank", 21));

        Warrior warrior = new Warrior("Tank", 5);
        assertThrows(IllegalArgumentException.class, () -> warrior.takeDamage(-1));
    }

    @Test
    void mageInheritsBaseStateAndCastsSpells() {
        Mage mage = new Mage("Merlin");
        GameUnit target = new GameUnit("Dummy", 100);

        assertEquals(80, mage.getHealth());
        assertEquals(30, mage.getMana());
        assertEquals(10, mage.getAttackPower());

        assertTrue(mage.castSpell(target));
        assertEquals(75, target.getHealth());
        assertEquals(20, mage.getMana());

        assertTrue(mage.castSpell(target));
        assertTrue(mage.castSpell(target));
        assertFalse(mage.castSpell(target));
        assertEquals(0, mage.getMana());
    }

    @Test
    void mageRestoresManaUpToMaximumAndValidatesAmount() {
        Mage mage = new Mage("Merlin");
        mage.castSpell(new GameUnit("Dummy", 100));

        mage.restoreMana(100);
        assertEquals(30, mage.getMana());
        assertThrows(IllegalArgumentException.class, () -> mage.restoreMana(0));
        assertThrows(IllegalArgumentException.class, () -> mage.restoreMana(-1));
    }

    @Test
    void mageBuildsDescriptionPolymorphically() {
        GameUnit unit = new Mage("Merlin");

        assertEquals("Merlin [MAGE], HP: 80, mana: 30", unit.getDescription());
    }

    @Test
    void childClassesKeepTheirOwnStatePrivate() {
        assertTrue(Arrays.stream(Warrior.class.getDeclaredFields())
                .allMatch(field -> Modifier.isPrivate(field.getModifiers())));
        assertTrue(Arrays.stream(Mage.class.getDeclaredFields())
                .allMatch(field -> Modifier.isPrivate(field.getModifiers())));
        assertEquals(1, Warrior.class.getDeclaredFields().length);
        assertEquals(1, Mage.class.getDeclaredFields().length);
    }
}
