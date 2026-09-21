package learning.task016;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestProgressContractTest {
    @Test
    void createsQuestWithTrimmedTitleAndEmptyProgress() {
        QuestProgress quest = new QuestProgress("  Find relic  ", 5);

        assertEquals("Find relic", quest.getTitle());
        assertEquals(5, quest.getTargetSteps());
        assertEquals(0, quest.getCompletedSteps());
        assertEquals(5, quest.getRemainingSteps());
        assertFalse(quest.isCompleted());
    }

    @Test
    void addsProgressAndCompletesQuestAtTarget() {
        QuestProgress quest = new QuestProgress("Find relic", 5);

        quest.addProgress(3);
        assertEquals(3, quest.getCompletedSteps());
        assertEquals(2, quest.getRemainingSteps());

        quest.addProgress(10);
        assertEquals(5, quest.getCompletedSteps());
        assertEquals(0, quest.getRemainingSteps());
        assertTrue(quest.isCompleted());
    }

    @Test
    void rejectsInvalidConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () -> new QuestProgress(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> new QuestProgress("Find relic", 0));
        assertThrows(IllegalArgumentException.class, () -> new QuestProgress("Find relic", -1));
    }

    @Test
    void rejectsNonPositiveProgress() {
        QuestProgress quest = new QuestProgress("Find relic", 5);

        assertThrows(IllegalArgumentException.class, () -> quest.addProgress(0));
        assertThrows(IllegalArgumentException.class, () -> quest.addProgress(-1));
        assertEquals(0, quest.getCompletedSteps());
    }
}
