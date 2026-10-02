package learning.task065;

public class QuestAlreadyClaimedException extends RuntimeException {
    public QuestAlreadyClaimedException(String message) {
        super(message);
    }
}
