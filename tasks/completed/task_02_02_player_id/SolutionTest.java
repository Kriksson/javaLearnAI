package learning.topic02.task02;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SolutionTest {
    @Test
    void createsThreeCharacterGameCode() {
        assertEquals("MIN", Solution.createGameCode("  Minecraft  "));
    }

    @Test
    void keepsShortGameCode() {
        assertEquals("CS", Solution.createGameCode(" CS "));
        assertEquals("", Solution.createGameCode("   "));
    }

    @Test
    void createsPlayerIdAndNormalizesStrings() {
        assertEquals("MIN-SHADOW_PLAYER", Solution.createPlayerId("Shadow Player", "Minecraft"));
        assertEquals("DOT-ТЁМНЫЙ_РЫЦАРЬ", Solution.createPlayerId("  Тёмный Рыцарь  ", "Dota 2"));
    }

    @Test
    void rejectsBlankNicknameOrGameTitle() {
        assertEquals("Некорректные данные", Solution.createPlayerId("   ", "Minecraft"));
        assertEquals("Некорректные данные", Solution.createPlayerId("Kirill", "   "));
    }
}
