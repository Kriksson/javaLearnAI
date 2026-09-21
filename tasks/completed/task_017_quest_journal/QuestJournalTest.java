package learning.task017;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QuestJournalTest {
    // TODO: добавь минимум три теста с @Test по условию задачи

    @Test
    void fullRoadQuest() {
        Quest quest = new Quest("12-d", "Find Diamond");
        Assertions.assertEquals(QuestStatus.AVAILABLE, quest.getStatus());
        quest.start();
        Assertions.assertEquals(QuestStatus.ACTIVE, quest.getStatus());
        quest.complete();
        Assertions.assertEquals(QuestStatus.COMPLETED, quest.getStatus());
    }

    @Test
    void skipRoadQuest() {
        Quest quest = new Quest("12-d", "Find Diamond");
        Assertions.assertEquals(QuestStatus.AVAILABLE, quest.getStatus());
        Assertions.assertThrows(IllegalStateException.class, () -> quest.complete());
        quest.start();
        quest.complete();
        Assertions.assertThrows(IllegalStateException.class, () -> quest.complete());
        Assertions.assertThrows(IllegalStateException.class, () -> quest.start());
    }

    @Test
    void findByIdTest() {
        Quest quest = new Quest("12-d", "Find Diamond");
        QuestJournal questJournal = new QuestJournal();
        questJournal.add(quest);
        Assertions.assertEquals(quest, questJournal.getById("12-d"));
    }

    @Test
    void startTestThroughJournal() {
        Quest quest = new Quest("12-d", "Find Diamond");
        QuestJournal questJournal = new QuestJournal();
        questJournal.add(quest);

        Assertions.assertDoesNotThrow(() -> questJournal.startQuest("12-d"));
        Assertions.assertDoesNotThrow(() -> questJournal.completeQuest("12-d"));
        Assertions.assertEquals(QuestStatus.COMPLETED, quest.getStatus());
        Assertions.assertEquals(1, questJournal.getCompletedCount());
    }

    @Test
    void startQuestForUnknownID() {
        Quest quest = new Quest("12-d", "Find Diamond");
        QuestJournal questJournal = new QuestJournal();
        questJournal.add(quest);

        Assertions.assertThrows(IllegalArgumentException.class, () -> questJournal.startQuest("s-d"));
    }
}
