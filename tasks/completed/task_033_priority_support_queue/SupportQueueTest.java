package learning.task033;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SupportQueueTest {
    @Test
    void returnsTicketsByPriorityThenAgeThenId() {
        SupportQueue queue = new SupportQueue();
        SupportTicket low = new SupportTicket("low", TicketPriority.LOW, 0);
        SupportTicket urgentLater = new SupportTicket("zara", TicketPriority.URGENT, 5);
        SupportTicket urgentEarlier = new SupportTicket("mira", TicketPriority.URGENT, 2);
        SupportTicket urgentSameAge = new SupportTicket("alex", TicketPriority.URGENT, 2);
        queue.submit(low);
        queue.submit(urgentLater);
        queue.submit(urgentEarlier);
        queue.submit(urgentSameAge);

        assertEquals(List.of(urgentSameAge, urgentEarlier, urgentLater, low), queue.waiting());
        assertEquals(Optional.of(urgentSameAge), queue.next());
        assertEquals(Optional.of(urgentEarlier), queue.next());
    }

    @Test
    void preventsDuplicateIdsAndAllowsIdAfterTicketIsTaken() {
        SupportQueue queue = new SupportQueue();
        SupportTicket first = new SupportTicket("p-1", TicketPriority.NORMAL, 1);

        assertTrue(queue.submit(first));
        assertFalse(queue.submit(new SupportTicket("p-1", TicketPriority.URGENT, 0)));
        assertEquals(Optional.of(first), queue.next());
        assertTrue(queue.submit(new SupportTicket("p-1", TicketPriority.URGENT, 0)));
    }

    @Test
    void waitingReturnsTicketsInPollOrderRatherThanHeapIterationOrder() {
        SupportQueue queue = new SupportQueue();
        SupportTicket urgent = new SupportTicket("urgent", TicketPriority.URGENT, 0);
        SupportTicket low = new SupportTicket("low", TicketPriority.LOW, 0);
        SupportTicket normal = new SupportTicket("normal", TicketPriority.NORMAL, 0);
        queue.submit(urgent);
        queue.submit(low);
        queue.submit(normal);

        assertEquals(List.of(urgent, normal, low), queue.waiting());
    }

    @Test
    void returnsEmptyAndValidatesInput() {
        SupportQueue queue = new SupportQueue();

        assertEquals(Optional.empty(), queue.next());
        assertThrows(UnsupportedOperationException.class, () -> queue.waiting().clear());
        assertThrows(IllegalArgumentException.class, () -> new SupportTicket(null, TicketPriority.LOW, 0));
        assertThrows(IllegalArgumentException.class, () -> new SupportTicket("id", null, 0));
        assertThrows(IllegalArgumentException.class, () -> new SupportTicket("id", TicketPriority.LOW, -1));
        assertThrows(IllegalArgumentException.class, () -> queue.submit(null));
    }
}
