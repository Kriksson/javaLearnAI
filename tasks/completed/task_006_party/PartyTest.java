package learning.task006;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartyTest {
    @Test
    void keepsStateInPrivateFieldsAndUsesList() {
        var fields = Party.class.getDeclaredFields();

        assertTrue(Arrays.stream(fields)
                .allMatch(field -> Modifier.isPrivate(field.getModifiers())));
        assertTrue(Arrays.stream(fields)
                .anyMatch(field -> List.class.isAssignableFrom(field.getType())));
    }

    @Test
    void trimsNameAndRejectsInvalidConstructorArguments() {
        assertEquals("Night Watch", new Party("  Night Watch  ", 3).getName());
        assertThrows(IllegalArgumentException.class, () -> new Party("   ", 3));
        assertThrows(IllegalArgumentException.class, () -> new Party("Raid", 0));
    }

    @Test
    void addsMembersUntilCapacityIsReached() {
        Party party = new Party("Raid", 2);

        assertTrue(party.addMember(character("Mage", 20)));
        assertTrue(party.addMember(character("Tank", 30)));
        assertFalse(party.addMember(character("Archer", 15)));
        assertEquals(2, party.getSize());
    }

    @Test
    void rejectsDuplicateNicknameIgnoringCase() {
        Party party = new Party("Raid", 3);

        assertTrue(party.addMember(character("Kirill", 20)));
        assertFalse(party.addMember(character("KIRILL", 40)));
        assertEquals(1, party.getSize());
    }

    @Test
    void findsMemberIgnoringCaseAndOuterSpaces() {
        Party party = new Party("Raid", 3);
        GameCharacter kirill = character("Kirill", 20);
        party.addMember(kirill);

        assertSame(kirill, party.findMember("  kIrIlL  "));
        assertNull(party.findMember("Unknown"));
    }

    @Test
    void returnsStrongestMemberAndKeepsFirstOnTie() {
        Party party = new Party("Raid", 3);
        GameCharacter first = character("Mage", 40);
        GameCharacter second = character("Tank", 40);
        party.addMember(character("Archer", 15));
        party.addMember(first);
        party.addMember(second);

        assertSame(first, party.getStrongestMember());
    }

    @Test
    void calculatesAverageAndHandlesEmptyParty() {
        Party empty = new Party("Empty", 2);
        assertEquals(0.0, empty.getAverageLevel());
        assertNull(empty.getStrongestMember());

        Party party = new Party("Raid", 3);
        party.addMember(character("Mage", 20));
        party.addMember(character("Tank", 35));

        assertEquals(27.5, party.getAverageLevel());
    }

    private GameCharacter character(String nickname, int level) {
        return new GameCharacter(nickname, "Adventurer", level);
    }
}
