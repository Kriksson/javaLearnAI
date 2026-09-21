package learning.task017;

public class Quest {

    private final String id;
    private final String title;
    private QuestStatus status = QuestStatus.AVAILABLE;

    public Quest(String id, String title) {
        id = id.trim();
        title = title.trim();
        if (id.isEmpty() || title.isEmpty()) throw new IllegalArgumentException();
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public void start() {
        if (status == QuestStatus.ACTIVE ||  status == QuestStatus.COMPLETED) throw new IllegalStateException();
        status = QuestStatus.ACTIVE;
    }

    public void complete() {
        if (status == QuestStatus.COMPLETED || status == QuestStatus.AVAILABLE) throw new IllegalStateException();
        status = QuestStatus.COMPLETED;
    }
}
