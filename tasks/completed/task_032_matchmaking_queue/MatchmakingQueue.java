package learning.task032;

import java.util.*;

public class MatchmakingQueue {

    private final ArrayDeque<QueueTicket> queue = new ArrayDeque<>();
    private final Set<String> waitingIds = new HashSet<>();

    public boolean join(QueueTicket ticket) {
        if (ticket == null) throw new IllegalArgumentException();
        if (!waitingIds.add(ticket.playerId())) {
            return false;
        }
        queue.addLast(ticket);
        return true;
    }

    public boolean cancel(String playerId) {
        if (playerId == null || playerId.trim().isEmpty()) throw new IllegalArgumentException();
        boolean find = false;
        for (QueueTicket ticket : queue) {
            if (Objects.equals(ticket.playerId(), playerId.trim())) {
                queue.remove(ticket);
                waitingIds.remove(ticket.playerId());
                find = true;
                break;
            }
        }
        return find;
    }

    public List<QueueTicket> startMatch(int teamSize) {
        if (teamSize <= 0) throw new IllegalArgumentException();
        if (teamSize > queue.size()) return List.of();
        List<QueueTicket> result = new ArrayList<>();
        for (int i = 0; i < teamSize; i++) {
            QueueTicket ticket = queue.removeFirst();
            result.add(ticket);
            waitingIds.remove(ticket.playerId());
        }
        return List.copyOf(result);
    }

    public int waitingCount() {
        return queue.size();
    }

    public List<QueueTicket> waitingPlayers() {
        return List.copyOf(queue);
    }
}
