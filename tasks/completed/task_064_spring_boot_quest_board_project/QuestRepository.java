package learning.task064;


import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class QuestRepository {

    private final Map<Integer, Quest> quests = Map.of(
            1, new Quest("Лесной патруль", QuestDifficulty.EASY),
            2, new Quest("Пещера гоблинов", QuestDifficulty.NORMAL),
            3, new Quest("Башня мага", QuestDifficulty.HARD)
    );

    public Optional<Quest> findById(int id) {
        if (quests.containsKey(id)) {
            return  Optional.of(quests.get(id));
        }
        return Optional.empty();
    }

    public Map<Integer, Quest> findAll() {
        if (quests.isEmpty()) {
            return Map.of();
        } else  {
            return Map.copyOf(quests);
        }
    }
}
