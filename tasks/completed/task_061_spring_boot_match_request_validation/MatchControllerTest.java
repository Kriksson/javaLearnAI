package learning.task061;

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

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = MatchApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class MatchControllerTest {
    @Autowired
    private MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void ordinaryMatchIsNotRanked() throws Exception {
        assertPreview(submit("{\"playerName\":\"Мира\",\"level\":49,\"opponentId\":2}"),
                "Мира", 2, false);
    }

    @Test
    void rankedStartsAtLevelFiftyAndPreservesName() throws Exception {
        assertPreview(submit("{\"playerName\":\" Тор \",\"level\":50,\"opponentId\":1}"),
                " Тор ", 1, true);
    }

    @Test
    void acceptsInclusiveBoundaries() throws Exception {
        assertPreview(submit("{\"playerName\":\"12345678901234567890\",\"level\":1,\"opponentId\":1}"),
                "12345678901234567890", 1, false);
        assertPreview(submit("{\"playerName\":\"А\",\"level\":100,\"opponentId\":2147483647}"),
                "А", 2147483647, true);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"level\":50,\"opponentId\":1}",
            "{\"playerName\":null,\"level\":50,\"opponentId\":1}",
            "{\"playerName\":\"\",\"level\":50,\"opponentId\":1}",
            "{\"playerName\":\"   \",\"level\":50,\"opponentId\":1}",
            "{\"playerName\":\"123456789012345678901\",\"level\":50,\"opponentId\":1}",
            "{\"playerName\":\"Мира\",\"opponentId\":1}",
            "{\"playerName\":\"Мира\",\"level\":null,\"opponentId\":1}",
            "{\"playerName\":\"Мира\",\"level\":0,\"opponentId\":1}",
            "{\"playerName\":\"Мира\",\"level\":101,\"opponentId\":1}",
            "{\"playerName\":\"Мира\",\"level\":-1,\"opponentId\":1}",
            "{\"playerName\":\"Мира\",\"level\":50}",
            "{\"playerName\":\"Мира\",\"level\":50,\"opponentId\":null}",
            "{\"playerName\":\"Мира\",\"level\":50,\"opponentId\":0}",
            "{\"playerName\":\"Мира\",\"level\":50,\"opponentId\":-1}"
    })
    void beanValidationRejectsInvalidFields(String body) throws Exception {
        assertEquals(400, submit(body).getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{broken", "null", "[]", "42",
            "{\"playerName\":\"Мира\",\"level\":\"abc\",\"opponentId\":1}"})
    void springRejectsUnreadableJson(String body) throws Exception {
        assertEquals(400, submit(body).getStatus());
    }

    @Test
    void otherMethodAndPathDoNotInvokePreview() throws Exception {
        assertEquals(405, mvc.perform(get("/matches/preview")).andReturn().getResponse().getStatus());
        assertEquals(404, mvc.perform(post("/matches/preview/extra")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerName\":\"Мира\",\"level\":50,\"opponentId\":1}"))
                .andReturn().getResponse().getStatus());
    }

    private MockHttpServletResponse submit(String body) throws Exception {
        return mvc.perform(post("/matches/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.getBytes(StandardCharsets.UTF_8)))
                .andReturn().getResponse();
    }

    private void assertPreview(MockHttpServletResponse response, String playerName,
                               int opponentId, boolean ranked) throws Exception {
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode json = mapper.readTree(response.getContentAsByteArray());
        assertTrue(json.isObject());
        assertEquals(3, json.size());
        assertTrue(json.path("playerName").isTextual());
        assertEquals(playerName, json.path("playerName").textValue());
        assertTrue(json.path("opponentId").isIntegralNumber());
        assertEquals(opponentId, json.path("opponentId").intValue());
        assertTrue(json.path("ranked").isBoolean());
        assertEquals(ranked, json.path("ranked").booleanValue());
    }
}
