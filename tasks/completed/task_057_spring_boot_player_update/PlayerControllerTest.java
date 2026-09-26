package learning.task057;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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
    }

    @Test
    void rejectsInvalidAndUnknownIdsOnGet() throws Exception {
        assertEmptyStatus(getResponse(0), 400);
        assertEmptyStatus(getResponse(-1), 400);
        assertEmptyStatus(getResponse(9), 404);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void trimsNameUpdatesExistingPlayerAndIsIdempotent() throws Exception {
        String body = "{\"name\":\"  Лиса  \",\"level\":11}";
        assertPlayer(submit(1, body), 1, "Лиса", 11);
        assertPlayer(read(1), 1, "Лиса", 11);
        assertPlayer(submit(1, body), 1, "Лиса", 11);
        assertPlayer(read(2), 2, "Тор", 20);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void acceptsNameAndLevelBoundaries() throws Exception {
        String twenty = "12345678901234567890";
        assertPlayer(submit(2, "{\"name\":\"" + twenty + "\",\"level\":1}"), 2, twenty, 1);
        assertPlayer(submit(2, "{\"name\":\"А\",\"level\":100}"), 2, "А", 100);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"name\":\"Лиса\"}", "{\"level\":11}",
            "{\"name\":null,\"level\":11}", "{\"name\":\"Лиса\",\"level\":null}",
            "{\"name\":\"\",\"level\":11}", "{\"name\":\"   \",\"level\":11}",
            "{\"name\":\"123456789012345678901\",\"level\":11}",
            "{\"name\":\"Лиса\",\"level\":0}", "{\"name\":\"Лиса\",\"level\":101}",
            "{\"name\":\"Лиса\",\"level\":-1}"
    })
    void rejectsInvalidFieldsWithoutChangingPlayer(String body) throws Exception {
        assertEmptyStatus(submit(1, body), 400);
        assertPlayer(read(1), 1, "Мира", 10);
    }

    @Test
    void checksIdAndExistenceBeforeUpdating() throws Exception {
        String valid = "{\"name\":\"Лиса\",\"level\":11}";
        assertEmptyStatus(submit(0, valid), 400);
        assertEmptyStatus(submit(-1, valid), 400);
        assertEmptyStatus(submit(9, valid), 404);
        assertEmptyStatus(submit(9, "{}"), 404);
        assertPlayer(read(1), 1, "Мира", 10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{broken", "null", "[]", "42", "{\"name\":\"Лиса\",\"level\":\"abc\"}"})
    void springRejectsUnreadableJson(String body) throws Exception {
        assertEquals(400, submit(1, body).getStatus());
        assertPlayer(read(1), 1, "Мира", 10);
    }

    @Test
    void otherMethodsAndPathsDoNotUpdatePlayer() throws Exception {
        assertEquals(405, mvc.perform(post("/players/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Лиса\",\"level\":11}"))
                .andReturn().getResponse().getStatus());
        assertEquals(404, mvc.perform(put("/players/1/extra")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Лиса\",\"level\":11}"))
                .andReturn().getResponse().getStatus());
        assertPlayer(read(1), 1, "Мира", 10);
    }

    private MockHttpServletResponse getResponse(int id) throws Exception {
        return mvc.perform(get("/players/{id}", id)).andReturn().getResponse();
    }

    private MockHttpServletResponse submit(int id, String body) throws Exception {
        return mvc.perform(put("/players/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.getBytes(StandardCharsets.UTF_8)))
                .andReturn().getResponse();
    }

    private MockHttpServletResponse read(int id) throws Exception {
        return getResponse(id);
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
