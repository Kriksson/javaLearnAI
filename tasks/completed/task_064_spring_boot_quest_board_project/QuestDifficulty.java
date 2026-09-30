package learning.task064;

public enum QuestDifficulty {
    EASY(40),
    NORMAL(75),
    HARD(120);

    private final int reward;

    QuestDifficulty(int reward) {
        this.reward = reward;
    }

    public int getReward() {
        return reward;
    }
}
