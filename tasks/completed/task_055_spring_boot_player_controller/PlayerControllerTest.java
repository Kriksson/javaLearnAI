package learning.task055;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = PlayerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PlayerControllerTest {
    @Autowired
    private MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void returnsAliceAsTypedJson() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/players/1")).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode json = mapper.readTree(response.getContentAsByteArray());
        assertTrue(json.isObject());
        assertEquals(2, json.size());
        assertTrue(json.path("playerId").isIntegralNumber());
        assertEquals(1, json.path("playerId").intValue());
        assertTrue(json.path("name").isTextual());
        assertEquals("Alice", json.path("name").textValue());
    }

    @Test
    void returnsCyrillicNameAndEscapedQuotesInUtf8() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/players/7")).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        JsonNode json = mapper.readTree(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
        assertEquals(7, json.path("playerId").intValue());
        assertEquals("Кот \"Рыцарь\"", json.path("name").textValue());
    }

    @Test
    void acceptsLeadingZeroes() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/players/007")).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals(7, mapper.readTree(response.getContentAsByteArray()).path("playerId").intValue());
    }

    @Test
    void unknownPlayerReturns404WithoutBody() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/players/99")).andReturn().getResponse();
        assertEquals(404, response.getStatus());
        assertEquals(0, response.getContentAsByteArray().length);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void nonPositiveIdReturns400WithoutBody(String id) throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/players/" + id)).andReturn().getResponse();
        assertEquals(400, response.getStatus());
        assertEquals(0, response.getContentAsByteArray().length);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "2147483648"})
    void springRejectsNonIntPathVariable(String id) throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/players/" + id)).andReturn().getResponse();
        assertEquals(400, response.getStatus());
    }

    @Test
    void postDoesNotRunGetController() throws Exception {
        MockHttpServletResponse response = mvc.perform(post("/players/7")).andReturn().getResponse();
        assertEquals(405, response.getStatus());
    }
}
