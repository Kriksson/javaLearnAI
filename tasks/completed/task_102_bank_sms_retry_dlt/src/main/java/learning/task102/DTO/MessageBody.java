package learning.task102.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MessageBody(
        @NotNull @Min(1) Long clientId,
        @NotBlank @Size(min=1, max=120) String text) {
}
