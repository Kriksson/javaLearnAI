package learning.task017;

import java.util.HashMap;
import java.util.Map;

public class QuestJournal {

    private final Map<String, Quest> questJournal =  new HashMap<>();

    public QuestJournal() {
    }

    public void add(Quest quest) {
        if (quest == null || questJournal.containsKey(quest.getId())) throw new IllegalArgumentException();
        questJournal.put(quest.getId(), quest);
    }

    public int size() {
        return questJournal.size();
    }

    public Quest getById(String id) {
        id = id.trim();
        if (id.isEmpty()) throw new IllegalArgumentException();
        if  (questJournal.containsKey(id)) return questJournal.get(id);
        throw new IllegalArgumentException();
    }

    public void startQuest(String id) {
        if (id.trim().isEmpty()) throw new IllegalArgumentException();
        Quest quest = getById(id);
        quest.start();
    }

    public void completeQuest(String id) {
        if (id.trim().isEmpty()) throw new IllegalArgumentException();
        Quest quest = getById(id);
        quest.complete();
    }

    public int getCompletedCount() {
        int total = 0;
        for (Quest quest : questJournal.values()) {
            if (quest.getStatus() == QuestStatus.COMPLETED) total++;
        }
        return total;
    }
}
