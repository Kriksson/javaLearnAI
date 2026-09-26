package learning.task058;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = PlayerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PlayerSearchControllerTest {
    private static final Map<Integer, PlayerView> EXPECTED = Map.of(
            1, new PlayerView(1, "Мира", 10),
            2, new PlayerView(2, "Тор", 20),
            3, new PlayerView(3, "Мирон", 30),
            4, new PlayerView(4, "Ари", 1),
            5, new PlayerView(5, "Тори", 100)
    );

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void returnsAllPlayersSortedById() throws Exception {
        assertPlayers(search(null, null), 1, 2, 3, 4, 5);
    }

    @Test
    void filtersByMinimumLevelInclusively() throws Exception {
        assertPlayers(search("20", null), 2, 3, 5);
        assertPlayers(search("1", null), 1, 2, 3, 4, 5);
        assertPlayers(search("100", null), 5);
    }

    @Test
    void trimsNameAndMatchesIgnoringCase() throws Exception {
        assertPlayers(search(null, "  МиР  "), 1, 3);
        assertPlayers(search(null, "ТОР"), 2, 5);
    }

    @Test
    void combinesFiltersAndKeepsIdOrder() throws Exception {
        assertPlayers(search("20", "тор"), 2, 5);
        assertPlayers(search("30", "мир"), 3);
    }

    @Test
    void returnsEmptyArrayWhenNothingMatches() throws Exception {
        assertPlayers(search(null, "несуществующий"));
        assertPlayers(search("100", "мир"));
    }

    @Test
    void acceptsTwentyCharacterNameFilter() throws Exception {
        assertPlayers(search(null, "12345678901234567890"));
        assertPlayers(search(null, "  12345678901234567890  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "101"})
    void rejectsOutOfRangeMinimumLevel(String minLevel) throws Exception {
        assertEmptyStatus(search(minLevel, null), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "123456789012345678901"})
    void rejectsInvalidNameFilter(String name) throws Exception {
        assertEmptyStatus(search(null, name), 400);
    }

    @Test
    void rejectsInvalidParameterEvenIfOtherFilterWouldFindNothing() throws Exception {
        assertEmptyStatus(search("0", "несуществующий"), 400);
        assertEmptyStatus(search("50", "   "), 400);
    }

    @Test
    void springRejectsNonnumericMinimumLevel() throws Exception {
        assertEquals(400, search("abc", null).getStatus());
    }

    @Test
    void otherMethodAndPathDoNotSearch() throws Exception {
        assertEquals(405, mvc.perform(post("/players").contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getStatus());
        assertEquals(404, mvc.perform(get("/players/extra"))
                .andReturn().getResponse().getStatus());
    }

    private MockHttpServletResponse search(String minLevel, String name) throws Exception {
        MockHttpServletRequestBuilder request = get("/players");
        if (minLevel != null) request.param("minLevel", minLevel);
        if (name != null) request.param("name", name);
        return mvc.perform(request).andReturn().getResponse();
    }

    private void assertPlayers(MockHttpServletResponse response, int... ids) throws Exception {
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode json = mapper.readTree(response.getContentAsByteArray());
        assertTrue(json.isArray());
        assertEquals(ids.length, json.size());
        for (int index = 0; index < ids.length; index++) {
            PlayerView expected = EXPECTED.get(ids[index]);
            JsonNode player = json.get(index);
            assertTrue(player.isObject());
            assertEquals(3, player.size());
            assertTrue(player.path("id").isIntegralNumber());
            assertEquals(expected.id(), player.path("id").intValue());
            assertTrue(player.path("name").isTextual());
            assertEquals(expected.name(), player.path("name").textValue());
            assertTrue(player.path("level").isIntegralNumber());
            assertEquals(expected.level(), player.path("level").intValue());
        }
    }

    private void assertEmptyStatus(MockHttpServletResponse response, int status) {
        assertEquals(status, response.getStatus());
        assertEquals(0, response.getContentAsByteArray().length);
    }
}
