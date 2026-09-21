package learning.task015;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkshopTest {
    @Test
    void weaponImplementsInterfaceAndIncreasesPower() {
        Upgradeable item = new Weapon("  Iron sword  ", 10);

        assertEquals("Iron sword", item.getName());
        assertEquals(0, item.getLevel());
        assertTrue(item.upgrade());
        assertEquals(1, item.getLevel());
        assertEquals(15, ((Weapon) item).getPower());
    }

    @Test
    void weaponStopsAtMaximumLevel() {
        Weapon weapon = new Weapon("Sword", 10);

        assertTrue(weapon.upgrade());
        assertTrue(weapon.upgrade());
        assertTrue(weapon.upgrade());
        assertFalse(weapon.upgrade());
        assertEquals(3, weapon.getLevel());
        assertEquals(25, weapon.getPower());
    }

    @Test
    void armorUsesItsOwnLimitAndDefenseIncrease() {
        Armor armor = new Armor("Leather", 4);

        assertTrue(armor.upgrade());
        assertTrue(armor.upgrade());
        assertFalse(armor.upgrade());
        assertEquals(2, armor.getLevel());
        assertEquals(10, armor.getDefense());
    }

    @Test
    void constructorsValidateArguments() {
        assertThrows(IllegalArgumentException.class, () -> new Weapon(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> new Weapon("Sword", 0));
        assertThrows(IllegalArgumentException.class, () -> new Armor(" ", 0));
        assertThrows(IllegalArgumentException.class, () -> new Armor("Leather", -1));
    }

    @Test
    void workshopUpgradesDifferentImplementationsPolymorphically() {
        Workshop workshop = new Workshop();
        Weapon weapon = new Weapon("Sword", 10);
        Armor armor = new Armor("Leather", 4);
        workshop.add(weapon);
        workshop.add(armor);

        assertEquals(2, workshop.size());
        assertEquals(2, workshop.upgradeAll());
        assertEquals(1, weapon.getLevel());
        assertEquals(1, armor.getLevel());

        armor.upgrade();
        assertEquals(1, workshop.upgradeAll());
        assertEquals(2, weapon.getLevel());
        assertEquals(2, armor.getLevel());
    }

    @Test
    void workshopRejectsNull() {
        Workshop workshop = new Workshop();

        assertThrows(IllegalArgumentException.class, () -> workshop.add(null));
    }
}
