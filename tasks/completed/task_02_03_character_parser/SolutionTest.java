package learning.topic02.task03;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SolutionTest {
    @Test
    void normalizesToken() {
        assertEquals("DARK_MAGE", Solution.normalizeToken("  Dark Mage  "));
    }

    @Test
    void createsCharacterCard() {
        assertEquals(
                "[MAGE] KIRILL (уровень 15)",
                Solution.createCharacterCard("Kirill|mage|15")
        );
    }

    @Test
    void trimsAndNormalizesCyrillicFields() {
        assertEquals(
                "[МАГ_ОГНЯ] ТЁМНЫЙ_РЫЦАРЬ (уровень 7)",
                Solution.createCharacterCard("  Тёмный рыцарь | маг огня | 7 ")
        );
    }

    @Test
    void rejectsWrongNumberOfSeparators() {
        assertEquals("Некорректные данные", Solution.createCharacterCard("Kirill|mage"));
        assertEquals("Некорректные данные", Solution.createCharacterCard("Kirill|mage|15|extra"));
    }

    @Test
    void rejectsEmptyFields() {
        assertEquals("Некорректные данные", Solution.createCharacterCard(" |mage|15"));
        assertEquals("Некорректные данные", Solution.createCharacterCard("Kirill| |15"));
        assertEquals("Некорректные данные", Solution.createCharacterCard("Kirill|mage|  "));
    }
}
