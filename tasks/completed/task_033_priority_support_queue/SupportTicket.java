package learning.task033;

public record SupportTicket(String id, TicketPriority priority, long createdOrder) {
    public SupportTicket(String id, TicketPriority priority, long createdOrder) {
        if (id == null || id.trim().isEmpty() || priority == null || createdOrder < 0)
            throw new IllegalArgumentException();
        id = id.trim();
        this.id = id;
        this.priority = priority;
        this.createdOrder = createdOrder;
    }
}
