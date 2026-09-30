package learning.task064;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuestBoardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsAllQuestsInIdOrder() throws Exception {
        mockMvc.perform(get("/quests"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Лесной патруль"))
                .andExpect(jsonPath("$[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Пещера гоблинов"))
                .andExpect(jsonPath("$[1].difficulty").value("NORMAL"))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[2].name").value("Башня мага"))
                .andExpect(jsonPath("$[2].difficulty").value("HARD"));
    }

    @Test
    void returnsOneQuest() throws Exception {
        mockMvc.perform(get("/quests/2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Пещера гоблинов"))
                .andExpect(jsonPath("$.difficulty").value("NORMAL"));
    }

    @ParameterizedTest
    @CsvSource({
            "1, 'Лесной патруль', 40",
            "2, 'Пещера гоблинов', 75",
            "3, 'Башня мага', 120"
    })
    void calculatesReward(int questId, String questName, int coins) throws Exception {
        mockMvc.perform(get("/quests/{questId}/reward", questId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.questId").value(questId))
                .andExpect(jsonPath("$.questName").value(questName))
                .andExpect(jsonPath("$.coins").value(coins));
    }

    @Test
    void unknownQuestReturns404WithoutBody() throws Exception {
        mockMvc.perform(get("/quests/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mockMvc.perform(get("/quests/99/reward"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/quests/abc", "/quests/abc/reward"})
    void nonNumericQuestIdReturns400(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isBadRequest());
    }
}
