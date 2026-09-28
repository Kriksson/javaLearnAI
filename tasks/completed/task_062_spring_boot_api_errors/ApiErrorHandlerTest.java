package learning.task062;

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

@SpringBootTest(classes = PlayerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ApiErrorHandlerTest {
    @Autowired
    private MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void validRequestStillReturnsNormalResponse() throws Exception {
        MockHttpServletResponse response = submit("{\"name\":\"Мира\",\"level\":10}");
        assertEquals(200, response.getStatus());
        JsonNode json = mapper.readTree(response.getContentAsByteArray());
        assertTrue(json.isObject());
        assertEquals(2, json.size());
        assertEquals("Мира", json.path("name").textValue());
        assertEquals(10, json.path("level").intValue());
    }

    @Test
    void reportsInvalidName() throws Exception {
        assertError(submit("{\"name\":\"\",\"level\":10}"), "invalid_request", "name");
        assertError(submit("{\"name\":\"123456789012345678901\",\"level\":10}"),
                "invalid_request", "name");
    }

    @Test
    void reportsInvalidLevel() throws Exception {
        assertError(submit("{\"name\":\"Мира\",\"level\":0}"), "invalid_request", "level");
        assertError(submit("{\"name\":\"Мира\",\"level\":101}"), "invalid_request", "level");
    }

    @Test
    void sortsFieldNamesAndRemovesDuplicates() throws Exception {
        assertError(submit("{\"name\":\"                     \",\"level\":0}"),
                "invalid_request", "level", "name");
    }

    @Test
    void reportsMissingFieldsInSortedOrder() throws Exception {
        assertError(submit("{}"), "invalid_request", "level", "name");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{broken", "null", "[]", "42",
            "{\"name\":\"Мира\",\"level\":\"abc\"}"})
    void unreadableJsonGetsSeparateErrorCode(String body) throws Exception {
        assertError(submit(body), "malformed_json");
    }

    @Test
    void otherMethodAndPathKeepTheirNormalStatuses() throws Exception {
        assertEquals(405, mvc.perform(get("/players/preview")).andReturn().getResponse().getStatus());
        assertEquals(404, mvc.perform(post("/players/preview/extra")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Мира\",\"level\":10}"))
                .andReturn().getResponse().getStatus());
    }

    private MockHttpServletResponse submit(String body) throws Exception {
        return mvc.perform(post("/players/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.getBytes(StandardCharsets.UTF_8)))
                .andReturn().getResponse();
    }

    private void assertError(MockHttpServletResponse response, String code, String... fields) throws Exception {
        assertEquals(400, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode json = mapper.readTree(response.getContentAsByteArray());
        assertTrue(json.isObject());
        assertEquals(2, json.size());
        assertTrue(json.path("code").isTextual());
        assertEquals(code, json.path("code").textValue());
        JsonNode actualFields = json.path("fields");
        assertTrue(actualFields.isArray());
        assertEquals(fields.length, actualFields.size());
        for (int index = 0; index < fields.length; index++) {
            assertTrue(actualFields.get(index).isTextual());
            assertEquals(fields[index], actualFields.get(index).textValue());
        }
    }
}
