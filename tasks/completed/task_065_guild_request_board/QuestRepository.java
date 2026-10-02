package learning.task065;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class QuestRepository {

    private int nextId = 1;

    private final Map<Integer, Quest> quests = new HashMap<>();

    public Map<Integer, Quest> getQuests() {
        return Map.copyOf(quests);
    }

    public int addQuest(Quest quest) {
        quests.put(nextId, quest);
        return nextId++;
    }

    public boolean isQuest(int id ) {
        return quests.containsKey(id);
    }

    public Optional<Quest> claimQuestById(int id, Hero hero) {
        if (id < 0 || id > quests.size()) return Optional.empty();
        Quest oldQuest = quests.get(id);
        if (oldQuest.claimedBy() == null) {
            Quest newQuest = new Quest(oldQuest.title(), oldQuest.reward(), QuestStatus.CLAIMED, hero.heroName());
            quests.put(id, newQuest);
            return Optional.of(newQuest);
        }
        throw new QuestAlreadyClaimedException(String.format("Quest with id %d already claimed", id));
    }

    public boolean removeQuest(Quest quest) {
        if (quests.containsValue(quest)) {
            quests.remove(quest);
            return true;
        }
        return false;
    }

    public Optional<Quest> getQuestById(int id) {
        if (quests.isEmpty()) {
            return Optional.empty();
        } else if (quests.get(id) != null) {
            return Optional.of(quests.get(id));
        } else {
            return Optional.empty();
        }
    }
}
