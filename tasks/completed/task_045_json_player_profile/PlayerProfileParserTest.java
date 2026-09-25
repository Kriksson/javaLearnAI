package learning.task045;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerProfileParserTest {
    private final PlayerProfileParser parser = new PlayerProfileParser();

    @Test
    void parsesOrdinaryProfile() {
        assertEquals(new PlayerProfile(7, "Лис", 12),
                parser.parse("{\"id\":7,\"name\":\"Лис\",\"level\":12}"));
    }

    @Test
    void ignoresOrderWhitespaceAndUnknownFieldsWithoutChangingName() {
        assertEquals(new PlayerProfile(1, " Лис ", 1),
                parser.parse(" { \"level\": 1, \"online\": true, \"name\": \" Лис \", \"id\": 1 } "));
    }

    @Test
    void acceptsUpperBoundaries() {
        assertEquals(new PlayerProfile(Integer.MAX_VALUE, "Герой", 100),
                parser.parse("{\"id\":2147483647,\"name\":\"Герой\",\"level\":100}"));
    }

    @Test
    void preservesEscapedCharacters() {
        assertEquals(new PlayerProfile(3, "Лис\nСерый", 5),
                parser.parse("{\"id\":3,\"name\":\"Лис\\nСерый\",\"level\":5}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "   ", "null", "[]", "42", "\"text\"",
            "{}", "{\"name\":\"Лис\",\"level\":12}",
            "{\"id\":7,\"level\":12}", "{\"id\":7,\"name\":\"Лис\"}",
            "{\"id\":null,\"name\":\"Лис\",\"level\":12}",
            "{\"id\":0,\"name\":\"Лис\",\"level\":12}",
            "{\"id\":-1,\"name\":\"Лис\",\"level\":12}",
            "{\"id\":2147483648,\"name\":\"Лис\",\"level\":12}",
            "{\"id\":1.5,\"name\":\"Лис\",\"level\":12}",
            "{\"id\":\"7\",\"name\":\"Лис\",\"level\":12}",
            "{\"id\":7,\"name\":null,\"level\":12}",
            "{\"id\":7,\"name\":\"\",\"level\":12}",
            "{\"id\":7,\"name\":\"   \",\"level\":12}",
            "{\"id\":7,\"name\":12,\"level\":12}",
            "{\"id\":7,\"name\":\"Лис\",\"level\":0}",
            "{\"id\":7,\"name\":\"Лис\",\"level\":101}",
            "{\"id\":7,\"name\":\"Лис\",\"level\":1.5}",
            "{\"id\":7,\"name\":\"Лис\",\"level\":\"12\"}"
    })
    void rejectsInvalidProfiles(String json) {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(json));
    }

    @Test
    void rejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }

    @Test
    void keepsSyntaxErrorAsCause() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("{broken"));
        assertInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class, error.getCause());
    }
}
