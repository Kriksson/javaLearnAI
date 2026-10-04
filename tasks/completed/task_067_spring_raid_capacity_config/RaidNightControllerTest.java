package learning.task067;

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
import org.springframework.test.web.servlet.ResultActions;

import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "guild.raids.max-participants-per-raid=2")
@AutoConfigureMockMvc
class RaidNightControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsParticipantAndReturnsItsLocation() throws Exception {
        MvcResult result = postParticipant("401", body("Кирилл"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.raidId").value(401))
                .andExpect(jsonPath("$.playerName").value("Кирилл"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        long id = response.get("id").asLong();
        assertTrue(id > 0L);
        assertEquals("/guild/raid-nights/401/participants/" + id,
                URI.create(result.getResponse().getHeader("Location")).getPath());
    }

    @Test
    void assignsDistinctPositiveIdsWithinRaid() throws Exception {
        long firstId = createParticipant(501, "Первый");
        long secondId = createParticipant(501, "Второй");

        assertTrue(firstId > 0L);
        assertTrue(secondId > 0L);
        assertNotEquals(firstId, secondId);
    }

    @Test
    void appliesConfiguredLimitSeparatelyForEachRaid() throws Exception {
        createParticipant(601, "Игрок один");
        createParticipant(601, "Игрок два");
        createParticipant(602, "Игрок три");
        createParticipant(602, "Игрок четыре");

        postParticipant("601", body("Игрок пять"))
                .andExpect(status().isConflict())
                .andExpect(content().string(""));
        postParticipant("602", body("Игрок шесть"))
                .andExpect(status().isConflict())
                .andExpect(content().string(""));
    }

    @Test
    void repeatedPlayerNameUsesAnotherPlace() throws Exception {
        long firstId = createParticipant(701, "Кирилл");
        long secondId = createParticipant(701, "Кирилл");

        assertNotEquals(firstId, secondId);
    }

    @ParameterizedTest
    @MethodSource("validBoundaryBodies")
    void acceptsInclusiveNameBounds(String body) throws Exception {
        postParticipant("801", body).andExpect(status().isCreated());
    }

    private static Stream<String> validBoundaryBodies() {
        return Stream.of(
                "{\"playerName\":\"x\"}",
                "{\"playerName\":\"" + "p".repeat(30) + "\"}"
        );
    }

    @Test
    void invalidRaidIdsReturnBadRequest() throws Exception {
        for (String raidId : new String[]{"0", "-1", "not-a-number", "9223372036854775808"}) {
            postParticipant(raidId, body("Кирилл"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void rejectsMissingRequestBody() throws Exception {
        mockMvc.perform(post("/guild/raid-nights/{raidId}/participants", 901)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void rejectsInvalidPlayerNamesAndMalformedJson(String body) throws Exception {
        postParticipant("902", body).andExpect(status().isBadRequest());
    }

    private static Stream<String> invalidBodies() {
        return Stream.of(
                "{\"playerName\":\"\"}",
                "{\"playerName\":null}",
                "{\"playerName\":\"   \"}",
                "{\"playerName\":\"" + "x".repeat(31) + "\"}",
                "{}",
                "{\"playerName\":"
        );
    }

    private long createParticipant(long raidId, String playerName) throws Exception {
        MvcResult result = postParticipant(Long.toString(raidId), body(playerName))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions postParticipant(String raidId, String requestBody) throws Exception {
        return mockMvc.perform(post("/guild/raid-nights/{raidId}/participants", raidId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }

    private String body(String playerName) throws Exception {
        return objectMapper.writeValueAsString(Map.of("playerName", playerName));
    }
}
