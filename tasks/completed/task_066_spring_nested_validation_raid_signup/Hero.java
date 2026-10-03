package learning.task066;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Hero(
        @NotBlank @Size(max=40) String name,
        @Min(1) @Max(100) int level,
        @NotBlank @Size(max=20) String className) {
}
