package learning.task035;

import java.time.LocalDate;

public record SeasonPass(String playerId, LocalDate startsOn, LocalDate endsOn) {
    public SeasonPass(String playerId, LocalDate startsOn, LocalDate endsOn) {
        if (playerId == null || playerId.trim().isEmpty() || startsOn == null || endsOn == null ||
                startsOn.isAfter(endsOn)) throw new IllegalArgumentException();
        playerId = playerId.trim();
        this.playerId = playerId;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
    }
}
