package learning.task019;

public final class RewardCalculator {
    private RewardCalculator() {
    }

    public static int calculate(int baseExperience,
                                int completedObjectives,
                                int totalObjectives,
                                boolean premium) {
        if (baseExperience < 0 || totalObjectives < 1 || (completedObjectives < 0 || completedObjectives > totalObjectives)) {
            throw new IllegalArgumentException();
        }

        int reward = baseExperience * completedObjectives / totalObjectives;
        if (premium && (completedObjectives / totalObjectives) == 1) {
            reward += reward/5;
        }
        return reward;
    }
}
