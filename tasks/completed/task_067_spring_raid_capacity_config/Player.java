package learning.task067;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Player(@NotBlank @Size(max = 30) String playerName) {}