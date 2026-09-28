package learning.task062;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlayerRequest(
        @NotBlank @Size(max = 20) String name,
        @NotNull @Min(1) @Max(100) Integer level) {
}
