package learning.task033;

public enum TicketPriority {
    URGENT(3),
    NORMAL(2),
    LOW(1);

    private final int weight;

    TicketPriority(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
