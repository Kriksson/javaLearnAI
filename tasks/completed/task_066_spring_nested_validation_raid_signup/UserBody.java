package learning.task066;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserBody(
        @NotBlank @Size(max=30) String playerName,
        @NotNull @Valid Hero hero) {
}
