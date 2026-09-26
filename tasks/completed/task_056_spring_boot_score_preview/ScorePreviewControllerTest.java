package learning.task056;

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

@SpringBootTest(classes = ScoreApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ScorePreviewControllerTest {
    @Autowired
    private MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void returnsTypedPreviewWithoutBonus() throws Exception {
        MockHttpServletResponse response = submit("{\"wins\":3,\"losses\":1}");
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode json = mapper.readTree(response.getContentAsByteArray());
        assertTrue(json.isObject());
        assertEquals(2, json.size());
        assertTrue(json.path("points").isIntegralNumber());
        assertEquals(8, json.path("points").intValue());
        assertTrue(json.path("bonus").isBoolean());
        assertEquals(false, json.path("bonus").booleanValue());
    }

    @Test
    void givesBonusAtFiveWins() throws Exception {
        MockHttpServletResponse response = submit("{\"wins\":5,\"losses\":0}");
        assertEquals(200, response.getStatus());
        JsonNode json = mapper.readTree(response.getContentAsByteArray());
        assertEquals(25, json.path("points").intValue());
        assertEquals(true, json.path("bonus").booleanValue());
    }

    @Test
    void fourWinsDoNotReceiveBonus() throws Exception {
        JsonNode json = readSuccessful("{\"wins\":4,\"losses\":0}");
        assertEquals(12, json.path("points").intValue());
        assertEquals(false, json.path("bonus").booleanValue());
    }

    @Test
    void pointsNeverFallBelowZero() throws Exception {
        JsonNode json = readSuccessful("{\"wins\":0,\"losses\":1}");
        assertEquals(0, json.path("points").intValue());
        assertEquals(false, json.path("bonus").booleanValue());
    }

    @Test
    void acceptsUpperBoundAndIgnoresExtraFields() throws Exception {
        JsonNode json = readSuccessful("{\"losses\":100,\"extra\":\"Кот\",\"wins\":100}");
        assertEquals(210, json.path("points").intValue());
        assertEquals(true, json.path("bonus").booleanValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"wins\":1}", "{\"losses\":1}",
            "{\"wins\":null,\"losses\":1}", "{\"wins\":1,\"losses\":null}",
            "{\"wins\":-1,\"losses\":1}", "{\"wins\":1,\"losses\":-1}",
            "{\"wins\":101,\"losses\":1}", "{\"wins\":1,\"losses\":101}",
            "{\"wins\":0,\"losses\":0}"
    })
    void rejectsInvalidValuesWithoutBody(String body) throws Exception {
        MockHttpServletResponse response = submit(body);
        assertEquals(400, response.getStatus());
        assertEquals(0, response.getContentAsByteArray().length);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "{broken", "null", "[]", "42",
            "{\"wins\":\"abc\",\"losses\":1}"
    })
    void springRejectsUnreadableJson(String body) throws Exception {
        assertEquals(400, submit(body).getStatus());
    }

    @Test
    void getDoesNotInvokePostMethod() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/scores/preview")).andReturn().getResponse();
        assertEquals(405, response.getStatus());
    }

    @Test
    void wrongPathDoesNotInvokePostMethod() throws Exception {
        MockHttpServletResponse response = mvc.perform(post("/scores/preview/extra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wins\":1,\"losses\":1}"))
                .andReturn().getResponse();
        assertEquals(404, response.getStatus());
    }

    private JsonNode readSuccessful(String body) throws Exception {
        MockHttpServletResponse response = submit(body);
        assertEquals(200, response.getStatus());
        return mapper.readTree(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    private MockHttpServletResponse submit(String body) throws Exception {
        return mvc.perform(post("/scores/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.getBytes(StandardCharsets.UTF_8)))
                .andReturn().getResponse();
    }
}
