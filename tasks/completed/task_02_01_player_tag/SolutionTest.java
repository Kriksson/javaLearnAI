package learning.topic02.task01;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SolutionTest {
    @Test
    void createsPlayerTag() {
        assertEquals("[MAGE] Kirill", Solution.createPlayerTag("Kirill", "mage"));
    }

    @Test
    void trimsSpacesAndSupportsCyrillic() {
        assertEquals("[ЛУЧНИК] Тень", Solution.createPlayerTag("  Тень  ", "  лучник "));
    }

    @Test
    void rejectsBlankNicknameOrClass() {
        assertEquals("Некорректные данные", Solution.createPlayerTag("   ", "warrior"));
        assertEquals("Некорректные данные", Solution.createPlayerTag("Kirill", "  "));
    }
}
