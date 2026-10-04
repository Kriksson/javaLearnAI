package learning.task070;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Recruit(
        @NotBlank @Size(max=30) String name,
        @Min(1) @Max(100) int level) {
}
