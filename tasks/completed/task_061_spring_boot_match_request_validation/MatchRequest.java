package learning.task061;


import jakarta.validation.constraints.*;

public record MatchRequest(
        @NotBlank @Size(max = 20)  String playerName,
        @NotNull @Min(1) @Max(100) Integer level,
        @NotNull @Min(1) Integer opponentId) { }
