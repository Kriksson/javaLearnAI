package learning.task016;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QuestProgressTest {
    @Test
    void createQuestWIthTrimAndCheckSteps() {
        QuestProgress questProgress = new QuestProgress(" Find Diamond ", 1);

        Assertions.assertEquals(1, questProgress.getTargetSteps());
        Assertions.assertEquals("Find Diamond", questProgress.getTitle());
        Assertions.assertEquals(0, questProgress.getCompletedSteps());
    }

    @Test
    void createQuestWithSpaceTitleAndZeroSteps() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new QuestProgress(" ", 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new QuestProgress(" ", 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new QuestProgress("Get stone sword", 0));
    }

    @Test
    void createQuestAndAddSteps() {
        QuestProgress questProgress = new QuestProgress(" Find Diamond ", 50);
        Assertions.assertEquals(50, questProgress.getTargetSteps());
        questProgress.addProgress(30);
        Assertions.assertEquals(30, questProgress.getCompletedSteps());
        Assertions.assertEquals(20, questProgress.getRemainingSteps());

    }

    @Test
    void createQuestAndAddStepsNegative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new QuestProgress(" Find Diamond ", -1));
    }

    @Test
    void createQuestAndCheckCompleteSteps() {
        QuestProgress questProgress = new QuestProgress(" Find Diamond ", 100);
        Assertions.assertEquals(100, questProgress.getTargetSteps());
        questProgress.addProgress(150);
        Assertions.assertEquals(100, questProgress.getCompletedSteps());
        Assertions.assertEquals(0, questProgress.getRemainingSteps());
    }
}
