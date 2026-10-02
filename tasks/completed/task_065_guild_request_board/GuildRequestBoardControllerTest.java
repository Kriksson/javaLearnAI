package learning.task065;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GuildRequestBoardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsRequestAndReturnsItsLocation() throws Exception {
        MvcResult result = mockMvc.perform(post("/guild/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Сопроводить караван", 90)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("Сопроводить караван"))
                .andExpect(jsonPath("$.reward").value(90))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        int id = body.get("id").asInt();
        assertTrue(id > 0);
        assertTrue(body.get("claimedBy").isNull());
        assertEquals("/guild/requests/" + id, result.getResponse().getHeader("Location"));
    }

    @Test
    void allowsIdenticalRequestsWithDifferentIds() throws Exception {
        int firstId = createRequest("Одинаковая заявка", 90);
        int secondId = createRequest("Одинаковая заявка", 90);

        assertNotEquals(firstId, secondId);
    }

    @ParameterizedTest
    @MethodSource("validCreateBodies")
    void acceptsInclusiveRequestBounds(String body) throws Exception {
        mockMvc.perform(post("/guild/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private static Stream<String> validCreateBodies() {
        return Stream.of(
                createBody("x", 1),
                createBody("x".repeat(80), 10000)
        );
    }

    @Test
    void listsRequestsInIdOrder() throws Exception {
        int firstId = createRequest("Первая заявка", 30);
        int secondId = createRequest("Вторая заявка", 60);

        MvcResult result = mockMvc.perform(get("/guild/requests"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(response.isArray());
        List<Integer> ids = new ArrayList<>();
        response.forEach(request -> ids.add(request.get("id").asInt()));

        assertTrue(ids.contains(firstId));
        assertTrue(ids.contains(secondId));
        assertEquals(ids.stream().sorted().toList(), ids);
    }

    @Test
    void returnsEmptyArrayWhenCatalogIsEmpty() throws Exception {
        QuestRepository emptyRepository = new QuestRepository();
        QuestController emptyController = new QuestController(
                new QuestService(emptyRepository), emptyRepository);
        MockMvc emptyMockMvc = MockMvcBuilders.standaloneSetup(emptyController).build();

        emptyMockMvc.perform(get("/guild/requests"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void returnsRequestDetails() throws Exception {
        int id = createRequest("Найти следы дракона", 125);

        mockMvc.perform(get("/guild/requests/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Найти следы дракона"))
                .andExpect(jsonPath("$.reward").value(125))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void claimingOpenRequestStoresHeroName() throws Exception {
        int id = createRequest("Защитить деревню", 200);

        mockMvc.perform(put("/guild/requests/{id}/claim", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimBody("Лира")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("CLAIMED"))
                .andExpect(jsonPath("$.claimedBy").value("Лира"));
    }

    @Test
    void alreadyClaimedRequestKeepsItsFirstHero() throws Exception {
        int id = createRequest("Исследовать руины", 150);

        mockMvc.perform(put("/guild/requests/{id}/claim", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimBody("Мира")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/guild/requests/{id}/claim", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimBody("Тарен")))
                .andExpect(status().isConflict())
                .andExpect(content().string(""));

        mockMvc.perform(get("/guild/requests/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLAIMED"))
                .andExpect(jsonPath("$.claimedBy").value("Мира"));
    }

    @Test
    void unknownAndMalformedIdsReturnClientErrors() throws Exception {
        mockMvc.perform(get("/guild/requests/2147483647"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mockMvc.perform(get("/guild/requests/0"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mockMvc.perform(get("/guild/requests/-1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mockMvc.perform(put("/guild/requests/2147483647/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimBody("Лира")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mockMvc.perform(put("/guild/requests/0/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimBody("Лира")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mockMvc.perform(put("/guild/requests/-1/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimBody("Лира")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mockMvc.perform(get("/guild/requests/not-a-number"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/guild/requests/not-a-number/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimBody("Лира")))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("invalidCreateBodies")
    void rejectsInvalidRequestFields(String body) throws Exception {
        mockMvc.perform(post("/guild/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private static Stream<String> invalidCreateBodies() {
        return Stream.of(
                "{\"title\":\"\",\"reward\":90}",
                "{\"title\":null,\"reward\":90}",
                "{\"title\":\"" + "x".repeat(81) + "\",\"reward\":90}",
                "{\"title\":\"Квест\",\"reward\":0}",
                "{\"title\":\"Квест\",\"reward\":10001}",
                "{\"title\":\"Квест\"}",
                "{\"title\":"
        );
    }

    @ParameterizedTest
    @MethodSource("invalidClaimBodies")
    void rejectsBlankOrOverlongHeroName(String body) throws Exception {
        int id = createRequest("Доставить письмо", 45);

        mockMvc.perform(put("/guild/requests/{id}/claim", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("validHeroNames")
    void acceptsInclusiveHeroNameBounds(String heroName) throws Exception {
        int id = createRequest("Заявка для героя " + heroName.length(), 45);

        mockMvc.perform(put("/guild/requests/{id}/claim", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimBody(heroName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimedBy").value(heroName));
    }

    private static Stream<String> validHeroNames() {
        return Stream.of("x", "x".repeat(40));
    }

    private static Stream<String> invalidClaimBodies() {
        return Stream.of(
                "{\"heroName\":\" \"}",
                "{\"heroName\":null}",
                "{\"heroName\":\"" + "x".repeat(41) + "\"}"
        );
    }

    private int createRequest(String title, int reward) throws Exception {
        MvcResult result = mockMvc.perform(post("/guild/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(title, reward)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
    }

    private static String createBody(String title, int reward) {
        return "{\"title\":\"" + title + "\",\"reward\":" + reward + "}";
    }

    private static String claimBody(String heroName) {
        return "{\"heroName\":\"" + heroName + "\"}";
    }
}
