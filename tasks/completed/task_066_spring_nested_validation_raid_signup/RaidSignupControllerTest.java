package learning.task066;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RaidSignupControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsSignupAndReturnsItsLocation() throws Exception {
        MvcResult result = mockMvc.perform(post("/guild/raid-signups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("Кирилл", "Лира", 25, "mage")))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.playerName").value("Кирилл"))
                .andExpect(jsonPath("$.hero.name").value("Лира"))
                .andExpect(jsonPath("$.hero.level").value(25))
                .andExpect(jsonPath("$.hero.className").value("mage"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        long id = response.get("id").asLong();
        assertTrue(id > 0L);
        assertEquals("/guild/raid-signups/" + id,
                URI.create(result.getResponse().getHeader("Location")).getPath());
    }

    @Test
    void readsCreatedSignup() throws Exception {
        long id = createSignup("Тарен", "Ворон", 40, "ranger");

        mockMvc.perform(get("/guild/raid-signups/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.playerName").value("Тарен"))
                .andExpect(jsonPath("$.hero.name").value("Ворон"))
                .andExpect(jsonPath("$.hero.level").value(40))
                .andExpect(jsonPath("$.hero.className").value("ranger"));
    }

    @Test
    void assignsDistinctPositiveIds() throws Exception {
        long firstId = createSignup("Игрок один", "Герой", 10, "mage");
        long secondId = createSignup("Игрок два", "Герой", 10, "mage");

        assertTrue(firstId > 0L);
        assertTrue(secondId > 0L);
        assertNotEquals(firstId, secondId);
    }

    @ParameterizedTest
    @MethodSource("validBoundaryBodies")
    void acceptsInclusiveFieldBounds(String body) throws Exception {
        mockMvc.perform(post("/guild/raid-signups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private static Stream<String> validBoundaryBodies() {
        return Stream.of(
                validBody("x", "y".repeat(40), 1, "m"),
                validBody("p".repeat(30), "z", 100, "c".repeat(20))
        );
    }

    @Test
    void unknownAndMalformedIdsReturnClientErrors() throws Exception {
        for (String id : new String[]{"0", "-1", "9223372036854775807"}) {
            mockMvc.perform(get("/guild/raid-signups/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(""));
        }

        for (String id : new String[]{"not-a-number", "9223372036854775808"}) {
            mockMvc.perform(get("/guild/raid-signups/{id}", id))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void rejectsMissingRequestBody() throws Exception {
        mockMvc.perform(post("/guild/raid-signups")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void rejectsInvalidTopLevelAndNestedFields(String body) throws Exception {
        mockMvc.perform(post("/guild/raid-signups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private static Stream<String> invalidBodies() {
        String validHero = "\"name\":\"Лира\",\"level\":25,\"className\":\"mage\"";
        return Stream.of(
                "{\"playerName\":\"\",\"hero\":{" + validHero + "}}",
                "{\"playerName\":null,\"hero\":{" + validHero + "}}",
                "{\"playerName\":\"   \",\"hero\":{" + validHero + "}}",
                "{\"playerName\":\"" + "x".repeat(31) + "\",\"hero\":{" + validHero + "}}",
                "{\"hero\":{" + validHero + "}}",
                "{\"playerName\":\"Кирилл\",\"hero\":null}",
                "{\"playerName\":\"Кирилл\"}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"name\":\"\",\"level\":25,\"className\":\"mage\"}}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"name\":null,\"level\":25,\"className\":\"mage\"}}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"name\":\"   \",\"level\":25,\"className\":\"mage\"}}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"level\":25,\"className\":\"mage\"}}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"name\":\"" + "x".repeat(41) + "\",\"level\":25,\"className\":\"mage\"}}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"name\":\"Лира\",\"level\":0,\"className\":\"mage\"}}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"name\":\"Лира\",\"level\":101,\"className\":\"mage\"}}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"name\":\"Лира\",\"level\":null,\"className\":\"mage\"}}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"name\":\"Лира\",\"className\":\"mage\"}}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"name\":\"Лира\",\"level\":25,\"className\":\"\"}}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"name\":\"Лира\",\"level\":25,\"className\":null}}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"name\":\"Лира\",\"level\":25,\"className\":\"   \"}}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"name\":\"Лира\",\"level\":25,\"className\":\"" + "c".repeat(21) + "\"}}",
                "{\"playerName\":\"Кирилл\",\"hero\":{\"name\":\"Лира\",\"level\":25}}",
                "{\"playerName\":"
        );
    }

    private long createSignup(String playerName, String heroName, int level, String className) throws Exception {
        MvcResult result = mockMvc.perform(post("/guild/raid-signups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(playerName, heroName, level, className)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private static String validBody(String playerName, String heroName, int level, String className) {
        return "{\"playerName\":\"" + playerName + "\",\"hero\":{\"name\":\"" + heroName
                + "\",\"level\":" + level + ",\"className\":\"" + className + "\"}}";
    }
}
