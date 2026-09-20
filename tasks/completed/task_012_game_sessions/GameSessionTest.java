package learning.task012;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {
    @Test
    void gameModesHaveExpectedLimits() {
        assertEquals(1, GameMode.SOLO.getMaxPlayers());
        assertEquals(2, GameMode.DUO.getMaxPlayers());
        assertEquals(4, GameMode.SQUAD.getMaxPlayers());
    }

    @Test
    void sessionTrimsTitleAndKeepsMode() {
        GameSession session = new GameSession("  Training  ", GameMode.DUO);

        assertEquals("Training", session.getTitle());
        assertEquals(GameMode.DUO, session.getMode());
    }

    @Test
    void sessionRejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new GameSession("   ", GameMode.SOLO));
        assertThrows(IllegalArgumentException.class, () -> new GameSession("x".repeat(21), GameMode.SOLO));
        assertThrows(IllegalArgumentException.class, () -> new GameSession("Training", null));
    }

    @Test
    void canJoinDependsOnModeLimit() {
        GameSession squad = new GameSession("Raid", GameMode.SQUAD);

        assertTrue(squad.canJoin(0));
        assertTrue(squad.canJoin(3));
        assertFalse(squad.canJoin(4));
        assertThrows(IllegalArgumentException.class, () -> squad.canJoin(-1));
    }

    @Test
    void idsAndCreatedCounterAreSharedByClass() {
        int countBefore = GameSession.getCreatedCount();
        GameSession first = new GameSession("Alpha", GameMode.SOLO);
        GameSession second = new GameSession("Beta", GameMode.DUO);

        assertEquals(first.getId() + 1, second.getId());
        assertEquals(countBefore + 2, GameSession.getCreatedCount());
    }

    @Test
    void maxTitleLengthIsPublicStaticFinalConstant() throws NoSuchFieldException {
        Field field = GameSession.class.getDeclaredField("MAX_TITLE_LENGTH");

        assertEquals(20, GameSession.MAX_TITLE_LENGTH);
        assertTrue(Modifier.isPublic(field.getModifiers()));
        assertTrue(Modifier.isStatic(field.getModifiers()));
        assertTrue(Modifier.isFinal(field.getModifiers()));
    }
}
