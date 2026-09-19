package learning.task001;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SolutionTest {
    @Test
    void printsCharacterProfile() {
        assertProgramOutput(
                "Kirill\nWarrior\n12\n",
                "Игрок: Kirill\nКласс: Warrior\nУровень: 12\nГотов к приключению!"
        );
    }

    @Test
    void supportsSpacesAndCyrillic() {
        assertProgramOutput(
                "Тёмный рыцарь\nМаг огня\n1\n",
                "Игрок: Тёмный рыцарь\nКласс: Маг огня\nУровень: 1\nГотов к приключению!"
        );
    }

    @Test
    void supportsMaximumLevel() {
        assertProgramOutput(
                "Player One\nSpace Ranger\n100\n",
                "Игрок: Player One\nКласс: Space Ranger\nУровень: 100\nГотов к приключению!"
        );
    }

    private void assertProgramOutput(String input, String expected) {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

            Solution.main(new String[0]);

            String actual = output.toString(StandardCharsets.UTF_8)
                    .replace("\r\n", "\n")
                    .stripTrailing();
            assertEquals(expected, actual);
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }
}
