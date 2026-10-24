package learning.task100.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MessageDTO(
        @Min(1) @NotNull Long clientId,
        @NotBlank @Size(min=1, max=120) String text) {
}
