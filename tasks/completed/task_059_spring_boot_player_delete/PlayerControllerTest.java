package learning.task059;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = PlayerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PlayerControllerTest {
    @Autowired
    private MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void readsInitialPlayers() throws Exception {
        assertPlayer(read(1), 1, "Мира", 10);
        assertPlayer(read(2), 2, "Тор", 20);
        assertPlayer(read(3), 3, "Ари", 30);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void deletesOnlySelectedPlayerAndRepeatReturnsNotFound() throws Exception {
        assertEmptyStatus(remove(1), 204);
        assertEmptyStatus(read(1), 404);
        assertEmptyStatus(remove(1), 404);
        assertPlayer(read(2), 2, "Тор", 20);
        assertPlayer(read(3), 3, "Ари", 30);

        assertEmptyStatus(remove(3), 204);
        assertEmptyStatus(read(3), 404);
        assertEmptyStatus(remove(3), 404);
        assertPlayer(read(2), 2, "Тор", 20);
    }

    @Test
    void rejectsActiveMatchWithoutDeletingPlayer() throws Exception {
        assertEmptyStatus(remove(2), 409);
        assertPlayer(read(2), 2, "Тор", 20);
        assertEmptyStatus(remove(2), 409);
        assertPlayer(read(2), 2, "Тор", 20);
    }

    @Test
    void rejectsInvalidAndUnknownIdsWithoutChangingPlayers() throws Exception {
        assertEmptyStatus(remove(0), 400);
        assertEmptyStatus(remove(-1), 400);
        assertEmptyStatus(remove(9), 404);
        assertPlayer(read(1), 1, "Мира", 10);
        assertPlayer(read(2), 2, "Тор", 20);
        assertPlayer(read(3), 3, "Ари", 30);
    }

    @Test
    void otherMethodAndPathDoNotDeletePlayer() throws Exception {
        assertEquals(405, mvc.perform(post("/players/1")).andReturn().getResponse().getStatus());
        assertEquals(404, mvc.perform(delete("/players/1/extra")).andReturn().getResponse().getStatus());
        assertPlayer(read(1), 1, "Мира", 10);
    }

    private MockHttpServletResponse remove(int id) throws Exception {
        return mvc.perform(delete("/players/{id}", id)).andReturn().getResponse();
    }

    private MockHttpServletResponse read(int id) throws Exception {
        return mvc.perform(get("/players/{id}", id)).andReturn().getResponse();
    }

    private void assertPlayer(MockHttpServletResponse response, int id, String name, int level) throws Exception {
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode json = mapper.readTree(response.getContentAsByteArray());
        assertTrue(json.isObject());
        assertEquals(3, json.size());
        assertTrue(json.path("id").isIntegralNumber());
        assertEquals(id, json.path("id").intValue());
        assertTrue(json.path("name").isTextual());
        assertEquals(name, json.path("name").textValue());
        assertTrue(json.path("level").isIntegralNumber());
        assertEquals(level, json.path("level").intValue());
    }

    private void assertEmptyStatus(MockHttpServletResponse response, int status) {
        assertEquals(status, response.getStatus());
        assertEquals(0, response.getContentAsByteArray().length);
    }
}
