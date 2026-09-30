package learning.task064;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
public class QuestController {

    QuestService questService;

    public QuestController(QuestService questService) {
        this.questService = questService;
    }


    @GetMapping("/quests")
    ResponseEntity<List<QuestAnswer>> getQuests() {
        if (questService.getAllQuests().isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            List<QuestAnswer> questAnswersList = new ArrayList<>();
            for (Map.Entry<Integer, Quest> entry : questService.getAllQuests().entrySet()) {
                int id = entry.getKey();
                Quest quest = entry.getValue();
                questAnswersList.add(new QuestAnswer(id, quest.name(), quest.difficulty()));
            }
            questAnswersList.sort(Comparator.comparingInt(QuestAnswer::id));
            return ResponseEntity.ok(questAnswersList);
        }
    }

    @GetMapping("/quests/{id}")
    ResponseEntity<QuestAnswer> getQuestById(@PathVariable("id") int id) {
        if (questService.getQuestById(id).isPresent()) {
            Quest quest = questService.getQuestById(id).get();
            QuestAnswer questAnswer = new QuestAnswer(id, quest.name(), quest.difficulty());
            return ResponseEntity.ok(questAnswer);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/quests/{id}/reward")
    ResponseEntity<QuestReward> getRewardById(@PathVariable("id") int id) {
        if (questService.getQuestById(id).isPresent()) {
            Quest quest = questService.getQuestById(id).get();
            int reward = questService.getRewardByQuest(quest);
            QuestReward questReward = new QuestReward(id, quest.name(), reward);
            return ResponseEntity.ok(questReward);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
