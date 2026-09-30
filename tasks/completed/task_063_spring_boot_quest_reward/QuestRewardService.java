package learning.task063;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class QuestRewardService {
    private final Map<Integer, QuestReward> rewards = Map.of(
            1, new QuestReward(1, 40),
            2, new QuestReward(2, 75),
            3, new QuestReward(3, 120)
    );

    public Optional<QuestReward> findReward(int questId) {
        if (questId < 1 || questId > rewards.size()) return Optional.empty();

        if (rewards.containsKey(questId)) {
            return Optional.of(rewards.get(questId));
        }

        return Optional.empty();
    }
}
