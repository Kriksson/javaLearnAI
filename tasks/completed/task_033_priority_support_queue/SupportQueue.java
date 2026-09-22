package learning.task033;

import java.util.*;

public class SupportQueue {

    private static final Comparator<SupportTicket> ticketSort = Comparator.comparingInt((SupportTicket e) -> e.priority().getWeight()).
            reversed().
            thenComparingLong(SupportTicket::createdOrder).
            thenComparing(SupportTicket::id);

    private final PriorityQueue<SupportTicket> priorityQueue =  new PriorityQueue<>(ticketSort);
    private final Set<String> waitingsIds = new HashSet<>();

    public boolean submit(SupportTicket ticket) {
        if (ticket == null) throw new IllegalArgumentException();
        if (!waitingsIds.add(ticket.id())) return false;
        priorityQueue.add(ticket);
        return true;
    }

    public Optional<SupportTicket> next() {
        if (priorityQueue.isEmpty()) return Optional.empty();
        SupportTicket ticket = priorityQueue.poll();
        waitingsIds.remove(ticket.id());
        return Optional.of(ticket);
    }

    public int size() {
        return priorityQueue.size();
    }

    public List<SupportTicket> waiting() {
        final List<SupportTicket> result = new ArrayList<>(List.copyOf(priorityQueue));
        result.sort(ticketSort);
        return List.copyOf(result);
    }
}
