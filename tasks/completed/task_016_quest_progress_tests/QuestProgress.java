package learning.task016;

public class QuestProgress {

    private final String title;
    private final int targetSteps;
    private int completedSteps = 0;

    public QuestProgress(String title, int targetSteps) {
        title = title.trim();
        if (title.isEmpty()) throw new IllegalArgumentException();
        if (targetSteps < 1) throw new IllegalArgumentException();
        this.title = title;
        this.targetSteps = targetSteps;
    }

    public String getTitle() {
        return title;
    }

    public int getTargetSteps() {
        return targetSteps;
    }

    public int getCompletedSteps() {
        return completedSteps;
    }

    public int getRemainingSteps() {
        return Math.max(0, targetSteps - completedSteps);
    }

    public boolean isCompleted() {
        return completedSteps == targetSteps;
    }

    public void addProgress(int steps) {
        if (steps < 1) throw new IllegalArgumentException();
        completedSteps = Math.min(targetSteps, completedSteps + steps);
    }
}
