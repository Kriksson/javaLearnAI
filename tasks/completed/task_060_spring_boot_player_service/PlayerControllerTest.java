package learning.task060;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = PlayerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PlayerControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private PlayerService service;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serviceIsRegisteredAndFindsOnlyExistingPlayers() {
        assertEquals(new PlayerView(1, "Мира", 10), service.find(1));
        assertEquals(new PlayerView(2, "Тор", 50), service.find(2));
        assertNull(service.find(9));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void serviceAwardsPointsAndDoesNotCreateUnknownPlayer() {
        assertNull(service.award(9, 5));
        assertNull(service.find(9));
        assertEquals(new PlayerView(1, "Мира", 15), service.award(1, 5));
        assertEquals(new PlayerView(1, "Мира", 15), service.find(1));
        assertEquals(new PlayerView(2, "Тор", 50), service.find(2));
    }

    @Test
    void readsInitialPlayersThroughController() throws Exception {
        assertPlayer(read(1), 1, "Мира", 10);
        assertPlayer(read(2), 2, "Тор", 50);
    }

    @Test
    void rejectsInvalidAndUnknownIdsOnGet() throws Exception {
        assertEmptyStatus(read(0), 400);
        assertEmptyStatus(read(-1), 400);
        assertEmptyStatus(read(9), 404);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void postAwardsAgainOnRepeatedRequest() throws Exception {
        assertPlayer(submit(1, "{\"points\":5}"), 1, "Мира", 15);
        assertPlayer(read(1), 1, "Мира", 15);
        assertPlayer(submit(1, "{\"points\":5}"), 1, "Мира", 20);
        assertPlayer(read(1), 1, "Мира", 20);
        assertPlayer(read(2), 2, "Тор", 50);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void acceptsPointsAtBothBounds() throws Exception {
        assertPlayer(submit(2, "{\"points\":1}"), 2, "Тор", 51);
        assertPlayer(submit(2, "{\"points\":20}"), 2, "Тор", 71);
        assertPlayer(read(1), 1, "Мира", 10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"points\":null}", "{\"points\":0}",
            "{\"points\":-1}", "{\"points\":21}"})
    void rejectsInvalidPointsWithoutChangingScore(String body) throws Exception {
        assertEmptyStatus(submit(1, body), 400);
        assertPlayer(read(1), 1, "Мира", 10);
    }

    @Test
    void checksIdAndExistenceBeforeValidatingParseableBody() throws Exception {
        assertEmptyStatus(submit(0, "{\"points\":5}"), 400);
        assertEmptyStatus(submit(-1, "{\"points\":5}"), 400);
        assertEmptyStatus(submit(9, "{\"points\":5}"), 404);
        assertEmptyStatus(submit(9, "{}"), 404);
        assertPlayer(read(1), 1, "Мира", 10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{broken", "null", "[]", "42", "{\"points\":\"abc\"}"})
    void springRejectsUnreadableJson(String body) throws Exception {
        assertEquals(400, submit(1, body).getStatus());
        assertPlayer(read(1), 1, "Мира", 10);
    }

    @Test
    void wrongMethodAndPathDoNotAwardPoints() throws Exception {
        assertEquals(405, mvc.perform(get("/players/1/points"))
                .andReturn().getResponse().getStatus());
        assertEquals(404, mvc.perform(post("/players/1/points/extra")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"points\":5}"))
                .andReturn().getResponse().getStatus());
        assertPlayer(read(1), 1, "Мира", 10);
    }

    private MockHttpServletResponse read(int id) throws Exception {
        return mvc.perform(get("/players/{id}", id)).andReturn().getResponse();
    }

    private MockHttpServletResponse submit(int id, String body) throws Exception {
        return mvc.perform(post("/players/{id}/points", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.getBytes(StandardCharsets.UTF_8)))
                .andReturn().getResponse();
    }

    private void assertPlayer(MockHttpServletResponse response, int id, String name, int score) throws Exception {
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode json = mapper.readTree(response.getContentAsByteArray());
        assertTrue(json.isObject());
        assertEquals(3, json.size());
        assertTrue(json.path("id").isIntegralNumber());
        assertEquals(id, json.path("id").intValue());
        assertTrue(json.path("name").isTextual());
        assertEquals(name, json.path("name").textValue());
        assertTrue(json.path("score").isIntegralNumber());
        assertEquals(score, json.path("score").intValue());
    }

    private void assertEmptyStatus(MockHttpServletResponse response, int status) {
        assertEquals(status, response.getStatus());
        assertEquals(0, response.getContentAsByteArray().length);
    }
}
