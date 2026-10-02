package learning.task065;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuestService {

    QuestRepository questRepository;
    public QuestService(QuestRepository questRepository) {
        this.questRepository = questRepository;
    }

    public int createQuest(QuestRequest questRequest) {
        if (questRequest == null) {
            return -1;
        } else {
            return questRepository.addQuest(new Quest(questRequest.title(), questRequest.reward(),
                    QuestStatus.OPEN, null));
        }
    }

    public Optional<QuestAnswer> claimQuestById(int id, Hero hero) {
        Optional<Quest> q = questRepository.claimQuestById(id, hero);
        if (q.isPresent()) {
            Quest quest = q.get();
            QuestAnswer questAnswer = new QuestAnswer(id,  quest.title(), quest.reward(), QuestStatus.CLAIMED, quest.claimedBy());
            return Optional.of(questAnswer);
        } else {
            return Optional.empty();
        }
    }

    public List<QuestAnswer> getAllQuests() {
        Map<Integer, Quest> quests = questRepository.getQuests();
        if (quests == null || quests.isEmpty()) {
            return List.of();
        } else {
            List<QuestAnswer> questAnswers = new ArrayList<>();
            for (Map.Entry<Integer, Quest> entry : quests.entrySet()) {
                Quest quest = entry.getValue();
                int id = entry.getKey();
                questAnswers.add(new QuestAnswer(id, quest.title(), quest.reward(), quest.status(), quest.claimedBy()));
            }
            questAnswers.sort(Comparator.comparingInt(e -> e.id()));
            return questAnswers;
        }
    }

    public Optional<QuestAnswer> getQuestById(int id) {
        Optional<Quest> quest = questRepository.getQuestById(id);
        if  (quest.isPresent()) {
            Quest q =  quest.get();
            return Optional.of(new QuestAnswer(id, q.title(), q.reward(), q.status(), q.claimedBy()));
        }
        return Optional.empty();
    }

}
