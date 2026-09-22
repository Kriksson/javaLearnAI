package learning.task032;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MatchmakingQueueTest {
    private static final QueueTicket MIRA = new QueueTicket(" mira ", 1200);
    private static final QueueTicket ALEX = new QueueTicket("alex", 1500);
    private static final QueueTicket ARIA = new QueueTicket("aria", 1300);

    @Test
    void joinsOnlyUniquePlayersAndKeepsWaitingOrder() {
        MatchmakingQueue queue = new MatchmakingQueue();

        assertTrue(queue.join(MIRA));
        assertTrue(queue.join(ALEX));
        assertFalse(queue.join(new QueueTicket("mira", 2000)));
        assertEquals(List.of(new QueueTicket("mira", 1200), ALEX), queue.waitingPlayers());
        assertThrows(UnsupportedOperationException.class, () -> queue.waitingPlayers().clear());
    }

    @Test
    void startsMatchInQueueOrderAndLeavesOthersWaiting() {
        MatchmakingQueue queue = new MatchmakingQueue();
        queue.join(MIRA);
        queue.join(ALEX);
        queue.join(ARIA);

        assertEquals(List.of(new QueueTicket("mira", 1200), ALEX), queue.startMatch(2));
        assertEquals(List.of(ARIA), queue.waitingPlayers());
        assertEquals(1, queue.waitingCount());
        assertTrue(queue.join(MIRA));
    }

    @Test
    void doesNotChangeQueueWhenPlayersAreInsufficientAndValidatesInput() {
        MatchmakingQueue queue = new MatchmakingQueue();
        queue.join(MIRA);

        assertEquals(List.of(), queue.startMatch(2));
        assertEquals(List.of(new QueueTicket("mira", 1200)), queue.waitingPlayers());
        assertFalse(queue.cancel("missing"));
        assertTrue(queue.cancel(" mira "));
        assertTrue(queue.join(MIRA));
        assertEquals(List.of(new QueueTicket("mira", 1200)), queue.startMatch(1));
        assertThrows(IllegalArgumentException.class, () -> new QueueTicket(null, 0));
        assertThrows(IllegalArgumentException.class, () -> new QueueTicket("mira", -1));
        assertThrows(IllegalArgumentException.class, () -> new QueueTicket("mira", 5001));
        assertThrows(IllegalArgumentException.class, () -> queue.join(null));
        assertThrows(IllegalArgumentException.class, () -> queue.cancel(" "));
        assertThrows(IllegalArgumentException.class, () -> queue.startMatch(0));
    }
}
