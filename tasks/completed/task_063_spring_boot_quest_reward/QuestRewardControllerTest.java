package learning.task063;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuestRewardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @CsvSource({"1, 40", "2, 75", "3, 120"})
    void returnsRewardForKnownQuest(int questId, int coins) throws Exception {
        mockMvc.perform(get("/quests/{questId}/reward", questId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.questId").value(questId))
                .andExpect(jsonPath("$.coins").value(coins));
    }

    @Test
    void returns404ForUnknownQuest() throws Exception {
        mockMvc.perform(get("/quests/99/reward"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void returns400WhenQuestIdIsNotAnInteger() throws Exception {
        mockMvc.perform(get("/quests/abc/reward"))
                .andExpect(status().isBadRequest());
    }
}
