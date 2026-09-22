package learning.task035;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class SeasonPassCalendar {
    public List<String> activePlayerIds(List<SeasonPass> passes, LocalDate date) {
        if (passes == null || date == null) throw new IllegalArgumentException();
        return passes.stream()
                .filter(Objects::nonNull)
                .filter((SeasonPass sp) -> !date.isBefore(sp.startsOn()) && !date.isAfter(sp.endsOn()))
                .sorted(Comparator.comparing(SeasonPass::playerId))
                .map(SeasonPass::playerId)
                .toList();
    }

    public long daysRemaining(SeasonPass pass, LocalDate date) {
        if (pass == null || date == null) throw new IllegalArgumentException();
        if (date.isBefore(pass.startsOn()) || date.isAfter(pass.endsOn())) return 0;
        return ChronoUnit.DAYS.between(date, pass.endsOn().plusDays(1));
    }
}
