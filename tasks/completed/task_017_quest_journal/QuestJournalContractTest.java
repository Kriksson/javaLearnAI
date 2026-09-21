package learning.task017;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestJournalContractTest {
    @Test
    void newQuestHasCleanedValuesAndAvailableStatus() {
        Quest quest = new Quest("  relic  ", "  Find relic  ");

        assertEquals("relic", quest.getId());
        assertEquals("Find relic", quest.getTitle());
        assertEquals(QuestStatus.AVAILABLE, quest.getStatus());
    }

    @Test
    void questAllowsOnlyValidStatusTransitions() {
        Quest quest = new Quest("relic", "Find relic");

        assertThrows(IllegalStateException.class, quest::complete);
        assertEquals(QuestStatus.AVAILABLE, quest.getStatus());

        quest.start();
        assertEquals(QuestStatus.ACTIVE, quest.getStatus());
        assertThrows(IllegalStateException.class, quest::start);

        quest.complete();
        assertEquals(QuestStatus.COMPLETED, quest.getStatus());
        assertThrows(IllegalStateException.class, quest::complete);
        assertThrows(IllegalStateException.class, quest::start);
    }

    @Test
    void validatesQuestArguments() {
        assertThrows(IllegalArgumentException.class, () -> new Quest(" ", "Find relic"));
        assertThrows(IllegalArgumentException.class, () -> new Quest("relic", " "));
    }

    @Test
    void journalAddsFindsAndRunsQuestById() {
        QuestJournal journal = new QuestJournal();
        Quest quest = new Quest("relic", "Find relic");
        journal.add(quest);

        assertEquals(1, journal.size());
        assertSame(quest, journal.getById(" relic "));

        journal.startQuest("relic");
        journal.completeQuest("relic");
        assertEquals(QuestStatus.COMPLETED, quest.getStatus());
        assertEquals(1, journal.getCompletedCount());
    }

    @Test
    void journalValidatesNullDuplicatesAndUnknownIds() {
        QuestJournal journal = new QuestJournal();
        journal.add(new Quest("relic", "Find relic"));

        assertThrows(IllegalArgumentException.class, () -> journal.add(null));
        assertThrows(IllegalArgumentException.class,
                () -> journal.add(new Quest("relic", "Another title")));
        assertThrows(IllegalArgumentException.class, () -> journal.getById(" "));
        assertThrows(IllegalArgumentException.class, () -> journal.getById("missing"));
        assertThrows(IllegalArgumentException.class, () -> journal.startQuest("missing"));
        assertThrows(IllegalArgumentException.class, () -> journal.completeQuest("missing"));
    }
}
