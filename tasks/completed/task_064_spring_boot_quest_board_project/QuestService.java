package learning.task064;


import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QuestService {

    private final QuestRepository questRepository;

    public QuestService(QuestRepository questRepository) {
        this.questRepository = questRepository;
    }

    public Map<Integer, Quest> getAllQuests() {
        return questRepository.findAll();
    }

    public Optional<Quest> getQuestById(int id) {
        return questRepository.findById(id);
    }

    public int getRewardByQuest(Quest quest) {
        return quest.difficulty().getReward();
    }
}
