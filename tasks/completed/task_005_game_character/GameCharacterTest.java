package learning.task005;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameCharacterTest {
    @Test
    void declaresStateInPrivateFields() {
        var fields = GameCharacter.class.getDeclaredFields();

        assertEquals(3, fields.length);
        assertTrue(Arrays.stream(fields)
                .allMatch(field -> Modifier.isPrivate(field.getModifiers())));
    }

    @Test
    void trimsAndStoresConstructorValues() {
        GameCharacter character = new GameCharacter("  Kirill  ", " Mage ", 9);

        assertEquals("Kirill", character.getNickname());
        assertEquals("Mage", character.getCharacterClass());
        assertEquals(9, character.getLevel());
    }

    @Test
    void rejectsBlankNicknameOrCharacterClass() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameCharacter("   ", "Mage", 10));
        assertThrows(IllegalArgumentException.class,
                () -> new GameCharacter("Kirill", "   ", 10));
    }

    @Test
    void rejectsLevelOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameCharacter("Kirill", "Mage", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new GameCharacter("Kirill", "Mage", 101));
    }

    @Test
    void increasesLevelAndStopsAtMaximum() {
        GameCharacter character = new GameCharacter("Tank", "Warrior", 99);

        character.levelUp();
        assertEquals(100, character.getLevel());

        character.levelUp();
        assertEquals(100, character.getLevel());
    }

    @Test
    void returnsRankAtEveryBoundary() {
        assertEquals("NOVICE", characterAt(1).getRank());
        assertEquals("NOVICE", characterAt(9).getRank());
        assertEquals("VETERAN", characterAt(10).getRank());
        assertEquals("VETERAN", characterAt(24).getRank());
        assertEquals("ELITE", characterAt(25).getRank());
        assertEquals("ELITE", characterAt(49).getRank());
        assertEquals("LEGEND", characterAt(50).getRank());
        assertEquals("LEGEND", characterAt(100).getRank());
    }

    @Test
    void createsProfileAndUpdatesRankAfterLevelUp() {
        GameCharacter character = new GameCharacter("Kirill", "Mage", 9);

        character.levelUp();

        assertEquals("[VETERAN] Kirill — Mage, уровень 10", character.getProfile());
    }

    private GameCharacter characterAt(int level) {
        return new GameCharacter("Hero", "Warrior", level);
    }
}
