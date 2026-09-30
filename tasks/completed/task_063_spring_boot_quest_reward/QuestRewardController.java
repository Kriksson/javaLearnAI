package learning.task063;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class QuestRewardController {
    private final QuestRewardService questRewardService;

    public QuestRewardController(QuestRewardService questRewardService) {
        this.questRewardService = questRewardService;
    }

    @GetMapping("/quests/{questId}/reward")
    public ResponseEntity<QuestReward> getReward(@PathVariable("questId") int questId) {
        Optional<QuestReward> result = questRewardService.findReward(questId);

        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        }

        return ResponseEntity.notFound().build();
    }
}
