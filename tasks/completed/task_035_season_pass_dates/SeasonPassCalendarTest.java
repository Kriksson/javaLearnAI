package learning.task035;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeasonPassCalendarTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 22);

    @Test
    void findsActivePassesIncludingBothPeriodBoundaries() {
        SeasonPassCalendar calendar = new SeasonPassCalendar();
        SeasonPass mira = new SeasonPass("Mira", LocalDate.of(2026, 9, 20), TODAY);
        SeasonPass alex = new SeasonPass("Alex", TODAY, LocalDate.of(2026, 9, 25));
        SeasonPass zed = new SeasonPass("Zed", LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 30));

        assertEquals(List.of("Alex", "Mira"),
                calendar.activePlayerIds(Arrays.asList(mira, null, zed, alex), TODAY));
    }

    @Test
    void countsRemainingDaysInclusivelyAndHandlesExpiredPass() {
        SeasonPassCalendar calendar = new SeasonPassCalendar();
        SeasonPass active = new SeasonPass("Mira", LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 25));
        SeasonPass expired = new SeasonPass("Zed", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 21));

        assertEquals(4, calendar.daysRemaining(active, TODAY));
        assertEquals(1, calendar.daysRemaining(new SeasonPass("Alex", TODAY, TODAY), TODAY));
        assertEquals(0, calendar.daysRemaining(expired, TODAY));
    }

    @Test
    void validatesDatesAndReturnsImmutableResult() {
        SeasonPassCalendar calendar = new SeasonPassCalendar();

        assertThrows(IllegalArgumentException.class,
                () -> new SeasonPass("Mira", TODAY, TODAY.minusDays(1)));
        assertEquals("Mira", new SeasonPass(" Mira ", TODAY, TODAY).playerId());
        assertThrows(IllegalArgumentException.class, () -> new SeasonPass(null, TODAY, TODAY));
        assertThrows(IllegalArgumentException.class, () -> calendar.activePlayerIds(null, TODAY));
        assertThrows(IllegalArgumentException.class, () -> calendar.activePlayerIds(List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> calendar.daysRemaining(null, TODAY));
        assertThrows(UnsupportedOperationException.class,
                () -> calendar.activePlayerIds(List.of(), TODAY).add("Mira"));
    }
}
